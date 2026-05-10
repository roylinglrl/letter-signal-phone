package net.royling.lsp.entity_model;

// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.owl.client.OwlRenderState;

public class owl extends EntityModel<OwlRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "owl"), "main");
	private final ModelPart owl;
	private final ModelPart body;
	private final ModelPart rightfoot;
	private final ModelPart leftfoot;
	private final ModelPart head;
	private final ModelPart letter;
	private final ModelPart rightwing;
	private final ModelPart leftwing;
	private final KeyframeAnimation idleAnimation;
	private final KeyframeAnimation flyAnimation;
	private final KeyframeAnimation walkAnimation;
	private final KeyframeAnimation sleepAnimation;
	private final KeyframeAnimation sleep2Animation;

	public owl(ModelPart root) {
		super(root);
		this.owl = root.getChild("owl");
		this.body = this.owl.getChild("body");
		this.rightfoot = this.body.getChild("rightfoot");
		this.leftfoot = this.body.getChild("leftfoot");
		this.head = this.body.getChild("head");
		this.letter = this.head.getChild("letter");
		this.letter.visible = false;
		this.rightwing = this.body.getChild("rightwing");
		this.leftwing = this.body.getChild("leftwing");
		this.idleAnimation = OwlAnimations.IDLE.bake(root);
		this.flyAnimation = OwlAnimations.FLY.bake(root);
		this.walkAnimation = OwlAnimations.WALK.bake(root);
		this.sleepAnimation = OwlAnimations.SLEEP.bake(root);
		this.sleep2Animation = OwlAnimations.SLEEP2.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition owl = partdefinition.addOrReplaceChild("owl", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = owl.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 28).addBox(-3.5F, -1.3141F, -2.7465F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-4.5F, -6.0641F, -3.9965F, 9.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.6859F, 0.7465F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(30, 21).addBox(-2.0F, -1.5F, -1.0F, 5.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 3.4299F, 3.6564F, 0.8727F, 0.0F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(30, 16).addBox(-3.0F, -1.5F, -1.0F, 6.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.9299F, 2.1564F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0F, 1.0F, -2.0F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5641F, -1.7465F, 0.2182F, 0.0F, 0.0F));

		PartDefinition rightfoot = body.addOrReplaceChild("rightfoot", CubeListBuilder.create().texOffs(10, 36).addBox(-2.0F, 2.0F, -2.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(30, 26).addBox(-2.0F, 2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(34, 26).addBox(0.0F, 2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(34, 26).addBox(-1.0F, 2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(26, 40).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(22, 36).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 2.6859F, -0.7465F));

		PartDefinition leftfoot = body.addOrReplaceChild("leftfoot", CubeListBuilder.create().texOffs(36, 10).addBox(-1.0F, 2.0F, -2.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(36, 14).addBox(-1.0F, 2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(38, 26).addBox(1.0F, 2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(38, 26).addBox(0.0F, 2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(34, 40).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 36).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 2.6859F, -0.7465F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -7.75F, -4.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(44, 21).addBox(-0.5F, -4.25F, -5.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.3141F, -0.7465F));

		PartDefinition cube_r4 = head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(34, 43).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -7.75F, 0.0F, -0.5236F, 0.0F, -0.3927F));

		PartDefinition cube_r5 = head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(42, 43).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -7.75F, 0.0F, 0.3491F, 0.0F, -0.3927F));

		PartDefinition cube_r6 = head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(30, 43).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -7.75F, 0.0F, -0.5236F, 0.0F, 0.3927F));

		PartDefinition cube_r7 = head.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(38, 43).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -7.75F, 0.0F, 0.3491F, 0.0F, 0.3927F));

		PartDefinition letter = head.addOrReplaceChild("letter", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition cube_r8 = letter.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(1, 46).addBox(-4.5F, -2.5F, 0.0F, 9.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.5F, -4.75F, -0.1745F, 0.0F, 0.0F));

		PartDefinition rightwing = body.addOrReplaceChild("rightwing", CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, 0.1667F, -3.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(42, 36).addBox(-0.5F, 1.1667F, 1.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 40).addBox(-0.5F, 6.1667F, -2.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.25F, -7.7308F, -0.7465F, 0.3491F, 0.0F, 0.0F));

		PartDefinition leftwing = body.addOrReplaceChild("leftwing", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.1667F, -3.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(26, 43).addBox(-0.5F, 1.1667F, 1.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 40).addBox(-0.5F, 6.1667F, -2.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.25F, -7.7308F, -0.7465F, 0.3491F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(OwlRenderState state) {
		super.setupAnim(state);
		letter.visible = false;
		long time = (long) (state.ageInTicks * 50.0F);
		if (state.sleeping) {
			(state.sleepPose == 0 ? sleepAnimation : sleep2Animation).apply(time, 1.0F);
		} else if (state.flying) {
			flyAnimation.apply(time, 1.0F);
		} else if (state.moving) {
			walkAnimation.applyWalk(state.walkAnimationPos, Math.max(state.walkAnimationSpeed, 0.15F), 2.0F, 1.0F);
		} else {
			idleAnimation.apply(time, 1.0F);
		}
		if (!state.sleeping && !state.flying) {
			head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.45F;
			head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.45F;
		}
	}
}
