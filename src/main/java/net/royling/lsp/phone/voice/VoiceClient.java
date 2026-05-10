package net.royling.lsp.phone.voice;

import net.royling.lsp.LetterSignalPhone;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.royling.lsp.phone.client.PhoneSettings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class VoiceClient {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHANNELS = 1;
    private static final int FRAME_SAMPLES = 960;
    private static final int PCM_BYTES = FRAME_SAMPLES * 2;
    private static final int MAX_OPUS_BYTES = 1275;
    private static final int MAX_PACKET_BYTES = 16 + MAX_OPUS_BYTES;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
    private static volatile boolean running;
    private static int serverPort = 24455;
    private static DatagramSocket socket;
    private static Thread captureThread;
    private static Thread playbackThread;

    private VoiceClient() {
    }

    public static synchronized void start() {
        if (running) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(250);
            InetSocketAddress serverAddress = new InetSocketAddress(resolveServerHost(minecraft), serverPort);
            UUID playerId = minecraft.player.getUUID();
            running = true;

            captureThread = new Thread(() -> captureLoop(playerId, serverAddress), "LetterSignalPhone Voice Capture");
            captureThread.setDaemon(true);
            captureThread.start();

            playbackThread = new Thread(VoiceClient::playbackLoop, "LetterSignalPhone Voice Playback");
            playbackThread.setDaemon(true);
            playbackThread.start();
        } catch (Exception exception) {
            running = false;
            closeSocket();
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("message.letter_signal_phone.voice.unavailable"));
            }
            LetterSignalPhone.LOGGER.warn("Unable to start voice client", exception);
        }
    }

    public static synchronized void stop() {
        running = false;
        closeSocket();
    }

    public static void configureServerPort(int port) {
        serverPort = port;
    }

    private static void captureLoop(UUID playerId, InetSocketAddress serverAddress) {
        TargetDataLine microphone = null;
        try {
            OpusEncoder encoder = new OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP);
            encoder.setBitrate(24_000);
            encoder.setUseVBR(true);
            encoder.setComplexity(5);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            Mixer.Info selectedMicrophone = PhoneSettings.selectedMicrophone();
            microphone = selectedMicrophone == null
                    ? (TargetDataLine) AudioSystem.getLine(info)
                    : (TargetDataLine) AudioSystem.getMixer(selectedMicrophone).getLine(info);
            microphone.open(FORMAT, PCM_BYTES * 4);
            microphone.start();

            byte[] pcm = new byte[PCM_BYTES];
            byte[] packetBytes = new byte[MAX_PACKET_BYTES];
            ByteBuffer.wrap(packetBytes, 0, 16).putLong(playerId.getMostSignificantBits()).putLong(playerId.getLeastSignificantBits());

            while (running && socket != null && !socket.isClosed()) {
                readFully(microphone, pcm);
                applyVolume(pcm, FRAME_SAMPLES, PhoneSettings.inputVolume());
                int encoded = encoder.encode(pcm, 0, FRAME_SAMPLES, packetBytes, 16, MAX_OPUS_BYTES);
                DatagramPacket packet = new DatagramPacket(packetBytes, 16 + encoded, serverAddress);
                socket.send(packet);
            }
        } catch (Exception exception) {
            if (running) {
                LetterSignalPhone.LOGGER.warn("Voice capture stopped", exception);
            }
        } finally {
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
        }
    }

    private static void playbackLoop() {
        SourceDataLine speaker = null;
        try {
            OpusDecoder decoder = new OpusDecoder(SAMPLE_RATE, CHANNELS);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(FORMAT, PCM_BYTES * 8);
            speaker.start();

            byte[] packetBuffer = new byte[MAX_PACKET_BYTES];
            byte[] pcm = new byte[PCM_BYTES * 3];
            while (running && socket != null && !socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
                try {
                    socket.receive(packet);
                    if (packet.getLength() > 16) {
                        int decodedSamples = decoder.decode(packet.getData(), packet.getOffset() + 16, packet.getLength() - 16, pcm, 0, FRAME_SAMPLES, false);
                        applyVolume(pcm, decodedSamples, PhoneSettings.outputVolume());
                        speaker.write(pcm, 0, decodedSamples * 2);
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (Exception exception) {
            if (running) {
                LetterSignalPhone.LOGGER.warn("Voice playback stopped", exception);
            }
        } finally {
            if (speaker != null) {
                speaker.drain();
                speaker.stop();
                speaker.close();
            }
        }
    }

    private static InetAddress resolveServerHost(Minecraft minecraft) throws IOException {
        ServerData server = minecraft.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            return InetAddress.getByName("127.0.0.1");
        }

        String host = server.ip;
        int portSeparator = host.lastIndexOf(':');
        if (portSeparator > 0 && host.indexOf(':') == portSeparator) {
            host = host.substring(0, portSeparator);
        }
        return InetAddress.getByName(host);
    }

    private static void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    private static void readFully(TargetDataLine microphone, byte[] pcm) {
        int offset = 0;
        while (running && offset < pcm.length) {
            offset += microphone.read(pcm, offset, pcm.length - offset);
        }
    }

    private static void applyVolume(byte[] pcm, int samples, float volume) {
        if (volume == 1.0F) {
            return;
        }
        int bytes = Math.min(pcm.length, samples * 2);
        for (int i = 0; i + 1 < bytes; i += 2) {
            int sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * volume)));
            pcm[i] = (byte) (scaled & 0xFF);
            pcm[i + 1] = (byte) ((scaled >> 8) & 0xFF);
        }
    }
}
