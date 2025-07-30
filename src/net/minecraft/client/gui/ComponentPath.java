package net.minecraft.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ComponentPath {
    static ComponentPath leaf(GuiEventListener pComponent) {
        return new ComponentPath.Leaf(pComponent);
    }

    @Nullable
    static ComponentPath path(ContainerEventHandler pComponent, @Nullable ComponentPath pChildPath) {
        return pChildPath == null ? null : new ComponentPath.Path(pComponent, pChildPath);
    }

    static ComponentPath path(GuiEventListener pLeafComponent, ContainerEventHandler... pAncestorComponents) {
        ComponentPath componentpath = leaf(pLeafComponent);

        for (ContainerEventHandler containereventhandler : pAncestorComponents) {
            componentpath = path(containereventhandler, componentpath);
        }

        return componentpath;
    }

    GuiEventListener component();

    void applyFocus(boolean pFocused);

    @OnlyIn(Dist.CLIENT)
    public static record Leaf(GuiEventListener component) implements ComponentPath {
        @Override
        public void applyFocus(boolean p_265248_) {
            this.component.setFocused(p_265248_);
        }

        @Override
        public GuiEventListener component() {
            return this.component;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
        @Override
        public void applyFocus(boolean p_265230_) {
            if (!p_265230_) {
                this.component.setFocused(null);
            } else {
                this.component.setFocused(this.childPath.component());
            }

            this.childPath.applyFocus(p_265230_);
        }

        public ContainerEventHandler component() {
            return this.component;
        }
    }
}