package simulation.network;

import commons.simulation.SimulationLoopExecutable;
import org.junit.Test;
import simulation.network.settings.SettingsSimple;
import simulation.network.tasks.TaskAppMessagesSoftState;
import simulation.util.Log;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for basic message soft state cleanup functionality
 */
public class TaskAppMessagesSoftStateTest {

    @Test
    public void testSoftStateRemain() {
        // Setup
        Log.setLogEnabled(false);
        NetworkSettings settings = new SettingsSimple();

        NetworkSimulator.resetInstance();
        NetworkSimulator networkSimulator = NetworkSimulator.getInstance();
        networkSimulator.setNetworkSettings(settings);

        Map<String, List<NetworkMessage>> messagesMap = Collections.synchronizedMap(new HashMap<>());
        List<NetworkMessage> messageList1 = Collections.synchronizedList(new LinkedList<>());
        List<NetworkMessage> messageList2 = Collections.synchronizedList(new LinkedList<>());

        for (int i = 0; i < 10; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList1.add(msg);
        }

        for (int i = 0; i < 5; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList2.add(msg);
        }

        messagesMap.put("fea86fb7a631", messageList1);
        messagesMap.put("fec2efa52351", messageList2);

        // Ensure that keeping valid messages works
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 2);
        assertTrue(messagesMap.get("fea86fb7a631").size() == 10);
        assertTrue(messagesMap.get("fec2efa52351").size() == 5);
        networkSimulator.didExecuteLoop(new LinkedList<SimulationLoopExecutable>(), 0, settings.getMessageBufferMaxTime() / 1000000L);
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 2);
        assertTrue(messagesMap.get("fea86fb7a631").size() == 10);
        assertTrue(messagesMap.get("fec2efa52351").size() == 5);

        // Enable log
        Log.setLogEnabled(true);
    }

    @Test
    public void testSoftStateClear() {
        // Setup
        Log.setLogEnabled(false);
        NetworkSettings settings = new SettingsSimple();

        NetworkSimulator.resetInstance();
        NetworkSimulator networkSimulator = NetworkSimulator.getInstance();
        networkSimulator.setNetworkSettings(settings);

        Map<String, List<NetworkMessage>> messagesMap = Collections.synchronizedMap(new HashMap<>());
        List<NetworkMessage> messageList1 = Collections.synchronizedList(new LinkedList<>());
        List<NetworkMessage> messageList2 = Collections.synchronizedList(new LinkedList<>());

        for (int i = 0; i < 10; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList1.add(msg);
        }

        for (int i = 0; i < 5; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList2.add(msg);
        }

        messagesMap.put("fea86fb7a631", messageList1);
        messagesMap.put("fec2efa52351", messageList2);

        // Ensure that clearing invalid messages works
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 2);
        assertTrue(messagesMap.get("fea86fb7a631").size() == 10);
        assertTrue(messagesMap.get("fec2efa52351").size() == 5);
        networkSimulator.didExecuteLoop(new LinkedList<SimulationLoopExecutable>(), 0, 2 + (2 * settings.getMessageBufferMaxTime() / 1000000L));
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 0);

        // Enable log
        Log.setLogEnabled(true);
    }

    @Test
    public void testSoftStateMixed() {
        // Setup
        Log.setLogEnabled(false);
        NetworkSettings settings = new SettingsSimple();

        NetworkSimulator.resetInstance();
        NetworkSimulator networkSimulator = NetworkSimulator.getInstance();
        networkSimulator.setNetworkSettings(settings);

        Map<String, List<NetworkMessage>> messagesMap = Collections.synchronizedMap(new HashMap<>());
        List<NetworkMessage> messageList1 = Collections.synchronizedList(new LinkedList<>());
        List<NetworkMessage> messageList2 = Collections.synchronizedList(new LinkedList<>());

        for (int i = 0; i < 10; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList1.add(msg);
        }

        for (int i = 0; i < 5; ++i) {
            long msgReceiveTimeNs = (i * (settings.getMessageBufferMaxTime() / 10)) + 1;
            NetworkMessage msg = new NetworkMessage();
            msg.setSimReceiveTimeNs(msgReceiveTimeNs);
            messageList2.add(msg);
        }

        messagesMap.put("fea86fb7a631", messageList1);
        messagesMap.put("fec2efa52351", messageList2);

        // Ensure some messages are kept and others are removed / cleared
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 2);
        assertTrue(messagesMap.get("fea86fb7a631").size() == 10);
        assertTrue(messagesMap.get("fec2efa52351").size() == 5);
        networkSimulator.didExecuteLoop(new LinkedList<SimulationLoopExecutable>(), 0, ((5 * (settings.getMessageBufferMaxTime() / 10)) + 1 + settings.getMessageBufferMaxTime()) / 1000000L);
        TaskAppMessagesSoftState.softStateCleanup(messagesMap);
        assertTrue(messagesMap.size() == 1);
        assertTrue(messagesMap.get("fea86fb7a631").size() == 5);

        // Enable log
        Log.setLogEnabled(true);
    }
}