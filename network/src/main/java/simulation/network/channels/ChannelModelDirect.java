package simulation.network.channels;

import commons.simulation.PhysicalObject;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import simulation.network.*;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.Vehicle;

import java.util.*;

/**
 * Class representing a channel objects for direct vehicle to vehicle communication
 */
public class ChannelModelDirect extends NetworkChannelModel {

    /** Bit error rate for noise in the environment */
    public final static double BIT_ERROR_RATE_NOISE = 1E-4;

    /** Path loss exponent for urban environment */
    public final static double PATH_LOSS_EXPONENT = 3.0;

    /** Path loss factor */
    public final static double PATH_LOSS_FACTOR = 7E-12;

    /** Modulation base error */
    public final static double MODULATION_BASE_ERROR = 1E-6;

    /** Modulation scale factor */
    public final static double MODULATION_SCALE_FACTOR = 1E+1;

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

    /** Maximum receive range */
    public final static double RANGE_MAXIMUM_RECEIVE = 1000.0;

    /** Interference receive range factor */
    public final static double RANGE_INTERFERENCE_FACTOR = 1.3;

    /** Channel information: Channel id, center carrier frequency in kHz, channel bandwidth in kHz */
    private static int[][] channelInfo = new int[][] {
        {5890000, 10000},
    };

    /**
     * Function that computes if and how transmissions between network nodes are received
     *
     * @param sender Node that stars the transmission
     * @param otherNode All other nodes that might receive the transmission
     * @param message Message to be sent
     */
    @Override
    public void computeTransmission(NetworkNode sender, NetworkNode otherNode, NetworkMessage message) {
        // Only send to other vehicles in this channel objects
        if (!(otherNode.getPhysicalObject() instanceof PhysicalVehicle)) {
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
        double maxClearTransmissionRange = RANGE_MAXIMUM_RECEIVE;
        double maxInterferenceRange = RANGE_MAXIMUM_RECEIVE * RANGE_INTERFERENCE_FACTOR;

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

        // If transmission successful, create successful transmission at receiver
        if (packetSuccessProbability >= randomChance && distance <= maxClearTransmissionRange) {
            long transmissionReceiveStart = NetworkUtils.simTimeWithDelay(NetworkUtils.calcPropagationTime(sender, otherNode));
            long transmissionReceiveEnd = NetworkUtils.simTimeWithDelay(NetworkUtils.calcPropagationTime(sender, otherNode) + NetworkUtils.calcTransmissionTime(message));
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
        resultList.add(0);
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
        return NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfoDefaultIndex();
    }

    /**
     * Function that is called when the network simulation starts, useful for channel objects initialization
     */
    @Override
    public void networkSimulationStart() {}
}
