package com.mjzuo.views.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.mjzuo.views.R;
import com.mjzuo.views.util.CommentUtils;

import java.text.DecimalFormat;

/**
 * 可拖动动环形刻度条
 *
 * @author mjzuo
 * @since 19/12/25
 */
public class SlideRingScaleView extends View {

    //圆环的宽度
    float ringWidth;
    //底部的圆弧颜色
    int ringBgCorlor;
    //滑动的圆弧颜色
    int slideRingCorlor;
    //同心圆的外圆半径
    int radius;
    //中间字的颜色
    int wordCorlor;
    //中间字的大小
    int wordSize;

    //最大进度范围
    int maxProgress;
    //最小进度范围
    int minProgress;
    //当前进度(总是将起始位置等分为100份)，通过进度的百分比求出实际显示数值
    int progress;
    //实际显示的数值
    double realShowProgress;
    //每次要增加减少的数值
    double addOrReduce = 1;
    //开始滑动的起始位置度数，顶部270 右侧0 底部90 左侧180,因为这是半圆直接写死从左侧180开始滑动
    int beginLocation;
    //当前可滑动区域的范围
    int slideAbleLocation;

    //圆环上的圆圈
    Bitmap mDragBitmap;
    //圆环的宽
    int bitmapWidth;
    //圆环的高度
    int bitmapHight;

    //外侧刻度线的数量
    int scaleLineCount;
    //外侧正常刻度线的长度
    int scaleLineLength;
    //线条的宽度
    int scaleLineWidth;
    //需要特殊处理的刻度线长度，例如正方位上的刻度或者当前刻度
    int specialScaleLineLength;
    //刻度结束的角度
    float sweepAngle = 180;//因为是半圆，这里就写死了
    //未滑动时的刻度线颜色
    int scaleLineNormalCorlor;
    //滑动后的刻度线颜色
    int specialScaleCorlor;
    //刻度线距离里面的环的距离
    int scaleToRingSpace;

    //画底部背景环的画笔
    Paint ringBgPaint;
    //画上面圆弧的画笔
    Paint slideRingPaint;
    //圆环上的小圆圈
    Paint mBitmapPaint;
    //写当前progress的画笔
    Paint wordPaint;
    //画普通背景刻度线的画笔
    Paint scalePaint;
    //这是画滑动后的刻度颜色画笔
    Paint specialScalePaint;

    //显示中间显示文字(当前为progress)所占的区域
    Rect rect;

    //这是保留小数的使用类
    DecimalFormat df;

    public SlideRingScaleView(Context context) {
        this(context, null);
    }

