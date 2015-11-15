package edu.umich.dpm.sensorgrabber.sensor;

import android.hardware.SensorEvent;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SensorReading implements Externalizable {
    private long mTimestamp = 0;
    private int mAccuracy = 0;
    private float[] mValues = null;


    public SensorReading() { }

    public void set(SensorEvent e) {
        mTimestamp = e.timestamp;
        mAccuracy = e.accuracy;

        if (mValues == null) {
            mValues = new float[e.values.length];
        }
        System.arraycopy(e.values, 0, mValues, 0, e.values.length);
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public int getAccuracy() {
        return mAccuracy;
    }

    public float[] getValues() {
        return mValues;
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        mTimestamp = objectInput.readLong();
        mAccuracy = objectInput.readInt();
        mValues = (float[]) objectInput.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeLong(mTimestamp);
        objectOutput.writeInt(mAccuracy);
        objectOutput.writeObject(mValues);
    }
}
