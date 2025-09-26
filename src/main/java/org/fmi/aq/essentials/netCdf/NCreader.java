/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.netCdf;

import java.io.File;
import org.fmi.aq.essentials.geoGrid.Dtime;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.GeoGrid;
import org.fmi.aq.enfuser.ftools.FuserTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import java.util.logging.Level;

/**
 * This class reads data from netCDF files, in most cases as GeoGrids.
 * The read methods have been tailored for data that has lat,lon dimensions
 * but can also have 'time' and 'height'.
 * 
 * The weak-point currently is 2D lat, lon dimensions with some rotation.
 * Besides that this class should be able to open and read data in a similar
 * way that Panoply can. 
 * @author Lasse Johansson
 */
public class NCreader {

    public final static String SCALER_ID = "scale_factor";
    public final static String OFFSET_ID = "add_offset";
    public final static String FILLER_ID = "FillValue";
    
    //file-specific variables 
    public ArrayList<String[]> globalAttr;// global attributes, simple text
    List<Attribute> globals;//same as above, but ucar class.
    NetcdfFile ncfile;//ucar file handle
    public List<Variable> vars;//available variabes in the file
    public ArrayList<String> varNames;//all variable names
    final String filename;
    

    //loaded variable
    public Array loadedData;
    public String loadedVar = "";
    public ArrayList<String[]> loadedVarAttr;//variable attributes. Note, value modifiers are read from here.
    NCindexer indexer;//for reading requested data from the file for various lat,lon,time and height.
    private Variable loadedVariable;//the loaded variable as ucar type
    protected ArrayList<Dim> dims;//loaded variable Dimensions
    int[] varSlots = null;//see NCindexer.class
    
    //value modifiers
    private double val_scaler = 1.0;
    private double val_offset = 0;
    public float fillValue =0.012345f;
    public float fillAndNanToThis =0;
    String scaleOff_info ="";//for debug printout methods.
    private boolean subsetHasActualValues = false;
    private Exception err = null;//recent catched exception
    boolean loadFail = false;//either file open or recent variable load failed critically.
    
    /**
     * Open the file handle.
     * @param filename local file, absolute path
     */
    public NCreader(String filename) {
        this.filename = filename;
        try {
            ncfile = NetcdfFile.open(filename);
            globals = ncfile.getGlobalAttributes();
            globalAttr = new ArrayList<>();
            for (Attribute l:globals) {
                EnfuserLogger.log(Level.FINEST,this.getClass(),
                        l.getFullName() + ", " + l.getStringValue());
                String[] temp = {l.getFullName(), l.getStringValue()};
                globalAttr.add(temp);
            }
            varNames = new ArrayList<>();
            vars = ncfile.getVariables();

            for (int i = 0; i < vars.size(); i++) {
                String varStr2 = vars.get(i).getFullName();
                varNames.add(varStr2);
            }

        } catch (IOException ioe) {
            EnfuserLogger.log(Level.WARNING,this.getClass(),
                    "NetcdfHandle: IOException encountered for file:" + filename);
            this.loadFail=true;
            this.err = ioe;
        }
    }
    /**
     * Printout info-log for global attributes and variable lists.
     * Does not inform about the loaded variable.
     * @return 
     */
    public String printoutFileContent() {
        String s="";
        EnfuserLogger.infoLog("NCreader: "+ this.filename +"\nGlobal Attrributes:");
        for (String[] globs:this.globalAttr) {
           s+=("\t"+globs[0] +" => "+ globs[1]) +"\n";
        }
        EnfuserLogger.infoLog("Variables:");
        for (String var:this.varNames) {
            s+=("\t"+var) +"\n";
        }
        EnfuserLogger.infoLog(s);
        return s;
    }
    
