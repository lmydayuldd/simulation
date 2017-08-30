package simulation.network;

import simulation.util.Log;

/**
 * Singleton class that records statistical data of the network simulation
 */
public class NetworkStatistics {

    /** Singleton instance of this class */
    private static NetworkStatistics instance = null;

    /** Sent amount of messages at PHY layer */
    private long sentMessagesAmountPhy = 0L;

    /** Received amount of messages at LINK layer */
    private long receivedMessagesAmountLink = 0L;

    /** Received amount of messages at APP layer */
    private long receivedMessagesAmountApp = 0L;

    /** Sent amount of messages at APP layer */
    private long sentMessagesAmountApp = 0L;

    /** Average latency of successfully received messages at APP layer in nanoseconds */
    private double averageLatencyNsApp = 0.0;

    /** Average size of sent messages in bits at PHY layer */
    private double averageSizeBitsPhy = 0.0;

    /** Average data rate of all messages at PHY layer */
    private double averageSentDataRateKBitsPhy = 0.0;

    /** Average application data size of all messages at PHY layer */
    private double averageSizeBitsApp = 0.0;

    /** Amount of interrupted message receiving processes at a receiver at PHY layer (at least two overlapping) */
    private long receiveInterruptionsPhy = 0L;

    /**
     * NetworkStatistics constructor, only called by getInstance singleton function
     */
    private NetworkStatistics() {
    }

    /**
     * Function to get or create a new singleton instance of this class
     *
     * @return Singleton instance of NetworkStatistics
     */
    public static NetworkStatistics getInstance() {
        if (instance == null) {
            instance = new NetworkStatistics();
        }

        return instance;
    }

    /**
     * Function to reset the singleton class, useful for tests
     */
    public static void resetInstance() {
        instance = null;
    }

    /**
     * Function to process a successfully received message on APP layer in the statistics
     */
    public void processReceivedMessageApp(NetworkMessage message) {
        // Ignore wired messages
        if (message.isWiredMessage()) {
            return;
        }

        if (message.getSimReceiveTimeNs() < message.getSimCreateTimeNs()) {
            Log.warning("NetworkStatistics - processReceivedMessageApp: Skipped message, receive time is invalid: " + message);
            return;
        }

        double latency = (double)(message.getSimReceiveTimeNs() - message.getSimCreateTimeNs());
        averageLatencyNsApp = (((double)(receivedMessagesAmountApp) * averageLatencyNsApp + latency) / (double)(receivedMessagesAmountApp + 1L));
        receivedMessagesAmountApp++;
    }

    /**
     * Function to process sending a message on APP layer in the statistics
     */
    public void processSendMessageApp(NetworkMessage message) {
        // Ignore wired messages
        if (message.isWiredMessage()) {
            return;
        }

        sentMessagesAmountApp++;
    }

    /**
     * Function to process a successfully received message on LINK layer in the statistics
     */
    public void processReceivedMessageLink(NetworkMessage message) {
        // Ignore wired messages
        if (message.isWiredMessage()) {
            return;
        }

        receivedMessagesAmountLink++;
    }

    /**
     * Function to process a started message sending process on PHY layer in the statistics
     */
    public void processSendMessageStartPhy(NetworkMessage message) {
        // Ignore wired messages
        if (message.isWiredMessage()) {
            return;
        }

        double dataRateKBits = (double)(message.getPhyDataRateKBits());
        double applicationMessageSizeBits = (double)(message.getApplicationLengthBits());
        double messageSize = (double)(message.getMessageLengthBits());
        averageSizeBitsPhy = (((double)(sentMessagesAmountPhy) * averageSizeBitsPhy + messageSize) / (double)(sentMessagesAmountPhy + 1L));
        averageSentDataRateKBitsPhy = (((double)(sentMessagesAmountPhy) * averageSentDataRateKBitsPhy + dataRateKBits) / (double)(sentMessagesAmountPhy + 1L));
        averageSizeBitsApp = (((double)(sentMessagesAmountPhy) * averageSizeBitsApp + applicationMessageSizeBits) / (double)(sentMessagesAmountPhy + 1L));
        sentMessagesAmountPhy++;
    }

    /**
     * Function to process receive interruptions on PHY layer
     */
    public void processReceiveInterruptionPhy() {
        receiveInterruptionsPhy++;
    }

    /**
     * Function that returns sentMessagesAmountPhy
     *
     * @return Value for sentMessagesAmountPhy
     */
    public long getSentMessagesAmountPhy() {
        return sentMessagesAmountPhy;
    }

    /**
     * Function that returns receivedMessagesAmountApp
     *
     * @return Value for receivedMessagesAmountApp
     */
    public long getReceivedMessagesAmountApp() {
        return receivedMessagesAmountApp;
    }

    /**
     * Function that returns averageLatencyNsApp
     *
     * @return Value for averageLatencyNsApp
     */
    public double getAverageLatencyNsApp() {
        return averageLatencyNsApp;
    }

    /**
     * Function that returns averageSizeBitsPhy
     *
     * @return Value for averageSizeBitsPhy
     */
    public double getAverageSizeBitsPhy() {
        return averageSizeBitsPhy;
    }

    /**
     * Function that returns averageSentDataRateKBitsPhy
     *
     * @return Value for averageSentDataRateKBitsPhy
     */
    public double getAverageSentDataRateKBitsPhy() {
        return averageSentDataRateKBitsPhy;
    }

    /**
     * Function that returns receiveInterruptionsPhy
     *
     * @return Value for receiveInterruptionsPhy
     */
    public long getReceiveInterruptionsPhy() {
        return receiveInterruptionsPhy;
    }

    /**
     * Function that returns averageSizeBitsApp
     *
     * @return Value for averageSizeBitsApp
     */
    public double getAverageSizeBitsApp() {
        return averageSizeBitsApp;
    }

    /**
     * Function that returns receivedMessagesAmountLink
     *
     * @return Value for receivedMessagesAmountLink
     */
    public long getReceivedMessagesAmountLink() {
        return receivedMessagesAmountLink;
    }

    /**
     * Function that returns sentMessagesAmountApp
     *
     * @return Value for sentMessagesAmountApp
     */
    public long getSentMessagesAmountApp() {
        return sentMessagesAmountApp;
    }

    /**
     * Function that produces the statistics text output
     *
     * @return String for the statistics output
     */
    public String generateStatisticsOutput() {
        return "NetworkStatistics{" +
                "sentMessagesAmountPhy=" + sentMessagesAmountPhy +
                ", receivedMessagesAmountLink=" + receivedMessagesAmountLink +
                ", sentMessagesAmountApp=" + sentMessagesAmountApp +
                ", receivedMessagesAmountApp=" + receivedMessagesAmountApp +
                ", averageSizeBitsPhy=" + averageSizeBitsPhy +
                ", averageSizeBitsApp=" + averageSizeBitsApp +
                ", averageLatencyNsApp=" + averageLatencyNsApp +
                ", averageSentDataRateKBitsPhy=" + averageSentDataRateKBitsPhy +
                ", receiveInterruptionsPhy=" + receiveInterruptionsPhy +
                '}';
    }
}
