package simulation.network;

/**
 * Class that represents the elements of a network message
 * This includes the message contents, length and header information
 */
public class NetworkMessage {

    /** Contents of message */
    private String messageContent = "";

    /** Possible transport sequence number */
    private int transportSequenceNumber = 0;

    /** Possible transport port source number */
    private int transportPortSourceNumber = 0;

    /** Possible transport port destination number */
    private int transportPortDestNumber = 0;

    /** Possible IPv6 address of the sender */
    private String networkIpv6Sender = "";

    /** Possible IPv6 address of the receiver */
    private String networkIpv6Receiver = "";

    /** Possible IPv6 hop count */
    private int networkHopLimit = 0;

    /** MAC address of the sender */
    private String macSender = "";

    /** MAC address of the receiver */
    private String macReceiver = "";

    /** Possible MAC sequence number */
    private int macSequenceNumber = 0;

    /** Length of message in KBits */
    private int phyDataRateKBits = 0;

    /** Code rate that is used for transmission, value between 0 and 1 */
    private double phyCodeRate = 0;

    /** Bits that are transmitted per signal, usually values of ld(2^n) */
    private int phyBitsPerSignal = 0;

    /** Length of slow transmission part in bits */
    private int phySlowTransmissionBits = 0;

    /** Channel ID on which message was sent and received, set by channel objects */
    private int phyChannelId = 0;

    /** Time of creation of the message in simulation, in nanoseconds */
    private long simCreateTimeNs = 0;

    /** Time of receiving of the message in simulation, in nanoseconds */
    private long simReceiveTimeNs = -1;

    /** Length of entire message in bits */
    private int messageLengthBits = 0;

    /** Amount of bits added by application layer */
    private int applicationLengthBits = 0;

    /** Amount of bits added by transport layer */
    private int transportAdditionalBits = 0;

    /** Amount of bits added by network layer */
    private int netAdditionalBits = 0;

    /** Amount of bits added by link layer */
    private int linkAdditionalBits = 0;

    /** Amount of bits added by physical layer */
    private int phyAdditionalBits = 0;

    /** Boolean flag indicating if message is sent over wired connection */
    private boolean isWiredMessage = false;

    /**
     * Function that returns messageContent
     *
     * @return Value for messageContent
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * Function that sets messageContent
     *
     * @param messageContent New value for messageContent
     */
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    /**
     * Function that returns transportSequenceNumber
     *
     * @return Value for transportSequenceNumber
     */
    public int getTransportSequenceNumber() {
        return transportSequenceNumber;
    }

    /**
     * Function that sets transportSequenceNumber
     *
     * @param transportSequenceNumber New value for transportSequenceNumber
     */
    public void setTransportSequenceNumber(int transportSequenceNumber) {
        this.transportSequenceNumber = transportSequenceNumber;
    }

    /**
     * Function that returns transportPortSourceNumber
     *
     * @return Value for transportPortSourceNumber
     */
    public int getTransportPortSourceNumber() {
        return transportPortSourceNumber;
    }

    /**
     * Function that sets transportSourcePortNumber
     *
     * @param transportPortSourceNumber New value for transportPortSourceNumber
     */
    public void setTransportPortSourceNumber(int transportPortSourceNumber) {
        this.transportPortSourceNumber = transportPortSourceNumber;
    }

    /**
     * Function that returns transportPortDestNumber
     *
     * @return Value for transportPortDestNumber
     */
    public int getTransportPortDestNumber() {
        return transportPortDestNumber;
    }

    /**
     * Function that sets transportPortDestNumber
     *
     * @param transportPortDestNumber New value for transportPortDestNumber
     */
    public void setTransportPortDestNumber(int transportPortDestNumber) {
        this.transportPortDestNumber = transportPortDestNumber;
    }

    /**
     * Function that returns networkIpv6Sender
     *
     * @return Value for networkIpv6Sender
     */
    public String getNetworkIpv6Sender() {
        return networkIpv6Sender;
    }

