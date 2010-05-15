
package net.clc.bt;

import net.clc.bt.Connection.OnConnectionLostListener;
import net.clc.bt.Connection.OnConnectionServiceReadyListener;
import net.clc.bt.Connection.OnIncomingConnectionListener;
import net.clc.bt.Connection.OnMaxConnectionsReachedListener;
import net.clc.bt.Connection.OnMessageReceivedListener;
import net.clc.bt.Demo_Ball.BOUNCE_TYPE;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;

import java.util.ArrayList;

public class AirHockey extends Activity implements Callback {

    public static final String TAG = "AirHockey";

    private static final int SERVER_LIST_RESULT_CODE = 42;

    public static final int UP = 3;

    public static final int DOWN = 4;

    public static final int FLIPTOP = 5;

    private AirHockey self;

    private int mType; // 0 = server, 1 = client

    private SurfaceView mSurface;

    private SurfaceHolder mHolder;

    private Paint bgPaint;

    private Paint goalPaint;

    private Paint ballPaint;

    private Paint paddlePaint;

    private PhysicsLoop pLoop;

    private ArrayList<Point> mPaddlePoints;

    private ArrayList<Long> mPaddleTimes;

    private int mPaddlePointWindowSize = 5;

    private int mPaddleRadius = 55;

    private Bitmap mPaddleBmp;

    private Demo_Ball mBall;

    private int mBallRadius = 40;

    private Connection mConnection;

    private String rivalDevice = "";

    private SoundPool mSoundPool;

    private int tockSound = 0;

    private MediaPlayer mPlayer;

    private int hostScore = 0;

    private int clientScore = 0;

