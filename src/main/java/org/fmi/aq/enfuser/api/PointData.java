/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 *
 * @author johanssl
 */
public class PointData {
        
        HashMap<String,String> units;
        String date;
        String localDate;
        HashMap<String,Double> meteorology = new HashMap<>();
        HashMap<String,Double> regional = new HashMap<>();
        public HashMap<String,Double> components = new HashMap<>();
        HashMap<String,Double> values = new HashMap<>();
        HashMap<String,Double> valAt100m = new HashMap<>();
        HashSet<String> compNames = new HashSet<>();
        
        public PointData(LinkedHashMap<String,Object> map, HashMap<String,String> units) {
            this.units = units;
            this.date = map.get("date").toString();
             this.localDate = map.get("localDate").toString();
            LinkedHashMap<String,Object> valhash = (LinkedHashMap)map.get("values");
            
            //meteorology
            ArrayList<Object> mets = (ArrayList)valhash.get("meteorology");
            if (mets!=null) 
            for (Object m:mets) {
                LinkedHashMap<String,Object> mh = (LinkedHashMap)m;
                String var = mh.get("name").toString();
                double val = Double.parseDouble(mh.get("value").toString());
                this.meteorology.put(var, val);
            }
            
            //pollutant species
            ArrayList<Object> pols = (ArrayList)valhash.get("pollutants");
            for (Object p:pols) {
                LinkedHashMap<String,Object> ph = (LinkedHashMap)p;
                String var = ph.get("name").toString();
                double val = Double.parseDouble(ph.get("value").toString());
                this.values.put(var, val);
                
                Object eleVal = ph.get("valueAt100m");
                if (eleVal!=null) {
                    double eval = Double.parseDouble(eleVal.toString());
                    this.valAt100m.put(var, eval);
                }
                Object rego = ph.get("regional");
                if (rego!=null) {
                    double regval = Double.parseDouble(rego.toString());
                    this.regional.put(var, regval);
                }
                
                //components
                if (ph.containsKey("components")) {
                    try {
                    LinkedHashMap<String,Object> cm = (LinkedHashMap)ph.get("components");
                    for (String key:cm.keySet()) {
                        double cval = Double.parseDouble(cm.get(key).toString());
                        //System.out.println("key= "+key);
                        String compName = key;
                        this.components.put(var+"_"+compName, cval);
                        this.compNames.add(compName);
                    }
                    } catch (Exception exe) {
                        
                    }
                }//if components
            }//for pollutants
        }
        
        public String printout(boolean print, boolean simple) {
            String s = ("date:"+date +" (local: "+this.localDate+")\n");
            
            //Enfuser values and regional
            for (String var:this.values.keySet()) {
                double val = this.values.get(var);
                s+=("\t"+var +": "+val   +" ["+units.get(var)+"]\n");
                
                //components
                for (String compName:this.compNames) {
                    String ckey = var +"_"+compName;
                    Double cval = this.components.get(ckey);
                    if (cval==null) continue;
                    s+=("\t\t "+compName +": "+cval+"\n");
                }
                
                if (!simple)s+=("\tregional: "+this.regional.get(var)+"\n");
                Double ele = this.valAt100m.get(var);
                if (ele!=null && !simple)s+=("\tvalueAt100m: "+ele+"\n");
            }
            
            s+="\nMeteorology:\n";
            for (String var:this.meteorology.keySet()) {//met values
                double val = this.meteorology.get(var);
                if (simple) {
                    if (var.contains("rain") || var.contains("ABLH") || var.contains("temp") || var.contains("windD") || var.contains("windS")) {
                        
                    } else {
                         continue;
                    }  
                }//if simple, skip most
                
                s+=("\t"+var+": "+val +" ["+units.get(var)+"]\n");
            }
            
            if (print)System.out.println(s);
            return s;
        }
        
        public String unit(String var) {
            String s = this.units.get(var);
            if (s!=null) return s.replace("µ", "u");
            return null;
        }

       public Double getComponent(String var, String comp) {
          String key = var +"_"+comp;
          return components.get(key);
       } 
        
    }
