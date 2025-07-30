package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum Rotation implements StringRepresentable {
    NONE("none", OctahedralGroup.IDENTITY),
    CLOCKWISE_90("clockwise_90", OctahedralGroup.ROT_90_Y_NEG),
    CLOCKWISE_180("180", OctahedralGroup.ROT_180_FACE_XZ),
    COUNTERCLOCKWISE_90("counterclockwise_90", OctahedralGroup.ROT_90_Y_POS);

    public static final Codec<Rotation> CODEC = StringRepresentable.fromEnum(Rotation::values);
    private final String id;
    private final OctahedralGroup rotation;

    private Rotation(final String pId, final OctahedralGroup pRotation) {
        this.id = pId;
        this.rotation = pRotation;
    }

    public Rotation getRotated(Rotation pRotation) {
        return switch (pRotation) {
            case CLOCKWISE_90 -> {
                switch (this) {
                    case NONE:
                        yield CLOCKWISE_90;
                    case CLOCKWISE_90:
                        yield CLOCKWISE_180;
                    case CLOCKWISE_180:
                        yield COUNTERCLOCKWISE_90;
                    case COUNTERCLOCKWISE_90:
                        yield NONE;
                    default:
                        throw new MatchException(null, null);
                }
            }
            case CLOCKWISE_180 -> {
                switch (this) {
                    case NONE:
                        yield CLOCKWISE_180;
                    case CLOCKWISE_90:
                        yield COUNTERCLOCKWISE_90;
                    case CLOCKWISE_180:
                        yield NONE;
                    case COUNTERCLOCKWISE_90:
                        yield CLOCKWISE_90;
                    default:
                        throw new MatchException(null, null);
                }
            }
            case COUNTERCLOCKWISE_90 -> {
                switch (this) {
                    case NONE:
                        yield COUNTERCLOCKWISE_90;
                    case CLOCKWISE_90:
                        yield NONE;
                    case CLOCKWISE_180:
                        yield CLOCKWISE_90;
                    case COUNTERCLOCKWISE_90:
                        yield CLOCKWISE_180;
                    default:
                        throw new MatchException(null, null);
                }
            }
            default -> this;
        };
    }

    public OctahedralGroup rotation() {
        return this.rotation;
    }

    public Direction rotate(Direction pFacing) {
        if (pFacing.getAxis() == Direction.Axis.Y) {
            return pFacing;
        } else {
            return switch (this) {
                case CLOCKWISE_90 -> pFacing.getClockWise();
                case CLOCKWISE_180 -> pFacing.getOpposite();
                case COUNTERCLOCKWISE_90 -> pFacing.getCounterClockWise();
                default -> pFacing;
            };
        }
    }

    public int rotate(int pRotation, int pPositionCount) {
        return switch (this) {
            case CLOCKWISE_90 -> (pRotation + pPositionCount / 4) % pPositionCount;
            case CLOCKWISE_180 -> (pRotation + pPositionCount / 2) % pPositionCount;
            case COUNTERCLOCKWISE_90 -> (pRotation + pPositionCount * 3 / 4) % pPositionCount;
            default -> pRotation;
        };
    }

    public static Rotation getRandom(RandomSource pRandom) {
        return Util.getRandom(values(), pRandom);
    }

    public static List<Rotation> getShuffled(RandomSource pRandom) {
        return Util.shuffledCopy(values(), pRandom);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}