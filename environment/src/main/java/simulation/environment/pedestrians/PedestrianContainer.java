package simulation.environment.pedestrians;

import simulation.environment.geometry.osmadapter.GeomStreet;
import simulation.environment.visualisationadapter.interfaces.EnvNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by lukas on 13.02.17.
 * This class encapsulates pedestrians and maps/spawns them onto streets
 *
 */
public class PedestrianContainer {

    private static final long[] STREET_WHITELIST_OSMID = new long[] {
            // Halifax
            35855882,
            352800013,
            35856131,
            205455272,
            35856134,
            923950489,
            982622428,
            1215161067,
            205455284,
            950115158,
            1226939217,
            950118552,
            960111750,
            960111736,
            1245209060,
            35856138,
            2950029556L,
            1211965118,
            35856140,
            1603590243,
            36831054,
            35856142,
            950034512,
            205455294,
            3481600395L,
            205455308,
            380718867,
            35856146,

            // Ahornstrasse
            1245248648,
            1223037301,
            1223037296,
            1223037302,
            1223037304,
            1223391180,
            1223037298,

            // "Kleiner Weg"
            1604305237
    };

    /**
     * Constant that determines how many pedestrians will be spawned on the streets
     */
    public static final double THRESHOLD_METER_PER_PEDESTRIAN = 120;

    private List<Pedestrian> pedestrians;
    public List<Pedestrian> getPedestrians() {
        return pedestrians;
    }


    private void spawnPedestriansOnStreet(GeomStreet s) {
        List<EnvNode> listOfNodes = s.getDeterminator().getStreet().getNodes();
        double apprDistanceForStreet = 0;

        // Calculate an approximately length of the street to determine how
        // many pedestrians should be spawned on it
        // Street should always have at least two nodes; otherwise a street could be a point
        for (int i = 0; i < listOfNodes.size() - 1; i++) {
            // Always compare two adjacent nodes
            EnvNode n1 = listOfNodes.get(i);
            EnvNode n2 = listOfNodes.get(i + 1);

            apprDistanceForStreet += n1.getPoint().distance(n2.getPoint());
        }

        // Flooring prevents really short streets from adding a mass of pedestrians
        int numOfPedestrians = (int) Math.floor(apprDistanceForStreet / THRESHOLD_METER_PER_PEDESTRIAN);

        // Now the actual generation of pedestrians
        for (int i = 0; i < numOfPedestrians; i++) {
            Pedestrian ped = new Pedestrian(s);
            ped.spawnAtRandomLocation(new Random());
            // ped.setStreetParameters(s.spawnPedestrian());
            pedestrians.add(ped);
        }
    }

    /**
     * spawn x pedestrians on each street
     * @param streets the streets on which to spawn pedestrians
     */
    private void spawnPedestrians(ArrayList<GeomStreet> streets) {
        for(GeomStreet s : streets) {
            List<EnvNode> listOfNodes = s.getDeterminator().getStreet().getNodes();

            // Only one id must match and the whole street is added
            boolean isOnWhiteList = false;
            for (int i = 0; !isOnWhiteList &&  i < listOfNodes.size(); i++) {
                long osmId = listOfNodes.get(i).getOsmId();

                isOnWhiteList = Arrays.binarySearch(STREET_WHITELIST_OSMID, osmId) >= 0;
            }

            // Only if we found the street in the whitelist we spawn there
            if (isOnWhiteList) {
                spawnPedestriansOnStreet(s);
            }
        }
    }

    /**
     * XXX: This is not the final constructor api
     * @param streets streets to consider spawning pedestrians on
     */
    public PedestrianContainer(ArrayList<GeomStreet> streets) {
        pedestrians = new ArrayList<>();

        Arrays.sort(STREET_WHITELIST_OSMID);

        spawnPedestrians(streets);
    }

    /**
     * @deprecated
     */
    public PedestrianContainer(ArrayList<GeomStreet> streets, int numberOfPedestriansPerStreet) {
        this(streets);
    }

    /**
     * compute movement of all pedestrians
     * just a demonstration how it works
     */
    /*
    private void updatePedestrians() {
        for(GeomStreet s : streetToPedestrian.keySet()) {
            for(IPedestrian p : streetToPedestrian.get(s)) {
                //get last movement parameters
                PedestrianStreetParameters lastMovementParameters = p.getStreetParameters();

                //movement a pedestrians does in each time step
                double distance = (4.5/3600000);

                //compute new movement parameters
                PedestrianStreetParameters newParams = s.getMovementOfPedestrian(lastMovementParameters, distance);

                //set new movement parameters in pedestrian
                p.setStreetParameters(newParams);
            }
        }
    }
    */
}
