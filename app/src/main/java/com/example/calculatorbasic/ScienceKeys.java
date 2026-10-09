package com.example.calculatorbasic;

/** Labels, inserts and secondary functions are defined together. */
final class ScienceKeys {
    static final String[][] ROWS = {
        {"SETUP","History","Undo","Text"}, {"SHIFT","ALPHA","◀","▲","▼","▶"},
        {"sin","cos","tan","ln","log","x²"}, {"√","xʸ","a/b","π","x!","%"},
        {"(",")","Ans","S⇔D","Vars","Catalog"},
        {"7","8","9","DEL","AC"}, {"4","5","6","×","÷"},
        {"1","2","3","+","−"}, {"0",".","(−)","×10ˣ","="}
    };
    static final String[][] FUNCTIONS = {
        {"sin","sin(","sin⁻¹","asin("}, {"cos","cos(","cos⁻¹","acos("},
        {"tan","tan(","tan⁻¹","atan("}, {"ln","ln(","eˣ","exp("},
        {"log","log(","logₐ","log(,)"}, {"x²","^2","x³","^3"},
        {"√","sqrt(","∛","cbrt("}, {"xʸ","^","ⁿ√","root(,)"},
        {"a/b","frac(,)","a b/c","mixed(,,)"}, {"π","pi","e","e"},
        {"x!","!","nCr","ncr(,)"}, {"%","%","mod","mod(,)"},
        {"(","(","x⁻¹","^(-1)"}, {")",")","|x|","abs("},
        {"Ans","Ans","M","M"}, {"S⇔D","S⇔D","ENG","ENG"},
        {"Vars","Vars","M+","M+"}, {"Catalog","Catalog","M−","M−"}
    };
    static String label(String key, boolean shift) {
        for (String[] f : FUNCTIONS) if (f[0].equals(key)) return f[shift ? 2 : 0];
        return key;
    }
    static String token(String key, boolean shift) {
        for (String[] f : FUNCTIONS) if (f[0].equals(key)) return f[shift ? 3 : 1];
        switch (key) {
            case "×": return "*"; case "÷": return "/"; case "−": return "-";
            case "(−)": return "NEG"; case "×10ˣ": return "*10^";
            default: return key;
        }
    }
    static final String[] CATEGORIES = {
        "Trigonometric / Hyperbolic", "Powers / Logarithms / Roots", "Numbers / Fractions",
        "Integers / Random", "Complex", "Calculus / Summation", "Custom functions"
    };
    static final String[][] CATALOG = {
        {"sin(","cos(","tan(","asin(","acos(","atan(","sinh(","cosh(","tanh(","asinh(","acosh(","atanh("},
        {"ln(","log(,)","exp(","10^(","sqrt(","cbrt(","root(,)","^(-1)"},
        {"abs(","floor(","ceil(","round(","trunc(","%","frac(,)","mixed(,,)","dms(,,)","deg(","rad(","grad("},
        {"factorial(","npr(,)","ncr(,)","gcd(,)","lcm(,)","mod(,)","rand()","randint(,)"},
        {"i","polar(,)","re(","im(","arg(","conj("},
        {"diff(,)","integral(,,)","sum(,,)","product(,,)"}, {"f(","g("}
    };
}
