/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.enfuser.ftools;

import org.fmi.aq.essentials.geoGrid.Dtime;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import ucar.nc2.Attribute;
import java.util.logging.Level;
import org.fmi.aq.addons.visualization.VisualOptions;

/**
 * A heterogenious collection of fairly simple methods.
 * Mostly file-related operations.
 * @author Lasse Johansson
 */
public class FuserTools {

    public static final String EMPTY_STRING = "";

    /**
     * Checks data time range validity with respect a timespan of interest.This
 is a brute-force check, iterating one hour at a time. If any hour matched
 within the span this returns true.
     *
     * @param start start time for domain of interest
     * @param end end time for domain of interest
     * @param sysHours_data [start,end] in terms of data date system hours.
     * @return true if an overlap of the time spans was observed, signaling that
     * the dataset is time-wise applicable. Otherwise, returns false.
     */
    public static boolean evaluateSpan(Dtime start, Dtime end, int[] sysHours_data) {

        for (int dh = sysHours_data[0]; dh <= sysHours_data[1]; dh++) {
            if (dh >= start.systemHours() && dh <= end.systemHours()) {
                return true;
            }
        }

        return false;
    }
    /**
     * A simple test method to check if the given h-w index would
     * cause on OutOfBoundsException in the pbject grid.
     * @param dat the grid
     * @param h h index
     * @param w w index
     * @return true if OutOfBounds.
     */
    public static boolean ObjectGridOobs(Object[][] dat, int h, int w) {
        if (h<0 || h > dat.length-1) return true;
        if (w<0 || w > dat[0].length-1) return true;
        return false;
    }

    public static String getUrlContent_old(String address) throws IOException {

        URL hirlam = new URL(address);
        EnfuserLogger.log(Level.FINER,FuserTools.class,"Opening connection to " + address);
        URLConnection yc = hirlam.openConnection();

        InputStream aa = yc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(aa));

        String inputLine;
        String currContent = "";

        int counter = 0;
        while ((inputLine = in.readLine()) != null) {
//EnfuserLogger.log(Level.FINER,FuserTools.class,inputLine); 
            counter++;
            currContent += inputLine;
        }

        in.close();

        EnfuserLogger.log(Level.FINER,FuserTools.class,
                "Read successfully" + counter + " lines from url.\n\n" + currContent);

