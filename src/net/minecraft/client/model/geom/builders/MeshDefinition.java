package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;

public class MeshDefinition {
    private final PartDefinition root;

    public MeshDefinition() {
        this(new PartDefinition(ImmutableList.of(), PartPose.ZERO));
    }

    private MeshDefinition(PartDefinition pRoot) {
        this.root = pRoot;
    }

    public PartDefinition getRoot() {
        this.root.setName("root");
        return this.root;
    }

    public MeshDefinition transformed(UnaryOperator<PartPose> pTransformer) {
        return new MeshDefinition(this.root.transformed(pTransformer));
    }
}