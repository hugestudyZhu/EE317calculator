package com.example.calculatorbasic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** A pocket calculator: operations are applied in the order entered. */
public final class BasicCalculator implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final MathContext PRECISION = new MathContext(14, RoundingMode.HALF_EVEN);
    private String entry = "0";
    private String operator;
    private BigDecimal accumulator;
    private String repeatOperator;
    private BigDecimal repeatOperand;
    private String expression = "";
    private boolean waiting;
    private boolean evaluated;
    private boolean error;

    public String display() { return error ? "—" : entry; }
    public String expression() { return expression; }
    public String activeOperator() { return waiting ? operator : null; }
    public boolean hasError() { return error; }

    public void press(String key) {
        if ("AC".equals(key)) { clear(); return; }
        if (error) clear();
        if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
            startEntry();
            if (entry.replace("-", "").replace(".", "").length() >= 14) return;
            if ("0".equals(entry)) entry = key;
            else if ("-0".equals(entry)) entry = "-" + key;
            else entry += key;
        } else if (".".equals(key)) {
            startEntry();
            if (!entry.contains(".")) entry += ".";
        } else if ("±".equals(key)) {
            startEntryIfWaiting();
            entry = entry.startsWith("-") ? entry.substring(1) : "-" + entry;
            repeatOperator = null;
        } else if ("%".equals(key)) {
            entry = format(value().movePointLeft(2));
            waiting = false;
            evaluated = true;
            repeatOperator = null;
            if (operator == null) expression = "";
        } else if ("=".equals(key)) {
            equalsPressed();
        } else if ("+".equals(key) || "−".equals(key) || "×".equals(key) || "÷".equals(key)) {
            operatorPressed(key);
        }
    }

    private void startEntry() {
        if (waiting || evaluated || entry.contains("e")) {
            entry = "0";
            if (operator == null) expression = "";
            waiting = false;
            evaluated = false;
            repeatOperator = null;
        }
    }

    private void startEntryIfWaiting() {
        if (waiting) { entry = "0"; waiting = false; }
        if (evaluated) expression = "";
    }

    private void operatorPressed(String next) {
        if (operator != null && !waiting) {
            BigDecimal answer = calculate(accumulator, value(), operator);
            if (error) return;
            entry = format(answer);
        }
        accumulator = value();
        operator = next;
        expression = format(accumulator) + " " + operator;
        waiting = true;
        evaluated = false;
        repeatOperator = null;
    }

    private void equalsPressed() {
        String operation = operator != null ? operator : repeatOperator;
        if (operation == null) return;
        BigDecimal left = operator != null ? accumulator : value();
        BigDecimal right = operator != null ? value() : repeatOperand;
        BigDecimal answer = calculate(left, right, operation);
        if (error) return;
        expression = format(left) + " " + operation + " " + format(right) + " =";
        entry = format(answer);
        repeatOperator = operation;
        repeatOperand = right;
        operator = null;
        accumulator = null;
        waiting = false;
        evaluated = true;
    }

    private BigDecimal calculate(BigDecimal left, BigDecimal right, String operation) {
        switch (operation) {
            case "+": return left.add(right, PRECISION);
            case "−": return left.subtract(right, PRECISION);
            case "×": return left.multiply(right, PRECISION);
            case "÷":
                if (right.signum() == 0) { clear(); error = true; return BigDecimal.ZERO; }
                return left.divide(right, PRECISION);
            default: throw new IllegalArgumentException(operation);
        }
    }

    private BigDecimal value() { return new BigDecimal(entry); }

    private static String format(BigDecimal number) {
        BigDecimal normalized = number.round(PRECISION).stripTrailingZeros();
        if (normalized.signum() == 0) return "0";
        BigDecimal magnitude = normalized.abs();
        if (magnitude.compareTo(new BigDecimal("1e14")) >= 0
                || magnitude.compareTo(new BigDecimal("1e-8")) < 0) {
            return normalized.toEngineeringString().replace("E+", "e").replace("E", "e");
        }
        return normalized.toPlainString();
    }

    private void clear() {
        entry = "0";
        expression = "";
        operator = null;
        accumulator = null;
        repeatOperator = null;
        repeatOperand = null;
        waiting = false;
        evaluated = false;
        error = false;
    }
}
