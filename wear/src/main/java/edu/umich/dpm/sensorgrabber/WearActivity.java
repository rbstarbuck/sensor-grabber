package edu.umich.dpm.sensorgrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.umich.dpm.sensorgrabber.sensor.SensorConnector;
import edu.umich.dpm.sensorgrabber.stream.MessengerService;
import edu.umich.dpm.sensorgrabber.stream.MessengerServiceListener;
import edu.umich.dpm.sensorgrabber.stream.OutputStreamConnector;

public final class WearActivity extends Activity implements MessengerServiceListener {
    private static final String TAG = "WearActivity";

    private Button mButtonStream;
    private TextView mTextView;


    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mButtonStream = (Button) findViewById(R.id.btn_stream);
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
        super.onStart();

        MessengerService.addConnectionListener(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        MessengerService.refreshListener(this);

        super.onResume();
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

    public void onClickButtonStream(View view) {
        Log.d(TAG, "onClickButtonStream");

        if (mButtonStream.getText().equals(getString(R.string.btn_stream_start))) {
            MessengerService.startStreaming();
        }
        else {
            MessengerService.stopStreaming();
        }
    }

    private void makeConnectors() {
        Log.d(TAG, "makeConnectors");

        OutputStreamConnector outputStreamConnector = new OutputStreamConnector(MessengerService.getGoogleApiClient());
        SensorConnector sensorConnectorAccelerometer = new SensorConnector(
                this,
                Sensor.TYPE_ACCELEROMETER,
                outputStreamConnector,
                8,
                128);
        SensorConnector sensorConnectorGyroscope = new SensorConnector(
                this,
                Sensor.TYPE_GYROSCOPE,
                outputStreamConnector,
                8,
                128);

        MessengerService.addStreamListener(outputStreamConnector);
        MessengerService.addStreamListener(sensorConnectorAccelerometer);
        MessengerService.addStreamListener(sensorConnectorGyroscope);
    }

    private final class InterpretState implements Runnable {
        private final MessengerService.ConnectionState mState;

        public InterpretState(MessengerService.ConnectionState state) {
            mState = state;
        }

        @Override
        public void run() {
            switch (mState) {
                case Streaming:
                    mTextView.setText(R.string.tv_connect_streaming);
                    mButtonStream.setText(R.string.btn_stream_stop);
                    mButtonStream.setEnabled(true);
                    break;

                case Connected:
                    mTextView.setText(R.string.tv_connect_connected);
                    mButtonStream.setText(R.string.btn_stream_start);
                    mButtonStream.setEnabled(true);
                    break;

                case Disconnected:
                    mTextView.setText(R.string.tv_connect_disconnected);
                    mButtonStream.setEnabled(false);
                    break;

                case GoogleApiConnectionSuccess:
                    mTextView.setText(R.string.tv_connect_nodevice);
                    mButtonStream.setEnabled(false);
                    makeConnectors();
                    break;

                case NoDevicesDiscovered:
                    mTextView.setText(R.string.tv_connect_nodevice);
                    mButtonStream.setEnabled(false);
                    break;

                case GoogleApiConnectionFailed:
                case GoogleApiConnectionSuspended:
                    mTextView.setText(R.string.tv_connect_nogoogleapi);
                    mButtonStream.setEnabled(false);
                    break;

                case NoMessengerService:
                    mTextView.setText(R.string.tv_connect_nomessenger);
                    mButtonStream.setEnabled(false);
                    break;
            }
        }
    }
}