    /**
     * Function that sets networkIpv6Sender
     *
     * @param networkIpv6Sender New value for networkIpv6Sender
     */
    public void setNetworkIpv6Sender(String networkIpv6Sender) {
        this.networkIpv6Sender = networkIpv6Sender;
    }

    /**
     * Function that returns networkIpv6Receiver
     *
     * @return Value for networkIpv6Receiver
     */
    public String getNetworkIpv6Receiver() {
        return networkIpv6Receiver;
    }

    /**
     * Function that sets networkIpv6Receiver
     *
     * @param networkIpv6Receiver New value for networkIpv6Receiver
     */
    public void setNetworkIpv6Receiver(String networkIpv6Receiver) {
        this.networkIpv6Receiver = networkIpv6Receiver;
    }

    /**
     * Function that returns networkHopLimit
     *
     * @return Value for networkHopLimit
     */
    public int getNetworkHopLimit() {
        return networkHopLimit;
    }

    /**
     * Function that sets networkHopLimit
     *
     * @param networkHopLimit New value for networkHopLimit
     */
    public void setNetworkHopLimit(int networkHopLimit) {
        this.networkHopLimit = networkHopLimit;
    }

    /**
     * Function that returns macSender
     *
     * @return Value for macSender
     */
    public String getMacSender() {
        return macSender;
    }

    /**
     * Function that sets macSender
     *
     * @param macSender New value for macSender
     */
    public void setMacSender(String macSender) {
        this.macSender = macSender;
    }

    /**
     * Function that returns macReceiver
     *
     * @return Value for macReceiver
     */
    public String getMacReceiver() {
        return macReceiver;
    }

    /**
     * Function that sets macReceiver
     *
     * @param macReceiver New value for macReceiver
     */
    public void setMacReceiver(String macReceiver) {
        this.macReceiver = macReceiver;
    }

    /**
     * Function that returns macSequenceNumber
     *
     * @return Value for macSequenceNumber
     */
    public int getMacSequenceNumber() {
        return macSequenceNumber;
    }

    /**
     * Function that sets macSequenceNumber
     *
     * @param macSequenceNumber New value for macSequenceNumber
     */
    public void setMacSequenceNumber(int macSequenceNumber) {
        this.macSequenceNumber = macSequenceNumber;
    }

    /**
     * Function that returns phyDataRateKBits
     *
     * @return Value for phyDataRateKBits
     */
    public int getPhyDataRateKBits() {
        return phyDataRateKBits;
    }

    /**
     * Function that sets phyDataRateKBits
     *
     * @param phyDataRateKBits New value for phyDataRateKBits
     */
    public void setPhyDataRateKBits(int phyDataRateKBits) {
        this.phyDataRateKBits = phyDataRateKBits;
    }

    /**
     * Function that returns phyCodeRate
     *
     * @return Value for phyCodeRate
     */
    public double getPhyCodeRate() {
        return phyCodeRate;
    }

    /**
     * Function that sets phyCodeRate
     *
     * @param phyCodeRate New value for phyCodeRate
     */
    public void setPhyCodeRate(double phyCodeRate) {
        this.phyCodeRate = phyCodeRate;
    }

    /**
     * Function that returns phyBitsPerSignal
     *
     * @return Value for phyBitsPerSignal
     */
    public int getPhyBitsPerSignal() {
        return phyBitsPerSignal;
    }

    /**
     * Function that sets phyBitsPerSignal
     *
     * @param phyBitsPerSignal New value for phyBitsPerSignal
     */
    public void setPhyBitsPerSignal(int phyBitsPerSignal) {
        this.phyBitsPerSignal = phyBitsPerSignal;
    }

    /**
     * Function that returns phySlowTransmissionBits
     *
     * @return Value for phySlowTransmissionBits
     */
    public int getPhySlowTransmissionBits() {
        return phySlowTransmissionBits;
    }

    /**
     * Function that sets phySlowTransmissionBits
     *
     * @param phySlowTransmissionBits New value for phySlowTransmissionBits
     */
    public void setPhySlowTransmissionBits(int phySlowTransmissionBits) {
        this.phySlowTransmissionBits = phySlowTransmissionBits;
    }

