package net.royling.lsp.phone.call;

import net.royling.lsp.phone.data.PhoneData;
import net.royling.lsp.phone.data.PhoneSavedData;
import net.royling.lsp.phone.voice.VoiceServer;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.phone.network.PhonePayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CallManager {
    private static final long STALE_NUMBER_TICKS = 7L * 24_000L;
    private static final Map<UUID, CallSession> SESSIONS = new HashMap<>();

    private CallManager() {
    }

    public static CallStatus statusFor(UUID playerId) {
        CallSession session = SESSIONS.get(playerId);
        if (session == null) {
            return CallStatus.IDLE;
        }
        if (session.connected) {
            return CallStatus.CONNECTED;
        }
        return session.callee.equals(playerId) ? CallStatus.INCOMING : CallStatus.CALLING;
    }

    public static String peerNameFor(ServerPlayer player) {
        CallSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return "";
        }

        UUID peerId = session.caller.equals(player.getUUID()) ? session.callee : session.caller;
        ServerPlayer peer = player.level().getServer().getPlayerList().getPlayer(peerId);
        return peer == null ? "" : peer.getScoreboardName();
    }

    public static void lookup(ServerPlayer requester, String number) {
        Optional<ServerPlayer> target = findHolder(requester.level().getServer(), number);
        if (target.isPresent()) {
            ServerPlayer player = target.get();
            PacketDistributor.sendToPlayer(requester, new PhonePayloads.LookupResultPayload(number, true, player.getUUID().toString(), player.getScoreboardName()));
        } else {
            PacketDistributor.sendToPlayer(requester, new PhonePayloads.LookupResultPayload(number, false, "", ""));
        }
    }

    public static void call(ServerPlayer caller, String number) {
        // 鍛煎彨鍓嶆牎楠屽弻鏂圭姸鎬佸拰鐢佃瘽鍗?token锛岄伩鍏嶆棫鍗℃垨浼€犲彿鐮佺户缁娇鐢ㄣ€?
        if (SESSIONS.containsKey(caller.getUUID())) {
            sendStatus(caller, statusFor(caller.getUUID()), peerNameFor(caller));
            return;
        }

        Optional<ServerPlayer> maybeTarget = findHolder(caller.level().getServer(), number);
        if (maybeTarget.isEmpty()) {
            caller.sendSystemMessage(Component.translatable("message.letter_signal_phone.call.unavailable"), true);
            sendStatus(caller, CallStatus.IDLE, "");
            return;
        }

        ServerPlayer target = maybeTarget.get();
        if (target.getUUID().equals(caller.getUUID()) || SESSIONS.containsKey(target.getUUID())) {
            caller.sendSystemMessage(Component.translatable("message.letter_signal_phone.call.busy"), true);
            sendStatus(caller, CallStatus.IDLE, "");
            return;
        }

        String callerNumber = findInstalledNumber(caller).orElse("");
        String callerToken = findInstalledToken(caller).orElse("");
        String calleeToken = findInstalledToken(target).orElse("");
        if (callerNumber.isEmpty() || callerToken.isEmpty() || calleeToken.isEmpty()) {
            caller.sendSystemMessage(Component.translatable("message.letter_signal_phone.phone.no_card"), true);
            sendStatus(caller, CallStatus.IDLE, "");
            return;
        }

        CallSession session = new CallSession(caller.getUUID(), target.getUUID(), callerNumber, callerToken, number, calleeToken);
        SESSIONS.put(caller.getUUID(), session);
        SESSIONS.put(target.getUUID(), session);
        sendStatus(caller, CallStatus.CALLING, target.getScoreboardName());
        PacketDistributor.sendToPlayer(target, new PhonePayloads.IncomingCallPayload(caller.getUUID().toString(), caller.getScoreboardName(), callerNumber));
        sendStatus(target, CallStatus.INCOMING, caller.getScoreboardName());
    }

    public static void accept(ServerPlayer player) {
        CallSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.callee.equals(player.getUUID())) {
            sendStatus(player, CallStatus.IDLE, "");
            return;
        }

        ServerPlayer caller = player.level().getServer().getPlayerList().getPlayer(session.caller);
        if (caller == null) {
            clear(session);
            sendStatus(player, CallStatus.IDLE, "");
            return;
        }

        session.connected = true;
        int voicePort = VoiceServer.port();
        PacketDistributor.sendToPlayer(caller, new PhonePayloads.VoiceConfigPayload(voicePort));
        PacketDistributor.sendToPlayer(player, new PhonePayloads.VoiceConfigPayload(voicePort));
        sendStatus(caller, CallStatus.CONNECTED, player.getScoreboardName());
        sendStatus(player, CallStatus.CONNECTED, caller.getScoreboardName());
    }

    public static void hangup(ServerPlayer player) {
        CallSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            sendStatus(player, CallStatus.IDLE, "");
            return;
        }

        ServerPlayer caller = player.level().getServer().getPlayerList().getPlayer(session.caller);
        ServerPlayer callee = player.level().getServer().getPlayerList().getPlayer(session.callee);
        clear(session);
        if (caller != null) {
            sendStatus(caller, CallStatus.IDLE, "");
        }
        if (callee != null) {
            sendStatus(callee, CallStatus.IDLE, "");
        }
    }

    public static void tick(MinecraftServer server) {
        VoiceServer.ensureStarted();
        refreshNumberLeases(server);

        for (CallSession session : SESSIONS.values().stream().distinct().toList()) {
            ServerPlayer caller = server.getPlayerList().getPlayer(session.caller);
            ServerPlayer callee = server.getPlayerList().getPlayer(session.callee);
            if (caller == null || callee == null || !hasInstalledNumber(server, caller, session.callerNumber, session.callerToken) || !hasInstalledNumber(server, callee, session.calleeNumber, session.calleeToken)) {
                clear(session);
                if (caller != null) {
                    sendStatus(caller, CallStatus.IDLE, "");
                }
                if (callee != null) {
                    sendStatus(callee, CallStatus.IDLE, "");
                }
            }
        }
    }

    public static Optional<String> findInstalledNumber(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.PHONE.get()) && PhoneData.hasInstalledCard(stack)) {
                return Optional.of(PhoneData.getInstalledNumber(stack));
            }
        }
        return Optional.empty();
    }

    public static Optional<UUID> connectedPeer(UUID playerId) {
        CallSession session = SESSIONS.get(playerId);
        if (session == null || !session.connected) {
            return Optional.empty();
        }
        return Optional.of(session.caller.equals(playerId) ? session.callee : session.caller);
    }

    private static Optional<String> findInstalledToken(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.PHONE.get()) && PhoneData.hasInstalledCard(stack)) {
                return Optional.of(PhoneData.getInstalledToken(stack));
            }
        }
        return Optional.empty();
    }

    private static boolean hasInstalledNumber(MinecraftServer server, ServerPlayer player, String number, String token) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.PHONE.get()) && number.equals(PhoneData.getInstalledNumber(stack)) && token.equals(PhoneData.getInstalledToken(stack)) && PhoneSavedData.get(server).matches(number, token)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<ServerPlayer> findHolder(MinecraftServer server, String number) {
        PhoneSavedData data = PhoneSavedData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(ModItems.PHONE.get()) && number.equals(PhoneData.getInstalledNumber(stack)) && data.matches(number, PhoneData.getInstalledToken(stack))) {
                    return Optional.of(player);
                }
            }
        }
        return Optional.empty();
    }

    private static void refreshNumberLeases(MinecraftServer server) {
        // 鍦ㄧ嚎鐜╁鎸佹湁鐨勭數璇?鐢佃瘽鍗′細鍒锋柊绉熺害锛岄暱鏈熸棤浜烘寔鏈夌殑鍙风爜浼氳鍥炴敹銆?
        PhoneSavedData data = PhoneSavedData.get(server);
        long now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(ModItems.PHONE.get()) && PhoneData.hasInstalledCard(stack)) {
                    data.touchIfCurrent(PhoneData.getInstalledNumber(stack), PhoneData.getInstalledToken(stack), now);
                } else if (stack.is(ModItems.PHONE_CARD.get()) && PhoneData.hasCardNumber(stack)) {
                    data.touchIfCurrent(PhoneData.getCardNumber(stack), PhoneData.getCardToken(stack), now);
                }
            }
        }
        data.reclaimStale(now, STALE_NUMBER_TICKS);
    }

    private static void sendStatus(ServerPlayer player, CallStatus status, String peerName) {
        PacketDistributor.sendToPlayer(player, new PhonePayloads.CallStatusPayload(status, peerName));
    }

    private static void clear(CallSession session) {
        SESSIONS.remove(session.caller);
        SESSIONS.remove(session.callee);
    }

    private static class CallSession {
        final UUID caller;
        final UUID callee;
        final String callerNumber;
        final String calleeNumber;
        final String callerToken;
        final String calleeToken;
        boolean connected;

        CallSession(UUID caller, UUID callee, String callerNumber, String callerToken, String calleeNumber, String calleeToken) {
            this.caller = caller;
            this.callee = callee;
            this.callerNumber = callerNumber;
            this.calleeNumber = calleeNumber;
            this.callerToken = callerToken;
            this.calleeToken = calleeToken;
        }
    }
}
