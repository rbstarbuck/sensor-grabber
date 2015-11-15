package edu.umich.dpm.sensorgrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import edu.umich.dpm.sensorgrabber.data.SensorDataHandler;
import edu.umich.dpm.sensorgrabber.data.SensorDataHandlerToFile;
import edu.umich.dpm.sensorgrabber.stream.InputStreamConnector;
import edu.umich.dpm.sensorgrabber.stream.MessengerService;
import edu.umich.dpm.sensorgrabber.stream.MessengerServiceListener;


public class MobileActivity extends Activity implements MessengerServiceListener {
    private static final String TAG = "MobileActivity";

    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.tv_connect);

        startService(new Intent(this, MessengerService.class));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

        MessengerService.refreshListener(this);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        MessengerService.addConnectionListener(this);

        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        MessengerService.refreshListener(this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        MessengerService.removeConnectionListener(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        MessengerService.requestDestruction();

        super.onDestroy();
    }

    @Override
    public void onConnectionStateChanged(MessengerService.ConnectionState state) {
        Log.d(TAG, "onConnectionStateChanged: " + state.toString());

        runOnUiThread(new InterpretState(state));
    }

    private void makeConnectors() {
        Log.d(TAG, "makeConnectors");

        SensorDataHandler sensorDataHandler = new SensorDataHandlerToFile();
        InputStreamConnector inputStreamConnector = new InputStreamConnector(sensorDataHandler);

        MessengerService.addStreamListener(inputStreamConnector);
        MessengerService.addStreamListener(sensorDataHandler);
    }

    private class InterpretState implements Runnable {
        private MessengerService.ConnectionState mState;

        public InterpretState(MessengerService.ConnectionState state) {
            mState = state;
        }

        @Override
        public void run() {
            switch (mState) {
                case Streaming:
                    mTextView.setText(R.string.tv_connect_streaming);
                    break;

                case GoogleApiConnectionSuccess:
                    mTextView.setText(R.string.tv_connect_nodevice);
                    makeConnectors();
                    break;

                case Connected:
                    mTextView.setText(R.string.tv_connect_connected);
                    break;

                case Disconnected:
                    mTextView.setText(R.string.tv_connect_disconnected);
                    break;

                case NoDevicesDiscovered:
                    mTextView.setText(R.string.tv_connect_nodevice);
                    break;

                case GoogleApiConnectionFailed:
                case GoogleApiConnectionSuspended:
                    mTextView.setText(R.string.tv_connect_nogoogleapi);
                    break;

                case NoMessengerService:
                    mTextView.setText(R.string.tv_connect_nomessenger);
                    break;
            }
        }
    }
}
