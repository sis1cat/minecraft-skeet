package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class ForcedChunksSavedData extends SavedData {
    public static final String FILE_ID = "chunks";
    private static final String TAG_FORCED = "Forced";
    private final LongSet chunks;

    public static SavedData.Factory<ForcedChunksSavedData> factory() {
        return new SavedData.Factory<>(ForcedChunksSavedData::new, ForcedChunksSavedData::load, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
    }

    private ForcedChunksSavedData(LongSet pChunks) {
        this.chunks = pChunks;
    }

    public ForcedChunksSavedData() {
        this(new LongOpenHashSet());
    }

    public static ForcedChunksSavedData load(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        return new ForcedChunksSavedData(new LongOpenHashSet(pTag.getLongArray("Forced")));
    }

    @Override
    public CompoundTag save(CompoundTag p_46120_, HolderLookup.Provider p_329035_) {
        p_46120_.putLongArray("Forced", this.chunks.toLongArray());
        return p_46120_;
    }

    public LongSet getChunks() {
        return this.chunks;
    }
}