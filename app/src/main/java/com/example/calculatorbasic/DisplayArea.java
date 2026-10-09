package com.example.calculatorbasic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/** Routes display swipes before a nested expression scroller can consume them. */
public final class DisplayArea extends LinearLayout {
    private float startX, startY;
    private boolean dragging;
    private Runnable switchMode, history;
    public DisplayArea(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setGestures(Runnable switchMode, Runnable history) {
        this.switchMode=switchMode; this.history=history;
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN) {
            startX=event.getX(); startY=event.getY(); dragging=false;
            boolean handled=super.dispatchTouchEvent(event);
            getParent().requestDisallowInterceptTouchEvent(true);
            return handled;
        }
        if (action==MotionEvent.ACTION_MOVE && !dragging && moved(event)) {
            dragging=true;
            MotionEvent cancel=MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle();
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            boolean consumed=dragging;
            dragging=false;
            if (consumed && action==MotionEvent.ACTION_UP && moved(event)) {
                Runnable callback=Math.abs(event.getX()-startX)>Math.abs(event.getY()-startY) ? switchMode : history;
                if (callback!=null) callback.run();
            }
            boolean handled=consumed || super.dispatchTouchEvent(event);
            getParent().requestDisallowInterceptTouchEvent(false);
            return handled;
        }
        return dragging || super.dispatchTouchEvent(event);
    }
    @Override public void requestDisallowInterceptTouchEvent(boolean disallow) {
        // The outer page must leave vertical display swipes to this view too.
        super.requestDisallowInterceptTouchEvent(true);
    }
    private boolean moved(MotionEvent event) {
        float threshold=32*getResources().getDisplayMetrics().density;
        return Math.max(Math.abs(event.getX()-startX),Math.abs(event.getY()-startY))>threshold;
    }
}
