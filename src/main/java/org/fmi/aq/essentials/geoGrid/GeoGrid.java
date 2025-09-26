/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.geoGrid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import java.util.logging.Level;

/**
 * This widely used class is a 2D gridded dataset for a specific geographic area
 * defined by the Boundaries. Often the data presented in the grid correspond to
 * a specific time.
 * 
 * NOTE: This is a Serialized class - it is used as attributes in various objects-on-file.
 * If you are not familiar with Serialization in Java, dont touch the code!
 * 
 * @author Lasse Johansson
 */
public class GeoGrid implements Serializable {

    
    private static final long serialVersionUID = 7525472294622776147L;//Important: change this IF changes are made that breaks serialization!    
    public float[][] values;
    public final Boundaries gridBounds;// the grid
    public final int H, W;

    public final double dlat, dlon;
    public final double latRange, lonRange;
    final public Dtime dt;

    public String varName;

    public final double pi = Math.PI;
    private final static double degreeInM = 2 * Math.PI * 6371000.0 / 360.0;

    public final double dlatInMeters;
    public final double dlonInMeters;
    public final double lonScaler;
    public final double cellA_m;
    public String[] visOpsStrings = null;//can be used to automatically set visualization texts (FigureData)

    /**
     * A grid the presents gridded data values over certain geographic area and
     * time.
     *
     * @param name name of the grid,
     * @param vals values for the grid (HxW)
     * @param dt Time attribution (null should not be used: instead use
     * Dtime.getSystemDate()
     * @param bounds geoarea for data.
     */
    public GeoGrid(String name, float[][] vals, Dtime dt, Boundaries bounds) {
        this(vals, dt, bounds.clone(name));
    }
    
    public GeoGrid(AreaNfo in) {
        this(new float[in.H][in.W], in.dt.clone(), in.bounds.clone());
    }

