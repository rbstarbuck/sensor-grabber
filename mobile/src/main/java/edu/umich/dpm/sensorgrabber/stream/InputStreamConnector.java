package edu.umich.dpm.sensorgrabber.stream;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.umich.dpm.sensorgrabber.data.SensorDataHandler;
import edu.umich.dpm.sensorgrabber.sensor.SensorQueue;

public final class InputStreamConnector implements Runnable, Streamable, DataApi.DataListener {
    private static final String TAG = "InputStreamConnector";
    private static final long SLEEP_MS = 1000;

    private GoogleApiClient mGoogleApiClient;

    private final ConcurrentLinkedQueue<Asset> mQueue = new ConcurrentLinkedQueue<>();
    private final SensorDataHandler mSensorDataHandler;

    private Thread mThread;
    private volatile boolean mIsRunning = false;


    public InputStreamConnector(SensorDataHandler handler) {
        Log.d(TAG, "Constructor");

        mSensorDataHandler = handler;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents.toString());

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(MessengerService.PATH_SENSOR_DATA_ROOT)) {
                    mQueue.add(DataMapItem.fromDataItem(item)
                                    .getDataMap()
                                    .getAsset(MessengerService.PATH_SENSOR_DATA_ITEM));
                }
            }
        }
    }

    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        if (!mIsRunning) {
            mGoogleApiClient = MessengerService.getGoogleApiClient();
            if (mGoogleApiClient != null) {
                Wearable.DataApi.addListener(mGoogleApiClient, this);
                mThread = new Thread(this);
                mThread.start();
            }
            else {
                Log.e(TAG, "Couldn't get GoogleApiClient");
            }
        }
        else {
            Log.e(TAG, "Already running");
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

            if (mGoogleApiClient != null) {
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
            }
        }
    }

    @Override
    public void run() {
        mIsRunning = true;
        while (mIsRunning) {
            processQueue();
            try {
                Thread.sleep(SLEEP_MS);
            }
            catch (InterruptedException e) {
                Log.d(TAG, "Thread interrupted");
            }
        }
    }

    private void processQueue() {
        Log.d(TAG, "processQueue");

        while(!mQueue.isEmpty()) {
            Asset asset = mQueue.poll();
            if (asset != null) {
                Log.d(TAG, "Processing item: " + asset.toString());

                Collection<SensorQueue> collection = deserializeAsset(asset);
                if (collection != null) {
                    mSensorDataHandler.receiveData(collection);
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Collection<SensorQueue> deserializeAsset(Asset asset) {
        Log.d(TAG, "deserializeAsset");

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            Log.e(TAG, "Failed to deserialize asset: not connected to GoogleApiClient");
            return null;
        }

        final InputStream assetInputStream =
                Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset)
                        .await()
                        .getInputStream();

        if (assetInputStream == null) {
            Log.e(TAG, "Failed to deserialize asset: requested unknown asset");
            return null;
        }

        final Collection<SensorQueue> collection;
        final ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(assetInputStream));
            collection = (Collection<SensorQueue>) ois.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to deserialize asset: object stream IOException: " + e.getMessage());
            return null;
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to deserialize asset: class not found");
            return null;
        }
        finally {
            try {
                assetInputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Deserialization successful");
        return collection;
    }

    public boolean isRunning() {
        return mIsRunning;
    }
}
