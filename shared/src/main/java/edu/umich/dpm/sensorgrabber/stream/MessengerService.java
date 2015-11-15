package edu.umich.dpm.sensorgrabber.stream;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;

public final class MessengerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MessengerService";

    public static final String CAPABILITY_SENSOR_STREAM = "sensor_stream_capability";
    public static final String PATH_SENSOR_DATA_ROOT = "/sensor";
    public static final String PATH_SENSOR_DATA_ITEM = "data";
    public static final String PATH_MESSAGE_APP_OPENED = "/sensor_stream/app_opened";
    public static final String PATH_MESSAGE_APP_CLOSED = "/sensor_stream/app_closed";
    public static final String PATH_MESSAGE_APP_PRESENT = "/sensor_stream/app_present";
    public static final String PATH_MESSAGE_STREAM_STARTED = "/sensor_stream/started";
    public static final String PATH_MESSAGE_STREAM_STOPPED = "/sensor_stream/stopped";

    public enum ConnectionState {
        NoMessengerService,
        GoogleApiConnectionSuccess,
        GoogleApiConnectionFailed,
        GoogleApiConnectionSuspended,
        NoDevicesDiscovered,
        Disconnected,
        Connected,
        Streaming
    }

    private static MessengerService mInstance;

    private static Collection<MessengerServiceListener> mConnectionListeners = new HashSet<MessengerServiceListener>(5);
    private static Collection<Streamable> mStreamListeners = new HashSet<Streamable>(5);

    private static PowerManager.WakeLock mWakeLock;

    private static GoogleApiClient mGoogleApiClient;
    private static Node mConnectedNode;

    private static ConnectionState mCurrentState = ConnectionState.NoMessengerService;
    private static boolean mDestructionRequested = false;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        mInstance = this;
        mConnectedNode = null;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessengerWakelockTag");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        mDestructionRequested = false;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        new SendMessageTask() {
            @Override
            protected void onPostExecute(MessageApi.SendMessageResult sendMessageResult) {
                Wearable.MessageApi.removeListener(mGoogleApiClient, MessengerService.this);
                Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, MessengerService.this, CAPABILITY_SENSOR_STREAM);

                updateConnectionState(ConnectionState.NoMessengerService);
                updateStreamListenersOnStop();

                mConnectionListeners.clear();
                mStreamListeners.clear();
                mConnectedNode = null;

                mGoogleApiClient.disconnect();
                releaseWakeLock();

                MessengerService.super.onDestroy();
            }
        }.execute(PATH_MESSAGE_APP_CLOSED);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, MessengerService.this, CAPABILITY_SENSOR_STREAM);

        updateConnectionState(ConnectionState.GoogleApiConnectionSuccess);

        Wearable.CapabilityApi.getCapability(mGoogleApiClient, CAPABILITY_SENSOR_STREAM, CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult getCapabilityResult) {
                        Collection<Node> nodes = getCapabilityResult.getCapability().getNodes();
                        if (checkForConnectableNode(nodes)) {
                            new SendMessageTask().execute(PATH_MESSAGE_APP_OPENED);
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + String.valueOf(i));

        new SendMessageTask().execute(PATH_MESSAGE_APP_CLOSED);

        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this, CAPABILITY_SENSOR_STREAM);

        updateConnectionState(ConnectionState.GoogleApiConnectionSuspended);
        updateStreamListenersOnStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult.toString());

        updateConnectionState(ConnectionState.GoogleApiConnectionFailed);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent.toString());
        super.onMessageReceived(messageEvent);

        String path = messageEvent.getPath();

        if (path.equals(PATH_MESSAGE_APP_OPENED)) {
            new SendMessageTask() {
                @Override
                protected void onPostExecute(MessageApi.SendMessageResult sendMessageResult) {
                    if (sendMessageResult != null && sendMessageResult.getStatus().isSuccess()) {
                        updateConnectionState(ConnectionState.Connected);
                    }
                }
            }.execute(PATH_MESSAGE_APP_PRESENT);
        }
        else if (path.equals(PATH_MESSAGE_APP_PRESENT)) {
            updateConnectionState(ConnectionState.Connected);
        }
        else if (path.equals(PATH_MESSAGE_APP_CLOSED)) {
            terminateStream();
            updateConnectionState(ConnectionState.Disconnected);
        }
        else if (path.equals(PATH_MESSAGE_STREAM_STARTED)) {
            beginStream();
        }
        else if (path.equals(PATH_MESSAGE_STREAM_STOPPED)) {
            terminateStream();
        }
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo.toString());
        super.onCapabilityChanged(capabilityInfo);

        Collection<Node> nodes = capabilityInfo.getNodes();
        if (checkForConnectableNode(nodes)) {
            new SendMessageTask().execute(PATH_MESSAGE_APP_OPENED);
        }
    }

    public static void startStreaming() {
        new SendMessageTask() {
            @Override
            protected void onPostExecute(MessageApi.SendMessageResult sendMessageResult) {
                if (sendMessageResult != null && sendMessageResult.getStatus().isSuccess()) {
                    beginStream();
                }
            }
        }.execute(PATH_MESSAGE_STREAM_STARTED);
    }

    public static void stopStreaming() {
        new SendMessageTask() {
            @Override
            protected void onPostExecute(MessageApi.SendMessageResult sendMessageResult) {
                if (sendMessageResult != null && sendMessageResult.getStatus().isSuccess()) {
                    terminateStream();
                }
            }
        }.execute(PATH_MESSAGE_STREAM_STOPPED);
    }

    public static void requestDestruction() {
        Log.d(TAG, "requestDestruction");

        if (mCurrentState == ConnectionState.Streaming) {
            mDestructionRequested = true;
        }
        else if (mInstance != null) {
            mInstance.stopSelf();
        }
    }

    public static ConnectionState getConnectionState() {
        return mCurrentState;
    }

    public static void refreshListener(MessengerServiceListener listener) {
        Log.d(TAG, "refreshListener");

        listener.onConnectionStateChanged(mCurrentState);
    }

    @Nullable
    public static GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public static boolean isConnected() {
        return (mCurrentState == ConnectionState.Connected ||
                mCurrentState == ConnectionState.Streaming);
    }

    public static boolean addConnectionListener(MessengerServiceListener listener) {
        Log.d(TAG, "addConnectionListener: old count = " + mConnectionListeners.size());

        boolean status = mConnectionListeners.add(listener);
        Log.d(TAG, "addConnectionListener: new count = " + mConnectionListeners.size());
        return status;
    }

    public static boolean removeConnectionListener(MessengerServiceListener listener) {
        Log.d(TAG, "removeConnectionListener: old count = " + mConnectionListeners.size());

        boolean status = mConnectionListeners.remove(listener);
        Log.d(TAG, "removeConnectionListener: new count = " + mConnectionListeners.size());
        return status;
    }

    public static boolean addStreamListener(Streamable listener) {
        Log.d(TAG, "addStreamListener: old count = " + mStreamListeners.size());

        boolean status = mStreamListeners.add(listener);
        Log.d(TAG, "addStreamListener: new count = " + mStreamListeners.size());
        return status;
    }

    public static boolean removeStreamListener(Streamable listener) {
        Log.d(TAG, "removeStreamListener: old count = " + mStreamListeners.size());

        boolean status;
        if (listener != null) {
            listener.onStreamStopped();
            status = mStreamListeners.remove(listener);
        }
        else {
            status = false;
        }

        Log.d(TAG, "removeStreamListener: new count = " + mStreamListeners.size());
        return status;
    }

    @Nullable
    public static Node getConnectedNode() {
        return mConnectedNode;
    }

    private static void beginStream() {
        Log.d(TAG, "beginStream");

        updateConnectionState(ConnectionState.Streaming);
        updateStreamListenersOnStart();
        getWakeLock();
    }

    private static void terminateStream() {
        Log.d(TAG, "terminateStream");

        updateConnectionState(ConnectionState.Connected);
        updateStreamListenersOnStop();
        releaseWakeLock();
        if (mDestructionRequested && mInstance != null) {
            mInstance.stopSelf();
        }
    }

    private static void updateConnectionState(ConnectionState state) {
        Log.d(TAG, "updateConnectionState: " + state);

        if (state != mCurrentState) {
            mCurrentState = state;
            for (MessengerServiceListener listener : mConnectionListeners) {
                if (listener != null) {
                    listener.onConnectionStateChanged(state);
                }
            }
        }
    }

    private static void updateStreamListenersOnStart() {
        Log.d(TAG, "updateStreamListenersOnStart");

        for (Streamable streamable : mStreamListeners) {
            if (streamable != null) {
                streamable.onStreamStarted();
            }
        }
    }

    private static void updateStreamListenersOnStop() {
        Log.d(TAG, "updateStreamListenersOnStop");

        for (Streamable streamable : mStreamListeners) {
            if (streamable != null) {
                streamable.onStreamStopped();
            }
        }
    }

    private static boolean checkForConnectableNode(Collection<Node> nodes) {
        Log.d(TAG, "checkForConnectableNode: " + nodes.toString());

        if (!nodes.isEmpty()) {
            for (Node node : nodes) {
                if (node.isNearby()) {
                    mConnectedNode = node;
                    updateConnectionState(ConnectionState.Disconnected);
                    return true;
                }
            }
        }

        mConnectedNode = null;
        updateConnectionState(ConnectionState.NoDevicesDiscovered);
        updateStreamListenersOnStop();
        return false;
    }


    private static synchronized void getWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private static synchronized void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
