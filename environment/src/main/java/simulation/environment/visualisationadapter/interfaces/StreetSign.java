package simulation.environment.visualisationadapter.interfaces;

import javafx.geometry.Point3D;

/**
 * Created by lukas on 10.03.17.
 */
public interface StreetSign {

    public abstract SignTypeAndState getSignState();

    public abstract SignTypeAndState getType();

    public abstract long getId();

    public abstract boolean isOne();

    public abstract boolean isTwo();

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getZ1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract double getZ2();

    public abstract void setOne(Point3D p1);

    public abstract void setTwo(Point3D p2);

}
