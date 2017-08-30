package simulation.network.tasks;

import simulation.network.*;
import simulation.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static simulation.network.NetworkDiscreteEventId.*;

/**
 * Network task that just receives messages and forwards them to the next layer with small delay
 */
public class TaskPhySimple extends NetworkTask {

    /**
     * Constructor for this task
     *
     * @param node Node that the task is created for
     */
    public TaskPhySimple(NetworkNode node) {
        setTaskId(NetworkTaskId.NETWORK_TASK_ID_PHY_SIMPLE);
        setNetworkNode(node);
        setTaskEventIdList(Arrays.asList(NETWORK_EVENT_ID_PHY_SEND_START, NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END));
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
                // Basic transmission values
                int[][] modulationAndRateInfo = NetworkSimulator.getInstance().getNetworkSettings().getModulationAndDataRateInfo();
                int defaultModulationAndRateInfoIndex = NetworkSimulator.getInstance().getNetworkSettings().getNetworkChannelModel().computeModulationAndDataRateIndex(networkNode);

                if (modulationAndRateInfo == null || defaultModulationAndRateInfoIndex >= modulationAndRateInfo.length) {
                    Log.warning("TaskPhySimple: modulationRateInfo has invalid information, abort!");
                    return;
                }

                int dataRate = modulationAndRateInfo[defaultModulationAndRateInfoIndex][0];
                int bitsPerSignal = modulationAndRateInfo[defaultModulationAndRateInfoIndex][1];
                double codeRate = (double)(modulationAndRateInfo[defaultModulationAndRateInfoIndex][2]) / (double)(modulationAndRateInfo[defaultModulationAndRateInfoIndex][3]);
                event.getEventMessage().setPhyDataRateKBits(dataRate);
                event.getEventMessage().setPhyBitsPerSignal(bitsPerSignal);
                event.getEventMessage().setPhyCodeRate(codeRate);

                // Set information about bits
                event.getEventMessage().setPhySlowTransmissionBits(40);
                event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + 62);

                // For direct or simple communication: Add padding to the next multiple of OFDM bits per signal = Data rate (Mbit/s) * 8
                if (NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() == NetworkSettingsId.NETWORK_SETTINGS_ID_DIRECT || NetworkSimulator.getInstance().getNetworkSettings().getSettingsId() == NetworkSettingsId.NETWORK_SETTINGS_ID_SIMPLE) {
                    int ofdmSize = (dataRate / 1000) * 8;
                    int nextMultiple = (int) (Math.ceil((double) (event.getEventMessage().getMessageLengthBits()) / (double) (ofdmSize)) * ofdmSize);

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
                            Log.warning("TaskPhySimple: Padding could not be computed correctly (1)! ofdmSize: " + ofdmSize + ", length:" + event.getEventMessage().getMessageLengthBits() + ", nextMultiple: " + nextMultiple);
                        }
                    }

                    // Add padding to message
                    int padding = nextMultiple - event.getEventMessage().getMessageLengthBits();

                    if (padding >= 0 && padding < ofdmSize) {
                        event.getEventMessage().setMessageLengthBits(event.getEventMessage().getMessageLengthBits() + padding);
                        event.getEventMessage().setPhyAdditionalBits(62 + padding);
                    } else {
                        Log.warning("TaskPhySimple: Padding could not be computed correctly (2)! ofdmSize: " + ofdmSize + ", length:" + event.getEventMessage().getMessageLengthBits() + ", nextMultiple: " + nextMultiple);
                    }
                }

                // Schedule end of transmission sending
                NetworkDiscreteEvent sendingEnd = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.calcTransmissionTime(event.getEventMessage())), NetworkDiscreteEventId.NETWORK_EVENT_ID_PHY_SEND_END, event.getNetworkNode(), event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(sendingEnd);

                // Now general processing in NetworkSimulator will take over to forward message to channel objects

                return;
            }
            case NETWORK_EVENT_ID_PHY_RECEIVE_MESSAGE_END: {
                NetworkDiscreteEvent newEvent = new NetworkDiscreteEvent(NetworkUtils.simTimeWithDelay(NetworkUtils.randomNextLayerSimulationTime()), NetworkDiscreteEventId.NETWORK_EVENT_ID_LINK_RECEIVE, networkNode, event.getEventMessage());
                NetworkSimulator.getInstance().scheduleEvent(newEvent);
                return;
            }
            default:
                return;
        }
    }
}
