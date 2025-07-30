package net.minecraft.client.gui.screens.options.controls;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ControlsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("controls.title");

    private static OptionInstance<?>[] options(Options pOptions) {
        return new OptionInstance[]{pOptions.toggleCrouch(), pOptions.toggleSprint(), pOptions.autoJump(), pOptions.operatorItemsTab()};
    }

    public ControlsScreen(Screen pLastScreen, Options pOptions) {
        super(pLastScreen, pOptions, TITLE);
    }

    @Override
    protected void addOptions() {
        this.list
            .addSmall(
                Button.builder(
                        Component.translatable("options.mouse_settings"), p_344287_ -> this.minecraft.setScreen(new MouseSettingsScreen(this, this.options))
                    )
                    .build(),
                Button.builder(Component.translatable("controls.keybinds"), p_343299_ -> this.minecraft.setScreen(new KeyBindsScreen(this, this.options)))
                    .build()
            );
        this.list.addSmall(options(this.options));
    }
}