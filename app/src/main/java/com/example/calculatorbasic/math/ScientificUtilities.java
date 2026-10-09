package com.example.calculatorbasic.math;

import java.math.BigInteger;

/** Approximate fraction conversion used by SETUP. */
public final class ScientificUtilities {
    private ScientificUtilities() {}
    public static ExactNumber rationalApproximation(double value) {
        if(!Double.isFinite(value)||Math.abs(value)>1e12)return null;
        long h0=0,h1=1,k0=1,k1=0;double x=Math.abs(value);
        for(int i=0;i<40;i++) {
            long a=(long)Math.floor(x);if(a>1000000000000L)break;
            if(h1!=0&&a>(Long.MAX_VALUE-h0)/h1||k1!=0&&a>(1000000-k0)/k1)break;
            long h=a*h1+h0,k=a*k1+k0;if(k==0||k>1000000)break;
            h0=h1;h1=h;k0=k1;k1=k;
            if(Math.abs((double)h/k-Math.abs(value))<=1e-12*Math.max(1,Math.abs(value)))return new ExactNumber(BigInteger.valueOf(value<0?-h:h),BigInteger.valueOf(k));
            double fraction=x-a;if(fraction==0)break;x=1/fraction;
        }return null;
    }
}
