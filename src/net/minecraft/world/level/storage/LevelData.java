package net.minecraft.world.level.storage;

import java.util.Locale;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelHeightAccessor;

public interface LevelData {
    BlockPos getSpawnPos();

    float getSpawnAngle();

    long getGameTime();

    long getDayTime();

    boolean isThundering();

    boolean isRaining();

    void setRaining(boolean pRaining);

    boolean isHardcore();

    Difficulty getDifficulty();

    boolean isDifficultyLocked();

    default void fillCrashReportCategory(CrashReportCategory pCrashReportCategory, LevelHeightAccessor pLevel) {
        pCrashReportCategory.setDetail("Level spawn location", () -> CrashReportCategory.formatLocation(pLevel, this.getSpawnPos()));
        pCrashReportCategory.setDetail("Level time", () -> String.format(Locale.ROOT, "%d game time, %d day time", this.getGameTime(), this.getDayTime()));
    }
}