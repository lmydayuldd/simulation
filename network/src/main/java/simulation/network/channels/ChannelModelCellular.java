package simulation.network.channels;

import commons.simulation.PhysicalObject;
import commons.simulation.PhysicalObjectType;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import simulation.network.*;
import simulation.util.Log;
import simulation.vehicle.Vehicle;

import java.util.*;

/**
 * Class representing a channel objects for indirect vehicle to vehicle communication via cellular network
 */
public class ChannelModelCellular extends NetworkChannelModel {

    /** Bit error rate for noise in the environment */
    public final static double BIT_ERROR_RATE_NOISE = 1E-4;

    /** Path loss exponent for urban environment */
    public final static double PATH_LOSS_EXPONENT = 3.0;

    /** Path loss factor */
    public final static double PATH_LOSS_FACTOR = 7E-12;

    /** Multi path base error */
    public final static double MULTI_PATH_BASE_ERROR = 1E-5;

    /** Multi path scale factor */
    public final static double MULTI_PATH_SCALE_FACTOR = 5E-4;

    /** Multi path maximum distance */
    public final static double MULTI_PATH_MAX_DISTANCE = 300.0;

    /** Multi path accuracy */
    public final static int MULTI_PATH_ACCURACY = 50;

    /** Multi path accuracy nearest position */
    public final static int MULTI_PATH_ACCURACY_NEAREST_POSITION = 10;

    /** Doppler effect base error */
    public final static double DOPPLER_BASE_ERROR = 1E-8;

    /** Doppler effect scale factor */
    public final static double DOPPLER_SCALE_FACTOR = 4.0;

    /** Code rate and modulation scale factor */
    public final static double CODE_RATE_MODULATION_SCALE_FACTOR = 0.45;

    /** Code rate and modulation exponent factor one */
    public final static double CODE_RATE_MODULATION_EXPONENT_FACTOR_ONE = 6.0;

    /** Code rate and modulation exponent factor two */
    public final static double CODE_RATE_MODULATION_EXPONENT_FACTOR_TWO = 2.0;

    /** Code rate and modulation exponent factor three */
    public final static double CODE_RATE_MODULATION_EXPONENT_FACTOR_THREE = 0.75;

    /** Maximum receive range QPSK */
    public final static double RANGE_MAXIMUM_RECEIVE_QPSK = 5000.0;

    /** Maximum receive range 16-QAM */
    public final static double RANGE_MAXIMUM_RECEIVE_16QAM = 3000.0;

    /** Maximum receive range 64-QAM */
    public final static double RANGE_MAXIMUM_RECEIVE_64QAM = 1000.0;

    /** Interference receive range factor */
    public final static double RANGE_INTERFERENCE_FACTOR = 1.3;

    /** Cellular objects specific: Hybrid Automatic Repeat Request (HARQ) limit error probability */
    public final static double HARQ_LIMIT_ERROR_PROBABILITY = 0.3;

    /** Cellular objects specific: Handover distance offset */
    public final static double HANDOVER_DISTANCE_OFFSET = 30.0;

    /** Channel information: Channel id, center carrier frequency in kHz, channel bandwidth in kHz */
    private static int[][] channelInfo = new int[][] {
        // Uplink channels: Band 3
        {1715000, 10000},
        {1725000, 10000},
        {1735000, 10000},
        {1745000, 10000},
        {1755000, 10000},
        {1765000, 10000},
        {1775000, 10000},
        // Uplink channels: Band 7
        {2505000, 10000},
        {2515000, 10000},
        {2525000, 10000},
        {2535000, 10000},
        {2545000, 10000},
        {2555000, 10000},
        {2565000, 10000},
        // Uplink channels: Band 65
        {1925000, 10000},
        {1935000, 10000},
        {1945000, 10000},
        {1955000, 10000},
        {1965000, 10000},
        {1975000, 10000},
        {1985000, 10000},

        // Downlink channels: Band 3
        {1810000, 10000},
        {1820000, 10000},
        {1830000, 10000},
        {1840000, 10000},
        {1850000, 10000},
        {1860000, 10000},
        {1870000, 10000},
        // Downlink channels: Band 7
        {2625000, 10000},
        {2635000, 10000},
        {2645000, 10000},
        {2655000, 10000},
        {2665000, 10000},
        {2675000, 10000},
        {2685000, 10000},
        // Downlink channels: Band 65
        {2115000, 10000},
        {2125000, 10000},
        {2135000, 10000},
        {2145000, 10000},
        {2155000, 10000},
        {2165000, 10000},
        {2175000, 10000},
    };

