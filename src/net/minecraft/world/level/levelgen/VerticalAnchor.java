package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionType;

public interface VerticalAnchor {
    Codec<VerticalAnchor> CODEC = Codec.xor(
            VerticalAnchor.Absolute.CODEC, Codec.xor(VerticalAnchor.AboveBottom.CODEC, VerticalAnchor.BelowTop.CODEC)
        )
        .xmap(VerticalAnchor::merge, VerticalAnchor::split);
    VerticalAnchor BOTTOM = aboveBottom(0);
    VerticalAnchor TOP = belowTop(0);

    static VerticalAnchor absolute(int pValue) {
        return new VerticalAnchor.Absolute(pValue);
    }

    static VerticalAnchor aboveBottom(int pValue) {
        return new VerticalAnchor.AboveBottom(pValue);
    }

    static VerticalAnchor belowTop(int pValue) {
        return new VerticalAnchor.BelowTop(pValue);
    }

    static VerticalAnchor bottom() {
        return BOTTOM;
    }

    static VerticalAnchor top() {
        return TOP;
    }

    private static VerticalAnchor merge(Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> pAnchor) {
        return pAnchor.map(Function.identity(), Either::unwrap);
    }

    private static Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> split(VerticalAnchor pAnchor) {
        return pAnchor instanceof VerticalAnchor.Absolute
            ? Either.left((VerticalAnchor.Absolute)pAnchor)
            : Either.right(
                pAnchor instanceof VerticalAnchor.AboveBottom
                    ? Either.left((VerticalAnchor.AboveBottom)pAnchor)
                    : Either.right((VerticalAnchor.BelowTop)pAnchor)
            );
    }

    int resolveY(WorldGenerationContext pContext);

    public static record AboveBottom(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.AboveBottom> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("above_bottom")
            .xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor.AboveBottom::offset)
            .codec();

        @Override
        public int resolveY(WorldGenerationContext p_158942_) {
            return p_158942_.getMinGenY() + this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " above bottom";
        }
    }

    public static record Absolute(int y) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.Absolute> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("absolute")
            .xmap(VerticalAnchor.Absolute::new, VerticalAnchor.Absolute::y)
            .codec();

        @Override
        public int resolveY(WorldGenerationContext p_158949_) {
            return this.y;
        }

        @Override
        public String toString() {
            return this.y + " absolute";
        }
    }

    public static record BelowTop(int offset) implements VerticalAnchor {
        public static final Codec<VerticalAnchor.BelowTop> CODEC = Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
            .fieldOf("below_top")
            .xmap(VerticalAnchor.BelowTop::new, VerticalAnchor.BelowTop::offset)
            .codec();

        @Override
        public int resolveY(WorldGenerationContext p_158956_) {
            return p_158956_.getGenDepth() - 1 + p_158956_.getMinGenY() - this.offset;
        }

        @Override
        public String toString() {
            return this.offset + " below top";
        }
    }
}