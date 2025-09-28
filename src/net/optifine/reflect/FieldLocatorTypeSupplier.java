package net.optifine.reflect;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.optifine.Log;

public class FieldLocatorTypeSupplier implements IFieldLocator {
    private ReflectorClass reflectorClass = null;
    private Supplier<Class> targetFieldTypeSupplier = null;
    private int targetFieldIndex;

    public FieldLocatorTypeSupplier(ReflectorClass reflectorClass, Supplier<Class> targetFieldTypeSupplier) {
        this(reflectorClass, targetFieldTypeSupplier, 0);
    }

    public FieldLocatorTypeSupplier(ReflectorClass reflectorClass, Supplier<Class> targetFieldTypeSupplier, int targetFieldIndex) {
        this.reflectorClass = reflectorClass;
        this.targetFieldTypeSupplier = targetFieldTypeSupplier;
        this.targetFieldIndex = targetFieldIndex;
    }

    @Override
    public Field getField() {
        Class oclass = this.targetFieldTypeSupplier.get();
        Class oclass1 = this.reflectorClass.getTargetClass();
        if (oclass1 == null) {
            return null;
        } else {
            try {
                Field[] afield = oclass1.getDeclaredFields();
                int i = 0;

                for (int j = 0; j < afield.length; j++) {
                    Field field = afield[j];
                    if (field.getType() == oclass) {
                        if (i == this.targetFieldIndex) {
                            field.setAccessible(true);
                            return field;
                        }

                        i++;
                    }
                }

                Log.log("(Reflector) Field not present: " + oclass1.getName() + ".(type: " + oclass + ", index: " + this.targetFieldIndex + ")");
                return null;
            } catch (SecurityException securityexception) {
                securityexception.printStackTrace();
                return null;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return null;
            }
        }
    }
}