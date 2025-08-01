package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class WeatheringCopperTrapDoorBlock extends TrapDoorBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperTrapDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360471_ -> p_360471_.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(TrapDoorBlock::getType),
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperTrapDoorBlock::getAge),
                    propertiesCodec()
                )
                .apply(p_360471_, WeatheringCopperTrapDoorBlock::new)
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<WeatheringCopperTrapDoorBlock> codec() {
        return CODEC;
    }

    protected WeatheringCopperTrapDoorBlock(BlockSetType pType, WeatheringCopper.WeatherState pWeatherState, BlockBehaviour.Properties pProperties) {
        super(pType, pProperties);
        this.weatherState = pWeatherState;
    }

    @Override
    protected void randomTick(BlockState p_311400_, ServerLevel p_310287_, BlockPos p_310085_, RandomSource p_311069_) {
        this.changeOverTime(p_311400_, p_310287_, p_310085_, p_311069_);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_312946_) {
        return WeatheringCopper.getNext(p_312946_.getBlock()).isPresent();
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}