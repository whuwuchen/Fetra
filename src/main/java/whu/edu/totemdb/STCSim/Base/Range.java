package whu.edu.totemdb.STCSim.Base;

public class Range {
    private double[] range;
    private double latGridWidth;
    private double lonGridWidth;

    private int indexHorizontalSize;

    private int indexVerticalSize;
    public Range(double latMin,double latMax,double lonMin,double lonMax,double latGridWidth,double lonGridWidth){
        range = new double[4];
        range[0] = latMin;
        range[1] = latMax;
        range[2] = lonMin;
        range[3] = lonMax;
        this.latGridWidth = latGridWidth;
        this.lonGridWidth = lonGridWidth;
        indexHorizontalSize = (int) ((lonMax-lonMin)/lonGridWidth) + 1;
        indexVerticalSize = (int) ((latMax-latMin)/latGridWidth) + 1;
    }
    public double getlatMin(){
        return range[0];
    }

    public double getlatMax(){
        return range[1];
    }
    public double getlonMin(){
        return range[2];
    }
    public double getlonMax(){
        return range[3];
    }

    public int calculateGridId(double lat,double lon){
        return ((int)((lat-range[0])/latGridWidth))*indexHorizontalSize+(int)((lon-range[2])/lonGridWidth) +1;

    }
    public double getLatGridWidth() {
        return latGridWidth;
    }

    public double getLonGridWidth() {
        return lonGridWidth;
    }
}
