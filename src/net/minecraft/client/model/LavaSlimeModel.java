package net.minecraft.client.model;

import java.util.Arrays;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LavaSlimeModel extends EntityModel<SlimeRenderState> {
    private static final int SEGMENT_COUNT = 8;
    private final ModelPart[] bodyCubes = new ModelPart[8];

    public LavaSlimeModel(ModelPart pRoot) {
        super(pRoot);
        Arrays.setAll(this.bodyCubes, p_170709_ -> pRoot.getChild(getSegmentName(p_170709_)));
    }

    private static String getSegmentName(int pIndex) {
        return "cube" + pIndex;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        for (int i = 0; i < 8; i++) {
            int j = 0;
            int k = 0;
            if (i > 0 && i < 4) {
                k += 9 * i;
            } else if (i > 3) {
                j = 32;
                k += 9 * i - 36;
            }

            partdefinition.addOrReplaceChild(
                getSegmentName(i), CubeListBuilder.create().texOffs(j, k).addBox(-4.0F, (float)(16 + i), -4.0F, 8.0F, 1.0F, 8.0F), PartPose.ZERO
            );
        }

        partdefinition.addOrReplaceChild(
            "inside_cube", CubeListBuilder.create().texOffs(24, 40).addBox(-2.0F, 18.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.ZERO
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(SlimeRenderState p_370173_) {
        super.setupAnim(p_370173_);
        float f = Math.max(0.0F, p_370173_.squish);

        for (int i = 0; i < this.bodyCubes.length; i++) {
            this.bodyCubes[i].y = (float)(-(4 - i)) * f * 1.7F;
        }
    }
}