    private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
        public void OnMessageReceived(String device, String message) {
            if (message.indexOf("SCORE") == 0) {
                String[] scoreMessageSplit = message.split(":");
                hostScore = Integer.parseInt(scoreMessageSplit[1]);
                clientScore = Integer.parseInt(scoreMessageSplit[2]);
                showScore();
            } else {
                mBall.restoreState(message);
            }
        }
    };

    private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
        public void OnMaxConnectionsReached() {

        }
    };

    private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
        public void OnIncomingConnection(String device) {
            rivalDevice = device;
            WindowManager w = getWindowManager();
            Display d = w.getDefaultDisplay();
            int width = d.getWidth();
            int height = d.getHeight();
            mBall = new Demo_Ball(true, width, height - 60);
            mBall.putOnScreen(width / 2, (height / 2 + (int) (height * .05)), 0, 0, 0, 0, 0);
        }
    };

    private OnConnectionLostListener disconnectedListener = new OnConnectionLostListener() {
        public void OnConnectionLost(String device) {
            class displayConnectionLostAlert implements Runnable {
                public void run() {
                    Builder connectionLostAlert = new Builder(self);

                    connectionLostAlert.setTitle("Connection lost");
                    connectionLostAlert
                            .setMessage("Your connection with the other player has been lost.");

                    connectionLostAlert.setPositiveButton("Ok", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    connectionLostAlert.setCancelable(false);
                    try {
                    connectionLostAlert.show();
                    } catch (BadTokenException e){
                        // Something really bad happened here; 
                        // seems like the Activity itself went away before
                        // the runnable finished.
                        // Bail out gracefully here and do nothing.
                    }
                }
            }
            self.runOnUiThread(new displayConnectionLostAlert());
        }
    };

    private OnConnectionServiceReadyListener serviceReadyListener = new OnConnectionServiceReadyListener() {
        public void OnConnectionServiceReady() {
            if (mType == 0) {
                mConnection.startServer(1, connectedListener, maxConnectionsListener,
                        dataReceivedListener, disconnectedListener);
                self.setTitle("Air Hockey: " + mConnection.getName() + "-" + mConnection.getAddress());
            } else {
                WindowManager w = getWindowManager();
                Display d = w.getDefaultDisplay();
                int width = d.getWidth();
                int height = d.getHeight();
                mBall = new Demo_Ball(false, width, height - 60);
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
        mPaddleBmp = BitmapFactory.decodeResource(getResources(), R.drawable.paddlelarge);

        mPaddlePoints = new ArrayList<Point>();
        mPaddleTimes = new ArrayList<Long>();

        Intent startingIntent = getIntent();
        mType = startingIntent.getIntExtra("TYPE", 0);

        setContentView(R.layout.main);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mHolder = mSurface.getHolder();

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        goalPaint = new Paint();
        goalPaint.setColor(Color.RED);

        ballPaint = new Paint();
        ballPaint.setColor(Color.GREEN);
        ballPaint.setAntiAlias(true);

        paddlePaint = new Paint();
        paddlePaint.setColor(Color.BLUE);
        paddlePaint.setAntiAlias(true);

        mPlayer = MediaPlayer.create(this, R.raw.collision);

        mConnection = new Connection(this, serviceReadyListener);
        mHolder.addCallback(self);
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            mConnection.shutdown();
        }
        if (mPlayer != null) {
            mPlayer.release();
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
        c.drawRect(0, c.getHeight() - (int) (c.getHeight() * 0.02), c.getWidth(), c.getHeight(),
                goalPaint);

        if (mPaddleTimes.size() > 0) {
            Point p = mPaddlePoints.get(mPaddlePoints.size() - 1);

            // Debug circle
            // Point debugPaddleCircle = getPaddleCenter();
            // c.drawCircle(debugPaddleCircle.x, debugPaddleCircle.y,
            // mPaddleRadius, ballPaint);
            if (p != null) {
                c.drawBitmap(mPaddleBmp, p.x - 60, p.y - 200, new Paint());
            }
        }
        if ((mBall == null) || !mBall.isOnScreen()) {
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

            // Debug circle
            Point debugBallCircle = getBallCenter();
            // c.drawCircle(debugBallCircle.x, debugBallCircle.y, mBallRadius,
            // ballPaint);

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
                        handleCollision();
                        int position = mBall.update();
                        mBall.setAcceleration(0, 0);
                        if (position != 0) {
                            if ((position == UP) && (rivalDevice.length() > 1)) {
                                mConnection.sendMessage(rivalDevice, mBall.getState() + "|"
                                        + FLIPTOP);
                            } else if (position == DOWN) {
                                if (mType == 0) {
                                    clientScore = clientScore + 1;
                                } else {
                                    hostScore = hostScore + 1;
                                }
                                mConnection.sendMessage(rivalDevice, "SCORE:" + hostScore + ":"
                                        + clientScore);
                                showScore();
                                WindowManager w = getWindowManager();
                                Display d = w.getDefaultDisplay();
                                int width = d.getWidth();
                                int height = d.getHeight();
                                mBall.putOnScreen(width / 2, (height / 2 + (int) (height * .05)),
                                        0, 0, 0, 0, 0);
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
            } else {
                rivalDevice = device;
            }
            return;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
            if (mPaddleTimes.size() > mPaddlePointWindowSize) {
                mPaddleTimes.remove(0);
                mPaddlePoints.remove(0);
            }
        } else {
            mPaddleTimes = new ArrayList<Long>();
            mPaddlePoints = new ArrayList<Point>();
        }
        return false;
    }

    // TODO: Scale this for G1 sized screens
    public Point getBallCenter() {
        if (mBall == null) {
            return new Point(-1, -1);
        }
        int x = (int) mBall.getX();
        int y = (int) mBall.getY();
        return new Point(x + 10, y + 12);
    }

    // TODO: Scale this for G1 sized screens
    public Point getPaddleCenter() {
        if (mPaddleTimes.size() > 0) {
            Point p = mPaddlePoints.get(mPaddlePoints.size() - 1);
            int x = p.x + 10;
            int y = p.y - 130;
            return new Point(x, y);
        } else {
            return new Point(-1, -1);
        }
    }

    private void showScore() {
        class showScoreRunnable implements Runnable {
            public void run() {
                String scoreString = "";
                if (mType == 0) {
                    scoreString = hostScore + " - " + clientScore;
                } else {
                    scoreString = clientScore + " - " + hostScore;
                }
                Toast.makeText(self, scoreString, 0).show();
            }
        }
        self.runOnUiThread(new showScoreRunnable());
    }

    private void handleCollision() {
        // TODO: Handle multiball case
        if (mBall == null) {
            return;
        }
        if (mPaddleTimes.size() < 1) {
            return;
        }

        Point ballCenter = getBallCenter();
        Point paddleCenter = getPaddleCenter();

        final int dy = ballCenter.y - paddleCenter.y;
        final int dx = ballCenter.x - paddleCenter.x;

        final float distance = dy * dy + dx * dx;

        if (distance < ((2 * mBallRadius) * (2 * mPaddleRadius))) {
            // Get paddle velocity
            float vX = 0;
            float vY = 0;
            Point endPoint = new Point(-1, -1);
            Point startPoint = new Point(-1, -1);
            long timeDiff = 0;
            try {
                endPoint = mPaddlePoints.get(mPaddlePoints.size() - 1);
                startPoint = mPaddlePoints.get(0);
                timeDiff = mPaddleTimes.get(mPaddleTimes.size() - 1) - mPaddleTimes.get(0);
            } catch (IndexOutOfBoundsException e) {
                // Paddle points were removed at the last moment
                return;
            }
            if (timeDiff > 0) {
                vX = ((float) (endPoint.x - startPoint.x)) / timeDiff;
                vY = ((float) (endPoint.y - startPoint.y)) / timeDiff;
            }
            // Determine the bounce type
            BOUNCE_TYPE bounceType = BOUNCE_TYPE.TOP;
            if ((ballCenter.x < (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y - mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.TOPLEFT;
            } else if ((ballCenter.x > (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y - mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.TOPRIGHT;
            } else if ((ballCenter.x < (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.y > (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.BOTTOMLEFT;
            } else if ((ballCenter.x > (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y > (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.BOTTOMRIGHT;
            } else if ((ballCenter.x < paddleCenter.x)
                    && (ballCenter.y > (paddleCenter.y - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.LEFT;
            } else if ((ballCenter.x > paddleCenter.x)
                    && (ballCenter.y > (paddleCenter.y - mPaddleRadius / 2))
                    && (ballCenter.y < (paddleCenter.y + mPaddleRadius / 2))) {
                bounceType = BOUNCE_TYPE.RIGHT;
            } else if ((ballCenter.x > (paddleCenter.x - mPaddleRadius / 2))
                    && (ballCenter.x < (paddleCenter.x + mPaddleRadius / 2))
                    && (ballCenter.y > paddleCenter.y)) {
                bounceType = BOUNCE_TYPE.RIGHT;
            }

            mBall.doBounce(bounceType, vX, vY);
            if (!mPlayer.isPlaying()) {
                mPlayer.release();
                mPlayer = MediaPlayer.create(this, R.raw.collision);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.start();
            }
        }
    }

}
