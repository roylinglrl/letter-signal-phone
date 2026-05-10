package net.royling.lsp.mail.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.tooltip.StampTooltipComponent;

public class ClientStampTooltipComponent implements ClientTooltipComponent {
    private static final int SIZE = 32;
    private final StampTooltipComponent component;

    public ClientStampTooltipComponent(StampTooltipComponent component) {
        this.component = component;
    }

    @Override
    public int getHeight(Font font) {
        return SIZE + 2;
    }

    @Override
    public int getWidth(Font font) {
        return SIZE;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, component.texture(), x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        StampFoilRenderer.draw(graphics, x, y, SIZE, component.foilEffect());
    }

    public static void drawHolographicStripes(GuiGraphicsExtractor graphics, int x, int y, int size) {
        StampFoilRenderer.draw(graphics, x, y, size, StampData.FOIL_HOLOGRAPHIC_STRIPES);
    }

    public static void drawFoil(GuiGraphicsExtractor graphics, int x, int y, int size, String foilEffect) {
        StampFoilRenderer.draw(graphics, x, y, size, foilEffect);
    }
}
