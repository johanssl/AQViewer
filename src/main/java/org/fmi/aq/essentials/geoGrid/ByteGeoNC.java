/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.geoGrid;

import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import java.util.logging.Level;

/**
 * A compact representation of float data, using bytes. Floats are converted
 * into bytes using a 250 stepped-scaling (and an offset) The main use case is
 * for netCDF: the offset and scaler are compatible with netCDF. NOTE:
 * ByteGeoGrid is NOT netCDF compatible
 *
 * @author Lasse Johansson
 */
public class ByteGeoNC  {

    public static ByteGeoNC byteCasted(GeoGrid g) {
        byte[][] dat = new byte[g.H][g.W];
        for (int h = 0;h < g.H;h++) {
            for (int w = 0;w<g.W;w++) {
                dat[h][w] = (byte)g.values[h][w];
            }
        }
        return new ByteGeoNC(dat,g.dt,g.gridBounds);
    }
    
    public byte[][] values;
    public final float VAL_SCALER;
    public final float VAL_OFFSET;
    public final AreaNfo in;
    final public Dtime dt;
    public String varName;

    /**
     * A somewhat duplicate class for ByteGeoGrid with some additional features.
     *
     * @param name Name of the grid.
     * @param vals values stored in the grid (HxW)
     * @param dt Dtime for time attribute.
     * @param bounds Geographic area for the grid.
     */
    public ByteGeoNC(String name, byte[][] vals, Dtime dt, Boundaries bounds) {
        this(vals, dt, bounds.clone(name));
    }

    public ByteGeoNC(byte[][] vals, Dtime dt, Boundaries bounds) {
        this.VAL_SCALER = 1;
        this.VAL_OFFSET = 0;
        int H = vals.length;
        int W = vals[0].length;
        this.in = new AreaNfo(bounds,H,W,dt);
        this.values = vals;
        this.dt = dt;
        varName = bounds.name;
    }
    
     public ByteGeoNC(GeoGrid g) {
         this(g.values,g.dt,g.gridBounds);
     }
    
     public ByteGeoNC(float[][] floats, Dtime dt, Boundaries bounds) {
        int H = floats.length;
        int W = floats[0].length;
        this.in = new AreaNfo(bounds,H,W,dt);
        this.values = new byte[H][W];

        float maxVal = Float.MIN_VALUE;
        float minVal = Float.MAX_VALUE;
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                float val = floats[h][w];
                if (Float.isNaN(val)) continue;
                if (val > maxVal) maxVal = val;
                if (val < minVal) minVal = val; 
            }
        }

        this.dt = dt;
        varName = bounds.name;
        float range = maxVal - minVal;
        //scaler MUST NOT BE 0 (divByZero) and this happens with constant field
        if (range == 0) {//max,min are the same
            this.VAL_SCALER = 1;
            this.VAL_OFFSET = minVal;
            EnfuserLogger.log(Level.FINER,this.getClass(),"Constant value field: scaler is " + VAL_SCALER + ", offset is zero.");

        } else {
            this.VAL_OFFSET = minVal + 0.5f * range;
            this.VAL_SCALER = range / 250;
        }

        //setup values
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                values[h][w] = this.convertToByte(floats[h][w]);
            }
        }
    }

    private float convertToFloat(byte b) {
        //conversion
        //f = offset + b*scaler => b = (f - offset)/scaler
        float f = ((float) b * VAL_SCALER + VAL_OFFSET);
        return f;
    }

    private byte convertToByte(float f) {
        if (Float.isNaN(f)) return (byte)(-127);
        //conversion
        //f = offset + b*scaler => b = (f - offset)/scaler
        f -= this.VAL_OFFSET;
        f /= this.VAL_SCALER;
        return (byte) f;
    }

    public GeoGrid convert() {
        float[][] nvals = new float[in.H][in.W];
        for (int h = 0; h < in.H; h++) {
            for (int w = 0; w < in.W; w++) {
                nvals[h][w] = (float) this.getValueAtIndex(h, w);
            }
        }
        return new GeoGrid(nvals, this.dt.clone(), this.in.bounds.clone());
    }

    public float getValueAtIndex(int h, int w) {
        return this.convertToFloat(this.values[h][w]);
    }

    public float getValue(double lat, double lon) {
        int h = in.getH(lat);
        int w = in.getW(lon);
        return this.getValueAtIndex(h, w);
    }

}
