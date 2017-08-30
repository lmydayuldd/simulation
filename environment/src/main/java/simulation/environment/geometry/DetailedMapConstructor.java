package simulation.environment.geometry;

import commons.map.IControllerNode;
import commons.map.PathListener;
import javafx.geometry.Point3D;
import simulation.environment.geometry.splines.LinearInterpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lukas on 13.02.17.
 * Constructs the detailed Path for the controller-group for a given List of OSM-Ids
 */
@Deprecated
public class DetailedMapConstructor implements PathListener {

    private HashMap<Long, Point3D> idToPoint;

    public DetailedMapConstructor(HashMap<Long, Point3D> idToPoint) {
        this.idToPoint = idToPoint;
    }

    @Override
    /**
     * @return List of IControllerNode which specifies the detailed path
     */
    public List<IControllerNode> getDetailedPath(List<Long> list) {
        List<IControllerNode> result = new ArrayList<>();

        for(int i = 0; i < list.size() - 1; i++) {
            long osmId1 = list.get(i);
            long osmId2 = list.get(i + 1);

            ArrayList<IControllerNode> tmpList = new LinearInterpolator(idToPoint.get(osmId1), idToPoint.get(osmId2), 0.d, osmId1, osmId2, false).convertToControllerList();
            if(i != 0) {
                tmpList.remove(0);
            }
            result.addAll(tmpList);

        }
        return result;
    }
}
