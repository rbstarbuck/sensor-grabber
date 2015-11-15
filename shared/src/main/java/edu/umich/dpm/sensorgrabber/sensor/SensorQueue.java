package edu.umich.dpm.sensorgrabber.sensor;

import android.hardware.SensorEvent;
import android.util.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public final class SensorQueue implements Externalizable {
    private static final String TAG = "SensorQueue";

    public enum Status {
        ReadyToRead,
        ReadyToSend,
        Locked
    }

    private int mSensorType;
    private SensorReading[] mReadings;

    private transient int mCounter = 0;
    private transient boolean mIsLocked = false;


    public SensorQueue() {
        // Need public no-arg construcor for Externalizable
        // Never use this
    }

    public SensorQueue(int sensorType, int size)
            throws IllegalAccessException, InstantiationException {
        mSensorType = sensorType;
        mReadings = new SensorReading[size];
        for (int i = 0; i < size; ++i) {
            mReadings[i] = new SensorReading();
        }
    }

    public Status recordReading(SensorEvent e) {
        Log.v(TAG, "recordReading");
        if (mIsLocked) {
            Log.e(TAG, "Tried to recordReading on locked queue");
            return Status.Locked;
        }

        mReadings[mCounter].set(e);
        ++mCounter;

        if (mCounter < mReadings.length) {
            return Status.ReadyToRead;
        }
        else {
            Log.d(TAG, "Ready to send");
            mIsLocked = true;
            return Status.ReadyToSend;
        }
    }

    public void unlock() {
        mCounter = 0;
        mIsLocked = false;
    }

    public boolean isLocked() {
        return mIsLocked;
    }

    public int getSensorType() {
        return mSensorType;
    }

    public int getSize() {
        return mReadings.length;
    }

    public SensorReading[] getReadings() {
        return mReadings;
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        mSensorType = objectInput.readInt();
        mReadings = (SensorReading[]) objectInput.readObject();
        mCounter = 0;
        mIsLocked = false;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeInt(mSensorType);
        objectOutput.writeObject(mReadings);
    }
}
