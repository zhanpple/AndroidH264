package com.zmp.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by zmp on 2019/3/5 15:59
 *
 * @author zmp
 */
public class CircleImageView extends AppCompatImageView {

        private static final String TAG = "CircleImageView";

        private float measuredWidth;

        private float measuredHeight;

        private Bitmap mBitMap;

        private int scale;

        private Paint mPaint;

        private Matrix shaderMx;


        private float shaderR;

        private float centerX;

        private float centerY;

        public CircleImageView(Context context) {
                this(context, null);
        }

        public CircleImageView(Context context, AttributeSet attrs) {
                this(context, attrs, 0);
        }

        public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
                init();
        }

        private void init() {
                mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaint.setAntiAlias(true);
                shaderMx = new Matrix();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                measuredWidth = getMeasuredWidth();
                measuredHeight = getMeasuredHeight();
                centerX = getMeasuredWidth() / 2F;
                centerY = getMeasuredHeight() / 2F;
                shaderR = Math.min(measuredWidth, measuredHeight) / 2F;
        }

        public void setBitmap(Bitmap bitMap) {
                if (measuredWidth * measuredHeight == 0) {
                        return;
                }
                if (bitMap != null) {
                        if (this.mBitMap != null) {
                                this.mBitMap.recycle();
                        }
                        this.mBitMap = bitMap;
                        setShader();
                }
        }


        public void setNv21(byte[] data, int o) {
                try {
                        byte[] bytes = ImageUtil.NV21toJPEG(data, 320, 240, o);
                        final Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        setBitmap(mBitmap);
                } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "setNv21: ", e);
                }
        }

        private void setShader() {
                shaderMx.reset();
                int width = mBitMap.getWidth();
                int height = mBitMap.getHeight();
                float scale = Math.max(measuredWidth / width, measuredHeight / height);
                if (scale < 1) {
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitMap, (int) (scale * width), (int) (scale * height), false);
                        scale = 1F;
                        //                        mBitMap.recycle();
                        mBitMap = scaledBitmap;
                }
                shaderMx.setScale(-scale, scale);
                shaderMx.postTranslate((int) centerX + mBitMap.getWidth() * scale / 2,
                        (int) centerY - mBitMap.getHeight() * scale / 2);
                BitmapShader bitmapShader = new BitmapShader(mBitMap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                bitmapShader.setLocalMatrix(shaderMx);
                mPaint.setShader(bitmapShader);
                postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                canvas.drawCircle(centerX, centerY, shaderR, mPaint);
        }

}
