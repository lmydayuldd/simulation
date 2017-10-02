package simulation.network.settings;

import commons.simulation.PhysicalObjectType;
import simulation.network.NetworkSettings;
import simulation.network.NetworkSettingsId;
import simulation.network.NetworkTaskId;
import simulation.network.channels.ChannelModelDirect;

import java.util.*;

import static simulation.network.NetworkTaskId.*;

/**
 * Class representing network settings for direct vehicle to vehicle communication
 */
public class SettingsDirect extends NetworkSettings {

    /**
     * Constructor that sets the values for network settings
     */
    public SettingsDirect() {
        setSettingsId(NetworkSettingsId.NETWORK_SETTINGS_ID_DIRECT);

        setIpv6Prefix("fde938777acb4bd4");
        setIpv6LinkLocalMulticastAddress("ff020000000000000000000000000001");
        setMacBroadcastAddress("ffffffffffff");
        setMacPrefix("fe");

        setSlowDataRateKBits(1000);

        setModulationAndDataRateInfo(new int[][] {
                {3000, 1, 1, 2},
                {4500, 1, 3, 4},
                {6000, 2, 1, 2},
                {9000, 2, 3, 4},
                {12000, 4, 1, 2},
                {18000, 4, 3, 4},
                {24000, 6, 2, 3},
                {27000, 6, 3, 4},
        });
        setModulationAndDataRateInfoDefaultIndex(0);

        setApplicationBeaconUpdateInterval(375000000L);

        setMinimumLocalDelayPerLayer(2000000L / 4L);
        setMaximumLocalDelayPerLayer(3000000L / 4L);

        setMinTaskStartTimeNs(0L);
        setMaxTaskStartTimeNs(3000000000L);

        setMessageBufferSize(30);
        setMessageBufferMaxTime(15000000000L);

        setNetworkChannelModel(new ChannelModelDirect());

        Map<PhysicalObjectType, List<NetworkTaskId>> networkTaskIdMap = new HashMap<>();

        List<NetworkTaskId> taskIdListCars =
            Arrays.asList(NETWORK_TASK_ID_PHY_INTERFERENCE, NETWORK_TASK_ID_LINK_CSMA, NETWORK_TASK_ID_NET_SIMPLE, NETWORK_TASK_ID_TRANSPORT_SIMPLE,
                NETWORK_TASK_ID_APP_BEACON, NETWORK_TASK_ID_APP_MESSAGES_SOFT_STATE, NETWORK_TASK_ID_APP_TRAFFIC_OPTIMIZATION, NETWORK_TASK_ID_APP_VELOCITY_CONTROL);
        networkTaskIdMap.put(PhysicalObjectType.PHYSICAL_OBJECT_TYPE_CAR_DEFAULT, new LinkedList<>(taskIdListCars));

        setNetworkTaskIdMap(networkTaskIdMap);
    }
}
