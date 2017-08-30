package simulation.environment.geometry;

import javafx.geometry.Point3D;
import simulation.environment.geometry.osmadapter.GeomStreet;
import simulation.environment.geometry.osmadapter.SplineDeterminator;
import simulation.environment.geometry.splines.Spline;
import simulation.environment.visualisationadapter.implementation.Node2D;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 20.03.17.
 */
public class StreetSignPositioner {

    public static void positionStreetSigns(List<GeomStreet> streets) {
        for(GeomStreet s : streets) {
            SplineDeterminator deter = s.getDeterminator();
            ArrayList<EnvNode> nodes = new ArrayList<>(s.getObject().getNodes());
            for (int i = 0; i < nodes.size(); i++) {
                Node2D n2 = (Node2D) nodes.get(i);
                if (n2.getStreetSign().getType() != SignTypeAndState.EMPTY_SIGN) {
                    if(i != 0) {
                        Node2D n1 = (Node2D) nodes.get(i - 1);
                        Spline spline = deter.getSplineForPoints(n1.getPoint(), n2.getPoint());
                        if(n1.getPoint().distance(n2.getPoint()) >= deter.getStreet().getStreetWidth().doubleValue() * 0.5) {
                            Point3D difference = spline.getDifference().normalize();
                            Point3D rightBorder = spline.getBorder(false, false);
                            Point3D firstPosition = rightBorder.subtract(difference.multiply(0.5 * deter.getStreet().getStreetWidth().doubleValue()));
                            n2.getStreetSign().setOne(firstPosition);
                        } else {
                            Point3D rightBorder = spline.getBorder(false, true);
                            n2.getStreetSign().setOne(rightBorder);
                        }
                    }

                    if(i != nodes.size() - 1) {
                        Node2D n1 = (Node2D) nodes.get(i + 1);
                        Spline spline = deter.getSplineForPoints(n2.getPoint(), n1.getPoint());
                        if(n1.getPoint().distance(n2.getPoint()) >= deter.getStreet().getStreetWidth().doubleValue() * 0.5) {
                            Point3D difference = spline.getDifference().normalize();
                            Point3D leftBorder = spline.getBorder(true, true);
                            Point3D secondPosition = leftBorder.add(difference.multiply(0.5 * deter.getStreet().getStreetWidth().doubleValue()));
                            n2.getStreetSign().setTwo(secondPosition);
                        } else {
                            Point3D leftBorder = spline.getBorder(true, false);
                            n2.getStreetSign().setTwo(leftBorder);
                        }
                    }
                }
            }
        }

    }
}

