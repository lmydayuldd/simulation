package simulation.environment.osm;

import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.VisualisationEnvironmentContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lukas on 22.01.17.
 * Inverse mapping of IntersectionMapper
 * currently not needed
 */
public class IntersectionToStreetMapper {
    private VisualisationEnvironmentContainer container;

    private HashMap<EnvNode, ArrayList<EnvStreet>> intersectionToStreet;

    public IntersectionToStreetMapper(VisualisationEnvironmentContainer container) {
        this.container = container;
        init();
    }

    private void init() {
        Collection<EnvStreet> streets = container.getStreets();
        this.intersectionToStreet = new HashMap<>();

        for(EnvStreet street : streets) {
            for(EnvNode intersection : street.getIntersections()) {
                if(!intersectionToStreet.containsKey(intersection)) {
                    intersectionToStreet.put(intersection, new ArrayList<>());
                }

                if(!intersectionToStreet.get(intersection).contains(street)) {
                    intersectionToStreet.get(intersection).add(street);
                }
            }
        }
    }

    public List<EnvStreet> getStreetsForIntersection(EnvNode intersection) {
        if(!intersectionToStreet.containsKey(intersection)) {
            return new ArrayList<>();
        }

        return intersectionToStreet.get(intersection);
    }




}
