package net.royling.lsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.owl.OwlNestBlockEntity;
import net.royling.lsp.telegraph.TelegraphBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LetterSignalPhone.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TelegraphBlockEntity>> TELEGRAPH =
            BLOCK_ENTITIES.register("telegraph_machine", () -> new BlockEntityType<>(TelegraphBlockEntity::new, ModBlocks.TELEGRAPH_MACHINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OwlNestBlockEntity>> OWL_NEST =
            BLOCK_ENTITIES.register("owl_nest", () -> new BlockEntityType<>(OwlNestBlockEntity::new, ModBlocks.OWL_NEST.get()));

    private ModBlockEntities() {
    }
}
