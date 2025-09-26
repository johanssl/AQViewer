/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.fmi.aq.enfuser.api.EnfuserAPI.URLBASE;
import org.fmi.aq.essentials.geoGrid.Boundaries;

/**
 *
 * @author johanssl
 */
public class AreaMeta {
    
    public static AreaMeta getMeta(String token) {
        try {
            String address = URLBASE+"regions-areas";
            String resp = EnfuserAPI.httpsGet(address, token);
            return new AreaMeta(resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
        final String resp;
        ArrayList<String> areas = new ArrayList<>();
        ArrayList<String> regions = new ArrayList<>();
        ArrayList<String[]> timeRange = new ArrayList<>();
        ArrayList<String> box = new ArrayList<>();
        ArrayList<String> statusStr = new ArrayList<>();
        ArrayList<String[]> areaVars = new ArrayList<>();
        
        public AreaMeta(String resp) {
            this.resp ="{\"content\":"+resp +"}";//hack the response into a JSON Map of objects first
             //parse content
             System.out.println(this.resp);
         ObjectMapper objectMapper = new ObjectMapper();  
            try {
                Map<String, Object> map = objectMapper.readValue(this.resp,
                   new TypeReference<Map<String,Object>>(){});

                //areas
                ArrayList<Object> arr = (ArrayList)map.get("content");
                
                for (Object o:arr) {
                    LinkedHashMap<String,Object> lm = (LinkedHashMap)o;
                    String reg = lm.get("region").toString();
                    System.out.println("REGION - "+ reg);
                    
                    //get areas
                    ArrayList<Object> areaList = (ArrayList)lm.get("areas");
                    for (Object oa:areaList) {
                        LinkedHashMap<String,Object> areaHash = (LinkedHashMap)oa;
                        String name = areaHash.get("name").toString();
                        System.out.println("\t"+name);
                        
                        LinkedHashMap<String,Object> timeR = (LinkedHashMap)areaHash.get("timeRange");
                        String start = timeR.get("startTime").toString();
                        String end = timeR.get("endTime").toString();
                        ArrayList<Object> vars = (ArrayList)areaHash.get("variables");
                     
                        String[] varList = new String[vars.size()];
                        int k =-1;
                        for (Object ob:vars) {
                            k++;
                            varList[k]=ob.toString();
                        }
                        String status = areaHash.get("status").toString();
                        String bb = areaHash.get("coordinates").toString();
                        
                        this.areas.add(name);
                        this.areaVars.add(varList);
                        this.box.add(bb);
                        this.regions.add(reg);
                        this.statusStr.add(status);
                        this.timeRange.add(new String[]{start,end});
                    }//for areas
                }//for regions
                
            } catch (Exception ex) {
              ex.printStackTrace();
            }   
        }
        
        public void printout() {
            for (int i =0;i<areas.size();i++) {
                String start = timeRange.get(i)[0];
                String end = timeRange.get(i)[1];
                String v="";
                for (String s:this.areaVars.get(i)) v+=s+",";
                
                System.out.println("\n"+regions.get(i) +" - "+ areas.get(i));
                System.out.println("\t\t timespan: "+ start +" - "+end);
                System.out.println("\t\t status: "+ this.statusStr.get(i));
                System.out.println("\t\t variables: "+v);
                System.out.println("\t\t box: "+this.box.get(i) +"\n");
            }  
        }
        
        public Boundaries getBounds(String area) {
            int ind = getIndex(area);
            String bb = this.box.get(ind);//e.g., [60.37804498518611, 22.043908814589667], [60.52304498518611, 22.463908814589665]]
            //just a little bit of hacking...
            String clean = bb.replace("[", "");
            clean = clean.replace("]", "");
            clean = clean.replace(" ", "");
            String[] sp = clean.split(",");
            //we need to swap [1] and [2]
            String lonmin =sp[1]+"";
            sp[1] = sp[2];
            sp[2]=lonmin;
            return new Boundaries(sp,0);
        }

    int getIndex(String area) {
       int k =-1;
        for (String s:this.areas) {
            k++;
            if (s.equals(area))return k;
        }
        return -1;
    }

    String[] getPollutantVars(int ind) {
        ArrayList<String> arr = new ArrayList<>();
        for (String s:this.areaVars.get(ind)) {

            if (s.contains("O3") || s.contains("NO") || s.contains("PM") 
                    || s.contains("BC") || s.contains("LDSA") || s.contains("PNC") || s.contains("AQI")) {
                arr.add(s);
            }
        }
        
        String[] vars = new String[arr.size()];
        for (int i =0;i<vars.length;i++) vars[i]=arr.get(i);
        return vars;
    }
       
    
}
