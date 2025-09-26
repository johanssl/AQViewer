/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;

import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.GeoGrid;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import java.util.logging.Level;
import static org.fmi.aq.addons.visualization.HueList.getHuesAsList;
import static org.fmi.aq.addons.visualization.HueList.getHuesAsList_natural;
import static org.fmi.aq.enfuser.ftools.FileOps.Z;
import org.fmi.aq.enfuser.ftools.FuserTools;
import org.fmi.aq.essentials.geoGrid.AreaNfo;

/**
 * This class stores all relevant information to produce
 * informative figures from emission grids. Several color Schemes are available
 * (check: final static int fields) Color/value scaling can be manipulated
 * through Options. ALL figures will have a minimum horizontal resolution equal
 * to minW, which means that even low-resolution grids can be neatly drawn. 
 * 
 *  @author Lasse Johansson
 */
public class FigureData {

    public int borderSize = 0; // width in pixels for grid Image
    public Color borderCol = Color.BLACK;

    int HEIGHT_ADD_COLBAR = 0;
    int CBAR_HEIGHT = 100;

    //Schemes
    public static final int COLOR_BASIC = 0; //radiant yellow to dark brown
    public static final int COLOR_GREEN_TO_VIOLET = 1;
    public static final int COLOR_TEMPERATURE = 2;
    public static final int COLOR_WHITE_TO_BLUE = 3;
    public static final int COLOR_COMPARISON = 4;
    public static final int COLOR_DEEP_PURPLE = 5;
    public static final int COLOR_BLUE_PURPLE = 6;
    public static final int COLOR_NAT_GREENS = 7;
    public static final int COLOR_PASTEL_GR = 8;
    public static final int COLOR_SHADE = 9;
    public static final int COLOR_FLAME = 10;
    public static final int COLOR_MONOB = 11;
    public static final int COLOR_DIRT_FOREST = 12;
    public static final int COLOR_BLACKTEMP = 13;
    public static final int COLOR_BLACKGR = 14;

    public static final String[] COLOR_SCHEME_NAMES = {
        "LightBrown",
        "GreenToPurple",
        "Temperature",
        "LightBlue",
        "FullPalette",
        "DeepPurple",
        "BluePurple",
        "NaturalGreens",
        "PastelGR",
        "Shade",
        "Flame",
        "MonoBlack",
        "DirtToForest",
        "BTemperature",
        "BGP"
    };
    //old basic 0.17,0.5,1_0.9,0.9,0.3_inv
    
    public static final String[] RECIPEES = {
        "0.17,0.5,1_0.9,0.9,0.3_inv",//basic
        "0.28,1,1_0.8,1,0.6_inv", //GRtoV
        "0.63,1,1_0.95,1,0.8_inv",//TEMP
        "0.47,0.2,1_0.86,1,0.05",//WhB
        "0.6,1,1_0.8,1,0.9_inv",//COMP
        "0.17,1,1_0.83,1,0.25_inv",//DeepPurple
        "0.5,1,1_0.88,1,0.4", //blue purp
        "0.14,0.9,1_0.32,0.9,0.1",//natGreens
        "0.27,0.9,1_0.95,0.9,0.9_inv",//pastel
        "0.0,0.5,0.2_0.15,0.2,1",
        "0.19,1,0_0.88,1,1_inv_flip_flame",
        "0,0,0_1,0,1_trueSB",
        "0.01,0.7,0_0.36,1,1",
        "0.63,1,1_0.95,1,0.8_inv_blackRamp",
        "0.28,1,1_0.8,1,0.6_inv_blackRamp"
    };
    //old WtB "0.51,0.5,1_0.75,1,0.15
    //layers
    public BufferedImage[] layers;
    private int LAYER_BG_MASK = 0;
    private int LAYER_GRID = 1;
    private int LAYER_POST = 2;

    private Object maps;
    
    public BufferedImage colBar;
    public BufferedImage banner;
    private BufferedImage fullImg;

    public static int TRANSPARENCY_OFF = 0;
    public static int TRANSPARENCY_LOW = 1;
    public static int TRANSPARENCY_MED = 2;
    public static int TRANSPARENCY_HIGH = 3;
    public static int TRANSPARENCY_STEAM = 4;

    public static String[] TRANSP_NAMES = {
        "TRANSPARENCY_OFF",
        "TRANSPARENCY_LOW",
        "TRANSPARENCY_MED",
        "TRANSPARENCY_HIGH",
        "TRANSPARENCY_AT_LOW_VALUES"

    };
    //no opacity as default, 0= 100% transparent

