package simulation.network;

import simulation.util.MathHelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Class that provides static utility functions for network computations
 */
public final class NetworkUtils {

    /** Empty constructor, this class has no instances, only static functions */
    private NetworkUtils() {}

    /**
     * Function that returns the current simulation time with additional delay
     *
     * @param delay Delay to be added to current simulation time
     */
    public static long simTimeWithDelay(long delay) {
        return NetworkSimulator.getInstance().getSimulationTimeNs() + delay;
    }

    /**
     * Function that returns the time that is needed for the transmission of a message
     *
     * @param message Message for which time should be computed
     */
    public static long calcTransmissionTime(NetworkMessage message) {
        int slowDataRate = NetworkSimulator.getInstance().getNetworkSettings().getSlowDataRateKBits() * 1000;
        int slowDataLength = message.getPhySlowTransmissionBits();
        int dataRate = message.getPhyDataRateKBits() * 1000;
        int dataLength = message.getMessageLengthBits() - slowDataLength;
        long result = (long)(1000000000.0 * (((double)(slowDataLength) / (double)(slowDataRate)) + ((double)(dataLength) / (double)(dataRate))));
        return result;
    }

    /**
     * Function that returns the time that is needed for the propagation between two nodes
     *
     * @param node1 First communicating node
     * @param node2 Second communicating node
     */
    public static long calcPropagationTime(NetworkNode node1, NetworkNode node2) {
        double distance = node1.getPhysicalObject().getGeometryPos().getDistance(node2.getPhysicalObject().getGeometryPos());
        long result = (long)(1000000000.0 * (distance / (double)(NetworkSettings.SPEED_OF_LIGHT)));
        return result;
    }

    /**
     * Function that returns the time that is needed for the transmission between two nodes
     *
     * @param networkNode Node in which a message should be stored
     * @param message Message to be stored in node
     */
    public static void putMessageInNode(NetworkNode networkNode, NetworkMessage message) {
        NetworkSettings settings = NetworkSimulator.getInstance().getNetworkSettings();

        // Abort if 0 or invalid size specified
        if (settings.getMessageBufferSize() <= 0) {
            return;
        }

        // Ipv6 message processing
        if (networkNode.getRecentIpv6MessagesMap().containsKey(message.getNetworkIpv6Sender())) {
            List<NetworkMessage> messageList = networkNode.getRecentIpv6MessagesMap().get(message.getNetworkIpv6Sender());
            messageList.add(0, message);

            // Keep maximum size
            if (messageList.size() >= settings.getMessageBufferSize()) {
                List<NetworkMessage> subMessageList = Collections.synchronizedList(new LinkedList<>(messageList.subList(0, settings.getMessageBufferSize())));
                messageList.clear();
                messageList.addAll(subMessageList);
            }
        } else {
            // Create new map entry
            List<NetworkMessage> newList = Collections.synchronizedList(new LinkedList<NetworkMessage>());
            newList.add(message);
            networkNode.getRecentIpv6MessagesMap().putIfAbsent(message.getNetworkIpv6Sender(), newList);
        }

        // Mac message processing
        if (settings.getSettingsId() != NetworkSettingsId.NETWORK_SETTINGS_ID_CELLULAR) {
            if (networkNode.getRecentMacMessagesMap().containsKey(message.getMacSender())) {
                List<NetworkMessage> messageList = networkNode.getRecentMacMessagesMap().get(message.getMacSender());
                messageList.add(0, message);

                // Keep maximum size
                if (messageList.size() >= settings.getMessageBufferSize()) {
                    List<NetworkMessage> subMessageList = Collections.synchronizedList(new LinkedList<>(messageList.subList(0, settings.getMessageBufferSize())));
                    messageList.clear();
                    messageList.addAll(subMessageList);
                }
            } else {
                // Create new map entry
                List<NetworkMessage> newList = Collections.synchronizedList(new LinkedList<NetworkMessage>());
                newList.add(message);
                networkNode.getRecentMacMessagesMap().putIfAbsent(message.getMacSender(), newList);
            }
        }
    }

    /**
     * Function that returns a random number between 0 and 2^bits-1
     *
     * @param bits Amount of bits to be used for max value
     * @return Random number between 0 and 2^bits-1
     */
    public static int getRandomPositiveNumberBits(int bits) {
        Random random = new Random();
        return random.nextInt(1 << bits);
    }

    /**
     * Function that returns a bit representation of a list of floats
     *
     * @param floatList List of floats to be converted to bit output
     * @return Concatenated string of bits for float values
     */
    public static String floatListToBitString(List<Float> floatList) {
        String result = "";

        for (Float value : floatList) {
            int intRepresentation = Float.floatToIntBits(value);
            String strRepresentation = Integer.toBinaryString(intRepresentation);

            // Need to prepend zeros until we are at 32 bit
            while (strRepresentation.length() < 32) {
                strRepresentation = "0" + strRepresentation;
            }

            result = result + strRepresentation;
        }

        return result;
    }

    /**
     * Function that returns a list of floats from a bit representation
     *
     * @param bitString String of bits to be converted to list of floats
     * @throws IllegalArgumentException if input is not a multiple of 32
     * @return List of floats represented in the bit string
     */
    public static List<Float> bitStringToFloatList(String bitString) {
        // Check for valid input, otherwise throw exception
        if ((bitString.length() % 32) != 0) {
            throw new IllegalArgumentException("NetworkUtils - bitStringToFloatList: Invalid input, no multiple of 32: " + bitString);
        }

        int chunkCount = bitString.length() / 32;
        List<Float> resultList = Collections.synchronizedList(new LinkedList<>());

        for (int i = 0; i < chunkCount; ++i) {
            String floatStr = bitString.substring(i * 32, (i+1) * 32);

            // Remove leading zeros, considering special case that original string might have been just a single "0"
            while (floatStr.length() > 1 && floatStr.charAt(0) == '0') {
                floatStr = floatStr.substring(1);
            }

            resultList.add(Float.intBitsToFloat(Integer.parseUnsignedInt(floatStr, 2)));
        }

        return resultList;
    }

    /**
     * Function that returns the simulation time for the next layer within a specified randomized interval in network settings
     *
     * @return Long value for random delay of layer
     */
    public static long randomNextLayerSimulationTime() {
        long delay = MathHelper.randomLong(NetworkSimulator.getInstance().getNetworkSettings().getMinimumLocalDelayPerLayer(), NetworkSimulator.getInstance().getNetworkSettings().getMaximumLocalDelayPerLayer());
        return delay;
    }
}
