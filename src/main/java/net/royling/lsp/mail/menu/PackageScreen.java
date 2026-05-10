package net.royling.lsp.mail.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.mail.network.MailPayloads;

public class PackageScreen extends AbstractContainerScreen<PackageMenu> {
    public PackageScreen(PackageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 172);
        inventoryLabelY = 78;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFD5C1A1);
        gui.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + imageHeight - 6, 0xFFF8EDD8);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        drawButton(gui, leftPos + 63, topPos + 56, 50, 18, Component.translatable("screen.letter_signal_phone.package.unpack"));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        gui.text(font, title, titleLabelX, titleLabelY, 0xFF3D2B1F, false);
        gui.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3D2B1F, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inside((int) event.x(), (int) event.y(), leftPos + 63, topPos + 56, 50, 18)) {
            ClientPacketDistributor.sendToServer(new MailPayloads.UnpackPayload());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawButton(GuiGraphicsExtractor gui, int x, int y, int width, int height, Component text) {
        gui.fill(x, y, x + width, y + height, 0xFF775844);
        gui.centeredText(font, text, x + width / 2, y + 5, 0xFFFFFFFF);
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
