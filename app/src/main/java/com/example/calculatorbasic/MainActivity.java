package com.example.calculatorbasic;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import com.example.calculatorbasic.math.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Screen composition and event routing; calculator logic lives in the state classes. */
public class MainActivity extends Activity {
    private static final String[][] BASIC = {
        {"AC","±","%","÷"}, {"7","8","9","×"}, {"4","5","6","−"},
        {"1","2","3","+"}, {"0",".","="}
    };
    private final List<KeyButton> buttons = new ArrayList<>();
    private final List<LinearLayout> rows = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private CalculatorState state;
    private Palette palette;
    private ScrollView root;
    private LinearLayout content, keypad;
    private DisplayArea display;
    private TextView basicMode, mode, result, expression, status, hint;
    private HorizontalScrollView inputHost, exactHost;
    private MathDisplayView input, exactResult;
    private CalculatorSheets sheets;
    private ValueAnimator colorTransition;
    private long revision;
    private boolean busy;
    private boolean compactScience;

    @Override @SuppressWarnings("deprecation") public void onCreate(Bundle saved) {
        super.onCreate(saved);
        state = saved == null ? loadState() : (CalculatorState) saved.getSerializable("state");
        if (state == null) state = new CalculatorState();
        setContentView(R.layout.activity_main);
        root=findViewById(R.id.root); content=findViewById(R.id.content);
        keypad=findViewById(R.id.keypad); display=findViewById(R.id.display);
        basicMode=findViewById(R.id.basic_mode); mode=findViewById(R.id.mode);
        result=findViewById(R.id.result); expression=findViewById(R.id.expression);
        status=findViewById(R.id.science_status); hint=findViewById(R.id.gesture_hint);
        inputHost=findViewById(R.id.science_input); exactHost=findViewById(R.id.exact_result);
        input=new MathDisplayView(this); exactResult=new MathDisplayView(this);
        inputHost.addView(input,new HorizontalScrollView.LayoutParams(-2,-2));
        exactHost.addView(exactResult,new HorizontalScrollView.LayoutParams(-2,-2));
        input.setTextSize(22); input.setInset(8);
        exactResult.setTextSize(56); exactResult.setInset(2);
        exactResult.setAlignRight(true);
        result.setHorizontallyScrolling(false);
        if (Build.VERSION.SDK_INT < 26) result.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitLegacyResult());
        sheets=new CalculatorSheets(this);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        display.setGestures(()->switchMode(true),sheets::history);
        hint.setOnClickListener(v->sheets.history());
        mode.setOnClickListener(v->{if (!state.scientific) switchMode(true);});
        basicMode.setOnClickListener(v->{if (state.scientific) switchMode(true);});
        findViewById(R.id.appearance).setOnClickListener(v->sheets.appearance());
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->resizeKeys());
        input.setOnKeyListener((v,key,event)->{
            if (event.getAction()!=KeyEvent.ACTION_DOWN) return false;
            switch (key) {
                case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER: press("="); return true;
                case KeyEvent.KEYCODE_DEL: press("DEL"); return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: press("◀"); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: press("▶"); return true;
                case KeyEvent.KEYCODE_DPAD_UP: press("▲"); return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: press("▼"); return true;
                case KeyEvent.KEYCODE_TAB: state.science.editor.nextSlot(); refresh(false); return true;
                default:
                    if (event.isCtrlPressed() || event.isAltPressed()) return false;
                    int c=event.getUnicodeChar();
                    if (c>=32 && c<127) { insert(Character.toString((char)c)); return true; }
                    return false;
            }
        });
        applyMode(false);
        if (saved==null) enterAnimation(content,0);
    }

    CalculatorState state() { return state; }
    Palette palette() { return palette; }
    void setAppearance(int appearance) { state.appearance=appearance; applyMode(true); }
    int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }

    private void switchMode(boolean animate) {
        state.scientific=!state.scientific;
        state.science.shift=false;
        applyMode(animate);
    }
    void applyMode(boolean animate) {
        int previous=palette==null ? new Palette(state.scientific,state.appearance).canvas : palette.canvas;
        palette=new Palette(state.scientific,state.appearance);
        compactScience=state.scientific && getResources().getConfiguration().screenHeightDp<700;
        if (colorTransition!=null) colorTransition.cancel();
        root.setBackgroundColor(palette.canvas);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(palette.canvas));
        updateSystemBars();
        mode.setTextColor(state.scientific ? palette.accent : palette.muted);
        basicMode.setTextColor(state.scientific ? palette.muted : palette.accent);
        mode.setSelected(state.scientific); basicMode.setSelected(!state.scientific);
        ((ImageButton)findViewById(R.id.appearance)).setImageTintList(ColorStateList.valueOf(palette.muted));
        mode.setContentDescription(getString(R.string.switch_scientific));
        basicMode.setContentDescription(getString(R.string.switch_basic));
        if (Build.VERSION.SDK_INT>=26) mode.setTooltipText(getString(R.string.scientific));
        expression.setTextColor(palette.muted); status.setTextColor(palette.muted);
        hint.setTextColor(palette.muted);
        hint.setText(state.scientific ? R.string.swipe_science : R.string.swipe_basic);
        hint.setVisibility(state.scientific ? View.GONE : View.VISIBLE);
        display.setMinimumHeight(dp(state.scientific ? 178 : 170));
        LinearLayout.LayoutParams displayParams=(LinearLayout.LayoutParams)display.getLayoutParams();
        displayParams.height=state.scientific ? dp(178) : 0;
        display.setLayoutParams(displayParams);
        LinearLayout.LayoutParams inputParams=(LinearLayout.LayoutParams)inputHost.getLayoutParams();
        inputParams.height=dp(60); inputParams.weight=0; inputHost.setMinimumHeight(0);
        inputHost.setLayoutParams(inputParams);
        exactHost.getLayoutParams().height=dp(80);
        display.setPadding(0,dp(state.scientific ? 8 : 12),0,dp(state.scientific ? 8 : 18));
        input.setPalette(palette.ink,palette.accent); exactResult.setPalette(palette.ink,palette.accent);
        inputHost.setVisibility(state.scientific ? View.VISIBLE : View.GONE);
        status.setVisibility(state.scientific ? View.VISIBLE : View.GONE);
        expression.setVisibility(state.scientific ? View.GONE : View.VISIBLE);
        result.getLayoutParams().height=dp(state.scientific ? 80 : 110);
        if (Build.VERSION.SDK_INT >= 26) result.setAutoSizeTextTypeUniformWithConfiguration(20,state.scientific ? 48 : 88,2,TypedValue.COMPLEX_UNIT_SP);
        buildKeypad(); refresh(false);
        root.scrollTo(0,0); root.post(this::resizeKeys);
        if (animate && animationsEnabled()) {
            colorTransition=ValueAnimator.ofObject(new ArgbEvaluator(),previous,palette.canvas);
            colorTransition.setDuration(260);
            colorTransition.addUpdateListener(a->root.setBackgroundColor((int)a.getAnimatedValue()));
            colorTransition.start(); enterAnimation(content,state.scientific ? dp(10) : -dp(10));
        }
    }

    private void buildKeypad() {
        buttons.clear(); rows.clear(); keypad.removeAllViews();
        String[][] layout=state.scientific ? ScienceKeys.ROWS : BASIC;
        for (int i=0;i<layout.length;i++) {
            LinearLayout row=new LinearLayout(this); row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            row.setGravity(Gravity.CENTER); row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(state.scientific ? 48 : 56));
            if (i<layout.length-1) rp.bottomMargin=dp(state.scientific ? compactScience ? 4 : 6 : 12);
            keypad.addView(row,rp); rows.add(row);
            for (int j=0;j<layout[i].length;j++) {
                KeyButton button=new KeyButton(layout[i][j],state.scientific && i<5);
                boolean wide=!state.scientific && "0".equals(button.key);
                LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-1,wide ? 2 : 1);
                if (j<layout[i].length-1) bp.rightMargin=dp(state.scientific ? 6 : 12);
                row.addView(button,bp); buttons.add(button);
            }
        }
    }
    private void resizeKeys() {
        int width=root.getWidth()-root.getPaddingLeft()-root.getPaddingRight();
        if (width<=0) return;
        int inset=Math.max(dp(state.scientific ? 16 : 24),(width-dp(state.scientific ? 520 : 440))/2);
        int top=dp(state.scientific ? 8 : 24), bottom=dp(state.scientific ? 8 : 20);
        if (content.getPaddingLeft()!=inset || content.getPaddingTop()!=top) content.setPadding(inset,top,inset,bottom);
        int height=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();
        int cell=(width-2*inset-dp(36))/4;
        int rowHeight=state.scientific ? dp(compactScience ? 36 : 48) : Math.max(dp(52),Math.min(dp(84),Math.min(cell,(height-dp(286)-Math.max(dp(48),findViewById(R.id.header).getHeight()))/5)));
        for (LinearLayout row:rows) {
            if (row.getLayoutParams().height!=rowHeight) { row.getLayoutParams().height=rowHeight; row.requestLayout(); }
            // Basic's last row spans two number cells; science rows use equal weights.
            if (!state.scientific) for (int j=0;j<row.getChildCount();j++) {
                KeyButton button=(KeyButton)row.getChildAt(j);
                LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)button.getLayoutParams();
                int keyWidth="0".equals(button.key) ? cell*2+dp(12) : cell;
                if (p.width!=keyWidth) { p.width=keyWidth; button.setLayoutParams(p); }
            }
        }
    }

    void press(String key) {
        if (state.scientific) {
            String token=ScienceKeys.token(key,state.science.shift);
            switch (token) {
                case "SETUP": sheets.settings(); return;
                case "History": sheets.history(); return;
                case "Text": sheets.textEntry(); return;
                case "ALPHA": case "Vars": sheets.variables(); return;
                case "Catalog": sheets.catalog(); return;
                case "=": calculate(); return;
                default: insert(token); return;
            }
        }
        state.basic.press(key);
        if ("=".equals(key) && !state.basic.hasError() && state.basic.expression().endsWith("=")) {
            String source=state.basic.expression().replace(" =","");
            state.remember(source,state.basic.display(),false);
            try { state.science.context.variables.put("ans",ScientificEngine.evaluate(state.basic.display(),state.science.context)); }
            catch (IllegalArgumentException ignored) { /* Basic may show a rounded display. */ }
        }
        refresh(true);
    }
    void insert(String token) {
        revision++; state.science.press(token); refresh(true); input.revealCursor();
    }
    void replace(String source) {
        revision++; state.science.replace(source); refresh(true); input.revealCursor();
    }
    void settingsChanged() { revision++; state.science.reformat(); refresh(true); }
    private void calculate() {
        if (busy) return;
        final String source;
        try { source=state.science.calculationSource(); }
        catch (IllegalArgumentException ex) { state.science.error=ex.getMessage(); refresh(true); return; }
        ScientificEngine.Context context=state.science.context.copy();
        long current=revision; busy=true; status.setText(R.string.calculating);
        worker.submit(()->{
            MathValue value=null; String failure=null;
            try { value=ScientificEngine.evaluate(source,context); }
            catch (RuntimeException ex) { failure=ex.getMessage()==null ? "Invalid expression" : ex.getMessage(); }
            final MathValue answer=value; final String error=failure;
            runOnUiThread(()->{
                busy=false;
                if (isDestroyed() || current!=revision) return;
                if (error!=null) state.science.error=error;
                else { state.science.commit(answer); state.remember(source,state.science.result,true); }
                refresh(true);
            });
        });
    }

    void refresh(boolean animate) {
        String next=state.scientific ? state.science.result : state.basic.display();
        if (state.scientific && !state.science.error.isEmpty()) next="—";
        boolean changed=!next.contentEquals(result.getText());
        result.animate().cancel(); result.setText(next); result.setTextColor(palette.ink);
        result.setContentDescription(state.scientific && !state.science.error.isEmpty() ? state.science.error : next);
        expression.setText(state.basic.hasError() ? getString(R.string.error) : state.basic.expression());
        boolean math=state.scientific && state.science.evaluated && state.science.error.isEmpty()
                && state.science.exact && "Norm".equals(state.science.format) && state.science.answer.exact!=null;
        exactHost.setVisibility(math ? View.VISIBLE : View.GONE); result.setVisibility(math ? View.GONE : View.VISIBLE);
        if (math) exactResult.setExpression(state.science.result);
        if (state.scientific) {
            input.setTextSize(state.science.evaluated ? 15 : 22);
            input.setEditor(state.science.editor,()->{ revision++; state.science.evaluated=false; refresh(false); });
            status.setText(!state.science.error.isEmpty() ? state.science.error : busy ? getString(R.string.calculating)
                    : state.science.context.angle+" · "+(state.science.exact ? "Math" : "Decimal")+" · "+state.science.format+(state.science.shift ? " · SHIFT" : ""));
        }
        if (Build.VERSION.SDK_INT>=26 && state.scientific) result.setAutoSizeTextTypeUniformWithConfiguration(20,state.science.evaluated ? 64 : 48,2,TypedValue.COMPLEX_UNIT_SP);
        if (Build.VERSION.SDK_INT < 26) result.post(this::fitLegacyResult);
        if (animate && changed) { enterAnimation(math ? exactHost : result,0); }
        else { result.setAlpha(1); result.setTranslationY(0); }
        for (KeyButton button:buttons) button.style();
    }

    private final class KeyButton extends Button {
        final String key; final boolean small;
        final GradientDrawable shape=new GradientDrawable();
        final RippleDrawable ripple;
        KeyButton(String key,boolean small) {
            super(MainActivity.this); this.key=key; this.small=small;
            setAllCaps(false); setSingleLine(true);
            setIncludeFontPadding(false); setGravity(Gravity.CENTER);
            setMinWidth(0); setMinimumWidth(0); setMinHeight(0); setMinimumHeight(0); setPadding(0,0,0,0);
            setStateListAnimator(null); setElevation(0);
            boolean word=key.matches("[A-Z]{2,}|History|Undo|Text|Vars|Catalog"), exponent="×10ˣ".equals(key);
            int font=state.scientific ? word ? 11 : exponent ? 16 : small ? 14 : 24 : "AC".equals(key) ? 21 : 30;
            setTypeface(Typeface.create(word ? "sans-serif-medium" : small || exponent ? "sans-serif" : "sans-serif-light",Typeface.NORMAL));
            setLetterSpacing(word ? .025f : -.015f);
            setFontFeatureSettings("tnum");
            setTextSize(font);
            if (Build.VERSION.SDK_INT >= 26) setAutoSizeTextTypeUniformWithConfiguration(Math.min(font,state.scientific ? 10 : 16),font,1,TypedValue.COMPLEX_UNIT_SP);
            shape.setCornerRadius(dp(state.scientific ? palette.scienceRadius : palette.basicRadius));
            ripple=new RippleDrawable(ColorStateList.valueOf(palette.ripple),shape,null); setBackground(ripple);
            setOnClickListener(v->{ performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); press(key); });
        }
        void style() {
            boolean operation="÷×−+".contains(key), equals="=".equals(key);
            boolean selected=state.scientific ? "SHIFT".equals(key) && state.science.shift : key.equals(state.basic.activeOperator());
            boolean utility=small || "AC".equals(key) || "DEL".equals(key) || "±".equals(key) || "%".equals(key);
            boolean clear=state.scientific && ("AC".equals(key) || "DEL".equals(key));
            boolean secondary=state.scientific && ("ALPHA".equals(key) || "Vars".equals(key) || "Catalog".equals(key) || "×10ˣ".equals(key));
            boolean shift=state.scientific && "SHIFT".equals(key);
            shape.setColor(equals || selected ? palette.accent : clear ? palette.clear : secondary ? palette.secondarySurface : operation || shift ? palette.operation : utility ? palette.utility : palette.number);
            setTextColor(equals || selected ? palette.accentText : clear ? palette.clearText : secondary ? palette.secondary : operation || shift ? palette.accent : small ? palette.functionText : palette.ink);
            setSelected(selected);
            String label=state.scientific ? ScienceKeys.label(key,state.science.shift) : key;
            if ("×10ˣ".equals(key)) {
                SpannableString power=new SpannableString("×10x");
                power.setSpan(new SuperscriptSpan(),3,4,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                power.setSpan(new RelativeSizeSpan(.65f),3,4,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                setText(power);
            } else setText(label);
            setContentDescription(description(label));
        }
        @Override public void setPressed(boolean pressed) {
            boolean changed=pressed!=isPressed(); super.setPressed(pressed);
            if (!changed) return;
            if (!animationsEnabled()) { setScaleX(1); setScaleY(1); return; }
            animate().scaleX(pressed ? .94f : 1).scaleY(pressed ? .94f : 1).setDuration(pressed ? 85 : 220)
                    .setInterpolator(pressed ? new DecelerateInterpolator() : new OvershootInterpolator(1.1f)).start();
        }
    }
    private String description(String key) {
        switch (key) {
            case "AC": return getString(R.string.clear); case "±": case "(−)": return getString(R.string.sign);
            case "%": return getString(R.string.percent); case "÷": return getString(R.string.divide);
            case "×": return getString(R.string.multiply); case "−": return getString(R.string.subtract);
            case "+": return getString(R.string.add); case ".": return getString(R.string.decimal);
            case "×10ˣ": return getString(R.string.exponent);
            case "=": return getString(R.string.equals); default: return key;
        }
    }
    private void enterAnimation(View view,int offsetX) {
        view.animate().cancel();
        if (!animationsEnabled()) { view.setAlpha(1); view.setTranslationX(0); view.setTranslationY(0); return; }
        view.setAlpha(.5f); view.setTranslationX(offsetX); view.setTranslationY(offsetX==0 ? dp(5) : 0);
        view.animate().alpha(1).translationX(0).translationY(0).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
    }
    private boolean animationsEnabled() { return Build.VERSION.SDK_INT<26 || ValueAnimator.areAnimatorsEnabled(); }
    @SuppressWarnings("deprecation") private void updateSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            int flags=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(state.scientific ? 0 : flags,flags);
        } else {
            int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (!state.scientific) flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT>=26 && !state.scientific) flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            root.setSystemUiVisibility(flags);
        }
        getWindow().setStatusBarColor(palette.canvas);
        getWindow().setNavigationBarColor(!state.scientific && Build.VERSION.SDK_INT<26 ? palette.ink : palette.canvas);
    }
    private void fitLegacyResult() {
        int width=result.getWidth(); if (width<=0) return;
        float scaled=getResources().getDisplayMetrics().scaledDensity, size=(state.scientific ? 48 : 88)*scaled;
        result.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);
        float measured=result.getPaint().measureText(result.getText().toString());
        if (measured>width) result.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(20*scaled,size*width/measured));
    }
    private CalculatorState loadState() {
        String saved=getSharedPreferences("calculator",MODE_PRIVATE).getString("state","");
        if (!saved.isEmpty()) try (ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(Base64.decode(saved,Base64.DEFAULT)))) {
            return (CalculatorState)in.readObject();
        } catch (IOException | ClassNotFoundException | RuntimeException ignored) { }
        return new CalculatorState();
    }
    @Override protected void onSaveInstanceState(Bundle saved) { saved.putSerializable("state",state); super.onSaveInstanceState(saved); }
    @Override protected void onPause() {
        try (ByteArrayOutputStream bytes=new ByteArrayOutputStream(); ObjectOutputStream out=new ObjectOutputStream(bytes)) {
            out.writeObject(state); out.flush();
            getSharedPreferences("calculator",MODE_PRIVATE).edit().putString("state",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP)).apply();
        } catch (IOException ignored) { }
        super.onPause();
    }
    @Override protected void onDestroy() {
        worker.shutdownNow(); sheets.close(); if (colorTransition!=null) colorTransition.cancel(); super.onDestroy();
    }
}
