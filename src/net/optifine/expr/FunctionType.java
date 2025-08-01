package net.optifine.expr;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.optifine.Config;
import net.optifine.shaders.uniform.Smoother;
import net.optifine.util.FrameEvent;
import net.optifine.util.MathUtils;

public enum FunctionType {
    PLUS(10, ExpressionType.FLOAT, "+", ExpressionType.FLOAT, ExpressionType.FLOAT),
    MINUS(10, ExpressionType.FLOAT, "-", ExpressionType.FLOAT, ExpressionType.FLOAT),
    MUL(11, ExpressionType.FLOAT, "*", ExpressionType.FLOAT, ExpressionType.FLOAT),
    DIV(11, ExpressionType.FLOAT, "/", ExpressionType.FLOAT, ExpressionType.FLOAT),
    MOD(11, ExpressionType.FLOAT, "%", ExpressionType.FLOAT, ExpressionType.FLOAT),
    NEG(12, ExpressionType.FLOAT, "neg", ExpressionType.FLOAT),
    PI(ExpressionType.FLOAT, "pi"),
    SIN(ExpressionType.FLOAT, "sin", ExpressionType.FLOAT),
    COS(ExpressionType.FLOAT, "cos", ExpressionType.FLOAT),
    ASIN(ExpressionType.FLOAT, "asin", ExpressionType.FLOAT),
    ACOS(ExpressionType.FLOAT, "acos", ExpressionType.FLOAT),
    TAN(ExpressionType.FLOAT, "tan", ExpressionType.FLOAT),
    ATAN(ExpressionType.FLOAT, "atan", ExpressionType.FLOAT),
    ATAN2(ExpressionType.FLOAT, "atan2", ExpressionType.FLOAT, ExpressionType.FLOAT),
    TORAD(ExpressionType.FLOAT, "torad", ExpressionType.FLOAT),
    TODEG(ExpressionType.FLOAT, "todeg", ExpressionType.FLOAT),
    MIN(ExpressionType.FLOAT, "min", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT)),
    MAX(ExpressionType.FLOAT, "max", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT)),
    CLAMP(ExpressionType.FLOAT, "clamp", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    ABS(ExpressionType.FLOAT, "abs", ExpressionType.FLOAT),
    FLOOR(ExpressionType.FLOAT, "floor", ExpressionType.FLOAT),
    CEIL(ExpressionType.FLOAT, "ceil", ExpressionType.FLOAT),
    EXP(ExpressionType.FLOAT, "exp", ExpressionType.FLOAT),
    FRAC(ExpressionType.FLOAT, "frac", ExpressionType.FLOAT),
    LOG(ExpressionType.FLOAT, "log", ExpressionType.FLOAT),
    POW(ExpressionType.FLOAT, "pow", ExpressionType.FLOAT, ExpressionType.FLOAT),
    RANDOM(ExpressionType.FLOAT, "random", new ParametersVariable().repeat(ExpressionType.FLOAT).maxCount(1)),
    ROUND(ExpressionType.FLOAT, "round", ExpressionType.FLOAT),
    SIGNUM(ExpressionType.FLOAT, "signum", ExpressionType.FLOAT),
    SQRT(ExpressionType.FLOAT, "sqrt", ExpressionType.FLOAT),
    FMOD(ExpressionType.FLOAT, "fmod", ExpressionType.FLOAT, ExpressionType.FLOAT),
    LERP(ExpressionType.FLOAT, "lerp", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    TIME(ExpressionType.FLOAT, "time"),
    DAY_TIME(ExpressionType.FLOAT, "day_time"),
    DAY_COUNT(ExpressionType.FLOAT, "day_count"),
    PRINT(ExpressionType.FLOAT, "print", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    PRINTB(ExpressionType.BOOL, "printb", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.BOOL),
    IF(
        ExpressionType.FLOAT,
        "if",
        new ParametersVariable().first(ExpressionType.BOOL, ExpressionType.FLOAT).repeat(ExpressionType.BOOL, ExpressionType.FLOAT).last(ExpressionType.FLOAT)
    ),
    NOT(12, ExpressionType.BOOL, "!", ExpressionType.BOOL),
    AND(3, ExpressionType.BOOL, "&&", ExpressionType.BOOL, ExpressionType.BOOL),
    OR(2, ExpressionType.BOOL, "||", ExpressionType.BOOL, ExpressionType.BOOL),
    GREATER(8, ExpressionType.BOOL, ">", ExpressionType.FLOAT, ExpressionType.FLOAT),
    GREATER_OR_EQUAL(8, ExpressionType.BOOL, ">=", ExpressionType.FLOAT, ExpressionType.FLOAT),
    SMALLER(8, ExpressionType.BOOL, "<", ExpressionType.FLOAT, ExpressionType.FLOAT),
    SMALLER_OR_EQUAL(8, ExpressionType.BOOL, "<=", ExpressionType.FLOAT, ExpressionType.FLOAT),
    EQUAL(7, ExpressionType.BOOL, "==", ExpressionType.FLOAT, ExpressionType.FLOAT),
    NOT_EQUAL(7, ExpressionType.BOOL, "!=", ExpressionType.FLOAT, ExpressionType.FLOAT),
    BETWEEN(7, ExpressionType.BOOL, "between", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    EQUALS(7, ExpressionType.BOOL, "equals", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    IN(ExpressionType.BOOL, "in", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT).last(ExpressionType.FLOAT)),
    SMOOTH(ExpressionType.FLOAT, "smooth", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT).maxCount(4)),
    TRUE(ExpressionType.BOOL, "true"),
    FALSE(ExpressionType.BOOL, "false"),
    IFB(
        ExpressionType.BOOL,
        "ifb",
        new ParametersVariable().first(ExpressionType.BOOL, ExpressionType.BOOL).repeat(ExpressionType.BOOL, ExpressionType.BOOL).last(ExpressionType.BOOL)
    ),
    VEC2(ExpressionType.FLOAT_ARRAY, "vec2", ExpressionType.FLOAT, ExpressionType.FLOAT),
    VEC3(ExpressionType.FLOAT_ARRAY, "vec3", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
    VEC4(ExpressionType.FLOAT_ARRAY, "vec4", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT);

    private int precedence;
    private ExpressionType expressionType;
    private String name;
    private IParameters parameters;
    public static FunctionType[] VALUES = values();
    private static final Map<Integer, Float> mapSmooth = new HashMap<>();

    private FunctionType(ExpressionType expressionType, String name, ExpressionType... parameterTypes) {
        this(0, expressionType, name, parameterTypes);
    }

    private FunctionType(int precedence, ExpressionType expressionType, String name, ExpressionType... parameterTypes) {
        this(precedence, expressionType, name, new Parameters(parameterTypes));
    }

    private FunctionType(ExpressionType expressionType, String name, IParameters parameters) {
        this(0, expressionType, name, parameters);
    }

    private FunctionType(int precedence, ExpressionType expressionType, String name, IParameters parameters) {
        this.precedence = precedence;
        this.expressionType = expressionType;
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return this.name;
    }

    public int getPrecedence() {
        return this.precedence;
    }

    public ExpressionType getExpressionType() {
        return this.expressionType;
    }

    public IParameters getParameters() {
        return this.parameters;
    }

    public int getParameterCount(IExpression[] arguments) {
        return this.parameters.getParameterTypes(arguments).length;
    }

    public ExpressionType[] getParameterTypes(IExpression[] arguments) {
        return this.parameters.getParameterTypes(arguments);
    }

    public float evalFloat(IExpression[] args) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        switch (this) {
            case PLUS:
                return evalFloat(args, 0) + evalFloat(args, 1);
            case MINUS:
                return evalFloat(args, 0) - evalFloat(args, 1);
            case MUL:
                return evalFloat(args, 0) * evalFloat(args, 1);
            case DIV:
                return evalFloat(args, 0) / evalFloat(args, 1);
            case MOD:
                float f = evalFloat(args, 0);
                float f1 = evalFloat(args, 1);
                return f - f1 * (float)((int)(f / f1));
            case NEG:
                return -evalFloat(args, 0);
            case PI:
                return (float) Math.PI;
            case SIN:
                return Mth.sin(evalFloat(args, 0));
            case COS:
                return Mth.cos(evalFloat(args, 0));
            case ASIN:
                return MathUtils.asin(evalFloat(args, 0));
            case ACOS:
                return MathUtils.acos(evalFloat(args, 0));
            case TAN:
                return (float)Math.tan((double)evalFloat(args, 0));
            case ATAN:
                return (float)Math.atan((double)evalFloat(args, 0));
            case ATAN2:
                return (float)Mth.atan2((double)evalFloat(args, 0), (double)evalFloat(args, 1));
            case TORAD:
                return MathUtils.toRad(evalFloat(args, 0));
            case TODEG:
                return MathUtils.toDeg(evalFloat(args, 0));
            case MIN:
                return this.getMin(args);
            case MAX:
                return this.getMax(args);
            case CLAMP:
                return Mth.clamp(evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2));
            case ABS:
                return Mth.abs(evalFloat(args, 0));
            case FLOOR:
                return (float)Mth.floor(evalFloat(args, 0));
            case CEIL:
                return (float)Mth.ceil(evalFloat(args, 0));
            case EXP:
                return (float)Math.exp((double)evalFloat(args, 0));
            case FRAC:
                return Mth.frac(evalFloat(args, 0));
            case LOG:
                return (float)Math.log((double)evalFloat(args, 0));
            case POW:
                return (float)Math.pow((double)evalFloat(args, 0), (double)evalFloat(args, 1));
            case RANDOM:
                if (args.length > 0) {
                    float f11 = evalFloat(args, 0);
                    int i1 = Float.floatToIntBits(f11);
                    int j1 = Config.intHash(i1);
                    return (float)Math.abs(j1) / 2.1474836E9F;
                }

                return (float)Math.random();
            case ROUND:
                return (float)Math.round(evalFloat(args, 0));
            case SIGNUM:
                return Math.signum(evalFloat(args, 0));
            case SQRT:
                return Mth.sqrt(evalFloat(args, 0));
            case FMOD:
                float f2 = evalFloat(args, 0);
                float f3 = evalFloat(args, 1);
                return f2 - f3 * (float)Mth.floor(f2 / f3);
            case LERP:
                float f4 = evalFloat(args, 0);
                float f5 = evalFloat(args, 1);
                float f6 = evalFloat(args, 2);
                return Mth.lerp(f4, f5, f6);
            case TIME:
                if (level == null) {
                    return 0.0F;
                }

                return (float)(level.getGameTime() % 27720L) + GameRenderer.getRenderPartialTicks();
            case DAY_TIME:
                if (level == null) {
                    return 0.0F;
                }

                return (float)(level.getDayTime() % 24000L) + GameRenderer.getRenderPartialTicks();
            case DAY_COUNT:
                if (level == null) {
                    return 0.0F;
                }

                return (float)(level.getDayTime() / 24000L);
            case PRINT:
                int i = (int)evalFloat(args, 0);
                int j = (int)evalFloat(args, 1);
                float f7 = evalFloat(args, 2);
                if (FrameEvent.isActive("CEM-PRINT-" + i, j)) {
                    Config.dbg("CEM print(" + i + ") = " + f7);
                }

                return f7;
            case PRINTB:
            case NOT:
            case AND:
            case OR:
            case GREATER:
            case GREATER_OR_EQUAL:
            case SMALLER:
            case SMALLER_OR_EQUAL:
            case EQUAL:
            case NOT_EQUAL:
            case BETWEEN:
            case EQUALS:
            case IN:
            default:
                Config.warn("Unknown function type: " + this);
                return 0.0F;
            case IF:
                int k = (args.length - 1) / 2;

                for (int k1 = 0; k1 < k; k1++) {
                    int l1 = k1 * 2;
                    if (evalBool(args, l1)) {
                        return evalFloat(args, l1 + 1);
                    }
                }

                return evalFloat(args, k * 2);
            case SMOOTH:
                int l = (int)evalFloat(args, 0);
                float f8 = evalFloat(args, 1);
                float f9 = args.length > 2 ? evalFloat(args, 2) : 1.0F;
                float f10 = args.length > 3 ? evalFloat(args, 3) : f9;
                return Smoother.getSmoothValue(l, f8, f9, f10);
        }
    }

    private float getMin(IExpression[] exprs) {
        if (exprs.length == 2) {
            return Math.min(evalFloat(exprs, 0), evalFloat(exprs, 1));
        } else {
            float f = evalFloat(exprs, 0);

            for (int i = 1; i < exprs.length; i++) {
                float f1 = evalFloat(exprs, i);
                if (f1 < f) {
                    f = f1;
                }
            }

            return f;
        }
    }

    private float getMax(IExpression[] exprs) {
        if (exprs.length == 2) {
            return Math.max(evalFloat(exprs, 0), evalFloat(exprs, 1));
        } else {
            float f = evalFloat(exprs, 0);

            for (int i = 1; i < exprs.length; i++) {
                float f1 = evalFloat(exprs, i);
                if (f1 > f) {
                    f = f1;
                }
            }

            return f;
        }
    }

    private static float evalFloat(IExpression[] exprs, int index) {
        IExpressionFloat iexpressionfloat = (IExpressionFloat)exprs[index];
        return iexpressionfloat.eval();
    }

    public boolean evalBool(IExpression[] args) {
        switch (this) {
            case PRINTB:
                int i1 = (int)evalFloat(args, 0);
                int j1 = (int)evalFloat(args, 1);
                boolean flag = evalBool(args, 2);
                if (FrameEvent.isActive("CEM-PRINTB-" + i1, j1)) {
                    Config.dbg("CEM printb(" + i1 + ") = " + flag);
                }

                return flag;
            case IF:
            case SMOOTH:
            default:
                Config.warn("Unknown function type: " + this);
                return false;
            case NOT:
                return !evalBool(args, 0);
            case AND:
                return evalBool(args, 0) && evalBool(args, 1);
            case OR:
                return evalBool(args, 0) || evalBool(args, 1);
            case GREATER:
                return evalFloat(args, 0) > evalFloat(args, 1);
            case GREATER_OR_EQUAL:
                return evalFloat(args, 0) >= evalFloat(args, 1);
            case SMALLER:
                return evalFloat(args, 0) < evalFloat(args, 1);
            case SMALLER_OR_EQUAL:
                return evalFloat(args, 0) <= evalFloat(args, 1);
            case EQUAL:
                return evalFloat(args, 0) == evalFloat(args, 1);
            case NOT_EQUAL:
                return evalFloat(args, 0) != evalFloat(args, 1);
            case BETWEEN:
                float f = evalFloat(args, 0);
                return f >= evalFloat(args, 1) && f <= evalFloat(args, 2);
            case EQUALS:
                float f1 = evalFloat(args, 0) - evalFloat(args, 1);
                float f2 = evalFloat(args, 2);
                return Math.abs(f1) <= f2;
            case IN:
                float f3 = evalFloat(args, 0);

                for (int l = 1; l < args.length; l++) {
                    float f4 = evalFloat(args, l);
                    if (f3 == f4) {
                        return true;
                    }
                }

                return false;
            case TRUE:
                return true;
            case FALSE:
                return false;
            case IFB:
                int i = (args.length - 1) / 2;

                for (int j = 0; j < i; j++) {
                    int k = j * 2;
                    if (evalBool(args, k)) {
                        return evalBool(args, k + 1);
                    }
                }

                return evalBool(args, i * 2);
        }
    }

    private static boolean evalBool(IExpression[] exprs, int index) {
        IExpressionBool iexpressionbool = (IExpressionBool)exprs[index];
        return iexpressionbool.eval();
    }

    public float[] evalFloatArray(IExpression[] args) {
        switch (this) {
            case VEC2:
                return new float[]{evalFloat(args, 0), evalFloat(args, 1)};
            case VEC3:
                return new float[]{evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2)};
            case VEC4:
                return new float[]{evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2), evalFloat(args, 3)};
            default:
                Config.warn("Unknown function type: " + this);
                return null;
        }
    }

    public static FunctionType parse(String str) {
        for (int i = 0; i < VALUES.length; i++) {
            FunctionType functiontype = VALUES[i];
            if (functiontype.getName().equals(str)) {
                return functiontype;
            }
        }

        return null;
    }
}