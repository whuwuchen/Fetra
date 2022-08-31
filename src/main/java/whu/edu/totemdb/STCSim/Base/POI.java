package whu.edu.totemdb.STCSim.Base;

import java.util.Objects;

public class POI implements BasePoint {
    private double lat;
    private double lon;
    private String name;
    private int id;
    public POI(double lat, double lon,String name,int id){
        this.lat=lat;
        this.lon=lon;
        this.name=name;
        this.id=id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        POI poi = (POI) o;
        return Double.compare(poi.lat, lat) == 0 && Double.compare(poi.lon, lon) == 0 && id == poi.id && Objects.equals(name, poi.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, name, id);
    }
}
