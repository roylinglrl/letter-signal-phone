package net.royling.lsp.telegraph.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.telegraph.TelegraphBlockEntity;
import net.royling.lsp.telegraph.TelegraphData;
import net.royling.lsp.telegraph.network.TelegraphPayloads;

public class TelegraphScreen extends AbstractContainerScreen<TelegraphMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/telegraph_machine.png");
    private static final int GUI_WIDTH = 194;
    private static final int GUI_HEIGHT = 209;
    private static final int DISPLAY_X = 66;
    private static final int DISPLAY_Y = 28;
    private static final int DISPLAY_W = 61;
    private static final int DISPLAY_H = 12;
    private static final int STATUS_X = 66;
    private static final int STATUS_Y = 96;
    private static final int STATUS_W = 61;
    private static final int CONFIRM_X = 15;
    private static final int CONFIRM_Y = 104;
    private static final int CONFIRM_W = 23;
    private static final int CONFIRM_H = 6;
    private static final int CONFIRM_OVERLAY_U = 195;
    private static final int CONFIRM_OVERLAY_V = 0;
    private static final int CONFIRM_OVERLAY_W = 25;
    private static final int CONFIRM_OVERLAY_H = 8;
    private static final int BUTTON_X = 71;
    private static final int BUTTON_Y = 78;
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_GAP = 3;
    private static final int BUTTON_OVERLAY_U = 195;
    private static final int BUTTON_OVERLAY_V = 9;
    private static final int FEEDBACK_TICKS = 8;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final String DOT = "\u00b7";
    private String draft = "";
    private int displayIndex;
    private Component status = Component.empty();
    private int statusTicks;
    private int pressedButton = -1;
    private int pressedTicks;
    private int confirmTicks;

    public TelegraphScreen(TelegraphMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        titleLabelX = 999;
        inventoryLabelY = 999;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (statusTicks > 0) {
            statusTicks--;
        }
        if (pressedTicks > 0) {
            pressedTicks--;
        } else {
            pressedButton = -1;
        }
        if (confirmTicks > 0) {
            confirmTicks--;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        drawTitle(gui);
        drawDisplay(gui);
        drawStatus(gui);
        drawPressedOverlays(gui);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();
        if (inside(x, y, leftPos + CONFIRM_X, topPos + CONFIRM_Y, CONFIRM_W, CONFIRM_H)) {
            confirmTicks = FEEDBACK_TICKS;
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.frequency"));
            ClientPacketDistributor.sendToServer(new TelegraphPayloads.TelegraphButtonPayload(TelegraphPayloads.TelegraphAction.CONFIRM));
            return true;
        }
        if (inside(x, y, leftPos + DISPLAY_X, topPos + DISPLAY_Y, DISPLAY_W, DISPLAY_H)) {
            cycleDisplay();
            return true;
        }
        for (int i = 0; i < 5; i++) {
            int bx = leftPos + BUTTON_X + i * (BUTTON_SIZE + BUTTON_GAP);
            if (inside(x, y, bx, topPos + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                press(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void press(int index) {
        TelegraphPayloads.TelegraphAction action = switch (index) {
            case 0 -> TelegraphPayloads.TelegraphAction.DOT;
            case 1 -> TelegraphPayloads.TelegraphAction.DASH;
            case 2 -> TelegraphPayloads.TelegraphAction.SPACE;
            case 3 -> TelegraphPayloads.TelegraphAction.START;
            default -> TelegraphPayloads.TelegraphAction.END;
        };
        pressedButton = index;
        pressedTicks = FEEDBACK_TICKS;
        updateLocalDraftAndStatus(action);
        ClientPacketDistributor.sendToServer(new TelegraphPayloads.TelegraphButtonPayload(action));
    }

    private void updateLocalDraftAndStatus(TelegraphPayloads.TelegraphAction action) {
        if (action == TelegraphPayloads.TelegraphAction.START) {
            if (!canStart()) {
                setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.cannot_start"));
                return;
            }
            draft = "";
            displayIndex = 0;
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.start"));
            return;
        }
        if (action == TelegraphPayloads.TelegraphAction.DOT) {
            draft += DOT;
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.dot"));
        } else if (action == TelegraphPayloads.TelegraphAction.DASH) {
            draft += "-";
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.dash"));
        } else if (action == TelegraphPayloads.TelegraphAction.SPACE) {
            if (!draft.isEmpty() && !draft.endsWith(",")) {
                draft += ",";
                displayIndex = parts().length - 1;
            }
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.space"));
        } else if (action == TelegraphPayloads.TelegraphAction.END) {
            setStatus(Component.translatable("screen.letter_signal_phone.telegraph.status.end"));
        }
    }

    private boolean canStart() {
        return !menu.isComposing()
                && !menu.getSlot(TelegraphBlockEntity.SLOT_WORK).hasItem()
                && menu.getSlot(TelegraphBlockEntity.SLOT_SEND_INPUT).getItem().is(Items.PAPER);
    }

    private void drawTitle(GuiGraphicsExtractor gui) {
        gui.text(font, title, leftPos + 96 - font.width(title) / 2, topPos + 5, TEXT_COLOR, false);
    }

    private void drawDisplay(GuiGraphicsExtractor gui) {
        drawLine(gui, currentPart(), DISPLAY_X, DISPLAY_Y, DISPLAY_W);
    }

    private void drawStatus(GuiGraphicsExtractor gui) {
        if (statusTicks <= 0) {
            return;
        }
        drawLine(gui, status.getString(), STATUS_X, STATUS_Y, STATUS_W);
    }

    private void drawLine(GuiGraphicsExtractor gui, String text, int x, int y, int width) {
        String line = text;
        if (font.width(line) > width - 4) {
            while (!line.isEmpty() && font.width(line + "...") > width - 4) {
                line = line.substring(1);
            }
            line = "..." + line;
        }
        gui.text(font, Component.literal(line), leftPos + x + 2, topPos + y + 2, TEXT_COLOR, false);
    }

    private void drawPressedOverlays(GuiGraphicsExtractor gui) {
        if (confirmTicks > 0) {
            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + CONFIRM_X - 1, topPos + CONFIRM_Y - 1, CONFIRM_OVERLAY_U, CONFIRM_OVERLAY_V, CONFIRM_OVERLAY_W, CONFIRM_OVERLAY_H, 256, 256);
        }
        if (pressedButton >= 0) {
            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + BUTTON_X + pressedButton * (BUTTON_SIZE + BUTTON_GAP), topPos + BUTTON_Y, BUTTON_OVERLAY_U, BUTTON_OVERLAY_V + pressedButton * BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, 256, 256);
        }
    }

    private void cycleDisplay() {
        String[] parts = parts();
        if (parts.length > 0) {
            displayIndex = (displayIndex + 1) % parts.length;
        }
    }

    private String currentPart() {
        String[] parts = parts();
        return parts.length == 0 ? "" : parts[Math.min(displayIndex, parts.length - 1)];
    }

    private String[] parts() {
        String value = currentDraft();
        if (value.isEmpty()) {
            return new String[0];
        }
        return value.split(",", -1);
    }

    private String currentDraft() {
        if (!draft.isEmpty()) {
            return draft;
        }
        return TelegraphData.getMessage(menu.getSlot(TelegraphBlockEntity.SLOT_WORK).getItem());
    }

    private void setStatus(Component status) {
        this.status = status;
        statusTicks = 40;
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
