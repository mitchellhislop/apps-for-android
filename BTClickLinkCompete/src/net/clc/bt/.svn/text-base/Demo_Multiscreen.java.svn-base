/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.clc.bt;

import net.clc.bt.Connection.OnConnectionLostListener;
import net.clc.bt.Connection.OnConnectionServiceReadyListener;
import net.clc.bt.Connection.OnIncomingConnectionListener;
import net.clc.bt.Connection.OnMaxConnectionsReachedListener;
import net.clc.bt.Connection.OnMessageReceivedListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.widget.Toast;

/**
 * Demo application that allows multiple Androids to be linked together as if
 * they were one large screen. The center screen is the server, and it can link
 * to 4 other devices: right, left, up, and down.
 */

public class Demo_Multiscreen extends Activity implements Callback {
    public static final String TAG = "Demo_Multiscreen";

    public static final int CENTER = 0;

    public static final int RIGHT = 1;

    public static final int LEFT = 2;

    public static final int UP = 3;

    public static final int DOWN = 4;

    private static final int SERVER_LIST_RESULT_CODE = 42;

    private Demo_Multiscreen self;

    private long lastTouchedTime = 0;

    private int mType; // 0 = server, 1 = client

    private int mPosition; // The device that has the ball

    private SurfaceView mSurface;

    private SurfaceHolder mHolder;

    private Demo_Ball mBall;

    private Paint bgPaint;

    private Paint ballPaint;

    private Connection mConnection;

    private String rightDevice = "";

    private String leftDevice = "";

    private String upDevice = "";

    private String downDevice = "";

