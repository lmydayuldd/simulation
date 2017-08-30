package simulation.vehicle;

import simulation.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simulation of vehicle's fault memory
 */
public class StatusLogger {
    /** Error messages stored in memory */
    protected List<StatusMessage> messages = new LinkedList<>();

    /**
     * Adds a message to status memory.
     * @param message Message to be added
     */
    public void addMessage(StatusMessage message) {
        messages.add(message);
    }

    /**
     * Clears status memory
     */
    public void clearMemory() {
        this.messages = new LinkedList<>();
    }

    /**
     * Provides the caller with all stored messages.
     *
     * @return List containing status messages
     */
    public List<StatusMessage> readStatusMemory() {
        return this.messages;
    }

    /**
     * Retrieve messages above with at least certain severness
     * @param level Minimum severeness level of messages
     * @return List of status messages
     */
    public List<StatusMessage> readStatusMemory(VehicleStatus level) {
        List<StatusMessage> messages = readStatusMemory();
        messages = messages.stream().filter(m ->  m.getStatus().isWorseThanOrEqual(level)).collect(Collectors.toList());
        return messages;
    }

    /**
     * Print logging memory via Logger
     */
    public void logStatusMemory() {
        for (StatusMessage m : messages) {
            Log.info(m.toString());
        }
    }

    /**
     * Retrieve all messages of specific status sensor
     * @param sensor Sensor to be queried
     * @return  List of status messages
     */
    public List<StatusMessage> readStatusMemory(VehicleStatusSensor sensor) {
        List<StatusMessage> messages = readStatusMemory();
        messages = messages.stream().filter(m ->  m.getSensor() == sensor).collect(Collectors.toList());
        return messages;
    }
}
