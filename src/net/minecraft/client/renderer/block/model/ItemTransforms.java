package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ItemTransforms(
    ItemTransform thirdPersonLeftHand,
    ItemTransform thirdPersonRightHand,
    ItemTransform firstPersonLeftHand,
    ItemTransform firstPersonRightHand,
    ItemTransform head,
    ItemTransform gui,
    ItemTransform ground,
    ItemTransform fixed
) {
    public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms(
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM,
        ItemTransform.NO_TRANSFORM
    );

    public ItemTransform getTransform(ItemDisplayContext pDisplayContext) {
        return switch (pDisplayContext) {
            case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
            case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
            case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
            case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
            case HEAD -> this.head;
            case GUI -> this.gui;
            case GROUND -> this.ground;
            case FIXED -> this.fixed;
            default -> ItemTransform.NO_TRANSFORM;
        };
    }

    @OnlyIn(Dist.CLIENT)
    protected static class Deserializer implements JsonDeserializer<ItemTransforms> {
        public ItemTransforms deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            ItemTransform itemtransform = this.getTransform(pContext, jsonobject, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            ItemTransform itemtransform1 = this.getTransform(pContext, jsonobject, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
            if (itemtransform1 == ItemTransform.NO_TRANSFORM) {
                itemtransform1 = itemtransform;
            }

            ItemTransform itemtransform2 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            ItemTransform itemtransform3 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            if (itemtransform3 == ItemTransform.NO_TRANSFORM) {
                itemtransform3 = itemtransform2;
            }

            ItemTransform itemtransform4 = this.getTransform(pContext, jsonobject, ItemDisplayContext.HEAD);
            ItemTransform itemtransform5 = this.getTransform(pContext, jsonobject, ItemDisplayContext.GUI);
            ItemTransform itemtransform6 = this.getTransform(pContext, jsonobject, ItemDisplayContext.GROUND);
            ItemTransform itemtransform7 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIXED);
            return new ItemTransforms(
                itemtransform1, itemtransform, itemtransform3, itemtransform2, itemtransform4, itemtransform5, itemtransform6, itemtransform7
            );
        }

        private ItemTransform getTransform(JsonDeserializationContext pDeserializationContext, JsonObject pJson, ItemDisplayContext pDisplayContext) {
            String s = pDisplayContext.getSerializedName();
            return pJson.has(s) ? pDeserializationContext.deserialize(pJson.get(s), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
        }
    }
}