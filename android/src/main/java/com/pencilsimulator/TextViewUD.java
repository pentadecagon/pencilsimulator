package com.pencilsimulator;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class TextViewUD extends TextView {

    public TextViewUD(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public TextViewUD(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public TextViewUD(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        float py = this.getHeight()/2.0f;
        float px = this.getWidth()/2.0f;
        //Log.d("testUD", String.format("w: %d h: %d ", this.getWidth(), this.getHeight()));
        //Log.d("testUD", String.format("w: %f h: %f ", py, px));
        canvas.rotate(180, px, py);

        super.onDraw(canvas);

        canvas.restore();
    }
}
