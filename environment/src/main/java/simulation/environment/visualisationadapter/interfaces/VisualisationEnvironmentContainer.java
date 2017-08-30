package simulation.environment.visualisationadapter.interfaces;

import javafx.geometry.Point3D;

import java.util.Collection;

/**
 * Created by lukas on 12.01.17.
 *
 * An interface for an EnvironmentContainer to be used by the Visualisation-Group only!
 */
public interface VisualisationEnvironmentContainer {
    /**
     * @return a Collection of all streets in the container
     */
    public abstract Collection<EnvStreet> getStreets();

    /**
     * @return a Collection of all buildings in the container
     */
    public abstract Collection<Building> getBuildings();

    /**
     * @return a Collection of all trees in the container
     */
    public abstract Collection<EnvNode> getTrees();

    /**
     * @return the boundaries of the container
     */
    public abstract EnvBounds getBounds();

    /**
     * @return the midpoint of this environment use for the height calculation
     */
    public abstract Point3D getMidpoint();

    /**
     * @return a two dimensional array with three rows, first row contains the circle height, second row the slope to the next circle, third row the distance to the next circle
     */
    public abstract double[][] getHeightMap();
}
