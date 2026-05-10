package net.royling.lsp.mail;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.registry.ModItems;

public final class StampData {
    public static final String VARIANT = "lsp_stamp_variant";
    public static final String NAME = "lsp_stamp_name";
    public static final String GUI_TEXTURE = "lsp_stamp_gui_texture";
    public static final String RARITY = "lsp_stamp_rarity";
    public static final String FOIL_EFFECT = "lsp_stamp_foil_effect";
    public static final String PACK_ORIGIN_PLAYER = "lsp_stamp_pack_origin_player";
    public static final String PACK_ORIGIN_USE = "lsp_stamp_pack_origin_use";
    public static final String RARITY_COMMON = "common";
    public static final String RARITY_RARE = "rare";
    public static final String RARITY_HIGH_RARE = "high_rare";
    public static final String RARITY_UNIQUE_RARE = "unique_rare";
    public static final String RARITY_RGB_RARE = "rgb_rare";
    public static final String FOIL_NONE = "none";
    public static final String FOIL_HOLOGRAPHIC_STRIPES = "holographic_stripes";
    public static final String FOIL_COLOR_CRYSTAL = "color_crystal";
    public static final String FOIL_DIAMOND = "diamond";
    public static final String FOIL_RGB_SHIFT = "rgb_shift";

    public static final Identifier DEFAULT_VARIANT = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "default");
    public static final Identifier DEFAULT_GUI_TEXTURE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/stamp/stamp.png");

    private StampData() {
    }

    public static ItemStack stackFor(StampVariant variant) {
        ItemStack stack = new ItemStack(ModItems.STAMP.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(VARIANT, variant.id().toString());
            tag.putString(NAME, variant.nameKey());
            tag.putString(GUI_TEXTURE, variant.guiTexture().toString());
            tag.putString(RARITY, RARITY_COMMON);
            tag.putString(FOIL_EFFECT, FOIL_NONE);
        });
        applyNameStyle(stack);
        stack.set(DataComponents.ITEM_MODEL, variant.itemModel());
        return stack;
    }

    public static ItemStack rareHolographicStackFor(StampVariant variant) {
        ItemStack stack = stackFor(variant);
        setRarityAndFoil(stack, RARITY_RARE, FOIL_HOLOGRAPHIC_STRIPES);
        return stack;
    }

    public static ItemStack highRareCrystalStackFor(StampVariant variant) {
        ItemStack stack = stackFor(variant);
        setRarityAndFoil(stack, RARITY_HIGH_RARE, FOIL_COLOR_CRYSTAL);
        return stack;
    }

    public static ItemStack uniqueRareDiamondStackFor(StampVariant variant) {
        ItemStack stack = stackFor(variant);
        setRarityAndFoil(stack, RARITY_UNIQUE_RARE, FOIL_DIAMOND);
        return stack;
    }

    public static ItemStack rgbRareStackFor(StampVariant variant) {
        ItemStack stack = stackFor(variant);
        setRarityAndFoil(stack, RARITY_RGB_RARE, FOIL_RGB_SHIFT);
        return stack;
    }

    public static void setRarityAndFoil(ItemStack stack, String rarity, String foilEffect) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(RARITY, rarity);
            tag.putString(FOIL_EFFECT, foilEffect);
        });
        applyNameStyle(stack);
    }

    public static void setPackOrigin(ItemStack stack, String playerName, int useCount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(PACK_ORIGIN_PLAYER, playerName);
            tag.putInt(PACK_ORIGIN_USE, useCount);
        });
    }

    public static Identifier variant(ItemStack stack) {
        String value = tag(stack).getStringOr(VARIANT, DEFAULT_VARIANT.toString());
        return Identifier.parse(value);
    }

    public static String name(ItemStack stack) {
        return tag(stack).getStringOr(NAME, "Stamp");
    }

    public static Identifier guiTexture(ItemStack stack) {
        String value = tag(stack).getStringOr(GUI_TEXTURE, DEFAULT_GUI_TEXTURE.toString());
        return Identifier.parse(value);
    }

    public static String rarity(ItemStack stack) {
        return tag(stack).getStringOr(RARITY, RARITY_COMMON);
    }

    public static String foilEffect(ItemStack stack) {
        return tag(stack).getStringOr(FOIL_EFFECT, FOIL_NONE);
    }

    public static String packOriginPlayer(ItemStack stack) {
        return tag(stack).getStringOr(PACK_ORIGIN_PLAYER, "");
    }

    public static int packOriginUse(ItemStack stack) {
        return tag(stack).getIntOr(PACK_ORIGIN_USE, 0);
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void applyNameStyle(ItemStack stack) {
        ChatFormatting color = rarityColor(rarity(stack));
        Component name = Component.translatable(name(stack));
        if (color == null) {
            stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(style -> style.withItalic(false)));
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(style -> style.withItalic(false).withColor(color)));
    }

    private static ChatFormatting rarityColor(String rarity) {
        return switch (rarity) {
            case RARITY_RARE -> ChatFormatting.AQUA;
            case RARITY_HIGH_RARE -> ChatFormatting.LIGHT_PURPLE;
            case RARITY_UNIQUE_RARE -> ChatFormatting.GOLD;
            case RARITY_RGB_RARE -> ChatFormatting.RED;
            default -> null;
        };
    }
}
