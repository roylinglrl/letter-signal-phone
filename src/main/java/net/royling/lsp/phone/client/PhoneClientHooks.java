package net.royling.lsp.phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.royling.lsp.phone.network.PhonePayloads;
import net.royling.lsp.phone.call.CallStatus;
import net.royling.lsp.phone.sound.RingtonePlayer;
import net.royling.lsp.phone.voice.VoiceClient;

public final class PhoneClientHooks {
    private static boolean ringing;
    private static String ringingCallerName = "";
    private static String ringingCallerNumber = "";
    private static int ringingTicks;

    private PhoneClientHooks() {
    }

    public static void openPhoneScreen(PhonePayloads.OpenPhoneScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.status() == CallStatus.INCOMING && !payload.peerName().isEmpty()) {
                startRinging(payload.peerName(), "");
            }
            Minecraft.getInstance().setScreen(new PhoneScreen(payload.number(), payload.status(), payload.peerName()));
        });
    }

    public static void lookupResult(PhonePayloads.LookupResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PhoneScreen screen) {
                screen.handleLookupResult(payload);
            }
        });
    }

    public static void incomingCall(PhonePayloads.IncomingCallPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable("message.letter_signal_phone.call.incoming", payload.callerName(), payload.callerNumber()));
            }
            startRinging(payload.callerName(), payload.callerNumber());
            if (minecraft.screen instanceof PhoneScreen screen) {
                screen.handleIncomingCall(payload);
            }
        });
    }

    public static void callStatus(PhonePayloads.CallStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.status() != CallStatus.INCOMING) {
                stopRinging();
            } else if (!payload.peerName().isEmpty()) {
                startRinging(payload.peerName(), ringingCallerNumber);
            }
            if (payload.status() == CallStatus.CONNECTED) {
                VoiceClient.start();
            } else {
                VoiceClient.stop();
            }
            if (Minecraft.getInstance().screen instanceof PhoneScreen screen) {
                screen.handleCallStatus(payload.status(), payload.peerName());
            }
        });
    }

    public static void voiceConfig(PhonePayloads.VoiceConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VoiceClient.configureServerPort(payload.udpPort()));
    }

    public static void clientTick() {
        if (!ringing) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            stopRinging();
            VoiceClient.stop();
            return;
        }

        ringingTicks++;
        minecraft.player.sendOverlayMessage(Component.translatable("message.letter_signal_phone.call.incoming", ringingCallerName, ringingCallerNumber));
        if (ringingTicks % 40 == 1) {
            RingtonePlayer.play();
        }
    }

    private static void startRinging(String callerName, String callerNumber) {
        ringing = true;
        ringingCallerName = callerName;
        ringingCallerNumber = callerNumber;
    }

    private static void stopRinging() {
        ringing = false;
        ringingCallerName = "";
        ringingCallerNumber = "";
        ringingTicks = 0;
        RingtonePlayer.stop();
    }
}
