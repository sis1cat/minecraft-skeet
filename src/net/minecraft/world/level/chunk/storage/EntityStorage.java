package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_TAG = "Entities";
    private static final String POSITION_TAG = "Position";
    private final ServerLevel level;
    private final SimpleRegionStorage simpleRegionStorage;
    private final LongSet emptyChunks = new LongOpenHashSet();
    private final ConsecutiveExecutor entityDeserializerQueue;

    public EntityStorage(SimpleRegionStorage pSimpleRegionStorage, ServerLevel pLevel, Executor pExecutor) {
        this.simpleRegionStorage = pSimpleRegionStorage;
        this.level = pLevel;
        this.entityDeserializerQueue = new ConsecutiveExecutor(pExecutor, "entity-deserializer");
    }

    @Override
    public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos p_156551_) {
        if (this.emptyChunks.contains(p_156551_.toLong())) {
            return CompletableFuture.completedFuture(emptyChunk(p_156551_));
        } else {
            CompletableFuture<Optional<CompoundTag>> completablefuture = this.simpleRegionStorage.read(p_156551_);
            this.reportLoadFailureIfPresent(completablefuture, p_156551_);
            return completablefuture.thenApplyAsync(p_341886_ -> {
                if (p_341886_.isEmpty()) {
                    this.emptyChunks.add(p_156551_.toLong());
                    return emptyChunk(p_156551_);
                } else {
                    try {
                        ChunkPos chunkpos = readChunkPos(p_341886_.get());
                        if (!Objects.equals(p_156551_, chunkpos)) {
                            LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", p_156551_, p_156551_, chunkpos);
                            this.level.getServer().reportMisplacedChunk(chunkpos, p_156551_, this.simpleRegionStorage.storageInfo());
                        }
                    } catch (Exception exception) {
                        LOGGER.warn("Failed to parse chunk {} position info", p_156551_, exception);
                        this.level.getServer().reportChunkLoadFailure(exception, this.simpleRegionStorage.storageInfo(), p_156551_);
                    }

                    CompoundTag compoundtag = this.simpleRegionStorage.upgradeChunkTag(p_341886_.get(), -1);
                    ListTag listtag = compoundtag.getList("Entities", 10);
                    List<Entity> list = EntityType.loadEntitiesRecursive(listtag, this.level, EntitySpawnReason.LOAD).collect(ImmutableList.toImmutableList());
                    return new ChunkEntities<>(p_156551_, list);
                }
            }, this.entityDeserializerQueue::schedule);
        }
    }

    private static ChunkPos readChunkPos(CompoundTag pTag) {
        int[] aint = pTag.getIntArray("Position");
        return new ChunkPos(aint[0], aint[1]);
    }

    private static void writeChunkPos(CompoundTag pTag, ChunkPos pPos) {
        pTag.put("Position", new IntArrayTag(new int[]{pPos.x, pPos.z}));
    }

    private static ChunkEntities<Entity> emptyChunk(ChunkPos pPos) {
        return new ChunkEntities<>(pPos, ImmutableList.of());
    }

    @Override
    public void storeEntities(ChunkEntities<Entity> p_156559_) {
        ChunkPos chunkpos = p_156559_.getPos();
        if (p_156559_.isEmpty()) {
            if (this.emptyChunks.add(chunkpos.toLong())) {
                this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, null), chunkpos);
            }
        } else {
            ListTag listtag = new ListTag();
            p_156559_.getEntities().forEach(p_156567_ -> {
                CompoundTag compoundtag1 = new CompoundTag();
                if (p_156567_.save(compoundtag1)) {
                    listtag.add(compoundtag1);
                }
            });
            CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
            compoundtag.put("Entities", listtag);
            writeChunkPos(compoundtag, chunkpos);
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, compoundtag), chunkpos);
            this.emptyChunks.remove(chunkpos.toLong());
        }
    }

    private void reportSaveFailureIfPresent(CompletableFuture<?> pFuture, ChunkPos pPos) {
        pFuture.exceptionally(p_341884_ -> {
            LOGGER.error("Failed to store entity chunk {}", pPos, p_341884_);
            this.level.getServer().reportChunkSaveFailure(p_341884_, this.simpleRegionStorage.storageInfo(), pPos);
            return null;
        });
    }

    private void reportLoadFailureIfPresent(CompletableFuture<?> pFuture, ChunkPos pPos) {
        pFuture.exceptionally(p_341888_ -> {
            LOGGER.error("Failed to load entity chunk {}", pPos, p_341888_);
            this.level.getServer().reportChunkLoadFailure(p_341888_, this.simpleRegionStorage.storageInfo(), pPos);
            return null;
        });
    }

    @Override
    public void flush(boolean p_182487_) {
        this.simpleRegionStorage.synchronize(p_182487_).join();
        this.entityDeserializerQueue.runAll();
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }
}