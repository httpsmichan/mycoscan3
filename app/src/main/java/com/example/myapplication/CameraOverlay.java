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
    private Paint scanPaint; // Paint for moving scan line
    private RectF cropRectF;
    private final float cornerRadius = 30f; // corner radius
    private float scanY; // current Y position of scan line
    private final float scanSpeed = 5f; // pixels per frame

    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        // White border for crop guide
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        // Semi-transparent dark gray outside area (~60% opacity)
        dimPaint = new Paint();
        dimPaint.setColor(Color.parseColor("#66000000")); // 60% opacity
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        // Green scan line paint
        scanPaint = new Paint();
        scanPaint.setColor(Color.GREEN);
        scanPaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 4:5 aspect ratio crop (centered)
        int cropWidth = (int) (width * 0.9);
        int cropHeight = (int) (cropWidth * 1.37);
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

        // Step 3: Draw outer faded white border
        Paint outerBorder = new Paint();
        outerBorder.setColor(Color.parseColor("#80FFFFFF"));
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(1f);

        float outerWidthOffset = 30f;
        float outerHeightOffset = 40f;
        RectF outerRect = new RectF(
                cropRectF.left - outerWidthOffset,
                cropRectF.top - outerHeightOffset,
                cropRectF.right + outerWidthOffset,
                cropRectF.bottom + outerHeightOffset
        );
        canvas.drawRoundRect(outerRect, cornerRadius + 5f, cornerRadius + 5f, outerBorder);

        // Step 4: Draw inner solid white border
        canvas.drawRoundRect(cropRectF, cornerRadius, cornerRadius, borderPaint);

        // Step 4b: Draw green corners
        Paint cornerPaint = new Paint();
        cornerPaint.setColor(Color.GREEN);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(2f);

        RectF topLeftArc = new RectF(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius);
        RectF topRightArc = new RectF(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius);
        RectF bottomLeftArc = new RectF(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom);
        RectF bottomRightArc = new RectF(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom);

        canvas.drawArc(topLeftArc, 180, 90, false, cornerPaint);
        canvas.drawArc(topRightArc, 270, 90, false, cornerPaint);
        canvas.drawArc(bottomLeftArc, 90, 90, false, cornerPaint);
        canvas.drawArc(bottomRightArc, 0, 90, false, cornerPaint);

        // Step 5: Draw moving green scan line inside inner border
        if (scanY == 0) scanY = cropRectF.top; // initialize at top
        canvas.drawLine(cropRectF.left, scanY, cropRectF.right, scanY, scanPaint);

        // Update scan position
        scanY += scanSpeed;
        if (scanY > cropRectF.bottom) scanY = cropRectF.top; // loop back

        // Schedule next frame
        postInvalidateOnAnimation();
    }

    public RectF getCropRectF() {
        return cropRectF;
    }
}
