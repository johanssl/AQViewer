/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.netCdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import org.fmi.aq.essentials.geoGrid.Dtime;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * This class describes one dimension for a loaded netCDF variable.
 * In particular there are four special dimensions of interest: lat, lon, time
 * and height. For time dimension this class parses the time line for the
 * available data in terms of Dtime's.
 * 
 * @author Lasse Johansson
 */
public class Dim {

    public static HashMap<String, Dim> toHash(ArrayList<Dim> dims) {
       HashMap<String, Dim> hash = new HashMap<>();
       for (Dim d:dims) {
           hash.put(d.fullName, d);
       }
       return hash;
    }
    
    public final static int LATDIM =1;
    public final static int LONDIM =2;
    public final static int TIMEDIM =3;
    public final static int HEIGHTDIM =4;
    
    static final String[] SPECIAL_NAMES = {"none","latitude-y","longitude-x","time","height"};
    
    public boolean isTimeDim = false;
    public Dtime[] dates=null;
    public Dtime firstDt=null;
    private double dT_secs =0;

    public float[] values;
    public final String fullName;
    public final String nameLC;
    public String shortName;
    public String units;
    Variable dVar;
    protected Integer specialType =null;
    
    public Dim(Dimension dim, NetcdfFile ncfile) throws IOException {
        
        String dName = dim.getFullName();
        dVar = ncfile.findVariable(dName);
        if (dVar==null ) {
            for (Variable v:ncfile.getVariables()) {
                if (v.getFullName().contains(dName) || dName.contains(v.getFullName()) ) {
                    dVar = v;
                    EnfuserLogger.fineLog("Dim: Could not Find variable with name: "
                            + dName +", but found '"+ v.getFullName()+"'");
                    break;
                }
            }
           
        }
        Array dArray;

            dArray = dVar.read();
            values = new float[(int)dVar.getSize()];
            for (int j = 0; j < dVar.getSize(); j++) {
                float val = dArray.getFloat(j);
                values[j]=val;
            }//for dim values
            this.fullName = dim.getFullName();
            this.nameLC = this.fullName.toLowerCase();
            this.shortName =  dim.getShortName();
            this.units = dVar.getUnitsString();
      
        
       if (nameLC.startsWith("time") || nameLC.endsWith("/time")) {
                if (!IGNORE_TIME) {
                parseTimeInfo(dVar);
                this.specialType = TIMEDIM;
                }

       } else if (nameLC.startsWith("height") || nameLC.equals("z")) {
           this.specialType = HEIGHTDIM;
           
       } else if (nameLC.equals("y") || nameLC.contains("latitude") || nameLC.equals("lat")) {
           this.specialType = LATDIM;
           
       } else if (nameLC.equals("x") || nameLC.contains("longitude") || nameLC.equals("lon")) {
           this.specialType = LONDIM;
           
       }
    }
    
    public static boolean IGNORE_TIME = false;
    
    protected boolean flipYaxis() {
      if (this.specialType()!= LATDIM) return false;
      float latDiff = this.values[0] - this.values[values.length-1] ;
      return latDiff <0;
    }
    
    public int length() {
        return this.values.length;
    }
    
    private final static String[] DT_DEFS = {"second", "minute", "hour", "day"};
    private final static double[] DT_STEP = {1, 60, 3600, 24*3600};
    public static boolean HISTORIC_TIME_PARSING = false;//set this 'true' if time is e.g., 'hours since 1900-01-01'
    
    private void parseTimeInfo(Variable var) throws IOException {

        String initDate = null;
        dT_secs = 3600;//assume 1h

        List<Attribute> list = var.getAttributes();
        for (Attribute l : list) {
            String s = l.getStringValue();
            if (s == null) continue;
            String desc = l.getStringValue();
            if (!desc.contains("since")) continue;
            //parse initial date and step in seconds
            desc = desc.toLowerCase();
            initDate = l.getStringValue().split("since ")[1];
            initDate = initDate.replace("Z", "");
            
            for (int k =0;k<DT_DEFS.length;k++) {
                if (desc.contains(DT_DEFS[k])) {
                    dT_secs = DT_STEP[k];
                    break;
                }
            }
            
            if (HISTORIC_TIME_PARSING) {
                if (initDate.endsWith(".0")) initDate = initDate.replace(".0", "");
            }
        }//for attr
        
        this.firstDt = new Dtime(initDate);
        this.dates = new Dtime[this.values.length];
        for (int k =0;k<this.values.length;k++) {
            int secsAdd = (int)(dT_secs*this.values[k]);
            Dtime dt = this.firstDt.clone();
            
            if (HISTORIC_TIME_PARSING) {
                long secs = dt.systemSeconds() + (long)dT_secs*(long)this.values[k];
                dt = Dtime.UTC_fromSystemSecLong(secs);
            } else {
                dt.addSeconds(secsAdd); 
            }
             dates[k]=dt;

        }
    }
    
    
    @Override
    public String toString() {
        String s = this.fullName+", "+this.shortName 
                +", units=" + this.units 
                +", length="+values.length +" ("+values[0] +" => "+ values[values.length-1]+")";
        s+=", specialType = "+SPECIAL_NAMES[this.specialType()];
        if (this.specialType() == TIMEDIM) s+=", TimeDimension, from "+ this.dates[0].getStringDate_noTS() +", step_s="+this.dT_secs;
        
         return s;       
    }
    
    public static ArrayList<Dim> list(List<Dimension> dimensions,NetcdfFile ncfile) throws IOException {
        ArrayList<Dim> dims = new ArrayList<>();
        for (Dimension dim : dimensions) {
            dims.add(new Dim(dim,ncfile));
        }//for dims
        return dims;
    }

    int specialType() {
        if (this.specialType==null) return 0;
        return this.specialType;
    }

    double getMaximum() {
       float f = Float.MAX_VALUE*-1;
       for (float test:this.values) if (test > f) f = test;
       return f;
    }

    double getMinimum() {
       float f = Float.MAX_VALUE;
       for (float test:this.values) if (test < f) f = test;
       return f;
    }
   
    
    
}
