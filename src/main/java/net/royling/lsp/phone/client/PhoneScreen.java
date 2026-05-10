package net.royling.lsp.phone.client;




import net.royling.lsp.phone.call.CallStatus;
import net.royling.lsp.phone.network.PhonePayloads;
import net.royling.lsp.LetterSignalPhone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class PhoneScreen extends Screen {
    private static final Identifier PHONE_TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/phone.png");
    private static final int PHONE_WIDTH = 97;
    private static final int PHONE_HEIGHT = 183;
    private static final int SCREEN_X = 3;
    private static final int SCREEN_Y = 14;
    private static final int SCREEN_WIDTH = 91;
    private static final int SCREEN_HEIGHT = 62;
    private static final int GREEN_X = 3;
    private static final int PURPLE_X = 34;
    private static final int RED_X = 65;
    private static final int BUTTON_Y = 84;
    private static final int BUTTON_SIZE = 29;
    private static final int SETTINGS_X = 65;
    private static final int SETTINGS_Y = 146;
    private static final int SETTINGS_WIDTH = 28;
    private static final int SETTINGS_HEIGHT = 29;

    private enum Page {
        HOME,
        DIAL
    }

    private final String ownNumber;
    private Page page = Page.HOME;
    private String dialedNumber = "";
    private boolean lookupFound;
    private String targetUuid = "";
    private String targetName = "";
    private CallStatus callStatus;
    private String peerName;

    public PhoneScreen(String ownNumber, CallStatus callStatus, String peerName) {
        super(Component.translatable("screen.letter_signal_phone.phone"));
        this.ownNumber = ownNumber;
        this.callStatus = callStatus;
        this.peerName = peerName;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }

        int panelX = panelX();
        int panelY = panelY();
        int localX = (int) event.x() - panelX;
        int localY = (int) event.y() - panelY;

        if (inside(localX, localY, SETTINGS_X, SETTINGS_Y, SETTINGS_WIDTH, SETTINGS_HEIGHT)) {
            Minecraft.getInstance().setScreen(new PhoneSettingsScreen(this));
            return true;
        }

        if (inside(localX, localY, RED_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
            ClientPacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(PhonePayloads.Action.HANGUP, ""));
            return true;
        }

        if (inside(localX, localY, GREEN_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
            if (callStatus == CallStatus.INCOMING) {
                ClientPacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(PhonePayloads.Action.ACCEPT, ""));
            } else if (callStatus == CallStatus.IDLE && page == Page.DIAL && dialedNumber.length() == 4 && lookupFound) {
                ClientPacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(PhonePayloads.Action.CALL, dialedNumber));
            }
            return true;
        }

        if (inside(localX, localY, PURPLE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
            if (callStatus == CallStatus.IDLE) {
                page = Page.DIAL;
            }
            return true;
        }

        if (callStatus == CallStatus.IDLE && page == Page.DIAL && inside(localX, localY, SCREEN_X, SCREEN_Y, SCREEN_WIDTH, SCREEN_HEIGHT)) {
            handleDialScreenClick(localX - SCREEN_X, localY - SCREEN_Y);
            return true;
        }

        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        int x = panelX();
        int y = panelY();

        gui.fill(0, 0, width, height, 0x66000000);
        gui.blit(RenderPipelines.GUI_TEXTURED, PHONE_TEXTURE, x, y, 0, 0, PHONE_WIDTH, PHONE_HEIGHT, 256, 256);
        drawSettingsButton(gui, x, y);

        if (ownNumber.isEmpty()) {
            screenText(gui, Component.translatable("screen.letter_signal_phone.no_card"), 24, 22);
            screenText(gui, Component.literal("ESC"), 35, 42);
            return;
        }

        if (callStatus != CallStatus.IDLE) {
            drawActiveCall(gui, x, y);
        } else if (page == Page.HOME) {
            drawHome(gui, x, y);
        } else {
            drawDial(gui, x, y);
        }
    }

    public void handleLookupResult(PhonePayloads.LookupResultPayload payload) {
        if (!payload.number().equals(dialedNumber)) {
            return;
        }
        lookupFound = payload.found();
        targetUuid = payload.targetUuid();
        targetName = payload.targetName();
    }

    public void handleIncomingCall(PhonePayloads.IncomingCallPayload payload) {
        callStatus = CallStatus.INCOMING;
        peerName = payload.callerName();
    }

    public void handleCallStatus(CallStatus status, String peerName) {
        callStatus = status;
        this.peerName = peerName;
        if (status == CallStatus.IDLE) {
            clearLookup();
            page = Page.HOME;
        }
    }

    private void drawHome(GuiGraphicsExtractor gui, int x, int y) {
        screenText(gui, Component.translatable("screen.letter_signal_phone.phone"), 8, 18);
        screenText(gui, Component.literal(ownNumber), 34, 32);
        screenText(gui, Component.translatable("screen.letter_signal_phone.status.idle"), 8, 48);
    }

    private void drawDial(GuiGraphicsExtractor gui, int x, int y) {
        centeredScreenText(gui, Component.literal(dialedNumber.isEmpty() ? "----" : padNumber()), 3);

        if (lookupFound) {
            centeredScreenText(gui, Component.literal(targetName), 14);
        } else if (dialedNumber.length() == 4) {
            centeredScreenText(gui, Component.translatable("screen.letter_signal_phone.number_unavailable"), 14);
        }

        drawKeypad(gui, x + SCREEN_X, y + SCREEN_Y + 28);
    }

    private void drawActiveCall(GuiGraphicsExtractor gui, int x, int y) {
        Component title = switch (callStatus) {
            case CALLING -> Component.translatable("screen.letter_signal_phone.call_page.calling");
            case INCOMING -> Component.translatable("screen.letter_signal_phone.call_page.incoming");
            case CONNECTED -> Component.translatable("screen.letter_signal_phone.call_page.connected");
            case IDLE -> Component.empty();
        };

        centeredScreenText(gui, title, 18);
        if (!peerName.isEmpty()) {
            centeredScreenText(gui, Component.literal(peerName), 34);
        }
    }

    private void drawSettingsButton(GuiGraphicsExtractor gui, int x, int y) {
        gui.text(font, Component.translatable("screen.letter_signal_phone.phone.settings_button"), x + SETTINGS_X + 4, y + SETTINGS_Y + 10, 0xFF000000, false);
    }

    private void drawKeypad(GuiGraphicsExtractor gui, int x, int y) {
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "OK"};
        for (int i = 0; i < labels.length; i++) {
            int col = i % 3;
            int row = i / 3;
            gui.text(font, Component.literal(labels[i]), x + 12 + col * 30, y + row * 8, 0xFF000000, false);
        }
    }

    private void drawTarget(GuiGraphicsExtractor gui, int x, int y) {
        Identifier skin = skinForTarget();
        if (skin != null) {
            gui.blit(RenderPipelines.GUI_TEXTURED, skin, x + 26, y, 8, 8, 24, 24, 64, 64);
            gui.blit(RenderPipelines.GUI_TEXTURED, skin, x + 26, y, 40, 8, 24, 24, 64, 64);
            return;
        }
        gui.fill(x + 26, y, x + 50, y + 24, 0xFF46515F);
        gui.centeredText(font, targetName.isEmpty() ? "?" : targetName.substring(0, 1), x + 38, y + 8, 0xFFFFFFFF);
    }

    private Identifier skinForTarget() {
        if (targetUuid.isEmpty()) {
            return null;
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() == null) {
                return null;
            }
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(UUID.fromString(targetUuid));
            return info == null ? null : info.getSkin().body().texturePath();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String padNumber() {
        return dialedNumber + "-".repeat(4 - dialedNumber.length());
    }

    private void handleDialScreenClick(int screenLocalX, int screenLocalY) {
        int keypadY = 28;
        if (screenLocalY < keypadY) {
            return;
        }

        int col = screenLocalX / 30;
        int row = (screenLocalY - keypadY) / 8;
        if (col < 0 || col > 2 || row < 0 || row > 3) {
            return;
        }

        int index = row * 3 + col;
        if (index == 9) {
            if (!dialedNumber.isEmpty()) {
                dialedNumber = dialedNumber.substring(0, dialedNumber.length() - 1);
                clearLookup();
            }
            return;
        }
        if (index == 11) {
            if (dialedNumber.length() == 4 && lookupFound) {
                ClientPacketDistributor.sendToServer(new PhonePayloads.PhoneActionPayload(PhonePayloads.Action.CALL, dialedNumber));
            }
            return;
        }

        int digit = index == 10 ? 0 : index + 1;
        if (dialedNumber.length() < 4) {
            dialedNumber += digit;
            clearLookup();
            if (dialedNumber.length() == 4) {
                ClientPacketDistributor.sendToServer(new PhonePayloads.LookupNumberPayload(dialedNumber));
            }
        }
    }

    private void clearLookup() {
        lookupFound = false;
        targetUuid = "";
        targetName = "";
    }

    private void screenText(GuiGraphicsExtractor gui, Component text, int localX, int localY) {
        gui.text(font, text, panelX() + SCREEN_X + localX, panelY() + SCREEN_Y + localY, 0xFF000000, false);
    }

    private void centeredScreenText(GuiGraphicsExtractor gui, Component text, int localY) {
        int textWidth = font.width(text);
        int x = panelX() + SCREEN_X + (SCREEN_WIDTH - textWidth) / 2;
        gui.text(font, text, x, panelY() + SCREEN_Y + localY, 0xFF000000, false);
    }

    private int panelX() {
        return width - PHONE_WIDTH - 18;
    }

    private int panelY() {
        return (height - PHONE_HEIGHT) / 2;
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
