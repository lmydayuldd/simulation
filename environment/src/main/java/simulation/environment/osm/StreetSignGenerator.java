package simulation.environment.osm;

import simulation.environment.object.TrafficLightSwitcher;
import simulation.environment.visualisationadapter.implementation.EnvironmentContainer2D;
import simulation.environment.visualisationadapter.implementation.TrafficLight;
import simulation.environment.visualisationadapter.implementation.Node2D;
import simulation.environment.visualisationadapter.implementation.StreetSignImpl;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;

import java.util.*;

/**
 * Created by lukas on 10.03.17.
 */
public class StreetSignGenerator {
    private static Random r = new Random();

    private enum IntersectionType {
        NORMAL_INTERSECTION, TRAFFIC_LIGHT_INTERSECTION, EGG_INTERSECTION;
    }

    public static void generateStreetSigns(EnvironmentContainer2D container) {
        IntersectionToStreetMapper mapper = new IntersectionToStreetMapper(container);

        HashMap<EnvNode, IntersectionType> intersectionTypeMapper = new HashMap<>();
        HashMap<EnvNode, List<TrafficLight>> trafficSignalMapper = new HashMap<>();

        Collection<EnvStreet> streets = container.getStreets();
        for(EnvStreet street : streets) {
            List<EnvNode> nodes = street.getNodes();
            Collection<EnvNode> intersections = street.getIntersections();
            for(int i = 0; i < nodes.size(); i++) {
                Node2D node = (Node2D) nodes.get(i);

                if(intersections.contains(node)) {
                    if(!intersectionTypeMapper.containsKey(node)) {
                        intersectionTypeMapper.put(node, generateType());
                    }

                    IntersectionType type = intersectionTypeMapper.get(node);
                    if(type == IntersectionType.EGG_INTERSECTION) {
                        List<EnvStreet> intersectionStreets =  mapper.getStreetsForIntersection(node);
                        if(canBeEgg(intersectionStreets, node)) {
                            node.setStreetSign(new StreetSignImpl(SignTypeAndState.EGG_SIGN));
                        } else {
                            boolean stop = r.nextBoolean();
                            if(stop) {
                                node.setStreetSign(new StreetSignImpl(SignTypeAndState.STOP_SIGN));
                            } else {
                                node.setStreetSign(new StreetSignImpl(SignTypeAndState.PRIORITY_SIGN));
                            }
                        }
                    } else if(type == IntersectionType.NORMAL_INTERSECTION) {
                        node.setStreetSign(new StreetSignImpl(SignTypeAndState.INTERSECTION_SIGN));
                    } else {
                        if(!trafficSignalMapper.containsKey(node)) {
                            trafficSignalMapper.put(node, new ArrayList<>());
                        }
                        TrafficLight signal = new TrafficLight();
                        node.setStreetSign(signal);
                        trafficSignalMapper.get(node).add(signal);
                    }
                }
            }
        }

        for(EnvNode n : trafficSignalMapper.keySet()) {
            TrafficLightSwitcher.addSwitcher(new TrafficLightSwitcher(trafficSignalMapper.get(n)));
        }
    }

    private static IntersectionType generateType() {
        int val = r.nextInt(3);

        if(val == 0) {
            return IntersectionType.EGG_INTERSECTION;
        } else if(val == 1) {
            return IntersectionType.NORMAL_INTERSECTION;
        } else {
            return IntersectionType.TRAFFIC_LIGHT_INTERSECTION;
        }
    }

    private static boolean canBeEgg(List<EnvStreet> streets, EnvNode intersection) {
        boolean result = true;
        for(EnvStreet street : streets) {
            for(EnvNode n : street.getNodes()) {
                if(n.equals(intersection)) {
                    result &= n.getStreetSign().getType() != SignTypeAndState.EGG_SIGN;
                }

                if(!result) {
                    return result;
                }
            }
        }

        return result;
    }
}
