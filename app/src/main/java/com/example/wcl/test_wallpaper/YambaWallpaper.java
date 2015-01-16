package com.example.wcl.test_wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;

import java.util.List;

/**
 * Created by wangchenlong on 15-1-16.
 */
public class YambaWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new YambaWallpaperEngine(
                ((YambaApplication) getApplication())
                        .getYambaClient());
    }

    // 壁纸引擎
    private class YambaWallpaperEngine extends Engine implements Runnable {
        private Handler mHandler = new Handler(); // 没有传递参数，与主线程有关
        private final ContentThread mContentThread = new ContentThread();
        private YambaClient mYambaClient;

        private Paint mPaint;

        private String[] mContents = new String[20];
        private TextPoint[] mTextPoints = new TextPoint[20];
        private int mCurrent = -1;
        private boolean mIsRunning = true;
        private float mOffset = 0;

        public YambaWallpaperEngine(YambaClient client) {
            mYambaClient = client;
            mPaint = new Paint();
            mPaint.setColor(Color.MAGENTA);
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(1);
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextSize(50);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mIsRunning = true;
            mContentThread.start();
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(this);
            mIsRunning = false;
            synchronized (mContentThread) {
                mContentThread.interrupt();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(this);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mHandler.removeCallbacks(this);
        }

        @Override
        public void onOffsetsChanged(
                float xOffset, float yOffset,
                float xOffsetStep, float yOffsetStep,
                int xPixelOffset, int yPixelOffset) {
            mOffset = xPixelOffset;
            drawFrame();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mCurrent++;
                if (mCurrent >= mTextPoints.length) {
                    mCurrent = 0;
                }

                String text = mContents[mCurrent];
                if (text != null) {
                    mTextPoints[mCurrent] = new TextPoint(text, event.getX() - mOffset, event.getY());
                }
            }
            super.onTouchEvent(event);
        }

        @Override
        public void run() {
            drawFrame();
        }

        private void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            c = holder.lockCanvas();
            if (c != null) {
                drawText(c);
                holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(this);
            if (isVisible()) {
                mHandler.postDelayed(this, 40);
            }

        }

        // 提取20条信息
        private boolean getContent() {
            List<YambaClient.Status> timeline = null;

            try {
                timeline = mYambaClient.getTimeline(20);
            } catch (YambaClientException e) {
                e.printStackTrace();
            }
            int i = -1;
            mContents = new String[20];
            if (timeline != null) {
                for (YambaClient.Status status : timeline) {
                    i++;
                    mContents[i] = status.getMessage();
                }
            }

            return timeline != null && !timeline.isEmpty();
        }

        // 把每个点画在画布上
        private void drawText(Canvas c) {
            c.drawColor(Color.YELLOW);

            for (TextPoint textPoint : mTextPoints) {
                if (textPoint != null) {
                    c.drawText(textPoint.text,
                            textPoint.x + mOffset, textPoint.y, mPaint);
                }
            }
        }

        // 字符串和位置
        private class TextPoint {
            public String text;
            public float x;
            public float y;

            public TextPoint(String t, float xp, float yp) {
                text = t;
                x = xp;
                y = yp;
            }
        }

        // 线程
        private class ContentThread extends Thread {
            public void run() {
                while (mIsRunning) {
                    boolean hascontent = getContent();

                    try {
                        if (hascontent) {
                            Thread.sleep(60000);
                        } else {
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
