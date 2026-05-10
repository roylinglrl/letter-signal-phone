package net.royling.lsp.mail.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.network.MailPayloads;

import java.util.ArrayList;
import java.util.List;

public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/letter_box.png");
    private static final int RECIPIENT_X = 12;
    private static final int RECIPIENT_Y = 52;
    private static final int RECIPIENT_WIDTH = 59;
    private static final int RECIPIENT_HEIGHT = 14;
    private static final int TITLE_X = 65;
    private static final int TITLE_Y = 5;
    private static final int TITLE_WIDTH = 64;
    private static final int SEND_X = 42;
    private static final int SEND_Y = 27;
    private static final int SEND_WIDTH = 48;
    private static final int SEND_HEIGHT = 16;
    private static final int SEND_PRESSED_U = 195;
    private static final int SEND_PRESSED_V = 0;
    private static final int CONFIRM_X = 74;
    private static final int CONFIRM_Y = 51;
    private static final int CONFIRM_SIZE = 16;
    private static final int CONFIRM_PRESSED_U = 195;
    private static final int CONFIRM_PRESSED_V = 16;
    private static final int STATUS_X = 12;
    private static final int STATUS_Y = 76;
    private static final int STATUS_WIDTH = 77;
    private static final int STATUS_HEIGHT = 32;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final long PRESS_FEEDBACK_MS = 120L;
    private EditBox recipient;
    private Component status = Component.translatable("screen.letter_signal_phone.mailbox.status.idle");
    private long sendPressedUntil;
    private long confirmPressedUntil;

    public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 209);
        inventoryLabelY = 121;
    }

    @Override
    protected void init() {
        super.init();
        recipient = new EditBox(font, leftPos + RECIPIENT_X, topPos + RECIPIENT_Y + 2, RECIPIENT_WIDTH, RECIPIENT_HEIGHT - 2, Component.translatable("screen.letter_signal_phone.mailbox.recipient"));
        recipient.setMaxLength(16);
        recipient.setBordered(false);
        recipient.setTextColor(TEXT_COLOR);
        addRenderableWidget(recipient);
    }

    public void setStatus(Component status) {
        this.status = status;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        long now = System.currentTimeMillis();
        if (now < sendPressedUntil) {
            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + SEND_X, topPos + SEND_Y, SEND_PRESSED_U, SEND_PRESSED_V, SEND_WIDTH, SEND_HEIGHT, 256, 256);
        }
        if (now < confirmPressedUntil) {
            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + CONFIRM_X, topPos + CONFIRM_Y, CONFIRM_PRESSED_U, CONFIRM_PRESSED_V, CONFIRM_SIZE, CONFIRM_SIZE, 256, 256);
        }
        drawCenteredText(gui, title, leftPos + TITLE_X + TITLE_WIDTH / 2, topPos + TITLE_Y, TEXT_COLOR);
        drawCenteredText(gui, Component.translatable("screen.letter_signal_phone.mailbox.send"), leftPos + SEND_X + SEND_WIDTH / 2, topPos + SEND_Y + 4, TEXT_COLOR);
        drawCenteredText(gui, Component.translatable("screen.letter_signal_phone.mailbox.confirm"), leftPos + CONFIRM_X + CONFIRM_SIZE / 2, topPos + CONFIRM_Y + 4, TEXT_COLOR);
        drawStatus(gui);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inside((int) event.x(), (int) event.y(), leftPos + SEND_X, topPos + SEND_Y, SEND_WIDTH, SEND_HEIGHT)) {
            sendPressedUntil = System.currentTimeMillis() + PRESS_FEEDBACK_MS;
            ClientPacketDistributor.sendToServer(new MailPayloads.MailboxSendPayload(recipient.getValue()));
            return true;
        }
        if (inside((int) event.x(), (int) event.y(), leftPos + CONFIRM_X, topPos + CONFIRM_Y, CONFIRM_SIZE, CONFIRM_SIZE)) {
            confirmPressedUntil = System.currentTimeMillis() + PRESS_FEEDBACK_MS;
            ClientPacketDistributor.sendToServer(new MailPayloads.MailboxCheckPayload(recipient.getValue()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (recipient.keyPressed(event)) {
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(event) && recipient.isFocused()) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return recipient.charTyped(event) || super.charTyped(event);
    }

    private void drawStatus(GuiGraphicsExtractor gui) {
        int y = topPos + STATUS_Y + 2;
        for (String line : wrap(status.getString(), STATUS_WIDTH - 4)) {
            if (y + 8 > topPos + STATUS_Y + STATUS_HEIGHT) {
                break;
            }
            gui.text(font, Component.literal(line), leftPos + STATUS_X + 2, y, TEXT_COLOR, false);
            y += 9;
        }
    }

    private List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String next = line.toString() + value.charAt(i);
            if (!line.isEmpty() && font.width(next) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(value.charAt(i));
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void drawCenteredText(GuiGraphicsExtractor gui, Component text, int centerX, int y, int color) {
        gui.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