    /**
     * This array contains elements with the form (text,lat,lon,additional). The
     * text will be displayed in the figure in the location defined by the
     * coordinates. There's a couple of additional features: colored dots. - In
     * case the text can be parsed into a double value and VisualOptions has a
     * defined "dot size", then a "colored dot" will be drawn instead. - The
     * color will be based on the numeric value, checked from the color range of
     * this FigureData. - In case the 4th element exists (additional) and can be
     * parsed to a double, then the value is used to scale the default radius of
     * the drawn circle.
     */
    public ArrayList<GridDot> dots = new ArrayList<>();
    public VisualOptions ops;
    public int fontSize;

    public Color col_smaller;
    public Color col_larger;
    public Color[] cols;
    private Color[] nonTransp_cols;
    public double[] nonTransp_vals;

    public double[] vals;

    public String type = "type";
    public String aboveScale1 = null;
    public String text_upperCenter = null;

    protected GeoGrid dat;
    public int trueH; // low resolution emission grid will not cause the background layer to have a low resoltion. => independent H-W for bg layers!
    public int trueW;
    double upscaleRatio;
    double sizer;
    double colScaling_max;
    double colScaling_min;
    double range;

    private boolean processed = false;
    Boundaries reg;
    private double southAdjust = 0;

    HashMap<Integer,GeoGrid> metGrids;
    public PolyPainter polyp=null;
    private final GraphicsAddon im;

    

    public FigureData(float[][] dat, Boundaries reg, VisualOptions ops) {
        this(new GeoGrid(dat, null, reg), ops);
    }

    public FigureData(GeoGrid grid, VisualOptions ops) {
        this.im = new GraphicsAddon();
        this.reg = grid.gridBounds;
        this.ops = ops;
        this.dat =grid;
        // resolution and margins evaluation      
        this.updateWidthsAndHeights();
        
        this.layers = new BufferedImage[3];
        if (ops.shaderPostLayer) {//switch order
            LAYER_BG_MASK = 1;
            LAYER_GRID = 0;
        } 
        this.type = reg.name;
        if (this.type == null) this.type = "type";
       
        String[] texts = ops.text;
        if (texts==null && grid.visOpsStrings!=null) texts = grid.visOpsStrings;
        if (texts!=null) {
            type= texts[TXT_TYPE_IND];
            if (texts.length>1)aboveScale1=texts[TXT_COLB_IND];
            if (texts.length>2)text_upperCenter = texts[TXT_HEADER_IND];
        }
    }
    
    public final static int TXT_TYPE_IND =0;
    public final static int TXT_COLB_IND =1;
    public final static int TXT_HEADER_IND =2;

    private void updateWidthsAndHeights() {
        this.trueW = Math.max(ops.fd_minimumWidth, dat.W);//can be higher than dat.W
        this.upscaleRatio = (double)trueW/(double)dat.W;
        float hw_ratio = (float)dat.H/(float)dat.W;
        this.trueH = (int) (hw_ratio * trueW);
        if (trueH % 2 == 1) trueH++;//for video encoding, must be paired
        if (trueW % 2 == 1) trueW++;//for video encoding, must be paired
        
        //font and cbar sizes
        sizer = Math.sqrt(trueW*trueH)/1000;
        if (sizer > 3) sizer =3;
        fontSize = (int) (sizer * 18 * ops.fontScaler_master);
        this.CBAR_HEIGHT = (int) (sizer * 110 * ops.cbarScaler);
        if (this.CBAR_HEIGHT%2==1) this.CBAR_HEIGHT++;
        //basic color bar adds to true image height, and this can be a problem for Google Earth layers.
        this.HEIGHT_ADD_COLBAR =0;
        if (ops.colBarStyle == VisualOptions.CBAR_BASIC) this.HEIGHT_ADD_COLBAR = CBAR_HEIGHT;
        double saFra = (double) (HEIGHT_ADD_COLBAR / (double)trueH);
        this.southAdjust = saFra * (reg.latmax - reg.latmin); // scaling and THE scale drives the figure south

        this.processed = false; // changing resolution should mess-up all layers if they are not processed again accordingly. Settting this false we force re-processing.  
    }

    public static int MAX_STEAM_TRANSP = 255;
    public static int MIN_STEAM_TRANSP = 0;
    
