package edu.umich.dpm.sensorgrabber.stream;

public interface MessengerServiceListener {
    public void onConnectionStateChanged(MessengerService.ConnectionState state);
}
