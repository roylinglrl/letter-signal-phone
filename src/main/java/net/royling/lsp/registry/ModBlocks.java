package net.royling.lsp.registry;

import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.MailboxBlock;
import net.royling.lsp.mail.MessageInBottleBlock;
import net.royling.lsp.owl.OwlNestBlock;
import net.royling.lsp.phone.block.CardWriterBlock;
import net.royling.lsp.telegraph.TelegraphBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LetterSignalPhone.MODID);

    public static final DeferredBlock<CardWriterBlock> CARD_WRITER = BLOCKS.registerBlock(
            "card_writer",
            CardWriterBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F, 6.0F).requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<MailboxBlock> MAILBOX = BLOCKS.registerBlock(
            "mailbox",
            MailboxBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(2.0F, 3.0F)
    );
    public static final DeferredBlock<MessageInBottleBlock> MESSAGE_IN_BOTTLE = BLOCKS.registerBlock(
            "message_in_bottle",
            MessageInBottleBlock::new,
            properties -> properties.mapColor(MapColor.NONE).strength(0.3F).noOcclusion().dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ)
    );
    public static final DeferredBlock<TelegraphBlock> TELEGRAPH_MACHINE = BLOCKS.registerBlock(
            "telegraph_machine",
            TelegraphBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F, 6.0F).requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<OwlNestBlock> OWL_NEST = BLOCKS.registerBlock(
            "owl_nest",
            OwlNestBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(2.0F, 3.0F)
    );

    private ModBlocks() {
    }
}
