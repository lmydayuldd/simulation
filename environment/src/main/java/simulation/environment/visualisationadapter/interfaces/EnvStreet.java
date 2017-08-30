package simulation.environment.visualisationadapter.interfaces;

import java.util.Collection;

/**
 * Created by lukas on 15.12.16.
 *
 * An interface for Streets in the Environment
 */
public interface EnvStreet extends EnvObject {
    /**
     * set STREET_WIDTH to 6 meters
     */
    public final double STREET_WIDTH = 6;

    /**
     *
     * @return the speedlimit on this street
     */
    public abstract Number getSpeedLimit();

    /**
     *
     * @return a collection of Intersections on this street
     */
    public abstract Collection<EnvNode> getIntersections();

    /**
     *
     * @return the width of this street
     */
    public abstract Number getStreetWidth();

    /**
     *
     * @return Is the street oneWay
     */
    public abstract boolean isOneWay();
}
