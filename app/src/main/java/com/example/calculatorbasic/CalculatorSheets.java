package com.example.calculatorbasic;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import com.example.calculatorbasic.math.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Reusable themed sheets for the retained SETUP, Text, Vars and Catalog actions. */
final class CalculatorSheets {
    private final MainActivity host;
    private Dialog dialog;
    private LinearLayout body;
    private Palette p;
    CalculatorSheets(MainActivity host) { this.host=host; }
    void close() { if (dialog!=null) dialog.dismiss(); }
    private ScientificCalculator science() { return host.state().science; }

    private void open(String title) {
        if (dialog!=null) dialog.dismiss();
        p=host.palette(); dialog=new Dialog(host);
        LinearLayout panel=new LinearLayout(host); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(host.dp(24),host.dp(12),host.dp(24),host.dp(24));
        GradientDrawable background=new GradientDrawable(); background.setColor(p.canvas);
        background.setCornerRadii(new float[]{host.dp(28),host.dp(28),host.dp(28),host.dp(28),0,0,0,0});
        panel.setBackground(background);
        View handle=new View(host); GradientDrawable pill=new GradientDrawable(); pill.setColor(p.muted); pill.setCornerRadius(host.dp(2)); handle.setBackground(pill);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(host.dp(32),host.dp(4)); hp.gravity=Gravity.CENTER; hp.bottomMargin=host.dp(12); panel.addView(handle,hp);
        LinearLayout header=new LinearLayout(host); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text(title,22,p.ink); heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button close=button("×",()->dialog.dismiss()); close.setTextSize(26); close.setContentDescription(host.getString(R.string.close));
        header.addView(close,new LinearLayout.LayoutParams(host.dp(48),host.dp(48))); panel.addView(header);
        ScrollView scroll=new ScrollView(host); scroll.setFillViewport(false); scroll.setVerticalScrollBarEnabled(false);
        body=new LinearLayout(host); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        dialog.setContentView(panel);
        Window window=dialog.getWindow();
        if (window!=null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); window.setDimAmount(.35f);
            window.setGravity(Gravity.BOTTOM); window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        if (window!=null) {
            window.setLayout(Math.min(host.getResources().getDisplayMetrics().widthPixels,host.dp(560)),
                    Math.min(host.dp(560),(int)(host.getResources().getDisplayMetrics().heightPixels*.78)));
            window.setNavigationBarColor(p.canvas);
            if (Build.VERSION.SDK_INT>=30) window.getInsetsController().setSystemBarsAppearance(
                    host.state().scientific ? 0 : WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }
    private TextView text(String value,int size,int color) {
        TextView v=new TextView(host); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("tnum"); return v;
    }
    private Button button(String label,Runnable action) {
        Button b=new Button(host); b.setText(label); b.setAllCaps(false); b.setTextSize(15);
        b.setTextColor(p.ink); b.setBackgroundTintList(ColorStateList.valueOf(p.utility));
        b.setStateListAnimator(null); b.setElevation(0); b.setMinHeight(host.dp(48)); b.setOnClickListener(v->action.run()); return b;
    }
    private void choice(String label,Runnable action) {
        Button b=button(label,()->{dialog.dismiss();action.run();});
        b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setPadding(host.dp(16),host.dp(12),host.dp(16),host.dp(12));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(p.number); bg.setCornerRadius(host.dp(16)); b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=host.dp(8); body.addView(b,lp);
    }
    private void choices(String title,String[] options,IntConsumer action) {
        open(title); for (int i=0;i<options.length;i++) { final int selected=i; choice(options[i],()->action.accept(selected)); }
    }
    private void note(String value) {
        TextView v=text(value,13,p.muted); v.setPadding(0,host.dp(12),0,host.dp(12)); body.addView(v);
    }
    private void changed() { host.settingsChanged(); }
    private void error(RuntimeException ex) { Toast.makeText(host,ex.getMessage(),Toast.LENGTH_LONG).show(); }

    void appearance() {
        open(host.getString(R.string.appearance));
        note(host.getString(R.string.appearance_hint));
        int[] names={R.string.theme_graphite,R.string.theme_ocean,R.string.theme_matcha};
        int[] details={R.string.theme_graphite_detail,R.string.theme_ocean_detail,R.string.theme_matcha_detail};
        for (int i=0;i<names.length;i++) {
            final int selected=i;
            Palette preview=new Palette(host.state().scientific,i);
            LinearLayout card=new LinearLayout(host); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(host.dp(18),host.dp(14),host.dp(18),host.dp(14));
            GradientDrawable bg=new GradientDrawable(); bg.setColor(preview.utility); bg.setCornerRadius(host.dp(18));
            if (host.state().appearance==i) bg.setStroke(host.dp(1),preview.accent);
            card.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(preview.ripple),bg,null));
            LinearLayout title=new LinearLayout(host); title.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=text(host.getString(names[i]),16,preview.ink);
            name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            title.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            title.addView(text(host.state().appearance==i ? "✓" : "",18,preview.accent)); card.addView(title);
            TextView detail=text(host.getString(details[i]),12,preview.muted); detail.setPadding(0,host.dp(5),0,host.dp(12)); card.addView(detail);
            LinearLayout samples=new LinearLayout(host); samples.setGravity(Gravity.CENTER_VERTICAL);
            String[] labels={"sin","7","+","="};
            int[] fills={preview.secondarySurface,preview.number,preview.operation,preview.accent};
            int[] inks={preview.secondary,preview.ink,preview.accent,preview.accentText};
            for (int j=0;j<labels.length;j++) {
                TextView key=text(labels[j],j==0 ? 12 : 18,inks[j]); key.setGravity(Gravity.CENTER); key.setIncludeFontPadding(false);
                GradientDrawable shape=new GradientDrawable(); shape.setColor(fills[j]); shape.setCornerRadius(host.dp(preview.scienceRadius)); key.setBackground(shape);
                LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(0,host.dp(34),1); if (j<3) kp.rightMargin=host.dp(7); samples.addView(key,kp);
            }
            card.addView(samples);
            card.setContentDescription(host.getString(names[i])); card.setOnClickListener(v->{dialog.dismiss();host.setAppearance(selected);});
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.topMargin=host.dp(10); body.addView(card,cp);
        }
    }

