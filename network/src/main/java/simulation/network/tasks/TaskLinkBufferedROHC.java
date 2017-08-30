package simulation.network.tasks;

import simulation.network.*;

import java.util.*;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Task that performs buffered medium access and avoids sending when the node is already sending with ROHC
 */
public class TaskLinkBufferedROHC extends NetworkTask {

    /** ROHC compression factor NET layer */
    public final static int TASK_LINK_BUFFERED_ROHC_COMPRESSION_NET = 10;

    /** ROHC compression factor TRANSPORT layer */
    public final static int TASK_LINK_BUFFERED_ROHC_COMPRESSION_TRANSPORT = 8;

    /** Queue of messages to be sent when medium is idle */
    private final List<NetworkMessage> messageQueue = Collections.synchronizedList(new LinkedList<>());

    /** Set of busy channel IDs */
    private Set<Integer> busyChannels = Collections.synchronizedSet(new HashSet<>());

    /** Time value indicating when forwarding to PHY is done */
    private long forwardToPhyDoneTimeNs = 0L;

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskLinkBufferedROHC(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_LINK_BUFFERED_ROHC);
        setNetworkNode(node);
        this.messageQueue.clear();
        this.busyChannels.clear();
        forwardToPhyDoneTimeNs = 0L;
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_LINK_RECEIVE, NETWORK_EVENT_ID_LINK_SEND,
                NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, NETWORK_EVENT_ID_LINK_FORWARD_TO_PHY));
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
            case NETWORK_EVENT_ID_LINK_RECEIVE: {
                // For wired messages, just forward message
                if (event.getEventMessage().isWiredMessage()) {
                    NetworkDiscreteEvent wiredEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_NET_RECEIVE, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(wiredEvent);
                    return;
                }

                // Robust Header Compression (ROHC) for upper layers - decompression
                int netDecompressed = event.getEventMessage().getNetAdditionalBits() * TASK_LINK_BUFFERED_ROHC_COMPRESSION_NET;
                int transportDecompressed = event.getEventMessage().getTransportAdditionalBits() * TASK_LINK_BUFFERED_ROHC_COMPRESSION_TRANSPORT;
                int addBits = (netDecompressed - event.getEventMessage().getNetAdditionalBits()) + (transportDecompressed - event.getEventMessage().getTransportAdditionalBits());
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + addBits);
                event.getEventMessage().setNetAdditionalBits(netDecompressed);
                event.getEventMessage().setTransportAdditionalBits(transportDecompressed);

                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_NET_RECEIVE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_LINK_FORWARD_TO_PHY: {
                long eventTime = forwardToPhyDoneTimeNs - 1;
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(eventTime, NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_START, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_LINK_SEND: {
                // For wired messages, just forward message
                if (event.getEventMessage().isWiredMessage()) {
                    NetworkDiscreteEvent wiredEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_START, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(wiredEvent);
                    return;
                }

                // Robust Header Compression (ROHC) for upper layers - compression
                int netCompressed = event.getEventMessage().getNetAdditionalBits() / TASK_LINK_BUFFERED_ROHC_COMPRESSION_NET;
                int transportCompressed = event.getEventMessage().getTransportAdditionalBits() / TASK_LINK_BUFFERED_ROHC_COMPRESSION_TRANSPORT;
                int removeBits = (event.getEventMessage().getNetAdditionalBits() - netCompressed) + (event.getEventMessage().getTransportAdditionalBits() - transportCompressed);
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() - removeBits);
                event.getEventMessage().setNetAdditionalBits(netCompressed);
                event.getEventMessage().setTransportAdditionalBits(transportCompressed);

                event.getEventMessage().setMacSender("");
                event.getEventMessage().setMacSequenceNumber(NetworkUtils.getRandomPositiveNumberBits(16));
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 48);
                event.getEventMessage().setLinkAdditionalBits(48);

                // Put message in queue and check for channel status
                messageQueue.add(0, event.getEventMessage());
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            case NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS: {
                // Do not update if forwarding to PHY is in progress
                if (forwardToPhyDoneTimeNs != 0L && NetworkUtils.simTimeWithDelay(0L) <= forwardToPhyDoneTimeNs) {
                    return;
                }

                forwardToPhyDoneTimeNs = 0L;

                // Do not update if node is sending a message, then this task will be notified when transmission finished
                if (!networkNode.getSendingChannelsMap().isEmpty()) {
                    return;
                }

                // Update stored busy channels for this task based on information set in PHY layer
                busyChannels.clear();
                Set<Integer> currentBusyChannelIDs = Collections.synchronizedSet(new HashSet<>());
                currentBusyChannelIDs.addAll(networkNode.getReceiveChannelsMap().keySet());
                currentBusyChannelIDs.addAll(networkNode.getSendingChannelsMap().keySet());
                busyChannels.addAll(currentBusyChannelIDs);

                // Check if any sending channel is currently busy
                List<Integer> sendingChannelIDs = NetworkSimulator.getInstance().getNetworkSettings().getNetworkChannelModel().computeChannelIDs(networkNode, true);
                List<Integer> remainingChannelIDs = new LinkedList<>(sendingChannelIDs);
                remainingChannelIDs.retainAll(busyChannels);
                boolean sendingChannelsBusy = !remainingChannelIDs.isEmpty();

                // When there is a message to send and sending channels are idle, immediately forward it to PHY
                if (!messageQueue.isEmpty() && !sendingChannelsBusy) {
                    NetworkMessage message = messageQueue.get(messageQueue.size() - 1);
                    messageQueue.remove(messageQueue.size() - 1);
                    forwardToPhyDoneTimeNs = NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()) + 1;
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_FORWARD_TO_PHY, networkNode, message);
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                }

                return;
            }
            default:
                return;
        }
    }
}
