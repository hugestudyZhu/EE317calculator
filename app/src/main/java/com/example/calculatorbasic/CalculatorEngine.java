package com.example.calculatorbasic;

import com.example.calculatorbasic.math.ScientificEngine;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

/** Compatibility facade for real calculations and numerical functions. */
public final class CalculatorEngine {
    private CalculatorEngine() {}
    public interface CompiledExpression { double evaluate(double x); }
    private static ScientificEngine.Context context(boolean degrees) {
        ScientificEngine.Context c=new ScientificEngine.Context();c.angle=degrees?ScientificEngine.Angle.DEG:ScientificEngine.Angle.RAD;c.variablesAllowed=false;return c;
    }
    public static CompiledExpression compile(String source,boolean allowX,boolean degrees) {
        validate(source);
        if(!allowX){double value=ScientificEngine.evaluate(source,context(degrees)).realOnly();return x->value;}
        ScientificEngine.Expression expression=ScientificEngine.compile(source,context(degrees));return x->expression.evaluate(x).realOnly();
    }
    public static double calculate(String source,boolean degrees) {validate(source);return ScientificEngine.evaluate(source,context(degrees)).realOnly();}
    private static void validate(String source){if(source!=null&&source.length()>180)throw new IllegalArgumentException("Expression is too long");}
    public static String format(double value) {
        if(!Double.isFinite(value))return "Invalid result";
        if(value==0)return "0";
        double magnitude=Math.abs(value);
        if(magnitude>=1e12||magnitude<1e-8)return String.format(Locale.US,"%.15g",value);
        return BigDecimal.valueOf(value).round(new MathContext(15)).stripTrailingZeros().toPlainString();
    }
}
