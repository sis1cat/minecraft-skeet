package sisicat.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import sisicat.IDefault;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.functions.FunctionsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Config implements IDefault {

    public static String loaded = "none";

    public Config(){

        File configsFolder = new File(mc.gameDirectory + "\\configs");
        File lastLoadedConfig = new File(mc.gameDirectory + "\\configs\\.last");

        if(!configsFolder.exists())
            configsFolder.mkdirs();

        if(!lastLoadedConfig.exists())
            try (FileWriter writer = new FileWriter(mc.gameDirectory + "\\configs\\.last")) {
                writer.write("none");
            } catch (IOException e) {
                e.printStackTrace();
            }

        else try (BufferedReader reader = new BufferedReader(new FileReader(mc.gameDirectory + "\\configs\\.last"))) {
                loadConfig(reader.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    public void saveConfig(String name) {

        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

        Path configPath = Path.of(mc.gameDirectory + "\\configs\\" + name);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(configPath), StandardCharsets.UTF_8)) {
            gson.toJson(FunctionsManager.getFunctionsArray(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadConfig(name);

    }

    public void loadConfig(String name){

        if(!new File(mc.gameDirectory + "\\configs\\" + name).exists())
            return;

        Gson gson = new Gson();

        try {

            String jsonConfig = Files.readString(Path.of(mc.gameDirectory + "\\configs\\" + name));
            Function[] loadedFunctions = gson.fromJson(jsonConfig, Function[].class);

            for (Function loadedFunction : loadedFunctions) {
                for (Function function : FunctionsManager.getFunctionsArray()) {
                    if (function.getName().equals(loadedFunction.getName())) {

                        function.setKeyBind(loadedFunction.getKeyBind());
                        function.setBindType(loadedFunction.getBindType());
                        function.setCanBeActivated(loadedFunction.canBeActivated());
                        function.setActivated(loadedFunction.isActivated());

                        for(FunctionSetting loadedFunctionSetting : loadedFunction.getFunctionSettings()) {

                            FunctionSetting functionSetting = function.getSettingByName(loadedFunctionSetting.getName());

                            if(functionSetting == null)
                                continue;

                            functionSetting.setStringValue(loadedFunctionSetting.getStringValue());
                            functionSetting.setColor(loadedFunctionSetting.getHSVAColor());
                            functionSetting.setCanBeActivated(loadedFunctionSetting.getCanBeActivated());
                            functionSetting.setBindType(loadedFunctionSetting.getBindType());
                            functionSetting.setKeyBind(loadedFunctionSetting.getKeyBind());
                            functionSetting.floatValue = (float) loadedFunctionSetting.floatValue; // ?????
                            functionSetting.selectedOptionsList = loadedFunctionSetting.selectedOptionsList;
                            functionSetting.setColor(loadedFunctionSetting.getHSVAColor());

                        }

                        break;

                    }
                }
            }

            loaded = name;

            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(Path.of(mc.gameDirectory + "\\configs\\.last")), StandardCharsets.UTF_8)) {
                writer.write(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }catch (JsonSyntaxException | JsonIOException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
