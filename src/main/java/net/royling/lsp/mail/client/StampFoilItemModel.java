package net.royling.lsp.mail.client;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.registry.ModItems;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class StampFoilItemModel implements ItemModel {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "stamp_foil");

    private final ItemModel base;
    private final ItemModel rare;
    private final ItemModel highRare;
    private final ItemModel uniqueRare;
    private final ItemModel rgbRare;

    private StampFoilItemModel(ItemModel base, ItemModel rare, ItemModel highRare, ItemModel uniqueRare, ItemModel rgbRare) {
        this.base = base;
        this.rare = rare;
        this.highRare = highRare;
        this.uniqueRare = uniqueRare;
        this.rgbRare = rgbRare;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        base.update(output, item, resolver, displayContext, level, owner, seed);
        ItemModel overlay = overlayFor(item);
        if (overlay != null) {
            overlay.update(output, foilStack(item), resolver, displayContext, level, owner, seed);
        }
    }

    private ItemModel overlayFor(ItemStack stack) {
        if (!stack.is(ModItems.STAMP.get())) {
            return null;
        }
        return switch (StampData.rarity(stack)) {
            case StampData.RARITY_RARE -> rare;
            case StampData.RARITY_HIGH_RARE -> highRare;
            case StampData.RARITY_UNIQUE_RARE -> uniqueRare;
            case StampData.RARITY_RGB_RARE -> rgbRare;
            default -> null;
        };
    }

    private static ItemStack foilStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return copy;
    }

    public record Unbaked(
            Optional<Transformation> transformation,
            ItemModel.Unbaked base,
            ItemModel.Unbaked rare,
            ItemModel.Unbaked highRare,
            ItemModel.Unbaked uniqueRare,
            ItemModel.Unbaked rgbRare
    ) implements ItemModel.Unbaked {
        public static final MapCodec<StampFoilItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(StampFoilItemModel.Unbaked::transformation),
                                ItemModels.CODEC.fieldOf("base").forGetter(StampFoilItemModel.Unbaked::base),
                                ItemModels.CODEC.fieldOf("rare").forGetter(StampFoilItemModel.Unbaked::rare),
                                ItemModels.CODEC.fieldOf("high_rare").forGetter(StampFoilItemModel.Unbaked::highRare),
                                ItemModels.CODEC.fieldOf("unique_rare").forGetter(StampFoilItemModel.Unbaked::uniqueRare),
                                ItemModels.CODEC.fieldOf("rgb_rare").forGetter(StampFoilItemModel.Unbaked::rgbRare)
                        )
                        .apply(instance, StampFoilItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<StampFoilItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            return new StampFoilItemModel(
                    base.bake(context, childTransform),
                    rare.bake(context, childTransform),
                    highRare.bake(context, childTransform),
                    uniqueRare.bake(context, childTransform),
                    rgbRare.bake(context, childTransform)
            );
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            base.resolveDependencies(resolver);
            rare.resolveDependencies(resolver);
            highRare.resolveDependencies(resolver);
            uniqueRare.resolveDependencies(resolver);
            rgbRare.resolveDependencies(resolver);
        }
    }
}
