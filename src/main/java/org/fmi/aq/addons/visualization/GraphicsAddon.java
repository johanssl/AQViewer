/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.addons.visualization.GEProd;

/**
 * This class provides a bridge to visualization, graphics, animation and GUI tools
 * that are included in addon-packages. 
 * @author Lasse Johansson
 */
public class GraphicsAddon {

    //fileTypes
    public static final int IMG_FILE_PNG = 0;
    public static final int IMG_FILE_JPG = 1;
    public static final String[] fileTypes = {"PNG", "JPG"};
    public static Color NAN_COLOR = new Color(150,150,150,255); 
    private BufferedImage img;
    int imgW=-1;
    int imgH=-1;

    public GraphicsAddon() {
        
    }
    
    public static void NaNColorAsGray() {
       NAN_COLOR = new Color(150,150,150,255); 
    }
    
    public void pixelsIntoImage(int H, int W, int[] npix, Boundaries b,
            String dir, String name, boolean preserve) {
      BufferedImage pixelImage = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        pixelImage.setRGB(0, 0, W, H, npix, 0, W);
        GEProd.imageToKMZ(pixelImage, dir, b, name, true);
    }

   /**
   * Read pixel rgb-values into integer array.
   * @param fname image file name
   * @return 
   */
    public int[] openPNG(String fname) {
        try {
            img = ImageIO.read(new File(fname));
            imgH=img.getHeight();
            imgW=img.getWidth();
            
            return new int[]{imgH,imgW};
        } catch (IOException ex) {
            Logger.getLogger(GraphicsAddon.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    
    public int[] getPixels() {
        int pixels[] = new int[imgW * imgH];
        img.getRGB(0, 0, imgW, imgH, pixels, 0, imgW);
        return pixels;
    }

    /**
 * Convert RGB in [0,255] space into HSB in [0,1] space.
 * @param r red component
 * @param g green component
 * @param b blue component
 * @param temp a temporary int[] to hold r,g,b. can be null.
 * @return float[3] HSB
 */
    public float[] RGBtoHSB(int r, int g, int b, float[] temp) {
       return Color.RGBtoHSB(r, g, b, temp);
    }


 /**
 * Convert H,S,B in [0,1] space into 32-bit RGB integer.
 * @param h hue
 * @param s saturation
 * @param b brightness
 * @return  32-bit RGB integer
 */
    public int HSBtoRGB(float h, float s, float b) {
       return Color.HSBtoRGB(h, s, b);
    }

    public int RGBtoPix(int[] rgb) {
    int Red = rgb[0];
    int Green = rgb[1];
    int Blue = rgb[2];
    Red = (Red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
    Green = (Green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
    Blue = Blue & 0x000000FF; //Mask out anything not blue.

    return 0xFF000000 | Red | Green | Blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.

    }

    public Object pixelsToBufferedImage(int H, int W, int[] pixels) {
                
        BufferedImage pixelImage = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        pixelImage.setRGB(0, 0, W, H, pixels, 0, W);
        return pixelImage;
    }

    public File saveImage(String dir, Object img, String name, int IMG_FORMAT) {
                String fullName = dir + name + "." + fileTypes[IMG_FORMAT];

        try {
            // Save as new values_img
            ImageIO.write((BufferedImage)img, fileTypes[IMG_FORMAT], new File(fullName));
            return new File(fullName);
        } catch (IOException ex) {
            Logger.getLogger(GraphicsAddon.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
    }

    public void imageToFrame(BufferedImage buff, boolean exitOnClose) {
        JFrame frame = new JFrame();
            JLabel label = new JLabel(new ImageIcon(buff));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
            if (exitOnClose)frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

   

    public float[][] getRasterDataFromLoaded() {
        Raster raster = img.getData();
        float[][] dat = new float[imgH][imgW];
         
        for (int h = 0; h < imgH; h++) {
            for (int w = 0; w < imgW; w++) {
                float f = raster.getSampleFloat(w, h, 0);
                dat[h][w]=f;
            }
        }
        return dat;
    }

 

    private final static int HUE =0;
    private final static int SAT =1;
    private final static int BR =2;
    
    public Color modifyBrightness(Color c, float adj, boolean saturMod) {
        float[] hsb = this.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[BR]+=adj;
        if (hsb[BR]<0) hsb[BR]=0;
        if (hsb[BR]>1) hsb[BR]=1;
        
        if (saturMod) {
           adj*=-1; //higher brightness is paired with lower saturation.
           hsb[SAT]+=adj;
           if (hsb[SAT]<0) hsb[SAT]=0;
           if (hsb[SAT]>1) hsb[SAT]=1; 
        }
        return Color.getHSBColor(hsb[HUE], hsb[SAT], hsb[BR]);
    }
    
    public Color modifyBrightness(Color c, float adj) {
        float[] hsb = this.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[BR]+=adj;
        if (hsb[BR]<0) hsb[BR]=0;
        if (hsb[BR]>1) hsb[BR]=1;
        return Color.getHSBColor(hsb[HUE], hsb[SAT], hsb[BR]);
    }
    
    public Color modifySaturation(Color c, float adj) {
        float[] hsb = this.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[SAT]+=adj;
        if (hsb[SAT]<0) hsb[SAT]=0;
        if (hsb[SAT]>1) hsb[SAT]=1;
        return Color.getHSBColor(hsb[HUE], hsb[SAT], hsb[BR]);
    }

   /**
   * Edit current or maximum value of a progress bar.
   * @param jbar JProgressBar as Object.
   * @param value the value to be set
   * @param max if true, then sets max value.
   */
    public void setJbarValue(Object jbar, int value, boolean max) {
       JProgressBar jp = (JProgressBar)jbar;
       if (max) {
           jp.setMaximum(value);
       } else {
           jp.setValue(value);
           
       }
    }

   /**
   * Draw info lines to a frame and show it.
   * @param arr 
   */
    public void textToPanel(ArrayList<String> arr) {
        String output = "";
        for (String line : arr) {
            output += line + "\n";
        }
        JFrame frame = new JFrame();
        JTextArea jTF_full = new JTextArea();
        frame.add(jTF_full);
        jTF_full.setText(output);
        frame.pack();
        frame.setVisible(true);

    }

    public void setJbarString(Object jbar, String txt, boolean stringPainted) {
         JProgressBar jp = (JProgressBar)jbar;
         jp.setString(txt);
         jp.setStringPainted(stringPainted);
    }
    
    public static BufferedImage resizeImage(BufferedImage image, double scaler) {
        int type = 0;
        type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage resizedImage = new BufferedImage((int) (image.getWidth() * scaler),
                (int) (image.getHeight() * scaler), type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, (int) (image.getWidth() * scaler), (int) (image.getHeight() * scaler), null);
        g.dispose();
        return resizedImage;
    }
    
    
}
