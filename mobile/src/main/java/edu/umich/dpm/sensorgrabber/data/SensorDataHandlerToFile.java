package edu.umich.dpm.sensorgrabber.data;

import android.hardware.Sensor;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import edu.umich.dpm.sensorgrabber.sensor.SensorQueue;

public class SensorDataHandlerToFile implements SensorDataHandler {
    private static final String TAG = "SensorDataHandlerToFile";

    private final Hashtable<Integer, FileHandler> mFileHandlers = new Hashtable<>(5);

    private String mFileNamePrefix = "";
    private Date mSessionStartTime;


    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted");

        mSessionStartTime = Calendar.getInstance().getTime();
        mFileNamePrefix = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(mSessionStartTime);
    }

    @Override
    public void onStreamStopped() {
        Log.d(TAG, "onStreamStopped");

        for (FileHandler handler : mFileHandlers.values()) {
            handler.close();
        }
        mFileHandlers.clear();
    }

    @Override
    public void receiveData(Collection<SensorQueue> data) {
        Log.d(TAG, "Received " + data.size() + " queues");

        for (SensorQueue queue : data) {
            Log.d(TAG, "Writing queue w/ starting timestamp = " + String.valueOf(queue.getReadings()[0].getTimestamp()));
            int sensor = queue.getSensorType();
            FileHandler handler = mFileHandlers.get(sensor);
            try {
                if (handler == null) {
                    String filename = makeFileName(sensor);
                    Log.d(TAG, "Making new file handler: " + filename);
                    handler = new FileHandler(filename, mSessionStartTime);
                    mFileHandlers.put(sensor, handler);
                }
                handler.writeQueue(queue);
            }
            catch (IOException e) {
                Log.e(TAG, "Unable to decode data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static String sensorStringId(int sensor) {
        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:                 return "Accelerometer";
            case Sensor.TYPE_GYROSCOPE:                     return "Gyroscope";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:        return "GyroscopeUncalibrated";
            case Sensor.TYPE_HEART_RATE:                    return "HeartRate";
            case Sensor.TYPE_ROTATION_VECTOR:               return "RotationVector";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:   return "GeomagneticRotationVector";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:          return "GameRotationVector";
            case Sensor.TYPE_LINEAR_ACCELERATION:           return "LinearAcceleration";
            case Sensor.TYPE_GRAVITY:                       return "Gravity";
            case Sensor.TYPE_MAGNETIC_FIELD:                return "MagneticField";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:   return "MagneticFieldUncalibrated";
            case Sensor.TYPE_PROXIMITY:                     return "Proximity";
            case Sensor.TYPE_SIGNIFICANT_MOTION:            return "SignificantMotion";
            case Sensor.TYPE_STEP_COUNTER:                  return "StepCounter";
            case Sensor.TYPE_STEP_DETECTOR:                 return "StepDetector";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:           return "AmbientTemperature";
            case Sensor.TYPE_LIGHT:                         return "Light";
            case Sensor.TYPE_PRESSURE:                      return "Pressure";
            case Sensor.TYPE_RELATIVE_HUMIDITY:             return "RelativeHumidity";

            default:
                return "Unknown";
        }
    }

    public static String sensorDisplayName(int sensor) {
        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:                 return "Accelerometer";
            case Sensor.TYPE_GYROSCOPE:                     return "Gyroscope";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:        return "Gyroscope (uncalibrated)";
            case Sensor.TYPE_HEART_RATE:                    return "Heart rate";
            case Sensor.TYPE_ROTATION_VECTOR:               return "Rotation vector";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:   return "Geomagnetic rotation vector";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:          return "Game rotation vector";
            case Sensor.TYPE_LINEAR_ACCELERATION:           return "Linear acceleration";
            case Sensor.TYPE_GRAVITY:                       return "Gravity";
            case Sensor.TYPE_MAGNETIC_FIELD:                return "Magnetic field";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:   return "Magnetic field (uncalibrated)";
            case Sensor.TYPE_PROXIMITY:                     return "Proximity";
            case Sensor.TYPE_SIGNIFICANT_MOTION:            return "Significant motion";
            case Sensor.TYPE_STEP_COUNTER:                  return "Step counter";
            case Sensor.TYPE_STEP_DETECTOR:                 return "Step detector";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:           return "Ambient temperature";
            case Sensor.TYPE_LIGHT:                         return "Light";
            case Sensor.TYPE_PRESSURE:                      return "Pressure";
            case Sensor.TYPE_RELATIVE_HUMIDITY:             return "Relative humidity";

            default:
                return "Unknown";
        }
    }

    private String makeFileName(int sensor) {
        return mFileNamePrefix + "_" + sensorStringId(sensor);
    }
}
