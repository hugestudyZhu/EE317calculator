package com.example.calculatorbasic;

import com.example.calculatorbasic.math.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Scientific state and editing, independent of Android and of the screen. */
public final class ScientificCalculator implements Serializable {
    private static final long serialVersionUID = 2L;
    public MathEditor editor = new MathEditor();
    public final ScientificEngine.Context context = new ScientificEngine.Context();
    public MathValue answer = MathValue.integer(0);
    public String result = "0", format = "Norm";
    public int digits = 10;
    public boolean exact = true, mixed, polar, shift, evaluated;
    public String error = "";
    private final List<MathEditor> undo = new ArrayList<>();

    public ScientificCalculator() {
        for (String name : new String[]{"a","b","c","d","e","f","x","y","z","m","ans"}) {
            context.variables.put(name, MathValue.integer(0));
        }
        context.complex = true;
    }

    public void press(String token) {
        if ("SHIFT".equals(token)) { shift = !shift; return; }
        if ("S⇔D".equals(token)) { exact = !exact; reformat(); return; }
        if ("ENG".equals(token)) { format = "ENG"; reformat(); return; }
        if ("M+".equals(token) || "M−".equals(token)) {
            MathValue memory = context.variables.get("m");
            context.variables.put("m", "M+".equals(token) ? memory.add(answer) : memory.subtract(answer));
            return;
        }
        if ("Undo".equals(token)) {
            if (!undo.isEmpty()) { editor = undo.remove(undo.size()-1); edited(); }
            return;
        }
        if ("◀".equals(token) || "▶".equals(token)) { editor.move("◀".equals(token) ? -1 : 1); evaluated=false; return; }
        if ("▲".equals(token) || "▼".equals(token)) { editor.vertical("▲".equals(token) ? -1 : 1); evaluated=false; return; }
        remember();
        if ("AC".equals(token)) { editor = new MathEditor(); result = "0"; }
        else if ("DEL".equals(token)) editor.delete();
        else {
            if (evaluated) {
                boolean continues = token.matches("[+*/^%!-]") || token.startsWith("^")
                        || token.equals("*10^") || token.startsWith("frac(");
                editor = continues ? MathEditor.fromSource(answer.expression()) : new MathEditor();
            }
            editor.insert(token);
        }
        edited();
        shift = false;
    }

    public void replace(String source) {
        if (source == null || source.length() > 4096) throw new IllegalArgumentException("Expression exceeds 4096 characters");
        remember(); editor = MathEditor.fromSource(source); edited();
    }

    public String calculationSource() {
        if (editor.source().trim().isEmpty()) throw new IllegalArgumentException("Enter an expression");
        if (editor.incomplete()) throw new IllegalArgumentException("Fill in every placeholder");
        return editor.source();
    }

    public void commit(MathValue value) {
        answer = value; context.variables.put("ans", value); evaluated = true; error = ""; reformat();
    }

    public void reformat() { result = format(answer); }
    public String format(MathValue value) {
        if (value.imaginary != 0) {
            if (polar) return number(value.abs()) + " ∠ " + number(context.angle.fromRadians(value.arg()));
            return number(value.real) + (value.imaginary < 0 ? " − " : " + ") + number(Math.abs(value.imaginary)) + "i";
        }
        if (exact && "Norm".equals(format) && value.exact != null) return value.exact.format(mixed);
        return number(value.real);
    }
    private String number(double value) {
        if ("Fix".equals(format)) return String.format(Locale.US, "%."+digits+"f", value).trim();
        if ("Sci".equals(format)) return String.format(Locale.US, "%."+(Math.max(1,digits)-1)+"e", value);
        if ("ENG".equals(format) && value != 0) {
            int exponent = (int)(3*Math.floor(Math.log10(Math.abs(value))/3));
            double factor = Math.pow(10, exponent);
            if (factor != 0 && Double.isFinite(factor)) return CalculatorEngine.format(value/factor)+" × 10^("+exponent+")";
        }
        return CalculatorEngine.format(value);
    }
    private void remember() {
        if (undo.size() >= 100) undo.remove(0);
        undo.add(editor.copy());
    }
    private void edited() { evaluated=false; error=""; }
}
