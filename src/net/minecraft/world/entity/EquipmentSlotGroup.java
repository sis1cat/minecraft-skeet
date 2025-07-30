package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum EquipmentSlotGroup implements StringRepresentable {
    ANY(0, "any", p_335585_ -> true),
    MAINHAND(1, "mainhand", EquipmentSlot.MAINHAND),
    OFFHAND(2, "offhand", EquipmentSlot.OFFHAND),
    HAND(3, "hand", p_330375_ -> p_330375_.getType() == EquipmentSlot.Type.HAND),
    FEET(4, "feet", EquipmentSlot.FEET),
    LEGS(5, "legs", EquipmentSlot.LEGS),
    CHEST(6, "chest", EquipmentSlot.CHEST),
    HEAD(7, "head", EquipmentSlot.HEAD),
    ARMOR(8, "armor", EquipmentSlot::isArmor),
    BODY(9, "body", EquipmentSlot.BODY);

    public static final IntFunction<EquipmentSlotGroup> BY_ID = ByIdMap.continuous(
        p_331450_ -> p_331450_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
    );
    public static final Codec<EquipmentSlotGroup> CODEC = StringRepresentable.fromEnum(EquipmentSlotGroup::values);
    public static final StreamCodec<ByteBuf, EquipmentSlotGroup> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_330886_ -> p_330886_.id);
    private final int id;
    private final String key;
    private final Predicate<EquipmentSlot> predicate;

    private EquipmentSlotGroup(final int pId, final String pKey, final Predicate<EquipmentSlot> pPredicate) {
        this.id = pId;
        this.key = pKey;
        this.predicate = pPredicate;
    }

    private EquipmentSlotGroup(final int pId, final String pKey, final EquipmentSlot pSlot) {
        this(pId, pKey, p_330757_ -> p_330757_ == pSlot);
    }

    public static EquipmentSlotGroup bySlot(EquipmentSlot pSlot) {
        return switch (pSlot) {
            case MAINHAND -> MAINHAND;
            case OFFHAND -> OFFHAND;
            case FEET -> FEET;
            case LEGS -> LEGS;
            case CHEST -> CHEST;
            case HEAD -> HEAD;
            case BODY -> BODY;
        };
    }

    @Override
    public String getSerializedName() {
        return this.key;
    }

    public boolean test(EquipmentSlot pSlot) {
        return this.predicate.test(pSlot);
    }
}