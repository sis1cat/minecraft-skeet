package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianModel extends EntityModel<GuardianRenderState> {
    public static final MeshTransformer ELDER_GUARDIAN_SCALE = MeshTransformer.scaling(2.35F);
    private static final float[] SPIKE_X_ROT = new float[]{1.75F, 0.25F, 0.0F, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F, 1.25F, 0.75F, 0.0F, 0.0F};
    private static final float[] SPIKE_Y_ROT = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.25F, 1.75F, 1.25F, 0.75F, 0.0F, 0.0F, 0.0F, 0.0F};
    private static final float[] SPIKE_Z_ROT = new float[]{0.0F, 0.0F, 0.25F, 1.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 1.25F};
    private static final float[] SPIKE_X = new float[]{0.0F, 0.0F, 8.0F, -8.0F, -8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F, 8.0F, -8.0F};
    private static final float[] SPIKE_Y = new float[]{-8.0F, -8.0F, -8.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F};
    private static final float[] SPIKE_Z = new float[]{8.0F, -8.0F, 0.0F, 0.0F, -8.0F, -8.0F, 8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F};
    private static final String EYE = "eye";
    private static final String TAIL_0 = "tail0";
    private static final String TAIL_1 = "tail1";
    private static final String TAIL_2 = "tail2";
    private final ModelPart head;
    private final ModelPart eye;
    private final ModelPart[] spikeParts = new ModelPart[12];
    private final ModelPart[] tailParts;

    public GuardianModel(ModelPart pRoot) {
        super(pRoot);
        this.head = pRoot.getChild("head");

        for (int i = 0; i < this.spikeParts.length; i++) {
            this.spikeParts[i] = this.head.getChild(createSpikeName(i));
        }

        this.eye = this.head.getChild("eye");
        this.tailParts = new ModelPart[3];
        this.tailParts[0] = this.head.getChild("tail0");
        this.tailParts[1] = this.tailParts[0].getChild("tail1");
        this.tailParts[2] = this.tailParts[1].getChild("tail2");
    }

    private static String createSpikeName(int pIndex) {
        return "spike" + pIndex;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0F, 10.0F, -8.0F, 12.0F, 12.0F, 16.0F)
                .texOffs(0, 28)
                .addBox(-8.0F, 10.0F, -6.0F, 2.0F, 12.0F, 12.0F)
                .texOffs(0, 28)
                .addBox(6.0F, 10.0F, -6.0F, 2.0F, 12.0F, 12.0F, true)
                .texOffs(16, 40)
                .addBox(-6.0F, 8.0F, -6.0F, 12.0F, 2.0F, 12.0F)
                .texOffs(16, 40)
                .addBox(-6.0F, 22.0F, -6.0F, 12.0F, 2.0F, 12.0F),
            PartPose.ZERO
        );
        CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F);

        for (int i = 0; i < 12; i++) {
            float f = getSpikeX(i, 0.0F, 0.0F);
            float f1 = getSpikeY(i, 0.0F, 0.0F);
            float f2 = getSpikeZ(i, 0.0F, 0.0F);
            float f3 = (float) Math.PI * SPIKE_X_ROT[i];
            float f4 = (float) Math.PI * SPIKE_Y_ROT[i];
            float f5 = (float) Math.PI * SPIKE_Z_ROT[i];
            partdefinition1.addOrReplaceChild(createSpikeName(i), cubelistbuilder, PartPose.offsetAndRotation(f, f1, f2, f3, f4, f5));
        }

        partdefinition1.addOrReplaceChild(
            "eye", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 15.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -8.25F)
        );
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
            "tail0", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, 14.0F, 7.0F, 4.0F, 4.0F, 8.0F), PartPose.ZERO
        );
        PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
            "tail1", CubeListBuilder.create().texOffs(0, 54).addBox(0.0F, 14.0F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.offset(-1.5F, 0.5F, 14.0F)
        );
        partdefinition3.addOrReplaceChild(
            "tail2",
            CubeListBuilder.create()
                .texOffs(41, 32)
                .addBox(0.0F, 14.0F, 0.0F, 2.0F, 2.0F, 6.0F)
                .texOffs(25, 19)
                .addBox(1.0F, 10.5F, 3.0F, 1.0F, 9.0F, 9.0F),
            PartPose.offset(0.5F, 0.5F, 6.0F)
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public static LayerDefinition createElderGuardianLayer() {
        return createBodyLayer().apply(ELDER_GUARDIAN_SCALE);
    }

    public void setupAnim(GuardianRenderState p_362630_) {
        super.setupAnim(p_362630_);
        this.head.yRot = p_362630_.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = p_362630_.xRot * (float) (Math.PI / 180.0);
        float f = (1.0F - p_362630_.spikesAnimation) * 0.55F;
        this.setupSpikes(p_362630_.ageInTicks, f);
        if (p_362630_.lookAtPosition != null && p_362630_.lookDirection != null) {
            double d0 = p_362630_.lookAtPosition.y - p_362630_.eyePosition.y;
            if (d0 > 0.0) {
                this.eye.y = 0.0F;
            } else {
                this.eye.y = 1.0F;
            }

            Vec3 vec3 = p_362630_.lookDirection;
            vec3 = new Vec3(vec3.x, 0.0, vec3.z);
            Vec3 vec31 = new Vec3(p_362630_.eyePosition.x - p_362630_.lookAtPosition.x, 0.0, p_362630_.eyePosition.z - p_362630_.lookAtPosition.z)
                .normalize()
                .yRot((float) (Math.PI / 2));
            double d1 = vec3.dot(vec31);
            this.eye.x = Mth.sqrt((float)Math.abs(d1)) * 2.0F * (float)Math.signum(d1);
        }

        this.eye.visible = true;
        float f1 = p_362630_.tailAnimation;
        this.tailParts[0].yRot = Mth.sin(f1) * (float) Math.PI * 0.05F;
        this.tailParts[1].yRot = Mth.sin(f1) * (float) Math.PI * 0.1F;
        this.tailParts[2].yRot = Mth.sin(f1) * (float) Math.PI * 0.15F;
    }

    private void setupSpikes(float pAgeInTicks, float pSpikeAnimation) {
        for (int i = 0; i < 12; i++) {
            this.spikeParts[i].x = getSpikeX(i, pAgeInTicks, pSpikeAnimation);
            this.spikeParts[i].y = getSpikeY(i, pAgeInTicks, pSpikeAnimation);
            this.spikeParts[i].z = getSpikeZ(i, pAgeInTicks, pSpikeAnimation);
        }
    }

    private static float getSpikeOffset(int pIndex, float pAgeInTicks, float pSpikeAnimation) {
        return 1.0F + Mth.cos(pAgeInTicks * 1.5F + (float)pIndex) * 0.01F - pSpikeAnimation;
    }

    private static float getSpikeX(int pIndex, float pAgeInTicks, float pSpikeAnimation) {
        return SPIKE_X[pIndex] * getSpikeOffset(pIndex, pAgeInTicks, pSpikeAnimation);
    }

    private static float getSpikeY(int pIndex, float pAgeInTicks, float pSpikeAnimation) {
        return 16.0F + SPIKE_Y[pIndex] * getSpikeOffset(pIndex, pAgeInTicks, pSpikeAnimation);
    }

    private static float getSpikeZ(int pIndex, float pAgeInTicks, float pSpikeAnimation) {
        return SPIKE_Z[pIndex] * getSpikeOffset(pIndex, pAgeInTicks, pSpikeAnimation);
    }
}