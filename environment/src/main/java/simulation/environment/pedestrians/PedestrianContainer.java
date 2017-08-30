package simulation.environment.pedestrians;

import simulation.environment.geometry.osmadapter.GeomStreet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 13.02.17.
 * This class encapsulates pedestrians and maps/spawns them onto streets and updates them
 * 
 */
public class PedestrianContainer {
    private List<Pedestrian> pedestrians;
    public List<Pedestrian> getPedestrians() {
        return pedestrians;
    }

    /**
     * spawn x pedestrians on each street
     * @param streets
     */
    private void spawnPedestrians(ArrayList<GeomStreet> streets, int numberOfPedestriansPerStreet) {
        for(GeomStreet s : streets) {
            ArrayList<IPedestrian> peds = new ArrayList<>();
            for(int i = 0; i < numberOfPedestriansPerStreet; i++) {
                Pedestrian ped = new Pedestrian(s);
                ped.setStreetParameters(s.spawnPedestrian());
                pedestrians.add(ped);
                peds.add(ped);
            }
        }
    }
    
    public PedestrianContainer(ArrayList<GeomStreet> streets, int numberOfPedestriansPerStreet) {
        pedestrians = new ArrayList<>();
        spawnPedestrians(streets,numberOfPedestriansPerStreet);
    }

    /**
     * compute movement of all pedestrians
     * just a demonstration how it works
     */
/*    private void updatePedestrians() {
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
    }*/



}
