package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

public class EventRender implements Event {
    public float partialTicks;
    public Window scaledResolution;
    public Type type;
    public PoseStack matrixStack;
    public Matrix4f matrix;


    public EventRender(float partialTicks, PoseStack stack, Window scaledResolution, Type type,Matrix4f matrix) {
        this.partialTicks = partialTicks;
        this.scaledResolution = scaledResolution;
        this.matrixStack = stack;
        this.type = type;
        this.matrix = matrix;
    }

    public boolean isRender3D() {
        return this.type == Type.RENDER3D;
    }

    public boolean isRender2D() {
        return this.type == Type.RENDER2D;
    }

    public enum Type {
        RENDER3D, RENDER2D
    }
}
