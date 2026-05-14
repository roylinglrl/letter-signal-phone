package net.royling.lsp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.royling.lsp.mail.client.ClientStampTooltipComponent;
import net.royling.lsp.mail.client.MailClientHooks;
import net.royling.lsp.mail.client.MailboxNoticeParticle;
import net.royling.lsp.mail.client.StampFoilItemModel;
import net.royling.lsp.mail.menu.LetterScreen;
import net.royling.lsp.mail.menu.MailboxScreen;
import net.royling.lsp.mail.menu.PackageScreen;
import net.royling.lsp.mail.menu.PackingScreen;
import net.royling.lsp.mail.menu.StampAlbumScreen;
import net.royling.lsp.mail.tooltip.StampTooltipComponent;
import net.royling.lsp.entity_model.baby_owl;
import net.royling.lsp.entity_model.owl;
import net.royling.lsp.owl.client.OwlRenderer;
import net.royling.lsp.phone.client.PhoneClientHooks;
import net.royling.lsp.registry.ModMenus;
import net.royling.lsp.registry.ModEntityTypes;
import net.royling.lsp.registry.ModParticles;
import net.royling.lsp.telegraph.menu.TelegraphScreen;

@Mod(value = LetterSignalPhone.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = LetterSignalPhone.MODID, value = Dist.CLIENT)
public class LetterSignalPhoneClient {
    public LetterSignalPhoneClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        PhoneClientHooks.clientTick();
        MailClientHooks.clientTick();
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MAILBOX.get(), MailboxScreen::new);
        event.register(ModMenus.LETTER.get(), LetterScreen::new);
        event.register(ModMenus.PACKING.get(), PackingScreen::new);
        event.register(ModMenus.PACKAGE.get(), PackageScreen::new);
        event.register(ModMenus.STAMP_ALBUM.get(), StampAlbumScreen::new);
        event.register(ModMenus.TELEGRAPH.get(), TelegraphScreen::new);
    }

    @SubscribeEvent
    static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(StampFoilItemModel.TYPE, StampFoilItemModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THROWN_MESSAGE_IN_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.OWL.get(), OwlRenderer::new);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(owl.LAYER_LOCATION, owl::createBodyLayer);
        event.registerLayerDefinition(baby_owl.LAYER_LOCATION, baby_owl::createBodyLayer);
    }

    @SubscribeEvent
    static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(StampTooltipComponent.class, ClientStampTooltipComponent::new);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MAILBOX_NOTICE.get(), sprites ->
                (type, level, x, y, z, xd, yd, zd, random) -> new MailboxNoticeParticle(level, x, y, z, sprites));
    }
}