        return currContent;
    }

    public static String correctDir(String dir) {

        //could it be that the wrong file separator is being used?
        String original = dir + "";
        String other = "/";
        if (FileOps.Z.equals(other)) {
            other = "\\";
        }

        if (dir.contains(other + "")) {
            dir = dir.replace(other + "", FileOps.Z);
        }

        if (!dir.startsWith(FileOps.Z + FileOps.Z)) {// it is safe to remove double file separators
            dir = dir.replace(FileOps.Z + FileOps.Z, FileOps.Z);
        }

        String finalDir;
        if (dir.endsWith(FileOps.Z)) {
            finalDir = dir;//all ok
        } else {
            finalDir = dir + FileOps.Z;
        }
        if (!original.equals(finalDir)) {
            EnfuserLogger.log(Level.FINER,FuserTools.class,
                    "correctDir: a correction has been made: " + original + " => " + finalDir);
        }
        return finalDir;
    }
    
    /**
     * Recursive delete method to clear a directory with sub-directories.
     * Deletion will occur only for the given file types (for safety) 
     * @param directoryToBeDeleted
     * @param delTypes
     * @return 
     */
    public static boolean deleteDirectoryContents(File directoryToBeDeleted, String[] delTypes) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
        for (File file : allContents) {
           deleteDirectoryContents(file, delTypes);
        }
    }
    
    boolean ok = false;
    for (String s:delTypes) {
        if (directoryToBeDeleted.getName().endsWith(s)) ok = true;
    }

    if (directoryToBeDeleted.isDirectory())ok=true;
    
    if (ok) {
      EnfuserLogger.log(Level.FINE, FuserTools.class,
              "Deleting: "+ directoryToBeDeleted.getAbsolutePath());
      return directoryToBeDeleted.delete(); 
    } else {
        return false;
    }
  
}
    
    
    public static File findFileThatContains(String rootdir, String[] contains) {
        return findFileThatContains(rootdir, contains,false);
    }

    public static File findFileThatContains(String rootdir, String[] contains, boolean lenient) {
        File f = null;
        File root = new File(rootdir);
        int k = 0;
        EnfuserLogger.fineLog("Finding files with '"+contains[0] +"' from all subdirectories of "+ rootdir);
        try {
            boolean recursive = true;

            Collection files = FileUtils.listFiles(root, null, recursive);

            for (Iterator iterator = files.iterator(); iterator.hasNext();) {
                File file = (File) iterator.next();
                boolean containsAll = true;
                for (String test : contains) {
                    
                    String cont =test;
                    String fn = file.getName();
                    if (lenient) {
                        cont = cont.toLowerCase();
                        fn = fn.toLowerCase();
                    }
                    if (!fn.contains(cont)) {
                        containsAll = false;
                    }
                }
                k++;
                if (containsAll) {
                    EnfuserLogger.log(Level.FINER,FuserTools.class,
                            "Found the file matching description, k = " + k);
                    return file;
                }
            }
        } catch (Exception e) {
           EnfuserLogger.log(e,Level.SEVERE,FuserTools.class,
                   "File search encountered an error from "+ rootdir);
        }
        EnfuserLogger.log(Level.FINER,FuserTools.class,"Searched " + k + " files without match.");
        return f;
    }

    public static void clearDirContent(String dir) {
        EnfuserLogger.log(Level.FINER,FuserTools.class,"Clearing content of " + dir);
        File f = new File(dir);
        File[] files = f.listFiles();
        EnfuserLogger.log(Level.FINER,FuserTools.class,files.length + " files inside.");

        for (File ff : files) {
            if (!ff.isDirectory()) {
                ff.delete();
            } else if (ff.getName().contains("QC_locs")) {
                ff.delete();
            }
        }

    }

    public static float[][] transform_byteFloat(byte[][] dat) {
        float[][] ndat = new float[dat.length][dat[0].length];

        for (int h = 0; h < dat.length; h++) {
            for (int w = 0; w < dat[0].length; w++) {
                ndat[h][w] = (float) dat[h][w];
            }
        }
        return ndat;
    }

    public static float editPrecisionF(double value, int precision) {

        if (precision >= 0) {
            long prec = (long) Math.pow(10, precision);
            long temp = (long) (prec * value);
            float result = (float) temp / prec;
            return result;
        } else {
            // reduction in precision, e.g. 12345 => 12000
            int divisor = (int) Math.pow(10, Math.abs(precision));
            int temp = (int) (value / divisor);
            float result = temp * divisor;
            return result;

        }

    }

    public static void copyfile(String srFile, String dtFile) 
            throws FileNotFoundException, IOException {
      
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            EnfuserLogger.log(Level.FINER,FuserTools.class,"File copied.");
       
    }


    public static void deleteFile(String fileName) {
        // A File object to represent the filename
        boolean doIt = true;
        File f = new File(fileName);

        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) {
            doIt = false;
            EnfuserLogger.log(Level.WARNING,FuserTools.class,
                    "Delete: no such file or directory: " + fileName);
        }

        if (!f.canWrite()) {
            doIt = false;
            EnfuserLogger.log(Level.WARNING,FuserTools.class,
                    "Delete: write protected: " + fileName);
        }

        // Attempt to delete it
        boolean success = false;
        if (doIt) {
            success = f.delete();
        }

        if (!success) {
            throw new IllegalArgumentException("Delete: deletion failed");
        }
    }

    public static final void unzip(String file, String rootdir) {
        Enumeration entries;
        ZipFile zipFile;

        try {
            zipFile = new ZipFile(file);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
// Assume directories are stored parents first then children.
                    EnfuserLogger.log(Level.FINE,FuserTools.class,"Extracting directory: " + entry.getName());
// This is not robust, just for demonstration purposes.
                    (new File(entry.getName())).mkdir();
                    continue;
                }
                EnfuserLogger.log(Level.FINE,FuserTools.class,"Extracting file: " + entry.getName());

                String outFile = rootdir + entry.getName();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
                InputStream in = zipFile.getInputStream(entry);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();

            }
            zipFile.close();
        } catch (IOException ioe) {
            EnfuserLogger.log(Level.WARNING,FuserTools.class,
                    "File unzip unsuccessful: "+ file +" => "+ rootdir);
        }
    }

    /**
     * Returns a FilenameFilter which accepts files with the given extension.
     *
     * @param extension The file extension (e.g. "csv"). Not case sensitive.
     * @return FilenameFilter accepting files with the extension.
     */
    public static FilenameFilter getFileExtensionFilter(String extension) {
        return new FilenameFilter() {
            public boolean accept(File file, String name) {
                return name.toLowerCase().endsWith("." + extension.replace(".", "")); // some tolerance for usage
            }
        };
    }
    
    
      public static String tab(String s, int len) {
        while (s.length() < len) {
            s += " ";
        }
        return s;
    }

    public static String tab(String s) {
        while (s.length() < 15) {
            s += " ";
        }
        return s;
    }
    
    // parent folders of dest must exist before calling this function
    public static void copyTo(File src, File dest) throws IOException {

            FileInputStream fileInputStream = new FileInputStream(src);
            FileOutputStream fileOutputStream = new FileOutputStream(dest);

            int bufferSize;
            byte[] bufffer = new byte[512];
            while ((bufferSize = fileInputStream.read(bufffer)) > 0) {
                fileOutputStream.write(bufffer, 0, bufferSize);
            }
            fileInputStream.close();
            fileOutputStream.close();

    }

    public static ArrayList<Attribute> getNetCDF_attributes(Dtime lastObservationTime) {
        ArrayList<Attribute> netAttr = new ArrayList<>();
        Dtime sysDt = Dtime.getSystemDate_utc(false, Dtime.STAMP_NOZONE);
        netAttr.add(new Attribute("Conventions", "CF-1.0"));
        netAttr.add(new Attribute("institution", "Finnish Meteorological Institute"));
        netAttr.add(new Attribute("creator", "Lasse Johansson, email lasse.johansson@fmi.fi"));
        //netAttr.add(new Attribute("history", "model version: " + DataCore.VERSION_INFO + ", file produced " + sysDt.getStringDate(Dtime.STAMP_NOZONE) + "Z"));
        if (lastObservationTime != null) {
            netAttr.add(new Attribute("info", "last observed datapoint: " + lastObservationTime.getStringDate(Dtime.STAMP_NOZONE) + "Z"));
        }
        netAttr.add(new Attribute("title", "Modelled pollutant concentrations and supplemental data according to FMI-ENFUSER"));
        return netAttr;
    }

    
     public static ArrayList<Integer> arrayConvert(ArrayList<Byte> arr) {
        ArrayList<Integer> ar2 = new ArrayList<>();
        for (byte b : arr) {
            ar2.add((int) b);
        }
        return ar2;
    }

    public static ArrayList<Byte> arrayConvert2(ArrayList<Integer> arr) {
        ArrayList<Byte> ar2 = new ArrayList<>();
        for (int b : arr) {
            ar2.add((byte) b);
        }
        return ar2;
    }
    
    /**
     * Wait for the given amount of seconds and return a manually specified system
     * input line.
     * This method can be used to query user input in cases where a user is present
     * and manual input is required to proceed further.
     * @param waitSeconds amount of time in seconds allowed for input typing.
     * @return manually types input String. Returns null if none was given.
     */
    public static String readSystemInput(int waitSeconds) {
        String resp =null;
        int x = waitSeconds; // wait 20 seconds at most
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            long startTime = System.currentTimeMillis();
            try {
                while ((System.currentTimeMillis() - startTime) < x * 1000
                        && !in.ready()) {
                }

                if (in.ready()) {
                    resp = in.readLine();
                    EnfuserLogger.log(Level.INFO,FuserTools.class,"Input given: " + resp);
                } else {
                    //rule 3: if manual and no input has been defined, assume 'flex'
                    EnfuserLogger.log(Level.INFO,FuserTools.class,"Input was not given.");
                    return null;
                }

            } catch (IOException ex) {
                 EnfuserLogger.log(ex,Level.WARNING,FuserTools.class,"Input read failure!");
            }

     return resp;   
    }
    
    
    public static Dtime[] fullpreviousMonth(Dtime dt) {
        Dtime end = dt.clone();
        end.addSeconds(-1*(end.sec + end.min*60 + end.hour*3600 + (end.day-1)*24*3600));
        end.addSeconds(-3600);
        //System.out.println("last month's last hour (END): "
        //        +end.getStringDate_noTS());
        
        Dtime start = end.clone();
        start.addSeconds(-1*(start.sec + start.min*60 + start.hour*3600 + (start.day-1)*24*3600));
        //System.out.println("last month's first hour (START): "
        //        + start.getStringDate_noTS());
        return new Dtime[]{start,end};
    }
    
    private final static String CONCAT_NAME ="concat.csv";
    public static void concatenateCSVFiles(String dir, boolean headerRem, String[] replacements) {
        ArrayList<String> lines = new ArrayList<>();
        File f = new File(dir);
        int k =0;
        for (File test:f.listFiles()) {
            String name = test.getName();
            if (!name.endsWith(".csv")) continue;
            if (name.equals(CONCAT_NAME)) continue;
            k++;
            System.out.println("Added as "+ k+": "+ name);
            ArrayList<String> arr = FileOps.readStringArrayFromFile(test);
            if (!lines.isEmpty() && headerRem) {
                arr.remove(0);//remove header, it has been added once.
            }
            
            if (replacements!=null) {
                ArrayList<String> temp = new ArrayList<>();
                for (String line:arr) {
                    
                for (int i =0;i<replacements.length;i+=2) {
                    String target = replacements[i];
                    String rep = replacements[i+1];
                    line = line.replace(target, rep);
                        
                    }//for mods
                temp.add(line);
                }//for lines
                arr = temp;
            }//if mods
            
            lines.addAll(arr);
        }
        
        FileOps.printOutALtoFile2(dir, lines, CONCAT_NAME, false);
        System.out.println("Concat Done.");
    }
    
    public static float sum(float[] ex) {
         if (ex==null) return 0;
        float sum =0;
        for (float f:ex) {
            sum+=f;
        }
        return sum;
    }
    
    public static Object deepClone(Object object) throws IOException, ClassNotFoundException {
        //stream any object into byte array. Then read a new instance of the object with fully cloned properties.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.flush();
        oos.close();
        bos.close();
        byte[] byteData = bos.toByteArray();
        //Restore your class from a stream of bytes:

        ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
        return new ObjectInputStream(bais).readObject();
    
    }

    public static float[] copyArray(float[] orig) {
       float[] copy = new float[orig.length];
       for (int k =0;k<copy.length;k++) {
           copy[k]=orig[k];
       }
       return copy;
    }

    /**
     * Fetch String that lies between identifiers.
     *
     * @param full Text that content is searched from
     * @param before MUST BE UNIQUE! a subString that is known to occur before
     * target
     * @param after a subString that is known to occur after the target
     * @return target String
     */
    public static String getBetween(String full, String before, String after) {
        //  "<img class=\"leaflet-tile leaflet-tile-loaded\" src=\"./OpenStreetMap_files/4738(7).png\" style=\"height: 256px; width: 256px; left: 339px; top: 257px;\">\n";
        //before = "width:
        //after = px;
        try {
            String[] temp = full.split(before); // temp[1] = : 256px; left: -1197px; top: 2561px;">
            String[] temp2 = temp[1].split(after); //temp2[0] =  256;
            return temp2[0];
        } catch (ArrayIndexOutOfBoundsException w) {
            return null;
        }
    }

    /** this tool method edits the number of decimals of d2 double
     *   int precision is the number of decimals wanted
     * @param value
     * @param precision
     * @return
     */
    public static double editPrecision(double value, int precision) {
        int prec = (int) Math.pow(10, precision);
        int temp = (int) (prec * value);
        double result = (double) temp / prec;
        return result;
    }
    
    public static String shortenPrec(double value) {
        return shortenPrec(value,0);
    }
    
    public static String shortenPrec(double value, VisualOptions vops) {
        if (vops.valueScale_precisionFloater) {
            if (value==0) return "0";
            return String.format("%."+vops.valueScale_precisionAdd+"E", value);
        }
        return shortenPrec(value,vops.valueScale_precisionAdd);
    }
    
     private static String shortenPrec(double value, int pAdd) {
         if (pAdd<0) pAdd =0;
         double v = Math.abs(value);//for assessing precision.
         if (v==0) return "0";
         int p =-5;
         boolean asFloat = true;
         if (v < 10000000) {
             p = -5;
             asFloat = false;
         }
         if (v < 1000000) p = -4;
         if (v < 100000) p = -3;
         if (v < 10000) p = -2;
         if (v < 1000) p = -1; 
         if (v < 100) p = 0;
         if (v < 10) p=1;
         if (v < 1) p=1;
         if (v < 0.1) p=2;
         if (v < 0.01) p=3;
       
         p+=pAdd;

         
         if (p<0) {//this is a large value
             
             if (asFloat) {
                 String s = (float)value +"";
                 int prLen =3+pAdd;
                 int totLen = 5+ pAdd;
                 if (value <0) {
                     prLen++;
                     totLen++;
                 }
                 if (s.length()> totLen && s.contains("E")) {
                     String prior = s.split("E")[0];
                     String post = s.split("E")[1];
                     prior = prior.substring(0, prLen);
                     return prior +"E"+post; 
                 }
             }
             
             long div = (long)Math.pow(10, p*-1);
             long i= (long)(value/div)*div;
             return i+"";
                     
         } else if (p==0) {
             return (int)value+"";
             
         } else {
             double d= editPrecision(value,p);
             return d+"";
         } 
     }
     
     
     
     
     public static void main(String[] args) {
         double d = 0.000001;
         for (int i =0;i<100;i++) {
             d = d*1.6;
             String line = d +" => ";
             for (int p = 0;p<=3;p++) line+= shortenPrec(d,p) +"  ";
             System.out.println(line);
         }
         
         d = -0.000001;
         System.out.println("\n------------NEG -----------------\n");
         for (int i =0;i<100;i++) {
             d = d*1.6;
             String line = d +" => ";
             for (int p = 0;p<=3;p++) line+= shortenPrec(d,p) +"  ";
             System.out.println(line);
         }
         
     }
     
     
     
     
    
}
