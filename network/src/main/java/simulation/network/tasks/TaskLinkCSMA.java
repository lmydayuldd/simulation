package simulation.network.tasks;

import simulation.network.*;
import simulation.util.Log;
import simulation.util.MathHelper;

import java.util.*;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Task that performs medium access with Carrier Sense Multiple Access (Collision Avoidance)
 * Do not send when medium is busy and choose random backoff times
 */
public class TaskLinkCSMA extends NetworkTask {

    /** Distributed Inter Frame Space in nanoseconds */
    public static long TASK_LINK_CSMA_DIFS_NS = 58000L;

    /** Slot time in nanoseconds */
    public static long TASK_LINK_CSMA_SLOT_TIME_NS = 13000L;

    /** Wait refresh time, needs to be a common divisor of slot time and DIFS */
    public static long TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS = 1000L;

    /** Contention window min slot count */
    public static int TASK_LINK_CSMA_CONTENTION_WINDOW_MIN_SLOT = 15;

    /** Contention window max slot count */
    public static int TASK_LINK_CSMA_CONTENTION_WINDOW_MAX_SLOT = 1023;

    /** Contention window fraction for broadcast communication, not able to increase window based on ACKs then */
    public static double TASK_LINK_CSMA_CONTENTION_WINDOW_MAX_BROADCAST_FRACTION = 0.25;

    /** Queue of messages to be sent when medium is idle */
    private final List<NetworkMessage> messageQueue = Collections.synchronizedList(new LinkedList<>());

    /** Set of busy channel IDs */
    private Set<Integer> busyChannels = Collections.synchronizedSet(new HashSet<>());

    /** Boolean flag indicating if task is waiting for DIFS to pass */
    private boolean waitingForDifs = false;

    /** Boolean flag indicating if task waiting for DIFS was interrupted by new busy channel */
    private boolean waitingForDifsInterrupted = false;

    /** Time value for the remaining amount of DIFS time */
    private long waitingDifsRemainingTimeNs = 0L;

    /** Boolean flag indicating if task is waiting for backoff to pass */
    private boolean waitingForBackoff = false;

    /** Boolean flag indicating if task waiting for backoff was interrupted by new busy channel */
    private boolean waitingForBackoffInterrupted = false;

    /** Time value for the interruption of backoff waiting phase */
    private long waitingBackoffInterruptTimeNs = 0L;

    /** Time value for the remaining amount of backoff time */
    private long waitingBackoffRemainingTimeNs = 0L;