    /** Last maximum computed HARQ delay */
    private long lastMaxHarqDelay = 0L;

    /** Map for handover channel information, key vehicle ID, value base station ID */
    private final Map<Long, Long> cellBaseStationAssignmentMap = Collections.synchronizedMap(new HashMap<>());

    /** Map of all cell base stations, key base station ID, value base station network node */
    private final Map<Long, NetworkNode> cellBaseStationMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Function that handles network events
     *
     * @param event Network discrete event that is processed
     */
    @Override
    public synchronized void handleNetworkEvent(NetworkDiscreteEvent event) {
        // Time values for ending transmission sending in sender node
        long transmissionEnd = NetworkUtils.simTimeWithDelay(NetworkUtils.calcTransmissionTime(event.getEventMessage()));
        long transmissionEndDelay = 0L;
        lastMaxHarqDelay = 0L;

        // Call computation method for all network nodes except sending node
        super.handleNetworkEvent(event);

        // No further processing for wired message
        if (event.getEventMessage().isWiredMessage()) {
            return;
        }

        // Compute transmission calls changed last HARQ delay value, manage it correctly
        transmissionEndDelay = lastMaxHarqDelay;
        lastMaxHarqDelay = 0L;

        // Schedule end of transmission sending in sender node
        NetworkDiscreteEvent sendingEnd = new NetworkDiscreteEvent(transmissionEnd + transmissionEndDelay, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_END, event.getNetworkNode(), event.getEventMessage());
        NetworkSimulator.getInstance().scheduleEvent(sendingEnd);
    }

    /**
     * Function that computes if and how transmissions between network nodes are received
     *
     * @param sender Node that stars the transmission
     * @param otherNode All other nodes that might receive the transmission
     * @param message Message to be sent
     */
    @Override
    public synchronized void computeTransmission(NetworkNode sender, NetworkNode otherNode, NetworkMessage message) {
        // Special handling of wired messages between base stations
        if (message.isWiredMessage()) {
            if (sender.getPhysicalObject().getPhysicalObjectType() == PhysicalObjectType.PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION && otherNode.getPhysicalObject().getPhysicalObjectType() == PhysicalObjectType.PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION) {
                NetworkCellBaseStation senderStation = (NetworkCellBaseStation)(sender.getPhysicalObject());

                if (senderStation.getConnectedBaseStationIDs().contains(otherNode.getPhysicalObject().getId())) {
                    NetworkDiscreteEvent wiredEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(1500000L), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END, otherNode, message);
                    NetworkSimulator.getInstance().scheduleEvent(wiredEvent);
                }
            }

            return;
        }

        // Get channel information
        int channelId = 0;
        List<Integer> channelIDs = computeChannelIDs(sender, true);
        List<Integer> receiverChannelIDs = computeChannelIDs(otherNode, false);
        channelIDs.retainAll(receiverChannelIDs);

        // Check if sending and receiving channels match
        if (channelIDs.isEmpty()) {
            return;
        }

        channelId = channelIDs.get(0);
        message.setPhyChannelId(channelId);
        int channelCarrierKHz = channelInfo[channelId][0];

        // Compute maximum range values
        double maxClearTransmissionRange = RANGE_MAXIMUM_RECEIVE_QPSK;

        switch (message.getPhyBitsPerSignal()) {
            case 2: maxClearTransmissionRange = RANGE_MAXIMUM_RECEIVE_QPSK; break;
            case 4: maxClearTransmissionRange = RANGE_MAXIMUM_RECEIVE_16QAM; break;
            case 6: maxClearTransmissionRange = RANGE_MAXIMUM_RECEIVE_64QAM; break;
            default: break;
        }

        double maxInterferenceRange = maxClearTransmissionRange * RANGE_INTERFERENCE_FACTOR;

        // Compute total bit error rate
        double totalBitErrorRate = 0.0;

        // Noise objects, plain packet error rate from bit error rate and message length
        double errorRateNoise = BIT_ERROR_RATE_NOISE;
        totalBitErrorRate += errorRateNoise;

