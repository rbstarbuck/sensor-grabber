package edu.umich.dpm.sensorgrabber.stream;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

public class SendMessageTask extends AsyncTask<String, Void, MessageApi.SendMessageResult> {
    private static final String TAG = "SendMessageTask";

    private byte[] mData;

    public SendMessageTask() {
        mData = null;
    }

    public SendMessageTask(byte[] data) {
        mData = data;
    }

    @Override
    protected MessageApi.SendMessageResult doInBackground(String... path) {
        Log.d(TAG, "SendMessageTask: " + path[0] + ", " + mData);
        GoogleApiClient client = MessengerService.getGoogleApiClient();
        Node node = MessengerService.getConnectedNode();

        if (client != null && node != null) {
            return Wearable.MessageApi.sendMessage(
                    MessengerService.getGoogleApiClient(),
                    MessengerService.getConnectedNode().getId(),
                    path[0],
                    mData
            ).await();
        }
        else {
            return null;
        }
    }
}
