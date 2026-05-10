package net.royling.lsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.entity.ThrownMessageInBottle;
import net.royling.lsp.owl.OwlEntity;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LetterSignalPhone.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownMessageInBottle>> THROWN_MESSAGE_IN_BOTTLE =
            ENTITY_TYPES.register("thrown_message_in_bottle", () -> EntityType.Builder
                    .<ThrownMessageInBottle>of(ThrownMessageInBottle::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "thrown_message_in_bottle"))));
    public static final DeferredHolder<EntityType<?>, EntityType<OwlEntity>> OWL =
            ENTITY_TYPES.register("owl", () -> EntityType.Builder
                    .of(OwlEntity::new, MobCategory.CREATURE)
                    .sized(0.45F, 0.65F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "owl"))));

    private ModEntityTypes() {
    }
}
