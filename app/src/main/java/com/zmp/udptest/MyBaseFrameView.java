package com.zmp.udptest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zmp.udptest.camera2.ImageUtil;

import java.io.File;
import java.util.Arrays;

/**
 * @author zmp
 * 帧动画
 */
public class MyBaseFrameView extends SurfaceView {

        private static final String TAG = "MyBaseFrameView";


        private int frameTime = 60;

        private String mFileDir;

        private String[] mFileNames;

        private int[] mRes;

        private boolean isRes;

        private final SurfaceHolder surfaceHolder = getHolder();

        private int measuredWidth;

        private int measuredHeight;

        private Handler handler;

        private HandlerThread handlerThread;

        private volatile boolean isCreated = false;

        public boolean isReverse() {
                return isReverse;
        }

        public void setReverse(boolean reverse) {
                isReverse = reverse;
        }

        private boolean isReverse = false;

        public MyBaseFrameView(Context context) {
                this(context, null);

        }

        public MyBaseFrameView(Context context, @Nullable AttributeSet attrs) {
                this(context, attrs, 0);
        }

        public MyBaseFrameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
                init();

        }

        private void init() {
                setZOrderOnTop(true);
                surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
                surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder holder) {
                                synchronized (MyBaseFrameView.this) {
                                        handlerThread = new HandlerThread(TAG);
                                        handlerThread.start();
                                        handler = new Handler(handlerThread.getLooper());
                                        isCreated = true;
                                }
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                resumeAnimation();
                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder holder) {
                                synchronized (MyBaseFrameView.this) {
                                        destroyAnimation();
                                        isCreated = false;
                                }
                        }
                });
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                measuredWidth = getMeasuredWidth();
                measuredHeight = getMeasuredHeight();
        }


        public synchronized void drawBitMap(Bitmap bitmap) {
                if (!isCreated) {
                        return;
                }
                Canvas canvas = null;
                try {
                        if (surfaceHolder.getSurface().isValid()) {
                                canvas = surfaceHolder.lockCanvas();
                                if (canvas == null) {
                                        return;
                                }
                                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                if (bitmap != null && !bitmap.isRecycled()) {
                                        canvas.drawBitmap(bitmap, (measuredWidth - bitmap.getWidth()) / 2F, (measuredHeight - bitmap.getHeight()) / 2F, null);
                                        bitmap.recycle();
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if (canvas != null) {
                                surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                }
        }



        public void setBitmapDir(String dir, String[] fileNames) {
                setBitmapDir(dir, fileNames, null);
        }

        public void setBitmapDir(String dir, String[] fileNames, IPlayEndListener iPlayEndListener) {
                this.iPlayEndListener = iPlayEndListener;
                this.mFileDir = dir;
                this.mFileNames = fileNames;
                if (mFileNames == null || mFileNames.length == 0) {
                        return;
                }
                isRes = false;
                resumeAnimation();
        }

        public void setBitmapRes(int[] res) {
                this.mRes = res;
                if (res == null || res.length == 0) {
                        return;
                }
                isRes = true;
                resumeAnimation();
        }

        public synchronized void resumeAnimation() {
                if (handler != null) {
                        handler.removeCallbacks(runnable);
                        handler.post(runnable);
                }
        }

        public synchronized void pauseAnimation() {
                if (handler != null) {
                        handler.removeCallbacks(runnable);
                }
        }

        private synchronized void destroyAnimation() {
                if (handler != null) {
                        handler.removeCallbacks(runnable);
                }
                if (handlerThread != null) {
                        handlerThread.quitSafely();
                }
        }

        private int index;

        Runnable runnable = new Runnable() {

                @Override
                public void run() {
                        try {
                                if (isRes) {
                                        loadRes();
                                } else {
                                        loadFile();
                                }
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        handler.removeCallbacks(this);
                        handler.postDelayed(this, frameTime);
                }
        };

        boolean isDel = false;

        private void loadRes() {
                if (mRes == null || mRes.length == 0) {
                        return;
                }
                if (index >= mRes.length - 1) {
                        isDel = true;
                        if (!isReverse) {
                                index = 0;
                        } else {
                                index = mRes.length - 1;
                        }
                        if (iPlayEndListener != null) {
                                iPlayEndListener.onEnd();
                                if (iPlayEndListener.isPause()) {
                                        return;
                                }
                        }
                }
                if (index <= 0) {
                        isDel = false;
                        index = 0;
                }
                Log.d(TAG, "run: " + index);
                Context context = getContext();
                if (context == null) {
                        return;
                }
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mRes[index]);
                drawBitMap(bitmap);
                if (!isReverse) {
                        index++;
                        return;
                }
                if (isDel) {
                        index--;
                } else {
                        index++;
                }
        }

        private void loadFile() {
                if (mFileNames == null || mFileNames.length == 0) {
                        return;
                }
                if (index >= mFileNames.length - 1) {
                        isDel = true;
                        if (!isReverse) {
                                index = 0;
                        } else {
                                index = mFileNames.length - 1;
                        }

                        if (iPlayEndListener != null) {
                                iPlayEndListener.onEnd();
                                if (iPlayEndListener.isPause()) {
                                        return;
                                }
                        }
                }
                if (index <= 0) {
                        isDel = false;
                        index = 0;
                }
                Log.d(TAG, "run: " + index);
                Log.d(TAG, "run: " + mFileDir + mFileNames[index]);
                Context context = getContext();
                if (context == null) {
                        return;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(mFileDir + mFileNames[index]);
                drawBitMap(bitmap);
                if (isDel) {
                        index--;
                } else {
                        index++;
                }
        }


        @Override
        protected void onDraw(Canvas canvas) {
                try {
                        super.onDraw(canvas);
                } catch (Exception e) {
                        //Glide 异常
                        e.printStackTrace();
                        Log.e(TAG, "onDraw: ", e);
                }
        }


        public void setPlayEndListener(IPlayEndListener iPlayEndListener) {
                this.iPlayEndListener = iPlayEndListener;
        }

        private IPlayEndListener iPlayEndListener;



        public void setNv21(byte[] dataArr, int or0) {
                try {
                        byte[] bytes = ImageUtil.NV21toJPEG(dataArr, CameraConfig.WIDTH, CameraConfig.HEIGHT, or0);
                        final Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        drawBitMap(mBitmap);
                } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "setNv21: ", e);
                }
        }

        public interface IPlayEndListener {

                /**
                 * 播放结束
                 */
                void onEnd();

                /**
                 * 是否继续
                 *
                 * @return isPause
                 */
                boolean isPause();

        }

}
