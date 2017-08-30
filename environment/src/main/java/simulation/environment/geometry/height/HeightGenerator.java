package simulation.environment.geometry.height;

/**
 * Created by lukas on 16.02.17.
 *
 * An interface specifying a height generator
 */
public interface HeightGenerator {
    /**
     * @param x
     * @param y
     * @return the z-Coordinate corresponding to x and y
     */
    public abstract double getGround(double x, double y);

    public abstract double[][] toHeightMap();
}
