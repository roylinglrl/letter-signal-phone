package net.royling.lsp.phone.voice;

import net.royling.lsp.phone.call.CallManager;
import net.royling.lsp.LspConfig;
import net.royling.lsp.LetterSignalPhone;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceServer {
    private static final int MAX_PACKET_SIZE = 1400;
    private static final Map<UUID, SocketAddress> ENDPOINTS = new ConcurrentHashMap<>();
    private static DatagramSocket socket;
    private static Thread thread;
    private static int activePort = -1;

    private VoiceServer() {
    }

    public static synchronized void ensureStarted() {
        int configuredPort = LspConfig.voiceUdpPort();
        if (socket != null && !socket.isClosed() && activePort == configuredPort) {
            return;
        }

        stop();

        try {
            socket = new DatagramSocket(configuredPort);
            activePort = configuredPort;
        } catch (IOException exception) {
            LetterSignalPhone.LOGGER.warn("Unable to start Letter,Signal,Phone voice UDP server on port {}", configuredPort, exception);
            return;
        }

        thread = new Thread(VoiceServer::receiveLoop, "LetterSignalPhone Voice UDP");
        thread.setDaemon(true);
        thread.start();
        LetterSignalPhone.LOGGER.info("Letter,Signal,Phone voice UDP server listening on port {}", activePort);
    }

    public static synchronized int port() {
        ensureStarted();
        return activePort > 0 ? activePort : LspConfig.voiceUdpPort();
    }

    private static synchronized void stop() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        ENDPOINTS.clear();
        activePort = -1;
    }

    private static void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (socket != null && !socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                relay(packet);
            } catch (IOException exception) {
                if (socket != null && !socket.isClosed()) {
                    LetterSignalPhone.LOGGER.warn("Voice UDP receive failed", exception);
                }
            }
        }
    }

    private static void relay(DatagramPacket packet) throws IOException {
        if (packet.getLength() <= 16) {
            return;
        }

        ByteBuffer header = ByteBuffer.wrap(packet.getData(), packet.getOffset(), 16);
        UUID sender = new UUID(header.getLong(), header.getLong());
        ENDPOINTS.put(sender, packet.getSocketAddress());

        Optional<UUID> peer = CallManager.connectedPeer(sender);
        if (peer.isEmpty()) {
            return;
        }

        SocketAddress peerAddress = ENDPOINTS.get(peer.get());
        if (peerAddress == null) {
            return;
        }

        byte[] copy = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), copy, 0, packet.getLength());
        socket.send(new DatagramPacket(copy, copy.length, peerAddress));
    }
}
