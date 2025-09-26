/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.netCdf;

import org.fmi.aq.essentials.geoGrid.Dtime;

import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.GeoGrid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import org.fmi.aq.essentials.geoGrid.ByteGG3D;
import org.fmi.aq.essentials.geoGrid.ByteGeoNC;
import org.fmi.aq.essentials.geoGrid.AreaNfo;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * This static class writes netCDF files, using GeoGrids, ByteGeoGrids (and other
 * gridded data formats) as input. The resulting netCDF file can be either
 * static (one time slice) or dynamic (many time slices per variable), there
 * can be many variables included. 
 * The variables may use byte-conversion to safe
 * space.
 * 
 * @author Lasse Johansson
 */
public class NCout {

    public static DataType LATLON_DTYPE = DataType.DOUBLE;
    private final String fullFileName;
    private final ArrayList<Attribute> attr;

    private Variable lat;
    private Variable lon;
    private Variable timeV;
    private NetcdfFileWriter writeableFile;

    private ArrayList<NcInfo> infoArr;
    private final ArrayList<Variable> vars;

    private int H, W;
    private int maxT;
    private int timeUnitStep;
    private AreaNfo in;
  
    //choice of input data (one of these must exist
    private ArrayList<GeoGrid> gg = null;
    private ArrayList<float[][][]> dats = null;
   
    //temporary sets (current var)
    private ByteGG3D bgg3d = null;
    private GeoGrid g = null;
    private ByteGeoNC bg=null;
    
    private float[][][] dat = null;
    private Float ADD_OFFSET;
    private Float VALUE_SCALER;

    public NCout(String fullname, ArrayList<Attribute> attr) {
        this.fullFileName = fullname;
        this.attr = attr;
        this.vars = new ArrayList<>();
    }

    public void setData_gg(ArrayList<GeoGrid> gg, ArrayList<NcInfo> infos, boolean byteCompress) {

        GeoGrid gTemp = gg.get(0);
        H = gTemp.H;
        W = gTemp.W;
        maxT = 1;
        in = new AreaNfo(gTemp.gridBounds.clone(), H, W, Dtime.getSystemDate());

        this.infoArr = infos;
        this.setCompressionMode(byteCompress);
        this.gg = gg;
    }
    
    private void setCompressionMode(boolean byteCompress) {
       if (!byteCompress) return;
       for (NcInfo inf:this.infoArr) {
           if (byteCompress && inf.compressionMode == NcInfo.COMPRESSION_NONE) {
               inf.compressionMode = NcInfo.COMPRESSION_BYTE;
           }
       }
    }
    
    public void setData_dyn(ArrayList<float[][][]> thw, Boundaries b,
            ArrayList<NcInfo> infos, boolean byteCompress) {

        this.dats = thw;
        float[][][] example = thw.get(0);

        maxT = example.length;
        H = example[0].length;
        W = example[0][0].length;
        in = new AreaNfo(b.clone(), H, W, Dtime.getSystemDate());

        this.infoArr = infos;
        this.setCompressionMode(byteCompress);

    }

    public void write() throws IOException, InvalidRangeException {

        writeableFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3,
                this.fullFileName, null);

        this.timeUnitStep = this.infoArr.get(0).timeUnitStep;

        if (attr != null) {
            for (Attribute a : attr) {
                writeableFile.addGroupAttribute(null, a);
            }        // define dimensions, including unlimited
        }

        // define dimensions, including unlimited
        Dimension timeDim = writeableFile.addUnlimitedDimension("time");
        Dimension latDim = writeableFile.addDimension(null, "latitude", H);
        Dimension lonDim = writeableFile.addDimension(null, "longitude", W);

        // define Variables
        List<Dimension> dims = new ArrayList<>();
        dims.add(timeDim);
        dims.add(latDim);
        dims.add(lonDim);

        for (NcInfo inf : this.infoArr) {
            Variable var;
            DataType dtype = DataType.FLOAT;
            if (inf.compressionMode!=NcInfo.COMPRESSION_NONE) {
                dtype = DataType.BYTE;
            }

            var = writeableFile.addVariable(null, inf.varname, dtype, dims);

            ArrayList<Attribute> atr = inf.getDefVariableAttributes();
            for (Attribute a : atr) {
                var.addAttribute(a);
            }
            this.vars.add(var);
        }//for infos, vars
       
        //lat
        List<Dimension> latdims = new ArrayList();
        latdims.add(latDim);
        lat = writeableFile.addVariable(null, "latitude", LATLON_DTYPE, latdims);
        lat.addAttribute(new Attribute("units", "degrees_north"));

        //lon
        List<Dimension> londims = new ArrayList();
        londims.add(lonDim);
        lon = writeableFile.addVariable(null, "longitude", LATLON_DTYPE, londims);
        lon.addAttribute(new Attribute("units", "degrees_east"));

        //time
        List<Dimension> tdims = new ArrayList();
        tdims.add(timeDim);
        timeV = writeableFile.addVariable(null, "time", DataType.INT, tdims);
        Attribute ta = infoArr.get(0).getTimeAttribute();
        timeV.addAttribute(ta);

