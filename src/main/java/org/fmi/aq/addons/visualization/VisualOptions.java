/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;

import java.util.HashMap;
import org.fmi.aq.addons.visualization.AwtOptions;
import org.fmi.aq.addons.visualization.FigureData;

/**
 * This class is a settings object that contains the necessary parameters
 * to make visualizations and animations. 
 * 
 * @author Lasse Johansson
 */
public class VisualOptions {

   
    public final static int I_GIS_BG_SEAS=0;
    public final static int I_GIS_GB_SEA_BORDER=1;
    public static final int I_GIS_BG_LAND = 20;
    public static final int I_GIS_BG_LAKE =30; 
    public static final int I_GIS_BG_RIVER =40;
    
    public final static int I_FIGURE_BASE_COLOR=2;
    public final static int I_MASK_OTHER_COL=3;
    public final static int I_FONT_COLOR=4;
    public final static int I_SEA_SATMIX = 5;
    private static final String VOPS_FILE = "visualizationInstructions.csv";

    public static VisualOptions darkSteam() {
        return darkSteam(null, true, 1.0, false);
    }
    
    public static VisualOptions darkSteam(Object ol, boolean temp, double prog, boolean transparent_bg) {
      VisualOptions vops = new VisualOptions();
      vops.colorScheme= FigureData.COLOR_BLACKGR;
      if (temp) vops.colorScheme= FigureData.COLOR_TEMPERATURE;
      vops.transparency=FigureData.TRANSPARENCY_STEAM;
      
      vops.scaleProgression =prog;
      vops.scaleMaxCut=1;
      vops.numCols =80;
      vops.fontScaler_master =1.5;
      vops.setFontToColor(new int[]{255,255,255,255});//white font with black background.
      vops.font3D=true;
      vops.font ="Arial";
      vops.bannerStyle = VisualOptions.BANNER_NONE;
      vops.colBarStyle = VisualOptions.CBAR_TRANSP;
      vops.mapRendering_THE_DARKNESS(true, true, true);
      vops.ol = ol;
      
      int tr = 255;
      if (transparent_bg) tr =0;
      vops.customColors.put(VisualOptions.I_FIGURE_BASE_COLOR, new int[]{0,0,0,tr});
      return vops;
    }
    
    public final static int CBAR_NONE = 0;
    public final static int CBAR_BASIC = 1;
    public final static int CBAR_TRANSP = 2;
    
    public static String[] CBAR_NAMES = {
    "BANNER_NONE",
    "BASIC",
    "TRANSPARENT"
}; 
    
    public final static int BANNER_NONE = 0;
    public final static int BANNER_TRANSP = 2;
    public int bannerStyle = BANNER_NONE;
    
    public static String[] BANNER_NAMES = {
        "CBAR_OFF",
        "TRANSPARENT",
        "TRANSPARENT"
    };
         
    public final static int ENCODING_AVI = 0;
    public final static int ENCODING_H264 = 1;
    public final static int ENCODING_WMV = 2;
    
    public final static String BANNER_IMG_JUSTLOGO = "Logo.png";
    public final static String BANNER_IMG_JUSTLOGO_INV = "Logo_noText_inv.png";
    public final static String BANNER_IMG_LOGO_FMI = "Logo_FMI.png";
    public final static String BANNER_IMG_LOGO_FMI_INV = "Logo_FMI.png";

    public final static String BANNER_IMG_ENFUSER = "Logo_ENFUSER.png";
    public final static String BANNER_IMG_STEAM_PUFF = "Logo_STEAMPUFF.png";
    public final static String BANNER_IMG_STEAM = "Logo_STEAM.png";
    
    public final static String FONT_ARIAL = "Arial";
    public final static String FONT_TIMES_NR = "Times New Roman";
    public final static String FONT_TAHOMA = "Tahoma";
    public final static String FONT_HELVETICA = "Helvetica";
    public final static String FONT_SERIF = "Serif";

    public HashMap<Integer, int[]> customColors = new HashMap<>();
    public int fontStyle = 1;//font.bold

    public HashMap<Integer, int[]> mask_categorical_cols = new HashMap<>();
    public HashMap<Integer, int[]> mask_surf_cols = new HashMap<>();
    public HashMap<Integer, int[]> mask_func_cols = new HashMap<>();
    
    public int metSymbolDensity = 5; // of total width and height, e.g. amount of symbols is approximately (1/this)*(1/this)
    public float metSymbScaler =0.8f;//symbol sizing
    public boolean metSummary = true;
    
