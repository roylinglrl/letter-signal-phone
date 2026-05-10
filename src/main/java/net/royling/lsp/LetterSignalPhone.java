package net.royling.lsp;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.royling.lsp.mail.MailSavedData;
import net.royling.lsp.mail.MessageInBottleCrafting;
import net.royling.lsp.mail.MessageInBottleLetterPool;
import net.royling.lsp.mail.MessageInBottleSpawner;
import net.royling.lsp.mail.StampVariantManager;
import net.royling.lsp.mail.StampPackManager;
import net.royling.lsp.mail.command.MailCommands;
import net.royling.lsp.mail.network.MailPayloads;
import net.royling.lsp.phone.call.CallManager;
import net.royling.lsp.phone.network.PhonePayloads;
import net.royling.lsp.owl.OwlEntity;
import net.royling.lsp.registry.ModBlocks;
import net.royling.lsp.registry.ModBlockEntities;
import net.royling.lsp.registry.ModCreativeTabs;
import net.royling.lsp.registry.ModEntityTypes;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.registry.ModMenus;
import net.royling.lsp.registry.ModParticles;
import net.royling.lsp.registry.ModSounds;
import net.royling.lsp.telegraph.TelegraphData;
import net.royling.lsp.telegraph.network.TelegraphPayloads;
import net.minecraft.network.chat.Component;

@Mod(LetterSignalPhone.MODID)
public class LetterSignalPhone {
    public static final String MODID = "letter_signal_phone";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final float MESSAGE_IN_BOTTLE_FISHING_CHANCE = 0.05F;

    public LetterSignalPhone(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, LspConfig.SERVER_SPEC, "letter_signal_phone-server.toml");
        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(PhonePayloads::register);
        modEventBus.addListener(MailPayloads::register);
        modEventBus.addListener(TelegraphPayloads::register);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(MessageInBottleCrafting::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onItemFished);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(StampVariantManager::register);
        NeoForge.EVENT_BUS.addListener(StampPackManager::register);
        NeoForge.EVENT_BUS.addListener(MessageInBottleLetterPool::register);
    }

    private void onServerTick(ServerTickEvent.Post event) {
        CallManager.tick(event.getServer());
        MailSavedData.get(event.getServer()).tick(event.getServer());
        MessageInBottleSpawner.tick(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        MailSavedData.get(event.getEntity().level().getServer()).refreshMailboxBinding(event.getEntity().level().getServer(), event.getEntity().getUUID());
    }

    private void onItemFished(ItemFishedEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity().getRandom().nextFloat() < MESSAGE_IN_BOTTLE_FISHING_CHANCE) {
            event.getDrops().add(new ItemStack(ModItems.MESSAGE_IN_BOTTLE.get()));
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MailCommands.register(event.getDispatcher());
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.OWL.get(), OwlEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntityTypes.OWL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnReason, pos, random) -> Animal.checkAnimalSpawnRules(type, level, spawnReason, pos, random)
                        && level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.DIRT),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    private void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.TELEGRAM_PAPER.get()) && TelegraphData.hasMessage(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.letter_signal_phone.telegraph.message", TelegraphData.getMessage(event.getItemStack())));
        }
    }
}
