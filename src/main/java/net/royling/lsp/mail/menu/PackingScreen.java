package net.royling.lsp.mail.menu;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.network.MailPayloads;

public class PackingScreen extends AbstractContainerScreen<PackingMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/packing.png");
    private static final int BUTTON_X = 73;
    private static final int BUTTON_Y = 28;
    private static final int BUTTON_WIDTH = 31;
    private static final int BUTTON_HEIGHT = 16;

    public PackingScreen(PackingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 172);
        inventoryLabelY = 78;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        gui.text(font, title, titleLabelX, titleLabelY, 0xFF3D2B1F, false);
        gui.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF3D2B1F, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inside((int) event.x(), (int) event.y(), leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            ClientPacketDistributor.sendToServer(new MailPayloads.PackPayload());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
