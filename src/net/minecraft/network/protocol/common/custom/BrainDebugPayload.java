package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public record BrainDebugPayload(BrainDebugPayload.BrainDump brainDump) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BrainDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
        BrainDebugPayload::write, BrainDebugPayload::new
    );
    public static final CustomPacketPayload.Type<BrainDebugPayload> TYPE = CustomPacketPayload.createType("debug/brain");

    private BrainDebugPayload(FriendlyByteBuf pBuffer) {
        this(new BrainDebugPayload.BrainDump(pBuffer));
    }

    private void write(FriendlyByteBuf pBuffer) {
        this.brainDump.write(pBuffer);
    }

    @Override
    public CustomPacketPayload.Type<BrainDebugPayload> type() {
        return TYPE;
    }

    public static record BrainDump(
        UUID uuid,
        int id,
        String name,
        String profession,
        int xp,
        float health,
        float maxHealth,
        Vec3 pos,
        String inventory,
        @Nullable Path path,
        boolean wantsGolem,
        int angerLevel,
        List<String> activities,
        List<String> behaviors,
        List<String> memories,
        List<String> gossips,
        Set<BlockPos> pois,
        Set<BlockPos> potentialPois
    ) {
        public BrainDump(FriendlyByteBuf pBuffer) {
            this(
                pBuffer.readUUID(),
                pBuffer.readInt(),
                pBuffer.readUtf(),
                pBuffer.readUtf(),
                pBuffer.readInt(),
                pBuffer.readFloat(),
                pBuffer.readFloat(),
                pBuffer.readVec3(),
                pBuffer.readUtf(),
                pBuffer.readNullable(Path::createFromStream),
                pBuffer.readBoolean(),
                pBuffer.readInt(),
                pBuffer.readList(FriendlyByteBuf::readUtf),
                pBuffer.readList(FriendlyByteBuf::readUtf),
                pBuffer.readList(FriendlyByteBuf::readUtf),
                pBuffer.readList(FriendlyByteBuf::readUtf),
                pBuffer.readCollection(HashSet::new, BlockPos.STREAM_CODEC),
                pBuffer.readCollection(HashSet::new, BlockPos.STREAM_CODEC)
            );
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeUUID(this.uuid);
            pBuffer.writeInt(this.id);
            pBuffer.writeUtf(this.name);
            pBuffer.writeUtf(this.profession);
            pBuffer.writeInt(this.xp);
            pBuffer.writeFloat(this.health);
            pBuffer.writeFloat(this.maxHealth);
            pBuffer.writeVec3(this.pos);
            pBuffer.writeUtf(this.inventory);
            pBuffer.writeNullable(this.path, (p_297936_, p_301045_) -> p_301045_.writeToStream(p_297936_));
            pBuffer.writeBoolean(this.wantsGolem);
            pBuffer.writeInt(this.angerLevel);
            pBuffer.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.pois, BlockPos.STREAM_CODEC);
            pBuffer.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
        }

        public boolean hasPoi(BlockPos pPos) {
            return this.pois.contains(pPos);
        }

        public boolean hasPotentialPoi(BlockPos pPos) {
            return this.potentialPois.contains(pPos);
        }
    }
}