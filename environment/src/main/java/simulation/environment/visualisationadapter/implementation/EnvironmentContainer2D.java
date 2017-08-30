package simulation.environment.visualisationadapter.implementation;

import javafx.geometry.Point3D;
import simulation.environment.visualisationadapter.interfaces.*;

import java.util.Collection;

/**
 * Created by lukas on 12.01.17.
 * A container which contains all objects in the environment
 */

public class EnvironmentContainer2D implements VisualisationEnvironmentContainer {

    private Collection<EnvStreet> streets;
    private Collection<Building> buildings;
    private Collection<EnvNode> trees;
    private EnvBounds bounds = null;

    private Point3D midPoint;
    private double[][] heightMap;


    public EnvironmentContainer2D(EnvBounds bounds, Collection<EnvStreet> streets, Collection<Building> buildings) {
        this.bounds = bounds;
        this.streets = streets;
        this.buildings = buildings;

    }


    @Override
    public Collection<EnvStreet> getStreets() {
        return this.streets;
    }

    @Override
    public Collection<Building> getBuildings() {
        return this.buildings;
    }

    @Override
    public Collection<EnvNode> getTrees() {
        return trees;
    }

    public void setTrees(Collection<EnvNode> trees) {
        this.trees = trees;
    }

    @Override
    public EnvBounds getBounds() {
        return this.bounds;
    }

    @Override
    public Point3D getMidpoint() {
        return this.midPoint;
    }

    @Override
    public double[][] getHeightMap() {
        return this.heightMap;
    }

    public void setHeightMap(double[][] heightMap) {
        this.heightMap = heightMap;
        //calculate midpoint
        Point3D maxPoint = new Point3D(this.bounds.getMaxX(), this.bounds.getMaxY(), this.bounds.getMaxZ());
        Point3D minPoint = new Point3D(this.bounds.getMinX(), this.bounds.getMinY(), this.bounds.getMinY());

        this.midPoint = maxPoint.midpoint(minPoint);
    }

    public void setBounds(EnvBounds bounds) {
        this.bounds = bounds;
    }
}
