package com.example.calculatorbasic.math;

import java.io.Serializable;

public final class MathValue implements Serializable {
    private static final long serialVersionUID = 1L;
    public final double real, imaginary;
    public final ExactNumber exact;
    public MathValue(double r, double i) { this(r, i, null); }
    public MathValue(double r, double i, ExactNumber exact) {
        if (!Double.isFinite(r) || !Double.isFinite(i)) throw new IllegalArgumentException("Result out of range or function undefined");
        real = r == 0 ? 0 : r; imaginary = i == 0 ? 0 : i; this.exact = i == 0 ? exact : null;
    }
    public static MathValue of(double r) { return new MathValue(r, 0); }
    public static MathValue integer(long r) { return new MathValue(r, 0, ExactNumber.integer(r)); }
    public double realOnly() {
        if (imaginary != 0) throw new IllegalArgumentException("This function requires a real number");
        return real;
    }
    public MathValue add(MathValue b) { return new MathValue(real + b.real, imaginary + b.imaginary, exact != null && b.exact != null ? exact.add(b.exact) : null); }
    public MathValue negate() { return new MathValue(-real, -imaginary, exact == null ? null : exact.negate()); }
    public MathValue subtract(MathValue b) { return add(b.negate()); }
    public MathValue multiply(MathValue b) { return new MathValue(real*b.real-imaginary*b.imaginary, real*b.imaginary+imaginary*b.real, exact != null && b.exact != null ? exact.multiply(b.exact) : null); }
    public MathValue divide(MathValue b) {
        if (b.real == 0 && b.imaginary == 0) throw new IllegalArgumentException("Division by zero");
        if (imaginary == 0 && b.imaginary == 0) return new MathValue(real/b.real, 0, exact != null && b.exact != null ? exact.divide(b.exact) : null);
        double r, i;
        if (Math.abs(b.real) >= Math.abs(b.imaginary)) {
            double t = b.imaginary/b.real, d = 1+t*t;
            r=(real/b.real+(imaginary/b.real)*t)/d; i=(imaginary/b.real-(real/b.real)*t)/d;
        } else {
            double t=b.real/b.imaginary, d=1+t*t;
            r=((real/b.imaginary)*t+imaginary/b.imaginary)/d; i=((imaginary/b.imaginary)*t-real/b.imaginary)/d;
        }
        return new MathValue(r, i);
    }
    public double abs() { return Math.hypot(real, imaginary); }
    public double arg() {
        if (real == 0 && imaginary == 0) throw new IllegalArgumentException("The argument of zero is undefined");
        return Math.atan2(imaginary, real);
    }
    public MathValue pow(MathValue b, boolean complexAllowed) {
        if (imaginary == 0 && b.imaginary == 0 && (real >= 0 || b.real == Math.rint(b.real))) {
            double result=Math.pow(real, b.real);
            ExactNumber e = exact != null && Math.abs(b.real) <= 100 && b.real == Math.rint(b.real) ? exact.pow((int)b.real) : null;
            return new MathValue(result, 0, e);
        }
        if (!complexAllowed) throw new IllegalArgumentException("This power is undefined in real mode");
        if (abs() == 0) throw new IllegalArgumentException("This complex power of zero is undefined");
        double l=Math.log(abs()), a=arg(), amplitude=Math.exp(b.real*l-b.imaginary*a), phase=b.imaginary*l+b.real*a;
        return new MathValue(amplitude*Math.cos(phase), amplitude*Math.sin(phase));
    }
    public MathValue sqrt(boolean complexAllowed) {
        if (imaginary == 0 && real >= 0) return new MathValue(Math.sqrt(real), 0, exact == null ? null : exact.sqrt());
        if (!complexAllowed) throw new IllegalArgumentException("Square root requires a nonnegative argument in real mode");
        double a=Math.sqrt((abs()+Math.abs(real))/2), r, i;
        if (real >= 0) { r=a; i=imaginary/(2*a); }
        else { i=Math.copySign(a, imaginary == 0 ? 1 : imaginary); r=imaginary/(2*i); }
        return new MathValue(r,i);
    }
    public String expression() {
        if (exact != null) return "(" + exact.format(false) + ")";
        if (imaginary == 0) return "(" + Double.toString(real) + ")";
        return "(" + real + (imaginary < 0 ? "" : "+") + imaginary + "*i)";
    }
}
