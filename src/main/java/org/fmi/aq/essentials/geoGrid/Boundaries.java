/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.essentials.geoGrid;

import java.io.Serializable;
import org.fmi.aq.enfuser.ftools.FuserTools;
import static org.fmi.aq.essentials.geoGrid.AreaNfo.DEGREE_IN_METERS;

/**
 * This class describes a rectangular bounding box in EPSG:4326.
 * It is widely used in all packages that deal with gridded data and areas.
 * 
 * NOTE: This is a Serialized class - it is used as attributes in various objects-on-file.
 * If you are not familiar with Serialization in Java, don't touch the code!
 * 
 * @author Lasse Johansson
 */
public class Boundaries implements Serializable {

    private static final long serialVersionUID = 7526475294622776147L;//Important: change this IF changes are made that breaks serialization!
    private final static double degreeInKM = 2 * Math.PI * 6371.0 / 360.0;
    
    public static Boundaries expansionStarter() {
        return new Boundaries(90,-90,180,-180);
    }
    
    public double latmin;
    public double latmax;
    public double lonmin;
    public double lonmax;
    public String name = "";
    public final double lonScaler;

    

    /**
     * An object for Geographic area definition. This is a rectangular WGS-84
     * definition.
     *
     * @param latmin minimum latitude value (South).
     * @param latmax max latitude value (North).
     * @param lonmin minimum longitude value (West).
     * @param lonmax maximum longitude value (East).
     */
    public Boundaries(double latmin, double latmax, double lonmin, double lonmax) {
        this.latmin = latmin;
        this.latmax = latmax;
        this.lonmin = lonmin;
        this.lonmax = lonmax;

        double midlat = (latmax + latmin) / 2.0;
        lonScaler = Math.cos(midlat / 360 * 2 * Math.PI);
    }
    
    public static Boundaries fromCSV(String boundsString) {
        String splitter = ";";
        if (boundsString.contains(",") && !boundsString.contains(";")) splitter =",";
        String[] sp = boundsString.split(splitter);
        return new Boundaries(
        Double.parseDouble(sp[0]),
        Double.parseDouble(sp[1]),
        Double.parseDouble(sp[2]),
        Double.parseDouble(sp[3])
        );
    }
    
    public String toCSV() {
        return this.latmin+";"+this.latmax+";"+this.lonmin+";" + this.lonmax;
    }

    /**
     * Form Boundaries from a splitted String that contains latmin,
     * latmax,lonmin,lonmax in the default order.
     *
     * @param split the Strings that contains at least 4 parseable doubles.
     * @param startIndex the index that contains 'latmin'. The rest of the
     * doubles must be found in the default sequence after this one.
     */
    public Boundaries(String[] split, int startIndex) {
        this.latmin = Double.parseDouble(split[startIndex]);
        this.latmax = Double.parseDouble(split[startIndex + 1]);
        this.lonmin = Double.parseDouble(split[startIndex + 2]);
        this.lonmax = Double.parseDouble(split[startIndex + 3]);

        double midlat = (latmax + latmin) / 2.0;
        lonScaler = Math.cos(midlat / 360 * 2 * Math.PI);
    }

    public Boundaries(double[] boxCoordinates) {
        this(
                boxCoordinates[0],//latmin
                boxCoordinates[1],//latmax
                boxCoordinates[2],//lonmin
                boxCoordinates[3]//lonmax
        );
    }

    public String toText() {
        return this.latmin + "N to " + this.latmax + "N, " + this.lonmin + "E to " + this.lonmax + "E.";
    }
    
        public String toText(int prec) {
        return    FuserTools.editPrecision(this.latmin,prec) + "N to " 
                + FuserTools.editPrecision(this.latmax,prec) + "N, " 
                + FuserTools.editPrecision(this.lonmin,prec) + "E to " 
                + FuserTools.editPrecision(this.lonmax,prec) + "E.";
    }

    /**
     * Forms a String 'latmin_latmax_lonmin_lonmax_'.
     *
     * @param d if non-null, then the doubles are rounded up with the specified
     * amount of decimals.
     * @return string representation of bounds suitable for filename parts.
     */
    public String toText_fileFriendly(Integer d) {
        if (d == null) {
            return this.latmin + "_" + this.latmax + "_" + this.lonmin + "_" + this.lonmax + "_";
        } else {
            return FuserTools.editPrecision(this.latmin, d) + "_"
                    + FuserTools.editPrecision(this.latmax, d) + "_"
                    + FuserTools.editPrecision(this.lonmin, d) + "_"
                    + FuserTools.editPrecision(this.lonmax, d) + "_";
        }
    }

    public static Boundaries getDefault() {
        return new Boundaries(0, 1, 0, 1);
    }

    public Boundaries clone(String newName) {
        Boundaries b = this.clone();
        b.name = newName;
        return b;
    }

    @Override
    public Boundaries clone() {
        Boundaries b = new Boundaries(this.latmin, this.latmax, this.lonmin, this.lonmax);
        b.name = name;
        return b;
    }

    public Boundaries(double lat, double lon) {
        this.latmax = lat;
        this.latmin = lat;
        this.lonmax = lon;
        this.lonmin = lon;

        double midlat = (latmax + latmin) / 2.0;
        lonScaler = Math.cos(midlat / 360 * 2 * Math.PI);
    }

    public void expandIfNeeded(double lat, double lon) {
        if (lat > latmax) latmax = lat;
        if (lat < latmin) latmin = lat;
        if (lon > lonmax) lonmax = lon;
        if (lon < lonmin) lonmin = lon;
    }
    