    private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
        public void OnMessageReceived(String device, String message) {
            if (message.startsWith("ASSIGNMENT:")) {
                if (message.equals("ASSIGNMENT:RIGHT")) {
                    leftDevice = device;
                } else if (message.equals("ASSIGNMENT:LEFT")) {
                    rightDevice = device;
                } else if (message.equals("ASSIGNMENT:UP")) {
                    downDevice = device;
                } else if (message.equals("ASSIGNMENT:DOWN")) {
                    upDevice = device;
                }
            } else {
                mPosition = CENTER;
                mBall.restoreState(message);
            }
        }
    };

    private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
        public void OnMaxConnectionsReached() {
            Log.e(TAG, "Max connections reached!");
        }
    };

    private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
        public void OnIncomingConnection(String device) {
            if (rightDevice.length() < 1) {
                mConnection.sendMessage(device, "ASSIGNMENT:RIGHT");
                rightDevice = device;
            } else if (leftDevice.length() < 1) {
                mConnection.sendMessage(device, "ASSIGNMENT:LEFT");
                leftDevice = device;
            } else if (upDevice.length() < 1) {
                mConnection.sendMessage(device, "ASSIGNMENT:UP");
                upDevice = device;
            } else if (downDevice.length() < 1) {
                mConnection.sendMessage(device, "ASSIGNMENT:DOWN");
                downDevice = device;
            }
        }
    };

    private OnConnectionLostListener disconnectedListener = new OnConnectionLostListener() {
        public void OnConnectionLost(String device) {
            if (rightDevice.equals(device)) {
                rightDevice = "";
                if (mPosition == RIGHT) {
                    mBall = new Demo_Ball(true);
                }
            } else if (leftDevice.equals(device)) {
                leftDevice = "";
                if (mPosition == LEFT) {
                    mBall = new Demo_Ball(true);
                }
            } else if (upDevice.equals(device)) {
                upDevice = "";
                if (mPosition == UP) {
                    mBall = new Demo_Ball(true);
                }
            } else if (downDevice.equals(device)) {
                downDevice = "";
                if (mPosition == DOWN) {
                    mBall = new Demo_Ball(true);
                }
            }
        }
    };

    private OnConnectionServiceReadyListener serviceReadyListener = new OnConnectionServiceReadyListener() {
        public void OnConnectionServiceReady() {
            if (mType == 0) {
                mBall = new Demo_Ball(true);
                mConnection.startServer(4, connectedListener, maxConnectionsListener,
                        dataReceivedListener, disconnectedListener);
                self.setTitle("MultiScreen: " + mConnection.getName() + "-" + mConnection.getAddress());
            } else {
                mBall = new Demo_Ball(false);
                Intent serverListIntent = new Intent(self, ServerListActivity.class);
                startActivityForResult(serverListIntent, SERVER_LIST_RESULT_CODE);
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        self = this;

        Intent startingIntent = getIntent();
        mType = startingIntent.getIntExtra("TYPE", 0);

        setContentView(R.layout.main);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mHolder = mSurface.getHolder();

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);

        ballPaint = new Paint();
        ballPaint.setColor(Color.GREEN);
        ballPaint.setAntiAlias(true);

        mConnection = new Connection(this, serviceReadyListener);
        mHolder.addCallback(self);
    }

    private PhysicsLoop pLoop;

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            mConnection.shutdown();
        }
        super.onDestroy();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        pLoop = new PhysicsLoop();
        pLoop.start();
    }

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = mHolder.lockCanvas();
            if (canvas != null) {
                doDraw(canvas);
            }
        } finally {
            if (canvas != null) {
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void doDraw(Canvas c) {
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), bgPaint);
        if (mBall == null) {
            return;
        }
        float x = mBall.getX();
        float y = mBall.getY();
        if ((x != -1) && (y != -1)) {
            float xv = mBall.getXVelocity();
            Bitmap bmp = BitmapFactory
                    .decodeResource(this.getResources(), R.drawable.android_right);
            if (xv < 0) {
                bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.android_left);
            }
            c.drawBitmap(bmp, x - 17, y - 23, new Paint());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            pLoop.safeStop();
        } finally {
            pLoop = null;
        }
    }

    private class PhysicsLoop extends Thread {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(5);
                    draw();
                    if (mBall != null) {
                        int position = mBall.update();
                        mBall.setAcceleration(0, 0);
                        if (position == RIGHT) {
                            if ((rightDevice.length() > 1)
                                    && (mConnection.sendMessage(rightDevice, mBall.getState() + "|"
                                            + LEFT) == Connection.SUCCESS)) {
                                mPosition = RIGHT;
                            } else {
                                mBall.doRebound();
                            }
                        } else if (position == LEFT) {
                            if ((leftDevice.length() > 1)
                                    && (mConnection.sendMessage(leftDevice, mBall.getState() + "|"
                                            + RIGHT) == Connection.SUCCESS)) {
                                mPosition = LEFT;
                            } else {
                                mBall.doRebound();
                            }
                        } else if (position == UP) {
                            if ((upDevice.length() > 1)
                                    && (mConnection.sendMessage(upDevice, mBall.getState() + "|"
                                            + DOWN) == Connection.SUCCESS)) {
                                mPosition = UP;
                            } else {
                                mBall.doRebound();
                            }
                        } else if (position == DOWN) {
                            if ((downDevice.length() > 1)
                                    && (mConnection.sendMessage(downDevice, mBall.getState() + "|"
                                            + UP) == Connection.SUCCESS)) {
                                mPosition = DOWN;
                            } else {
                                mBall.doRebound();
                            }
                        }
                    }
                } catch (InterruptedException ie) {
                    running = false;
                }
            }
        }

        public void safeStop() {
            running = false;
            interrupt();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == Activity.RESULT_OK) && (requestCode == SERVER_LIST_RESULT_CODE)) {
            String device = data.getStringExtra(ServerListActivity.EXTRA_SELECTED_ADDRESS);
            int connectionStatus = mConnection.connect(device, dataReceivedListener,
                    disconnectedListener);
            if (connectionStatus != Connection.SUCCESS) {
                Toast.makeText(self, "Unable to connect; please try again.", 1).show();
            }
            return;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastTouchedTime = System.currentTimeMillis();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float startX = event.getHistoricalX(0);
            float startY = event.getHistoricalY(0);
            float endX = event.getX();
            float endY = event.getY();
            long timeMs = (System.currentTimeMillis() - lastTouchedTime);
            float time = timeMs / 1000.0f;
            float aX = 2 * (endX - startX) / (time * time * 5);
            float aY = 2 * (endY - startY) / (time * time * 5);
            // Log.e("Physics debug", startX + " : " + startY + " : " + endX +
            // " : " + endY + " : " + time + " : " + aX + " : " + aY);
            float dampeningFudgeFactor = (float) 0.3;
            if (mBall != null) {
                mBall.setAcceleration(aX * dampeningFudgeFactor, aY * dampeningFudgeFactor);
            }
            return true;
        }
        return false;
    }

}
