package net.fidansamet.facedetector;

import android.graphics.Canvas;
import android.widget.Button;

public abstract class TakeView {

    Button cap, clear;


    // when face detected according to device type call appropiate class


    public abstract Canvas Draw (Canvas canvas, float RectLeft, float RectTop, float RectRight, float RectBottom, int color);


}