        // Distance and power objects
        double distance = sender.getPhysicalObject().getGeometryPos().getDistance(otherNode.getPhysicalObject().getGeometryPos());
        double errorRateDistance = (PATH_LOSS_FACTOR * Math.pow(distance, PATH_LOSS_EXPONENT));
        totalBitErrorRate += errorRateDistance;

        // Multi-path objects
        double errorRateMultiPath = MULTI_PATH_BASE_ERROR;

        // Add error for all other physical objects that are near the direct line communication depending on their distance
        for (PhysicalObject physicalObject : NetworkSimulator.getInstance().getPhysicalObjects()) {
            if (physicalObject.getId() != sender.getPhysicalObject().getId() && physicalObject.getId() != otherNode.getPhysicalObject().getId()) {
                RealVector senderPos = sender.getPhysicalObject().getGeometryPos();
                RealVector otherPos = physicalObject.getGeometryPos();
                RealVector diffVector = otherPos.subtract(senderPos);
                List<RealVector> positionList = Collections.synchronizedList(new LinkedList<>());

                for (int i = 0; i <= MULTI_PATH_ACCURACY; ++i) {
                    double factor = (double)(i) / (double)(MULTI_PATH_ACCURACY);
                    positionList.add(senderPos.add(diffVector.mapMultiply(factor)));
                }

                Map.Entry<Integer, RealVector> otherPositionInfo = Vehicle.getNearestPositionOnPositionList(positionList, otherPos, MULTI_PATH_ACCURACY_NEAREST_POSITION);
                RealVector otherNearestPosOnRay = otherPositionInfo.getValue();

                // Compute error value depending on distance
                double otherNearestDistance = otherPos.getDistance(otherNearestPosOnRay);

                if (otherNearestDistance < MULTI_PATH_MAX_DISTANCE) {

                    if (otherNearestDistance < 1.0) {
                        otherNearestDistance = 0.5;
                    }

                    errorRateMultiPath += (MULTI_PATH_SCALE_FACTOR / otherNearestDistance);
                }
            }
        }

        totalBitErrorRate += errorRateMultiPath;

        // Doppler shift objects
        double dopplerShiftAmount = 0.0;
        RealVector senderVelocity = sender.getPhysicalObject().getVelocity();
        double senderVelocityNorm = senderVelocity.getNorm();
        RealVector otherVelocity = otherNode.getPhysicalObject().getVelocity();
        double otherVelocityNorm = otherVelocity.getNorm();
        RealVector senderToOther = otherNode.getPhysicalObject().getGeometryPos().subtract(sender.getPhysicalObject().getGeometryPos());
        RealVector otherToSender = senderToOther.mapMultiply(-1.0);

        // There must be a distance for relativistic Doppler Effect
        if (senderToOther.getNorm() > 0.0) {
            double senderDopplerVelocity = 0.0;
            double otherDopplerVelocity = 0.0;

            if (senderVelocityNorm > 0.0) {
                double senderAngle = Math.acos(senderVelocity.cosine(otherToSender));
                senderDopplerVelocity = Math.cos(senderAngle) * senderVelocityNorm;
            }

            if (otherVelocityNorm > 0.0) {
                double otherAngle = Math.acos(otherVelocity.cosine(senderToOther));
                otherDopplerVelocity = Math.cos(otherAngle) * otherVelocityNorm;
            }

            double relativeVelocity = senderDopplerVelocity + otherDopplerVelocity;

            // Relative velocity needs to be negative if nodes move towards each other
            // It is positive when they move away from each other, put these values in formula
            double dopplerShiftAbsolute = 1000.0 * channelCarrierKHz * Math.sqrt((1.0 - (relativeVelocity / (double)(NetworkSettings.SPEED_OF_LIGHT))) / (1.0 + (relativeVelocity / (double)(NetworkSettings.SPEED_OF_LIGHT))));
            dopplerShiftAmount = Math.abs(dopplerShiftAbsolute - 1000.0 * channelCarrierKHz);
        }

        // Use absolute value for difference to center frequency and apply error
        double errorRateDoppler = 0.0;
        if (dopplerShiftAmount > 0.001) {
            errorRateDoppler = DOPPLER_BASE_ERROR * Math.pow(DOPPLER_SCALE_FACTOR, FastMath.log(2.0, dopplerShiftAmount));
            totalBitErrorRate += errorRateDoppler;
        }

