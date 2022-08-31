package whu.edu.totemdb.STCSim.Index;
import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import rx.functions.Func1;
import whu.edu.totemdb.STCSim.Base.POI;
import whu.edu.totemdb.STCSim.Base.StayPoint;

import java.util.List;
import java.util.stream.Collectors;


public class LocalRTreeIndex {
    private RTree<StayPoint, Point> rTree;
    List<StayPoint> sps;

    public LocalRTreeIndex(List<StayPoint> sps){
        rTree = RTree.star().minChildren(3).maxChildren(6).create();
        this.sps=sps;
        for(StayPoint p : sps){
            rTree = rTree.add(p, Geometries.pointGeographic(p.getLon(),p.getLat()));
        }
    }



    public List<StayPoint> queryStayPoints(double lat, double lng, double radius){
        final Position from = Position.create(lat,lng);
        Rectangle bound = createBounds(from, radius);

        return rTree.search(bound)
                .filter(new Func1<Entry<StayPoint, Point>, Boolean>() {
                    @Override
                    public Boolean call(Entry<StayPoint, Point> poiPointEntry) {
                        Point p = poiPointEntry.geometry();
                        Position position = Position.create(p.y(), p.x());
                        return from.getDistanceToKm(position)<radius;
                    }
                }).toList().toBlocking().single().stream().map(entry->entry.value()).collect(Collectors.toList());

    }


    private Rectangle createBounds(final Position from, final double distanceKm) {
        // this calculates a pretty accurate bounding box. Depending on the
        // performance you require you wouldn't have to be this accurate because
        // accuracy is enforced later
        Position north = from.predict(distanceKm, 0);
        Position south = from.predict(distanceKm, 180);
        Position east = from.predict(distanceKm, 90);
        Position west = from.predict(distanceKm, 270);

        return Geometries.rectangle(west.getLon(), south.getLat(), east.getLon(), north.getLat());
    }

}

