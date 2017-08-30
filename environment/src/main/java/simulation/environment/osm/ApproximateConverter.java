package simulation.environment.osm;

import javafx.geometry.Point3D;

/**
 * Created by lukas on 24.01.17.
 * An ApproxiamteConverter which converts Longitude and Latitude to Kilometric units (basically manhatten distance)
 * For details on the math see: http://stackoverflow.com/a/1253545/2451431
 */
public class ApproximateConverter implements MetricConverter {

    private double minLong;
    private double minLat;

    private final double LAT_CONSTANT = 110.574;
    private final double LONG_CONSTANT = 111.320;

    public ApproximateConverter(double minLong, double minLat) {
        this.minLong = minLong;
        this.minLat = minLat;
    }

    @Override
    public double convertLongToMeters(double longitude, double latitude) {
        return 1000*(longitude - minLong) * (LONG_CONSTANT * Math.cos(Math.toRadians(latitude)));
    }

    @Override
    public double convertLatToMeters(double lat) {
        return 1000*(lat - minLat) * LAT_CONSTANT;
    }

    @Override
    public Point3D convertLongLatPoint(Point3D p) {
        return new Point3D(convertLongToMeters(p.getX(), p.getY()), convertLatToMeters(p.getY()), p.getZ());
    }

    public double getMinLong() {
        return this.minLong;
    }

    public double getMinLat() {
        return this.minLat;
    }

}
