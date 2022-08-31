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


public class RTreeIndex {
    private RTree<POI, Point> rTree;
    List<POI> pois;

    public RTreeIndex(List<POI> pois){
        rTree = RTree.star().minChildren(3).maxChildren(6).create();
        this.pois=pois;
        for(POI p : pois){
            rTree = rTree.add(p,Geometries.pointGeographic(p.getLon(),p.getLat()));
        }

    }

    public List<POI> queryPois(double lat, double lng, double radius){
        final Position from = Position.create(lat,lng);
        Rectangle bound = createBounds(from, radius);

        /*List<Entry<POI, Point>> res =  rTree.search(p,radius/1000).toList().toBlocking().single();
        List<POI> m =  res.stream().map(entry->entry.value()).collect(Collectors.toList());*/

        return rTree.search(bound).filter(new Func1<Entry<POI, Point>, Boolean>() {
            @Override
            public Boolean call(Entry<POI, Point> poiPointEntry) {
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
