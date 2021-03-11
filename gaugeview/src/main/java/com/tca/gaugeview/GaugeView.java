package com.tca.gaugeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by TCA on 19-12-2017.
 */
public class GaugeView extends View{

    public static class Section{

        private final float startValue;
        private final float endValue;
        private final int color;

        public Section(float startValue, float endValue, int color) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.color = color;
        }

        public float getStartValue() {
            return startValue;
        }

        public float getEndValue() {
            return endValue;
        }

        public int getColor() {
            return color;
        }
    }

    private static final String TAG = GaugeView.class.getSimpleName();

    private float startValue=0F;
    private float endValue=100F;
    private float currentValue=65F;
    private float canvasCenterX;
    private float canvasCenterY;
    private float canvasWidth;
    private float canvasHeight;
    private float startAngle = 38F;
    private float sectionGap = 1.2F;

    private int textColorUnit = Color.parseColor("#AAAAAA");
    private int textColorValue = Color.parseColor("#DDDDDD");
    private float requestedTextSize = 0;
    private String upperText = "";
    private String lowerText = "km/hr";

    private Paint needlePaint;
    private Path needlePath;
    private Paint needlePaintAdj;
    private Path needlePathAdj;
    private Paint needleScrewPaint;
    private float needleWidth;
    private float needleLength;
    private int needleColor = Color.parseColor("#FF3030");
    private int needleColorAdj = Color.parseColor("#CC1010");
    private boolean isNeedleShadow = true;

    private float needleAngleToMoveAnimate = 5F;
    private long  needleTimeToAnimate= 5;
    private boolean animateNeedle = true;
    private long lastMoveTime = 0;

    float totalAngleForPlot;
    float valueDiff;
    float anglePerValue;

    private int mainClicks = 8;
    private int subclicks = 12;

    private ArrayList<Section> sections;
    private ArrayList<Paint> sectionPaints;
    private ArrayList<Paint> scalePaints;
    private ArrayList<Paint> scaleLabelPaints;

    private Paint rimPaint;
    private Paint rimCirclePaint;
    private Paint facePaint;
    private Paint rimShadowPaint;
    private Paint textPaint;

    private RectF rimRect;
    private RectF faceRect;
    private RectF zoneRect;
    private RectF innerFaceRect;
    private RectF scaleRect;

    private boolean isIntScale = true;
    private float needleAngle = startAngle;
    private float currentValueAngle = startAngle;

    public GaugeView(Context context) {
        super(context);
        initializeDrawing();
    }

    public GaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        applyAttrs(context, attrs);
        initializeDrawing();
    }

    public GaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyAttrs(context, attrs);
        initializeDrawing();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        applyAttrs(context, attrs);
        initializeDrawing();
    }

    /**
     * Set all values needed to construct the gauge view
     * @param startValue Start value of gauge view
     * @param endValue End value of gauge view
     * @param sections  ArrayList of Sections with in the gauge. Each section will contain start value end value and color associated with it.
     * @param currentValue
     */
    public void setGaugeValues(float startValue, float endValue, ArrayList<Section> sections, float currentValue){
        this.startValue = startValue;
        this.endValue = endValue;
        setSections(sections);
        this.currentValue = currentValue;
        reCalculateCurrentValueAngle();
        invalidate();
    }

    /**
     * Used to set / update shown value in gauge view.
     * @param currentValue  value that needs to be set as the current value.
     * @return True if current value is set to the gauge view.
     */
    public boolean setCurrenrValue(float currentValue){
        if(currentValue>=startValue && currentValue<=endValue){
            this.currentValue = currentValue;
            reCalculateCurrentValueAngle();
            invalidate();
            return true;
        }
        return false;
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, 0, 0);
        startValue = a.getFloat(R.styleable.GaugeView_minValue, startValue);
        endValue = a.getFloat(R.styleable.GaugeView_maxValue, endValue);
        isIntScale = a.getBoolean(R.styleable.GaugeView_intScale, isIntScale);
        currentValue = a.getFloat(R.styleable.GaugeView_initialValue, currentValue);
        isNeedleShadow = a.getBoolean(R.styleable.GaugeView_needleShadow, isNeedleShadow);
        requestedTextSize = a.getFloat(R.styleable.GaugeView_infoTextSize, requestedTextSize);
        lowerText = a.getString(R.styleable.GaugeView_unitText) == null ? lowerText : a.getString(R.styleable.GaugeView_unitText) ;
        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Log.i(TAG, "onSizeChanged");
        canvasWidth = (float) w;
        canvasHeight = (float) h;
        canvasCenterX = canvasWidth / 2f;
        canvasCenterY = canvasHeight / 2f;

        rimRect = new RectF(canvasWidth * .05f, canvasHeight * .05f, canvasWidth * 0.95f, canvasHeight * 0.95f);
        rimPaint.setShader(new LinearGradient(canvasWidth * 0.40f, canvasHeight * 0.0f, canvasWidth * 0.60f, canvasHeight * 1.0f,
                Color.parseColor("#c0c0c0"),
                Color.parseColor("#c8c8c8"),
                Shader.TileMode.CLAMP));

        float rimSize = 0.02f * canvasWidth;

        faceRect = new RectF();
        faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
                rimRect.right - rimSize, rimRect.bottom - rimSize);

        rimShadowPaint.setShader(new RadialGradient(0.5f * canvasWidth, 0.5f * canvasHeight, faceRect.width() / 2.0f,
                new int[]{0x00000000, 0x00000500, 0x50000500},
                new float[]{0.96f, 0.96f, 0.99f},
                Shader.TileMode.MIRROR));

        float faceMargin = 2 * rimSize;

        zoneRect = new RectF();
        zoneRect.set(rimRect.left + faceMargin, rimRect.top + faceMargin,
                rimRect.right - faceMargin, rimRect.bottom - faceMargin);

        float zoneSize = 4 * rimSize;

        innerFaceRect = new RectF();
        innerFaceRect.set(rimRect.left + zoneSize, rimRect.top + zoneSize,
                rimRect.right - zoneSize, rimRect.bottom - zoneSize);

        float scalePosition = 0.015f * canvasWidth;
        scaleRect = new RectF();
        scaleRect.set(innerFaceRect.left + scalePosition, innerFaceRect.top + scalePosition,
                innerFaceRect.right - scalePosition, innerFaceRect.bottom - scalePosition);

        if(scalePaints != null){
            for (int i = 0; i < scalePaints.size(); i++) {
                Paint scalePaint = scalePaints.get(i);
                Paint scaleLabelPaint = scaleLabelPaints.get(i);
                scalePaint.setStrokeWidth(0.007f * canvasWidth);
                scaleLabelPaint.setTextSize(0.05f * canvasWidth);
                scaleLabelPaint.setStrokeWidth(0.015f * canvasWidth);
            }
        }

        if (requestedTextSize > 0) {
            textPaint.setTextSize(requestedTextSize);
        } else {
            textPaint.setTextSize(w / 20f);
        }

        needleWidth = canvasWidth / 40f;
        needleLength = (canvasWidth / 2f) * 0.45f;

        needlePaint.setStrokeWidth(canvasWidth / 200f);
        needlePaintAdj.setStrokeWidth(canvasWidth / 200f);

        if (isNeedleShadow)
            needlePaint.setShadowLayer(canvasWidth / 4f, canvasWidth / 10f, canvasWidth / 10f, Color.WHITE);

        setUpNeedle();

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Log.i(TAG, "onDraw()");
        drawRim(canvas);
        drawFace(canvas);
        drawSectionArcs(canvas);
        drawScaleWithText(canvas);
        drawTexts(canvas);
        drawNeedle(canvas);
        invalidate();

    }

    private void drawNeedle(Canvas canvas) {

        if(needleNeedsToMove()) {
            if (animateNeedle) {
                changeNeedleAngleToAnimate();
            } else {
                float currentValueDiff = currentValue - startValue;
                float currentValueInDegrees = anglePerValue * currentValueDiff;
                needleAngle = startAngle + currentValueInDegrees;
            }
        }
        float totalDegreesToRotate = 90F + needleAngle;

        canvas.rotate(totalDegreesToRotate, canvasCenterX, canvasCenterY);

        canvas.drawPath(needlePath, needlePaint);
        canvas.drawPath(needlePathAdj, needlePaintAdj);

        canvas.drawCircle(canvasCenterX, canvasCenterY, needleWidth * 1.3F , needleScrewPaint);
    }

    private boolean needleNeedsToMove() {
        return Math.abs(needleAngle - currentValueAngle) > 0;
    }

    private void changeNeedleAngleToAnimate() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastMoveTime;

        if (deltaTime >= needleTimeToAnimate) {
            if (Math.abs(needleAngle - currentValueAngle) <= needleAngleToMoveAnimate) {
                needleAngle = currentValueAngle;
            } else {
                if (currentValueAngle > needleAngle) {
                    needleAngle = needleAngle + needleAngleToMoveAnimate;
                } else {
                    needleAngle = needleAngle - needleAngleToMoveAnimate;
                }
            }
            lastMoveTime = System.currentTimeMillis();
        }
    }

    private void drawScaleWithText(Canvas canvas) {
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.rotate(90F + startAngle, canvasCenterX, canvasCenterY);

        int totalClickDivisions =  (mainClicks-1) * (subclicks+1);
        int totalClicks = totalClickDivisions+1;

        float valuePerClickDivision = valueDiff / totalClickDivisions;
        float anglePerClickDivision = totalAngleForPlot / totalClickDivisions;

        float x1 = scaleRect.right - (0.010f * canvasWidth);
        float x1_main = scaleRect.right - (0.040f * canvasWidth);
        float x2 = x1 + (0.021f * canvasWidth);
        float y =  0.5f * canvasHeight;

        float valueToPlot = startValue;
        int sectionScaling = 0;
        int subClickCount = 0;
        for (int clickDrawing = 0; clickDrawing < totalClicks; clickDrawing++) {

            Paint scalePaint = scalePaints.get(sectionScaling);
            Paint scaleLabelPaint = scaleLabelPaints.get(sectionScaling);

            if(clickDrawing == 0 || subClickCount == subclicks) {
                float currentStrokeWidth = scalePaint.getStrokeWidth();
                scalePaint.setStrokeWidth(1.5f * currentStrokeWidth);
                canvas.drawLine(x1_main, y, x2, y, scalePaint);

                //------------------------------------------------------------
                drawTiltedText(canvas, x1_main, y, valueToPlot, scaleLabelPaint);
                //------------------------------------------------------------
                scalePaint.setStrokeWidth(currentStrokeWidth);
                subClickCount = 0;
            }else{
                subClickCount++;
                canvas.drawLine(x1, y, x2, y, scalePaint);
            }

            canvas.rotate(anglePerClickDivision, canvasCenterX, canvasCenterY);

            valueToPlot = valueToPlot + valuePerClickDivision;
            if(sectionScaling<(sections.size()-1)){
                if(valueToPlot>=sections.get(sectionScaling).getEndValue()){
                    sectionScaling++;
                }
            }
        }
        canvas.restore();
    }

    private void drawTiltedText(Canvas canvas, float x, float y, float labelValueToPlot, Paint scaleLabelPaint) {
        String valueToDrawAsText;
        valueToDrawAsText = getValueString(labelValueToPlot);
        float tx1= x - (0.04f * canvasWidth);
        canvas.save();
        canvas.rotate(90f, tx1, y);
        drawTextCentered(valueToDrawAsText,tx1, y , scaleLabelPaint , canvas);
        canvas.restore();
    }

    private void drawSectionArcs(Canvas canvas) {
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.rotate(90F,canvasCenterX, canvasCenterY);

        float currentAngle = startAngle;
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            float angleForSection = (section.getEndValue() -  section.getStartValue()) * anglePerValue;
            float sweepAngle = angleForSection - sectionGap;
            //To avoid section gap for last section
            if((i+1) == sections.size()){
                sweepAngle = (360F - startAngle) - currentAngle;
            }
            canvas.drawArc(zoneRect, currentAngle, sweepAngle, true, sectionPaints.get(i));
            currentAngle = currentAngle + angleForSection;

        }
        canvas.restore();

        canvas.drawOval(innerFaceRect, facePaint);
    }


    private String getValueString(float valueToPlot) {
        String valueToDrawAsText;
        if (isIntScale) {
            valueToDrawAsText = String.valueOf(Math.round(valueToPlot));
        } else {
            valueToDrawAsText = String.format("%.2f",valueToPlot);
        }
        return valueToDrawAsText;
    }

    private void initializeDrawing() {
        if(sections== null){
            ArrayList<Section> sectionsLocal = new ArrayList<>();
            float diffBy3 = (endValue - startValue)/3F;
            sectionsLocal.add(new Section(startValue, startValue + diffBy3, Color.parseColor("#FFFF00")));
            sectionsLocal.add(new Section( startValue + diffBy3,  startValue + diffBy3 +diffBy3, Color.parseColor("#76FF03")));
            sectionsLocal.add(new Section( startValue + diffBy3 +diffBy3, -endValue, Color.parseColor("#F4511E")));
            setSections(sectionsLocal);
        }

        setSaveEnabled(true);

        rimPaint = new Paint();
        rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        rimCirclePaint = new Paint();
        rimCirclePaint.setAntiAlias(true);
        rimCirclePaint.setStyle(Paint.Style.STROKE);
        rimCirclePaint.setColor(Color.parseColor("#aab8b8b8"));
        rimCirclePaint.setStrokeWidth(0.5f);

        rimShadowPaint = new Paint();
        rimShadowPaint.setStyle(Paint.Style.FILL);

        facePaint = new Paint();
        facePaint.setAntiAlias(true);
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(Color.parseColor("#344152"));

        textPaint = new Paint();
        textPaint.setColor(textColorValue);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setTextAlign(Paint.Align.CENTER);

        needlePaint = new Paint();
        needlePaint.setColor(needleColor);
        needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        needlePaint.setAntiAlias(true);

        needlePath = new Path();

        needlePaintAdj = new Paint();
        needlePaintAdj.setColor(needleColorAdj);
        needlePaintAdj.setStyle(Paint.Style.FILL_AND_STROKE);
        needlePaintAdj.setAntiAlias(true);

        needlePathAdj = new Path();

        needleScrewPaint = new Paint();
        needleScrewPaint.setColor(Color.parseColor("#AAAAAA"));
        needleScrewPaint.setAntiAlias(true);

        needleAngle = startAngle;
        currentValueAngle = startAngle;
        reCalculateCurrentValueAngle();
    }

    private void drawRim(Canvas canvas) {
        canvas.drawOval(rimRect, rimPaint);
        canvas.drawOval(rimRect, rimCirclePaint);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawOval(faceRect, facePaint);
        canvas.drawOval(faceRect, rimCirclePaint);
        canvas.drawOval(faceRect, rimShadowPaint);
    }

    private void setSections(ArrayList<Section> sections){
        this.sections = sections;
        sectionPaints = new ArrayList<>();
        scalePaints = new ArrayList<>();
        scaleLabelPaints = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Paint sectionPaint = new Paint();
            sectionPaint.setAntiAlias(true);
            sectionPaint.setColor(sections.get(i).getColor());
            sectionPaints.add(sectionPaint);

            Paint scalePaint = new Paint();
            scalePaint.setStyle(Paint.Style.STROKE);
            scalePaint.setAntiAlias(true);
            scalePaint.setColor(sections.get(i).getColor());
            scalePaints.add(scalePaint);

            Paint scaleLabelPaint = new Paint();
            scaleLabelPaint.setTypeface(Typeface.SANS_SERIF);
            scaleLabelPaint.setTextAlign(Paint.Align.CENTER);
            scaleLabelPaint.setColor(sections.get(i).getColor());
            scaleLabelPaints.add(scaleLabelPaint);
        }

        totalAngleForPlot = 360F - (2 * startAngle);
        valueDiff = endValue - startValue;
        anglePerValue = totalAngleForPlot/valueDiff;
    }

    private void reCalculateCurrentValueAngle() {
        float currentValueDiff = currentValue - startValue ;
        float currentValueInDegrees = anglePerValue * currentValueDiff;
        currentValueAngle = startAngle + currentValueInDegrees;
    }

    private void drawTextCentered(String text, float x, float y, Paint paint, Canvas canvas) {
        float yPos = (y - ((paint.descent() + paint.ascent()) / 2f));
        canvas.drawText(text, x, yPos, paint);
    }

    private void drawTexts(Canvas canvas) {
        textPaint.setColor(textColorUnit);
        drawTextCentered(upperText, canvasCenterX, canvasCenterY - (canvasHeight / 6.5f), textPaint, canvas);
        drawTextCentered(lowerText, canvasCenterX, canvasCenterY + (canvasHeight / 6.5f), textPaint, canvas);
        textPaint.setColor(textColorValue);
        drawTextCentered(getValueString(currentValue), canvasCenterX, canvasHeight - (canvasHeight * 0.15F), textPaint, canvas);
    }

    private void setUpNeedle() {

        needlePath.reset();
        needlePath.moveTo(canvasCenterX, canvasCenterY);
        needlePath.lineTo(canvasCenterX, canvasCenterY + (needleWidth));
        needlePath.lineTo(canvasCenterX + needleLength, canvasCenterY);
        needlePath.lineTo(canvasCenterX , canvasCenterY);
        needlePath.close();

        needlePathAdj.reset();
        needlePathAdj.moveTo(canvasCenterX, canvasCenterY);
        needlePathAdj.lineTo(canvasCenterX, canvasCenterY - (needleWidth));
        needlePathAdj.lineTo(canvasCenterX + needleLength, canvasCenterY);
        needlePathAdj.lineTo(canvasCenterX , canvasCenterY);
        needlePathAdj.close();

        needleScrewPaint.setShader(new RadialGradient(canvasCenterX, canvasCenterY, needleWidth * 1.1F,
                Color.parseColor("#AAAAAA"), Color.parseColor("#888888"), Shader.TileMode.CLAMP));
    }


}
