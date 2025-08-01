package net.optifine.entity.model.anim;

public class ModelUpdater {
    private ModelVariableUpdater[] modelVariableUpdaters;

    public ModelUpdater(ModelVariableUpdater[] modelVariableUpdaters) {
        this.modelVariableUpdaters = modelVariableUpdaters;
    }

    public ModelVariableUpdater[] getModelVariableUpdaters() {
        return this.modelVariableUpdaters;
    }

    public void update() {
        for (int i = 0; i < this.modelVariableUpdaters.length; i++) {
            ModelVariableUpdater modelvariableupdater = this.modelVariableUpdaters[i];
            modelvariableupdater.update();
        }
    }

    public boolean initialize(IModelResolver mr) {
        for (int i = 0; i < this.modelVariableUpdaters.length; i++) {
            ModelVariableUpdater modelvariableupdater = this.modelVariableUpdaters[i];
            if (!modelvariableupdater.initialize(mr)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "variableUpdaters: " + this.modelVariableUpdaters.length;
    }
}