    public boolean placeMiniWindow = false;//instruction to automatically add a gridset for Animation.
    public double miniWindow_sizeScaler = 5;
    public boolean displayDensity = false;

    public boolean lowCut_transparency = false;
    public int transparency = FigureData.TRANSPARENCY_MED;
    public double bannerSizer = 0.5f;
    public int colorScheme = FigureData.COLOR_BASIC;
    public double scaleProgression = 1.0;
    public double scaleMaxCut = 1.0;

    public boolean blackBG = false;
    public float metSymb_xShift=0;

    public float satMask_saturation=1f;
    public float satMask_br=1f;
    public boolean satMask_mixing = false; // water from landuse
    public boolean satMaskPriority =false;
    public boolean shaderPostLayer =false;
    
    public double[] minMax;
    //animation options
    public int vid_interpolationFactor = 6;
    public int imgsOutModulo = 6*4;
    public int vid_frames_perS = 18;
    public int fd_minimumWidth = 800;
    public int colBarStyle = CBAR_BASIC;
    
    public int vidEncoding = ENCODING_H264;
    public double scaleValueModcaler=1;
    public int numCols = 40;
    public String customPaletteRecipee = null;
    private String customBannerFile = "";
    public double fontScaler_master = 1.0;
    
    public String font = FONT_SERIF;
    public boolean font3D = false;
    public double cbarScaler = 1.0;
    public boolean colorSmartFecth = true;
    public Double dotSize_percent = 0.013;
    
    //object carried to FigureData========
    public Object ol=null;//if true, then osmLayer can be inserted to FD automatically during FigureData.createExtended().
    public String[] text=null;
    public float TXT_UPCENTER_LOC = 0.5f;
    
    public boolean imageOutAsRunnable=false;
    public String[] fontSpecs = null;//name, scaler, B/W, boolean
    public double waterValueScaler_anim =1.0;
    public boolean slimWindVectorField =false;
    public boolean miniWindowSatelliteStyle=true;
    public int txtRotation=30;
    
    public int valueScale_precisionAdd =0;
    public boolean valueScale_precisionFloater = false;
    
    public float cbarFont_scaler=1f;
    public Integer cbarIntegerCasting = null;
    
    public boolean polyPainter_transparency = false;
    public int polyPaint_basecolKey = I_FIGURE_BASE_COLOR;
    
    public void inheritFontSpecs() {
        if (fontSpecs!=null) {
            try {
                String fnam = fontSpecs[0];
                if (fnam!=null && fnam.length()>2)this.font = fnam;
            }catch (Exception e) {}
            
           try {
                double scaler = Double.valueOf(fontSpecs[1]);
                this.fontScaler_master = scaler;
                
            }catch (Exception e) {} 
            
            try {
                String bw = fontSpecs[2];
                if (bw.contains("B")) {
                  this.customColors.put (I_FONT_COLOR, new int[]{0,0,0,255});
                } else {
                  this.customColors.put (I_FONT_COLOR, new int[]{255,255,255,255});
                }
                
            }catch (Exception e) {}
          
            try {
                String d3 = fontSpecs[3];
                if (d3.contains("true")) {
                    this.font3D=true;
                } else {
                   this.font3D = false;
                }
                
            }catch (Exception e) {}
            
            
        }//if not null
    }
    
    public void setFont(String f) {
        this.font =f;
    }

    
   public VisualOptions() {    
    this.customColors.put (I_GIS_BG_SEAS, new int[]{255, 255, 255, 255}); // this is landmass also
    this.customColors.put (I_GIS_GB_SEA_BORDER, new int[]{0,0,0,255}); // if null, no border drawing!
    this.customColors.put (I_FIGURE_BASE_COLOR, new int[]{180, 180, 180, 255}); // this is landmass also
    this.customColors.put (I_FONT_COLOR, new int[]{0,0,0,255});
    customBannerFile = BANNER_IMG_LOGO_FMI;//default - logo plus FMI
    setupDefaultMaskColors();
   }

    public final static int VALUEDOT_OFF = 0;
    public final static int VALUEDOT_SMALL = 1;
    public final static int VALUEDOT_MEDIUM = 2;
    public final static int VALUEDOT_LARGE = 3;
    public final static int VALUEDOT_LARGEST = 4;
    public final static int VALUEDOT_MEGA = 5;

