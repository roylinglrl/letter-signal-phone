package net.royling.lsp.phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PhoneSettingsScreen extends Screen {
    private static final Identifier WIDGETS_TEXTURE = Identifier.withDefaultNamespace("textures/gui/widgets.png");
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 166;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SMALL_BUTTON = 20;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private final Screen parent;

    public PhoneSettingsScreen(Screen parent) {
        super(Component.translatable("screen.letter_signal_phone.phone.settings"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }

        int panelX = panelX();
        int panelY = panelY();
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (inside(mouseX, mouseY, panelX + 92, panelY + 38, 168, BUTTON_HEIGHT)) {
            PhoneSettings.cycleMicrophone();
            return true;
        }
        if (handleVolumeClick(mouseX, mouseY, panelY + 72, () -> PhoneSettings.adjustInputVolume(-0.1F), () -> PhoneSettings.adjustInputVolume(0.1F))) {
            return true;
        }
        if (handleVolumeClick(mouseX, mouseY, panelY + 96, () -> PhoneSettings.adjustOutputVolume(-0.1F), () -> PhoneSettings.adjustOutputVolume(0.1F))) {
            return true;
        }
        if (handleVolumeClick(mouseX, mouseY, panelY + 120, () -> PhoneSettings.adjustRingtoneVolume(-0.1F), () -> PhoneSettings.adjustRingtoneVolume(0.1F))) {
            return true;
        }
        if (inside(mouseX, mouseY, panelX + 100, panelY + 142, 80, BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        int panelX = panelX();
        int panelY = panelY();

        gui.fill(0, 0, width, height, 0x99000000);

        gui.centeredText(font, title, width / 2, panelY + 12, LABEL_COLOR);
        drawMicrophoneRow(gui, panelX, panelY);
        drawVolumeRow(gui, panelX, panelY + 72, Component.translatable("screen.letter_signal_phone.phone.input_volume"), PhoneSettings.inputVolume());
        drawVolumeRow(gui, panelX, panelY + 96, Component.translatable("screen.letter_signal_phone.phone.output_volume"), PhoneSettings.outputVolume());
        drawVolumeRow(gui, panelX, panelY + 120, Component.translatable("screen.letter_signal_phone.phone.ringtone_volume"), PhoneSettings.ringtoneVolume());
        drawButton(gui, panelX + 100, panelY + 142, 80, BUTTON_HEIGHT, Component.translatable("gui.done"));
    }

    private void drawMicrophoneRow(GuiGraphicsExtractor gui, int panelX, int panelY) {
        gui.text(font, Component.translatable("screen.letter_signal_phone.phone.microphone"), panelX + 20, panelY + 44, LABEL_COLOR, false);
        Component label = PhoneSettings.usesDefaultMicrophone()
                ? Component.translatable("screen.letter_signal_phone.phone.microphone.default")
                : Component.literal(shorten(PhoneSettings.microphoneLabel(), 24));
        drawButton(gui, panelX + 92, panelY + 38, 168, BUTTON_HEIGHT, label);
    }

    private void drawVolumeRow(GuiGraphicsExtractor gui, int panelX, int y, Component label, float volume) {
        gui.text(font, label, panelX + 20, y + 6, LABEL_COLOR, false);
        drawButton(gui, panelX + 92, y, SMALL_BUTTON, BUTTON_HEIGHT, Component.literal("-"));
        gui.centeredText(font, Component.literal(String.format("%.0f%%", volume * 100.0F)), panelX + 162, y + 6, LABEL_COLOR);
        drawButton(gui, panelX + 220, y, SMALL_BUTTON, BUTTON_HEIGHT, Component.literal("+"));
    }

    private void drawButton(GuiGraphicsExtractor gui, int x, int y, int w, int h, Component label) {
        int leftWidth = Math.min(w / 2, 100);
        int rightWidth = w - leftWidth;
        gui.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, x, y, 0, 66, leftWidth, h, 256, 256);
        gui.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, x + leftWidth, y, 200 - rightWidth, 66, rightWidth, h, 256, 256);
        gui.text(font, label, x + (w - font.width(label)) / 2, y + 6, BUTTON_TEXT_COLOR, true);
    }

    private boolean handleVolumeClick(int mouseX, int mouseY, int y, Runnable minus, Runnable plus) {
        int panelX = panelX();
        if (inside(mouseX, mouseY, panelX + 92, y, SMALL_BUTTON, BUTTON_HEIGHT)) {
            minus.run();
            return true;
        }
        if (inside(mouseX, mouseY, panelX + 220, y, SMALL_BUTTON, BUTTON_HEIGHT)) {
            plus.run();
            return true;
        }
        return false;
    }

    private String shorten(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private int panelX() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