     /**
     * Printout info-log for the loaded variable, including attributes, dimensions
     * bounding box etc.
     * 
     * @param gridLoadTest if true, then an attempt is made to read a GeoGrid
     * and illustrate briefly the content.
     * @return 
     */
    public String printoutLoadedVar(boolean gridLoadTest) {
        String s="";
        s+=("\nLoadedVar: "+ this.loadedVar) +"\n";
        if (this.err!=null) {
            EnfuserLogger.infoLog("Contains an error: "+err.getMessage());
            err.printStackTrace();
            EnfuserLogger.infoLog(s);
            return s;
        }
        for (String[] a:this.loadedVarAttr) {
            s+=("\t"+a[0] +" => "+ a[1]) +"\n";
        }
        EnfuserLogger.infoLog("Dimensions:");
        for (Dim d:this.dims) {
            s+=("\t"+d.toString()) +"\n";
        }
        Boundaries b = this.getBounds();
        if (b==null) {
            EnfuserLogger.infoLog("Not griddable.");
            EnfuserLogger.infoLog(s);
            return s;
        }
        if (scaleOff_info.length()>0) EnfuserLogger.infoLog(this.scaleOff_info);
        s+=("Bounds:\n"+ b.toText()) +"\n";
        s+=("IndexingSlots:\n");
        for (int i:this.varSlots) s+=("\t"+ Dim.SPECIAL_NAMES[i]) +"\n";
        
        if (gridLoadTest) {
            try {
                GeoGrid g = this.getContentOnGeoGrid_2d();
                if (g!=null) {
                double[] m = g.getMinMaxAverage(1);
                s+=("Grid read test: successful, "+g.H+" x "+ g.W+ ", minMaxAve="
                        + FuserTools.editPrecision(m[0],2)+", "+FuserTools.editPrecision(m[1],2) +", "+FuserTools.editPrecision(m[2],2)
                +", tInd = "+indexer.t_ind+", hgt_ind = "+indexer.hgt_ind +", yFlip = "+ this.flipYaxis()) +"\n";
                } else {
                    s+=("Grid was NULL\n");
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        EnfuserLogger.infoLog(s);
        return s;
    }
    
    /**
     * There are two equally correct ways to have the gridded data: 1) 
     * the origin (0,0) is at the top-left corner or 2) the origin is at
     * the bottom left corner. This means that in many occasions we must
     * 'flip' the data so that the extracted information aligns with our
     *  indexing format.
     * @return true if a flip is needed.
     */
    protected boolean flipYaxis() {
        Dim d = this.getSpecialDimension(Dim.LATDIM);
        if (d==null) return false;
        return d.flipYaxis();
    }

    /**
     * Load the variable, after which data can be extracted for the loaded
     * variable.
     * @param var variable name. 
     */
    public void loadVariable(String var)  {
        if (loadedVar!=null && this.loadedVar.equals(var)) return;
        try {
        int i =-1;
        for (String vari:varNames) {
            i++;
            if (!vari.equals(var)) continue;

                    this.loadedVariable = vars.get(i);
                    this.loadedData = vars.get(i).read();
                    this.loadedVar = var;
                    List<Dimension> dimensions = this.loadedVariable.getDimensions();
                    this.dims = Dim.list(dimensions, ncfile);
                    this.varSlots = new int[dims.size()];
                    for (int k =0;k<dims.size();k++) this.varSlots[k] = dims.get(k).specialType();
                    
                    List<Attribute> list = vars.get(i).getAttributes();
                    this.loadedVarAttr = new ArrayList<>();
                    for (Attribute l : list) {
                        String atrVal = l.getStringValue();
                        if (atrVal == null || atrVal.equals("null")) {//find numeric, string is null!
                            try {
                                double d = +l.getNumericValue().doubleValue();
                                atrVal = d + "";
                            } catch (Exception ee) {
                            }
                        }
                        String[] temp = {l.getFullName(), atrVal};
                        this.loadedVarAttr.add(temp);
                    }//for variable attributes
                    
                    initScaleOffset();
                    err =null;
                    break;
        }//for find var
        } catch (Exception e) {
            err = e;
            this.loadFail = true;
        }
    }
      
    public List<Attribute> getVariableAttributes() {
        return this.loadedVariable.getAttributes();
    } 

    public String getGlobalAttribute(String key) {
        for (String[] attr : this.globalAttr) {
            if (attr[0].equals(key))  return attr[1]; 
        }
        return null;
    }
    
    public Dim getSpecialDimension(int type) {
        for (Dim d:this.dims) {
            if (d.specialType()==type) return d;
        }
        return null;
    }

    public boolean loadFailed() {
        return this.loadFail;
    }
    
    public Dtime[] getTimes() {
        for (Dim d:this.dims) {
            if (d.specialType()==Dim.TIMEDIM) {
                return d.dates;
            }
        }
        return null;
    }
    
    public float[][] getForcedSlice() {
        return getForcedSlice(null, null);
    }
    
     /**
     * This method is aimed for tricky gridded data that has some weird rotated
     * 2D x-y dimension setup.With this method one can brute force out the values,
     * also for the dimensions, and process the content further.
     * @param dim1_value if non-null then the first dimension index is fixed
     * as given. Example, a dataset has dimensions 'month','y','x' and
     * specifying the dim1 value one can extract data for different months.
     * @param dim2_value if non-null then the second dimension index is fixed
     * as given. Note that this should be done only if there's 4 dimensions.
     * @return 
     */
    public float[][] getForcedSlice(Integer dim1_value, Integer dim2_value) {
        indexer = new NCindexer(null,0, this, true);//dummy indexer. H and W are the last two dimensions.
        float[][] dat = new float[indexer.H()][indexer.W()];
        for (int h = 0; h < indexer.H(); h++) {
            for (int w = 0; w < indexer.W(); w++) {
                //place indices
                indexer.set(h,w);
                //manual indexing
                if (dim1_value !=null) indexer.s[0] = dim1_value;
                if (dim2_value !=null) indexer.s[1] = dim2_value;
                float val = indexer.getValue();
                dat[h][w] = valueCheck(val);
            }//w-lon
        }//h-lat
        return dat;
    }
     
    public GeoGrid getContentOnGeoGrid_2d() {
        return getGrid(null, 0,null);
    }
    
    GeoGrid getGrid(Dtime dt, double height_m) {
        return getGrid(dt, height_m, null);
    }

    /**
     * For the whole area of loaded variable, get data as GeoGrid.
     * @param dt request time. If this is outside of the scope of data
     * then this returns null. In case the given parameter is null the first
     * time slice is returned. In case time is irrelevant (not a dimension) then
     * use null.
     * @param height_m request height in meters. In case height is a dimension
     * then the index to match this will be evaluated. Too high value snaps into
     * the highest layer and does not return null due to the high value.
     * @param firstInd can be used to customize the search e.g., if the data
     * has some exotic first dimension.
     * @return GeoGrid of data. Can be null, if request time is not possible,
     * or something goes wrong.
     */
    public GeoGrid getGrid(Dtime dt, double height_m, Integer firstInd) {
        indexer = new NCindexer(dt,height_m, this);
        if (indexer.t_ind<0 || !indexer.usable) return null;//temporal miss.
        boolean flipy = this.flipYaxis();
        GeoGrid g = indexer.getEmptyGrid();
        
        for (int h = 0; h < indexer.H(); h++) {
            int realH = h;
            if (flipy) realH = indexer.H() -h -1;
            for (int w = 0; w < indexer.W(); w++) {
                //place indices
                indexer.set(h,w);
                if (firstInd!=null) indexer.s[0] = firstInd;
                float val = indexer.getValue();
                g.values[realH][w] = valueCheck(val);

            }//w-lon
        }//h-lat
        return g;
    }
    

    
    /**
     * Similar to getGrid method, but aimed for extracing subsets.
     * @param dt request time. If this is outside of the scope of data
     * then this returns null. In case the given parameter is null the first
     * time slice is returned. In case time is irrelevant (not a dimension) then
     * use null.
     * @param height_m request height in meters. In case height is a dimension
     * then the index to match this will be evaluated. Too high value snaps into
     * the highest layer and does not return null due to the high value.
     * @param blimit if non-null then this defines the subset area.
     * @param scale_andAdd if non-null then returned values are modified accordingly.
     * use-case: unit conversion.
     * @return GeoGrid of data for subset area. Can be null, if request time is not possible,
     * or something goes wrong.
     */
    public GeoGrid getSubsetGrid(Dtime dt, double height_m,
                Boundaries blimit, float[] scale_andAdd) {
        indexer = new NCindexer(dt,height_m, this);
        if (indexer.t_ind<0 || !indexer.usable) return null;//temporal miss.
        indexer.setLimitBounds(blimit);
        boolean flipy = this.flipYaxis();
        GeoGrid g = indexer.getEmptyGrid();
        this.subsetHasActualValues = false;
        for (int h = 0; h < g.H; h++) {//iterate over subset grid
            for (int w = 0; w<g.W; w++) {
                double lat = g.getLatitude(h);//based on lat,lon get index for netCDF data.
                double lon = g.getLongitude(w);
                int rh = indexer.in.getH(lat);
                    if (rh <0) rh =0;//avoid oobs
                    if (rh >= indexer.H()) rh = indexer.H()-1;
                    if (flipy) rh = indexer.H() - rh -1;//invert
                    
                int rw = indexer.in.getW(lon);
                    if (rw <0) rw =0;//avoid oobs
                    if (rw >= indexer.W()) rw = indexer.W()-1;
                //place indices
                indexer.set(rh,rw);
                float val = indexer.getValue();
                val = valueCheck(val);
                if (scale_andAdd != null) {//main use-case: molecular mass conversions. Therefore this releates to the unit.
                    val *= scale_andAdd[0];
                    val += scale_andAdd[1];
                }
                g.values[h][w] = val;//place the value on grid.
            }//w-lon
        }//h-lat
        if (!this.subsetHasActualValues) return null;
        return g;
    }
    
        private float valueCheck(float val) {
        if (Float.isNaN(val) || val == this.fillValue) {
            val = this.fillAndNanToThis;
        } else {
            val = (float) (val * this.val_scaler + this.val_offset);
            this.subsetHasActualValues=true;
        }
        return val;
    }
     
     public double[] BOUNDS_OFFSETS = null;   
     public ArrayList<GeoGrid> getSubsetOnGrid(double height_m, Dtime first, Dtime last,
            Boundaries limits, float[] scale_andAdd, Integer layerCountMax) {
        
         ArrayList<GeoGrid> gg = new ArrayList<>();
         Dim time = this.getSpecialDimension(Dim.TIMEDIM);
         if (time==null) return gg;
         for (Dtime dt:time.dates) {
             if (first!=null && dt.systemSeconds()< first.systemSeconds()) continue;
             if (last!=null && dt.systemSeconds()> last.systemSeconds()) continue;
             GeoGrid g = getSubsetGrid(dt, height_m,limits,scale_andAdd);
             if(g!=null) {
                 if (BOUNDS_OFFSETS!=null) {
                     double latOff = BOUNDS_OFFSETS[0]*g.dlat;
                     double lonOff = BOUNDS_OFFSETS[1]*g.dlon;
                     g.gridBounds.latmax+=latOff;
                     g.gridBounds.latmin+=latOff;
                     
                     g.gridBounds.lonmax+=lonOff;
                     g.gridBounds.lonmin+=lonOff;
                 }
                 gg.add(g);
             }
         }
         return gg;
         
     }    

    public Boundaries getBounds() {
        Dim lats = this.getSpecialDimension(Dim.LATDIM);
        Dim lons = this.getSpecialDimension(Dim.LONDIM);
        if (lats==null || lons == null) return null;
        double latmax = lats.getMaximum();
        double latmin = lats.getMinimum();

            double lonmax = lons.getMaximum();
            double lonmin = lons.getMinimum();
            
        Boundaries bounds = new Boundaries(latmin, latmax, lonmin, lonmax);
        bounds.name = this.loadedVar;
        return bounds;
    }
    
    public void close() {
        try {
            ncfile.close();
        } catch (IOException ioe) {
            EnfuserLogger.log(Level.FINEST,this.getClass(),"couldn't close netcdf-file");
            ioe.printStackTrace();
        }
    }

    private void initScaleOffset() {
        this.val_scaler = 1f;
        this.val_offset = 0;
        scaleOff_info ="";
        for (String[] attr : this.loadedVarAttr) {
                String name = attr[0];
                if (name.contains(OFFSET_ID)) {
                    this.val_offset = Double.parseDouble(attr[1]);
                    scaleOff_info +=", valueOffset USED: "+ val_offset;
                } else if (name.contains(SCALER_ID)) {
                    this.val_scaler = Double.parseDouble(attr[1]);
                    scaleOff_info +=", valueScaler USED: "+ val_scaler;
                } else if (name.contains(FILLER_ID)) {
                    this.fillValue = Double.valueOf(attr[1]).floatValue();
                    scaleOff_info +=", fillValue GIVEN: "+ fillValue;
                }
        }
    }
    
   public String findVariableAttribute(String key) {
        for (String[] attr : this.loadedVarAttr) {
                String name = attr[0];
                if (name.equals(key)) {
                   return attr[1];
                }
        }
       return null; 
    }

    public List<Attribute> getGlobalAttributes() {
        return this.globals;
    }

    public boolean hasVariable(String combinedVar) {
       for (String s:this.varNames) {
           if (s.equals(combinedVar))return true;
       }
       return false;
    }

    Variable getLoadedVariable() {
      return this.loadedVariable;
    }

    public boolean hasOffset() {
       return this.val_offset!=0;
    }

    public String getStandardVar(String var) {
       int i =-1;
        for (String vari:varNames) {
            i++;
            if (!vari.equals(var)) continue;
                List<Attribute> list = vars.get(i).getAttributes();
                for (Attribute l : list) {
                    String attrName = l.getFullName();
                    if (attrName.equals("standard_name")) return l.getStringValue();
                }//for variable attributes
        }//for find var
        return var;
    }

    public String getLoadedUnit() {
       return findVariableAttribute("units");
    }


    public static GeoGrid fetch(File ncfile,String var, Dtime dt) {
        if (!ncfile.exists()) return null;
        NCreader nc = new NCreader(ncfile.getAbsolutePath());
        if (nc.varNames==null ||!nc.hasVariable(var)) return null;
        nc.loadVariable(var);
        
        GeoGrid g;
        if (dt==null) {
            g= nc.getContentOnGeoGrid_2d();
        } else {
            g = nc.getGrid(dt, 0, null);
        }
        
        String atr = nc.findVariableAttribute("units");
        if (atr!=null) g.addMetaData("units", atr);
        nc.close();
        return g;
    }

}
