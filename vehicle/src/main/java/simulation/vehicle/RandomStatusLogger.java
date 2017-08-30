package simulation.vehicle;

import simulation.util.Information;
import simulation.util.InformationService;
import simulation.util.NotificationCenter;
import simulation.util.Notification;

import java.util.Random;

/**
 * A special status logger that creates random events during the simulation
 */
public class RandomStatusLogger extends StatusLogger {

    /** Probability to detect a defect in engine */
    private double engineFailureProbability = 0.0000001;

    /** Service tyres every hour */
    private long brakeServiceFrequency = 60*60*1000;

    /** Simulation time of the last brake maintenance */
    private long lastBrakeService = 0;

    /** Frequency with which a critical oil level should be reported*/
    private long oilLevelFrequency = 1000;

    /** Status of the oil level*/
    private VehicleStatus oilStatus = VehicleStatus.VEHICLE_STATUS_OK;

    /** Simulation time of the last oil level check*/
    private long lastOilCheck = 0;

    /**
     * Constructor for a status logger that is attached to simulation
     */
    public RandomStatusLogger() {
        //Start listening for loop iterations
        NotificationCenter.getSharedInstance().registerListener(Notification.NOTIFICATION_LOOP_DONE, this::createRandomMessages, this);
    }


    /**
     * Randomly creates status messages. Not guaranteed to generate a message on every call.
     * @param o Context of received notification. Ignored.
     */
    private void createRandomMessages(Object o) {
        Long simTime = (Long)InformationService.getSharedInstance().requestInformation(Information.SIMULATION_TIME);
        Random rand = new Random();

        //Service tires
        if (lastBrakeService + brakeServiceFrequency <= simTime) {
            lastBrakeService = simTime;
            StatusMessage brakeServiceMessage = new StatusMessage(VehicleStatusSensor.VEHICLE_STATUS_SENSOR_BRAKE, VehicleStatus.VEHICLE_STATUS_SERVICE_REQUIRED, 0);
            addMessage(brakeServiceMessage);
        }

        //Random engine failures
        if (rand.nextFloat() < engineFailureProbability) {
            StatusMessage engineBrokenMessage = new StatusMessage(VehicleStatusSensor.VEHICLE_STATUS_SENSOR_ENGINE_STATUS, VehicleStatus.VEHICLE_STATUS_FAILURE, 0);
            addMessage(engineBrokenMessage);
        }

        //Report oil level
        if (lastOilCheck + oilLevelFrequency <= simTime) {
            lastOilCheck = simTime;
            if (rand.nextFloat() < 0.0001) {
                oilStatus = VehicleStatus.VEHICLE_STATUS_CRITICAL;
            }
            StatusMessage oilLevelMessage = new StatusMessage(VehicleStatusSensor.VEHICLE_STATUS_SENSOR_OIL_LEVEL, oilStatus, 0);
            addMessage(oilLevelMessage);
        }
    }
}