        // Modulation and code rate objects
        double codeRateModulationFactor = 1.0;
        double modulationExponent = FastMath.log(2.0, message.getPhyBitsPerSignal());
        if (message.getPhyBitsPerSignal() == 1) {
            modulationExponent = 0.0;
        }

        if (message.getPhyCodeRate() > 0.0 && message.getPhyCodeRate() <= 1.0) {
            codeRateModulationFactor = message.getPhyCodeRate() * Math.pow(CODE_RATE_MODULATION_SCALE_FACTOR, (CODE_RATE_MODULATION_EXPONENT_FACTOR_ONE - (CODE_RATE_MODULATION_EXPONENT_FACTOR_TWO * (modulationExponent + CODE_RATE_MODULATION_EXPONENT_FACTOR_THREE) * message.getPhyCodeRate())));
        }

        totalBitErrorRate *= codeRateModulationFactor;

        // Normalize bit error rate between 0.0 and 1.0
        if (totalBitErrorRate < 0.0) {
            totalBitErrorRate = 0.0;
        }

        if (totalBitErrorRate > 1.0) {
            totalBitErrorRate = 1.0;
        }

        // From bit error rate compute success for packet receiving
        double packetErrorRate = (1.0 - Math.pow((1.0 - totalBitErrorRate), message.getMessageLengthBits() / message.getPhyCodeRate()));
        double packetSuccessProbability = (1.0 - packetErrorRate);

        Random rand = new Random();
        double randomChance = rand.nextDouble();

        // HARQ delay for cellular objects
        long harqDelay = 0L;
        if (packetSuccessProbability < randomChance) {
            if (packetSuccessProbability >= HARQ_LIMIT_ERROR_PROBABILITY) {
                long baseTime = 2 * (2 * NetworkUtils.randomNextLayerSimulationTime() + NetworkUtils.calcPropagationTime(sender, otherNode) + NetworkUtils.calcTransmissionTime(message));
                double factor = (1.0 / packetSuccessProbability) - 1.0;
                harqDelay = (long)(factor * baseTime);
                packetSuccessProbability = 1.0;

                // Update last max harq delay value
                if (harqDelay > lastMaxHarqDelay) {
                    lastMaxHarqDelay = harqDelay;
                }
            }
        }

        // If transmission successful, create successful transmission at receiver
        if (packetSuccessProbability >= randomChance && distance <= maxClearTransmissionRange) {
            long transmissionReceiveStart = NetworkUtils.simTimeWithDelay(NetworkUtils.calcPropagationTime(sender, otherNode));
            long transmissionReceiveEnd = NetworkUtils.simTimeWithDelay(harqDelay + NetworkUtils.calcPropagationTime(sender, otherNode) + NetworkUtils.calcTransmissionTime(message));
            NetworkDiscreteEvent eventReceiveStart = new NetworkDiscreteEvent(transmissionReceiveStart, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_START, otherNode, message);
            NetworkDiscreteEvent eventReceiveEnd = new NetworkDiscreteEvent(transmissionReceiveEnd, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END, otherNode, message);
            NetworkSimulator.getInstance().scheduleEvent(eventReceiveStart);
            NetworkSimulator.getInstance().scheduleEvent(eventReceiveEnd);

        // Otherwise if within interference range, create an interference receive at receiver
        } else if (distance <= maxInterferenceRange) {
            long transmissionReceiveStart = NetworkUtils.simTimeWithDelay(NetworkUtils.calcPropagationTime(sender, otherNode));
            long transmissionReceiveEnd = NetworkUtils.simTimeWithDelay(NetworkUtils.calcPropagationTime(sender, otherNode) + NetworkUtils.calcTransmissionTime(message));
            NetworkDiscreteEvent eventReceiveStart = new NetworkDiscreteEvent(transmissionReceiveStart, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_START, otherNode, message);
            NetworkDiscreteEvent eventReceiveEnd = new NetworkDiscreteEvent(transmissionReceiveEnd, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_END, otherNode, message);
            NetworkSimulator.getInstance().scheduleEvent(eventReceiveStart);
            NetworkSimulator.getInstance().scheduleEvent(eventReceiveEnd);
        }

