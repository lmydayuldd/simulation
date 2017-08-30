package simulation.network.tasks;

import commons.controller.commons.BusEntry;
import commons.controller.commons.Vertex;
import commons.simulation.Sensor;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import simulation.network.*;
import simulation.util.Log;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.Vehicle;

import java.util.*;

import static commons.controller.commons.BusEntry.SENSOR_COMPASS;
import static commons.controller.commons.BusEntry.SENSOR_GPS_COORDINATES;
import static commons.controller.commons.BusEntry.SENSOR_VELOCITY;
import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Task that acts as main application message receiver, parse messages and call other applications
 * Generate beacon messages in regular intervals
 */
public class TaskAppBeacon extends NetworkTask {

    /** Port number for status messages */
    public final static int APP_BEACON_PORT_NUMBER_STATUS_MSG = 60001;

    /** Port number for trajectory messages */
    public final static int APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG = 60002;

    /** Counter for beacons */
    private long beaconCounter = 0;

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskAppBeacon(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_APP_BEACON);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(
            NETWORK_EVENT_ID_RANDOM_START_INITIALIZE, NETWORK_EVENT_ID_APP_RECEIVE,
            NETWORK_EVENT_ID_APP_SEND, NETWORK_EVENT_ID_SELF_PERIODIC));

        beaconCounter = 0;
    }

    /**
     * Function that handles network events internally in the task
     *
     * @param event Network discrete event to be handled
     */
    @Override
    public void taskHandleNetworkEvent(NetworkDiscreteEvent event) {
        // Handle events
        switch (event.getNetworkEventId()) {
            case NETWORK_EVENT_ID_RANDOM_START_INITIALIZE: {
                // Schedule periodic event
                NetworkMessage messageTaskName = new NetworkMessage();
                messageTaskName.setMessageContent(getTaskId().name());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_SELF_PERIODIC: {
                // Return if periodic update that is not from this task
                if (!event.getEventMessage().getMessageContent().equals(getTaskId().name())) {
                    return;
                }

                // Perform periodic operations
                sendBroadcastBeaconMessage();

                // Create new event to repeat periodic call
                NetworkMessage messageTaskName = new NetworkMessage();
                messageTaskName.setMessageContent(getTaskId().name());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkSimulator.getInstance().getNetworkSettings().getApplicationBeaconUpdateInterval()), NetworkDiscreteEventId.NETWORK_EVENT_ID_SELF_PERIODIC, networkNode, messageTaskName);
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_APP_RECEIVE: {
                // Skip unsupported port numbers
                if (event.getEventMessage().getTransportPortDestNumber() != APP_BEACON_PORT_NUMBER_STATUS_MSG && event.getEventMessage().getTransportPortDestNumber() != APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG) {
                    return;
                }

                // Redirect messages to other application tasks
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_APP_UPDATE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_APP_SEND: {
                // Forward message with delay to transport
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_TRANSPORT_SEND, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }

    /**
     * Function that creates a new beacon message and sends it in broadcast mode
     */
    private void sendBroadcastBeaconMessage() {
        beaconCounter++;

        // Convert to vehicle
        if (networkNode.getPhysicalObject() instanceof PhysicalVehicle) {
            PhysicalVehicle physicalVehicle = (PhysicalVehicle)(networkNode.getPhysicalObject());
            Vehicle vehicle = physicalVehicle.getSimulationVehicle();
            List<Float> messageFloats = Collections.synchronizedList(new LinkedList<>());
            NetworkMessage message = new NetworkMessage();

            // Each third message is trajectory update message, otherwise regular status message
            if ((beaconCounter % 3L) == 0) {
                // Read GPS sensor value
                Optional<Sensor> gpsSensor = vehicle.getSensorByType(SENSOR_GPS_COORDINATES);
                RealVector gpsPosition = new ArrayRealVector(new double[]{0.0, 0.0, 0.0});

                if (gpsSensor.isPresent()) {
                    Object gpsSensorValue = gpsSensor.get().getValue();
                    gpsPosition = (RealVector)(gpsSensorValue);
                } else {
                    Log.warning("TaskAppBeacon - sendBroadcastBeaconMessage: Missing GPS sensor in trajectory extraction, vehicle: " + vehicle);
                    return;
                }

                // Extract next trajectory points for vehicle with at most 25 data points from vehicle
                List<Vertex> trajectoryList = vehicle.getTrajectory();

                // Nothing to process if list is empty
                if (trajectoryList.isEmpty()) {
                    return;
                }

                Map.Entry<Integer, RealVector> trajectoryNearestData = Vehicle.getNearestPositionOnTrajectory(trajectoryList, gpsPosition, 20);
                int nextVertex = trajectoryNearestData.getKey();
                RealVector nextVertexPos = trajectoryList.get(nextVertex).getPosition();
                RealVector nearestPosOnTrajectory = trajectoryNearestData.getValue();

                int maxSentLength = 25;

                // Add intermediate point if too far away from next regular node
                if (nearestPosOnTrajectory.getDistance(nextVertexPos) > 0.2) {
                    messageFloats.add((float)(nearestPosOnTrajectory.getEntry(0)));
                    messageFloats.add((float)(nearestPosOnTrajectory.getEntry(1)));
                    messageFloats.add((float)(nearestPosOnTrajectory.getEntry(2)));
                    maxSentLength--;
                }

                // Add trajectory values to message
                for (Vertex v : trajectoryList.subList(nextVertex, Math.min(trajectoryList.size(), nextVertex + maxSentLength))) {
                    RealVector vectorData = v.getPosition();
                    messageFloats.add((float)(vectorData.getEntry(0)));
                    messageFloats.add((float)(vectorData.getEntry(1)));
                    messageFloats.add((float)(vectorData.getEntry(2)));
                }

                // Set message ports
                message.setTransportPortSourceNumber(APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG);
                message.setTransportPortDestNumber(APP_BEACON_PORT_NUMBER_TRAJECTORY_MSG);
            } else {
                // Add sensor values to message
                List<BusEntry> sensorEntries = Arrays.asList(SENSOR_GPS_COORDINATES, SENSOR_VELOCITY, SENSOR_COMPASS);
                for (BusEntry sensorEntry : sensorEntries) {
                    Optional<Sensor> sensor = vehicle.getSensorByType(sensorEntry);

                    if (sensor.isPresent()) {
                        Object sensorValue = sensor.get().getValue();

                        switch (sensorEntry) {
                            case SENSOR_GPS_COORDINATES:
                                RealVector vectorData = (RealVector)(sensorValue);
                                messageFloats.add((float)(vectorData.getEntry(0)));
                                messageFloats.add((float)(vectorData.getEntry(1)));
                                messageFloats.add((float)(vectorData.getEntry(2)));
                                break;
                            case SENSOR_VELOCITY:
                            case SENSOR_COMPASS:
                                double doubleData = (Double)(sensorValue);
                                messageFloats.add((float)(doubleData));
                                break;
                            default:
                                Log.warning("TaskAppBeacon - sendBroadcastBeaconMessage: Missing switch case for sensor entry: " + sensorEntry.name() + ", vehicle: " + vehicle);
                                break;
                        }
                    } else {
                        Log.warning("TaskAppBeacon - sendBroadcastBeaconMessage: No message sent, vehicle has missing sensor: " + sensorEntry.name() + ", vehicle: " + vehicle);
                        return;
                    }
                }

                // Add vehicle values to message
                messageFloats.add((float)(vehicle.getLength()));
                messageFloats.add((float)(vehicle.getWidth()));
                messageFloats.add((float)(vehicle.getHeight()));

                // Set message ports
                message.setTransportPortSourceNumber(APP_BEACON_PORT_NUMBER_STATUS_MSG);
                message.setTransportPortDestNumber(APP_BEACON_PORT_NUMBER_STATUS_MSG);
            }

            message.setMessageContent(NetworkUtils.floatListToBitString(messageFloats));
            message.setMessageLengthBits(32 * messageFloats.size());
            message.setApplicationLengthBits(32 * messageFloats.size());
            message.setNetworkIpv6Receiver(NetworkSimulator.getInstance().getNetworkSettings().getIpv6LinkLocalMulticastAddress());

            // Schedule event for sending
            NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_APP_SEND, networkNode, message);
            NetworkSimulator.getInstance().scheduleEvent(newEvent);
        }
    }
}