    public boolean containsCoordinates(double lat1, double lon1) {

        if (lat1 >= this.gridBounds.latmax || lat1 <= this.gridBounds.latmin 
                || lon1 >= this.gridBounds.lonmax || lon1 <= this.gridBounds.lonmin) {
            //   console.out("point 1 is not covered. (lat, lon) " +lat1 +", "+lon1);
            return false;

        } else {
            return true;
        }
    }
    
    
    public GeoGrid hardCopy() {
       float[][] dat = new float[H][W];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
               dat[h][w] = this.values[h][w]; 
            }
        }
        return new GeoGrid(dat,this.dt.clone(),this.gridBounds.clone());
    }
    
    public void add(float val) {
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
               this.values[h][w]+=val; 
            }
        }
    }
    
    public void scale(double d) {
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
               this.values[h][w]*=d; 
            }
        }
    }

    
     public GeoGrid scaledHardCopy(float sc) {
       float[][] dat = new float[H][W];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
               dat[h][w] = this.values[h][w]*sc; 
            }
        }
        return new GeoGrid(dat,this.dt.clone(),this.gridBounds.clone());
    }


    public GeoGrid(float[][] vals, Dtime dt, Boundaries bounds) {

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
        this.cellA_m = dlatInMeters * dlonInMeters;
    }

   
    
    /**
     * Set value in the grid.In case the given index would cause OutOfBounds
     * then nothing is done.
     * @param h h index
     * @param w w index
     * @param val the set value
     * @return true if value was set. False means OutOfBounds.
     */
    public boolean setValue_oobSafe(int h, int w, float val) {
        if (h<0 || h> H-1) return false;
        if (w<0 || w> W-1) return false;
        this.values[h][w]=val;
        return true;
    }
    
     /**
     * Adds value to the grid by summing to the existing value.
     * In case the given index would cause OutOfBounds
     * then nothing is done.
     * @param h h index
     * @param w w index
     * @param val the set value
     */
    public void addToValue_oobSafe(int h, int w, float val) {
        if (h<0 ||h> H-1) return;
        if (w<0 ||w> W-1) return;
        this.values[h][w]+=val;
    }



    public double getValueAtIndex(int h, int w) {
        return this.values[h][w];
    }

    public double getValueAtIndex_oobSafe(int h, int w) {
        if (h < 0) h = 0;
        if (h >= H) h = H - 1;
        if (w < 0) w = 0;
        if (w >= W) w = W - 1;
        return this.values[h][w];
    }

    public double getValue(double lat, double lon) {
        int h = AreaNfo.getH(gridBounds, lat, dlat);
        int w = AreaNfo.getW(gridBounds, lon, dlon);
        return this.values[h][w];
    }

    public double getValue_closest(double lat, double lon) {
        int h = AreaNfo.getH(gridBounds, lat, dlat);
        int w = AreaNfo.getW(gridBounds, lon, dlon);
        if (h < 0) h = 0;
        if (h >= H) h = H - 1;
        if (w < 0) w = 0;
        if (w >= W) w = W - 1;
        return this.values[h][w];
    }

    public Float getValueAt_exSafe(double lat, double lon) {
        try {
            return (float) this.getValue(lat, lon);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public double getLatitude(int h) {
        return AreaNfo.getLat(gridBounds, h, dlat);
    }

    public double getLongitude(int w) {
        return AreaNfo.getLon(gridBounds, w, dlon);
    }

    public int getH(double lat) {
        return AreaNfo.getH(gridBounds, lat, dlat);
    }

    public int getW(double lon) {
        return AreaNfo.getW(gridBounds, lon, dlon);
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
    
    public static boolean gridOobs(float[][] dat, int h, int w) {
       if (h < 0 || h > dat.length-1) {
            return true;
        } else if (w < 0 || w > dat[0].length-1) {
            return true;
        }
        return false;
    }
    
    
    public static void gridSafeSetValue(float[][] dat, int h, int w, float f) {
       if (gridOobs(dat,h,w)) return;
       dat[h][w]=f;
    }
    
    public static void gridSafeAddValue(float[][] dat, int h, int w, float f) {
       if (gridOobs(dat,h,w)) return;
       dat[h][w]+=f;
    }

    public double[] getMinMaxAverage(int sparser) {
        double min = Double.MAX_VALUE;
        double max = Double.MAX_VALUE*-1;
        double ave = 0;
        int n =0;
         for (int h = 0; h < H; h+=sparser) {
            for (int w = 0; w < W; w+=sparser) {
                float val = this.values[h][w];
                if (val < min) min = val;
                if (val > max) max = val;
                ave+=val;
                n++;
            }
         }
         ave/=n;
         
         return new double[]{min,max,ave};
    }
    
    public ByteGeoGrid convert() {
        return new ByteGeoGrid(this.values,this.dt.clone(),this.gridBounds.clone());
    }
    
    public ByteGeoGrid convert_byteCasting() {
        byte[][] dats = new byte[H][W];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                dats[h][w] = (byte)(Math.round(values[h][w]));
            }
        }
        return new ByteGeoGrid(dats,this.dt.clone(),this.gridBounds.clone());
    }

    public boolean SafeAddValue(float lat, float lon, float add) {
       int h = this.getH(lat);
       int w = this.getW(lon);
       if (!this.oobs_hw(h, w)) {
           this.values[h][w]+=add;
           return true;
       }
       return false;
    }

    public void flipDataVertically() {
       float[][] ndat = new float[this.H][this.W];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                float val = this.values[h][w];
                ndat[H-h-1][w] =val;
            }
        }
        this.values = ndat;
    }
    
    public static GeoGrid flipY(GeoGrid g) {
        float[][] dat = new float[g.H][g.W];
        
        for (int h =0;h<g.H;h++) {
            for (int w =0;w<g.W;w++) {
                int hInv = g.H - h-1;
                dat[h][w] = g.values[hInv][w];
            }
        }
        return new GeoGrid(dat,g.dt.clone(),g.gridBounds.clone());
    }

    public float getSum() {
        float sum=0;
        for (int h =0;h<H;h++) {
            for (int w =0;w<W;w++) {
                float f = values[h][w];
               if (!Float.isNaN(f)) sum+=f;
            }
        }
        return sum;
    }
    
    public AreaNfo getNfo() {
        return new AreaNfo(gridBounds,H,W,dt);
    }

    public void applyScaleOffset(int h, int w, float[] scaleOff) {
        if (scaleOff==null) return; 
        this.values[h][w]*=scaleOff[0];
        this.values[h][w]+=scaleOff[1]; 
    }
    
    public void applyScaleOffset(float[] scaleOff) {
        if (scaleOff==null) return; 
        for (int h =0;h<H;h++) {
            for (int w =0;w<W;w++) {
                this.values[h][w]*=scaleOff[0];
                this.values[h][w]+=scaleOff[1];
            }
        }   
    }

    public HashMap<String,String> meta=null;
    public final static String META_INGESTION_UNITS = "ingestion_units";
    public final static String META_INGESTION_VAR = "ingestion_variable";
    public final static String META_SOURCE_FILE = "source_file";
    public void addMetaData(String key, String value) {
        if (meta==null) meta = new HashMap<>();
        meta.put(key,value);
    }
    
    public String getMeta(String key) {
        if (meta==null) return null;
        return meta.get(key);
    }

    public void simpleSum(GeoGrid add) {
       for (int h =0;h<H;h++) {
            for (int w =0;w<W;w++) {
              this.values[h][w]+=add.values[h][w]; 
            }
        }
    }
    
    public void addByNearestValue(GeoGrid add, float scaler) {
       for (int h =0;h<H;h++) {
           double lat = this.getLatitude(h);
            for (int w =0;w<W;w++) {
              double lon = this.getLongitude(w);
              double val = add.getValue_closest(lat, lon);
              this.values[h][w]+=(float)val*scaler; 
            }
        }
    }

 
}
