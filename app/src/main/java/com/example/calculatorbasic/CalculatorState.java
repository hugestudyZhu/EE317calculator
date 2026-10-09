package com.example.calculatorbasic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Both drafts and a shared, bounded calculation history. */
public final class CalculatorState implements Serializable {
    private static final long serialVersionUID = 1L;
    public final BasicCalculator basic = new BasicCalculator();
    public final ScientificCalculator science = new ScientificCalculator();
    public boolean scientific;
    public int appearance;
    public final List<Entry> history = new ArrayList<>();

    public void remember(String source, String answer, boolean scientific) {
        if (history.size() >= 100) history.remove(0);
        history.add(new Entry(source, answer, scientific));
    }
    public void replay(Entry item) {
        scientific = true;
        science.replace(item.source);
    }
    public static final class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String source, answer;
        public final boolean scientific;
        Entry(String source, String answer, boolean scientific) {
            this.source=source; this.answer=answer; this.scientific=scientific;
        }
    }
}
