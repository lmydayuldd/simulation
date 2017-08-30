package simulation.environment.visualisationadapter.implementation;

import javafx.geometry.Point3D;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;
import simulation.environment.visualisationadapter.interfaces.StreetSign;

/**
 * Created by lukas on 08.01.17.
 *
 * This class represents a Node in the Environment. It implements both EnvNode and IControllerNode.
 * The latter had to be implemented due to memory issues. Thus getId() returns the osm-id, too.
 * Note: (Normally there is a difference between getId() and getOsmId())
 */
public class Node2D implements EnvNode {

    protected Point3D point;

    protected long osmId;

    protected StreetSign sign;

    public Node2D(Number longX, Number latY) {
        this.point = new Point3D(longX.doubleValue(), latY.doubleValue(), 0.d);
    }

    public Node2D(Number x, Number y, long osmId) {
        this(x, y);
        this.osmId = osmId;
    }

    public Node2D(double x, double y, double z, long osmId) {
        this.point = new Point3D(x, y, z);
        this.osmId = osmId;
    }

    public Node2D(double x, double y, double z) {
        this.point = new Point3D(x, y, z);
    }

    @Override
    public Number getX() {
        return point.getX();
    }

    @Override
    public Number getY() {
        return point.getY();
    }

    @Override
    public Number getZ() {
        return point.getZ();
    }

    public void setZ(Number z) {
        this.point = new Point3D(point.getX(), point.getY(), z.doubleValue());
    }

    public Point3D getPoint() {
        return this.point;
    }

    @Override
    public StreetSign getStreetSign() {
        if(this.sign == null) {
            this.sign = new StreetSignImpl(SignTypeAndState.EMPTY_SIGN);
        }
        return this.sign;
    }

    public void setStreetSign(StreetSign sign) {
        this.sign = sign;
    }

    @Override
    public long getId() {
        return this.osmId;
    }


    public long getOsmId() {
        return this.osmId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node2D)) return false;

        Node2D node2D = (Node2D) o;

        return point.equals(node2D.point);

    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }

    public String toString() {
        return "Osm-ID: " + this.osmId + "\n\t" + this.point.toString() ;
    }

}
