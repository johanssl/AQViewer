/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.geoGrid;

import java.io.Serializable;
import org.fmi.aq.essentials.geoGrid.Boundaries;

/**
 * The purpose of this class is to be able to express a geographic area as a grid
 * with chosen resolution. It also helps with indexing and transformations
 * of (h,w) to-from (lat,lon).
 * 
 * @author Lasse Johansson
 */
public class AreaNfo implements Serializable{

     private static final long serialVersionUID = 7226472294623476147L;//Important: change this IF changes are made that breaks serialization!
     private final static double EPS = 0.00000001;
      public final static double DEGREE_IN_METERS = 2 * Math.PI * 6371000 / 360;
      
    public static int getH(Boundaries b, double lat, double dlat) {
        return (int)((b.latmax - lat) / dlat)+1;
    }

    public static double getLon(Boundaries b, int w, double dlon) {
        return b.lonmin + w * dlon;
    }

    public static int getW(Boundaries b, double lon, double dlon) {
        return (int) Math.round((lon - b.lonmin) / dlon);
    }

    public static double getLat(Boundaries b, int h, double dlat) {
        return b.latmax - h * dlat + EPS;
    }
    
    public final Dtime dt;
    public final int H, W;
    public final Boundaries bounds;
    public final double latrange;
    public final double lonrange;

    //derivitatives
    public final float res_m;
    public final float cellA_m2;
    public final double dlat, dlon;
    public final float coslat;
    public String name = "";

    public AreaNfo(Boundaries b, float res_m, Dtime dt) {
        this.bounds = b.clone();
        this.dt = dt.clone();
        //this.H =H;
        //this.W =W;
        //=========================
        int testRes = 1000;
        int[] HW = bounds.getMaxHWFromRes(testRes);
        double heightTotal_m = (bounds.latmax - bounds.latmin) * DEGREE_IN_METERS;
        float cellRes_m = (float) (heightTotal_m / HW[0]);//with the tester resolution we get some pixel resolution. E.g., res_m is 500m and with the tester we get 200m

        float scaler = cellRes_m / res_m;//scaler is e.g., 200m/500m = 0.4.
        int newRes = (int) (testRes * scaler);//e.g., 1000*0.4 = 400

        HW = bounds.getMaxHWFromRes(newRes);
        H = HW[0];
        W = HW[1];

        //=========================
        latrange = b.latmax - b.latmin;
        this.dlat = latrange / H;
        
        lonrange = b.lonmax - b.lonmin;
        this.dlon = lonrange / W;

        this.cellA_m2 = b.getCellArea_km2(H, W) * 1000000;
        this.res_m = (float) (dlat * DEGREE_IN_METERS);
        this.coslat = (float) Math.cos(bounds.getMidLat() * Math.PI / 180.0);
    }

    public AreaNfo(Boundaries b, int H, int W, Dtime dt) {
        this.bounds = b.clone();
        this.dt = dt.clone();
        this.H = H;
        this.W = W;

        latrange = b.latmax - b.latmin;
        this.dlat = latrange / H;
        lonrange = b.lonmax - b.lonmin;
        this.dlon = lonrange / W;
        
        this.cellA_m2 = b.getCellArea_km2(H, W) * 1000000;
        this.res_m = (float) (dlat * DEGREE_IN_METERS);
        this.coslat = (float) Math.cos(bounds.getMidLat() * Math.PI / 180.0);
    }
    
    public AreaNfo(Boundaries b, int H, int W) {
        this(b,H,W,Dtime.getSystemDate());
    }

    
    public AreaNfo(Boundaries b, float resm) {
       this(b,resm,Dtime.getSystemDate());
    }


    public double midlat() {
        return this.bounds.getMidLat();
    }

    public double midlon() {
        return this.bounds.getMidLon();
    }
   
    public double getLat(int h) {
        return getLat(this.bounds,h,dlat);
    }

    public double getLon(int w) {
        return getLon(this.bounds,w,dlon);
    }

    public String shortDesc() {
        String s = "{" + this.bounds.toText_fileFriendly(null) + ", " 
                + this.dt.getStringDate_YYYY_MM_DDTHH() + ", " + H + "x" 
                + W + "/" + this.res_m + "m}";
        return s;
    }

    public int getH(double lat) {
        return getH(this.bounds,lat, this.dlat);
    }

    public int getW(double lon) {
        return getW(this.bounds,lon,this.dlon);
    }

    public String areaCheckString(boolean addSeconds) {
        String s = bounds.toText_fileFriendly(null) + "_H" + H + "_W" + W;
        if (addSeconds) {
            s += "_s" + this.dt.systemSeconds();
        }
        return s;
    }

    public boolean oobs(int h, int w) {
      if (h<0 || h> H-1) {
          return true;
      } else if (w<0 || w> W-1) {
          return true;
      }
      return false;
    }

    public double latRange_m() {
        return (bounds.latmax - bounds.latmin) * DEGREE_IN_METERS;
    }
    
     public double lonRange_m() {
        return (bounds.lonmax - bounds.lonmin) * DEGREE_IN_METERS * coslat;
    }

    public boolean isFrameCell(int h, int w) {
        if (h==0 || h==H-1) return true;
        if (w==0 || w==W-1) return true;
        return false;
    }

}
