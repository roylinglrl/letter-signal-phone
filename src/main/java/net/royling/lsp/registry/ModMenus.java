package net.royling.lsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.menu.LetterMenu;
import net.royling.lsp.mail.menu.MailboxMenu;
import net.royling.lsp.mail.menu.PackageMenu;
import net.royling.lsp.mail.menu.PackingMenu;
import net.royling.lsp.mail.menu.StampAlbumMenu;
import net.royling.lsp.telegraph.menu.TelegraphMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, LetterSignalPhone.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MailboxMenu>> MAILBOX =
            MENUS.register("mailbox", () -> IMenuTypeExtension.create((id, inventory, data) -> new MailboxMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<LetterMenu>> LETTER =
            MENUS.register("letter", () -> IMenuTypeExtension.create((id, inventory, data) -> new LetterMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<PackingMenu>> PACKING =
            MENUS.register("packing", () -> IMenuTypeExtension.create((id, inventory, data) -> new PackingMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<PackageMenu>> PACKAGE =
            MENUS.register("package", () -> IMenuTypeExtension.create((id, inventory, data) -> new PackageMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<StampAlbumMenu>> STAMP_ALBUM =
            MENUS.register("stamp_album", () -> IMenuTypeExtension.create((id, inventory, data) -> new StampAlbumMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<TelegraphMenu>> TELEGRAPH =
            MENUS.register("telegraph_machine", () -> IMenuTypeExtension.create((id, inventory, data) -> new TelegraphMenu(id, inventory)));

    private ModMenus() {
    }
}
