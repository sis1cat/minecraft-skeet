package net.minecraft.client;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import sisicat.main.utilities.Animation;

@OnlyIn(Dist.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
    @Nullable
    public GuiMessageTag.Icon icon() {
        return this.tag != null ? this.tag.icon() : null;
    }

    @OnlyIn(Dist.CLIENT)
    public record Line(int addedTime, FormattedCharSequence content, @Nullable GuiMessageTag tag, boolean endOfEntry, Interpolation interpolation) {
    }

    public static class Interpolation {

        public float value;
        private final Animation animation;

        public Interpolation(boolean repeated) {

            value = repeated ? 50 : -400;
            animation = new Animation();

        }

        public void interpolate(){
            value = animation.interpolate(value, 0, 50d);
        }

    }

}