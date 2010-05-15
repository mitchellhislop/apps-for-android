
package net.clc.bt;

import net.clc.bt.Connection.OnConnectionLostListener;
import net.clc.bt.Connection.OnConnectionServiceReadyListener;
import net.clc.bt.Connection.OnIncomingConnectionListener;
import net.clc.bt.Connection.OnMaxConnectionsReachedListener;
import net.clc.bt.Connection.OnMessageReceivedListener;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;

public class MultiScreenVideo extends Activity {
    private class MultiScreenVideoView extends VideoView {
        public static final int POSITION_LEFT = 0;

        public static final int POSITION_RIGHT = 1;

        private int pos;

        public MultiScreenVideoView(Context context, int position) {
            super(context);
            pos = position;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(960, MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(640, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean canSend = true;

    private class PositionSyncerThread implements Runnable {
        public void run() {
            while (mVideo != null) {
                if (canSend) {
                    canSend = false;
                    mConnection.sendMessage(connectedDevice, "SYNC:" + mVideo.getCurrentPosition());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static final int SERVER_LIST_RESULT_CODE = 42;

    private MultiScreenVideo self;

    private MultiScreenVideoView mVideo;

    private Connection mConnection;

    private String connectedDevice = "";

    private boolean isMaster = false;

    private int lastSynced = 0;

    private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
        public void OnMessageReceived(String device, String message) {
            if (message.indexOf("SYNC") == 0) {
                try {
                    String[] syncMessageSplit = message.split(":");
                    int diff = Integer.parseInt(syncMessageSplit[1]) - mVideo.getCurrentPosition();
                    Log.e("master - slave diff", Integer.parseInt(syncMessageSplit[1])
                            - mVideo.getCurrentPosition() + "");
                    if ((Math.abs(diff) > 100) && (mVideo.getCurrentPosition() - lastSynced > 1000)) {
                        mVideo.seekTo(mVideo.getCurrentPosition() + diff + 300);
                        lastSynced = mVideo.getCurrentPosition() + diff + 300;
                    }
                } catch (IllegalStateException e) {
                    // Do nothing; this can happen as you are quitting the app
                    // mid video
                }
                mConnection.sendMessage(connectedDevice, "ACK");
            } else if (message.indexOf("START") == 0) {
                Log.e("received start", "0");
                mVideo.start();
            } else if (message.indexOf("ACK") == 0) {
                canSend = true;
            }

        }
    };

    private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
        public void OnMaxConnectionsReached() {

        }
    };

    private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
        public void OnIncomingConnection(String device) {
            connectedDevice = device;
            if (isMaster) {
                Log.e("device connected", connectedDevice);
                mConnection.sendMessage(connectedDevice, "START");
                new Thread(new PositionSyncerThread()).start();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mVideo.start();
            }
        }
    };

    private OnConnectionLostListener disconnectedListener = new OnConnectionLostListener() {
        public void OnConnectionLost(String device) {
            class displayConnectionLostAlert implements Runnable {
                public void run() {
                    Builder connectionLostAlert = new Builder(self);

                    connectionLostAlert.setTitle("Connection lost");
                    connectionLostAlert
                            .setMessage("Your connection with the other Android has been lost.");

                    connectionLostAlert.setPositiveButton("Ok", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    connectionLostAlert.setCancelable(false);
                    try {
                        connectionLostAlert.show();
                    } catch (BadTokenException e) {
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
            if (isMaster) {
                mConnection.startServer(1, connectedListener, maxConnectionsListener,
                        dataReceivedListener, disconnectedListener);
                self.setTitle("MultiScreen Video: " + mConnection.getName() + "-"
                        + mConnection.getAddress());
            } else {
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
        isMaster = startingIntent.getBooleanExtra("isMaster", false);

        int pos = MultiScreenVideoView.POSITION_LEFT;
        if (!isMaster) {
            pos = MultiScreenVideoView.POSITION_RIGHT;
        }

        mVideo = new MultiScreenVideoView(this, pos);
        mVideo.setVideoPath("/sdcard/android.mp4");

        LinearLayout mLinearLayout = new LinearLayout(this);
        if (!isMaster) {
            mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            mLinearLayout.setGravity(Gravity.RIGHT);
            mLinearLayout.setPadding(0, 0, 120, 0);
        }
        mLinearLayout.addView(mVideo);

        setContentView(mLinearLayout);

        mConnection = new Connection(this, serviceReadyListener);
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
                connectedDevice = device;
            }
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection != null) {
            mConnection.shutdown();
        }
    }

}
