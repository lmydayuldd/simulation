package simulation.environment.visualisationadapter.implementation;

import simulation.environment.visualisationadapter.interfaces.EnvIntersection;

/**
 * Created by lukas on 08.01.17.
 *
 * a container for an intersection
 */
public class Intersection2D extends Node2D implements EnvIntersection {

    public Intersection2D(Number x, Number y) {
        super(x, y);
    }

    public Intersection2D(Number x, Number y, long osmId) {
        super(x, y, osmId);
    }

    public Intersection2D(double x, double y, double z, long osmId) {super(x, y, z, osmId);}

    public Intersection2D(double x, double y, double z) {super(x,y,z); }
}
