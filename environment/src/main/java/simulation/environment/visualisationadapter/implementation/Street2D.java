package simulation.environment.visualisationadapter.implementation;

import simulation.environment.visualisationadapter.interfaces.EnvIntersection;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.EnvTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lukas on 08.01.17.
 *
 * This Class represents streets
 */
public class Street2D extends EnvObject2D implements EnvStreet {

    private boolean isOneWay;
    private Number speedLimit;
    private ArrayList<EnvNode> intersections;

    public Street2D(List<EnvNode> nodes, Number speedLimit, Collection<EnvIntersection> intersections, boolean isOneWay) {
        super(nodes, EnvTag.STREET);
        init(speedLimit, intersections, isOneWay);
    }

    public Street2D(List<EnvNode> nodes, Number speedLimit, Collection<EnvIntersection> intersections, long osmId, boolean isOneWay) {
        super(nodes, EnvTag.STREET, osmId);
        init(speedLimit, intersections, isOneWay);
    }

    private void init(Number speedLimit, Collection<EnvIntersection> intersections, boolean isOneWay) {
        this.speedLimit = speedLimit;
        this.isOneWay = isOneWay;
        if (intersections == null) {
            this.intersections = new ArrayList<>();
        } else {
            this.intersections = new ArrayList<>(intersections);
        }
    }

    @Override
    public Number getSpeedLimit() {
        return speedLimit;
    }

    @Override
    public Collection<EnvNode> getIntersections() {
        return intersections;
    }

    @Override
    public Number getStreetWidth() {
        return EnvStreet.STREET_WIDTH;
    }

    @Override
    public boolean isOneWay() {
        return isOneWay;
    }

    public String toString() {
        return this.nodes.toString();
    }

}
