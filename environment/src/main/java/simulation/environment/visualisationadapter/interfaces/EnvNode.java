package simulation.environment.visualisationadapter.interfaces;

import commons.map.IControllerNode;
import javafx.geometry.Point3D;

/**
 * Created by lukas on 15.12.16.
 *
 * An Interface for EnvironmentNodes
 */
public interface EnvNode extends IControllerNode {

    /**
     *
     * @return x-Coordinate of this Node
     */
    public abstract Number getX();

    /**
     *
     * @return y-Coordinate of this Node
     */
    public abstract Number getY();

    /**
     *
     * @return z-Coordinate of this Node
     */
    public abstract Number getZ();

    /**
     *
     * @return OpenStreetMap-Id of this Node
     */
    public abstract long getOsmId();

    /**
     * @return a representation of this Node as 3D-Point
     */
    public abstract Point3D getPoint();

    /**
     *
     * @return the street sign for this node
     */
    public abstract StreetSign getStreetSign();
}
