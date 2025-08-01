package net.minecraft.world.entity.npc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

public class CatSpawner implements CustomSpawner {
    private static final int TICK_DELAY = 1200;
    private int nextTick;

    @Override
    public int tick(ServerLevel pLevel, boolean pSpawnHostiles, boolean pSpawnPassives) {
        if (pSpawnPassives && pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            this.nextTick--;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick = 1200;
                Player player = pLevel.getRandomPlayer();
                if (player == null) {
                    return 0;
                } else {
                    RandomSource randomsource = pLevel.random;
                    int i = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                    int j = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                    BlockPos blockpos = player.blockPosition().offset(i, 0, j);
                    int k = 10;
                    if (!pLevel.hasChunksAt(blockpos.getX() - 10, blockpos.getZ() - 10, blockpos.getX() + 10, blockpos.getZ() + 10)) {
                        return 0;
                    } else {
                        if (SpawnPlacements.isSpawnPositionOk(EntityType.CAT, pLevel, blockpos)) {
                            if (pLevel.isCloseToVillage(blockpos, 2)) {
                                return this.spawnInVillage(pLevel, blockpos);
                            }

                            if (pLevel.structureManager().getStructureWithPieceAt(blockpos, StructureTags.CATS_SPAWN_IN).isValid()) {
                                return this.spawnInHut(pLevel, blockpos);
                            }
                        }

                        return 0;
                    }
                }
            }
        } else {
            return 0;
        }
    }

    private int spawnInVillage(ServerLevel pServerLevel, BlockPos pPos) {
        int i = 48;
        if (pServerLevel.getPoiManager().getCountInRange(p_219610_ -> p_219610_.is(PoiTypes.HOME), pPos, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
            List<Cat> list = pServerLevel.getEntitiesOfClass(Cat.class, new AABB(pPos).inflate(48.0, 8.0, 48.0));
            if (list.size() < 5) {
                return this.spawnCat(pPos, pServerLevel);
            }
        }

        return 0;
    }

    private int spawnInHut(ServerLevel pServerLevel, BlockPos pPos) {
        int i = 16;
        List<Cat> list = pServerLevel.getEntitiesOfClass(Cat.class, new AABB(pPos).inflate(16.0, 8.0, 16.0));
        return list.size() < 1 ? this.spawnCat(pPos, pServerLevel) : 0;
    }

    private int spawnCat(BlockPos pPos, ServerLevel pServerLevel) {
        Cat cat = EntityType.CAT.create(pServerLevel, EntitySpawnReason.NATURAL);
        if (cat == null) {
            return 0;
        } else {
            cat.finalizeSpawn(pServerLevel, pServerLevel.getCurrentDifficultyAt(pPos), EntitySpawnReason.NATURAL, null);
            cat.moveTo(pPos, 0.0F, 0.0F);
            pServerLevel.addFreshEntityWithPassengers(cat);
            return 1;
        }
    }
}