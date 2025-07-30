package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.optifine.reflect.Reflector;
import org.slf4j.Logger;

public abstract class DistanceManager {
    static final Logger LOGGER = LogUtils.getLogger();
    static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
    private final DistanceManager.ChunkTicketTracker ticketTracker = new DistanceManager.ChunkTicketTracker();
    private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
    private final TickingTracker tickingTicketsTracker = new TickingTracker();
    private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(64);
    final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet<>();
    final ThrottlingChunkTaskDispatcher ticketDispatcher;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private long ticketTickCounter;
    private int simulationDistance = 10;
    final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> forcedTickets = new Long2ObjectOpenHashMap<>();

    protected DistanceManager(Executor pDispatcher, Executor pMainThreadExecutor) {
        TaskScheduler<Runnable> taskscheduler = TaskScheduler.wrapExecutor("player ticket throttler", pMainThreadExecutor);
        this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(taskscheduler, pDispatcher, 4);
        this.mainThreadExecutor = pMainThreadExecutor;
    }

    protected void purgeStaleTickets() {
        this.ticketTickCounter++;
        ObjectIterator<Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext()) {
            Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            Iterator<Ticket<?>> iterator = entry.getValue().iterator();
            boolean flag = false;

            while (iterator.hasNext()) {
                Ticket<?> ticket = iterator.next();
                if (ticket.timedOut(this.ticketTickCounter)) {
                    iterator.remove();
                    flag = true;
                    this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket);
                }
            }

            if (flag) {
                this.ticketTracker.update(entry.getLongKey(), getTicketLevelAt(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectiterator.remove();
            }
        }
    }

    private static int getTicketLevelAt(SortedArraySet<Ticket<?>> pTickets) {
        return !pTickets.isEmpty() ? pTickets.first().getTicketLevel() : ChunkLevel.MAX_LEVEL + 1;
    }

    protected abstract boolean isChunkToRemove(long pChunkPos);

    @Nullable
    protected abstract ChunkHolder getChunk(long pChunkPos);

