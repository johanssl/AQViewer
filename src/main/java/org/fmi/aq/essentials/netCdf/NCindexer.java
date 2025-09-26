/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.essentials.netCdf;

import org.fmi.aq.essentials.geoGrid.Dtime;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.GeoGrid;
import org.fmi.aq.essentials.geoGrid.AreaNfo;
import ucar.ma2.Array;
import ucar.ma2.Index;

/**
 * This class assists in defining the indices for netCDF's data array get-method.
 * This get-method is surprisingly difficult to call automatically, as there can be 2,3 or 4 parameters
 * depending on the number of dimensions. The order of time and height dimensions
 * is not trivially known either.
 * 
 * The requested dataset can also be a subset, and one of the purposes of this
 * class is to assist in the indexing for subsets.
 * @author Lasse Johansson
 */
public class NCindexer {
      
    Index index;//set indices here for data fetch.
    int[] s;//a temporary array for indices. The length of this is equal to the
    //number of dimensions, order matters.
    int[] varSlots;//an array of special dimension indices. E.g., if the first
    //element == TIMEDIM, then the first dimension is for time.
    
    AreaNfo in;//overall data area (not subset)
    boolean usable =true;
    int t_ind;//place for time dimension.
    int hgt_ind;//place for height dimension.
    
    private Array loadedData;
    //subset
    AreaNfo insub;
    
    protected NCindexer(Dtime dt, double height_m, NCreader r) {
        this(dt,height_m,r,false);
    }
          
    /**
     * Set up the indexer based on data search parameters.
     * @param dt requested data time. If null, then first time slice is used.
     * If the data does not have 'time' as a dimension simply use null.
     * @param height_m height in meters to set up the height index.
     * If height is not a valid dimension for the data, then use 0.
     * @param r the reader
     * @param dummy use 'true' for difficult netCDF data that is read 
     * using forcedSlice-method.
     */
    protected NCindexer(Dtime dt, double height_m, NCreader r, boolean dummy) {
        index = r.loadedData.getIndex();
        varSlots = r.varSlots;
        Boundaries bounds = r.getBounds();
        if (dummy) bounds = Boundaries.getDefault();//the data cannot be gridded with standard methods, but a float[][] can be extracted.
        if (bounds==null) {//if we get here, proper geo-data is requested but the data does not seem to have the needed lat,lon info.
            usable = false;
            return;
        }
        this.loadedData =r.loadedData;
        Dim lats = r.getSpecialDimension(Dim.LATDIM);
        Dim lons = r.getSpecialDimension(Dim.LONDIM);
        if (dummy && lats==null) {//happens with weird nc-data which is read with forcedSlice.
            int lati = r.dims.size()-2;
            int loni = r.dims.size()-1;
            lons = r.dims.get(loni);
            lats = r.dims.get(lati);
            this.varSlots[lati] = Dim.LATDIM;
            this.varSlots[loni] = Dim.LONDIM;    
        }
        
        int H = lats.length();
        int W = lons.length();
        //if (dummy) System.out.println("NCindexer: dummy HxW = "+ H +" x "+W);
        
        //fix time index
        t_ind =0;
        Dim time = r.getSpecialDimension(Dim.TIMEDIM);
        if (time !=null) {//the data HAS time
            t_ind = -1;//assume miss.
            if (dt ==null) {//interpretation: use first time
                dt = time.dates[0];
            } 
            //find index
            long sec= dt.systemSeconds();
            long minDiff = Long.MAX_VALUE;
            Dtime [] ds = time.dates;
            if (sec >= ds[0].systemSeconds() && sec <= ds[ds.length-1].systemSeconds()) {
                for (int k =0;k<time.length();k++) {
                    Dtime test = ds[k];
                    long diff = test.systemSeconds() - sec;
                    if (diff >=0 && diff < minDiff) {
                        t_ind =k;
                        minDiff = diff;
                    }
                }//for time
            }//if within span
        }//deal with time, time exists.
        
        hgt_ind =0;
        Dim hgt = r.getSpecialDimension(Dim.HEIGHTDIM);
        if (hgt!=null) {
            double minDiff = Double.MAX_VALUE;
            hgt_ind =hgt.values.length-1;//assume highest
            for (int k =0;k<hgt.length();k++) {
                double diff = hgt.values[k] - height_m;
                if (diff >=0 && diff < minDiff) {
                    hgt_ind =k;
                    minDiff = diff;
                }
            }
        }//deal with height, height exists.
        
         s = new int[r.varSlots.length];//for each h,w, place indices here in proper order.
         if (dt ==null) dt = Dtime.getSystemDate();
         this.in = new AreaNfo(bounds,H,W,dt);
    }


    int H() {
       return in.H;
    }

    int W() {
       return in.W;
    }
    
    /**
     * For the given h,w spatial indices, prepare the indexer to fetch data
     * from the netCDF array. This also considers the time and height that
     * have been defined via the constructor.
     * @param h lat index.
     * @param w lon index
     */
    void set(int h, int w) {
        for (int k =0;k<s.length;k++) {
            if (this.varSlots[k] == Dim.LATDIM) s[k] = h;//place lat index.
            if (this.varSlots[k] == Dim.LONDIM) s[k] = w;//place lat index.
            if (this.varSlots[k] == Dim.HEIGHTDIM) s[k] = hgt_ind;
            if (this.varSlots[k] == Dim.TIMEDIM) s[k] = t_ind;
        }//for len
    }

    float getValue() {     
        if (s.length==2) {//lat,lon case
          return loadedData.getFloat(index.set(s[0],s[1]));
          
        } else if (s.length==3) {//3 dimensional case, e.g., time-latlon.
          return loadedData.getFloat(index.set(s[0],s[1], s[2]));  
          
        } else {//4 dimensional case, e.g., time-height-latlon.
          return loadedData.getFloat(index.set(s[0],s[1], s[2], s[3])); 
          
        }
    }

    GeoGrid getEmptyGrid() { 
      AreaNfo an = this.in;
      if (this.insub!=null) an = insub;
        
       float[][] data = new float[an.H][an.W]; 
       GeoGrid g = new GeoGrid(data, an.dt, an.bounds.clone());
       return g;
    }

    void setLimitBounds(Boundaries blimit) {
       if (blimit ==null) return; 
       double resm = in.res_m;
       if (resm > 100) resm = in.res_m*0.7;//for potential indexing round-ups this is better.
       this.insub = new AreaNfo(blimit.clone(),(float)resm,in.dt);
    }

}