    /**
     * Function that returns phyChannelId
     *
     * @return Value for phyChannelId
     */
    public int getPhyChannelId() {
        return phyChannelId;
    }

    /**
     * Function that sets phyChannelId
     *
     * @param phyChannelId New value for phyChannelId
     */
    public void setPhyChannelId(int phyChannelId) {
        this.phyChannelId = phyChannelId;
    }

    /**
     * Function that returns simCreateTimeNs
     *
     * @return Value for simCreateTimeNs
     */
    public long getSimCreateTimeNs() {
        return simCreateTimeNs;
    }

    /**
     * Function that sets simCreateTimeNs
     *
     * @param simCreateTimeNs New value for simCreateTimeNs
     */
    public void setSimCreateTimeNs(long simCreateTimeNs) {
        this.simCreateTimeNs = simCreateTimeNs;
    }

    /**
     * Function that returns simReceiveTimeNs
     *
     * @return Value for simReceiveTimeNs
     */
    public long getSimReceiveTimeNs() {
        return simReceiveTimeNs;
    }

    /**
     * Function that sets simReceiveTimeNs
     *
     * @param simReceiveTimeNs New value for simReceiveTimeNs
     */
    public void setSimReceiveTimeNs(long simReceiveTimeNs) {
        this.simReceiveTimeNs = simReceiveTimeNs;
    }

    /**
     * Function that returns messageLengthBits
     *
     * @return Value for messageLengthBits
     */
    public int getMessageLengthBits() {
        return messageLengthBits;
    }

    /**
     * Function that sets messageLengthBits
     *
     * @param messageLengthBits New value for messageLengthBits
     */
    public void setMessageLengthBits(int messageLengthBits) {
        this.messageLengthBits = messageLengthBits;
    }

    /**
     * Function that returns applicationLengthBits
     *
     * @return Value for applicationLengthBits
     */
    public int getApplicationLengthBits() {
        return applicationLengthBits;
    }

    /**
     * Function that sets applicationLengthBits
     *
     * @param applicationLengthBits New value for applicationLengthBits
     */
    public void setApplicationLengthBits(int applicationLengthBits) {
        this.applicationLengthBits = applicationLengthBits;
    }

    /**
     * Function that returns transportAdditionalBits
     *
     * @return Value for transportAdditionalBits
     */
    public int getTransportAdditionalBits() {
        return transportAdditionalBits;
    }

    /**
     * Function that sets transportAdditionalBits
     *
     * @param transportAdditionalBits New value for transportAdditionalBits
     */
    public void setTransportAdditionalBits(int transportAdditionalBits) {
        this.transportAdditionalBits = transportAdditionalBits;
    }

    /**
     * Function that returns netAdditionalBits
     *
     * @return Value for netAdditionalBits
     */
    public int getNetAdditionalBits() {
        return netAdditionalBits;
    }

    /**
     * Function that sets netAdditionalBits
     *
     * @param netAdditionalBits New value for netAdditionalBits
     */
    public void setNetAdditionalBits(int netAdditionalBits) {
        this.netAdditionalBits = netAdditionalBits;
    }

    /**
     * Function that returns linkAdditionalBits
     *
     * @return Value for linkAdditionalBits
     */
    public int getLinkAdditionalBits() {
        return linkAdditionalBits;
    }

    /**
     * Function that sets linkAdditionalBits
     *
     * @param linkAdditionalBits New value for linkAdditionalBits
     */
    public void setLinkAdditionalBits(int linkAdditionalBits) {
        this.linkAdditionalBits = linkAdditionalBits;
    }

    /**
     * Function that returns phyAdditionalBits
     *
     * @return Value for phyAdditionalBits
     */
    public int getPhyAdditionalBits() {
        return phyAdditionalBits;
    }

    /**
     * Function that sets phyAdditionalBits
     *
     * @param phyAdditionalBits New value for phyAdditionalBits
     */
    public void setPhyAdditionalBits(int phyAdditionalBits) {
        this.phyAdditionalBits = phyAdditionalBits;
    }

