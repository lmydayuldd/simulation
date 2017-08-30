package simulation.environment.osm;

import javafx.geometry.Point3D;

/**
 * Created by lukas on 24.01.17.
 * Specifies the Methods of a Converter from longitude/latitude to metric x and y
 */
public interface MetricConverter {

    /**
     *
     * @param longitude
     * @param latitude
     * @return the metric value according to the given longitude in dependence of the latitude
     */
    public abstract double convertLongToMeters(double longitude, double latitude);

    /**
     * @param latitude
     * @return the metric value according to the given latitude
     */
    public abstract  double convertLatToMeters(double latitude);

    /**
     * @param p
     * @return a point with the according metric values
     */
    public abstract Point3D convertLongLatPoint(Point3D p);
}