    private static int getTransparency(int transpMode, float colorNumberFraction) {
        if (transpMode == FigureData.TRANSPARENCY_OFF) {
            return 255;
        } else if (transpMode == FigureData.TRANSPARENCY_LOW) {
            return 200;
        } else if (transpMode == FigureData.TRANSPARENCY_MED) {
            return 185;
        } else if (transpMode == FigureData.TRANSPARENCY_HIGH) {
            return 165;

        } else  { //scaling (STEAM) 
            int colIndex_transp = (int) (colorNumberFraction*1600);
            if (colIndex_transp < MIN_STEAM_TRANSP) colIndex_transp= MIN_STEAM_TRANSP;
            return Math.min(MAX_STEAM_TRANSP, colIndex_transp);     // color[10] has 140 which is still  quite transparent.
            
        } 
    }

    public Boundaries getBounds() {
        return this.reg;
    }

    public void setTypeString(String s) {
        this.type = s;
    }

    public void setColorPalette(int t) {
        this.ops.colorScheme = t;
    }

    public void setTextAboveColBar(String s) {
        this.aboveScale1 = s;
    }

    public void switchData(GeoGrid g) {
        boolean resetBG = (g.H != dat.H || g.W !=dat.W);
        this.dat=g;
        if (resetBG) {
            this.updateWidthsAndHeights();
            this.layers[LAYER_BG_MASK]=null;
        } 
        this.processed = false;
    }

    private void process() {
        this.setupColsAndVals();
        this.layers[LAYER_GRID] = this.setupGridImage();//the beef
        this.setupColBarImage();
        this.fullImg = this.setupFinalImage();
        this.processed = true;
    }

    public BufferedImage getBufferedImage() {
        if (!this.processed) this.process(); // process all layers just to be sure
        return this.fullImg;
    }

    public void drawToCanvasScaledByWidth(Canvas canvas, int Width) {
        if (!this.processed) this.process();
        canvas.setIgnoreRepaint(false);

        Graphics g = canvas.getGraphics();
        g.clearRect(0, 0, 2000, 2000);

        int height = dat.H;
        int width = dat.W;
        double scaler = (double) Width / (double) width;
        Image img = fullImg.getScaledInstance((int) (width * scaler),
                (int) (height * scaler), Image.SCALE_SMOOTH);
        g.drawImage(img, 0, 0, null);
        g.dispose();

        canvas.setIgnoreRepaint(true);
    }
    
    public void drawToCanvas(Canvas canvas) {
        if (!this.processed) this.process();
        drawToCanvas(canvas,this.fullImg);
    }
        
     public static void drawToCanvas(Canvas canvas, BufferedImage fullImg) {
       
        canvas.setIgnoreRepaint(false);
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        
        Graphics g = canvas.getGraphics();
        g.clearRect(0, 0, width, height);

        Image img = fullImg.getScaledInstance(width,height, Image.SCALE_SMOOTH);
        g.drawImage(img, 0, 0, null);
        g.dispose();

        canvas.setIgnoreRepaint(true);
    }     

    public Image getScaledImage(int Width) {
        if (!this.processed) this.process();
        int height = dat.H;
        int width = dat.W;

        double scaler = (double) Width / (double) width;
        Image img = fullImg.getScaledInstance((int) (width * scaler),
                (int) (height * scaler), Image.SCALE_SMOOTH);
        return img;
    }

    public static Image getScaledImage(BufferedImage bimg, int nw, int nh) {
        Image img = bimg.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        return img;
    }

    public String KMZ_idAdd = null;
    //Produces a Google Earth layer from the full emission figure.
    public void ProduceGEfiles(boolean preserveImage, String outDir) {
        if (!savedToFile) {
            saveImage(outDir);
        }
        String kmzid = this.type + "";
        if (this.KMZ_idAdd != null) {
            kmzid += this.KMZ_idAdd;
        }
        //there is a possibility that the longiude has been shifted. This is we TrueLonmin is called since GE layers must be based on the actual coordinates
        GEProd.produceKML(outDir, this.type, "testi", reg.lonmin, reg.lonmax,
                reg.latmin - southAdjust, reg.latmax, kmzid);
        GEProd.produceKMZ(this.type, outDir, preserveImage);
    }

    public static String saveImage(String dir, BufferedImage img, String name, int figType) {
        String fullName = dir + name + "." + GraphicsAddon.fileTypes[figType];
        try {
            // Save as new values_img
            ImageIO.write(img, GraphicsAddon.fileTypes[figType], new File(fullName));
            return fullName;
        } catch (IOException ex) {
            return null;
        }

    }
    private boolean savedToFile = false;

