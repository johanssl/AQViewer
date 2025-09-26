/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.geoGrid;

import java.io.Serializable;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import java.util.logging.Level;

/**
 * This is similar to GeoGrid but packs the data more efficiently into Byte variables.
 * This is especially useful for 2D data in which the variability of values can
 * be expressed in 255 unique value thresholds so that the roundup errors 
 * are acceptable. Perfect example is classification data where a specific
 * integer (byte) corresponds to certain class and there are less than 255 classes.
 * 
 * For a given float[][] this class assesses an optimal value scaler and offset
 * so that the back and forth conversion  byte-to-float, float-to-byte can occur.
 * 
 * NOTE: This is a Serialized class - it is used as attributes in various objects-on-file.
 * If you are not familiar with Serialization in Java, dont touch the code!
 * 
 * @author Lasse Johansson
 */
public class ByteGeoGrid implements Serializable {

    private static final long serialVersionUID = 7526372294622776147L;//Important: change this IF changes are made that breaks serialization!
    public byte[][] values;
    public final float VAL_SCALER;
    public final float VAL_FLAT_ADD;

    public final Boundaries gridBounds;//the grid
    public final int H, W;

    public final double dlat, dlon;
    public final double latRange, lonRange;
    final public Dtime dt;

    public String varName;

    public final double pi = Math.PI;
    public final static double degreeInM = 2 * Math.PI * 6371000.0 / 360.0;

    public final double dlatInMeters;
    public final double dlonInMeters;
    public final double lonScaler;
    public final double cellA_m2;

    /**
     * A compact representation of geogridded data, using bytes.
     *
     * @param name Name of the grid
     * @param vals values for grid.
     * @param dt Time attribute
     * @param bounds Geobounds for the grid.
     */
    public ByteGeoGrid(String name, byte[][] vals, Dtime dt, Boundaries bounds) {
        this(vals, dt, bounds.clone(name));
    }

    public ByteGeoGrid(byte[][] vals, Dtime dt, Boundaries bounds) {

        this.VAL_SCALER = 1;
        this.VAL_FLAT_ADD = 0;
        this.gridBounds = bounds;

        H = vals.length;
        W = vals[0].length;
        this.values = vals;

        this.dt = dt;

        this.latRange = gridBounds.latmax - gridBounds.latmin;
        this.lonRange = gridBounds.lonmax - gridBounds.lonmin;

        this.dlat = (latRange) / (double) H;
        this.dlon = (lonRange) / (double) W;

        varName = bounds.name;
        double midlat = (gridBounds.latmax + gridBounds.latmin) / 2.0;
        lonScaler = Math.cos(midlat / 360 * 2 * pi);
        this.dlatInMeters = dlat * degreeInM;
        this.dlonInMeters = dlon * degreeInM * lonScaler;
        this.cellA_m2 = dlatInMeters * dlonInMeters;
    }

