package net.royling.lsp.mail;


import net.royling.lsp.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class MailItemCodec {
    private static final String PART = "\t";
    private static final String STACK = ";";
    private static final String COUNT = ",";

    private MailItemCodec() {
    }

    public static boolean canSend(ItemStack stack) {
        return (stack.is(ModItems.LETTER.get()) && LetterData.isSealed(stack)) || (stack.is(ModItems.PACKAGE.get()) && PackageData.isPacked(stack));
    }

    public static String encodeMail(ItemStack stack) {
        if (stack.is(ModItems.LETTER.get()) && LetterData.isSealed(stack)) {
            return "letter"
                    + PART + b64(LetterData.getText(stack))
                    + PART + b64(LetterData.getStamp(stack))
                    + PART + b64(LetterData.getSigner(stack))
                    + PART + b64(LetterData.getStampVariant(stack))
                    + PART + b64(LetterData.getStampGuiTexture(stack))
                    + PART + b64(LetterData.getSignerUuid(stack))
                    + PART + b64(LetterData.getStampRarity(stack))
                    + PART + b64(LetterData.getStampFoilEffect(stack));
        }
        if (stack.is(ModItems.PACKAGE.get()) && PackageData.isPacked(stack)) {
            return "package" + PART + b64(PackageData.getItems(stack));
        }
        return "";
    }

    public static ItemStack decodeMail(String encoded) {
        String[] parts = encoded.split(PART, -1);
        if (parts.length >= 3 && parts[0].equals("letter")) {
            ItemStack stack = new ItemStack(ModItems.LETTER.get());
            if (parts.length >= 9) {
                LetterData.seal(stack, fromB64(parts[1]), fromB64(parts[2]), fromB64(parts[4]), fromB64(parts[5]), fromB64(parts[7]), fromB64(parts[8]), fromB64(parts[3]), fromB64(parts[6]));
            } else if (parts.length >= 7) {
                LetterData.seal(stack, fromB64(parts[1]), fromB64(parts[2]), fromB64(parts[4]), fromB64(parts[5]), fromB64(parts[3]), fromB64(parts[6]));
            } else {
                LetterData.seal(stack, fromB64(parts[1]), fromB64(parts[2]), parts.length >= 4 ? fromB64(parts[3]) : "");
            }
            return stack;
        }
        if (parts.length >= 2 && parts[0].equals("package")) {
            ItemStack stack = new ItemStack(ModItems.PACKAGE.get());
            PackageData.setItems(stack, fromB64(parts[1]));
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static String encodePackedStacks(List<PackedStack> stacks) {
        StringBuilder builder = new StringBuilder();
        for (PackedStack stack : stacks) {
            if (!builder.isEmpty()) {
                builder.append(STACK);
            }
            builder.append(stack.encodedStack());
        }
        return builder.toString();
    }

    public static List<PackedStack> decodePackedStacks(String encoded) {
        List<PackedStack> stacks = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return stacks;
        }
        for (String entry : encoded.split(STACK, -1)) {
            ItemStack fullStack = decodeStack(entry);
            if (!fullStack.isEmpty()) {
                stacks.add(PackedStack.of(fullStack));
                continue;
            }
            decodeLegacyPackedStack(entry, stacks);
        }
        return stacks;
    }

    public static PackedStack packedStack(ItemStack stack) {
        return PackedStack.of(stack);
    }

    private static void decodeLegacyPackedStack(String entry, List<PackedStack> stacks) {
        String[] parts = entry.split(COUNT, -1);
        if (parts.length != 2) {
            return;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parts[0]));
            if (item != null) {
                stacks.add(PackedStack.of(new ItemStack(item, Math.max(1, Math.min(64, Integer.parseInt(parts[1]))))));
            }
        } catch (Exception ignored) {
        }
    }

    private static String encodeStack(ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
                .result()
                .map(Tag::toString)
                .map(MailItemCodec::b64)
                .orElse("");
    }

    private static ItemStack decodeStack(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseFully(fromB64(encoded));
            return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String value) {
        if (value.isEmpty()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public record PackedStack(String encodedStack, int count) {
        static PackedStack of(ItemStack stack) {
            ItemStack copy = stack.copy();
            return new PackedStack(encodeStack(copy), copy.getCount());
        }

        public ItemStack toStack() {
            return decodeStack(encodedStack);
        }
    }
}
