package edu.umich.dpm.sensorgrabber.data;

import android.os.Build;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.umich.dpm.sensorgrabber.sensor.SensorQueue;
import edu.umich.dpm.sensorgrabber.sensor.SensorReading;

public class FileHandler {
    private static final String TAG = "FileHandler";

    private static final String SUB_DIRECTORY = "SensorGrabber";
    private static final String EXTENSION = "csv";

    private static final String STORAGE_DIRECTORY =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                    Environment.DIRECTORY_DOCUMENTS : Environment.DIRECTORY_DCIM);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h:mm:ss.SSS a");

    private final BufferedWriter mWriter;

    private final Date mStartTime;
    private long mFirstSensorTimestamp = -1;


    public FileHandler(String filename, Date startTime) throws IOException {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            throw new IOException("External storage is not mounted");
        }

        File root = new File(Environment.getExternalStoragePublicDirectory(STORAGE_DIRECTORY), SUB_DIRECTORY);
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new FileNotFoundException("Unable to create directory \"" + SUB_DIRECTORY + "\" in \"" + STORAGE_DIRECTORY + "\"");
            }
        }

        File file = new File(root, filename + "." + EXTENSION);
        mWriter = new BufferedWriter(new FileWriter(file, true));
        mStartTime = startTime;
    }

    public void writeQueue(SensorQueue queue) throws IOException {
        SensorReading[] readings = queue.getReadings();

        if (mFirstSensorTimestamp < 0 && readings.length > 0) {
            mFirstSensorTimestamp = readings[0].getTimestamp();
        }

        for (SensorReading r : readings) {
            writeLine(r);
        }
    }

    private void writeLine(SensorReading reading) throws IOException {
        long timeFromStart = (reading.getTimestamp() - mFirstSensorTimestamp) / 1000000;

        mWriter.write(String.valueOf(timeFromStart));
        mWriter.write(44);
        mWriter.write(DATE_FORMAT.format(mStartTime.getTime() + timeFromStart));
        mWriter.write(44);
        mWriter.write(String.valueOf(reading.getAccuracy()));
        for (float value : reading.getValues()) {
            mWriter.write(44);
            mWriter.write(String.valueOf(value));
        }
        mWriter.write(10);
    }

    public void close() {
        try {
            mWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