    /**
     * A compact representation of geogridded data, using bytes. Floats are
     * converted into bytes using a 250 stepped-scaling (and an offset)
     *
     * @param floats values for grid.
     * @param dt Time attribute
     * @param bounds Geobounds for the grid.
     */
    public ByteGeoGrid(float[][] floats, Dtime dt, Boundaries bounds) {

        this.gridBounds = bounds;
        H = floats.length;
        W = floats[0].length;
        this.values = new byte[H][W];

        float maxVal = Float.MIN_VALUE;
        float minVal = Float.MAX_VALUE;
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                if (Float.isNaN(floats[h][w])) continue;
                if (floats[h][w] > maxVal) maxVal = floats[h][w];
                if (floats[h][w] < minVal) minVal = floats[h][w]; 
            }
        }

        this.dt = dt;
        float range = maxVal - minVal;
        this.VAL_FLAT_ADD = minVal;
        if (range == 0) {
            this.VAL_SCALER = 0;
        } else {
            this.VAL_SCALER = 250f / range;
        }

            EnfuserLogger.log(Level.FINER,"Byte scaling is used! Scaler is " 
                    + VAL_SCALER + "\n" + "Range/flatAdd => " + range + "/" + this.VAL_FLAT_ADD);
        //setup values
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                if (Float.isNaN(floats[h][w])) {
                    values[h][w] = (byte)(-127);//mark as NaN
                    continue;
                }
                values[h][w] = (byte) ((floats[h][w] - this.VAL_FLAT_ADD) * VAL_SCALER - 125);
            }
        }

        this.latRange = gridBounds.latmax - gridBounds.latmin;
        this.lonRange = gridBounds.lonmax - gridBounds.lonmin;

        this.dlat = (latRange) / (double) H;
        this.dlon = (lonRange) / (double) W;

        varName = bounds.name;
        double midlat = (gridBounds.latmax + gridBounds.latmin) / 2.0;
        lonScaler = Math.cos(midlat / 360 * 2 * pi);
        this.dlatInMeters = dlat * degreeInM;
        this.dlonInMeters = dlon * degreeInM * lonScaler;
        this.cellA_m2 = dlatInMeters * dlonInMeters;
    }

    public boolean containsCoordinates(double lat1, double lon1) {
        if (lat1 >= this.gridBounds.latmax || lat1 <= this.gridBounds.latmin 
                || lon1 >= this.gridBounds.lonmax || lon1 <= this.gridBounds.lonmin) {
            return false;
        } else {
            return true;
        }
    }

    public GeoGrid convert() {
        float[][] nvals = new float[H][W];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                nvals[h][w] = (float) this.getValueAtIndex(h, w);
            }
        }
        EnfuserLogger.log(Level.FINER,this.getClass(),"Converting ByteGeoGrid to GeoGrid.");
        return new GeoGrid(nvals, this.dt.clone(), this.gridBounds.clone());
    }

    public double getValueAtIndex(int h, int w) {
        return scaleBack(h, w);
    }
    
    public boolean isNanAtIndex(int h, int w) {
        int temp = this.values[h][w] + 125; //reposition from -128 scale to 0 -256 scale;
        if (temp<-1) return true;
        return false;
    }

    public double nanValue=0;
    private double scaleBack(int h, int w) {
        if (VAL_SCALER == 0) {
            return this.VAL_FLAT_ADD;//singular grid
        }
        int temp = this.values[h][w] + 125; //reposition from -128 scale to 0 -256 scale;
        if (temp<-1) return nanValue;
        
        return (temp / VAL_SCALER + this.VAL_FLAT_ADD);
    }
    
    public int countNaNs() {
        int count =0;
        for (int h=0;h<H;h++) {
            for (int w =0;w<W;w++) {
                if(isNanAtIndex(h,w)) count++;
            }
        }
        return count;
    }

    public static double getValue_OOBzero(ByteGeoGrid g, double lat, double lon) {
        int h = g.getH(lat);
        int w = g.getW(lon);
        try {
            return g.scaleBack(h, w);
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    public double getValue(double lat, double lon) {
        int h = getH(lat);
        int w = getW(lon);
        return scaleBack(h, w);
    }

    public Double getValue_excSafe(double lat, double lon) {

        int h = getH(lat);
        int w = getW(lon);
        try {
            return scaleBack(h, w);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
    public double getValue_closest(double lat, double lon) {
       int h = getH(lat);
       int w = getW(lon);

        if (h < 0) h = 0;
        if (h >= H)  h = H - 1;
        if (w < 0) w = 0;
        if (w >= W) w = W - 1;
        return this.getValueAtIndex(h, w);
        
    }
    
    public double[] getMinMaxAverage(int sparser) {
        double min = Double.MAX_VALUE;
        double max = Double.MAX_VALUE*-1;
        double ave = 0;
        int n =0;
         for (int h = 0; h < H; h+=sparser) {
            for (int w = 0; w < W; w+=sparser) {
                double val = this.getValueAtIndex(h, w);
                if (val < min) min = val;
                if (val > max) max = val;
                ave+=val;
                n++;
            }
         }
         ave/=n;
         
         return new double[]{min,max,ave};
    }
        
    public int getH(double lat) {
        return AreaNfo.getH(gridBounds, lat, dlat);
    }

    public int getW(double lon) {
        return AreaNfo.getW(gridBounds, lon, dlon);
    }
    
    public double getLatitude(int h) {
        return AreaNfo.getLat(gridBounds, h, dlat);
    }

    public double getLongitude(int w) {
        return AreaNfo.getLon(gridBounds, w, dlon);
    }

    public boolean oobs_cc(double lat, double lon) {
      int h = getH(lat);
      int w = getW(lon);
      return oobs_hw(h,w);
    }
    
    public boolean oobs_hw(int h, int w) {
        if (h < 0 || h > H-1) {
            return true;
        } else if (w < 0 || w > W-1) {
            return true;
        }
        return false;
    }
    
    public float getSum() {
        float sum=0;
        for (int h =0;h<H;h++) {
            for (int w =0;w<W;w++) {
                sum+=this.getValueAtIndex(h, w);
            }
        }
        return sum;
    }
    
    public AreaNfo getNfo() {
        return new AreaNfo(gridBounds,H,W,dt);
    }

}
