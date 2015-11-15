package edu.umich.dpm.sensorgrabber.stream;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.umich.dpm.sensorgrabber.sensor.SensorQueue;

public final class OutputStreamConnector implements Runnable, Streamable, Serializable {
    private static final String TAG = "OutputStreamConnector";
    private static final long SLEEP_MS = 1000;

    private Thread mThread;

    private final GoogleApiClient mGoogleApiClient;

    private final ConcurrentLinkedQueue<SensorQueue> mQueues = new ConcurrentLinkedQueue<SensorQueue>();

    private volatile boolean mIsRunning = false;


    public OutputStreamConnector(GoogleApiClient client) {
        Log.d(TAG, "Constructor");

        mGoogleApiClient = client;
    }

    public void sendQueue(SensorQueue q) {
        Log.d(TAG, "sendQueue");

        mQueues.add(q);
    }

    @Override
    public void run() {
        Log.d(TAG, "Beginning run procedure");

        mIsRunning = true;
        while (mIsRunning) {
            if (mQueues.size() > 0) {
                transmitQueuedData();
            }
            try {
                Thread.sleep(SLEEP_MS);
            }
            catch (InterruptedException e) {
                Log.d(TAG, "Thread interrupted");
            }
        }

        Log.d(TAG, "Finishing run procedure");
    }

    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        if (!mIsRunning) {
            mThread = new Thread(this);
            mThread.start();
        }
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "onStreamStopped");

        if (mIsRunning) {
            mIsRunning = false;
            mThread.interrupt();
            try {
                mThread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }
//        closeStreams();
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    @Nullable
    private Asset makeAsset() {
        Collection<SensorQueue> queues = new ArrayList<SensorQueue>(mQueues);
        Log.d(TAG, "Making asset from " + queues.size() + " queues");

        Asset asset = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(queues);

            asset = Asset.createFromBytes(byteArrayOutputStream.toByteArray());

            for (SensorQueue q : queues) {
                q.unlock();
            }
            mQueues.removeAll(queues);

        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to make asset: " + e.getMessage());
        }
        finally {
            try {
                if (objectOutputStream != null){
                    objectOutputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return asset;
    }

    private boolean transmitQueuedData() {
        Log.d(TAG, "transmitQueuedData");

        final Asset asset = makeAsset();
        if (asset == null) {
            return false;
        }

        final PutDataMapRequest dataMap = PutDataMapRequest.create(MessengerService.PATH_SENSOR_DATA_ROOT);
        dataMap.getDataMap().putAsset(MessengerService.PATH_SENSOR_DATA_ITEM, asset);
        final PutDataRequest request = dataMap.asPutDataRequest();

        Log.d(TAG, "Sending asset");
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
        Log.d(TAG, "Asset sent");

        return true;
    }
}
