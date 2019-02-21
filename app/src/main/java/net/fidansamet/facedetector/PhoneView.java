package net.fidansamet.facedetector;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;

public class PhoneView extends TakeView {


    @Override
    public Canvas Draw(Canvas canvas, float RectLeft, float RectTop, float RectRight, float RectBottom, int color) {

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(3);
        canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);
        return canvas;
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
