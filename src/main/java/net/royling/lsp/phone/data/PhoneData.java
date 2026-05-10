package net.royling.lsp.phone.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PhoneData {
    public static final String CARD_NUMBER = "lsp_phone_number";
    public static final String CARD_TOKEN = "lsp_phone_token";
    public static final String INSTALLED_NUMBER = "lsp_installed_number";
    public static final String INSTALLED_TOKEN = "lsp_installed_token";

    private PhoneData() {
    }

    public static boolean hasCardNumber(ItemStack stack) {
        return !getCardNumber(stack).isEmpty();
    }

    public static String getCardNumber(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.getStringOr(CARD_NUMBER, "");
    }

    public static void setCardNumber(ItemStack stack, String number) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(CARD_NUMBER, number));
    }

    public static String getCardToken(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.getStringOr(CARD_TOKEN, "");
    }

    public static void setCardRegistration(ItemStack stack, String number, String token) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(CARD_NUMBER, number);
            tag.putString(CARD_TOKEN, token);
        });
    }

    public static void clearCardNumber(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(CARD_NUMBER));
    }

    public static String getInstalledNumber(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.getStringOr(INSTALLED_NUMBER, "");
    }

    public static String getInstalledToken(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.getStringOr(INSTALLED_TOKEN, "");
    }

    public static boolean hasInstalledCard(ItemStack stack) {
        return !getInstalledNumber(stack).isEmpty();
    }

    public static void setInstalledNumber(ItemStack stack, String number) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(INSTALLED_NUMBER, number));
    }

    public static void setInstalledRegistration(ItemStack stack, String number, String token) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(INSTALLED_NUMBER, number);
            tag.putString(INSTALLED_TOKEN, token);
        });
    }

    public static void clearInstalledNumber(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(INSTALLED_NUMBER);
            tag.remove(INSTALLED_TOKEN);
        });
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }
}
