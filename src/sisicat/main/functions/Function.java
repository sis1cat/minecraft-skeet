package sisicat.main.functions;

import com.darkmagician6.eventapi.EventManager;
import com.google.gson.annotations.Expose;
import sisicat.IDefault;

import java.util.ArrayList;
import java.util.Collections;

public class Function implements IDefault {
    @Expose
    private final String name;
    @Expose
    private int bindType;
    @Expose
    private int keyBind;

    @Expose
    private boolean canBeActivated = false;
    private boolean isActivated = false;

    @Expose
    private ArrayList<FunctionSetting> functionSettings = new ArrayList<>();

    public Function(String name){
        this.name = name;
    }

    public void addToFunctionsManager(){
        FunctionsManager.getFunctionsArray().add(this);
    }

    public String getName(){
        return name;
    }
    public int getKeyBind(){
        return keyBind;
    }
    public int getBindType() {
        return bindType;
    }
    public boolean canBeActivated() {
        return canBeActivated;
    }
    public boolean isActivated() {
        return isActivated;
    }

    public ArrayList<FunctionSetting> getFunctionSettings(){
        return functionSettings;
    }
    public void setFunctionSettings(ArrayList<FunctionSetting> functionSettings){
        this.functionSettings = functionSettings;
    }

    public void setBindType(int bindType) {
        setActivated(false);
        this.bindType = bindType;
    }
    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public void toggleCanBeActivated(){

        canBeActivated = !canBeActivated;

    }

    public void setCanBeActivated(boolean state){

        canBeActivated = state;

    }

    public void setActivated(boolean state){

        if(!state && isActivated) {
            isActivated = false;

            unregisterFromEventManager();
            onDeactivated();

            return;
        }

        if(canBeActivated && !isActivated && state) { isActivated = true;  onActivated(); registerToEventManager(); }

    }

    public Function addSetting(FunctionSetting... functionSetting){
        Collections.addAll(functionSettings, functionSetting);
        return this;
    }

    public FunctionSetting getSettingByName(String settingName){

        for(FunctionSetting functionSetting : functionSettings)
            if(functionSetting.getName().equals(settingName))
                return functionSetting;

        return null;

    }

    private void registerToEventManager(){
        EventManager.register(this);
    }

    private void unregisterFromEventManager(){
        EventManager.unregister(this);
    }

    public void onActivated(){

    }

    public void onDeactivated(){

    }



}
