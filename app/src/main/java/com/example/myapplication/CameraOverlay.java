package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlay extends View {

    private Paint borderPaint;
    private Paint dimPaint;
    private RectF cropRectF;
    private final float cornerRadius = 30f; // 20px radius

    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        // White border for crop guide
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);

        // Semi-transparent dark gray outside area
        dimPaint = new Paint();
        dimPaint.setColor(Color.parseColor("#B3000000")); // ~70% opacity dark gray
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 4:5 aspect ratio crop (centered)
        int cropWidth = (int) (width * 0.9);
        int cropHeight = (int) (cropWidth * 1.35);
        float left = (width - cropWidth) / 2f;
        float top = (height - cropHeight) / 2f;
        float right = left + cropWidth;
        float bottom = top + cropHeight;

        cropRectF = new RectF(left, top, right, bottom);

        // Step 1: Draw dark overlay over the whole screen
        canvas.drawRect(0, 0, width, height, dimPaint);

        // Step 2: Cut out the crop area (clear rectangle with rounded corners)
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(cropRectF, cornerRadius, cornerRadius, clearPaint);

        // Step 3: Draw the white border around crop rect
        canvas.drawRoundRect(cropRectF, cornerRadius, cornerRadius, borderPaint);
    }

    public RectF getCropRectF() {
        return cropRectF;
    }
}
