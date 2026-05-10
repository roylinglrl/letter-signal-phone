package net.royling.lsp.mail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.royling.lsp.mail.MailSavedData;

public final class MailCommands {
    private MailCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lsp")
                .then(Commands.literal("mail")
                        .then(Commands.literal("refresh")
                                .executes(context -> refreshMailbox(context.getSource())))
                        .then(Commands.literal("locate")
                                .executes(context -> locateOwnMailbox(context.getSource()))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> locateMailbox(context.getSource(), StringArgumentType.getString(context, "player")))))));
    }

    private static int refreshMailbox(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        boolean removed = MailSavedData.get(server).refreshMailboxBinding(server, player.getUUID());
        source.sendSuccess(() -> Component.translatable(removed
                ? "commands.letter_signal_phone.mail.refresh.removed"
                : "commands.letter_signal_phone.mail.refresh.synced"), false);
        return removed ? 1 : 0;
    }

    private static int locateOwnMailbox(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return locateMailbox(source, player.getScoreboardName());
    }

    private static int locateMailbox(CommandSourceStack source, String playerName) {
        return MailSavedData.get(source.getServer()).findMailboxByName(source.getServer(), playerName)
                .map(pos -> {
                    source.sendSuccess(() -> Component.translatable(
                            "commands.letter_signal_phone.mail.locate.found",
                            playerName,
                            locationComponent(source, pos)
                    ), false);
                    return 1;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.translatable("commands.letter_signal_phone.mail.locate.not_found", playerName));
                    return 0;
                });
    }

    private static Component locationComponent(CommandSourceStack source, BlockPos pos) {
        Component coords = Component.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]");
        if (!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return coords;
        }
        String command = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return coords.copy().withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.letter_signal_phone.mail.locate.teleport"))));
    }
}