    public double getMidLat() {
        return (latmax + latmin) / 2.0;
    }

    public double getMidLon() {
        return (lonmax + lonmin) / 2.0;
    }

    public int[] getMaxHWFromRes(int res) {

        double wLength = (this.lonmax - this.lonmin) * this.lonScaler;
        double hLength = (this.latmax - this.latmin);

        int height;
        int width;

        if (wLength > hLength) {
            width = res;
            height = (int) (hLength / wLength * res);
        } else {
            height = res;
            width = (int) (wLength / hLength * res);
        }

        int[] temp = {height, width};
        return temp;

    }

    /**
     * Check whether or not a fixed point overlaps or touches with this
     * Boundaries.
     *
     * @param lat the point's latitude
     * @param lon the point's longitude
     * @return boolean value, true if some degree of overlap was found.
     */
    public boolean intersectsOrContains(double lat, double lon) {

        if (lat <= latmax && lat >= latmin && lon >= lonmin && lon <= lonmax) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Check whether or not this Boundaries overlaps or touches with another
     * Boundaries.
     *
     * @param b the bounds to test intersection.
     * @return boolean value, true if some degree of overlap was found.
     */
    public boolean intersectsOrContains(Boundaries b) {
        boolean inters = intersectsOrContains(b, this);
        if (inters) {
            return true;
        }

        inters = intersectsOrContains(this, b);
        if (inters) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean intersectsOrContains(Boundaries b, Boundaries bthis) {

        boolean intersects = bthis.intersectsOrContains(b.latmin, b.lonmin);
        if (intersects) {
            return true;
        }

        intersects = bthis.intersectsOrContains(b.latmax, b.lonmax);
        if (intersects) {
            return true;
        }

        intersects = bthis.intersectsOrContains(b.latmin, b.lonmax);
        if (intersects) {
            return true;
        }

        intersects = bthis.intersectsOrContains(b.latmax, b.lonmin);
        if (intersects) {
            return true;
        }

        return false;

    }

    public double getDlat(int h, int w) {
        return (this.latmax - this.latmin) / h;
    }

    public double getDlon(int h, int w) {
        return (this.lonmax - this.lonmin) / w;
    }

    public double getFullArea_km2() {

        double latRange = (latmax - latmin);
        double lonRange = (lonmax - lonmin);

        double dlatInKm = latRange * degreeInKM;
        double dlonInKm = lonRange * degreeInKM * lonScaler;
        double area = dlatInKm * dlonInKm;
        //EnfuserLogger.log(Level.FINER,this.getClass(),"Cell area: "+area+"km2");
        return area;
    }

    public float getCellArea_km2(int h, int w) {
        return (float) this.getFullArea_km2() / (h * w);
    }

    /**
     * Increase or decrease the boundary size based on the given scaling value.
     *
     * @param amount A value of 0 does nothing, and a value of 1 effectively
     * increases the area to a 9 times larger area. The value must be larger
     * than -0.5. This works by operating the ranges, for example by computing
     * (latmax-latmin)*amount, and adding this to the latmax and reducing this
     * from latmin. This means that a slight uniform increase in the area is
     * achieved by giving a value of 0.1, or, a slight reduction is achieved by
     * giving -0.1.
     *
     */
    public void expand(double amount) {

        double dlat = (latmax - latmin) * amount;
        double dlon = (lonmax - lonmin) * amount;

        this.latmax = latmax + dlat;
        this.latmin = latmin - dlat;
        this.lonmax = lonmax + dlon;
        this.lonmin = lonmin - dlon;

        //EnfuserLogger.log(Level.FINER,this.getClass(),"boundary coordinates expanded by "+amount);
    }

    public double latRange() {
        return (this.latmax - this.latmin);
    }

    public double lonRange() {
        return this.lonmax - this.lonmin;
    }

    /**
     * Check whether the given Boundaries instance is fully contained within
     * this Boundaries.
     *
     * @param b the bounds to be tested.
     * @return true if 'b' is fully contained within this instance.
     */
    public boolean fullyContains(Boundaries b) {
        if (!this.intersectsOrContains(b.latmin, b.lonmin)) return false;
        //the LowerLeft corner of b was not within this bounds => false;
        if (!this.intersectsOrContains(b.latmax, b.lonmax)) return false;
        //the UpperRight corner of b was not within this bounds => false;
        return true;
    }

    public double[] toDoubles() {
        return new double[]{
            this.latmin,
            this.latmax,
            this.lonmin,
            this.lonmax
        };

    }

    public void editPrecisions(int i) {
        this.latmax = FuserTools.editPrecision(latmax, i);
        this.latmin = FuserTools.editPrecision(latmin, i);

        this.lonmax = FuserTools.editPrecision(lonmax, i);
        this.lonmin = FuserTools.editPrecision(lonmin, i);

    }

    public void flatExpand_km(double km) {
       double addLat = km*1000.0/DEGREE_IN_METERS;
       double addLon = km*1000.0/(DEGREE_IN_METERS*this.lonScaler);
       
       this.latmax+= addLat;
       this.latmin-= addLat;
       
       this.lonmax +=addLon;
       this.lonmin -= addLon;
    }

    /**
     * A consistent boundaries have greater than zero lat and lon -ranges.
     * @return 
     */
    public boolean consistent() {
       if (this.latRange()<0) return false;
       if (this.lonRange()<0) return false;
       return true;
    }
    
}
