package net.royling.lsp.registry;

import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.item.LetterItem;
import net.royling.lsp.mail.item.MessageInBottleItem;
import net.royling.lsp.mail.item.PackageItem;
import net.royling.lsp.mail.item.PackingBoxItem;
import net.royling.lsp.mail.item.StampItem;
import net.royling.lsp.mail.item.StampPackItem;
import net.royling.lsp.mail.item.ThrowableMessageInBottleItem;
import net.royling.lsp.phone.item.PhoneCardItem;
import net.royling.lsp.phone.item.PhoneItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LetterSignalPhone.MODID);

    public static final Supplier<PhoneItem> PHONE =
            ITEMS.registerItem("phone", PhoneItem::new, properties -> properties.stacksTo(1));
    public static final Supplier<Item> BLANK_PHONE_CARD =
            ITEMS.registerSimpleItem("blank_phone_card", properties -> properties.stacksTo(16));
    public static final Supplier<PhoneCardItem> PHONE_CARD =
            ITEMS.registerItem("phone_card", PhoneCardItem::new, properties -> properties.stacksTo(1));

    public static final DeferredItem<BlockItem> CARD_WRITER =
            blockItem("card_writer", ModBlocks.CARD_WRITER);
    public static final DeferredItem<BlockItem> MAILBOX =
            blockItem("mailbox", ModBlocks.MAILBOX);
    public static final DeferredItem<MessageInBottleItem> MESSAGE_IN_BOTTLE =
            ITEMS.registerItem("message_in_bottle", properties -> new MessageInBottleItem(ModBlocks.MESSAGE_IN_BOTTLE.get(), properties), properties -> properties.stacksTo(16));
    public static final Supplier<ThrowableMessageInBottleItem> THROWABLE_MESSAGE_IN_BOTTLE =
            ITEMS.registerItem("throwable_message_in_bottle", ThrowableMessageInBottleItem::new, properties -> properties.stacksTo(16));
    public static final DeferredItem<BlockItem> TELEGRAPH_MACHINE =
            blockItem("telegraph_machine", ModBlocks.TELEGRAPH_MACHINE);
    public static final Supplier<Item> TELEGRAM_PAPER =
            ITEMS.registerSimpleItem("telegram_paper", properties -> properties.stacksTo(1));
    public static final DeferredItem<BlockItem> OWL_NEST =
            blockItem("owl_nest", ModBlocks.OWL_NEST);
    public static final Supplier<Item> OWL_EGG =
            ITEMS.registerSimpleItem("owl_egg", properties -> properties.stacksTo(16));
    public static final Supplier<SpawnEggItem> OWL_SPAWN_EGG =
            ITEMS.registerItem("owl_spawn_egg", SpawnEggItem::new, properties -> properties.component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntityTypes.OWL.get(), new CompoundTag())));

    public static final Supplier<LetterItem> LETTER =
            ITEMS.registerItem("letter", LetterItem::new, properties -> properties.stacksTo(1));
    public static final Supplier<StampItem> STAMP =
            ITEMS.registerItem("stamp", StampItem::new, properties -> properties.stacksTo(64));
    public static final Supplier<StampPackItem> STAMP_PACK =
            ITEMS.registerItem("stamp_pack", StampPackItem::new, properties -> properties.stacksTo(16));
    public static final Supplier<PackingBoxItem> PACKING_BOX =
            ITEMS.registerItem("packing_box", PackingBoxItem::new, properties -> properties.stacksTo(16));
    public static final Supplier<PackageItem> PACKAGE =
            ITEMS.registerItem("package", PackageItem::new, properties -> properties.stacksTo(1));

    private ModItems() {
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<?> block) {
        return ITEMS.registerSimpleBlockItem(name, block);
    }
}
