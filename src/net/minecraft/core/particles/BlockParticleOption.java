package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockParticleOption implements ParticleOptions {
    private static final Codec<BlockState> BLOCK_STATE_CODEC = Codec.withAlternative(BlockState.CODEC, BuiltInRegistries.BLOCK.byNameCodec(), Block::defaultBlockState);
    private final ParticleType<BlockParticleOption> type;
    private final BlockState state;

    public static MapCodec<BlockParticleOption> codec(ParticleType<BlockParticleOption> pParticleType) {
        return BLOCK_STATE_CODEC.xmap(p_123638_ -> new BlockParticleOption(pParticleType, p_123638_), p_123633_ -> p_123633_.state).fieldOf("block_state");
    }

    public static StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec(ParticleType<BlockParticleOption> pParticleType) {
        return ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).map(p_325792_ -> new BlockParticleOption(pParticleType, p_325792_), p_325793_ -> p_325793_.state);
    }

    public BlockParticleOption(ParticleType<BlockParticleOption> pType, BlockState pState) {
        this.type = pType;
        this.state = pState;
    }

    @Override
    public ParticleType<BlockParticleOption> getType() {
        return this.type;
    }

    public BlockState getState() {
        return this.state;
    }
}