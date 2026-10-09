package com.example.calculatorbasic.math;

import java.util.*;
import java.util.function.DoubleUnaryOperator;

public final class ScientificMath {
    private ScientificMath() {}
    public static double finite(double n) { if(!Double.isFinite(n)) throw new IllegalArgumentException("Result out of range or function undefined"); return n; }
    public static double derivative(DoubleUnaryOperator f,double x) {
        finite(x); double h=Math.max(1,Math.abs(x))*1e-4;
        double d1=(f.applyAsDouble(x+h)-f.applyAsDouble(x-h))/(2*h);
        double d2=(f.applyAsDouble(x+h/2)-f.applyAsDouble(x-h/2))/h;
        return finite((4*d2-d1)/3);
    }
    public static double integral(DoubleUnaryOperator f,double a,double b) {
        finite(a); finite(b); if(a==b) return 0;
        if(a>b) return -integral(f,b,a);
        double mid=a+(b-a)/2,fa=finite(f.applyAsDouble(a)),fb=finite(f.applyAsDouble(b)),fm=finite(f.applyAsDouble(mid));
        double whole=finite((b-a)*(fa+4*fm+fb)/6);
        return adaptive(f,a,b,fa,fm,fb,whole,1e-9*Math.max(1,Math.abs(whole)),22,new int[]{0});
    }
    private static double adaptive(DoubleUnaryOperator f,double a,double b,double fa,double fm,double fb,double whole,double tolerance,int depth,int[] calls) {
        if(Thread.currentThread().isInterrupted()) throw new IllegalArgumentException("Calculation cancelled");
        if((calls[0]+=2)>150000) throw new IllegalArgumentException("Integral did not converge; split the interval or check singularities");
        double m=a+(b-a)/2,fl=finite(f.applyAsDouble(a+(m-a)/2)),fr=finite(f.applyAsDouble(m+(b-m)/2));
        double l=(m-a)*(fa+4*fl+fm)/6,r=(b-m)*(fm+4*fr+fb)/6,delta=l+r-whole;
        if(Math.abs(delta)<=15*tolerance) return finite(l+r+delta/15);
        if(depth==0) throw new IllegalArgumentException("Integral accuracy not reached; check singularities in the interval");
        return finite(adaptive(f,a,m,fa,fl,fm,l,tolerance/2,depth-1,calls)+adaptive(f,m,b,fm,fr,fb,r,tolerance/2,depth-1,calls));
    }
    public static double series(DoubleUnaryOperator f,double lower,double upper,boolean product) {
        long a=ScientificEngine.integer(lower,-1000000000,1000000000),b=ScientificEngine.integer(upper,a,1000000000);
        if(b-a>100000) throw new IllegalArgumentException("Maximum 100001 terms");
        double result=product?1:0,correction=0;
        for(long i=a;i<=b;i++) {
            if(Thread.currentThread().isInterrupted()) throw new IllegalArgumentException("Calculation cancelled");
            double value=finite(f.applyAsDouble(i));
            if(product) result=finite(result*value);
            else { double y=value-correction,t=result+y; correction=(t-result)-y; result=finite(t); }
        }
        return result;
    }
}
