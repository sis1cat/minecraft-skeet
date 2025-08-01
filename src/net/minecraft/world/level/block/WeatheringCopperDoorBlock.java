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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class WeatheringCopperDoorBlock extends DoorBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360466_ -> p_360466_.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type),
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperDoorBlock::getAge),
                    propertiesCodec()
                )
                .apply(p_360466_, WeatheringCopperDoorBlock::new)
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<WeatheringCopperDoorBlock> codec() {
        return CODEC;
    }

    protected WeatheringCopperDoorBlock(BlockSetType pType, WeatheringCopper.WeatherState pWeatherState, BlockBehaviour.Properties pProperties) {
        super(pType, pProperties);
        this.weatherState = pWeatherState;
    }

    @Override
    protected void randomTick(BlockState p_312732_, ServerLevel p_309748_, BlockPos p_312849_, RandomSource p_311578_) {
        if (p_312732_.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
            this.changeOverTime(p_312732_, p_309748_, p_312849_, p_311578_);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_312606_) {
        return WeatheringCopper.getNext(p_312606_.getBlock()).isPresent();
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}