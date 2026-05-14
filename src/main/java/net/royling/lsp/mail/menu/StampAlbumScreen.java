package net.royling.lsp.mail.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.StampAlbumData;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.client.StampFoilRenderer;
import net.royling.lsp.mail.network.MailPayloads;
import net.royling.lsp.registry.ModItems;

public class StampAlbumScreen extends AbstractContainerScreen<StampAlbumMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/stamp_album.png");
    private static final int PREVIOUS_X = 8;
    private static final int PREVIOUS_Y = 143;
    private static final int NEXT_X = 200;
    private static final int NEXT_Y = 143;
    private static final int PAGE_BUTTON_SIZE = 7;
    private static final int STAMP_SIZE = 32;

    public StampAlbumScreen(StampAlbumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 214, 240);
        titleLabelX = 106;
        titleLabelY = 4;
        inventoryLabelX = 27;
        inventoryLabelY = 153;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        extractContents(gui, mouseX, mouseY, partialTick);
        extractAlbumStamps(gui);
        extractCarriedItem(gui, mouseX, mouseY);
        extractSnapbackItem(gui);
        extractTooltip(gui, mouseX, mouseY);
    }

    private void extractAlbumStamps(GuiGraphicsExtractor gui) {
        for (int i = 0; i < StampAlbumMenu.PAGE_SLOTS; i++) {
            ItemStack stack = menu.displayStack(i);
            if (!stack.is(ModItems.STAMP.get())) {
                continue;
            }
            int x = leftPos + StampAlbumMenu.stampX(i);
            int y = topPos + StampAlbumMenu.stampY(i);
            gui.blit(RenderPipelines.GUI_TEXTURED, StampData.guiTexture(stack), x, y, 0, 0, STAMP_SIZE, STAMP_SIZE, STAMP_SIZE, STAMP_SIZE);
            StampFoilRenderer.drawIfPresent(gui, stack, x, y, STAMP_SIZE);
        }
        Component page = Component.literal((menu.page() + 1) + "/" + StampAlbumData.PAGES);
        gui.text(font, page, leftPos + imageWidth / 2 - font.width(page) / 2, topPos + 143, 0xFF3D2B1F, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        gui.text(font, title, titleLabelX - font.width(title) / 2, titleLabelY, 0xFF3D2B1F, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (inside(mouseX, mouseY, leftPos + PREVIOUS_X, topPos + PREVIOUS_Y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE) && menu.page() > 0) {
            menu.previousPage();
            ClientPacketDistributor.sendToServer(new MailPayloads.StampAlbumPagePayload(menu.page()));
            return true;
        }
        if (inside(mouseX, mouseY, leftPos + NEXT_X, topPos + NEXT_Y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE) && menu.page() < StampAlbumData.PAGES - 1) {
            menu.nextPage();
            ClientPacketDistributor.sendToServer(new MailPayloads.StampAlbumPagePayload(menu.page()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
