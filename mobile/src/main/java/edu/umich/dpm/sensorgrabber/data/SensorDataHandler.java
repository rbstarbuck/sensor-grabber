package edu.umich.dpm.sensorgrabber.data;

import java.util.Collection;

import edu.umich.dpm.sensorgrabber.sensor.SensorQueue;
import edu.umich.dpm.sensorgrabber.stream.Streamable;

public interface SensorDataHandler extends Streamable {
    public void receiveData(Collection<SensorQueue> data);
}