    /**
     * Function that returns a copy of the current message
     *
     * @return Copy of the current message
     */
    public NetworkMessage copy() {
        NetworkMessage copiedMessage = new NetworkMessage();
        copiedMessage.messageContent = messageContent;
        copiedMessage.transportSequenceNumber = transportSequenceNumber;
        copiedMessage.transportPortSourceNumber = transportPortSourceNumber;
        copiedMessage.transportPortDestNumber = transportPortDestNumber;
        copiedMessage.networkIpv6Sender = networkIpv6Sender;
        copiedMessage.networkIpv6Receiver = networkIpv6Receiver;
        copiedMessage.networkHopLimit = networkHopLimit;
        copiedMessage.macSender = macSender;
        copiedMessage.macReceiver = macReceiver;
        copiedMessage.macSequenceNumber = macSequenceNumber;
        copiedMessage.phyDataRateKBits = phyDataRateKBits;
        copiedMessage.phyCodeRate = phyCodeRate;
        copiedMessage.phyBitsPerSignal = phyBitsPerSignal;
        copiedMessage.phySlowTransmissionBits = phySlowTransmissionBits;
        copiedMessage.phyChannelId = phyChannelId;
        copiedMessage.simCreateTimeNs = simCreateTimeNs;
        copiedMessage.simReceiveTimeNs = simReceiveTimeNs;
        copiedMessage.messageLengthBits = messageLengthBits;
        copiedMessage.applicationLengthBits = applicationLengthBits;
        copiedMessage.transportAdditionalBits = transportAdditionalBits;
        copiedMessage.netAdditionalBits = netAdditionalBits;
        copiedMessage.linkAdditionalBits = linkAdditionalBits;
        copiedMessage.phyAdditionalBits = phyAdditionalBits;
        copiedMessage.isWiredMessage = isWiredMessage;
        return copiedMessage;
    }

    /**
     * Function that returns isWiredMessage
     *
     * @return Value for isWiredMessage
     */
    public boolean isWiredMessage() {
        return isWiredMessage;
    }

    /**
     * Function that sets isWiredMessage
     *
     * @param wiredMessage New value for isWiredMessage
     */
    public void setWiredMessage(boolean wiredMessage) {
        isWiredMessage = wiredMessage;
    }

    /**
     * Include all settings in toString method
     *
     * @return String that describes a network message
     */
    @Override
    public String toString() {
        return "NetworkMessage{" +
                "messageContent='" + messageContent + '\'' +
                ", transportSequenceNumber=" + transportSequenceNumber +
                ", transportPortSourceNumber=" + transportPortSourceNumber +
                ", transportPortDestNumber=" + transportPortDestNumber +
                ", networkIpv6Sender='" + networkIpv6Sender + '\'' +
                ", networkIpv6Receiver='" + networkIpv6Receiver + '\'' +
                ", networkHopLimit=" + networkHopLimit +
                ", macSender='" + macSender + '\'' +
                ", macReceiver='" + macReceiver + '\'' +
                ", macSequenceNumber=" + macSequenceNumber +
                ", phyDataRateKBits=" + phyDataRateKBits +
                ", phyCodeRate=" + phyCodeRate +
                ", phyBitsPerSignal=" + phyBitsPerSignal +
                ", phySlowTransmissionBits=" + phySlowTransmissionBits +
                ", phyChannelId=" + phyChannelId +
                ", simCreateTimeNs=" + simCreateTimeNs +
                ", simReceiveTimeNs=" + simReceiveTimeNs +
                ", messageLengthBits=" + messageLengthBits +
                ", applicationLengthBits=" + applicationLengthBits +
                ", transportAdditionalBits=" + transportAdditionalBits +
                ", netAdditionalBits=" + netAdditionalBits +
                ", linkAdditionalBits=" + linkAdditionalBits +
                ", phyAdditionalBits=" + phyAdditionalBits +
                ", isWiredMessage=" + isWiredMessage +
                '}';
    }
}