        // Otherwise message did not have enough power to reach other node, do nothing in this case
    }

    /**
     * Function that computes a list of channel IDs used by a network node, either for receiving or sending
     *
     * @param node Node for which channel IDs should be computed
     * @param sending True to get the sending channel IDs for the node, false for receiving channel IDs
     * @return List of channel IDs, either for sending or receiving
     */
    @Override
    public List<Integer> computeChannelIDs(NetworkNode node, boolean sending) {
        List<Integer> resultList = new LinkedList<>();

        int frequencyBandCount = 3;
        int channelCountPerBand = 7;
        int sendOffset = (channelInfo.length / 2);

        // Base station communicates on all defined channels of its frequency band
        if (node.getPhysicalObject().getPhysicalObjectType() == PhysicalObjectType.PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION) {
            int startIndex = (int)(node.getPhysicalObject().getId() % (long)(frequencyBandCount)) * channelCountPerBand;
            for (int i = startIndex; i < startIndex + channelCountPerBand; ++i) {
                resultList.add(i + (sending ? sendOffset : 0));
            }

        // Otherwise assign one uplink or downlink channel
        } else {
            long nodeID = node.getPhysicalObject().getId();
            int channelIndexForBand = (int)(nodeID % (long)(channelCountPerBand));

            // Handover algorithm
            long preferredStationID = -1L;
            double preferredStationDistance = Double.MAX_VALUE;

            // Node was already assigned to a base station, get information
            if (cellBaseStationAssignmentMap.containsKey(nodeID)) {
                long stationID = cellBaseStationAssignmentMap.get(nodeID);

                if (cellBaseStationMap.containsKey(stationID)) {
                    RealVector stationPos = cellBaseStationMap.get(stationID).getPhysicalObject().getGeometryPos();
                    preferredStationID = cellBaseStationAssignmentMap.get(nodeID);
                    preferredStationDistance = node.getPhysicalObject().getGeometryPos().getDistance(stationPos);
                }
            }

            // Check all available base stations and choose nearest one with offset to prefer already connected station
            synchronized (cellBaseStationMap) {
                for (Map.Entry<Long, NetworkNode> entry : cellBaseStationMap.entrySet()) {
                    RealVector stationPos = entry.getValue().getPhysicalObject().getGeometryPos();
                    double distance = node.getPhysicalObject().getGeometryPos().getDistance(stationPos);

                    if (distance + (2.0 * HANDOVER_DISTANCE_OFFSET) < preferredStationDistance) {
                        preferredStationDistance = distance;
                        preferredStationID = entry.getKey();
                    }
                }
            }

            // Update information in map for valid station ID
            if (preferredStationID >= 0L) {
                cellBaseStationAssignmentMap.put(nodeID, preferredStationID);
            } else {
                Log.warning("ChannelModelCellular: No preferred base station was found, fallback to first frequency band!");
                preferredStationID = 0L;
            }

            // From preferredStationID compute final channel index
            int bandStartIndex = (int)(preferredStationID % (long)(frequencyBandCount)) * channelCountPerBand;
            int channelIndex = bandStartIndex + channelIndexForBand;
            resultList.add(channelIndex + (sending ? 0 : sendOffset));
        }

        return resultList;
    }

    /**
     * Function that computes the index for the modulation and data rate to be used
     *
     * @param node Node for which index should be computed
     * @return Integer index for the modulation and data rate info
     */
    @Override
    public int computeModulationAndDataRateIndex(NetworkNode node) {
        // Shift offset to downlink transmission values for base stations
        if (node.getPhysicalObject().getPhysicalObjectType() == PhysicalObjectType.PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION) {
            int arrayLength = NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfo().length;
            int halfArrayLength = arrayLength / 2;
            return Math.min(NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfoDefaultIndex() + halfArrayLength, arrayLength - 1);
        }

        // Return usual uplink value
        return NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfoDefaultIndex();
    }

    /**
     * Function that is called when the network simulation starts, useful for channel objects initialization
     */
    @Override
    public void networkSimulationStart() {
        for (NetworkNode node : NetworkSimulator.getInstance().getNetworkNodes()) {
            if (node.getPhysicalObject().getPhysicalObjectType() == PhysicalObjectType.PHYSICAL_OBJECT_TYPE_NETWORK_CELL_BASE_STATION) {
                cellBaseStationMap.put(node.getPhysicalObject().getId(), node);
            }
        }
    }
}