    public File saveImage(int figType, String outDir) {
        String dir = outDir;
        String name = this.type;
        EnfuserLogger.log(Level.FINE,this.getClass(),"Saving image to file: " + dir + ", " + name);
        File file = saveImage(dir, name, figType);
        savedToFile = true;
        return file;
    }

    public void saveImage(String outDir) {
        String dir = outDir;
        String name = this.type;
        if (ops.imageOutAsRunnable) {
              Runnable runnable = new ImageRunnable(this,outDir,name);
              Thread thread = new Thread(runnable);
              thread.start();
        } else {
        saveImage(dir, name, GraphicsAddon.IMG_FILE_PNG);
        }
        savedToFile = true;
    }
    
    public File saveImage(String dir, String name, int figType) {
        if (!this.processed) this.process();
        
        BufferedImage img;
        if (figType == GraphicsAddon.IMG_FILE_JPG) {
            // create a blank, RGB, same width and height, and a white background
            img = new BufferedImage(fullImg.getWidth(),
                    fullImg.getHeight(), BufferedImage.TYPE_INT_RGB);
            img.createGraphics().drawImage(fullImg, 0, 0, Color.WHITE, null);

        } else {
            img = this.fullImg;
        }

        String fullName = dir + name + "." + GraphicsAddon.fileTypes[figType];
        try {
            // Save as new values_img
            File f = new File(fullName);
            ImageIO.write(img, GraphicsAddon.fileTypes[figType], f);
            return f;
        } catch (IOException ex) {
            return null;
        }
    }

    public void drawImagetoPane() {
        if (!this.processed) this.process();
        new GraphicsAddon().imageToFrame(fullImg,false);
    }
    
    protected void drawString(int rotation, Graphics2D g2d, AwtOptions awt,
            float fontScaler, int x, int y, String s, boolean forceBW) {
        Color fontc = awt.fontColor();
        Color inv = awt.getInverseFontColor();
        if (forceBW) {
            fontc = Color.BLACK;
            inv = Color.WHITE;
        }
        int fs = (int) (this.fontSize * fontScaler);
        g2d.setFont(new Font(ops.font, ops.fontStyle, fs));
        if (ops.font3D) {
            g2d.setPaint(inv);
            GraphicsRotator.setRotatedText(g2d, x + 1, y + 1, rotation, s); //inverse color with a one pixel offset
        }
        g2d.setPaint(fontc);
        GraphicsRotator.setRotatedText(g2d, x, y, rotation, s);
    }
    

