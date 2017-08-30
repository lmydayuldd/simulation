package simulation.vehicle;

import simulation.util.Information;
import simulation.util.InformationService;

/**
 * A single error message to be stored in StatusLogger
 */
public class StatusMessage {
    /** Optional (human-readable) message for service staff */
    private String message;

    /** Error code */
    private Integer errorCode;

    /** Sensor reporting the message */
    private VehicleStatusSensor sensor;

    /** State of the sensor */
    private VehicleStatus status;

    /** Timestamp at which the status message was generated */
    private Long timestamp;

    /**
     * Create a status message with empty status message
     * @param sensor Attached status sensor
     * @param status Current state of status sensor
     * @param code   Error code of status sensor
     */
    public StatusMessage(VehicleStatusSensor sensor, VehicleStatus status, Integer code) {
        this.sensor = sensor;
        this.status = status;
        this.message = "";
        this.errorCode = code;
        this.timestamp = (Long) InformationService.getSharedInstance().requestInformation(Information.SIMULATION_TIME);
    }

    /**
     * Create a status message including a message
     * @param sensor  Attached status sensor
     * @param status  Current state of status sensor
     * @param message Human-readable message for service staff
     * @param code    Error code of status sensor
     */
    public StatusMessage(VehicleStatusSensor sensor, VehicleStatus status, Integer code, String message) {
        this.sensor = sensor;
        this.status = status;
        this.message = message;
        this.errorCode = code;
        this.timestamp = (Long) InformationService.getSharedInstance().requestInformation(Information.SIMULATION_TIME);
    }

    /**
     * Returns the (human-readable) error message
     * @return String containing status message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the sensor that generated the status message
     * @return Sensor that generated the status message
     */
    public VehicleStatusSensor getSensor() {
        return sensor;
    }

    /**
     * The status of the status message
     * @return Status of the message
     */
    public VehicleStatus getStatus() {
        return status;
    }

    /**
     * Overwrite toString() to get a nice output for status messages
     * @return String that contains all information of a status message
     */
    @Override
    public String toString() {
        return "StatusMessage{" +
                "message='" + message + '\'' +
                ", errorCode=" + errorCode +
                ", sensor=" + sensor +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}