    void history() {
        open(host.getString(R.string.history));
        note(host.getString(host.state().history.isEmpty() ? R.string.history_empty : R.string.history_hint));
        List<CalculatorState.Entry> items=new ArrayList<>(host.state().history); Collections.reverse(items);
        for (CalculatorState.Entry entry:items) choice(entry.source+"\n= "+entry.answer,()->{
            host.state().replay(entry); host.applyMode(true);
        });
        if (!items.isEmpty()) choice(host.getString(R.string.history_clear),()->{host.state().history.clear();history();});
    }
    void settings() {
        ScientificCalculator s=science();
        choices(host.getString(R.string.setup),new String[]{"Angle · "+s.context.angle,"Number format · "+s.format,
                "Result · "+(s.exact ? "Math" : "Decimal"),"Fraction · "+(s.mixed ? "Mixed" : "Improper"),
                "Complex display · "+(s.polar ? "r∠θ" : "a+bi"),"Approximate fraction conversion","Clear variables and functions"},i->{
            switch (i) {
                case 0: choices("Angle unit",new String[]{"DEG · Degrees","RAD · Radians","GRAD · Gradians"},j->{s.context.angle=ScientificEngine.Angle.values()[j];changed();}); break;
                case 1: choices("Number format",new String[]{"Norm","Fix","Sci","ENG"},j->{
                    String format=new String[]{"Norm","Fix","Sci","ENG"}[j];
                    if (j==1 || j==2) {
                        String[] digits=new String[j==1 ? 16 : 15];
                        for (int k=0;k<digits.length;k++) digits[k]=Integer.toString(j==1 ? k : k+1);
                        choices(j==1 ? "Decimal places" : "Significant digits",digits,k->{s.format=format;s.digits=j==1 ? k : k+1;changed();});
                    } else { s.format=format;changed(); }
                }); break;
                case 2: s.exact=!s.exact;changed();break;
                case 3: s.mixed=!s.mixed;changed();break;
                case 4: s.polar=!s.polar;changed();break;
                case 5:
                    ExactNumber fraction=s.answer.imaginary==0 ? ScientificUtilities.rationalApproximation(s.answer.real) : null;
                    open("Approximate fraction"); note(fraction==null ? "No suitable real fraction found." : fraction.format(s.mixed)+"\nNumerical approximation");break;
                default: choices("Clear variables and functions?",new String[]{host.getString(R.string.cancel),"Clear"},j->{
                    if (j==1) { for (String key:s.context.variables.keySet()) if (!"ans".equals(key)) s.context.variables.put(key,MathValue.integer(0)); s.context.functions.clear(); changed(); }
                });
            }
        });
    }
    void variables() {
        String[] names={"A","B","C","D","E","F","X","Y","Z","M","Ans"};
        String[] labels=new String[names.length+1];
        for (int i=0;i<names.length;i++) labels[i]=names[i]+" = "+science().format(science().context.variables.get(names[i].toLowerCase(Locale.US)));
        labels[names.length]="Assign variable / Memory";
        choices("ALPHA · Variables",labels,i->{
            if (i<names.length) host.insert(names[i]);
            else {
                String[] assign=Arrays.copyOf(names,names.length-1);
                choices("Assign variable",assign,j->edit("Variable "+assign[j],new String[]{"Value"},new String[]{science().answer.expression()},values->{
                    MathValue value=ScientificEngine.evaluate(values[0].isEmpty() ? "0" : values[0],science().context.copy());
                    science().context.variables.put(assign[j].toLowerCase(Locale.US),value);changed();
                }));
            }
        });
    }
    void catalog() {
        choices("Function catalog",ScienceKeys.CATEGORIES,i->{
            if (i==ScienceKeys.CATEGORIES.length-1) {
                choices("Custom functions",new String[]{"Insert f(x)","Insert g(x)","Define f(x) / g(x)"},j->{
                    if (j<2) host.insert(j==0 ? "f(" : "g(");
                    else edit("Define functions",new String[]{"f(x)","g(x)"},
                            new String[]{science().context.functions.getOrDefault("f",""),science().context.functions.getOrDefault("g","")},values->{
                        ScientificEngine.Context context=science().context.copy();
                        for (String value:values) if (!value.trim().isEmpty()) ScientificEngine.compile(value,context);
                        science().context.functions.clear();
                        if (!values[0].trim().isEmpty()) science().context.functions.put("f",values[0]);
                        if (!values[1].trim().isEmpty()) science().context.functions.put("g",values[1]);changed();
                    });
                });
            } else choices(ScienceKeys.CATEGORIES[i],ScienceKeys.CATALOG[i],j->host.insert(ScienceKeys.CATALOG[i][j]));
        });
    }
    void textEntry() {
        edit(host.getString(R.string.text_entry),new String[]{host.getString(R.string.expression_label)},
                new String[]{science().editor.source()},values->host.replace(values[0]));
    }
    private interface Apply { void run(String[] values); }
    private void edit(String title,String[] labels,String[] defaults,Apply apply) {
        open(title); EditText[] fields=new EditText[labels.length];
        for (int i=0;i<labels.length;i++) {
            note(labels[i]); EditText field=new EditText(host); field.setTextColor(p.ink); field.setTextSize(18);
            field.setBackgroundTintList(ColorStateList.valueOf(p.accent));
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setMaxLines(4); field.setText(defaults[i]); field.setSelectAllOnFocus(true);
            field.setContentDescription(labels[i]); body.addView(field,new LinearLayout.LayoutParams(-1,-2)); fields[i]=field;
        }
        Button applyButton=button(host.getString(R.string.apply),()->{
            String[] values=new String[fields.length]; for (int i=0;i<fields.length;i++) values[i]=fields[i].getText().toString();
            try { apply.run(values);dialog.dismiss(); } catch (RuntimeException ex) {error(ex);}
        });
        applyButton.setTextColor(p.accentText); applyButton.setBackgroundTintList(ColorStateList.valueOf(p.accent));
        body.addView(applyButton,new LinearLayout.LayoutParams(-1,host.dp(56)));
    }
}