    /** Time value indicating when forwarding to PHY is done */
    private long forwardToPhyDoneTimeNs = 0L;

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskLinkCSMA(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_LINK_CSMA);
        setNetworkNode(node);
        this.messageQueue.clear();
        this.busyChannels.clear();
        waitingForDifs = false;
        waitingForDifsInterrupted = false;
        waitingDifsRemainingTimeNs = 0L;
        waitingForBackoff = false;
        waitingForBackoffInterrupted = false;
        waitingBackoffInterruptTimeNs = 0L;
        waitingBackoffRemainingTimeNs = 0L;
        forwardToPhyDoneTimeNs = 0L;
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_LINK_RECEIVE, NETWORK_EVENT_ID_LINK_SEND,
                NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, NETWORK_EVENT_ID_LINK_FORWARD_TO_PHY,
                NETWORK_EVENT_ID_LINK_WAIT_FOR_SENDING));
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
                // Do not receive if this node is not supposed to receive
                if (!event.getEventMessage().getMacReceiver().equals(networkNode.getMacAddress()) && !event.getEventMessage().getMacReceiver().equals(NetworkSimulator.getInstance().getNetworkSettings().getMacBroadcastAddress())) {
                    return;
                }

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
                event.getEventMessage().setMacSender(networkNode.getMacAddress());
                event.getEventMessage().setMacSequenceNumber(NetworkUtils.getRandomPositiveNumberBits(16));
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 288);
                event.getEventMessage().setLinkAdditionalBits(288);

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

                // Choose backoff timer when sending channel is busy by other node and message queue is not empty and timer is zero
                if (sendingChannelsBusy && !messageQueue.isEmpty() && waitingBackoffRemainingTimeNs == 0L) {
                    int randomWindowSize = MathHelper.randomInt(TASK_LINK_CSMA_CONTENTION_WINDOW_MIN_SLOT, (int) (TASK_LINK_CSMA_CONTENTION_WINDOW_MAX_BROADCAST_FRACTION * TASK_LINK_CSMA_CONTENTION_WINDOW_MAX_SLOT));
                    waitingBackoffRemainingTimeNs = (long)(randomWindowSize) * TASK_LINK_CSMA_SLOT_TIME_NS;
                }

                // When waiting for DIFS and channel is busy, then DIFS was interrupted
                if (sendingChannelsBusy && waitingForDifs && !waitingForBackoff && !waitingForDifsInterrupted) {
                    waitingForDifsInterrupted = true;
                }

                // When waiting for backoff and channel is busy, then backoff was interrupted
                if (sendingChannelsBusy && !waitingForDifs && waitingForBackoff && !waitingForBackoffInterrupted) {
                    waitingBackoffInterruptTimeNs = NetworkUtils.simTimeWithDelay(0);
                    waitingForBackoffInterrupted = true;
                }

                // When there is a message to send and channel is idle and not waiting for DIFS or backoff, then start waiting for DIFS
                if (!messageQueue.isEmpty() && !sendingChannelsBusy && !waitingForDifs && !waitingForBackoff) {
                    waitingForDifs = true;
                    waitingDifsRemainingTimeNs = TASK_LINK_CSMA_DIFS_NS;
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_WAIT_FOR_SENDING, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                }

                return;
            }
            case NETWORK_EVENT_ID_LINK_WAIT_FOR_SENDING: {
                // Do nothing if waiting is not enabled
                if (!waitingForDifs && !waitingForBackoff) {
                    Log.warning("TaskLinkCSMA: Wait event called without waiting enabled! Skipped. NetworkNode: " + networkNode);
                    return;
                }

                // Skip waiting if waiting for DIFS was interrupted
                // That means channel got busy in the meantime and message sending is started as soon as it is idle again
                if (waitingForDifs && waitingForDifsInterrupted) {
                    waitingForDifs = false;
                    waitingForDifsInterrupted = false;
                    return;
                }

                // Skip waiting if waiting for backoff was interrupted
                // Need to compute remaining backoff time for next attempt
                if (waitingForBackoff && waitingForBackoffInterrupted) {
                    waitingForBackoff = false;
                    waitingForBackoffInterrupted = false;
                    waitingBackoffRemainingTimeNs = Math.max(0L, waitingBackoffRemainingTimeNs - TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS - (NetworkUtils.simTimeWithDelay(0) - waitingBackoffInterruptTimeNs));
                    waitingBackoffInterruptTimeNs = 0L;
                    return;
                }

                // Handle DIFS waiting
                if (waitingForDifs && !waitingForDifsInterrupted) {
                    waitingDifsRemainingTimeNs = Math.max(0L, waitingDifsRemainingTimeNs - TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS);

                    // Waiting for DIFS done without interruptions, start waiting for backoff
                    if (waitingDifsRemainingTimeNs == 0L) {
                        waitingForDifs = false;
                        waitingForBackoff = true;
                        waitingForBackoffInterrupted = false;
                        waitingBackoffInterruptTimeNs = 0L;
                    }

                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_WAIT_FOR_SENDING, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                    return;
                }

                // Handle backoff waiting
                if (waitingForBackoff && !waitingForBackoffInterrupted) {
                    waitingBackoffRemainingTimeNs = Math.max(0L, waitingBackoffRemainingTimeNs - TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS);

                    // Waiting for backoff done without interruptions, start sending first message from queue
                    if (waitingBackoffRemainingTimeNs == 0L) {
                        waitingForBackoff = false;
                        waitingBackoffInterruptTimeNs = 0L;

                        if (!messageQueue.isEmpty()) {
                            NetworkMessage message = messageQueue.get(messageQueue.size() - 1);
                            messageQueue.remove(messageQueue.size() - 1);
                            forwardToPhyDoneTimeNs = NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()) + 1;
                            NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_FORWARD_TO_PHY, networkNode, message);
                            NetworkSimulator.getInstance().scheduleEvent(newEvent);
                        } else {
                            Log.warning("TaskLinkCSMA: No message in queue and forwarding to PHY failed! NetworkNode: " + networkNode);
                        }
                    } else {
                        NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(TASK_LINK_CSMA_WAIT_REFRESH_TIME_NS), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_WAIT_FOR_SENDING, networkNode, event.getEventMessage());
                        NetworkSimulator.getInstance().scheduleEvent(newEvent);
                    }

                    return;
                }
            }
            default:
                return;
        }
    }
}
