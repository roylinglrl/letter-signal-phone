package net.royling.lsp.mail.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.LetterData;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.client.ClientStampTooltipComponent;
import net.royling.lsp.mail.network.MailPayloads;
import net.royling.lsp.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LetterScreen extends AbstractContainerScreen<LetterMenu> {
    private static final Identifier WRITE_TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/letter.png");
    private static final Identifier READ_TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/gui/letter_read.png");
    private static final Identifier DEFAULT_STAMP_TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/stamp/stamp.png");
    private static final int TITLE_X = 65;
    private static final int TITLE_Y = 5;
    private static final int TITLE_WIDTH = 63;
    private static final int STAMP_X = 44;
    private static final int STAMP_Y = 27;
    private static final int READ_STAMP_X = 15;
    private static final int READ_STAMP_Y = 16;
    private static final int READ_STAMP_SIZE = 32;
    private static final int READ_SIGNER_X = 55;
    private static final int READ_SIGNER_Y = 25;
    private static final int READ_SIGNER_WIDTH = 65;
    private static final int READ_TEXT_X = 13;
    private static final int READ_TEXT_Y = 54;
    private static final int READ_TEXT_WIDTH = 168;
    private static final int READ_TEXT_HEIGHT = 133;
    private static final int SAVE_X = 13;
    private static final int SAVE_Y = 50;
    private static final int SEAL_X = 13;
    private static final int SEAL_Y = 73;
    private static final int DELETE_X = 13;
    private static final int DELETE_Y = 96;
    private static final int BUTTON_WIDTH = 47;
    private static final int BUTTON_HEIGHT = 15;
    private static final int BUTTON_PRESSED_U = 195;
    private static final int BUTTON_PRESSED_V = 0;
    private static final int WRITE_X = 68;
    private static final int WRITE_Y = 27;
    private static final int WRITE_WIDTH = 115;
    private static final int WRITE_HEIGHT = 80;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BUTTON_TEXT_COLOR = 0xFF3D2B1F;
    private static final long PRESS_FEEDBACK_MS = 120L;
    private MultiLineEditBox textBox;
    private final Inventory playerInventory;
    private boolean readOnly;
    private String text = "";
    private String signer = "";
    private String stamp = "";
    private String signerUuid = "";
    private String stampGuiTexture = "";
    private String stampFoilEffect = StampData.FOIL_NONE;
    private long savePressedUntil;
    private long sealPressedUntil;
    private long deletePressedUntil;

    public LetterScreen(LetterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 209);
        this.playerInventory = inventory;
        inventoryLabelY = 121;
    }

    @Override
    protected void init() {
        super.init();
        textBox = MultiLineEditBox.builder()
                .setX(leftPos + WRITE_X)
                .setY(topPos + WRITE_Y)
                .setShowBackground(false)
                .build(font, WRITE_WIDTH, WRITE_HEIGHT, Component.translatable("screen.letter_signal_phone.letter"));
        textBox.setCharacterLimit(512);
        ItemStack held = playerInventory.player.getMainHandItem();
        if (held.is(ModItems.LETTER.get())) {
            loadLetter(LetterData.getText(held), LetterData.isReadOnly(held), LetterData.getSigner(held), LetterData.getStamp(held), LetterData.getSignerUuid(held), LetterData.getStampGuiTexture(held), LetterData.getStampFoilEffect(held));
        }
        addRenderableWidget(textBox);
    }

    public void loadLetter(String text, boolean readOnly, String signer, String stamp, String signerUuid, String stampGuiTexture) {
        loadLetter(text, readOnly, signer, stamp, signerUuid, stampGuiTexture, StampData.FOIL_NONE);
    }

    public void loadLetter(String text, boolean readOnly, String signer, String stamp, String signerUuid, String stampGuiTexture, String stampFoilEffect) {
        this.text = text;
        this.readOnly = readOnly;
        this.signer = signer;
        this.stamp = stamp;
        this.signerUuid = signerUuid;
        this.stampGuiTexture = stampGuiTexture;
        this.stampFoilEffect = stampFoilEffect;
        if (textBox != null) {
            textBox.setValue(text);
            textBox.active = !readOnly;
            textBox.visible = !readOnly;
        }
    }



    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        if (readOnly) {
            gui.blit(RenderPipelines.GUI_TEXTURED, READ_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
            drawReadOnlyLetter(gui);
            return;
        }
        gui.blit(RenderPipelines.GUI_TEXTURED, WRITE_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        textBox.active = true;
        textBox.visible = true;
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
        drawPressedButtons(gui);
        drawCenteredText(gui, title, leftPos + TITLE_X + TITLE_WIDTH / 2, topPos + TITLE_Y, TEXT_COLOR);
        drawButtonText(gui, leftPos + SAVE_X, topPos + SAVE_Y, BUTTON_WIDTH, Component.translatable("screen.letter_signal_phone.letter.save"));
        drawButtonText(gui, leftPos + SEAL_X, topPos + SEAL_Y, BUTTON_WIDTH, Component.translatable("screen.letter_signal_phone.letter.seal"));
        drawButtonText(gui, leftPos + DELETE_X, topPos + DELETE_Y, BUTTON_WIDTH, Component.translatable("screen.letter_signal_phone.letter.delete"));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();
        if (readOnly) {
            return true;
        }
        if (inside(x, y, leftPos + SAVE_X, topPos + SAVE_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            savePressedUntil = System.currentTimeMillis() + PRESS_FEEDBACK_MS;
            ClientPacketDistributor.sendToServer(new MailPayloads.LetterSavePayload(textBox.getValue()));
            return true;
        }
        if (inside(x, y, leftPos + SEAL_X, topPos + SEAL_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            sealPressedUntil = System.currentTimeMillis() + PRESS_FEEDBACK_MS;
            ClientPacketDistributor.sendToServer(new MailPayloads.LetterSealPayload(textBox.getValue()));
            return true;
        }
        if (inside(x, y, leftPos + DELETE_X, topPos + DELETE_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            deletePressedUntil = System.currentTimeMillis() + PRESS_FEEDBACK_MS;
            textBox.setValue("");
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (readOnly) {
            return super.keyPressed(event);
        }
        if (textBox.keyPressed(event)) {
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (readOnly) {
            return super.charTyped(event);
        }
        return textBox.charTyped(event) || super.charTyped(event);
    }

    private void drawButtonText(GuiGraphicsExtractor gui, int x, int y, int width, Component text) {
        drawCenteredText(gui, text, x + width / 2, y + 4, BUTTON_TEXT_COLOR);
    }

    private void drawPressedButtons(GuiGraphicsExtractor gui) {
        long now = System.currentTimeMillis();
        if (now < savePressedUntil) {
            drawPressedButton(gui, SAVE_X, SAVE_Y);
        }
        if (now < sealPressedUntil) {
            drawPressedButton(gui, SEAL_X, SEAL_Y);
        }
        if (now < deletePressedUntil) {
            drawPressedButton(gui, DELETE_X, DELETE_Y);
        }
    }

    private void drawPressedButton(GuiGraphicsExtractor gui, int x, int y) {
        gui.blit(RenderPipelines.GUI_TEXTURED, WRITE_TEXTURE, leftPos + x, topPos + y, BUTTON_PRESSED_U, BUTTON_PRESSED_V, BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
    }

    private void drawReadOnlyLetter(GuiGraphicsExtractor gui) {
        drawStamp(gui);
        drawSigner(gui);
        int y = topPos + READ_TEXT_Y;
        int maxY = topPos + READ_TEXT_Y + READ_TEXT_HEIGHT - font.lineHeight;
        for (String line : wrap(text, READ_TEXT_WIDTH)) {
            if (y > maxY) {
                break;
            }
            gui.text(font, Component.literal(line), leftPos + READ_TEXT_X, y, 0xFFFFFFFF, false);
            y += 9;
        }
    }

    private void drawStamp(GuiGraphicsExtractor gui) {
        if (stamp.isEmpty()) {
            drawCenteredText(gui, Component.translatable("screen.letter_signal_phone.letter.no_stamp"), leftPos + READ_STAMP_X + READ_STAMP_SIZE / 2, topPos + READ_STAMP_Y + 11, 0xFFFFFFFF);
            return;
        }
        gui.blit(RenderPipelines.GUI_TEXTURED, stampTexture(), leftPos + READ_STAMP_X, topPos + READ_STAMP_Y, 0, 0, READ_STAMP_SIZE, READ_STAMP_SIZE, READ_STAMP_SIZE, READ_STAMP_SIZE);
        ClientStampTooltipComponent.drawFoil(gui, leftPos + READ_STAMP_X, topPos + READ_STAMP_Y, READ_STAMP_SIZE, stampFoilEffect);
    }

    private Identifier stampTexture() {
        if (!stampGuiTexture.isEmpty()) {
            return Identifier.parse(stampGuiTexture);
        }
        if (stamp.isEmpty()) {
            return DEFAULT_STAMP_TEXTURE;
        }
        Identifier stampId = Identifier.parse(stamp);
        return Identifier.fromNamespaceAndPath(stampId.getNamespace(), "textures/stamp/" + stampId.getPath() + ".png");
    }

    private void drawSigner(GuiGraphicsExtractor gui) {
        //drawSignerHead(gui, leftPos + 38, topPos + 9);
        Component name = signer.isEmpty() ? Component.empty() : Component.literal(font.plainSubstrByWidth(signer, READ_SIGNER_WIDTH));
        gui.text(font, name, leftPos + READ_SIGNER_X, topPos + READ_SIGNER_Y, 0xFFFFFFFF, false);
    }
/* 娓叉煋鐜╁澶村儚锛岀洰鍓嶄技涔庢湁浜涢棶棰樹笉鐢熸晥銆?
    private void drawSignerHead(GuiGraphicsExtractor gui, int x, int y) {
        Identifier skin = signerSkin();
        if (skin == null) {
            gui.fill(x, y, x + 16, y + 16, 0xFF6D6258);
            drawCenteredText(gui, Component.literal(signer.isEmpty() ? "?" : signer.substring(0, 1)), x + 8, y + 4, 0xFFFFFFFF);
            return;
        }
        gui.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 16, 16, 8, 8, 8, 8, 64, 64);
    }*/

    private Identifier signerSkin() {
        if (signerUuid.isEmpty()) {
            return null;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() == null) {
                return null;
            }
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(UUID.fromString(signerUuid));
            return info == null ? null : info.getSkin().body().texturePath();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                String next = line.toString() + paragraph.charAt(i);
                if (!line.isEmpty() && font.width(next) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(paragraph.charAt(i));
            }
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
