package edu.umich.dpm.sensorgrabber.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import edu.umich.dpm.sensorgrabber.stream.OutputStreamConnector;
import edu.umich.dpm.sensorgrabber.stream.Streamable;

public final class SensorConnector implements SensorEventListener, Streamable {
    private static final String TAG = "SensorConnector";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private OutputStreamConnector mStreamConnector;

    private SensorBuffer mBuffer;


    public SensorConnector(Context context, int sensorType, OutputStreamConnector streamConnector,
                           int numQueues, int queueSize) {
        mStreamConnector = streamConnector;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(sensorType);

        try {
            mBuffer = new SensorBuffer(
                    mSensor.getType(),
                    numQueues,
                    queueSize
            );
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.v(TAG, "onSensorChanged");
        mBuffer.recordReading(event, mStreamConnector);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG, "onAccuracyChanged");
    }

    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "onStreamStopped");

        mSensorManager.unregisterListener(this);
    }
}
