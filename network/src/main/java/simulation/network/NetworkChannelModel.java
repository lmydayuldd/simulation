package simulation.network;

import java.util.List;

/**
 * Abstract class that prepares the computations for the network channel objects
 * Determine if and how transmissions reach other network nodes
 */
public abstract class NetworkChannelModel implements NetworkEventHandler {

    /**
     * Function that handles network events
     *
     * @param event Network discrete event that is processed
     */
    @Override
    public void handleNetworkEvent(NetworkDiscreteEvent event) {
        // Call computation method for all network nodes except sending node
        for (NetworkNode node : NetworkSimulator.getInstance().getNetworkNodes()) {
            if (!node.equals(event.getNetworkNode())) {
                // Create individual message copies for each transmission and compute transmission
                NetworkMessage newMessage = event.getEventMessage().copy();
                computeTransmission(event.getNetworkNode(), node, newMessage);
            }
        }
    }

    /**
     * Function that computes if and how transmissions between network nodes are received
     *
     * @param sender Node that stars the transmission
     * @param otherNode All other nodes that might receive the transmission
     * @param message Message to be sent
     */
    public abstract void computeTransmission(NetworkNode sender, NetworkNode otherNode, NetworkMessage message);

    /**
     * Function that computes a list of channel IDs used by a network node, either for receiving or sending
     *
     * @param node Node for which channel IDs should be computed
     * @param sending True to get the sending channel IDs for the node, false for receiving channel IDs
     * @return List of channel IDs, either for sending or receiving
     */
    public abstract List<Integer> computeChannelIDs(NetworkNode node, boolean sending);

    /**
     * Function that computes the index for the modulation and data rate to be used
     *
     * @param node Node for which index should be computed
     * @return Integer index for the modulation and data rate info
     */
    public abstract int computeModulationAndDataRateIndex(NetworkNode node);

    /**
     * Function that is called when the network simulation starts, useful for channel objects initialization
     */
    public abstract void networkSimulationStart();
}
