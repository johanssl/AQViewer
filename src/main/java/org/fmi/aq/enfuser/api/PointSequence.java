/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.fmi.aq.enfuser.ftools.FileOps;
import static org.fmi.aq.enfuser.ftools.FuserTools.editPrecision;

/**
 *
 * @author johanssl
 */
 public class PointSequence {

        final String resp;
        String warnings ="";
        double lat,lon;
        HashMap<String,String> units= new HashMap<>();
        HashMap<String,PointData> timeData= new HashMap<>();
        
        boolean failed = false;
        
        ArrayList<String> times = new ArrayList<>();
        PointData first = null;
        public PointSequence(String resp) {
          this.resp = resp;
         //parse content
         ObjectMapper objectMapper = new ObjectMapper();  
            try {
                Map<String, Object> map = objectMapper.readValue(resp,
                   new TypeReference<Map<String,Object>>(){});

                //coordinates
                this.lat = Double.parseDouble(map.get("latitude").toString());
                this.lon = Double.parseDouble(map.get("longitude").toString());
                //units
                LinkedHashMap<String,Object> uns = (LinkedHashMap)map.get("units");
                for (String key:uns.keySet()) {
                   String unit = uns.get(key).toString();
                   units.put(key, unit);
                }
                
                //data
                ArrayList<Object> data = (ArrayList)map.get("data");
                for (Object o:data) {
                    PointData ad = new PointData((LinkedHashMap<String,Object>)o, units);
                    this.timeData.put(ad.date, ad);
                    this.times.add(ad.date);
                    if (first==null) first = ad;
                }

            } catch (Exception ex) {
              if (resp.contains("inside a building"))  {
                 this.warnings = "Location is inside a building."; 
              } else {
                ex.printStackTrace(); 
                this.warnings ="Point query failed.";
              }
              
              failed = true;
            }
            
        }
        public void printout() {
           System.out.println("\n---ORIGINAL RESPONSE---\n"+resp+ "\n---------------------------");
           System.out.println(this.warnings+"\n");
           System.out.println("\tGot "+ this.timeData.size() +" timeDatapacks for "+ lat+", "+lon 
                        +". units size: "+ units.size());
           
           for (PointData ad:this.timeData.values()) {
               System.out.println("\n");
               ad.printout(true,false);
           }
            
        }

    PointData getForTime(String date) {
       String modded = date.replace(":00:00Z", ":00Z");
       if (this.timeData.containsKey(modded)) return this.timeData.get(modded);
       return this.timeData.get(date);
    }

    String shortInfo(String date) {
        PointData pd = getForTime(date);
        return pd.printout(true, true);
    }
    
    private String getLocalFileName(String format) {
        String firstDate = times.get(0);
        String lastDate = times.get(times.size()-1);
        String fname = editPrecision(lat,4)+"_"+editPrecision(lon,4) + "_"
                + firstDate.replace(":","-")+"_"+lastDate.replace(":", "-")
                +format;
        return fname;
    }

    public File saveToFile(String dir, boolean csv) {
        if (first == null) {
            System.out.println("Cannot store point query (it is empty)");
            return null;
        }
        
        String format = ".json";
        ArrayList<String> lines = new ArrayList<>();
        if (csv) {
            format = ".csv";
            Set<String> metKeys = first.meteorology.keySet();//use these for all PointData
            //what components should be printed for each variable?
            HashMap<String,HashSet<String>> var_comps = new HashMap<>();
            for (PointData pd:this.timeData.values()) {
                for (String var:pd.values.keySet()) {
                    for (String comp:pd.compNames) {
                        Double compVal = pd.getComponent(var, comp);
                        if (compVal!=null) {
                            //found a value, map this.
                            HashSet<String> ch = var_comps.get(var);
                            if (ch==null) {
                                ch = new HashSet<>();
                                var_comps.put(var, ch);
                            }//if new
                           ch.add(comp);
                        }//if found
                    }//for all possible components
                }//for all vars
            }
           
            //build column names as the first line
           Set<String> allVars = var_comps.keySet();
           if (allVars.isEmpty()) allVars = first.values.keySet();//if the response has no components, then use this instead.
           String header ="TIME_UTC;TIME_LOCAL;";
           
           for (String time:times){
               PointData pd = this.timeData.get(time);
               String line =pd.date+";"+ pd.localDate+";";
               
               for (String var:allVars) {//basic pollutant concentrations
                   Double d = pd.values.get(var);
                   if (d!=null)line+=d;
                   line+=";";
                   if (lines.isEmpty()) header+=var + " [" + pd.unit(var)+"];";
               }
               
               //meteorology
               for (String mk:metKeys) {
                   Double d = pd.meteorology.get(mk);
                   if (d!=null)line+=d;
                   line+=";";
                   if (lines.isEmpty()) header+=mk + " [" + pd.unit(mk)+"];";
               }
               
               //components
               for (String var:allVars) {
                   HashSet<String> h=var_comps.get(var);
                   if (h==null) continue;
                   for (String comp:h) {
                       Double d = pd.getComponent(var, comp);
                       if (d!=null)line+=d;
                       line+=";";
                       if (lines.isEmpty()) header+=var +"_" +  comp  + " [" + pd.unit(var)+"];";
                   }     
               }
              
               //regional
               if (!pd.regional.isEmpty())for (String var:allVars) {
                    Double d = pd.regional.get(var);
                    if (d!=null)line+=d;
                    line+=";";
                    if (lines.isEmpty()) header+="REGIONAL " + var + " [" + pd.unit(var)+"];";
               }
               //elevated
               if (!pd.valAt100m.isEmpty())for (String var:allVars) {
                    Double d = pd.valAt100m.get(var);
                    if (d!=null)line+=d;
                    line+=";";
                    if (lines.isEmpty()) header+=var + "at100m [" + pd.unit(var)+"];";
               }
               
             if (lines.isEmpty()) lines.add(header);  
             lines.add(line);  
           }//for time
           
            
        } else {
            lines.add(this.resp);
        }
        
        File f = new File(dir+this.getLocalFileName(format));
        FileOps.printOutALtoFile2(f, lines, false);
        System.out.println("\nSaved query to file: "+f.getAbsolutePath());
        return f;   
    }
    

}
