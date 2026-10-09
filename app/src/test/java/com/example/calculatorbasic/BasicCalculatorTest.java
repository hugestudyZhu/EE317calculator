package com.example.calculatorbasic;

import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class BasicCalculatorTest {
    private BasicCalculator enter(String... keys) {
        BasicCalculator calculator = new BasicCalculator();
        for (String key : keys) calculator.press(key);
        return calculator;
    }

    @Test public void fourOperations() {
        assertEquals("12", enter("7", "+", "5", "=").display());
        assertEquals("-2", enter("3", "−", "5", "=").display());
        assertEquals("42", enter("6", "×", "7", "=").display());
        assertEquals("2.5", enter("5", "÷", "2", "=").display());
    }
    @Test public void decimalsAreExact() {
        assertEquals("0.3", enter("0", ".", "1", "+", "0", ".", "2", "=").display());
        assertEquals("0.12", enter(".", "1", ".", "2").display());
    }
    @Test public void operationsChainFromLeftToRight() {
        assertEquals("20", enter("2", "+", "3", "×", "4", "=").display());
    }
    @Test public void pendingOperatorCanBeReplaced() {
        assertEquals("6", enter("8", "+", "−", "2", "=").display());
    }
    @Test public void equalsCanRepeat() {
        assertEquals("11", enter("2", "+", "3", "=", "=", "=").display());
    }
    @Test public void newNumberAfterEqualsStartsFresh() {
        assertEquals("4", enter("2", "+", "3", "=", "4").display());
    }
    @Test public void negativeOperandAndSignToggle() {
        assertEquals("-6", enter("3", "×", "±", "2", "=").display());
        assertEquals("5", enter("5", "±", "±").display());
    }
    @Test public void percentConvertsDisplayedValue() {
        assertEquals("0.25", enter("2", "5", "%").display());
        assertEquals("2", enter("2", "5", "%", "2").display());
        assertEquals("0.5", enter("2", "×", "2", "5", "%", "=").display());
    }
    @Test public void zeroDivisionRecoversOnNextEntry() {
        BasicCalculator calculator = enter("8", "÷", "0", "=");
        assertTrue(calculator.hasError());
        calculator.press("2");
        assertFalse(calculator.hasError());
        assertEquals("2", calculator.display());
    }
    @Test public void clearRemovesPendingAndRepeatedOperations() {
        assertEquals("3", enter("2", "+", "8", "=", "AC", "3", "=").display());
    }
    @Test public void largeResultsCanBeUsedInNextOperation() {
        BasicCalculator calculator = enter("9", "9", "9", "9", "9", "9", "9", "9", "9", "9", "9", "9", "9", "9", "×", "1", "0", "=");
        assertTrue(calculator.display().contains("e"));
        calculator.press("÷"); calculator.press("1"); calculator.press("0"); calculator.press("=");
        assertEquals("99999999999999", calculator.display());
        calculator.press("±"); calculator.press("2");
        assertEquals("2", calculator.display());
    }
    @Test public void stateRestoresPendingOperation() throws Exception {
        BasicCalculator before = enter("1", "2", "+");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(before);
        BasicCalculator restored = (BasicCalculator) new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        restored.press("3"); restored.press("=");
        assertEquals("15", restored.display());
    }
}
