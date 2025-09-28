package sisicat.main.functions.saves;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.MineSense;
import sisicat.events.GraphicsEvent;
import sisicat.main.Config;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.gui.elements.widgets.Configs;

import java.io.File;
import java.util.Objects;

public class ConfigurationFunction extends Function {

    private final FunctionSetting
            edit,
            load,
            save,
            delete,
            reset,
            importFromClipboard,
            exportToClipboard;

    public ConfigurationFunction(String name) {
        super(name);

        edit = new FunctionSetting("Edit", "");
        load = new FunctionSetting("Load");
        save = new FunctionSetting("Save");
        delete = new FunctionSetting("Delete");
        reset = new FunctionSetting("Reset");
        importFromClipboard = new FunctionSetting("Import from clipboard");
        exportToClipboard = new FunctionSetting("Export to clipboard");

        this.addSetting(edit);
        this.addSetting(load);
        this.addSetting(save);
        this.addSetting(delete);
        this.addSetting(reset);
        this.addSetting(importFromClipboard);
        this.addSetting(exportToClipboard);

        this.setCanBeActivated(true);

    }

    @EventTarget
    void _event(GraphicsEvent ignored){

        String selected = edit.getStringValue();

        if(load.isClicked && !selected.isEmpty()) {
            MineSense.getConfig().loadConfig(selected);
            Configs.reload();
        }

        if(save.isClicked && !selected.isEmpty()) {
            new Thread(() -> {
                MineSense.getConfig().saveConfig(selected);
                Configs.reload();
            }).start();
        }

        if(delete.isClicked && !selected.isEmpty()){
            try {

                File file = new File("configs\\" + edit.getStringValue());

                if(file.exists()) {

                    for (Configs.Config config : Configs.configurations) {

                        if (config.name.equals(edit.getStringValue()) && Objects.equals(Configs.Config.selected, edit.getStringValue())) {

                            final int currentConfigIndex = Configs.configurations.indexOf(config);

                            if (Configs.configurations.size() > currentConfigIndex + 1) {
                                Configs.Config.selected = Configs.configurations.get(currentConfigIndex + 1).name;
                            } else if(Configs.configurations.size() > 1) {
                                Configs.Config.selected = Configs.configurations.get(currentConfigIndex - 1).name;
                            } else {
                                Configs.Config.selected = "";
                            }

                            break;

                        }
                    }

                }

                file.delete();

                edit.setStringValue(Configs.Config.selected);

            } catch (Exception e) {
                e.printStackTrace();
            }

            Configs.reload();

        }

        if(reset.isClicked && !selected.isEmpty()) {

            if(!selected.equals(Config.loaded)) {

                String prevConfig = Config.loaded;

                MineSense.getConfig().loadConfig(selected);

                if(Objects.equals(Config.loaded, selected)) {

                    for (Function function : FunctionsManager.getFunctionsArray()) {

                        if (function instanceof ConfigurationFunction)
                            continue;

                        function.resetSettings();

                    }

                    new Thread(() -> MineSense.getConfig().saveConfig(selected)).start();

                    MineSense.getConfig().loadConfig(prevConfig);

                }

            } else {

                for (Function function : FunctionsManager.getFunctionsArray()) {

                    if(function instanceof ConfigurationFunction)
                        continue;

                    function.resetSettings();

                }
            }

        }

        load.isClicked = false;
        save.isClicked = false;
        delete.isClicked = false;
        reset.isClicked = false;

    }

}
