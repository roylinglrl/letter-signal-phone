package net.royling.lsp.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.royling.lsp.LetterSignalPhone;

public final class LetterData {
    public static final String TEXT = "lsp_letter_text";
    public static final String SEALED = "lsp_letter_sealed";
    public static final String READ_ONLY = "lsp_letter_read_only";
    public static final String STAMP = "lsp_letter_stamp";
    public static final String STAMP_VARIANT = "lsp_letter_stamp_variant";
    public static final String STAMP_GUI_TEXTURE = "lsp_letter_stamp_gui_texture";
    public static final String STAMP_RARITY = "lsp_letter_stamp_rarity";
    public static final String STAMP_FOIL_EFFECT = "lsp_letter_stamp_foil_effect";
    public static final String SIGNER = "lsp_letter_signer";
    public static final String SIGNER_UUID = "lsp_letter_signer_uuid";
    private static final Identifier LETTER_MODEL = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "letter");
    private static final Identifier SEALED_LETTER_MODEL = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "sealed_letter");

    private LetterData() {
    }

    public static String getText(ItemStack stack) {
        return customTag(stack).getStringOr(TEXT, "");
    }

    public static void setText(ItemStack stack, String text) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TEXT, text));
        stack.set(DataComponents.ITEM_MODEL, LETTER_MODEL);
    }

    public static boolean isSealed(ItemStack stack) {
        return customTag(stack).getBooleanOr(SEALED, false);
    }

    public static boolean isReadOnly(ItemStack stack) {
        return isSealed(stack) || customTag(stack).getBooleanOr(READ_ONLY, false);
    }

    public static String getStamp(ItemStack stack) {
        return customTag(stack).getStringOr(STAMP, "");
    }

    public static String getStampVariant(ItemStack stack) {
        return customTag(stack).getStringOr(STAMP_VARIANT, "");
    }

    public static String getStampGuiTexture(ItemStack stack) {
        return customTag(stack).getStringOr(STAMP_GUI_TEXTURE, "");
    }

    public static String getStampRarity(ItemStack stack) {
        return customTag(stack).getStringOr(STAMP_RARITY, StampData.RARITY_COMMON);
    }

    public static String getStampFoilEffect(ItemStack stack) {
        return customTag(stack).getStringOr(STAMP_FOIL_EFFECT, StampData.FOIL_NONE);
    }

    public static String getSigner(ItemStack stack) {
        return customTag(stack).getStringOr(SIGNER, "");
    }

    public static String getSignerUuid(ItemStack stack) {
        return customTag(stack).getStringOr(SIGNER_UUID, "");
    }

    public static void seal(ItemStack stack, String text, String stamp, String signer) {
        seal(stack, text, stamp, signer, "");
    }

    public static void seal(ItemStack stack, String text, String stamp, String signer, String signerUuid) {
        seal(stack, text, stamp, "", "", signer, signerUuid);
    }

    public static void seal(ItemStack stack, String text, String stamp, String stampVariant, String stampGuiTexture, String signer, String signerUuid) {
        seal(stack, text, stamp, stampVariant, stampGuiTexture, StampData.RARITY_COMMON, StampData.FOIL_NONE, signer, signerUuid);
    }

    public static void seal(ItemStack stack, String text, String stamp, String stampVariant, String stampGuiTexture, String stampRarity, String stampFoilEffect, String signer, String signerUuid) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TEXT, text);
            tag.putBoolean(SEALED, true);
            tag.putBoolean(READ_ONLY, true);
            tag.putString(STAMP, stamp);
            tag.putString(STAMP_VARIANT, stampVariant);
            tag.putString(STAMP_GUI_TEXTURE, stampGuiTexture);
            tag.putString(STAMP_RARITY, stampRarity);
            tag.putString(STAMP_FOIL_EFFECT, stampFoilEffect);
            tag.putString(SIGNER, signer);
            tag.putString(SIGNER_UUID, signerUuid);
        });
        stack.set(DataComponents.ITEM_MODEL, SEALED_LETTER_MODEL);
    }

    public static void unseal(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(SEALED, false);
            tag.remove(STAMP);
            tag.remove(STAMP_VARIANT);
            tag.remove(STAMP_GUI_TEXTURE);
            tag.remove(STAMP_RARITY);
            tag.remove(STAMP_FOIL_EFFECT);
        });
        stack.set(DataComponents.ITEM_MODEL, LETTER_MODEL);
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
