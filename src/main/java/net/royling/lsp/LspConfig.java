package net.royling.lsp;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LspConfig {
    public static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.IntValue VOICE_UDP_PORT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("voice");
        VOICE_UDP_PORT = builder
                .comment("UDP Port Configuration: If you are using a server, you must expose the corresponding ports to enable the voice calling feature.")
                .comment("UDP端口设置，如果使用服务器，则需要公开对应的端口来使用电话通话功能")
                .defineInRange("udpPort", 24455, 1, 65535);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private LspConfig() {
    }

    public static int voiceUdpPort() {
        return VOICE_UDP_PORT.getAsInt();
    }
}
