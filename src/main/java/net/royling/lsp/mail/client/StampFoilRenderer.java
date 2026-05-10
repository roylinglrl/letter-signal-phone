package net.royling.lsp.mail.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.registry.ModItems;

public final class StampFoilRenderer {
    private static final FoilTexture HOLOGRAPHIC = new FoilTexture(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/overlay/rainbow_color_foil.png"),
            630,
            1.0F,
            35L,
            55L,
            72L,
            42L
    );
    private static final FoilTexture COLOR_CRYSTAL = new FoilTexture(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/overlay/crystal_foil.png"),
            496,
            2.25F,
            95L,
            130L,
            160L,
            115L
    );
    private static final FoilTexture DIAMOND = new FoilTexture(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/overlay/diamond_foil.png"),
            405,
            2.8F,
            130L,
            170L,
            220L,
            150L
    );

    private StampFoilRenderer() {
    }

    public static void drawIfPresent(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int size) {
        FoilTexture texture = textureFor(stack);
        if (texture != null) {
            draw(graphics, x, y, size, texture);
        }
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int size) {
        draw(graphics, x, y, size, HOLOGRAPHIC);
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int size, String foilEffect) {
        FoilTexture texture = textureFor(foilEffect);
        if (texture != null) {
            draw(graphics, x, y, size, texture);
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, int x, int y, int size, FoilTexture texture) {
        if (texture.rgbShift()) {
            drawRgbShift(graphics, x, y, size);
            return;
        }
        long now = System.currentTimeMillis();
        int sampleSize = Math.max(size, Math.min(texture.size(), Math.round(size * texture.sampleScale())));
        int span = Math.max(1, texture.size() - sampleSize);
        float u = Math.floorMod((int) (now / texture.primaryUSpeed()), span);
        float v = Math.floorMod((int) (now / texture.primaryVSpeed()), span);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, u, v, size, size, sampleSize, sampleSize, texture.size(), texture.size());

        float reverseU = Math.floorMod(span - (int) (now / texture.secondaryUSpeed()), span);
        float reverseV = Math.floorMod((int) (now / texture.secondaryVSpeed()) + 157, span);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, reverseU, reverseV, size, size, sampleSize, sampleSize, texture.size(), texture.size());
    }

    private static FoilTexture textureFor(ItemStack stack) {
        if (!stack.is(ModItems.STAMP.get())) {
            return null;
        }
        return textureFor(StampData.foilEffect(stack));
    }

    private static FoilTexture textureFor(String foilEffect) {
        return switch (foilEffect) {
            case StampData.FOIL_HOLOGRAPHIC_STRIPES -> HOLOGRAPHIC;
            case StampData.FOIL_COLOR_CRYSTAL -> COLOR_CRYSTAL;
            case StampData.FOIL_DIAMOND -> DIAMOND;
            case StampData.FOIL_RGB_SHIFT -> RGB_SHIFT;
            default -> null;
        };
    }

    private static void drawRgbShift(GuiGraphicsExtractor graphics, int x, int y, int size) {
        float hue = (System.currentTimeMillis() % 4200L) / 4200.0F;
        int rgb = Mth.hsvToRgb(hue, 0.92F, 1.0F);
        int color = ARGB.color(86, rgb);
        graphics.fill(x, y, x + size, y + size, color);
    }

    private static final FoilTexture RGB_SHIFT = new FoilTexture(null, 1, 1.0F, 1L, 1L, 1L, 1L, true);

    private record FoilTexture(Identifier id, int size, float sampleScale, long primaryUSpeed, long primaryVSpeed, long secondaryUSpeed, long secondaryVSpeed, boolean rgbShift) {
        private FoilTexture(Identifier id, int size, float sampleScale, long primaryUSpeed, long primaryVSpeed, long secondaryUSpeed, long secondaryVSpeed) {
            this(id, size, sampleScale, primaryUSpeed, primaryVSpeed, secondaryUSpeed, secondaryVSpeed, false);
        }
    }
}
