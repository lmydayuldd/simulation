package simulation.environment.osm;

import javafx.geometry.Point3D;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;

/**
 * Created by lukas on 26.01.17.
 */
public class ApproximateConverterTest extends TestCase {
    public ApproximateConverterTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ApproximateConverterTest.class);
    }

    public void testApp() throws Exception {
        ApproximateConverter converter = new ApproximateConverter(6.06132, 50.78026);
        ArrayList<Point3D> points = new ArrayList<>();

        points.add(new Point3D(6.06146,50.78059, 0));
        points.add(new Point3D(6.06132,50.78055, 0));
        points.add(new Point3D(6.06166, 50.78033,0));
        points.add(new Point3D(6.06155, 50.78026, 0));

        ArrayList<Point3D> kPoints = new ArrayList<>();

        for(Point3D p : points) {
            kPoints.add(converter.convertLongLatPoint(p));
        }

//        out.println(kPoints.get(0));
 //       out.println(kPoints.get(1));


        Point3D origin = new Point3D(converter.getMinLat(), converter.getMinLong(), 0);

        for(int i = 0; i < points.size(); i++) {
            Point3D p = points.get(i);
            Point3D k = kPoints.get(i);
 //           out.println(k);
 //           out.println(p);
 //           out.println(Math.acos(Math.sin(converter.getMinLat()) * Math.sin(p.getX()) + Math.cos(converter.getMinLat()) * Math.cos(p.getX()) * Math.cos(p.getY() - converter.getMinLong())));
            double dist = 6378.388 * Math.acos(Math.sin(converter.getMinLat()) * Math.sin(p.getX()) + Math.cos(converter.getMinLat()) * Math.cos(p.getX()) * Math.cos(p.getY() - converter.getMinLong()));
            //assertEquals(dist, new Point3D(0, 0, 0).distance(k));

        }

        //distance ~90.69 m in Aachen
        Point3D point1 = new Point3D(6.058674, 50.776730, 0);
        Point3D point2 = new Point3D(6.058921, 50.777524, 0);

        System.out.println(point1.distance(point2));

        converter = new ApproximateConverter(point1.getX(), point1.getY());
        Point3D k1 = converter.convertLongLatPoint(point1);
        Point3D k2 = converter.convertLongLatPoint(point2);

        assertEquals(90.69, k2.distance(k1), 1.2);

        //distance Cologne to Aachen is about 63.95
        Point3D point3 = new Point3D(6.084934, 50.772430, 0);
        Point3D point4 = new Point3D(6.958025, 50.936766, 0);


        converter = new ApproximateConverter(point3.getX(), point3.getY());
        Point3D k3 = converter.convertLongLatPoint(point3);
        Point3D k4 = converter.convertLongLatPoint(point4);

        //larger error for larger distances
        assertEquals(63950, k3.distance(k4), 70);
    }
}