    private BufferedImage setupFinalImage() {
        int finalH = trueH + HEIGHT_ADD_COLBAR;
        //for Google earth the corner coordinates needs to be adjusted because a banner and a scale is added to the image
        AwtOptions awt = ops.getAWTops();
        //final image
        BufferedImage finalImg = new BufferedImage(trueW, finalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImg.createGraphics();
        g2d.setColor(awt.getCustomColor(VisualOptions.I_FIGURE_BASE_COLOR));
        g2d.fillRect(0, 0, trueW, trueH);
        //actual image is scaled (if needed) and placed
        for (int i = 0; i < layers.length; i++) {
            if (this.layers[i] == null) continue;
            if (upscaleRatio>1 && i == LAYER_GRID) { // scaling
                Image scaled = this.layers[i].getScaledInstance(trueW, trueH, Image.SCALE_SMOOTH);
                g2d.drawImage(scaled, 0, 0, null); // just under the banner
            } else {// no scaling           
                g2d.drawImage(this.layers[i], 0, 0, null); // just under the banner                                         
            }
        }// for layers
         
        this.drawAdditionalImages(g2d);
        
        g2d.drawImage(this.colBar, 0, finalH - this.CBAR_HEIGHT, null);
        g2d.drawImage(this.banner, 0, 0, null);
        
        //TEXT field
        if (this.text_upperCenter != null) {
            int downAdjust = (int) (0.025f * trueH) + g2d.getFontMetrics().getHeight();
            int halfLen = g2d.getFontMetrics().stringWidth(this.text_upperCenter) / 2; //half of length of the string in pixels               
            if (halfLen > 0.25 * this.trueW) halfLen = (int) (0.25 * this.trueW);
            int y =  downAdjust;
            int x = (int) (this.trueW * ops.TXT_UPCENTER_LOC - halfLen); // at center
            drawString(0, g2d, awt, 1.2f, x, y,this.text_upperCenter,false);
        }
       
        Color fontc = awt.fontColor();
        Color fontc_inv = awt.getInverseFontColor();
        GridDot.drawDots(this,g2d, fontc,fontc_inv);

        g2d.dispose();
        return finalImg;
    }
    
    public int getTrueX(double lon) {
        return (int)(dat.getW(lon)*this.upscaleRatio);
    }
    public int getTrueY(double lat) {
        return (int)(dat.getH(lat)*this.upscaleRatio);
    }


    public static Color[] getColors(String recipee, Canvas canv, int transInt) {
        Graphics g = null;
        if (canv != null) {
            g = canv.getGraphics();
            g.clearRect(0, 0, 1000, 1000);
        }

        String[] split = recipee.split("_");
        int steps = Integer.valueOf(split[0]);

        boolean inverse = recipee.contains("inv");
        boolean natural = recipee.contains("natural");
        boolean flip = recipee.contains("flip");
        boolean trueSB = recipee.contains("trueSB");
        boolean flame = recipee.contains("flame");
        boolean blackRamp = recipee.contains("blackRamp");
        
        String hsb1 = split[1];
        double h1 = Double.valueOf(hsb1.split(",")[0]);
        double s1 = Double.valueOf(hsb1.split(",")[1]);
        double b1 = Double.valueOf(hsb1.split(",")[2]);

        String hsb2 = split[2];
        double h2 = Double.valueOf(hsb2.split(",")[0]);
        double s2 = Double.valueOf(hsb2.split(",")[1]);
        double b2 = Double.valueOf(hsb2.split(",")[2]);

        //check range for natural 
        if (natural) {
            double range = h2 - h1;
            double effectScaler = 0.8f - range;
            if (effectScaler < 0.15) effectScaler = 0.15;
            //in between 0 and 0.5;
            //edits
            s1 -= effectScaler / 5;
            if (s1 < 0.85) s1 = 0.85;
            
            b2 -= effectScaler;
            if (b2 < 0.3) b2 = 0.4;
            if (b2 > 0.75) b2 = 0.75;
        }

        double stepsize;
        if (inverse) {
            stepsize = (h1 + (1 - h2)) / steps;
        } else {
            stepsize = (h2 - h1) / steps;
        }
        ArrayList<Double> hues;
        if (!natural) {
            hues = getHuesAsList(h1, h2, steps, inverse, stepsize);
        } else {
            hues = getHuesAsList_natural(h1, h2, steps);
        }

        if (flip) {
            ArrayList<Double> fhues = new ArrayList<>();
            for (int i = hues.size() - 1; i >= 0; i--) {
                fhues.add(hues.get(i));
            }
            hues = fhues;
        }

        Color[] cols = new Color[steps];
        int xStrip = 0;
        if (canv != null) {
            xStrip = (int) (canv.getWidth() / steps);
        }

        for (int w = 0; w < hues.size(); w++) {
            double numCol_scaler = (double) w / hues.size();
            double scaler = numCol_scaler * numCol_scaler;
            double br_scaler = scaler;
            if (!natural && (b2 - b1 > 0.8)) {//starts from black tones, and usually it is best to quickly move to brighter tones
                br_scaler = Math.pow(numCol_scaler, 0.45);
            }

            if (trueSB) {//saturation and brightness are exactly based on the number of color, no non-linear visual mods.
                scaler = numCol_scaler;
                br_scaler = scaler;
            }
            double H = hues.get(w);
            double S = (1 - scaler) * s1 + (scaler) * s2;
            double B = (1 - br_scaler) * b1 + (br_scaler) * b2;

            if (blackRamp) {//first colors are significantly reduced in brightness.
                double B2 = 0 + numCol_scaler*8;//at 0.05 this is already 1.0.
                if (B2 >1) B2 =1;
                B = Math.min(B2,B);
            }
            
            if (flame) {//edit SB since it's difficult to get right automatically
                S = 1;
                if (numCol_scaler < 0.0) {
                    B = 0;
                } else {
                    B = (numCol_scaler - 0.0) * 2.5;
                    if (B > 1) {
                        B = 1;
                    }
                }

                double S_toEnd = 1f - numCol_scaler;
                if (S_toEnd < 0.2) {//reduce saturation and go towards pure white
                    S = 1.0 - (0.2 - S_toEnd) * 4;
                    if (S < 0) {
                        S = 0;
                    }
                }
            }
            Color c = Color.getHSBColor((float) H, (float) S, (float) B);
            Color col = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                    getTransparency(transInt, (float) numCol_scaler));
            cols[w] = col;

            if (canv != null) {
                for (int x = 0; x < canv.getHeight(); x++) {
                    g.setColor(col);
                    g.drawLine(w * xStrip, x, (w + 1) * xStrip, x);
                }
            }
        }//for hue
        if (canv != null) g.dispose();
        return cols;
    }

