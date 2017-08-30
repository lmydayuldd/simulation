package simulation.environment.geometry.osmadapter;

import javafx.geometry.Point3D;
import simulation.environment.geometry.splines.LinearInterpolator;
import simulation.environment.geometry.splines.Spline;
import simulation.environment.visualisationadapter.implementation.Bounds2D;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

import java.util.ArrayList;

/**
 * Created by lukas on 22.01.17.
 *
 * A SplineDeterminator for the usage of Linear interpolation
 */
public class LinearSplineDeterminator extends SplineDeterminator{

    public LinearSplineDeterminator(EnvStreet street) {
        super(street);
        initMaps(street);
        initBounds();
    }

    protected void initBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;

        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxZ = Double.MIN_VALUE;

        for(Spline s : splines.values()) {
            ArrayList<Point3D> borderPoints = new ArrayList<>();
            LinearInterpolator leftPavement = s.getPavement(true);
            borderPoints.addAll(leftPavement.getAllBorders());
            LinearInterpolator rightPavement = s.getPavement(false);
            borderPoints.addAll(rightPavement.getAllBorders());

            for(Point3D p : borderPoints) {
                if(p.getX() < minX) {
                    minX = p.getX();
                }

                if(p.getY() < minY) {
                    minY = p.getX();
                }

                if(p.getZ() < minZ) {
                    minZ = p.getZ();
                }


                if(p.getX() > maxX) {
                    maxX = p.getX();
                }

                if(p.getY() > maxY) {
                    maxY = p.getY();
                }

                if(p.getZ() > maxZ) {
                    maxZ = p.getZ();
                }
            }
        }

        bounds = new Bounds2D(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * @param street
     * this function constructs the splines for a given street and puts them onto the splines-map in the superclass
     */
    private void initMaps(EnvStreet street) {

        ArrayList<EnvNode> nodes = (ArrayList<EnvNode>) street.getNodes();

        for(int i = 0; i < nodes.size() - 1; i++) {
            EnvNode n1 = nodes.get(i);
            EnvNode n2 = nodes.get(i+1);

            Point3D p1 = n1.getPoint();
            Point3D p2 = n2.getPoint();

            Key k = new Key(p1, p2);
            splines.put(k, new LinearInterpolator(p1, p2, EnvStreet.STREET_WIDTH, n1.getOsmId(), n2.getOsmId(), true));
        }
    }





}
