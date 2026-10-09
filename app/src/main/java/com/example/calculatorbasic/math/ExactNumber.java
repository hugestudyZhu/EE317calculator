package com.example.calculatorbasic.math;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Exact rational coefficient times a rational square root or an integer power of pi. */
public final class ExactNumber implements Serializable {
    private static final long serialVersionUID = 1L;
    public final BigInteger numerator, denominator, radicand;
    public final int piPower;
    public ExactNumber(BigInteger n, BigInteger d) { this(n, d, BigInteger.ONE, 0); }
    private ExactNumber(BigInteger n, BigInteger d, BigInteger r, int p) {
        if (d.signum() == 0) throw new IllegalArgumentException("Denominator cannot be zero");
        if (d.signum() < 0) { n = n.negate(); d = d.negate(); }
        BigInteger gcd = n.gcd(d);
        numerator = n.divide(gcd); denominator = d.divide(gcd);
        radicand = n.signum() == 0 ? BigInteger.ONE : r; piPower = n.signum() == 0 ? 0 : p;
    }
    public static ExactNumber decimal(String s) {
        BigDecimal d = new BigDecimal(s);
        if (Math.abs(d.scale()) > 1000 || d.precision() > 1000) return null;
        return d.scale() >= 0 ? new ExactNumber(d.unscaledValue(), BigInteger.TEN.pow(d.scale()))
                : new ExactNumber(d.unscaledValue().multiply(BigInteger.TEN.pow(-d.scale())), BigInteger.ONE);
    }
    public static ExactNumber integer(long n) { return new ExactNumber(BigInteger.valueOf(n), BigInteger.ONE); }
    public static ExactNumber pi() { return new ExactNumber(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, 1); }
    public boolean rational() { return radicand.equals(BigInteger.ONE) && piPower == 0; }
    public ExactNumber negate() { return new ExactNumber(numerator.negate(), denominator, radicand, piPower); }
    public ExactNumber add(ExactNumber b) {
        if (numerator.signum() == 0) return b;
        if (b.numerator.signum() == 0) return this;
        if (!radicand.equals(b.radicand) || piPower != b.piPower) return null;
        return bounded(new ExactNumber(numerator.multiply(b.denominator).add(b.numerator.multiply(denominator)),
                denominator.multiply(b.denominator), radicand, piPower));
    }
    public ExactNumber multiply(ExactNumber b) {
        return simplify(numerator.multiply(b.numerator), denominator.multiply(b.denominator),
                radicand.multiply(b.radicand), piPower + b.piPower);
    }
    public ExactNumber divide(ExactNumber b) {
        return simplify(numerator.multiply(b.denominator), denominator.multiply(b.numerator).multiply(b.radicand),
                radicand.multiply(b.radicand), piPower - b.piPower);
    }
    public ExactNumber sqrt() {
        if (!rational() || numerator.signum() < 0) return null;
        // sqrt(n/d) = sqrt(n*d)/d.
        return simplify(BigInteger.ONE, denominator, numerator.multiply(denominator), 0);
    }
    public ExactNumber pow(int n) {
        if (Math.abs((long)n) > 100) return null;
        ExactNumber result = integer(1);
        for (int i = 0; i < Math.abs(n); i++) {
            result = result.multiply(this);
            if (result == null) return null;
        }
        return n < 0 ? integer(1).divide(result) : result;
    }
    private static ExactNumber simplify(BigInteger n, BigInteger d, BigInteger r, int p) {
        if (r.signum() == 0) return integer(0);
        BigInteger root = integerSqrt(r);
        if (root.multiply(root).equals(r)) { n = n.multiply(root); r = BigInteger.ONE; }
        else if (r.bitLength() < 44) {
            long left = r.longValue(), outside = 1;
            for (long f = 2; f <= 100000 && f <= left / f; f++) {
                long square = f * f;
                while (left % square == 0) { outside *= f; left /= square; }
            }
            n = n.multiply(BigInteger.valueOf(outside)); r = BigInteger.valueOf(left);
        }
        return bounded(new ExactNumber(n, d, r, p));
    }
    private static BigInteger integerSqrt(BigInteger n) {
        if (n.signum() == 0) return BigInteger.ZERO;
        BigInteger x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2);
        while (true) { BigInteger next = x.add(n.divide(x)).shiftRight(1); if (next.compareTo(x) >= 0) return x; x = next; }
    }
    private static ExactNumber bounded(ExactNumber n) {
        return n.numerator.bitLength() > 4096 || n.denominator.bitLength() > 4096
                || n.radicand.bitLength() > 4096 || Math.abs(n.piPower) > 100 ? null : n;
    }
    public String format(boolean mixed) {
        if (rational()) {
            if (denominator.equals(BigInteger.ONE)) return numerator.toString();
            if (mixed && numerator.abs().compareTo(denominator) >= 0) {
                BigInteger[] q = numerator.abs().divideAndRemainder(denominator);
                return (numerator.signum() < 0 ? "−" : "") + q[0] + " " + q[1] + "/" + denominator;
            }
            return numerator + "/" + denominator;
        }
        String symbol = !radicand.equals(BigInteger.ONE) ? "sqrt(" + radicand + ")" : "";
        if (piPower != 0) symbol += (symbol.isEmpty() ? "" : "*") + "pi" + (piPower == 1 ? "" : "^(" + piPower + ")");
        String coefficient = numerator.equals(BigInteger.ONE) ? "" : numerator.equals(BigInteger.ONE.negate()) ? "-" : numerator + "*";
        return coefficient + symbol + (denominator.equals(BigInteger.ONE) ? "" : "/" + denominator);
    }
}
