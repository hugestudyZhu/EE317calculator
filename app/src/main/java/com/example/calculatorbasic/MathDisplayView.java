package com.example.calculatorbasic;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

/** The visible mathematical document is also the editor: no separate raw-source field. */
public final class MathDisplayView extends View {
    private int textColor=0xFF1C1C1E, accentColor=0xFFC45B19;
    private float sizeSp=24;
    private float fittedSize;
    private float insetDp=10;
    public void setInset(float value){insetDp=value;requestLayout();invalidate();}
    private boolean alignRight;
    public void setAlignRight(boolean value){alignRight=value;invalidate();}
    public void setPalette(int text,int accent){textColor=text;accentColor=accent;invalidate();}
    public void setTextSize(float size){sizeSp=size;requestLayout();invalidate();}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private MathEditor editor=new MathEditor();
    private boolean editable,blink=true,pendingReveal;
    private Runnable selectionChanged;
    private Box box;
    private final List<Caret> carets=new ArrayList<>();
    private final Runnable blinkCursor=new Runnable(){public void run(){if(!editable)return;blink=!blink;invalidate();postDelayed(this,500);}};
    public MathDisplayView(Context context){super(context);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));setFocusable(true);setContentDescription("Mathematical expression");}
    public void setEditor(MathEditor value,Runnable changed){editor=value;editable=true;selectionChanged=changed;updateDocument();}
    public void setPreview(MathEditor value){editor=value;editable=false;updateDocument();}
    public void setExpression(String source){
        source=source==null?"":source;
        java.util.regex.Matcher mixed=java.util.regex.Pattern.compile("([−-]?)([0-9]+) ([0-9]+)/([0-9]+)").matcher(source);
        if(mixed.matches())source=mixed.group(1)+"mixed("+mixed.group(2)+","+mixed.group(3)+","+mixed.group(4)+")";
        editor=MathEditor.fromSource(source);editable=false;updateDocument();
    }
    public void revealCursor(){pendingReveal=true;invalidate();}
    public void updateDocument(){box=new RowBox(editor.root);setContentDescription(editor.source().isEmpty()?"Empty expression":editor.source());blink=true;requestLayout();invalidate();removeCallbacks(blinkCursor);if(editable&&isAttachedToWindow())postDelayed(blinkCursor,500);}
    private float textSize(){return fittedSize>0?fittedSize:sizeSp*getResources().getDisplayMetrics().scaledDensity;}
    private float padding(){return insetDp*getResources().getDisplayMetrics().density;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(editable)postDelayed(blinkCursor,500);}
    @Override protected void onDetachedFromWindow(){removeCallbacks(blinkCursor);super.onDetachedFromWindow();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(box==null)box=new RowBox(editor.root);
        float base=sizeSp*getResources().getDisplayMetrics().scaledDensity;box.measure(base);
        int natural=(int)Math.ceil(box.height+2*padding()),height=resolveSize(natural,heightSpec);
        fittedSize=height<natural?base*Math.max(.1f,(height-2*padding())/box.height):base;
        box.measure(fittedSize);int width=(int)Math.ceil(box.width+2*padding());
        setMeasuredDimension(resolveSize(width,widthSpec),height);
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(box==null)return;box.measure(textSize());carets.clear();paint.setColor(textColor);box.draw(canvas,alignRight?Math.max(padding(),getWidth()-box.width-padding()):padding(),padding(),textSize());
        if(editable&&blink)for(Caret caret:carets)if(caret.row==editor.active&&caret.index==editor.position){paint.setColor(accentColor);paint.setStrokeWidth(2*getResources().getDisplayMetrics().density);canvas.drawLine(caret.x,caret.y,caret.x,caret.y+caret.height,paint);break;}
        if(editable&&pendingReveal){pendingReveal=false;Rect cursor=cursorBounds();post(()->requestRectangleOnScreen(cursor,false));}}
    public Rect cursorBounds(){for(Caret c:carets)if(c.row==editor.active&&c.index==editor.position)return new Rect((int)c.x-8,(int)c.y-8,(int)c.x+8,(int)(c.y+c.height)+8);return new Rect(0,0,1,1);}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){if(!editable)return super.onTouchEvent(event);if(event.getAction()==MotionEvent.ACTION_DOWN){requestFocus();return true;}if(event.getAction()==MotionEvent.ACTION_UP){Caret nearest=null;float score=Float.MAX_VALUE;for(Caret c:carets){float dx=event.getX()-c.x,dy=event.getY()-(c.y+c.height/2),distance=dx*dx+4*dy*dy;if(distance<score){score=distance;nearest=c;}}
            if(nearest!=null){editor.select(nearest.row,nearest.index);blink=true;invalidate();if(selectionChanged!=null)selectionChanged.run();}performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){if(!editable)return super.onKeyDown(keyCode,event);if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT)editor.move(-1);else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)editor.move(1);else if(keyCode==KeyEvent.KEYCODE_DPAD_UP)editor.vertical(-1);else if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN)editor.vertical(1);else return super.onKeyDown(keyCode,event);updateDocument();if(selectionChanged!=null)selectionChanged.run();return true;}
    private static final class Caret {final MathEditor.Row row;final int index;final float x,y,height;Caret(MathEditor.Row row,int index,float x,float y,float height){this.row=row;this.index=index;this.x=x;this.y=y;this.height=height;}}
    private abstract class Box {float width,height,baseline;abstract void measure(float size);abstract void draw(Canvas canvas,float x,float y,float size);}
    private final class TextBox extends Box {
        final String text;TextBox(String text){this.text=text.equals("pi")?"π":text.equals("*")?"×":text.equals("-")?"−":text;}
        void measure(float size){paint.setTextSize(size);width=paint.measureText(text);height=size*1.3f;baseline=size;}
        void draw(Canvas c,float x,float y,float size){paint.setTextSize(size);paint.setColor(textColor);c.drawText(text,x,y+baseline,paint);}
    }
    private final class RowBox extends Box {
        final MathEditor.Row row;final List<Box> children=new ArrayList<>();RowBox(MathEditor.Row row){this.row=row;for(MathEditor.Element e:row.elements)children.add(element(e));}
        void measure(float size){width=0;baseline=size;height=size*1.3f;float descent=size*.3f;for(Box b:children){b.measure(size);width+=b.width;baseline=Math.max(baseline,b.baseline);descent=Math.max(descent,b.height-b.baseline);}height=baseline+descent;if(children.isEmpty())width=size*.7f;}
        void draw(Canvas c,float x,float y,float size){float pen=x;carets.add(new Caret(row,0,pen,y+baseline-size,size*1.2f));
            if(children.isEmpty()&&editable){paint.setColor(row==editor.active?accentColor:textColor);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawRect(x+size*.1f,y+baseline-size*.8f,x+width-size*.1f,y+baseline+size*.1f,paint);paint.setStyle(Paint.Style.FILL);}
            for(int i=0;i<children.size();i++){Box b=children.get(i);b.draw(c,pen,y+baseline-b.baseline,size);pen+=b.width;carets.add(new Caret(row,i+1,pen,y+baseline-size,size*1.2f));}}
    }
    private final class Sequence extends Box {
        final List<Box> children;Sequence(Box... children){this.children=Arrays.asList(children);}
        void measure(float size){width=0;baseline=0;float descent=0;for(Box b:children){b.measure(size);width+=b.width;baseline=Math.max(baseline,b.baseline);descent=Math.max(descent,b.height-b.baseline);}height=baseline+descent;}
        void draw(Canvas c,float x,float y,float size){for(Box b:children){b.draw(c,x,y+baseline-b.baseline,size);x+=b.width;}}
    }
    private final class Fraction extends Box {
        final Box numerator,denominator;Fraction(Box n,Box d){numerator=n;denominator=d;}
        void measure(float size){numerator.measure(size*.86f);denominator.measure(size*.86f);width=Math.max(numerator.width,denominator.width)+size*.45f;height=numerator.height+denominator.height+size*.28f;baseline=numerator.height+size*.57f;}
        void draw(Canvas c,float x,float y,float size){numerator.draw(c,x+(width-numerator.width)/2,y,size*.86f);denominator.draw(c,x+(width-denominator.width)/2,y+numerator.height+size*.28f,size*.86f);paint.setColor(textColor);paint.setStrokeWidth(1.4f);c.drawLine(x,y+numerator.height+size*.11f,x+width,y+numerator.height+size*.11f,paint);}
    }
    private final class Power extends Box {
        final Box base,exponent;Power(Box base,Box exponent){this.base=base;this.exponent=exponent;}
        void measure(float size){base.measure(size);exponent.measure(size*.65f);width=base.width+exponent.width+size*.1f;height=base.height+exponent.height*.65f;baseline=base.baseline+exponent.height*.65f;}
        void draw(Canvas c,float x,float y,float size){base.draw(c,x,y+exponent.height*.65f,size);exponent.draw(c,x+base.width+size*.1f,y,size*.65f);}
    }
    private final class Root extends Box {
        final Box inside,index;Root(Box inside,Box index){this.inside=inside;this.index=index;}
        void measure(float size){inside.measure(size);if(index!=null)index.measure(size*.55f);float indexWidth=index==null?0:index.width*.8f;width=inside.width+size*.8f+indexWidth;height=Math.max(inside.height+size*.15f,index==null?0:index.height);baseline=inside.baseline+size*.15f;}
        void draw(Canvas c,float x,float y,float size){float indexWidth=index==null?0:index.width*.8f;if(index!=null)index.draw(c,x,y,size*.55f);float left=x+indexWidth,top=y+size*.06f;inside.draw(c,left+size*.8f,y+size*.15f,size);paint.setColor(textColor);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);Path path=new Path();path.moveTo(left+size*.05f,y+baseline-size*.15f);path.lineTo(left+size*.2f,y+baseline-size*.3f);path.lineTo(left+size*.4f,y+height-size*.1f);path.lineTo(left+size*.7f,top);path.lineTo(x+width,top);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
    }
    /** Limits remain editable slots; source order is function, lower bound, upper bound. */
    private final class Limits extends Box {
        final String symbol;final Box lower,upper;float signWidth,signTop;
        Limits(String symbol,Box lower,Box upper){this.symbol=symbol;this.lower=lower;this.upper=upper;}
        void measure(float size){lower.measure(size*.6f);upper.measure(size*.6f);paint.setTextSize(size*1.5f);signWidth=paint.measureText(symbol);width=Math.max(signWidth,Math.max(lower.width,upper.width))+size*.2f;signTop=upper.height;height=upper.height+size*1.65f+lower.height;baseline=upper.height+size*1.2f;}
        void draw(Canvas c,float x,float y,float size){upper.draw(c,x+(width-upper.width)/2,y,size*.6f);paint.setTextSize(size*1.5f);paint.setColor(textColor);c.drawText(symbol,x+(width-signWidth)/2,y+baseline,paint);lower.draw(c,x+(width-lower.width)/2,y+signTop+size*1.65f,size*.6f);}
    }
    private Box element(MathEditor.Element e){switch(e.kind){
        case ATOM:return new TextBox(e.text);
        case GROUP:return new Sequence(new TextBox("("),new RowBox(e.slots.get(0)),new TextBox(")"));
        case FRACTION:return new Fraction(new RowBox(e.slots.get(0)),new RowBox(e.slots.get(1)));
        case POWER:return new Power(powerBase(e.slots.get(0)),new RowBox(e.slots.get(1)));
        case ROOT:return e.text.equals("root")?new Root(new RowBox(e.slots.get(1)),new RowBox(e.slots.get(0))):new Root(new RowBox(e.slots.get(0)),e.text.equals("cbrt")?new TextBox("3"):null);
        default:
            if(e.slots.size()==3&&(e.text.equals("integral")||e.text.equals("sum")||e.text.equals("product"))){
                String symbol=e.text.equals("integral")?"∫":e.text.equals("sum")?"Σ":"Π";
                return new Sequence(new Limits(symbol,new RowBox(e.slots.get(1)),new RowBox(e.slots.get(2))),new RowBox(e.slots.get(0)),new TextBox(e.text.equals("integral")?" dx":""));
            }
            if(e.text.equals("diff")&&e.slots.size()==2)return new Sequence(new Fraction(new TextBox("d"),new TextBox("dx")),new TextBox("("),new RowBox(e.slots.get(0)),new TextBox(") | x="),new RowBox(e.slots.get(1)));
            if(e.text.equals("mixed")&&e.slots.size()==3)return new Sequence(new RowBox(e.slots.get(0)),new TextBox(" "),new Fraction(new RowBox(e.slots.get(1)),new RowBox(e.slots.get(2))));
            List<Box> parts=new ArrayList<>();parts.add(new TextBox(e.text+"("));for(int i=0;i<e.slots.size();i++){if(i>0)parts.add(new TextBox(", "));parts.add(new RowBox(e.slots.get(i)));}parts.add(new TextBox(")"));return new Sequence(parts.toArray(new Box[0]));}}
    private Box powerBase(MathEditor.Row row){
        boolean brackets=false;
        for(MathEditor.Element e:row.elements)if(e.kind==MathEditor.Kind.POWER||e.kind==MathEditor.Kind.ATOM&&e.text.matches("[+*/-]"))brackets=true;
        Box base=new RowBox(row);
        return brackets?new Sequence(new TextBox("("),base,new TextBox(")")):base;
    }
}