    public SlideRingScaleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideRingScaleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
        initPaint(context);
    }

    /**
     * 定位到指定刻度
     * @param p
     */
    public void setProgress(double p) {
        if(p <= maxProgress && p >= minProgress) {
            realShowProgress = p;
            progress = (int)((realShowProgress - minProgress) * 100.0 / (maxProgress - minProgress));
        }
        invalidate();
    }

    /**
     * 点击增加progress
     */
    public void addProgress() {
        if(realShowProgress < maxProgress){
            synchronized (SlideRingScaleView.class) {
                realShowProgress = Double.parseDouble(df.format(realShowProgress + addOrReduce));
                progress = (int)((realShowProgress - minProgress) * 100.0 / (maxProgress - minProgress));
            }
            invalidate();
        }
    }

    /**
     * 点击减少progress
     */
    public void reduceProgress() {
        if(realShowProgress > minProgress){
            synchronized (SlideRingScaleView.class) {
                realShowProgress = Double.parseDouble(df.format(realShowProgress - addOrReduce));
                progress = (int)((realShowProgress - minProgress) * 100.0 / (maxProgress - minProgress));
            }
            invalidate();
        }
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {

        /** 滑动事件范围*/
        slideAbleLocation = CommentUtils.dip2px(context, 30);

        /** 滑动按钮bitmap*/
        bitmapWidth = CommentUtils.dip2px(context, 30);
        bitmapHight = CommentUtils.dip2px(context, 30);
        mDragBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ring_dot);
        mDragBitmap = CommentUtils.conversionBitmap(mDragBitmap, bitmapWidth, bitmapHight);

        TypedArray array = context.getTheme().obtainStyledAttributes
                (attrs, R.styleable.SildeRingScaleView, defStyleAttr, 0);

        /** 圆环属性值*/
        ringWidth = array.getInt(R.styleable.SildeRingScaleView_slideRingWidth, CommentUtils.dip2px(context, 10));
        slideRingCorlor = array.getInt(R.styleable.SildeRingScaleView_slideRingCorlor2, 0xFF6cbe89);
        ringBgCorlor = array.getInt(R.styleable.SildeRingScaleView_slideRingBgCorlor, 0x806cbe89);
        radius = array.getInt(R.styleable.SildeRingScaleView_slideRadius, CommentUtils.dip2px(context, 64));
        wordCorlor = array.getInt(R.styleable.SildeRingScaleView_slideWordCorlor, 0xFF3399FF);
        wordSize = array.getInt(R.styleable.SildeRingScaleView_slideWordSize, 14);

        /** 下面是刻度线的属性*/
        scaleLineCount = array.getInt(R.styleable.SildeRingScaleView_slideScaleLineCount, 100);
        scaleLineLength = array.getInt(R.styleable.SildeRingScaleView_slideScaleLineLength, CommentUtils.dip2px(context, 8));
        specialScaleLineLength = array.getInt(R.styleable.SildeRingScaleView_slideSpecialScaleLineLength, CommentUtils.dip2px(context, 12));
        scaleToRingSpace = array.getInt(R.styleable.SildeRingScaleView_slideScaleToRingSpace, CommentUtils.dip2px(context, 6));
        scaleLineNormalCorlor = array.getInt(R.styleable.SildeRingScaleView_slideScaleLineNormalCorlor, 0x806cbe89);
        specialScaleCorlor = array.getInt(R.styleable.SildeRingScaleView_slideSpecialScaleCorlor, 0xffedc263);
        scaleLineWidth = array.getInt(R.styleable.SildeRingScaleView_slideScaleLineWidth, CommentUtils.dip2px(context, 2));

        /** 刻度值*/
        maxProgress = array.getInt(R.styleable.SildeRingScaleView_slideMaxProgress, 100);
        minProgress = array.getInt(R.styleable.SildeRingScaleView_slideMinProgress, 0);
        progress = array.getInt(R.styleable.SildeRingScaleView_slideProgress, 0);

        beginLocation = 180;//因为这是个半弧，所以我们直接写死了，从左侧开始

        array.recycle();//记得使用完销毁

        /** 保留1位小数*/
        df = new DecimalFormat("#.0");
        realShowProgress = getShowProgress(progress);
    }

    private void initPaint(Context context) {

        /** 圆圈bitmap*/
        mBitmapPaint = new Paint();
        mBitmapPaint.setDither(true);//设置防抖动
        mBitmapPaint.setFilterBitmap(true);//对Bitmap进行滤波处理
        mBitmapPaint.setAntiAlias(true);//设置抗锯齿

        /** 背景圆弧*/
        ringBgPaint = new Paint();
        ringBgPaint.setColor(ringBgCorlor);
        ringBgPaint.setAntiAlias(true);// 抗锯齿效果
        ringBgPaint.setStyle(Paint.Style.STROKE);//设置空心
        ringBgPaint.setStrokeWidth(ringWidth);//线宽度，即环宽
        ringBgPaint.setStrokeCap(Paint.Cap.ROUND);//圆形笔头

        /** 滑动圆弧*/
        slideRingPaint = new Paint();
        slideRingPaint.setAntiAlias(true);
        slideRingPaint.setStyle(Paint.Style.STROKE);
        slideRingPaint.setColor(slideRingCorlor);
        slideRingPaint.setStrokeWidth(ringWidth);
        slideRingPaint.setStrokeCap(Paint.Cap.ROUND);//圆形笔头

        /** 文字*/
        wordPaint = new Paint();
        wordPaint.setColor(wordCorlor);
        wordPaint.setTypeface(Typeface.DEFAULT_BOLD);
        wordPaint.setTextSize(CommentUtils.sp2px(context, wordSize));
        rect = new Rect();
        String str = progress + "";
        wordPaint.getTextBounds(str, 0, str.length(),rect);

        /** 刻度线*/
        scalePaint = new Paint();
        scalePaint.setColor(scaleLineNormalCorlor);
        scalePaint.setAntiAlias(true);
        scalePaint.setStyle(Paint.Style.STROKE);
        scalePaint.setStrokeWidth(scaleLineWidth);

        /** 滑动刻度线*/
        specialScalePaint = new Paint();
        specialScalePaint.setColor(specialScaleCorlor);
        specialScalePaint.setAntiAlias(true);
        specialScalePaint.setStyle(Paint.Style.STROKE);
        specialScalePaint.setStrokeWidth(scaleLineWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthModel = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightModel = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        switch (widthModel) {
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                widthSize = 2 * radius + getPaddingLeft() + getPaddingRight() +2 * (specialScaleLineLength + scaleToRingSpace);
                break;

            case MeasureSpec.EXACTLY://当宽度全屏或者固定尺寸时候
                break;
        }
        switch (heightModel) {
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                /**
                 * 当控件自适应时候，尺寸=半径+左右边距
                 */
                heightSize = radius + getPaddingTop() + getPaddingBottom()
                        + bitmapHight / 4 + specialScaleLineLength + scaleToRingSpace;
                break;

            case MeasureSpec.EXACTLY:
                break;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /** 背景圆环*/
        canvas.drawArc(new RectF(ringWidth / 2 + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                        , ringWidth / 2 + getPaddingTop() + specialScaleLineLength + scaleToRingSpace
                        , 2 * radius- ringWidth / 2 + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                        , 2 * radius - ringWidth / 2 + getPaddingTop() + specialScaleLineLength + scaleToRingSpace)
                , beginLocation, 180, false, ringBgPaint);

        /** 滑动圆环*/
        canvas.drawArc(new RectF(ringWidth / 2 + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                        , ringWidth / 2 + getPaddingTop() + specialScaleLineLength + scaleToRingSpace
                        , 2 * radius- ringWidth / 2 + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                        , 2 * radius - ringWidth / 2 + getPaddingTop() + specialScaleLineLength + scaleToRingSpace)
                , beginLocation, progress * 180 / 100, false, slideRingPaint);

        /** 滑动按钮bitmap*/
        PointF progressPoint = CommentUtils.calcArcEndPointXY(radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                , radius + getPaddingTop() + specialScaleLineLength + scaleToRingSpace
                , radius - ringWidth / 2
                , progress * 180 / 100, 180);
        int left = (int) progressPoint.x - mDragBitmap.getWidth() / 2;
        int top = (int) progressPoint.y - mDragBitmap.getHeight() / 2;
        canvas.drawBitmap(mDragBitmap, left, top, mBitmapPaint);

        /** 进度*/
        String str = (int)Math.floor(realShowProgress) + "";//整数
//        String str = realShowProgress + "";//小数
        wordPaint.getTextBounds(str, 0, str.length(),rect);
        canvas.drawText(str, radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace - rect.width() / 2
                , radius + getPaddingTop() + specialScaleLineLength + scaleToRingSpace, wordPaint);

        /** 刻度*/
        paintScale(canvas);

    }

    /**
     * 画背景刻度
     * @param canvas
     */
    private void paintScale(Canvas canvas) {
        canvas.save();

        //将坐标系移到圆中心
        canvas.translate(radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace
                ,radius + getPaddingTop() + specialScaleLineLength + scaleToRingSpace);

        //旋转坐标系
        canvas.rotate(90);

        for(int i = 0; i < scaleLineCount +1; i++) {
            //刻度线的实际展示长度
            int scaleLine = scaleLineLength;
            //正方位的线比较长
            if(i == 0 || i == scaleLineCount / 2 || i == scaleLineCount){
                scaleLine = specialScaleLineLength;
            }
            //画已经滑动过的刻度线,因为实际刻度数量都是按着100个来转换的
            if(i * (100 / scaleLineCount) <= progress){
                canvas.drawLine(0,radius + scaleToRingSpace
                        ,0
                        ,radius + scaleToRingSpace + scaleLine
                        ,specialScalePaint);
            }
            //画未滑动到的刻度线
            else{
                canvas.drawLine(0,radius + scaleToRingSpace
                        ,0
                        ,radius + scaleToRingSpace + scaleLine
                        ,scalePaint);
            }
            canvas.rotate(sweepAngle / (scaleLineCount * 1f));
        }

        //操作完成后恢复状态
        canvas.restore();
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isOnRing(x, y) && y <= radius + getPaddingTop() + specialScaleLineLength + scaleToRingSpace) {
                    updateProgress(x, y);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(y <= radius + getPaddingTop() + specialScaleLineLength + scaleToRingSpace) {
                    updateProgress(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 根据当前点的位置求角度，再转换成当前进度
     * @param eventX
     * @param eventY
     */
    private void updateProgress(int eventX, int eventY) {
        double angle = Math.atan2(eventY - (radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace)
                , eventX - (radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace)) / Math.PI;
        angle = ((2 + angle) % 2 + (-beginLocation / 180f)) % 2;
        if((int)Math.round(angle * 100) >= 0){
            progress = (int)Math.round(angle * 100);
            realShowProgress = getShowProgress(progress);
        }
        invalidate();
    }

    /**
     * 判断当前触摸屏幕的位置是否位于咱们定的可滑动区域内
     * @param eventX
     * @param eventY
     * @return
     */
    private boolean isOnRing(float eventX, float eventY) {
        boolean result = false;
        double distance = Math.sqrt(Math.pow(eventX - (radius + getPaddingLeft() + specialScaleLineLength + scaleToRingSpace), 2)
                + Math.pow(eventY - (radius+getPaddingLeft() + specialScaleLineLength + scaleToRingSpace), 2));
        if (distance < (2 * radius + getPaddingLeft() + getPaddingRight() + 2 * (specialScaleLineLength + scaleToRingSpace))
                && distance > radius - slideAbleLocation) {
            result = true;
        }
        return result;
    }

    /**
     * 根据progress，再求出如果首位不是0-100的时候的数字
     * @param progress
     * @return
     */
    private double getShowProgress(int progress) {
        return Double.parseDouble(df.format((maxProgress - minProgress) / 100.0 * progress + minProgress));
    }

}