        // create the file
        writeableFile.create();

        // write out the non-record variables
        double[] lats = new double[H];
        double[] lons = new double[W];
        int[] time = new int[maxT];
        for (int h = 0; h < H; h++) lats[h] = in.getLat(h);
        for (int w = 0; w < W; w++) lons[w] = in.getLon(w);
        for (int i = 0; i < maxT; i++) time[i] = i * timeUnitStep;
        
        writeableFile.write(lat, Array.factory(lats));
        writeableFile.write(lon, Array.factory(lons));
        writeableFile.write(timeV, Array.factory(time));

        //THEN THE ACTUAL DATA =========================
        //// heres where we write the record variables
        // different ways to create the data arrays. 
        // Note the outer dimension has shape 1, since we will write one record at a time
        int K = -1;
        for (Variable var : vars) {
            K++;
            boolean byteCompress = this.prepareSet(K);
            if (byteCompress) {
                writeableFile.setRedefineMode(true);
                ArrayList<Attribute> bytes = NcInfo.getVariableAttributes_byte(ADD_OFFSET, VALUE_SCALER);
                for (Attribute a : bytes) {
                    writeableFile.addVariableAttribute(var, a);
                }
                writeableFile.setRedefineMode(false);
            }

            ArrayByte.D3 data_bd3 = null;
            ArrayFloat.D3 data_fd3 = null;

            if (byteCompress) {
                data_bd3 = new ArrayByte.D3(1, latDim.getLength(), lonDim.getLength());
            } else {
                data_fd3 = new ArrayFloat.D3(1, latDim.getLength(), lonDim.getLength());
            }

            //Array timeData = Array.factory( DataType.INT, new int[] {1});
            int[] origin = new int[]{0, 0, 0}; //not sure what these origins are
            // loop over each record

            for (int t = 0; t < maxT; t++) {
                for (int h = 0; h < H; h++) {
                    for (int w = 0; w < W; w++) {

                        if (data_bd3 != null) {

                            byte byt = this.byteValue(t, h, w);
                            data_bd3.set(0, h, w, byt);

                        } else if (data_fd3 != null) {

                            float f = floatValue(t, h, w);
                            data_fd3.set(0, h, w, f);
                        }

                    }//for w
                }//for h
                // write the data out for one record
                // set the origin here
                origin[0] = t;

                if (byteCompress) {
                    writeableFile.write(var, origin, data_bd3);
                } else {
                    writeableFile.write(var, origin, data_fd3);
                }

            }//for t   

        }//for variables
        // all done
        writeableFile.close();

    }

    private boolean prepareSet(int K) {
        NcInfo inf = this.infoArr.get(K);
        //options 1: a list of GeoGrids
        if (this.gg != null) {
            if (inf.hasByteConversion()) {
                GeoGrid gTemp = gg.get(K);
                this.bg = inf.convertByteGeo(gTemp);//now bg holds the data (byteValue())
                this.ADD_OFFSET = bg.VAL_OFFSET;
                this.VALUE_SCALER = bg.VAL_SCALER;
                //special rule encoded in the dataset name:
                if (gTemp.varName!=null && gTemp.varName.contains("_rawBytes")) {
                    this.bg = ByteGeoNC.byteCasted(gTemp);
                    this.ADD_OFFSET = 0f;
                    this.VALUE_SCALER = 1f;
                }
                
            } else {
                this.g = gg.get(K);//now g holds data (floatValue())
            }
        }

        //option 2: a list of float[][][]
        if (this.dats != null) {
            if (inf.hasByteConversion()) {
                float[][][] ds = dats.get(K);
                bgg3d = new ByteGG3D(ds);//date does not matter at all
                //now bgg3d holds the data (byteValue())
                this.ADD_OFFSET = bgg3d.VAL_OFFSET;
                this.VALUE_SCALER = bgg3d.VAL_SCALER;
            } else {
                this.dat = dats.get(K);//now dat[][][] is used for floatValue()
            }
        }
        return inf.hasByteConversion();
    }

    private byte byteValue(int t, int h, int w) {
        if (bg != null) {
            return bg.values[h][w];
        } else {
            return bgg3d.getRawByte(t, h, w);
        }
    }

    int warns = 0;
    private float floatValue(int t, int h, int w) {
        //check for a possible problem
        if (g==null && dat==null) {
            warns++;
            if (warns<1) {
                EnfuserLogger.log(Level.WARNING,this.getClass(),
                  "DefaultWriter.floatValue(): datasets are null: "+ this.fullFileName);
            }
            return 0;
        }
        
        if (g != null) {
            return g.values[h][w];
        } else {
            return dat[t][h][w];
        }
    }
    
    public static void writeDynamicNetCDF(boolean byteCompress, NcInfo inf, ArrayList<GeoGrid> dats,
            ArrayList<Attribute> attr, String filename) {
        
        float[][][] fdat = new float[dats.size()][][];
        int k = -1;
        Boundaries b = null;
        for (GeoGrid g:dats) {
            k++;
            b = g.gridBounds;
            fdat[k] = g.values;
        }
        try {
            writeDynamicNetCDF(byteCompress, inf,fdat, b,attr, filename);
        } catch (IOException ex) {
            Logger.getLogger(NCout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NCout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    public static void writeDynamicNetCDF(boolean byteCompress, NcInfo nci,
            float[][][] floatData, Boundaries b,
            ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        NCout dw = new NCout(filename, attr);
        ArrayList<float[][][]> dats = new ArrayList<>();
        dats.add(floatData);
        ArrayList<NcInfo> infos = new ArrayList<>();
        infos.add(nci);

        dw.setData_dyn(dats, b, infos, byteCompress);
        dw.write();

    }

    /**
     * Stores multiple dynamic datasets in a single netCDF file. For this method
     * to function properly, all datasets MUST have the same Boundaries. Also,
     * all dataset types must have same temporal (t) and physical (b,h,w)
     * dimensions.
     *
     * @param byteCompress true for using scaled-offsetted byte values (25%
     * space usage with respect to float)
     * @param nfos a list of variable infos, one for each variable.
     * @param floatDats the variable data as float[t][h][w]
     * @param b geo definiton for all data.
     * @param attr global attributes
     * @param filename Full filename (path) for data dump (without directory,
     * should end with '.nc')
     * @throws IOException could not write netCDF-file
     * @throws InvalidRangeException dimensions were not set correctly
     */
    public static void writeMultiDynamicNetCDF(boolean byteCompress, ArrayList<NcInfo> nfos,
            ArrayList<float[][][]> floatDats, Boundaries b,
            ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        NCout dw = new NCout(filename, attr);
        dw.setData_dyn(floatDats, b, nfos, byteCompress);
        dw.write();
    }


    public static void writeDynamicNetCDF_singlevar(boolean byteCompress, String varname,
            String units, String longdesc,
            ArrayList<GeoGrid> gg, ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        ArrayList<float[][][]> floats = new ArrayList<>();
        float[][][] dat = new float[gg.size()][][];
        Boundaries b = null;
        Dtime dt1 = gg.get(0).dt;
        Dtime dt2 = gg.get(1).dt;
        ArrayList<NcInfo> infos = new ArrayList<>();
        infos.add(new NcInfo(varname, units, longdesc, dt1, dt2));
        int k = -1;
        for (GeoGrid g : gg) {
            k++;
            b = g.gridBounds;
            dat[k] = g.values;
        }
        floats.add(dat);

        NCout dw = new NCout(filename, attr);
        dw.setData_dyn(floats, b, infos, byteCompress);
        dw.write();

    }
    
        public static void writeStaticNetCDF(boolean byteCompress, NcInfo info,
            GeoGrid g, ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {
            
        ArrayList<NcInfo> infos = new ArrayList<>();
            infos.add(info);
        ArrayList<GeoGrid> gridData = new ArrayList<>();
            gridData.add(g);
               
        writeStaticNetCDFs(byteCompress, infos,gridData, attr,filename);
    }
    

    public static void writeStaticNetCDFs(boolean byteCompress, ArrayList<NcInfo> infos,
            ArrayList<GeoGrid> gridData, ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        NCout dw = new NCout(filename, attr);
        dw.setData_gg(gridData, infos, byteCompress);
        dw.write();
    }

    public static void writeMultiDynamicNetCDF(boolean byteCompress, ArrayList<NcInfo> infos,
            GeoGrid[][] gridData, ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        ArrayList<float[][][]> dats = new ArrayList<>();
        Boundaries b = null;
        for (int i = 0; i < gridData.length; i++) {
            GeoGrid[] gg = gridData[i];
            if (gg == null) continue;
            
            float[][][] dat = new float[gg.length][][];
            for (int j = 0; j < gg.length; j++) {
                dat[j] = gg[j].values;
                if (b == null) {
                    b = gg[j].gridBounds;
                }
            }
            dats.add(dat);
        }//for types 

        NCout dw = new NCout(filename, attr);
        dw.setData_dyn(dats, b, infos, byteCompress);
        dw.write();

    }

    public static void writeNetCDF_statc(boolean byteCompress, ArrayList<NcInfo> varNfo,
            ArrayList<GeoGrid> emsData,
            ArrayList<Attribute> attr, String filename)
            throws IOException, InvalidRangeException {

        NCout dw = new NCout(filename, attr);
        dw.setData_gg(emsData, varNfo, byteCompress);
        dw.write();

    }

//This method produces a netCDF-3 file which contains the geographical distribution of aggregated CO2 emissions in kg/cell form for each SHIP type
    public static void writeNetCDF_statc(boolean byteCompress, ArrayList<NcInfo> inf,
            float[][][] emsData, Boundaries b, ArrayList<Attribute> attr,
            String filename) throws IOException, InvalidRangeException {

        ArrayList<GeoGrid> gg = new ArrayList<>();
        for (float[][] dat : emsData) {
            GeoGrid g = new GeoGrid(dat, Dtime.getSystemDate(), b.clone());
            gg.add(g);
        }

        NCout dw = new NCout(filename, attr);
        dw.setData_gg(gg, inf, byteCompress);
        dw.write();

    }

}
