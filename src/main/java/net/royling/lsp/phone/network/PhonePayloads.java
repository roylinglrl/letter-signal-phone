package net.royling.lsp.phone.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.phone.call.CallManager;
import net.royling.lsp.phone.call.CallStatus;
import net.royling.lsp.phone.client.PhoneClientHooks;

public final class PhonePayloads {
    private PhonePayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registerClientHandlers(registrar);
        } else {
            registrar.playToClient(OpenPhoneScreenPayload.TYPE, OpenPhoneScreenPayload.STREAM_CODEC);
            registrar.playToClient(LookupResultPayload.TYPE, LookupResultPayload.STREAM_CODEC);
            registrar.playToClient(IncomingCallPayload.TYPE, IncomingCallPayload.STREAM_CODEC);
            registrar.playToClient(CallStatusPayload.TYPE, CallStatusPayload.STREAM_CODEC);
            registrar.playToClient(VoiceConfigPayload.TYPE, VoiceConfigPayload.STREAM_CODEC);
        }
        registrar.playToServer(LookupNumberPayload.TYPE, LookupNumberPayload.STREAM_CODEC, PhonePayloads::handleLookup);
        registrar.playToServer(PhoneActionPayload.TYPE, PhoneActionPayload.STREAM_CODEC, PhonePayloads::handleAction);
    }

    private static void registerClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(OpenPhoneScreenPayload.TYPE, OpenPhoneScreenPayload.STREAM_CODEC, PhoneClientHooks::openPhoneScreen);
        registrar.playToClient(LookupResultPayload.TYPE, LookupResultPayload.STREAM_CODEC, PhoneClientHooks::lookupResult);
        registrar.playToClient(IncomingCallPayload.TYPE, IncomingCallPayload.STREAM_CODEC, PhoneClientHooks::incomingCall);
        registrar.playToClient(CallStatusPayload.TYPE, CallStatusPayload.STREAM_CODEC, PhoneClientHooks::callStatus);
        registrar.playToClient(VoiceConfigPayload.TYPE, VoiceConfigPayload.STREAM_CODEC, PhoneClientHooks::voiceConfig);
    }

    private static void handleLookup(LookupNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && payload.number().matches("\\d{4}")) {
                CallManager.lookup(player, payload.number());
            }
        });
    }

    private static void handleAction(PhoneActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case CALL -> {
                    if (payload.number().matches("\\d{4}")) {
                        CallManager.call(player, payload.number());
                    }
                }
                case ACCEPT -> CallManager.accept(player);
                case HANGUP -> CallManager.hangup(player);
            }
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, path);
    }

    public record OpenPhoneScreenPayload(String number, CallStatus status, String peerName) implements CustomPacketPayload {
        public static final Type<OpenPhoneScreenPayload> TYPE = new Type<>(id("open_phone_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPhoneScreenPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.number(), 8);
                    buf.writeUtf(payload.status().name(), 16);
                    buf.writeUtf(payload.peerName(), 64);
                },
                buf -> new OpenPhoneScreenPayload(buf.readUtf(8), CallStatus.valueOf(buf.readUtf(16)), buf.readUtf(64))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LookupNumberPayload(String number) implements CustomPacketPayload {
        public static final Type<LookupNumberPayload> TYPE = new Type<>(id("lookup_number"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LookupNumberPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.number(), 4),
                buf -> new LookupNumberPayload(buf.readUtf(4))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LookupResultPayload(String number, boolean found, String targetUuid, String targetName) implements CustomPacketPayload {
        public static final Type<LookupResultPayload> TYPE = new Type<>(id("lookup_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LookupResultPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.number(), 4);
                    buf.writeBoolean(payload.found());
                    buf.writeUtf(payload.targetUuid(), 40);
                    buf.writeUtf(payload.targetName(), 64);
                },
                buf -> new LookupResultPayload(buf.readUtf(4), buf.readBoolean(), buf.readUtf(40), buf.readUtf(64))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record IncomingCallPayload(String callerUuid, String callerName, String callerNumber) implements CustomPacketPayload {
        public static final Type<IncomingCallPayload> TYPE = new Type<>(id("incoming_call"));
        public static final StreamCodec<RegistryFriendlyByteBuf, IncomingCallPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.callerUuid(), 40);
                    buf.writeUtf(payload.callerName(), 64);
                    buf.writeUtf(payload.callerNumber(), 4);
                },
                buf -> new IncomingCallPayload(buf.readUtf(40), buf.readUtf(64), buf.readUtf(4))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CallStatusPayload(CallStatus status, String peerName) implements CustomPacketPayload {
        public static final Type<CallStatusPayload> TYPE = new Type<>(id("call_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CallStatusPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.status().name(), 16);
                    buf.writeUtf(payload.peerName(), 64);
                },
                buf -> new CallStatusPayload(CallStatus.valueOf(buf.readUtf(16)), buf.readUtf(64))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record VoiceConfigPayload(int udpPort) implements CustomPacketPayload {
        public static final Type<VoiceConfigPayload> TYPE = new Type<>(id("voice_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VoiceConfigPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.udpPort()),
                buf -> new VoiceConfigPayload(buf.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PhoneActionPayload(Action action, String number) implements CustomPacketPayload {
        public static final Type<PhoneActionPayload> TYPE = new Type<>(id("phone_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PhoneActionPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.action().name(), 16);
                    buf.writeUtf(payload.number(), 4);
                },
                buf -> new PhoneActionPayload(Action.valueOf(buf.readUtf(16)), buf.readUtf(4))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum Action {
        CALL,
        ACCEPT,
        HANGUP
    }

}