package simulation.network.tasks;

import simulation.network.*;
import simulation.util.Log;

import java.util.*;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that receives messages, checks for interference and on success fowards them to the next layer
 */
public class TaskPhyInterference extends NetworkTask {

    /** Set of interrupted receive channels */
    private Set<Integer> interruptedReceiveChannels = Collections.synchronizedSet(new HashSet<>());

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskPhyInterference(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_PHY_INTERFERENCE);
        setNetworkNode(node);
        this.interruptedReceiveChannels.clear();
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_PHY_SEND_START, NETWORK_EVENT_ID_PHY_SEND_END,
                NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_START, NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END,
                NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_START, NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_END,
                NETWORK_EVENT_ID_PHY_RECEIVE_INTERRUPTION_DETECTED));
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
            case NETWORK_EVENT_ID_PHY_SEND_START: {
                // For wired messages, just forward message, will be handled by channel objects after return
                if (event.getEventMessage().isWiredMessage()) {
                    return;
                }

                // Set sending information in node
                List<Integer> senderChannelIDs = NetworkSimulator.getInstance().getNetworkSettings().getNetworkChannelModel().computeChannelIDs(networkNode, true);
                Map<Integer, Integer> sendingMap = networkNode.getSendingChannelsMap();

                for (Integer channelID : senderChannelIDs) {
                    if (sendingMap.containsKey(channelID)) {
                        Integer count = sendingMap.get(channelID);
                        sendingMap.put(channelID, count + 1);
                        Log.warning("TaskPhyInterference: Already existing sending value map entry is increased, should not happen! NetworkNode: " + networkNode);
                    } else {
                        sendingMap.put(channelID, 1);
                        NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                        NetworkSimulator.getInstance().scheduleEvent(newEvent);
                    }

                    // Add interruption if we start to send on a channel on which a receive is already in progress
                    if (networkNode.getReceiveChannelsMap().containsKey(channelID)) {
                        interruptedReceiveChannels.add(channelID);
                    }
                }

                // Basic transmission values
                int[][] modulationAndRateInfo = NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfo();
                int defaultModulationAndRateInfoIndex = NetworkSimulator.getInstance().getNetworkSettings().getNetworkChannelModel().computeModulationAndDataRateIndex(networkNode);

                if (modulationAndRateInfo == null || defaultModulationAndRateInfoIndex >= modulationAndRateInfo.length) {
                    Log.warning("TaskPhyInterference: modulationRateInfo has invalid information, abort!");
                    return;
                }

                int dataRate = modulationAndRateInfo[defaultModulationAndRateInfoIndex][0];
                int bitsPerSignal = modulationAndRateInfo[defaultModulationAndRateInfoIndex][1];
                double codeRate = (double)(modulationAndRateInfo[defaultModulationAndRateInfoIndex][2]) / (double)(modulationAndRateInfo[defaultModulationAndRateInfoIndex][3]);
                event.getEventMessage().setPhyDataRateKBits(dataRate);
                event.getEventMessage().setPhyBitsPerSignal(bitsPerSignal);
                event.getEventMessage().setPhyCodeRate(codeRate);

                // For direct or simple communication: Add padding to the next multiple of OFDM bits per signal = Data rate (Mbit/s) * 8
                if (NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() == NetworkSettingsId.NETWORK_SETTINGS_ID_DIRECT || NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() == NetworkSettingsId.NETWORK_SETTINGS_ID_SIMPLE) {
                    // Set information about bits
                    event.getEventMessage().setPhySlowTransmissionBits(40);
                    event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 62);

                    int ofdmSize = (dataRate / 1000) * 8;
                    int nextMultiple = (int)(Math.ceil((double)(event.getEventMessage().getMessageLengthBits()) / (double)(ofdmSize)) * ofdmSize);

                    // Check for rounding errors
                    if (nextMultiple < event.getEventMessage().getMessageLengthBits()) {
                        nextMultiple += ofdmSize;
                    }

                    if (nextMultiple > event.getEventMessage().getMessageLengthBits() + ofdmSize) {
                        nextMultiple -= ofdmSize;
                    }

                    for (int i = -2; i <= 2; ++i) {
                        if ((nextMultiple + i) % ofdmSize == 0 && (nextMultiple + i) >= event.getEventMessage().getMessageLengthBits()) {
                            nextMultiple = (nextMultiple + i);
                            break;
                        }

                        if (i == 2) {
                            Log.warning("TaskPhyInterference: Padding could not be computed correctly (1)! ofdmSize: " + ofdmSize + ", length:" + event.getEventMessage().getMessageLengthBits() + ", nextMultiple: " + nextMultiple);
                        }
                    }

                    // Add padding to message
                    int padding = nextMultiple - event.getEventMessage().getMessageLengthBits();

                    if (padding >= 0 && padding < ofdmSize) {
                        event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + padding);
                        event.getEventMessage().setPhyAdditionalBits(62 + padding);
                    } else {
                        Log.warning("TaskPhyInterference: Padding could not be computed correctly (2)! ofdmSize: " + ofdmSize + ", length:" + event.getEventMessage().getMessageLengthBits() + ", nextMultiple: " + nextMultiple);
                    }

                    // Schedule end of transmission sending
                    NetworkDiscreteEvent sendingEnd = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.calcTransmissionTime(event.getEventMessage())), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_END, event.getNetworkNode(), event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(sendingEnd);
                }

                // Now general processing in NetworkSimulator will take over to forward message to channel objects

                return;
            }
            case NETWORK_EVENT_ID_PHY_SEND_END: {
                // Clear sending information in node
                List<Integer> senderChannelIDs = NetworkSimulator.getInstance().getNetworkSettings().getNetworkChannelModel().computeChannelIDs(networkNode, true);
                Map<Integer, Integer> sendingMap = networkNode.getSendingChannelsMap();

                for (Integer channelID : senderChannelIDs) {
                    if (!sendingMap.containsKey(channelID)) {
                        Log.warning("TaskPhyInterference: Sending clear called but channel is not in map, should not happen! NetworkNode: " + networkNode);
                    } else {
                        Integer count = sendingMap.get(channelID);

                        if (count > 1) {
                            Log.warning("TaskPhyInterference: Sending clear called with value > 1, should not happen! NetworkNode: " + networkNode);
                            sendingMap.put(channelID, count - 1);
                        } else {
                            sendingMap.remove(channelID);
                            NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                            NetworkSimulator.getInstance().scheduleEvent(newEvent);
                        }
                    }
                }

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_START: {
                // Set receiving information in node
                Map<Integer, Integer> receivingMap = networkNode.getReceiveChannelsMap();
                Integer channelID = event.getEventMessage().getPhyChannelId();

                if (receivingMap.containsKey(channelID)) {
                    Integer count = receivingMap.get(channelID);
                    receivingMap.put(channelID, count + 1);
                    interruptedReceiveChannels.add(channelID);
                } else {
                    receivingMap.put(channelID, 1);
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                }

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_INTERFERENCE_END: {
                // Clear receiving information in node
                Map<Integer, Integer> receivingMap = networkNode.getReceiveChannelsMap();
                Integer channelID = event.getEventMessage().getPhyChannelId();

                if (!receivingMap.containsKey(channelID)) {
                    Log.warning("TaskPhyInterference: Receiving interference clear called but channel is not in map, should not happen! NetworkNode: " + networkNode);
                } else {
                    Integer count = receivingMap.get(channelID);

                    if (count > 1) {
                        receivingMap.put(channelID, count - 1);
                    } else {
                        receivingMap.remove(channelID);
                        NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                        NetworkSimulator.getInstance().scheduleEvent(newEvent);
                        interruptedReceiveChannels.remove(channelID);
                    }
                }

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_START: {
                // Set receiving information in node
                Map<Integer, Integer> receivingMap = networkNode.getReceiveChannelsMap();
                Integer channelID = event.getEventMessage().getPhyChannelId();

                if (receivingMap.containsKey(channelID)) {
                    Integer count = receivingMap.get(channelID);
                    receivingMap.put(channelID, count + 1);
                    interruptedReceiveChannels.add(channelID);
                } else {
                    receivingMap.put(channelID, 1);
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                }

                // If node is already sending on that channel, receive channel is interrupted / receiving not possible
                if (networkNode.getSendingChannelsMap().containsKey(channelID)) {
                    interruptedReceiveChannels.add(channelID);
                }

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END: {
                // For wired messages, just forward message
                if (event.getEventMessage().isWiredMessage()) {
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_RECEIVE, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                    return;
                }

                // Clear receiving information in node
                Map<Integer, Integer> receivingMap = networkNode.getReceiveChannelsMap();
                Integer channelID = event.getEventMessage().getPhyChannelId();
                boolean receiveSuccess = true;

                if (!receivingMap.containsKey(channelID)) {
                    Log.warning("TaskPhyInterference: Receiving message clear called but channel is not in map, should not happen! NetworkNode: " + networkNode);
                } else {
                    Integer count = receivingMap.get(channelID);

                    if (count > 1) {
                        receivingMap.put(channelID, count - 1);
                        receiveSuccess = false;
                    } else {
                        receivingMap.remove(channelID);
                        NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_CHECK_CHANNEL_STATUS, networkNode, event.getEventMessage());
                        NetworkSimulator.getInstance().scheduleEvent(newEvent);

                        if (interruptedReceiveChannels.contains(channelID)) {
                            interruptedReceiveChannels.remove(channelID);
                            receiveSuccess = false;
                        }
                    }
                }

                if (receiveSuccess) {
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_RECEIVE, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                } else {
                    NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(0), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_RECEIVE_INTERRUPTION_DETECTED, networkNode, event.getEventMessage());
                    NetworkSimulator.getInstance().scheduleEvent(newEvent);
                }

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_INTERRUPTION_DETECTED: {

                // General processing takes over and counts transmission interruptions

                return;
            }
            default:
                return;
        }
    }
}
