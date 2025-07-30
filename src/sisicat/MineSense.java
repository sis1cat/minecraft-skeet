package sisicat;

import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.client.Minecraft;
import sisicat.main.Config;

import sisicat.main.gui.MainRender;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.utilities.*;

public class MineSense implements IDefault {

    private static FunctionsManager functionsManager;
    //private static ScriptsManager scriptsManager;

    public static MainRender mainRender;

    private static Config config;

    public static FunctionsManager getFunctionsManager(){
        return functionsManager;
    }
    /*public static ScriptsManager getScriptsManager(){
        return scriptsManager;
    }*/

    public static Config getConfig() { return config; }

    // 1.20.1

    public MineSense(){

        Minecraft.mineSense = this;

        Render.initialize();
        Text.initialize();

        functionsManager = new FunctionsManager();
        //scriptsManager = new ScriptsManager();

        config = new Config();

        mainRender = new MainRender();

        ViaMCP.create();

    }

    public void saveConfig(){ /*config.saveLatest();*/ }

}
