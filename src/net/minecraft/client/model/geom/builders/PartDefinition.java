package net.minecraft.client.model.geom.builders;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public class PartDefinition {
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();
    private String name;

    PartDefinition(List<CubeDefinition> pCubes, PartPose pPartPose) {
        this.cubes = pCubes;
        this.partPose = pPartPose;
    }

    public PartDefinition addOrReplaceChild(String pName, CubeListBuilder pCubes, PartPose pPartPose) {
        PartDefinition partdefinition = new PartDefinition(pCubes.getCubes(), pPartPose);
        return this.addOrReplaceChild(pName, partdefinition);
    }

    public PartDefinition addOrReplaceChild(String pName, PartDefinition pChild) {
        pChild.setName(pName);
        PartDefinition partdefinition = this.children.put(pName, pChild);
        if (partdefinition != null) {
            pChild.children.putAll(partdefinition.children);
        }

        return pChild;
    }

    public PartDefinition clearChild(String pName) {
        return this.addOrReplaceChild(pName, CubeListBuilder.create(), PartPose.ZERO);
    }

    public ModelPart bake(int pTexWidth, int pTexHeight) {
        Object2ObjectArrayMap<String, ModelPart> object2objectarraymap = this.children
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey, entryIn -> entryIn.getValue().bake(pTexWidth, pTexHeight), (partIn, part2In) -> partIn, Object2ObjectArrayMap::new
                )
            );
        List<ModelPart.Cube> list = this.cubes.stream().map(cubeDefIn -> cubeDefIn.bake(pTexWidth, pTexHeight)).toList();
        ModelPart modelpart = new ModelPart(list, object2objectarraymap);
        modelpart.setInitialPose(this.partPose);
        modelpart.loadPose(this.partPose);
        modelpart.setName(this.name);
        return modelpart;
    }

    public PartDefinition getChild(String pName) {
        return this.children.get(pName);
    }

    public Set<Entry<String, PartDefinition>> getChildren() {
        return this.children.entrySet();
    }

    public PartDefinition transformed(UnaryOperator<PartPose> pTransformer) {
        PartDefinition partdefinition = new PartDefinition(this.cubes, pTransformer.apply(this.partPose));
        partdefinition.children.putAll(this.children);
        return partdefinition;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}