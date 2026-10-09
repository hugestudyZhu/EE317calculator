package com.example.calculatorbasic;

import com.example.calculatorbasic.math.*;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScientificCalculatorTest {
    private void calculate(ScientificCalculator s) {
        s.commit(ScientificEngine.evaluate(s.calculationSource(),s.context.copy()));
    }
    @Test public void fractionEditingAndExactDecimalSwitch() {
        ScientificCalculator s=new ScientificCalculator();
        for (String token:new String[]{"1","frac(,)","2"}) s.press(token);
        calculate(s); assertEquals("1/2",s.result);
        s.press("S⇔D"); assertEquals("0.5",s.result);
    }
    @Test public void completedAnswerCanBeUsedInPower() {
        ScientificCalculator s=new ScientificCalculator(); s.replace("2+3");calculate(s);
        s.press("^2");calculate(s);assertEquals("25",s.result);
        s.press("7");assertEquals("7",s.editor.source());
    }
    @Test public void undoRestoresEditAndIncompleteSlotsAreRejected() {
        ScientificCalculator s=new ScientificCalculator();s.press("sqrt(");
        assertThrows(IllegalArgumentException.class,s::calculationSource);
        s.press("9");s.press("Undo");assertTrue(s.editor.incomplete());
        s.press("Undo");assertEquals("",s.editor.source());
    }
    @Test public void secondaryKeysInsertInverseFunctions() {
        ScientificCalculator s=new ScientificCalculator();s.press("SHIFT");
        assertEquals("sin⁻¹",ScienceKeys.label("sin",s.shift));
        s.press(ScienceKeys.token("sin",s.shift));s.press("1");calculate(s);
        assertEquals(90,s.answer.real,1e-12);assertFalse(s.shift);
    }
    @Test public void memoryAndAnsUseCalculatedValues() {
        ScientificCalculator s=new ScientificCalculator();s.replace("3+4");calculate(s);
        s.press("M+");s.replace("M+Ans");calculate(s);assertEquals("14",s.result);
        s.press("M−");assertEquals(-7,s.context.variables.get("m").real,0);
    }
    @Test public void setupFormatsKeepExactAndDecimalOutputsDistinct() {
        ScientificCalculator s=new ScientificCalculator();s.replace("7/3");calculate(s);
        s.mixed=true;s.reformat();assertEquals("2 1/3",s.result);
        s.format="Fix";s.digits=2;s.reformat();assertEquals("2.33",s.result);
        s.format="Sci";s.digits=3;s.reformat();assertEquals("2.33e+00",s.result);
    }
    @Test public void variablesAndCustomFunctionsEvaluate() {
        ScientificCalculator s=new ScientificCalculator();s.context.variables.put("a",MathValue.integer(3));
        s.context.functions.put("f","x^2+A");s.replace("f(4)");calculate(s);assertEquals("19",s.result);
    }
    @Test public void historyIsBoundedAndReplaysEditableExpression() {
        CalculatorState state=new CalculatorState();
        for (int i=0;i<105;i++) state.remember("1/2", "1/2",true);
        assertEquals(100,state.history.size());state.replay(state.history.get(0));
        assertTrue(state.scientific);state.science.editor.vertical(1);
        assertEquals(.5,ScientificEngine.evaluate(state.science.editor.source(),state.science.context).real,0);
    }
    @Test public void modeChangesRetainSeparateDrafts() {
        CalculatorState state=new CalculatorState();state.basic.press("8");state.science.replace("sin(30)");
        state.scientific=true;state.scientific=false;
        assertEquals("8",state.basic.display());assertEquals("sin(30)",state.science.editor.source());
    }
    @Test public void persistenceRetainsModeVariablesHistoryAndCaret() throws Exception {
        CalculatorState before=new CalculatorState();before.scientific=true;
        before.science.press("frac(,)");before.science.press("1");before.science.press("▶");before.science.press("2");
        before.science.context.variables.put("a",MathValue.integer(9));before.remember("1+2","3",false);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(before);
        CalculatorState after=(CalculatorState)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertTrue(after.scientific);assertEquals(1,after.history.size());assertEquals(9,after.science.context.variables.get("a").real,0);
        after.science.press("DEL");after.science.press("4");calculate(after.science);assertEquals("1/4",after.science.result);
    }
}
