package net.royling.lsp.entity_model;// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.royling.lsp.LetterSignalPhone;
import net.royling.lsp.owl.client.OwlRenderState;

public class baby_owl extends EntityModel<OwlRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "baby_owl"), "main");
	private final ModelPart owl;
	private final ModelPart rightfoot;
	private final ModelPart leftfoot;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightwing;
	private final ModelPart leftwing;

	public baby_owl(ModelPart root) {
		super(root);
		this.owl = root.getChild("owl");
		this.rightfoot = this.owl.getChild("rightfoot");
		this.leftfoot = this.owl.getChild("leftfoot");
		this.body = this.owl.getChild("body");
		this.head = this.owl.getChild("head");
		this.rightwing = this.owl.getChild("rightwing");
		this.leftwing = this.owl.getChild("leftwing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition owl = partdefinition.addOrReplaceChild("owl", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition rightfoot = owl.addOrReplaceChild("rightfoot", CubeListBuilder.create().texOffs(10, 36).addBox(-4.0F, -1.0F, -2.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(30, 26).addBox(-4.0F, -1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(34, 26).addBox(-2.0F, -1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(34, 26).addBox(-3.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(22, 36).addBox(-3.0F, -2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition leftfoot = owl.addOrReplaceChild("leftfoot", CubeListBuilder.create().texOffs(36, 10).addBox(-4.0F, -1.0F, -2.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(36, 14).addBox(-4.0F, -1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(38, 26).addBox(-2.0F, -1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(38, 26).addBox(-3.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(32, 36).addBox(-4.0F, -2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 0.0F, 0.0F));

		PartDefinition body = owl.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 16).addBox(-2.5F, -6.25F, -3.25F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(31, 16).addBox(-2.0F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -2.256F, 2.9029F, 0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(2, 28).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.75F, -1.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition head = owl.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -21.0F, -4.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(44, 21).addBox(-0.5F, -17.5F, -5.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.25F, 0.0F));

		PartDefinition cube_r3 = head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 43).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -21.0F, 0.0F, -0.5236F, 0.0F, -0.3927F));

		PartDefinition cube_r4 = head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(42, 43).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -21.0F, 0.0F, 0.3491F, 0.0F, -0.3927F));

		PartDefinition cube_r5 = head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(30, 43).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -21.0F, 0.0F, -0.5236F, 0.0F, 0.3927F));

		PartDefinition cube_r6 = head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(38, 43).addBox(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -21.0F, 0.0F, 0.3491F, 0.0F, 0.3927F));

		PartDefinition rightwing = owl.addOrReplaceChild("rightwing", CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, 0.1667F, -3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(42, 36).addBox(-0.5F, 1.1667F, 1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 40).addBox(-0.5F, 4.1667F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -6.4167F, 0.0F, 0.3491F, 0.0F, 0.0F));

		PartDefinition leftwing = owl.addOrReplaceChild("leftwing", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.1667F, -3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(26, 43).addBox(-0.5F, 1.1667F, 1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 40).addBox(-0.5F, 4.1667F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -6.4167F, 0.0F, 0.3491F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(OwlRenderState state) {
		super.setupAnim(state);
	}
}
