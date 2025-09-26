/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.fmi.aq.essentials.geoGrid.GeoGrid;

/**
 * This class makes a mesh of several images and draws them together in an array
 * into a single larger image.
 * @author Lasse Johansson
 */
public class MultiElementImage {
    

     public static BufferedImage multiElementImage(ArrayList<Object> imgs, String odir, String nam) {
        BufferedImage comp =multiElementImage(imgs);
        new GraphicsAddon().saveImage(odir, comp, nam, GraphicsAddon.IMG_FILE_PNG);
        return comp;
    }
     
     public static BufferedImage multiElementImage(ArrayList<Object> imgs,
             String odir, String nam, Integer rows, Integer cols, boolean whiteBG) {
        BufferedImage comp =multiElementImage_rc(imgs,rows,cols,whiteBG);
        if (comp==null) {
            System.out.println("Null image, abort: "+ odir+nam);
            return null;
        }
        new GraphicsAddon().saveImage(odir, comp, nam, GraphicsAddon.IMG_FILE_PNG);
        return comp;
    }
     
    public static File multiElementImage_file(ArrayList<Object> imgs,
             String odir, String nam, Integer rows, Integer cols, boolean whiteBG) {
        BufferedImage comp =multiElementImage_rc(imgs,rows,cols,whiteBG);
        if (comp==null) {
            System.out.println("Null image, abort: "+ odir+nam);
            return null;
        }
        return new GraphicsAddon().saveImage(odir, comp, nam, GraphicsAddon.IMG_FILE_PNG);
    } 
     
    public static BufferedImage multiElementImage(Image[][] imgs, boolean exitOnClose, boolean draw) {
        int h = imgs[0][0].getHeight(null);
        int w = imgs[0][0].getWidth(null);

        int rows = imgs.length;
        int cols = imgs[0].length;

        BufferedImage bigPic = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bigPic.createGraphics();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Image simg = imgs[row][col];
                if (simg == null) {
                    continue;
                }

                int hp = row * h;
                int wp = col * w;
                g2d.drawImage(simg, wp, hp, null);
            }
        }
        g2d.dispose();
        if (draw)new GraphicsAddon().imageToFrame(bigPic, exitOnClose);
        return bigPic;
    }
    
    public static BufferedImage multiElementImage(ArrayList<Object> imgs) {
       return multiElementImage_rc(imgs, null,null,false);
    } 
    
    public static BufferedImage cast(Object o) {
        if (o==null) return null;
        if (o instanceof BufferedImage) return (BufferedImage)o;
        //a file then.
        try {
            File f =(File)o;
            return ImageIO.read(f);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
     public static BufferedImage multiElementImage_rc(ArrayList<Object> imgs,
             Integer rows, Integer cols, boolean white) {
         
         
        BufferedImage first = cast(imgs.get(0));
        //find true number of non-null images.
        int imgN =0;
        ArrayList<BufferedImage> nonNulls = new ArrayList<>();
        for (Object o:imgs) {
            BufferedImage img = cast(o);
            if (img!=null){
                imgN++;
                nonNulls.add(img);
                if (first==null) first = img;
            }
        }
        if (first==null) return null;
        int h = first.getHeight(null);
        int w = first.getWidth(null);
        
        boolean auto = false;
        if (rows==null && cols ==null) {//both unspecified - try to make balanced grid.
            auto = true;
            rows =1;
            cols =2;
            int N =rows*cols;
            while(N < imgN) {
                if ((int)rows==(int)cols) {//if equal, expand columns first
                    cols++;
                } else {
                    rows++;
                }
                N =rows*cols;
            }
        }//if unspecified
        else if (rows == null) {//floating row count
           auto = true;
           rows =1;
           int N =rows*cols;
           while(N < imgN) {
               rows++;
               N =rows*cols;
           }
            
        } else if (cols==null) {//floating column numbers
            auto = true;
            cols =1;
             int N =rows*cols;
              while(N < imgN) {
               cols++;
               N =rows*cols;
           }
        }
        
        if (auto) System.out.println("Automatic imageGrid with "+ imgN +" => "+rows +" x "+ cols);
        BufferedImage bigPic = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bigPic.createGraphics();
        g2d.setColor(Color.BLACK);
        if (white) g2d.setColor(Color.WHITE);
        int maxDim = Math.max(bigPic.getWidth(), bigPic.getHeight());
        g2d.fillRect(0, 0, maxDim, maxDim);
        int k =-1;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                k++;
                if (k>=nonNulls.size()) continue;
                Image simg= nonNulls.get(k);
                int hp = row * h;
                int wp = col * w;
                g2d.drawImage(simg, wp, hp, null);
            }
        }
        g2d.dispose();
        return bigPic;
    } 
    
}