    private static String getRecipeString(int scheme) {
        if (scheme < 0 || scheme > FigureData.RECIPEES.length - 1) {
            scheme = 0;
        }
        return FigureData.RECIPEES[scheme];
    }

    public static Color[] getCols(int selectedScheme, String recipee, int transInt, int numCols) {
        if (recipee == null || recipee.length() <= 7) {
            recipee = getRecipeString(selectedScheme);
        }
        Color[] cols = getColors(numCols + "_" + recipee, null, transInt);
        return cols;
    }
    
    public static Color[] getCols(VisualOptions vops, int steps) {
        String recipee = getRecipeString(vops.colorScheme);
        Color[] cols = getColors(steps + "_" + recipee, null,vops.transparency);
        return cols;
    }

    private void setupColsAndVals() {
        VisualOptions vops = this.ops;
        //automatic scaling
        if (vops.minMax == null) {
            double[] mma = dat.getMinMaxAverage(1);
            double minTemp = mma[0];
            double maxTemp = mma[1];

            this.colScaling_max = maxTemp / vops.scaleMaxCut;
            this.colScaling_min = minTemp;
        } else {
            this.colScaling_max = vops.minMax[1];
            this.colScaling_min = vops.minMax[0];
        }

        this.range = this.colScaling_max - this.colScaling_min;
        this.cols = getCols(vops.colorScheme, vops.customPaletteRecipee, vops.transparency, vops.numCols); // linear scale progression => double the amount of colors
        this.nonTransp_cols = getCols(vops.colorScheme, vops.customPaletteRecipee, FigureData.TRANSPARENCY_OFF, vops.numCols);
        this.col_larger = cols[cols.length - 1];
        if (vops.lowCut_transparency) {
            this.col_smaller = new Color(255, 255, 255, 0);
        } else {
            this.col_smaller = cols[0];
        }

        this.vals = getVals(cols.length, this.colScaling_max, this.colScaling_min, vops.scaleProgression);
        this.nonTransp_vals = getVals(nonTransp_cols.length, this.colScaling_max, this.colScaling_min, vops.scaleProgression);
    }

    private int getColorIndex(double val) {
        //e.g. value = 50.
        if (val >= this.colScaling_max) {
            return this.nonTransp_cols.length - 1;
        } else if (val <= this.colScaling_min) {
            return 0;
        } else {// in between
            double relative = (val - this.colScaling_min) / this.range;//e.g. relative = 0.5 in between min 10 and max 90.
            double curver = ops.scaleProgression;//e.g, this is 2.0
            relative = Math.pow(relative, 1.0 / curver);
            //e.g. relative is now 0.25
            int index = (int) (this.nonTransp_cols.length * relative);
            if (index > this.nonTransp_cols.length - 1) {
                index = this.nonTransp_cols.length - 1;
            }
            return index;
        }
    }

    private static double[] getVals(int length, double max, double min, double scaleProgression) {
        double[] vals = new double[length];
        double range = max - min;
        double pow = scaleProgression;
        //VALUES for the scale
        for (int i = 0; i < vals.length; i++) {
            double relIndex = (double) i / (double) (vals.length - 1);
            vals[i] = (min + range * Math.pow(relIndex, pow));
        }
        return vals;
    }

    
    //This method produces an image describing the emission grid based on color/value-set,maxMin,ops.colorProgression, ops.maxCutOff
    private BufferedImage setupGridImage() {
        int W = dat.W;
        int H = dat.H;
        int[] pixels = new int[W * H];
        //PIXEL COLOR evaluation
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                if (h < borderSize || w < borderSize || (H - h - 1) < borderSize 
                        || (W - w - 1) < borderSize) { //white borders
                    pixels[(h) * W + (w)] = this.borderCol.getRGB();
                } else {
                    float value = (dat.values[h][w]);
                    Color col = this.getColor(value);
                    //colIndex has been evaluated at this point
                    pixels[(h) * W + (w)] = col.getRGB();
                }//if not border
            }//width
        }//height
        BufferedImage pixelImage = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        pixelImage.setRGB(0, 0, W, H, pixels, 0, W);
        return pixelImage;
    }

