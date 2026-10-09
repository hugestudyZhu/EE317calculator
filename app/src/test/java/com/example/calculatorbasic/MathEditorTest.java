package com.example.calculatorbasic;

import com.example.calculatorbasic.math.*;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

/** Tests the calculator document and editing rules, without Android UI interaction. */
public class MathEditorTest {
    private MathEditor keys(String... tokens){MathEditor e=new MathEditor();for(String t:tokens)e.insert(t);return e;}
    private double value(MathEditor e){assertFalse(e.incomplete());return ScientificEngine.evaluate(e.source(),new ScientificEngine.Context()).realOnly();}

    @Test public void fractionStartsInNumeratorAndMovesToDenominator(){
        MathEditor e=keys("frac(,)");assertTrue(e.incomplete());e.insert("1");e.move(1);e.insert("2");
        assertEquals("frac(1,2)",e.source());assertEquals(.5,value(e),0);e.move(1);e.insert("+");e.insert("3");assertEquals(3.5,value(e),0);
    }
    @Test public void fractionCapturesTheWholePrecedingNumber(){MathEditor e=keys("1","2",".","5","frac(,)","2");assertEquals("frac(12.5,2)",e.source());assertEquals(6.25,value(e),0);}
    @Test public void upDownEditsExistingFractionSlots(){MathEditor e=keys("frac(,)","1",",","2");e.vertical(-1);e.delete();e.insert("3");e.vertical(1);e.delete();e.insert("4");assertEquals(.75,value(e),0);}
    @Test public void semicolonMovesToNextSlot(){MathEditor e=keys("frac(,)","2",";","5",";");assertSame(e.root,e.active);assertEquals(.4,value(e),0);}
    @Test public void nestedFractionsKeepTheirOwnCursor(){MathEditor e=keys("frac(,)","1",",","frac(,)","2",",","3");assertEquals(1.5,value(e),0);e.exitSlot();e.exitSlot();assertSame(e.root,e.active);}
    @Test public void rootDegreeAndRadicandAreSeparateSlots(){assertEquals(-2,value(keys("root(,)","3",",","-","8")),1e-14);assertEquals(3,value(keys("sqrt(","9")),0);}
    @Test public void exponentIsAnEditableSlot(){MathEditor e=keys("1","2","^","2");assertEquals(144,value(e),0);e.move(1);e.insert("+");e.insert("1");assertEquals(145,value(e),0);}
    @Test public void fixedPowersContinueOutsideExponent(){assertEquals(10,value(keys("3","^2","+","1")),0);assertEquals(.25,value(keys("4","^(-1)")),0);}
    @Test public void postfixBelongsToThePoweredOrDividedOperand(){assertEquals(14400,value(keys("5","!","^2")),0);assertEquals(.1,value(keys("2","0","%","frac(,)","2")),1e-14);}
    @Test public void scientificNotationIsCapturedAsOneOperand(){MathEditor e=MathEditor.fromSource("1.25e-3");e.insert("^2");assertEquals(1.5625e-6,value(e),1e-20);}
    @Test public void constantsAndFunctionsMultiplyWithoutMergingNumbers(){assertEquals(6*Math.E,value(keys("2","e","3")),1e-12);assertEquals(Math.PI*.5,value(keys("pi","sin(","3","0")),1e-12);assertEquals(6,value(keys("(","2",")","3")),0);}
    @Test public void exponentialCatalogAndTimesTenKeysWork(){assertEquals(1000,value(keys("10^(","3")),0);assertEquals(.003,value(keys("3","*10^","-","3")),1e-14);}
    @Test public void replacingAnOperatorDoesNotProduceAnInvalidExpression(){assertEquals(6,value(keys("2","+","*","3")),0);assertEquals(-6,value(keys("2","*","NEG","3")),0);}
    @Test public void constantsAndPrefixesInsertCompleteMathematicalOperands(){MathEditor e=keys("2");e.insertSource("1.602176634E-19");assertEquals(3.204353268e-19,value(e),1e-30);e.insertSource("*10^(-3)");assertEquals(3.204353268e-22,value(e),1e-32);}
    @Test public void deleteEntersAStructureThenRemovesItsContents(){MathEditor e=keys("sqrt(","9",")");e.delete();assertEquals("sqrt(9)",e.source());e.delete();assertTrue(e.incomplete());e.delete();assertEquals("",e.source());assertSame(e.root,e.active);}
    @Test public void importPreservesArithmeticPrecedence(){for(String source:new String[]{"1+2/3","2*3/4+5","2^3^2","-2^2","(-2)^2","frac(1,2)+sqrt(9)","root(-8,3)","1/(2+3)","rand()"}){MathEditor e=MathEditor.fromSource(source);if(source.equals("rand()")){assertEquals(source,e.source());continue;}assertEquals(source,ScientificEngine.evaluate(source,new ScientificEngine.Context()).realOnly(),value(e),1e-12);}}
    @Test public void emptyOrInvalidImportedFunctionsDoNotBecomeMalformedRoots(){assertEquals(MathEditor.Kind.FUNCTION,MathEditor.fromSource("sqrt()").root.elements.get(0).kind);assertEquals(MathEditor.Kind.FUNCTION,MathEditor.fromSource("sqrt(1,2)").root.elements.get(0).kind);}
    @Test public void replayDoesNotAccumulateParenthesesInPowers(){String source=keys("1","2","^","2").source();for(int i=0;i<20;i++){MathEditor replay=MathEditor.fromSource(source);assertEquals(source,replay.source());assertEquals(144,value(replay),0);}}
    @Test public void negativeAndSmallAnswersRemainSingleOperands(){for(MathValue n:new MathValue[]{MathValue.of(-2.5),MathValue.of(1.234e-120),MathValue.integer(-2)}){MathEditor e=MathEditor.fromSource(n.expression());e.insert("^2");assertEquals(n.real*n.real,value(e),Math.abs(n.real*n.real)*1e-14);}}
    @Test public void serializationRestoresTheActiveNestedSlot() throws Exception {
        MathEditor original=keys("frac(,)","1",",","sqrt(","9");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(original);}MathEditor restored;try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){restored=(MathEditor)in.readObject();}
        restored.delete();restored.insert("4");assertEquals("frac(1,sqrt(4))",restored.source());assertEquals(.5,value(restored),0);
    }
}
