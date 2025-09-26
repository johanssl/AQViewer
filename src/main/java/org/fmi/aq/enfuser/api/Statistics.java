/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import static org.fmi.aq.enfuser.api.EnfuserAPI.GROUPS;
import static org.fmi.aq.enfuser.api.EnfuserAPI.URLBASE;
import static org.fmi.aq.enfuser.api.EnfuserAPI.VAR_FILTER;
import static org.fmi.aq.enfuser.api.EnfuserAPI.WAIT_MS;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;

/**
 *
 * @author johanssl
 */
public class Statistics {
     
    /**
    * Make a query for Enfuser point service API for the given time span and location.
    * @param token your access token
    * @param lat latitude coordinate
    * @param lon longitude coordinate
    * @param date1 optional start time (if null then 'now' is used as default)
    * @param date2 optional end time (if start time is null then this should also
    * be null)
    * @return String JSON response
    * @throws Exception something went wrong
    */
    private static String fetchRawStats(String token, double lat, double lon,
            String date1, String date2) throws Exception {
        
        if (date1==null || date2==null) {
            System.out.println("Dates NOT specified");
            return null;
        }
        
        String address = URLBASE+"point-statistics?lat="
                +lat+"&lon="+lon+"&startTime="+date1+"&endTime="+date2
                +GROUPS+VAR_FILTER;
        
       return EnfuserAPI.httpsGet(address, token);
} 
    
   public static Statistics fetchStats(String token, double lat, double lon,
            String date1, String date2) {
       try {
                String resp = fetchRawStats(token,lat, lon, date1, date2);
                 EnfuserLogger.sleep(WAIT_MS, Statistics.class);
                return new Statistics(resp); 

       } catch (Exception ex) {
          ex.printStackTrace();
          EnfuserLogger.sleep(WAIT_MS, Statistics.class);
       } 
       return null;
   }    
   
   
    final String resp;
    public Statistics(String resp) {
        this.resp = resp;
        System.out.println(resp);
    }
    
}
