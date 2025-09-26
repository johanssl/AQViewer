/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.addons.visualization;

import java.awt.Polygon;
import java.io.Serializable;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.AreaNfo;

/**
 *
 * @author johanssl
 */
public class Vpolygon implements Serializable{
    private static final long serialVersionUID = 332912531;

    final float[] lats;
    final float[] lons;
    final int len;
    public int colorMapping_fill =VisualOptions.I_GIS_BG_SEAS;
    public int colorMapping_border =VisualOptions.I_GIS_GB_SEA_BORDER;
    public String info = null;
    public boolean isLine =false;
    public int categorization=0;
    private Boundaries b= null;
    public Vpolygon(float[] lats,  float[] lons) {
        this.lats = lats;
        this.lons = lons;
        len = lats.length;
        this.assessBounds();
    }
    
    private void assessBounds() {
        this.b = Boundaries.expansionStarter();
        for (int i =0;i<len;i++) {
            b.expandIfNeeded(lats[i],lons[i]);
        }
    }
    
    public boolean isRelevant(Boundaries b) {
        return b.intersectsOrContains(this.b);
    }
    
    public Polygon convertForGraphics(AreaNfo in) {
        int[] x = new int[len];
        int[] y = new int[len];
        for (int i =0;i<len;i++) {
            x[i] = in.getW(lons[i]);
            y[i] = in.getH(lats[i]);
        }
        return new Polygon(x,y,len);
    }
    
}
