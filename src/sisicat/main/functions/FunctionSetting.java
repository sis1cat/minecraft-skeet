package sisicat.main.functions;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import sisicat.IDefault;
import sisicat.main.utilities.Color;

public class FunctionSetting implements IDefault {

	@Expose
	private final String name;
	private final String type;

	@Expose
	public String stringValue = "";
	public ArrayList<String> optionsList = new ArrayList<>();
	@Expose
	public ArrayList<String> selectedOptionsList = new ArrayList<>();
	@Expose
	private boolean canBeActivated = false;
	private boolean isActivated = false;

	@Expose
	public float floatValue;
	public float minFloatValue;
	public float maxFloatValue;
	public float unitSize;
	public String unitName;

	@Expose
	private float[] color = {0, 0, 0, 0};

	public boolean isClicked = false;

	@Expose
	private int keyBind;
	@Expose
	private int bindType;
	
	public FunctionSetting(String name, ArrayList<String> optionsList, String stringValue){
		this.name = name;
		this.optionsList = optionsList;
		this.type = "Combo";
		this.stringValue = stringValue;
	}
	
	public FunctionSetting(String name){
		this.name = name;
		this.type = "Check";
	}

	public FunctionSetting(String name, String stringValue){
		this.name = name;
		this.type = "Edit";
		this.stringValue = stringValue;
	}
	
	public FunctionSetting(String name, ArrayList<String> optionsList, ArrayList<String> selectedOptionsList){
		this.name = name;
		this.optionsList = optionsList;
		this.selectedOptionsList = selectedOptionsList;
		this.type = "List";
	}

	public FunctionSetting(String name, float floatValue, float minFloatValue, float maxFloatValue, String unitName, float unitSize){
		this.name = name;
		this.floatValue = floatValue;
		this.minFloatValue = minFloatValue;
		this.maxFloatValue = maxFloatValue;
		this.type = "Slider";
		this.unitName = unitName;
		this.unitSize = unitSize;
	}

	public FunctionSetting(String name, float[] color){
		this.name = name;
		this.type = "Color";
		this.color = color;
	}
	
	public String getName(){
		return name;
	}
	
	public String getStringValue(){
		return this.stringValue;
	}

	public void setStringValue(String value){
		this.stringValue = value;
		Minecraft.mineSense.saveConfig();
	}
	
	public ArrayList<String> getOptionsList(){
		return this.optionsList;
	}
	public ArrayList<String> getSelectedOptionsList(){
		return this.selectedOptionsList;
	}
	
	public boolean getCanBeActivated(){
		return this.canBeActivated;
	}

	public void setBindType(int bindType){
		this.bindType = bindType;
	}

	public int getBindType(){
		return bindType;
	}

	public float[] getHSVAColor() {
		return color;
	}

	public float[] getRGBAColor() {

		float[] rgb = Color.hsvToRgb(this.color[0], this.color[1], this.color[2]);
		float alpha = this.color[3] * 255;

		if(alpha < 3)
			alpha = 0;

		return new float[]{rgb[0], rgb[1], rgb[2], alpha};

	}

	public static String rgbaToHex(int r, int g, int b, int a) {
		return String.format("%02X%02X%02X%02X", r, g, b, a);
	}

	public String getHEXColor() {
		return rgbaToHex((int) getRGBAColor()[0], (int) getRGBAColor()[1], (int) getRGBAColor()[2], (int) getRGBAColor()[3]);
	}

	public void setColor(float[] color) {
		this.color = color;
	}

	public void setHEXColor(String hex) {

		int rgba = (int) Long.parseLong(hex, 16);

		float r = (rgba >> 24) & 0xFF;
		float g = (rgba >> 16) & 0xFF;
		float b = (rgba >> 8) & 0xFF;
		float a = rgba & 0xFF;

		float[] hexColor = Color.rgbToHsv(r, g, b);
		color = new float[] {hexColor[0], hexColor[1], hexColor[2], a / 255f};

	}

	public void toggleCanBeActivated(){
		this.canBeActivated = !this.canBeActivated;

		if(!canBeActivated && isActivated)
			isActivated = false;

		Minecraft.mineSense.saveConfig();
	}

	public void setCanBeActivated(boolean value){
		this.canBeActivated = value;
		Minecraft.mineSense.saveConfig();
	}

	public void setActivated(boolean state){

		if(!state && isActivated) {
			isActivated = false;
			return;
		}

		if(canBeActivated && !isActivated && state)
			isActivated = true;

	}

	public boolean isActivated(){
		return isActivated;
	}

	public float getFloatValue(){
		floatValue = Mth.clamp(floatValue, minFloatValue, maxFloatValue);
		return this.floatValue;
	}

	public void setFloatValue(float value){
		this.floatValue = Mth.clamp(value, minFloatValue, maxFloatValue);
		Minecraft.mineSense.saveConfig();
	}
	
	public float getMinFloatValue(){
		return this.minFloatValue;
	}
	
	public float getMaxFloatValue(){
		return this.maxFloatValue;
	}
	
	public boolean isCombo(){
		return this.type.equalsIgnoreCase("Combo");
	}
	public boolean isCheck(){
		return this.type.equalsIgnoreCase("Check");
	}
	public boolean isSlider(){
		return this.type.equalsIgnoreCase("Slider");
	}

	public int getKeyBind(){
		return keyBind;
	}
	public void setKeyBind(int keyBind){
		this.keyBind = keyBind;
	}
	public String getUnitName() {
		return unitName;
	}
	public float getUnitSize() {
		return unitSize;
	}

}
