/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;

import java.awt.image.BufferedImage;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import static org.fmi.aq.addons.visualization.GEProd.imageToKMZ;

/**
 * The purpose of this class is make the image creation of FigureData run
 * in a parallel separate thread. This can save a bit of time when several
 * large PNG-images need to be written to file at once.
 * @author Lasse Johansson
 */
    public class ImageRunnable implements Runnable {
        FigureData fd;
        String out;
        String name;
        
        BufferedImage img;
        final boolean kmz;
        Boundaries b;
        boolean preserve;
        public ImageRunnable(FigureData fd, String out, String name) {
            this.fd = fd;
            this.out = out;
            this.name = name;
            kmz = false;
        }
        
        public ImageRunnable(BufferedImage img, String out, String name,
                Boundaries b, boolean preserve, boolean kmz) {
            this.img = img;
            this.out = out;
            this.name = name;
            this.b = b;
            this.preserve = preserve;
            this.kmz = kmz;
        }
        
        @Override
        public void run() {
            
           if (img!=null) {
               
               if (!kmz) {
                   FigureData.saveImage(out, img, name, GraphicsAddon.IMG_FILE_PNG);
                   return;
               }
               
                try {
                // Save as new values_img
                imageToKMZ(img, out, b, name, preserve);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
           
           } else {
            fd.saveImage(out,name,GraphicsAddon.IMG_FILE_PNG);
           }
        }
        
    }