    @Nullable
    protected abstract ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel);

    public boolean runAllUpdates(ChunkMap pChunkMap) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.tickingTicketsTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int i = Integer.MAX_VALUE - this.ticketTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (flag) {
        }

        if (!this.chunksToUpdateFutures.isEmpty()) {
            for (ChunkHolder chunkholder1 : this.chunksToUpdateFutures) {
                chunkholder1.updateHighestAllowedStatus(pChunkMap);
            }

            for (ChunkHolder chunkholder2 : this.chunksToUpdateFutures) {
                chunkholder2.updateFutures(pChunkMap, this.mainThreadExecutor);
            }

            this.chunksToUpdateFutures.clear();
            return true;
        } else {
            if (!this.ticketsToRelease.isEmpty()) {
                LongIterator longiterator = this.ticketsToRelease.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getTickets(j).stream().anyMatch(ticketIn -> ticketIn.getType() == TicketType.PLAYER)) {
                        ChunkHolder chunkholder = pChunkMap.getUpdatingChunkIfPresent(j);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                        completablefuture.thenAccept(voidIn -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(j, () -> {
                                }, false)));
                    }
                }

                this.ticketsToRelease.clear();
            }

            return flag;
        }
    }

    void addTicket(long pChunkPos, Ticket<?> pTicket) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTickets(pChunkPos);
        int i = getTicketLevelAt(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.addOrGet(pTicket);
        ticket.setCreatedTick(this.ticketTickCounter);
        if (pTicket.getTicketLevel() < i) {
            this.ticketTracker.update(pChunkPos, pTicket.getTicketLevel(), true);
        }

        if (Reflector.callBoolean(pTicket, Reflector.ForgeTicket_isForceTicks)) {
            SortedArraySet<Ticket<?>> sortedarrayset1 = this.forcedTickets.computeIfAbsent(pChunkPos, e -> SortedArraySet.create(4));
            sortedarrayset1.addOrGet(ticket);
        }
    }

    void removeTicket(long pChunkPos, Ticket<?> pTicket) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTickets(pChunkPos);
        if (sortedarrayset.remove(pTicket)) {
        }

        if (sortedarrayset.isEmpty()) {
            this.tickets.remove(pChunkPos);
        }

        this.ticketTracker.update(pChunkPos, getTicketLevelAt(sortedarrayset), false);
        if (Reflector.callBoolean(pTicket, Reflector.ForgeTicket_isForceTicks)) {
            SortedArraySet<Ticket<?>> sortedarrayset1 = this.forcedTickets.get(pChunkPos);
            if (sortedarrayset1 != null) {
                sortedarrayset1.remove(pTicket);
            }
        }
    }

    public <T> void addTicket(TicketType<T> pType, ChunkPos pPos, int pLevel, T pValue) {
        this.addTicket(pPos.toLong(), new Ticket<>(pType, pLevel, pValue));
    }

    public <T> void removeTicket(TicketType<T> pType, ChunkPos pPos, int pLevel, T pValue) {
        Ticket<T> ticket = new Ticket<>(pType, pLevel, pValue);
        this.removeTicket(pPos.toLong(), ticket);
    }

    public <T> void addRegionTicket(TicketType<T> pType, ChunkPos pPos, int pDistance, T pValue) {
        this.addRegionTicket(pType, pPos, pDistance, pValue, false);
    }

    public <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value, boolean forceTicks) {
        Ticket<T> ticket = new Ticket<>(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - distance, value);
        Reflector.setFieldValue(ticket, Reflector.ForgeTicket_forceTicks, forceTicks);
        long i = pos.toLong();
        this.addTicket(i, ticket);
        this.tickingTicketsTracker.addTicket(i, ticket);
    }

    public <T> void removeRegionTicket(TicketType<T> pType, ChunkPos pPos, int pDistance, T pValue) {
        this.removeRegionTicket(pType, pPos, pDistance, pValue, false);
    }

    public <T> void removeRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value, boolean forceTicks) {
        Ticket<T> ticket = new Ticket<>(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - distance, value);
        Reflector.setFieldValue(ticket, Reflector.ForgeTicket_forceTicks, forceTicks);
        long i = pos.toLong();
        this.removeTicket(i, ticket);
        this.tickingTicketsTracker.removeTicket(i, ticket);
    }

    private SortedArraySet<Ticket<?>> getTickets(long pChunkPos) {
        return this.tickets.computeIfAbsent(pChunkPos, posIn -> SortedArraySet.create(4));
    }

    protected void updateChunkForced(ChunkPos pPos, boolean pAdd) {
        Ticket<ChunkPos> ticket = new Ticket<>(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL, pPos);
        long i = pPos.toLong();
        if (pAdd) {
            this.addTicket(i, ticket);
            this.tickingTicketsTracker.addTicket(i, ticket);
        } else {
            this.removeTicket(i, ticket);
            this.tickingTicketsTracker.removeTicket(i, ticket);
        }
    }

    public void addPlayer(SectionPos pSectionPos, ServerPlayer pPlayer) {
        ChunkPos chunkpos = pSectionPos.chunk();
        long i = chunkpos.toLong();
        this.playersPerChunk.computeIfAbsent(i, posIn -> new ObjectOpenHashSet<>()).add(pPlayer);
        this.naturalSpawnChunkCounter.update(i, 0, true);
        this.playerTicketManager.update(i, 0, true);
        this.tickingTicketsTracker.addTicket(TicketType.PLAYER, chunkpos, this.getPlayerTicketLevel(), chunkpos);
    }

    public void removePlayer(SectionPos pSectionPos, ServerPlayer pPlayer) {
        ChunkPos chunkpos = pSectionPos.chunk();
        long i = chunkpos.toLong();
        ObjectSet<ServerPlayer> objectset = this.playersPerChunk.get(i);
        objectset.remove(pPlayer);
        if (objectset.isEmpty()) {
            this.playersPerChunk.remove(i);
            this.naturalSpawnChunkCounter.update(i, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(i, Integer.MAX_VALUE, false);
            this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, chunkpos, this.getPlayerTicketLevel(), chunkpos);
        }
    }

    private int getPlayerTicketLevel() {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long pChunkPos) {
        return ChunkLevel.isEntityTicking(this.tickingTicketsTracker.getLevel(pChunkPos));
    }

    public boolean inBlockTickingRange(long pChunkPos) {
        return ChunkLevel.isBlockTicking(this.tickingTicketsTracker.getLevel(pChunkPos));
    }

    protected String getTicketDebugString(long pChunkPos) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.tickets.get(pChunkPos);
        return sortedarrayset != null && !sortedarrayset.isEmpty() ? sortedarrayset.first().toString() : "no_ticket";
    }

    protected void updatePlayerTickets(int pViewDistance) {
        this.playerTicketManager.updateViewDistance(pViewDistance);
    }

    public void updateSimulationDistance(int pSimulationDistance) {
        if (pSimulationDistance != this.simulationDistance) {
            this.simulationDistance = pSimulationDistance;
            this.tickingTicketsTracker.replacePlayerTicketsLevel(this.getPlayerTicketLevel());
        }
    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public boolean hasPlayersNearby(long pChunkPos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.containsKey(pChunkPos);
    }

    public LongIterator getSpawnCandidateChunks() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
    }

    public String getDebugStatus() {
        return this.ticketDispatcher.getDebugStatus();
    }

    private void dumpTickets(String pFilename) {
        try (FileOutputStream fileoutputstream = new FileOutputStream(new File(pFilename))) {
            for (Entry<SortedArraySet<Ticket<?>>> entry : this.tickets.long2ObjectEntrySet()) {
                ChunkPos chunkpos = new ChunkPos(entry.getLongKey());

                for (Ticket<?> ticket : entry.getValue()) {
                    fileoutputstream.write(
                        (chunkpos.x + "\t" + chunkpos.z + "\t" + ticket.getType() + "\t" + ticket.getTicketLevel() + "\t\n")
                            .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        } catch (IOException ioexception1) {
            LOGGER.error("Failed to dump tickets to {}", pFilename, ioexception1);
        }
    }

    @VisibleForTesting
    TickingTracker tickingTracker() {
        return this.tickingTicketsTracker;
    }

    public LongSet getTickingChunks() {
        return this.tickingTicketsTracker.getTickingChunks();
    }

    public void removeTicketsOnClosing() {
        ImmutableSet<TicketType<?>> immutableset = ImmutableSet.of(TicketType.UNKNOWN);
        ObjectIterator<Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext()) {
            Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            Iterator<Ticket<?>> iterator = entry.getValue().iterator();
            boolean flag = false;

            while (iterator.hasNext()) {
                Ticket<?> ticket = iterator.next();
                if (!immutableset.contains(ticket.getType())) {
                    iterator.remove();
                    flag = true;
                    this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket);
                }
            }

            if (flag) {
                this.ticketTracker.update(entry.getLongKey(), getTicketLevelAt(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectiterator.remove();
            }
        }
    }

    public boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    public boolean shouldForceTicks(long chunkPos) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.forcedTickets.get(chunkPos);
        return sortedarrayset != null && !sortedarrayset.isEmpty();
    }

    class ChunkTicketTracker extends ChunkTracker {
        private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;

        public ChunkTicketTracker() {
            super(MAX_LEVEL + 1, 256, 256);
        }

        @Override
        protected int getLevelFromSource(long pPos) {
            SortedArraySet<Ticket<?>> sortedarrayset = DistanceManager.this.tickets.get(pPos);
            if (sortedarrayset == null) {
                return Integer.MAX_VALUE;
            } else {
                return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.first().getTicketLevel();
            }
        }

        @Override
        protected int getLevel(long pSectionPos) {
            if (!DistanceManager.this.isChunkToRemove(pSectionPos)) {
                ChunkHolder chunkholder = DistanceManager.this.getChunk(pSectionPos);
                if (chunkholder != null) {
                    return chunkholder.getTicketLevel();
                }
            }

            return MAX_LEVEL;
        }

        @Override
        protected void setLevel(long pSectionPos, int pLevel) {
            ChunkHolder chunkholder = DistanceManager.this.getChunk(pSectionPos);
            int i = chunkholder == null ? MAX_LEVEL : chunkholder.getTicketLevel();
            if (i != pLevel) {
                chunkholder = DistanceManager.this.updateChunkScheduling(pSectionPos, pLevel, chunkholder, i);
                if (chunkholder != null) {
                    DistanceManager.this.chunksToUpdateFutures.add(chunkholder);
                }
            }
        }

        public int runDistanceUpdates(int pToUpdateCount) {
            return this.runUpdates(pToUpdateCount);
        }
    }

    class FixedPlayerDistanceChunkTracker extends ChunkTracker {
        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(final int pMaxDistance) {
            super(pMaxDistance + 2, 2048, 2048);
            this.maxDistance = pMaxDistance;
            this.chunks.defaultReturnValue((byte)(pMaxDistance + 2));
        }

        @Override
        protected int getLevel(long pSectionPos) {
            return this.chunks.get(pSectionPos);
        }

        @Override
        protected void setLevel(long pSectionPos, int pLevel) {
            byte b0;
            if (pLevel > this.maxDistance) {
                b0 = this.chunks.remove(pSectionPos);
            } else {
                b0 = this.chunks.put(pSectionPos, (byte)pLevel);
            }

            this.onLevelChange(pSectionPos, b0, pLevel);
        }

        protected void onLevelChange(long pChunkPos, int pOldLevel, int pNewLevel) {
        }

        @Override
        protected int getLevelFromSource(long pPos) {
            return this.havePlayer(pPos) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long pChunkPos) {
            ObjectSet<ServerPlayer> objectset = DistanceManager.this.playersPerChunk.get(pChunkPos);
            return objectset != null && !objectset.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }

        private void dumpChunks(String pFilename) {
            try (FileOutputStream fileoutputstream = new FileOutputStream(new File(pFilename))) {
                for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet()) {
                    ChunkPos chunkpos = new ChunkPos(entry.getLongKey());
                    String s = Byte.toString(entry.getByteValue());
                    fileoutputstream.write((chunkpos.x + "\t" + chunkpos.z + "\t" + s + "\n").getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException ioexception1) {
                DistanceManager.LOGGER.error("Failed to dump chunks to {}", pFilename, ioexception1);
            }
        }
    }

    class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(final int p_140910_) {
            super(p_140910_);
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(p_140910_ + 2);
        }

        @Override
        protected void onLevelChange(long pChunkPos, int pOldLevel, int pNewLevel) {
            this.toUpdate.add(pChunkPos);
        }

        public void updateViewDistance(int pViewDistance) {
            for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet()) {
                byte b0 = entry.getByteValue();
                long i = entry.getLongKey();
                this.onLevelChange(i, b0, this.haveTicketFor(b0), b0 <= pViewDistance);
            }

            this.viewDistance = pViewDistance;
        }

        private void onLevelChange(long pChunkPos, int pLevel, boolean pHadTicket, boolean pHasTicket) {
            if (pHadTicket != pHasTicket) {
                Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, DistanceManager.PLAYER_TICKET_LEVEL, new ChunkPos(pChunkPos));
                if (pHasTicket) {
                    DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                            if (this.haveTicketFor(this.getLevel(pChunkPos))) {
                                DistanceManager.this.addTicket(pChunkPos, ticket);
                                DistanceManager.this.ticketsToRelease.add(pChunkPos);
                            } else {
                                DistanceManager.this.ticketDispatcher.release(pChunkPos, () -> {
                                }, false);
                            }
                        }), pChunkPos, () -> pLevel);
                } else {
                    DistanceManager.this.ticketDispatcher
                        .release(pChunkPos, () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.removeTicket(pChunkPos, ticket)), true);
                }
            }
        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator longiterator = this.toUpdate.iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    int j = this.queueLevels.get(i);
                    int k = this.getLevel(i);
                    if (j != k) {
                        DistanceManager.this.ticketDispatcher.onLevelChange(new ChunkPos(i), () -> this.queueLevels.get(i), k, levelIn -> {
                            if (levelIn >= this.queueLevels.defaultReturnValue()) {
                                this.queueLevels.remove(i);
                            } else {
                                this.queueLevels.put(i, levelIn);
                            }
                        });
                        this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
                    }
                }

                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(int pLevel) {
            return pLevel <= this.viewDistance;
        }
    }
}