// this method produces a scale image (bufferedImage) based on color/value sets
//Information text is alo added to the image
    private void setupColBarImage() {
        if (ops.colBarStyle == VisualOptions.CBAR_NONE) return;
        int colValueModulo = 1 + (int) (this.nonTransp_cols.length / 22);//e.g. 44 colors => modulo =3. If CBAR_transp then modulo will be 4
        //main purpose: many colors in the bar require FEWER amount of value texts for visual clarity!
        colValueModulo=(int)Math.round(colValueModulo*ops.scaleValueModcaler);
        
        float cBar_fractionOfHeight = 0.3f;
        AwtOptions awt = ops.getAWTops();
        boolean forceBW = false;
        
        Color white = new Color(255, 255, 255, 255);
        if (ops.colBarStyle == VisualOptions.CBAR_TRANSP) {
            white = new Color(255, 255, 255, 65);
            colValueModulo += 1;//text will be displayed a bit sparser
            cBar_fractionOfHeight = 0.28f;
        } else if (ops.colBarStyle == VisualOptions.CBAR_BASIC) {//font color must be black
            forceBW = true;
        }

        int cBar_shift = (int) (0.1 * CBAR_HEIGHT);
        if (ops.txtRotation ==0) {
            cBar_shift = (int) (0.05 * CBAR_HEIGHT);
        }
        int wMargin = (int) (0.03f * trueW); // the amount of pixels between horizontal borders 
        this.colBar = new BufferedImage(trueW, CBAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[trueW * CBAR_HEIGHT];
        //now its time to add a color scale to bottom - white background first

        for (int i = 1; i < CBAR_HEIGHT; i++) {
            for (int j = 1; j < trueW - 1; j++) {
                pixels[(CBAR_HEIGHT - i) * trueW + j] = white.getRGB();
            }
        }

        int cLower = (int) (CBAR_HEIGHT / 2 - cBar_fractionOfHeight * CBAR_HEIGHT / 2 - cBar_shift);
        int cUpper = (int) (CBAR_HEIGHT / 2 + cBar_fractionOfHeight * CBAR_HEIGHT / 2 - cBar_shift);

        int colB_width = trueW - 2 * wMargin;
        int n_colors = nonTransp_cols.length;
        int oneColWidth = colB_width / n_colors;

        for (int i = cLower; i <= cUpper; i++) { // top WWWCCCWWWWWW  bottom

            int colorIndex;
            int fromLeft;
            for (int j = wMargin; j <= trueW - wMargin; j++) {
                //borders with black
                if (j == wMargin || j == trueW - wMargin || i == cLower || i == cUpper) {
                    pixels[(i) * trueW + j] = Color.LIGHT_GRAY.getRGB();
                    continue;
                } else if (j == wMargin + 1 || j == trueW - wMargin - 1 || i == cLower + 1 || i == cUpper - 1) {
                    pixels[(i) * trueW + j] = Color.GRAY.getRGB();
                    continue;
                }

                fromLeft = j - wMargin;
                colorIndex = (int) (n_colors * ((float) fromLeft / (float) colB_width));
                if (colorIndex > n_colors - 1) colorIndex = n_colors - 1;
                pixels[(i) * trueW + j] = nonTransp_cols[colorIndex].getRGB(); // colorbar has no transparency effect
            }
        }
        this.colBar.setRGB(0, 0, trueW, CBAR_HEIGHT, pixels, 0, trueW);

        Graphics2D g2d = this.colBar.createGraphics();
        RenderingHints rh = new RenderingHints( // clear fonts!
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHints(rh);

        //TEXT=======================      
        if (this.aboveScale1 != null) {
            int y_from_bar = cLower - (int) (ops.cbarFont_scaler*this.fontSize * 0.25f); //cBar starts from cLower. The text should be slightly above it
            int x = wMargin;
            int y = y_from_bar;
            try {
              this.drawString(0, g2d, awt, ops.cbarFont_scaler, x, y, this.aboveScale1, forceBW);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }

        }

        for (int i = 0; i < nonTransp_vals.length; i++) { // nonTransp always contais 40 different values, vals[] might contain 80. cBar has always 40 colors.
            double value = nonTransp_vals[i];
            String s = FuserTools.shortenPrec(value,this.ops);
            if (ops.cbarIntegerCasting !=null) {
                int m = ops.cbarIntegerCasting;
                int iv = (int)(value/m)*m;
                s = iv +"";
            }
            //precision conversion for clean color bar values
            double temp = (double) (i) / n_colors;
            int x = wMargin + (int) (temp * (trueW - 2 * wMargin)) + (int) (oneColWidth * 0.15f);
            int y = cUpper + (int) (1.02f * fontSize*ops.cbarFont_scaler);

            //draw string s
            if (i % colValueModulo == 0) { // second... fourth.... max value
                this.drawString(ops.txtRotation, g2d, awt, ops.cbarFont_scaler, x, y, s,forceBW);
            }
        }//for vals
        g2d.dispose();
    }

    
    public Color getColor(float value) {
        if (Float.isNaN(value)) {
            if (GraphicsAddon.NAN_COLOR!=null) return GraphicsAddon.NAN_COLOR;
        } 
        
        //first check outliers
        if (value < vals[0]) {
            return col_smaller;
        } else if (value > this.vals[vals.length - 1]) {
            return col_larger;
        }

        int colIndex = -1;
        if (ops.scaleProgression == 1) {//proper color can be calculated
            double temp = this.cols.length * (value - this.vals[0]) / (this.vals[vals.length - 1] - this.vals[0]); //linear scaling

            colIndex = Math.round((float) temp);
            if (colIndex < 0) colIndex = 0;
            if (colIndex > cols.length - 1) colIndex = cols.length - 1;
            
        } else if (ops.colorSmartFecth) {
            colIndex = this.getColorIndex(value);

        } else {
            // BRUTE FORCE SEARCH=================
            boolean found = false;
            for (int k = 0; k < this.vals.length; k++) {
                if (value <= this.vals[k]) {
                    colIndex = k;
                    found = true;
                    break;
                }
            }
            if (!found) {
                colIndex = this.cols.length - 1;
            }
            //====================================
        }// else if not linear progression
        return this.cols[colIndex];
    }

    public Boundaries getVirtualKMZbounds() {
       return new Boundaries(
               reg.latmin - southAdjust,
               reg.latmax,
               reg.lonmin,
               reg.lonmax
       );
    }
    
    private static PolyPainter PP = null;
    public static String COAST_POLY_DIR ="";
    public void loadAndUseCoastPolys() {
            if (PP ==null) {
                String dir = COAST_POLY_DIR;
                PP = PolyPainter.load(dir);
            }
            polyp = PP;
         ops.putCustomColor(VisualOptions.I_GIS_BG_SEAS, new int[]{0,0,0,255});//black
         ops.putCustomColor(VisualOptions.I_GIS_BG_LAND, new int[]{255,255,255,255});//white
         ops.putCustomColor(VisualOptions.I_GIS_GB_SEA_BORDER, new int[]{60,60,60,255});//dark gray
         ops.putCustomColor(VisualOptions.I_GIS_BG_LAKE, new int[]{0,0,0,255});
         ops.putCustomColor(VisualOptions.I_GIS_BG_RIVER, new int[]{60,60,60,255});//dark gray
         ops.polyPaint_basecolKey = VisualOptions.I_GIS_BG_SEAS;//all pixels are initially assumed as sea (works when coastline polys encapsulate LAND).
         ops.polyPainter_transparency = true;//all land mappings are made fully transparent.
         ops.putCustomColor(VisualOptions.I_FIGURE_BASE_COLOR, new int[]{0,0,0,0});//fully TRANSPARENT.
    }

    float metSymbolScaler() {
        return (float)(this.sizer*ops.metSymbScaler);
    }
    
    AreaNfo in;
    public int limW_bg = Integer.MAX_VALUE;
    private double getLat(int h) {
        if (in==null) in = new AreaNfo(reg,trueH,trueW);
        return in.getLat(h);
    }
    private double getLon(int w) {
        if (in==null) in = new AreaNfo(reg,trueH,trueW);
        return in.getLon(w);
    }

     public static FigureData visualizeGeoless(float[][] data, VisualOptions ops) {
        GeoGrid g = new GeoGrid(data, null, Boundaries.getDefault());
        if (ops==null) ops = VisualOptions.darkSteam(null, true, 1.0, false);
        FigureData fd = new FigureData(g, ops);
        return fd;
    } 

     HashMap<String,Image> imgAdditions = null;
    public void addCustomImage(Image im, double yFrac, double xFrac) {
       if (this.imgAdditions==null) this.imgAdditions = new HashMap<>();
       String key = yFrac +"_"+ xFrac;
       this.imgAdditions.put(key, im);
    }

    private void drawAdditionalImages(Graphics2D g2d) {
        if (this.imgAdditions ==null) return;
        for (String key:imgAdditions.keySet()) {
            Image img = imgAdditions.get(key);
            String[] sp = key.split("_");
            double yFrac = Double.parseDouble(sp[0]);
            double xFrac = Double.parseDouble(sp[1]);
            int y = (int)(yFrac*this.trueH);
            int x = (int)(xFrac*this.trueW);
            g2d.drawImage(img, x, y, null);
        }
    }
     
}//class end
