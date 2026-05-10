package net.royling.lsp.registry;

import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.StampPackData;
import net.royling.lsp.mail.StampPackManager;
import net.royling.lsp.mail.StampVariantManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LetterSignalPhone.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS = TABS.register("items", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.letter_signal_phone"))
            .icon(() -> new ItemStack(ModItems.PHONE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PHONE.get());
                output.accept(ModItems.BLANK_PHONE_CARD.get());
                output.accept(ModItems.PHONE_CARD.get());
                output.accept(ModItems.CARD_WRITER.get());
                output.accept(ModItems.TELEGRAPH_MACHINE.get());
                output.accept(ModItems.TELEGRAM_PAPER.get());
                output.accept(ModItems.OWL_NEST.get());
                output.accept(ModItems.OWL_EGG.get());
                output.accept(ModItems.OWL_SPAWN_EGG.get());
                output.accept(ModItems.MAILBOX.get());
                output.accept(ModItems.MESSAGE_IN_BOTTLE.get());
                output.accept(ModItems.THROWABLE_MESSAGE_IN_BOTTLE.get());
                output.accept(ModItems.LETTER.get());
                // 閭エ鍙樹綋鏉ヨ嚜鏁版嵁鍖咃紝闇€瑕佹寜褰撳墠鍔犺浇缁撴灉鍔ㄦ€佺敓鎴愬垱閫犳爮鐗╁搧銆?
                for (var variant : StampVariantManager.INSTANCE.variants()) {
                    output.accept(StampData.stackFor(variant));
                    output.accept(StampData.rareHolographicStackFor(variant));
                    output.accept(StampData.highRareCrystalStackFor(variant));
                    output.accept(StampData.uniqueRareDiamondStackFor(variant));
                    output.accept(StampData.rgbRareStackFor(variant));
                }
                for (var pack : StampPackManager.INSTANCE.packs()) {
                    output.accept(StampPackData.stackFor(pack));
                }
                output.accept(ModItems.PACKING_BOX.get());
                output.accept(ModItems.PACKAGE.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
