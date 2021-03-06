package com.example.santa.picture;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.LoginFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Scroller;

/**
 * Created by santa on 16/7/25.
 */
public class SantaImageView extends ImageView {
    private final static String DEBUG = "SantaImageView_debug";
    //touch
    private PointF mLastPoint = new PointF();
    private Matrix mLastMatrix = new Matrix();
    private Matrix mDefaultMatrix = new Matrix();
    private PointF mMidPoint = new PointF();
    private double mLastDis = -1;
    private float mStartScale = 1;
    private final static int STATE_IDEL = 0;
    private final static int STATE_MOVE = 1;
    private final static int STATE_SCALE = 2;
    private float mLastRadian;
    private int mState = STATE_IDEL;
    private GestureDetector mGestureDetector;
    private final static float MAX_SCALE = 1.5f;
    private float mDensity;
    private Scroller mScoller;


    public SantaImageView(Context context) {
        this(context, null);
    }

    public SantaImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SantaImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SantaImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.FIT_CENTER);
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mDensity = metrics.density;

        setBackgroundColor(Color.BLACK);

        mScoller = new Scroller(context);
//        setScaleType(ScaleType.MATRIX);
        mGestureDetector = new GestureDetector(context, new ImageGestureListener());
//        getPictureHeight(context.getResources(), );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    private static int getPictureHeight(Resources resources, int resId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        return options.outHeight;
    }


    //缩放
    private Bitmap getFitBitmap(int newWidth, int newHeight) {
        Bitmap oldBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        int oldWidth = oldBitmap.getWidth();
        int oldHeight = oldBitmap.getHeight();

        float scale = Math.min(newWidth*1.0f/oldWidth, newHeight*1.0f/oldHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);

        return Bitmap.createBitmap(oldBitmap, 0, 0, oldWidth, oldHeight, matrix, true);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mDefaultMatrix.set(getImageMatrix());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
//                mScoller.abortAnimation();
                mScoller.forceFinished(true);

                mState = STATE_MOVE;
                setScaleType(ScaleType.MATRIX);
                mLastPoint.set(event.getX(), event.getY());
                mLastMatrix.set(getImageMatrix());
//                mStartScale = getStartScale(getImageMatrix());
                break;
            case MotionEvent.ACTION_MOVE:
                if (STATE_MOVE == mState) {
                    float transX = event.getX() - mLastPoint.x;
                    float transY = event.getY() - mLastPoint.y;

                    float translastionX = getMatrixTranslationX(mLastMatrix);
                    float translastionY = getMatrixTranslationY(mLastMatrix);
                    float scaleX = getScaledWidth(mLastMatrix);
                    float scaleY = getScaledHeight(mLastMatrix);


                    if (translastionX > 0 || translastionX < getWidth() - scaleX) {
                        if (translastionX > 0 && transX > 0 ) {
                            Log.d(DEBUG, "pager to left ");
                        } else if (translastionX < getWidth() - scaleX && transX < 0) {
                            Log.d(DEBUG, "pager to right ");
                        }
                        transX = 0;
                    }
                    if (scaleX == getWidth()) {

                    }


                    if (translastionY > 0 || translastionY < getHeight() - scaleY) {
                        transY = 0;
                    }

                    if (!isBeyondWidth()) {
                        transX = 0;
                    }
                    if (!isBeyondHeight()) {
                        transY = 0;
                    }
                    mLastMatrix.postTranslate(transX, transY);
                    mLastPoint.set(event.getX(), event.getY());
                } else if(STATE_SCALE == mState) {
                    double curDis = calculateDistance(event);
                    float scale = (float) (curDis/mLastDis);
                    float curRadian = calculateAngle(event);
//                    Log.d(DEBUG, "scale = "+scale);
//                    float angleDiff = transformToAngle(curRadian - mLastRadian);
                    float angleDiff = getAngle( mLastRadian, curRadian);
//                    Log.d(DEBUG, "scale total = "+getMatrixScale(mLastMatrix));


                    mLastDis = curDis;
                    mLastRadian = curRadian;
//                    mLastMatrix.postRotate(angleDiff, mMidPoint.x, mMidPoint.y);
                    mLastMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d(DEBUG, "release");

                release();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() > 2) {
                    Log.d(DEBUG, "more than 2 pointer can not be supported");
                    break;
                }
                mState = STATE_SCALE;
                mLastDis  = calculateDistance(event);
                mMidPoint = calculateMidPoint(event);
                mLastRadian = calculateAngle(event);
//                mLastMatrix.set(getImageMatrix());
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mState = STATE_IDEL;
                break;
        }
        setImageMatrix(mLastMatrix);
        return true;
    }


    private boolean isBeyondHeight() {
        return getScaledHeight(mLastMatrix) > getHeight();
    }

    private boolean isBeyondWidth() {
        return getScaledWidth(mLastMatrix) > getWidth();
    }

    private float getMaxScale() {
        Bitmap srcBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        return MAX_SCALE*srcBitmap.getHeight()*mDensity/getScaledHeight(mDefaultMatrix);
    }

    private void release() {

////        Bitmap srcBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
//        Log.d(DEBUG, "srcBitmap height = "+((BitmapDrawable) getDrawable()).getBitmap().getByteCount());
//        Log.d(DEBUG, "srcBitmap height = "+((BitmapDrawable) getDrawable()).getBitmap().getByteCount());
////        Log.d(DEBUG, "srcBitmap height = "+srcBitmap.getHeight());
//        Log.d(DEBUG, "start height"+getScaledHeight(mDefaultMatrix));
//        Log.d(DEBUG, "cur height"+getScaledHeight(mLastMatrix));


        final int height = getHeight();
        final int width = getWidth();
//        如果倍数小于原始倍数，恢复
        if (Math.abs(getMatrixScale(mLastMatrix)) < Math.abs(getMatrixScale(mDefaultMatrix))) {
            mLastMatrix.set(mDefaultMatrix);
        }

        //如果倍数大于最大倍数，恢复
        if (Math.abs(getMatrixScale(mLastMatrix)) > getMaxScale()) {
            mLastMatrix.postScale(mDensity/Math.abs(getMatrixScale(mLastMatrix)), mDensity/Math.abs(getMatrixScale(mLastMatrix)), mMidPoint.x, mMidPoint.y);
        }

        //使图片缩放后依旧居中
        int scaledHeight = getScaledHeight(mLastMatrix);
        float transLationY = getMatrixTranslationY(mLastMatrix);
        if (scaledHeight <= height) {
            mLastMatrix.postTranslate(0, height/2 - scaledHeight/2 - transLationY);
        } else {
            if(transLationY > 0 ) {
                mLastMatrix.postTranslate(0, 0 - transLationY);
            } else if(transLationY < 0 && transLationY + scaledHeight < height) {
                mLastMatrix.postTranslate(0, height - transLationY - scaledHeight);
            }
        }

        int scaledWidth = getScaledWidth(mLastMatrix);
        float transLationX = getMatrixTranslationX(mLastMatrix);
        if (scaledWidth <= width) {
            mLastMatrix.postTranslate(width/2 - scaledWidth/2 - transLationX, 0);
        } else {
            if(transLationX > 0 ) {
                mLastMatrix.postTranslate(0 - transLationX, 0);
            } else if(transLationX < 0 && transLationX + scaledWidth < width) {
                mLastMatrix.postTranslate(width - transLationX - scaledWidth, 0);
            }
        }
        setImageMatrix(mLastMatrix);

    }
    //缩放后的高度
    private int getScaledHeight(Matrix matrix) {
        Rect rect = getDrawable().getBounds();
        return (int)(rect.height() * getMatrixScale(matrix));
    }
    private int getScaledWidth(Matrix matrix) {
        Rect rect = getDrawable().getBounds();
        return (int)(rect.width() * getMatrixScale(matrix));
    }

    private float getMatrixScale(Matrix matrix) {
        return getMatrixInfo(matrix, Matrix.MSCALE_X);
    }

    private float getMatrixTranslationX(Matrix matrix) {
        return getMatrixInfo(matrix, Matrix.MTRANS_X);
    }

    private float getMatrixTranslationY(Matrix matrix) {
        return getMatrixInfo(matrix, Matrix.MTRANS_Y);
    }

    private float getMatrixInfo(Matrix matrix, int type) {
        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        return matrixValues[type];
    }

    private double calculateDistance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    private PointF calculateMidPoint(MotionEvent event) {
        float midX = (event.getX(1) + event.getX(0)) / 2;
        float midY = (event.getY(1) + event.getY(0)) / 2;
        return new PointF(midX, midY);
    }

    private float calculateAngle(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        if (dx == 0) {
            Log.d(DEBUG, "dy dy = "+dy);
            return (dy>0)? 90 : -90;
        }
        return (float) (Math.atan(dy/dx)*180/Math.PI);
    }

    private float getAngle(float lastAngle, float curAngle) {
        if (Math.abs(lastAngle)/lastAngle != Math.abs(curAngle)/curAngle) {
            lastAngle = -lastAngle;
        }
        return curAngle - lastAngle;
    }

    private float transformToAngle(double radianDiff) {
        return (float) (radianDiff*180/Math.PI);
    }


    private class ImageGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
