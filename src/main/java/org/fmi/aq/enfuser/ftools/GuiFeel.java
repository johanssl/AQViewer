/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.enfuser.ftools;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * A simple static class to setup GUI visual style.
 * 
 * @author Lasse Johansson
 */
public class GuiFeel {
 
    public final static String[] STYLES = {"nimbus","dark","darcula","intellij","light"};
    private static int STYLE_INT = -1;
    private static String FIXED_STYLE = null;
    
    public static void switchStyle(JFrame frame) {
        STYLE_INT++;
        if (STYLE_INT >= STYLES.length) STYLE_INT =0;
        FIXED_STYLE = STYLES[STYLE_INT];
        System.out.println("Switching to style '"+FIXED_STYLE+"'");
        System.out.println("\tNOTE: the default style during startup can be set to this by adding the line 'LOOK_AND_FEEL="+FIXED_STYLE+"' to globOps.txt");
        set(frame);
    }
    
    public static void set(JFrame frame) {
           String style = "dark";
           if (FIXED_STYLE!=null) {
               style = FIXED_STYLE;
           } 
          
         try { 
             if (style.contains("dark")) {
                 javax.swing.UIManager.setLookAndFeel(new FlatDarkLaf());
                    
             }  else if (style.contains("darcula")) {    
                 javax.swing.UIManager.setLookAndFeel(new FlatDarculaLaf());
                 
                 
             }  else if (style.contains("intellij")) {    
                 javax.swing.UIManager.setLookAndFeel(new FlatIntelliJLaf());
                
             }  else if (style.contains("light")) {    
                 javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
                     
             } else {
                javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels=javax.swing.UIManager.getInstalledLookAndFeels();
                 
                for (int idx=0; idx<installedLookAndFeels.length; idx++)
                if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
                    break;
                }
             }
             
             if (frame!=null) {
                 SwingUtilities.updateComponentTreeUI(frame);
                 frame.pack(); // Optional: Resize components
             }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } 
    }
      
}
