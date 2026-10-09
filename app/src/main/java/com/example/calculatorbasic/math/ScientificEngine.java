package com.example.calculatorbasic.math;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;

/** Bounded expression parser with exact-number metadata and complex arithmetic. */
public final class ScientificEngine {
    private ScientificEngine() {}
    public enum Angle { DEG, RAD, GRAD;
        public double radians(double x) { return this == RAD ? x : x * (this == DEG ? Math.PI/180 : Math.PI/200); }
        public double fromRadians(double x) { return this == RAD ? x : x * (this == DEG ? 180/Math.PI : 200/Math.PI); }
    }
    public static final class Context implements Serializable {
        private static final long serialVersionUID = 1L;
        public Angle angle = Angle.DEG;
        public boolean complex, variablesAllowed = true;
        public final Map<String, MathValue> variables = new LinkedHashMap<>();
        public final Map<String, String> functions = new LinkedHashMap<>();
        public Context copy() {
            Context c = new Context(); c.angle=angle; c.complex=complex; c.variablesAllowed=variablesAllowed;
            c.variables.putAll(variables); c.functions.putAll(functions); return c;
        }
    }
    public interface Expression { MathValue evaluate(double x); }
    public interface Expression2D { MathValue evaluate(double x, double y); }
    private interface Node { MathValue value(Evaluation e); }
    private static final class Budget { int left=1000000; }
    private static final class Evaluation {
        final Context c; final Double x, y; final int depth; final Budget budget;
        Evaluation(Context c, Double x, int depth, Budget budget) { this(c,x,null,depth,budget); }
        Evaluation(Context c, Double x, Double y, int depth, Budget budget) { this.c=c; this.x=x; this.y=y; this.depth=depth; this.budget=budget; }
        Evaluation at(double x) { return new Evaluation(c,x,y,depth+1,budget); }
        void tick() {
            if (Thread.currentThread().isInterrupted()) throw new IllegalArgumentException("Calculation cancelled");
            if (--budget.left < 0 || depth > 32) throw new IllegalArgumentException("Calculation budget or function nesting limit exceeded");
        }
    }
    public static MathValue evaluate(String source, Context context) {
        Node n=new Parser(source,context,false).parse();
        return n.value(new Evaluation(context,null,0,new Budget()));
    }
    public static Expression compile(String source, Context context) {
        Context copy=context.copy(); Node n=new Parser(source,copy,true).parse();
        return x -> {
            if (!Double.isFinite(x)) throw new IllegalArgumentException("x must be finite");
            return n.value(new Evaluation(copy,x,0,new Budget()));
        };
    }
    public static Expression2D compile2D(String source, Context context) {
        Context copy=context.copy();Node n=new Parser(source,copy,true).parse();
        return (x,y)->{
            if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Coordinates must be finite");
            return n.value(new Evaluation(copy,x,y,0,new Budget()));
        };
    }
    private static final class Parser {
        final String s; final Context c; int p, depth, localX;
        Parser(String source, Context c, boolean allowX) {
            if (source == null || source.trim().isEmpty()) throw new IllegalArgumentException("Enter an expression");
            if (source.length()>4096) throw new IllegalArgumentException("Expression exceeds 4096 characters");
            s=source.replace('×','*').replace('÷','/').replace('−','-').replace("π","pi")
                    .replace("√","sqrt").replace("²","^2").replace("³","^3");
            this.c=c; localX=allowX ? 1 : 0;
        }
        Node parse() { Node n=expression(); space(); if(p!=s.length()) throw error("Unexpected trailing input"); return n; }
        Node expression() {
            Node n=term();
            while(true) { space(); if(take('+')) { Node a=n,b=term(); n=e->a.value(e).add(b.value(e)); }
                else if(take('-')) { Node a=n,b=term(); n=e->a.value(e).subtract(b.value(e)); } else return n; }
        }
        Node term() {
            Node n=unary();
            while(true) { space();
                if(take('*')) { Node a=n,b=unary(); n=e->a.value(e).multiply(b.value(e)); }
                else if(take('/')) { Node a=n,b=unary(); n=e->a.value(e).divide(b.value(e)); }
                else if(take('∠')) { Node a=n,b=unary(); n=e->polar(a.value(e).realOnly(),b.value(e).realOnly(),e.c.angle); }
                else if(p<s.length() && (s.charAt(p)=='(' || Character.isLetter(s.charAt(p)))) {
                    Node a=n,b=unary(); n=e->a.value(e).multiply(b.value(e));
                } else return n;
            }
        }
        Node unary() {
            if(++depth>128) throw error("Expression nesting is too deep");
            space(); Node n;
            if(take('+')) n=unary(); else if(take('-')) { Node a=unary(); n=e->a.value(e).negate(); } else n=power();
            depth--; return n;
        }
        Node power() {
            Node n=postfix(); space();
            if(take('^')) { Node a=n,b=unary(); return e->a.value(e).pow(b.value(e),e.c.complex); } return n;
        }
        Node postfix() {
            Node n=primary();
            while(true) { space();
                if(take('!')) { Node a=n; n=e->factorial(a.value(e).realOnly()); }
                else if(take('%')) { Node a=n; n=e->a.value(e).divide(MathValue.integer(100)); } else return n;
            }
        }
        Node primary() {
            space();
            if(take('(')) { Node n=expression(); expect(')'); return n; }
            if(p>=s.length()) throw error("Incomplete expression");
            char ch=s.charAt(p);
            if(Character.isDigit(ch)||ch=='.') {
                int start=p; boolean digit=false;
                while(p<s.length()&&Character.isDigit(s.charAt(p))) { p++; digit=true; }
                if(take('.')) while(p<s.length()&&Character.isDigit(s.charAt(p))) { p++; digit=true; }
                if(!digit) throw error("Invalid number format");
                if(p<s.length()&&(s.charAt(p)=='e'||s.charAt(p)=='E')) {
                    int q=p+1; if(q<s.length()&&(s.charAt(q)=='+'||s.charAt(q)=='-')) q++;
                    int before=q; while(q<s.length()&&Character.isDigit(s.charAt(q))) q++;
                    if(q>before) p=q;
                }
                String text=s.substring(start,p);
                MathValue v=new MathValue(Double.parseDouble(text),0,ExactNumber.decimal(text));
                return e->{ e.tick(); return v; };
            }
            if(!Character.isLetter(ch)) throw error("Unrecognized symbol");
            int start=p; while(p<s.length()&&(Character.isLetter(s.charAt(p))||s.charAt(p)=='_')) p++;
            String original=s.substring(start,p);
            String name=original.toLowerCase(Locale.US); space();
            if (original.matches("[A-FXYZM]")) {
                if (!c.variablesAllowed) throw error("Variables are disabled");
                return e -> { e.tick(); MathValue v=e.c.variables.get(name); if(v==null) throw new IllegalArgumentException("Variable is not assigned: "+original); return v; };
            }
            if("pi".equals(name)) return e->new MathValue(Math.PI,0,ExactNumber.pi());
            if("e".equals(name)) return e->MathValue.of(Math.E);
            if("i".equals(name)) return e->{ if(!e.c.complex) throw new IllegalArgumentException("Switch to Complex mode"); return new MathValue(0,1); };
            if(name.matches("[a-dxyz]|ans|m")) {
                if(!c.variablesAllowed && !("x".equals(name)&&localX>0)) throw error("Variables are disabled");
                return e->{e.tick();if("x".equals(name)&&e.x!=null)return MathValue.of(e.x);if("y".equals(name)&&e.y!=null)return MathValue.of(e.y);MathValue v=e.c.variables.get(name);if(v==null)throw new IllegalArgumentException("Variable is not assigned: "+name);return v;};
            }
            if(!take('(')) {
                if("x".equals(name) && localX==0 && !c.variablesAllowed) throw error("x is not allowed in this expression");
                if(!name.matches("[a-fxyz]|ans|m")) throw error("Unknown variable: "+name);
                if(!c.variablesAllowed && !("x".equals(name)&&localX>0)) throw error("Variables are disabled");
                return e->{ e.tick(); if("x".equals(name)&&e.x!=null) return MathValue.of(e.x);
                    MathValue v=e.c.variables.get(name); if(v==null) throw new IllegalArgumentException("Variable is not assigned: "+name); return v; };
            }
            boolean lazy=name.equals("diff")||name.equals("integral")||name.equals("sum")||name.equals("product");
            if(lazy) localX++;
            List<Node> args=new ArrayList<>(); space();
            if(!take(')')) { do { args.add(expression()); space(); } while(take(',')); expect(')'); }
            if(lazy) localX--;
            if(!supported(name)&&!c.functions.containsKey(name)) throw error("Unknown function: "+name);
            return e->{
                e.tick();
                if(lazy) {
                    int count=name.equals("diff")?2:3; arity(name,args.size(),count);
                    DoubleUnaryOperator f=x->args.get(0).value(e.at(x)).realOnly();
                    double a=args.get(1).value(e).realOnly();
                    if(name.equals("diff")) return MathValue.of(ScientificMath.derivative(f,a));
                    double b=args.get(2).value(e).realOnly();
                    if(name.equals("integral")) return MathValue.of(ScientificMath.integral(f,a,b));
                    return MathValue.of(ScientificMath.series(f,a,b,name.equals("product")));
                }
                MathValue[] values=new MathValue[args.size()]; for(int j=0;j<values.length;j++) values[j]=args.get(j).value(e);
                if(e.c.functions.containsKey(name)) {
                    arity(name,values.length,1); Node function=new Parser(e.c.functions.get(name),e.c,true).parse();
                    return function.value(e.at(values[0].realOnly()));
                }
                return function(name,values,e.c);
            };
        }
        void space() { while(p<s.length()&&Character.isWhitespace(s.charAt(p))) p++; }
        boolean take(char ch) { if(p<s.length()&&s.charAt(p)==ch) {p++; return true;} return false; }
        void expect(char ch) { space(); if(!take(ch)) throw error("Missing "+ch); }
        IllegalArgumentException error(String m) { return new IllegalArgumentException(m+" (position "+(p+1)+")"); }
    }
    private static final Set<String> FUNCTIONS=new HashSet<>(Arrays.asList(
            "sin","cos","tan","asin","acos","atan","sinh","cosh","tanh","asinh","acosh","atanh",
            "sqrt","cbrt","root","ln","log","exp","abs","floor","ceil","round","trunc","frac","mixed",
            "gcd","lcm","mod","npr","ncr","factorial","rand","randint","polar","re","im","arg","conj",
            "deg","rad","grad","dms","diff","integral","sum","product"));
    private static boolean supported(String n) { return FUNCTIONS.contains(n); }
    private static void arity(String n,int actual,int expected) { if(actual!=expected) throw new IllegalArgumentException(n+" requires "+expected+" arguments"); }
    public static long integer(double x,long min,long max) {
        if(!Double.isFinite(x)||x!=Math.rint(x)||x<min||x>max) throw new IllegalArgumentException("Requires an integer in the range "+min+" to "+max+" (integer)"); return (long)x;
    }
    private static MathValue factorial(double x) {
        long n=integer(x,0,170); BigInteger result=BigInteger.ONE;
        for(long j=2;j<=n;j++) result=result.multiply(BigInteger.valueOf(j));
        return new MathValue(result.doubleValue(),0,new ExactNumber(result,BigInteger.ONE));
    }
    public static MathValue polar(double r,double angle,Angle unit) {
        if(r<0) throw new IllegalArgumentException("Magnitude cannot be negative"); double a=unit.radians(angle);
        return new MathValue(r*Math.cos(a),r*Math.sin(a));
    }
    private static MathValue function(String name,MathValue[] v,Context c) {
        if(name.equals("rand")) { arity(name,v.length,0); return MathValue.of(ThreadLocalRandom.current().nextDouble()); }
        if(name.equals("mixed")) { arity(name,v.length,3); double whole=v[0].realOnly(); integer(whole,-9007199254740991L,9007199254740991L);
            MathValue fraction=v[1].divide(v[2]); return v[0].add(whole<0?fraction.negate():fraction); }
        if(name.equals("dms")) { arity(name,v.length,3); double d=v[0].realOnly(),m=v[1].realOnly(),s=v[2].realOnly();
            if(m<0||m>=60||s<0||s>=60) throw new IllegalArgumentException("Minutes and seconds must be in [0,60)");
            return MathValue.of(Math.copySign(Math.abs(d)+m/60+s/3600,d)); }
        Set<String> two=new HashSet<>(Arrays.asList("root","frac","gcd","lcm","mod","npr","ncr","randint","polar"));
        int count=two.contains(name)||name.equals("log")&&v.length==2?2:1; arity(name,v.length,count);
        MathValue a=v[0]; double x;
        switch(name) {
            case "sqrt": return a.sqrt(c.complex);
            case "frac": return a.divide(v[1]);
            case "re": return MathValue.of(a.real);
            case "im": return MathValue.of(a.imaginary);
            case "abs": return MathValue.of(a.abs());
            case "arg": return MathValue.of(c.angle.fromRadians(a.arg()));
            case "conj": return new MathValue(a.real,-a.imaginary,a.exact);
            case "ln":
                if(c.complex && (a.imaginary!=0||a.real<0)) return new MathValue(Math.log(a.abs()),a.arg());
                return MathValue.of(Math.log(a.realOnly()));
            case "exp": return new MathValue(Math.exp(a.real)*Math.cos(a.imaginary),Math.exp(a.real)*Math.sin(a.imaginary));
            case "polar": return polar(a.realOnly(),v[1].realOnly(),c.angle);
            default: x=a.realOnly();
        }
        double y=count==2?v[1].realOnly():0;
        switch(name) {
            case "sin": return MathValue.of(Math.sin(c.angle.radians(x)));
            case "cos": return MathValue.of(Math.cos(c.angle.radians(x)));
            case "tan": if(Math.abs(Math.cos(c.angle.radians(x)))<1e-14) throw new IllegalArgumentException("tan is undefined at this angle"); return MathValue.of(Math.tan(c.angle.radians(x)));
            case "asin": return MathValue.of(c.angle.fromRadians(Math.asin(x)));
            case "acos": return MathValue.of(c.angle.fromRadians(Math.acos(x)));
            case "atan": return MathValue.of(c.angle.fromRadians(Math.atan(x)));
            case "sinh": return MathValue.of(Math.sinh(x));
            case "cosh": return MathValue.of(Math.cosh(x));
            case "tanh": return MathValue.of(Math.tanh(x));
            case "asinh": return MathValue.of(Math.copySign(Math.abs(x)>1e150?Math.log(Math.abs(x))+Math.log(2):Math.log(Math.abs(x)+Math.hypot(x,1)),x));
            case "acosh": if(x<1) throw new IllegalArgumentException("acosh requires an argument ≥ 1"); return MathValue.of(x>1e150?Math.log(x)+Math.log(2):Math.log(x+Math.sqrt(x-1)*Math.sqrt(x+1)));
            case "atanh": if(Math.abs(x)>=1) throw new IllegalArgumentException("atanh requires an argument in (-1,1)"); return MathValue.of(0.5*(Math.log1p(x)-Math.log1p(-x)));
            case "cbrt": return MathValue.of(Math.cbrt(x));
            case "root": integer(y,1,1000); if(x<0&&((long)y%2==0)) throw new IllegalArgumentException("An even real root of a negative number is undefined"); return MathValue.of(Math.copySign(Math.pow(Math.abs(x),1/y),x));
            case "log": if(x<=0||count==2&&(y<=0||y==1)) throw new IllegalArgumentException("Invalid logarithm argument or base"); return MathValue.of(count==1?Math.log10(x):Math.log(x)/Math.log(y));
            case "floor": return MathValue.of(Math.floor(x));
            case "ceil": return MathValue.of(Math.ceil(x));
            case "round": return MathValue.of(Math.abs(x)>=0x1.0p52?x:Math.round(x));
            case "trunc": return MathValue.of(x<0?Math.ceil(x):Math.floor(x));
            case "factorial": return factorial(x);
            case "mod": integer(x,-9007199254740991L,9007199254740991L); integer(y,-9007199254740991L,9007199254740991L); if(y==0) throw new IllegalArgumentException("Division by zero"); return MathValue.integer((long)x%(long)y);
            case "gcd": case "lcm": {
                long aa=integer(x,0,9007199254740991L),bb=integer(y,0,9007199254740991L);
                BigInteger left=BigInteger.valueOf(aa),right=BigInteger.valueOf(bb),g=left.gcd(right);
                BigInteger result=name.equals("gcd")?g:(g.signum()==0?BigInteger.ZERO:left.divide(g).multiply(right));
                return new MathValue(result.doubleValue(),0,new ExactNumber(result,BigInteger.ONE));
            }
            case "npr": case "ncr": {
                int n=(int)integer(x,0,100000),k=(int)integer(y,0,n); if(name.equals("ncr")) k=Math.min(k,n-k);
                if(k>10000) throw new IllegalArgumentException("Combination is too large to calculate"); BigInteger r=BigInteger.ONE;
                for(int j=1;j<=k;j++) { r=r.multiply(BigInteger.valueOf(n-j+1)); if(name.equals("ncr")) r=r.divide(BigInteger.valueOf(j)); if(r.bitLength()>1024) throw new IllegalArgumentException("Result exceeds the numeric range"); }
                return new MathValue(r.doubleValue(),0,new ExactNumber(r,BigInteger.ONE));
            }
            case "randint": { long low=integer(x,-1000000000,1000000000),high=integer(y,low,1000000000); return MathValue.integer(ThreadLocalRandom.current().nextLong(low,high+1)); }
            case "deg": return MathValue.of(c.angle.fromRadians(Math.toRadians(x)));
            case "rad": return MathValue.of(c.angle.fromRadians(x));
            case "grad": return MathValue.of(c.angle.fromRadians(x*Math.PI/200));
            default: throw new IllegalArgumentException("Unknown function: "+name);
        }
    }
}
