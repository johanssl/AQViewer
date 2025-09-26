/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;


import java.awt.Color;
import java.awt.Graphics2D;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.fmi.aq.essentials.geoGrid.AreaNfo;

public class PolyPainter implements Serializable{
private static final long serialVersionUID = 832812631;
public final static String DEFNAME ="polyPainter.dat";
  
    public static PolyPainter load(String dir)  {
        String filename = dir+DEFNAME;
        System.out.println("Loading PolyPainter from: " + filename);
        try {
        FileInputStream f_in = new FileInputStream(filename);
        ObjectInputStream obj_in = new ObjectInputStream(f_in);
        // Read an object
        Object obj = obj_in.readObject();
        if (obj instanceof PolyPainter) {
            PolyPainter mp = (PolyPainter) obj;
            System.out.println("PolyPainter loaded from .dat-file successfully.");
            return mp;
        }
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

  public ArrayList<Vpolygon> polys = new ArrayList<>();
  public ArrayList<Vpolygon> polys_inner = new ArrayList<>();
  
  public PolyPainter() {
      
  }
  
    public void saveToBinary(String dir) throws Exception {
        String fileName = dir +DEFNAME;
        System.out.println("Saving PolyPainter to: " + fileName);
        // Write to disk with FileOutputStream
        FileOutputStream f_out = new FileOutputStream(fileName);
        ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
        // Write object out to disk
        obj_out.writeObject(this);
        obj_out.close();
        f_out.close(); 
        System.out.println("Done.");
   } 
  
  public void cropToBounds(Boundaries b) {
      ArrayList<Vpolygon> npolys = new ArrayList<>();
      for (Vpolygon p:polys) {
          if (p.isRelevant(b)) npolys.add(p);
      }
      for (Vpolygon p:polys_inner) {
          if (p.isRelevant(b)) npolys.add(p);
      }
      
      System.out.println("Polys after boundaries check: "+ npolys.size());
      this.polys = npolys;
  }
  

    public BufferedImage intoImg(AreaNfo in, VisualOptions vops) {
      //hash colors that are used.  
      AwtOptions awt = vops.getAWTops();
      HashMap<Integer,Color> cols = new HashMap<>();
      for (Vpolygon p:polys) {
          Color c= awt.getCustomColor(p.colorMapping_border);
          if (c!=null) cols.put(p.colorMapping_border, c);
          
          Color c2 = awt.getCustomColor(p.colorMapping_fill);
          if (c2!=null) cols.put(p.colorMapping_fill, c2);
      }
      
      System.out.println("HxW = "+ in.H+" x "+ in.W);
      int trueW = in.W;
      int trueH = in.H;
      int skips =0;

      int[] pixels = new int[trueW * trueH];
        BufferedImage pixelImage = new BufferedImage(trueW, trueH, BufferedImage.TYPE_INT_ARGB);
        pixelImage.setRGB(0, 0, trueW, trueH, pixels, 0, trueW);
        //landMasses
        Graphics2D g = pixelImage.createGraphics();
        Color base = awt.getCustomColor(vops.polyPaint_basecolKey);
        g.setColor(base);
        g.fillRect(0, 0, trueW, trueH);
        int k =0;
        
        for (int j =1;j<=2;j++) {//for one, two
            ArrayList<Vpolygon> all = this.polys;
            if (j==2) all = this.polys_inner;//switch
            
            for (Vpolygon pr:all) {
                k++;
                if (!pr.isRelevant(in.bounds)) {//relevancy check for skip.
                  skips++;
                  continue;
                }

                if (pr.isLine) {//special case: it's a line.
                   Color bc = cols.get(pr.colorMapping_border);
                    if (bc!=null) {
                        g.setColor(bc);
                        Polygon p = pr.convertForGraphics(in);
                        //draw lines
                        for (int i =0;i<p.npoints-1;i++) {
                            int x1 = p.xpoints[i];
                            int x2 = p.xpoints[i+1];
                            int y1 = p.ypoints[i];
                            int y2 = p.ypoints[i+1];

                            g.drawLine(x1, y1, x2, y2);
                        }//for points
                    }//if color exist
                    continue;  
                } //if line 


                Polygon p = pr.convertForGraphics(in);
                //if (k%2000==0) System.out.println("poly "+ k +" of "+polys.size() +", skipped="+skips);
                Color c = cols.get(pr.colorMapping_fill);
                if (c!=null) {
                    g.setColor(c);
                    g.fillPolygon(p);
                }

                Color bc = cols.get(pr.colorMapping_border);
                if (bc!=null) {
                    g.setColor(bc);
                    g.drawPolygon(p); 
                }

            }//for all polys
        }
        g.dispose();
        //finally, transparency mod.
        Color land = awt.getCustomColor(VisualOptions.I_GIS_BG_LAND);
        if (vops.polyPainter_transparency && land!=null) {
            //GraphicsAddon ga = new GraphicsAddon();
            int transp = getARBGInt(255, 255,255, 255);
            BufferedImage newImage = new BufferedImage(in.W, in.H, pixelImage.getType());
            int pix;
            int red; int green; int blue;
            for (int h =0;h<in.H;h++) {
                for (int w =0;w<in.W;w++) {
                    pix = pixelImage.getRGB(w, h);
                    red = ((pix >> 16) & 0xff);
                    green = ((pix >> 8) & 0xff);
                    blue = (pix & 0xff);
                    
                    if (red == land.getRed() && green == land.getGreen() && blue==land.getBlue()) {
                        newImage.setRGB(w, h, transp);
                    } else {
                        newImage.setRGB(w, h, pix);
                    }
                }
            } 
            pixelImage = newImage;
        }//if alpha switch
         
        return pixelImage; 
  }
    
    private static int getARBGInt(int b, int r, int g, int a) {//this copy-pasta has a weird order for r,g,b.
	return ((a << 24) | 0xFF) + ((r << 16) | 0xFF) + ((g << 8) | 0xFF) + (b | 0xFF);
    }
    
} 