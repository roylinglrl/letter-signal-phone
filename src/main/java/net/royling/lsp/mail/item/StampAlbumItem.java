package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.royling.lsp.mail.menu.StampAlbumMenu;

public class StampAlbumItem extends Item {
    public StampAlbumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new StampAlbumMenu(id, inventory, serverPlayer, hand),
                Component.translatable("screen.letter_signal_phone.stamp_album")
        ));
        return InteractionResult.SUCCESS_SERVER;
    }
}
