package simulation.environment.visualisationadapter.interfaces;

/**
 * Created by lukas on 02.02.17.
 *
 * An Interface specifying the boundaries of the environment
 */
public interface EnvBounds {
    /**
     *@return the minimum x-Coordinate
     */
    public abstract double getMinX();

    /**
     *@return the minimum y-Coordinate
     */
    public abstract double getMinY();

    /**
     *@return the minimum z-Coordinate
     */
    public abstract double getMinZ();

    /**
     *@return the maximum x-Coordinate
     */
    public abstract double getMaxX();

    /**
     *@return the maximum y-Coordinate
     */
    public abstract double getMaxY();

    /**
     *@return the maximum z-Coordinate
     */
    public abstract double getMaxZ();

}
