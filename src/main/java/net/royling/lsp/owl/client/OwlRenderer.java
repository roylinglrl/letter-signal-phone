package net.royling.lsp.owl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.entity_model.baby_owl;
import net.royling.lsp.entity_model.owl;
import net.royling.lsp.owl.OwlEntity;

public class OwlRenderer extends AgeableMobRenderer<OwlEntity, OwlRenderState, EntityModel<? super OwlRenderState>> {
    private static final Identifier OWL = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/entity/owl.png");
    private static final Identifier OWL_SLEEP = Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "textures/entity/owl_sleep.png");

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new owl(context.bakeLayer(owl.LAYER_LOCATION)), new baby_owl(context.bakeLayer(baby_owl.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public OwlRenderState createRenderState() {
        return new OwlRenderState();
    }

    @Override
    public void extractRenderState(OwlEntity entity, OwlRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sleeping = entity.isSleepingOwl();
        state.sleepPose = entity.sleepPose();
        state.flying = !entity.onGround();
        state.moving = entity.getDeltaMovement().horizontalDistanceSqr() > 0.001D;
    }

    @Override
    protected void scale(OwlRenderState state, PoseStack poseStack) {
        float scale = state.isBaby ? 0.45F : 0.65F;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(OwlRenderState state) {
        return state.sleeping ? OWL_SLEEP : OWL;
    }
}
