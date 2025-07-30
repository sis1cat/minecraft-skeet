package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public record ScoreboardValue(ScoreboardNameProvider target, String score, float scale) implements NumberProvider {
    public static final MapCodec<ScoreboardValue> CODEC = RecordCodecBuilder.mapCodec(
        p_297867_ -> p_297867_.group(
                    ScoreboardNameProviders.CODEC.fieldOf("target").forGetter(ScoreboardValue::target),
                    Codec.STRING.fieldOf("score").forGetter(ScoreboardValue::score),
                    Codec.FLOAT.fieldOf("scale").orElse(1.0F).forGetter(ScoreboardValue::scale)
                )
                .apply(p_297867_, ScoreboardValue::new)
    );

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.SCORE;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.target.getReferencedContextParams();
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget pEntityTarget, String pScore) {
        return fromScoreboard(pEntityTarget, pScore, 1.0F);
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget pEntityTarget, String pScore, float pScale) {
        return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(pEntityTarget), pScore, pScale);
    }

    @Override
    public float getFloat(LootContext p_165758_) {
        ScoreHolder scoreholder = this.target.getScoreHolder(p_165758_);
        if (scoreholder == null) {
            return 0.0F;
        } else {
            Scoreboard scoreboard = p_165758_.getLevel().getScoreboard();
            Objective objective = scoreboard.getObjective(this.score);
            if (objective == null) {
                return 0.0F;
            } else {
                ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
                return readonlyscoreinfo == null ? 0.0F : (float)readonlyscoreinfo.value() * this.scale;
            }
        }
    }
}