//            Log.d(DEBUG, "onDown");
            mState = STATE_MOVE;
            mScoller.forceFinished(true);
            mMidPoint = new PointF(e.getX(), e.getY());
            mLastMatrix.set(getImageMatrix());
            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
//            Log.d(DEBUG, "onDoubleTap");
//            Log.d(DEBUG, "getMatrixScale(mLastMatrix) = "+getMatrixScale(mLastMatrix));
//            Log.d(DEBUG, "getMaxScale() = "+getMaxScale());

            if(getMatrixScale(mLastMatrix)< getMaxScale()) {
                float scale = getMaxScale() / getMatrixScale(mLastMatrix);
                Log.d(DEBUG, "scale = "+scale);
                mLastMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
            } else {
                mLastMatrix.set(mDefaultMatrix);
            }
            setImageMatrix(mLastMatrix);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(DEBUG, "onFling");
//            Log.d(DEBUG, "e1 = "+e1.getX());
//            Log.d(DEBUG, "e2 = "+e2.getX());
            if(!mScoller.isFinished()) {
                mScoller.forceFinished(true);
            }
            fling(velocityX, velocityY);
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }


    @Override
    public void computeScroll() {
        if (mScoller.computeScrollOffset()) {
//            Log.d(DEBUG, "scroll continue");
//            Log.d(DEBUG, "scroll getcurX = "+mScoller.getCurrX());
//            scrollTo(-mScoller.getCurrX(), -mScoller.getCurrY());
            mLastMatrix.postTranslate(mScoller.getCurrX() - getMatrixTranslationX(mLastMatrix),
                    mScoller.getCurrY() - getMatrixTranslationY(mLastMatrix));
            setImageMatrix(mLastMatrix);

        }
        super.computeScroll();
    }


    private void fling(float velocityX, float velocityY) {
        if(mState == STATE_IDEL)
            return;

        boolean isNeedFling = isBeyondWidth();
        int startX = (int) getMatrixTranslationX(mLastMatrix);
        int maxX = isNeedFling ? 0: startX;
        int minX = isNeedFling ? -(getScaledWidth(mLastMatrix) - getWidth()) : startX;


        isNeedFling = isBeyondHeight();
        int startY = (int) getMatrixTranslationY(mLastMatrix);
        int maxY = isNeedFling ?  0 : startY;
        int minY = isNeedFling ? - (getScaledHeight(mLastMatrix) - getHeight()): startY;
        mScoller.fling(startX, startY, (int)velocityX,  (int)velocityY, minX, maxX, minY, maxY);
        invalidate();
    }


}
