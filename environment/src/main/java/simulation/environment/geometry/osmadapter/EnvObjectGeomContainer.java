package simulation.environment.geometry.osmadapter;

import commons.simulation.PhysicalObject;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvObject;

/**
 * Created by lukas on 22.01.17.
 *
 * this is a Container for Geometric operations on Environment Objects
 */
public interface EnvObjectGeomContainer {

    /**
     *
     * @param o
     * @return Distance to the midpoint of this object from node
     */
    public abstract double getDistanceToMiddle(PhysicalObject o);

    /**
     *
     * @param n
     * @return Distance to the midpoint of this object from node
     */
    public abstract double getDistanceToMiddle(EnvNode n);


    /**
     *
     * @param o
     * @return Distance to the right border of this object from node
     */
    public abstract double getDistanceToRight(PhysicalObject o);

    /**
     *
     * @param o
     * @return Distance to the left border of this object from node
     */
    public abstract double getDistanceToLeft(PhysicalObject o);

    /**
     *
     * @param x
     * @param y
     * @param lastKnownZ
     * @return returns the according z-Coordinate for given x, y and lastKnownZ
     */
    public abstract double getGround(double x, double y, double lastKnownZ);

    /**
     *
     * @param node
     * @return true iff this object contains this node
     */
    public abstract boolean contains(EnvNode node);

    /**
     *
     * @return returns the corresponding EnvObject
     */
    public abstract EnvObject getObject();
}
