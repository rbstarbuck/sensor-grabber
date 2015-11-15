package edu.umich.dpm.sensorgrabber.sensor;

import android.hardware.SensorEvent;
import android.util.Log;

import edu.umich.dpm.sensorgrabber.stream.OutputStreamConnector;

public final class SensorBuffer {
    private static final String TAG = "SensorBuffer";

    private final SensorQueue[] mQueues;

    private int mActiveQueue = 0;
    private int mPreviousQueue = -1;


    public SensorBuffer(int sensorType, int numQueues, int queueSize)
            throws IllegalAccessException, InstantiationException {
        Log.d(TAG, "Constructor");

        mQueues = new SensorQueue[numQueues];
        for (int i = 0; i < numQueues; ++i) {
            mQueues[i] = new SensorQueue(sensorType, queueSize);
        }
    }

    public boolean recordReading(SensorEvent e, OutputStreamConnector connector) {
        Log.v(TAG, "recordReading");

        if (mActiveQueue == mPreviousQueue) {
            Log.e(TAG, "No available queues");
            return false;
        }

        SensorQueue.Status status = mQueues[mActiveQueue].recordReading(e);
        switch (status) {
            case ReadyToSend:
                Log.d(TAG, "Ready to send queue #" + String.valueOf(mActiveQueue));
                connector.sendQueue(mQueues[mActiveQueue]);
                mPreviousQueue = mActiveQueue;
                nextQueue();
                return true;
            case Locked:
                Log.d(TAG, "Locked");
                nextQueue();
                return recordReading(e, connector);
        }

        return true;
    }

    private void nextQueue() {
        if (++mActiveQueue == mQueues.length) {
            mActiveQueue = 0;
        }
    }
}