    public void setValueDotSize(int sizer) {
        this.dotSize_percent =getValueDotSize(sizer); 
    }
    
    public static Double getValueDotSize(int sizer) {
          switch (sizer) {
            case VALUEDOT_SMALL:
                return 0.01;

            case VALUEDOT_MEDIUM:
                return 0.013;

            case VALUEDOT_LARGE:
                return 0.017;

            case VALUEDOT_LARGEST:
                return 0.025;
                
            case VALUEDOT_MEGA:
                return 0.035;    
            default:
               return null;
        }
    }

    public final static int MASK_CAT_COASTLINE = 0;
    public final static int MASK_CAT_ROADS = 1;
    public final static int MASK_CAT_BUILDS = 2;
    public final static int MASK_CAT_WATER = 3;
    public final static int MASK_CAT_PEDESTR = 4;
    public final static int MASK_CAT_SURFS = 5;

    public void setupDefaultMaskColors() {
        clearAllColors();
        addDefaultWaterColors();
    }
    
    public AwtOptions getAWTops() {
        return new AwtOptions(this);
    }
 
    public void setupDefaultMaskColors(int brightness_adjust,
            HashMap<Integer, Integer> types) {
        clearAllColors();
    }

    public void mapRendering_THE_DARKNESS(boolean LU, boolean roads, boolean builds) {
        clearAllColors();
        int[] bg= new int[]{22, 22, 22, 255};
        this.customColors.put(I_MASK_OTHER_COL, bg);
        this.customColors.put (I_FIGURE_BASE_COLOR, new int[]{0, 0, 0, 255}); // this is landmass also
        this.mask_categorical_cols.put(MASK_CAT_COASTLINE, new int[]{25, 25, 25, 255});//darkish
        this.mask_categorical_cols.put(MASK_CAT_WATER, new int[]{38, 38, 38, 255});//lightest color that is defined

        if (roads)this.mask_categorical_cols.put(MASK_CAT_ROADS, new int[]{3, 3, 3, 255});//quite dark;
        if (builds) this.mask_categorical_cols.put(MASK_CAT_BUILDS, new int[]{0, 0, 0, 255});//very dark
    }

    public void clearAllColors() {
        this.mask_categorical_cols = new HashMap<>();
        this.mask_surf_cols = new HashMap<>();
        this.mask_func_cols = new HashMap<>();
    }

    public void addDefaultWaterColors() {
        this.mask_categorical_cols.put(MASK_CAT_COASTLINE, new int[]{60, 60, 60, 255});//darkish
        this.mask_categorical_cols.put(MASK_CAT_WATER, new int[]{220, 220, 220, 255});//lightest color that is defined
    }
    
    public void setShaderWaterColors() {
        clearAllColors();
        this.customColors.put(I_MASK_OTHER_COL, null);
        this.mask_categorical_cols.put(MASK_CAT_COASTLINE, new int[]{25, 25, 25, 90});
        this.mask_categorical_cols.put(MASK_CAT_WATER, new int[]{25, 25, 25, 50});
    }


    public void clearNonWaterColor() {
        HashMap<Integer, int[]> mask_categorical_cols2 = new HashMap<>();
        HashMap<Integer, int[]> mask_surf_cols2 = new HashMap<>();
        this.mask_surf_cols = mask_surf_cols2;
        HashMap<Integer, int[]> mask_func_cols2 = new HashMap<>();
        this.mask_func_cols = mask_func_cols2;

        int[] coast = this.mask_categorical_cols.get(MASK_CAT_COASTLINE);
        int[] water = this.mask_categorical_cols.get(MASK_CAT_WATER);
        mask_categorical_cols2.put(MASK_CAT_COASTLINE, coast);
        mask_categorical_cols2.put(MASK_CAT_WATER, water);
        this.mask_categorical_cols = mask_categorical_cols2;

    }

    public void setBannerImg(String s) {
        this.customBannerFile = s;
    }

    public String getBannerImg() {
        return this.customBannerFile;
    }

    public boolean hasCustomBanner() {
      return (this.customBannerFile!=null 
              && this.customBannerFile.length()>3
              && !this.customBannerFile.equals(BANNER_IMG_LOGO_FMI));
    }

    public void putCustomColor(int I_IND, int[] rgba) {
      this.customColors.put(I_IND, rgba);
    }

    public void setFontToColor(int[] rgb) {
       this.customColors.put(I_FONT_COLOR, rgb);
    }

}
