package simulation.network;

import commons.simulation.DiscreteEvent;

/**
 * Class that holds all relevant information for a network discrete event
 */
public class NetworkDiscreteEvent implements DiscreteEvent {

    /** Time of the event measured in nanoseconds */
    private long eventTimeNs;

    /** Type of the event */
    private NetworkDiscreteEventId eventId;

    /** Message of the event */
    private NetworkMessage eventMessage;

    /** Network node related to this event */
    private NetworkNode networkNode;

    /**
     * Constructor for a network discrete event
     * @param eventTimeNs Time of the event measured in nanoseconds
     * @param eventId Id of the event
     * @param networkNode Network node related to this event
     * @param eventMessage Message of the event
     */
    public NetworkDiscreteEvent(long eventTimeNs, NetworkDiscreteEventId eventId, NetworkNode networkNode, NetworkMessage eventMessage) {
        this.eventTimeNs = eventTimeNs;
        this.eventId = eventId;
        this.eventMessage = eventMessage;
        this.networkNode = networkNode;
    }

    /**
     * Function that returns the network event id of the event
     *
     * @return Network event id of the event
     */
    public NetworkDiscreteEventId getNetworkEventId() {
        return eventId;
    }

    /**
     * Function that returns the message of the event
     *
     * @return Message of the event
     */
    public NetworkMessage getEventMessage() {
        return eventMessage;
    }

    /**
     * Function that returns the network node of the event
     *
     * @return Network node of the event
     */
    public NetworkNode getNetworkNode() {
        return networkNode;
    }

    /**
     * Function that returns the time of the event
     *
     * @return Time of the event
     */
    @Override
    public long getEventTime() {
        return eventTimeNs;
    }

    /**
     * Function that returns a numeric identifier for the event
     *
     * @return Numeric identifier for the event
     */
    @Override
    public int getEventId() {
        return eventId.ordinal();
    }

    /**
     * Improved toString() method to get more information
     *
     * @return String of information about object
     */
    @Override
    public String toString() {
        return "NetworkDiscreteEvent{" +
                "eventTimeNs=" + eventTimeNs +
                ", eventId=" + eventId +
                ", eventMessage=" + eventMessage +
                ", networkNode=" + networkNode +
                '}';
    }
}
