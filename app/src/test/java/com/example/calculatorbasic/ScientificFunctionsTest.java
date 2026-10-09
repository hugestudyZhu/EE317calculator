package com.example.calculatorbasic;

import com.example.calculatorbasic.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScientificFunctionsTest {
    private ScientificEngine.Context context(){ScientificEngine.Context c=new ScientificCalculator().context;c.complex=false;return c;}
    private MathValue calc(String expression){return ScientificEngine.evaluate(expression,context());}
    @Test public void fractionsRemainExact(){assertEquals("1/2",calc("1/3+1/6").exact.format(false));assertEquals("-3/2",calc("frac(-3,2)").exact.format(false));assertEquals("−1 1/2",calc("mixed(-1,1,2)").exact.format(true));}
    @Test public void decimalArithmeticPreservesRationalMetadata(){assertEquals("3/10",calc("0.1+0.2").exact.format(false));assertEquals("1/1000",calc("1e-3").exact.format(false));}
    @Test public void radicalsSimplifyAndRationalize(){assertEquals("sqrt(2)",calc("sqrt(8)/2").exact.format(false));assertEquals("sqrt(2)/2",calc("1/sqrt(2)").exact.format(false));assertEquals("6",calc("sqrt(2)*sqrt(18)").exact.format(false));}
    @Test public void piFormsRemainExact(){assertEquals("pi/2",calc("pi/3+pi/6").exact.format(false));assertEquals("1",calc("pi/pi").exact.format(false));assertEquals("pi^(2)",calc("pi^2").exact.format(false));}
    @Test public void exactUnsupportedSumsFallBackToNumerical(){MathValue result=calc("1+sqrt(2)");assertNull(result.exact);assertEquals(1+Math.sqrt(2),result.real,1e-14);}
    @Test public void percentAndRemainderHaveDistinctMeaning(){assertEquals(20,calc("200*10%").real,1e-12);assertEquals(1,calc("mod(7,3)").real,0);assertEquals(1.5,calc("150%").real,0);}
    @Test public void combinatoricsAndIntegers(){assertEquals(120,calc("5!").real,0);assertEquals(120,calc("ncr(10,3)").real,0);assertEquals(720,calc("npr(10,3)").real,0);assertEquals(6,calc("gcd(48,18)").real,0);assertEquals(144,calc("lcm(48,18)").real,0);}
    @Test public void nthRootsAndLogarithms(){assertEquals(-2,calc("root(-8,3)").real,1e-14);assertEquals(3,calc("log(8,2)").real,1e-14);assertEquals(0.001,calc("10^-3").real,1e-14);}
    @Test public void hyperbolicInversesAreStable(){assertEquals(2,calc("asinh(sinh(2))").real,1e-12);assertEquals(2,calc("acosh(cosh(2))").real,1e-12);assertEquals(0.5,calc("atanh(tanh(0.5))").real,1e-12);assertEquals(Math.log(2)+Math.log(1e200),calc("asinh(1e200)").real,1e-12);}
    @Test public void gradModeAndAngleConversions(){ScientificEngine.Context c=context();c.angle=ScientificEngine.Angle.GRAD;assertEquals(1,ScientificEngine.evaluate("sin(100)",c).real,1e-14);assertEquals(100,ScientificEngine.evaluate("asin(1)",c).real,1e-14);c.angle=ScientificEngine.Angle.RAD;assertEquals(Math.PI/2,ScientificEngine.evaluate("deg(90)",c).real,1e-14);}
    @Test public void complexArithmetic(){ScientificEngine.Context c=context();c.complex=true;MathValue z=ScientificEngine.evaluate("(1+2*i)/(3-i)",c);assertEquals(0.1,z.real,1e-14);assertEquals(0.7,z.imaginary,1e-14);MathValue root=ScientificEngine.evaluate("sqrt(-4)",c);assertEquals(0,root.real,0);assertEquals(2,root.imaginary,0);}
    @Test public void complexComponentsAndPolar(){ScientificEngine.Context c=context();c.complex=true;assertEquals(5,ScientificEngine.evaluate("abs(3+4*i)",c).real,1e-12);assertEquals(90,ScientificEngine.evaluate("arg(i)",c).real,1e-12);assertEquals(-2,ScientificEngine.evaluate("im(conj(1+2*i))",c).real,0);MathValue p=ScientificEngine.evaluate("polar(2,30)",c);assertEquals(Math.sqrt(3),p.real,1e-12);assertEquals(1,p.imaginary,1e-12);}
    @Test public void uppercaseEVariableDoesNotReplaceEulerConstant(){ScientificEngine.Context c=context();c.variables.put("e",MathValue.integer(7));assertEquals(7+Math.E,ScientificEngine.evaluate("E+e",c).real,1e-12);}
    @Test public void functionsAndLocalXUseSeparateScope(){ScientificEngine.Context c=context();c.variables.put("x",MathValue.integer(99));c.functions.put("f","x^2+1");c.functions.put("g","f(x)+2");assertEquals(12,ScientificEngine.evaluate("g(3)",c).real,1e-12);assertEquals(55,ScientificEngine.evaluate("sum(x,1,10)",c).real,1e-12);assertEquals(99,c.variables.get("x").real,0);}
    @Test public void recursiveFunctionsAreBounded(){ScientificEngine.Context c=context();c.functions.put("f","f(x)");assertThrows(IllegalArgumentException.class,()->ScientificEngine.evaluate("f(1)",c));}
    @Test public void calculusFunctionsCanBeNestedInExpressions(){assertEquals(12,calc("diff(x^3,2)").real,1e-6);assertEquals(1.0/3,calc("integral(x^2,0,1)").real,1e-10);assertEquals(120,calc("product(x,1,5)").real,0);}
    @Test public void invalidFunctionArgumentsAreRejected(){String[] invalid={"sqrt(-1)","ln(0)","atanh(1)","acosh(0)","root(-8,2)","root(8,0)","ncr(3,4)","factorial(2.5)","gcd(2.1,3)","log(2,1)","mod(1,0)","randint(5,1)","sin(1,2)","arg(0)","1+i"};for(String expression:invalid)assertThrows(expression,IllegalArgumentException.class,()->calc(expression));}
    @Test public void implicitProductsAfterVariablesWork(){ScientificEngine.Context c=context();assertEquals(6,ScientificEngine.compile("x(x+1)",c).evaluate(2).real,0);c.variables.put("a",MathValue.integer(3));assertEquals(9,ScientificEngine.evaluate("A(1+2)",c).real,0);}
    @Test public void complexDivisionDoesNotOverflowItsDenominator(){MathValue z=new MathValue(1e308,1e308).divide(new MathValue(1e308,1e308));assertEquals(1,z.real,0);assertEquals(0,z.imaginary,0);}
    @Test public void deeplyNestedInputIsBounded(){String expression="(".repeat(150)+"1"+")".repeat(150);assertThrows(IllegalArgumentException.class,()->calc(expression));}
}
