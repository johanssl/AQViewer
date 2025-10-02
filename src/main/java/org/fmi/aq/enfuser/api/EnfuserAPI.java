/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import static org.fmi.aq.enfuser.api.AreaMeta.getMeta;
import org.fmi.aq.enfuser.ftools.EnfuserLogger;
import org.fmi.aq.essentials.geoGrid.Boundaries;

/**
 *
 * @author johanssl
 */
public class EnfuserAPI {
   
    protected final static String URL_ACCOUNT = "https://epk.2.rahtiapp.fi/realms/enfuser-portal/account"; //FYI
    protected final static String URL_TOKEN = "https://epk.2.rahtiapp.fi/realms/enfuser-portal/protocol/openid-connect/token";//fetch token from here
    protected final static String URLBASE = "https://enfuser-portal.2.rahtiapp.fi/enfuser/";
     
    protected final static String GROUP_AP ="pollutants";
    protected final static String GROUP_MET="meteorology";
    protected final static String GROUP_COMP="components";
    protected final static String GROUP_REG="regional";
    protected final static String GROUP_ELEV="elevated";
    
    static int WAIT_MS = 100;
    
    protected static String GROUPS = "";//"&values=pollutants,meteorology,components,regional"
    protected static String VAR_FILTER="";//"&variables=NO2,PM10"
    
    //;//  "&values=pollutants,meteorology,regional"; //components
    //add this to URL in case of testing API that supports request customization
    public static boolean TIME_LOG = true;
    public static void setVariableFilter(String[] pols) {
        if (pols==null) {
            VAR_FILTER ="";
            System.out.println("Variable filter cleared.");
        } else {
            VAR_FILTER = "&variables=";
            for (int i =0;i<pols.length;i++) {
                VAR_FILTER+=pols[i];
                if (i<pols.length-1) VAR_FILTER+=",";
            }
            System.out.println("Variable filter set: "+VAR_FILTER);
        }
    } 
    
    public static void setGroupFilter(String[] grs) {
        if (grs==null) {
            GROUPS ="";
            System.out.println("DataGroup filter cleared.");
        } else {
            GROUPS = "&values=";
            for (int i =0;i<grs.length;i++) {
                GROUPS+=grs[i];
                if (i<grs.length-1) GROUPS+=",";
            }
            System.out.println("DataGroup filter set: "+GROUPS);
        }
    } 

   public static void main(String[] args) {
       //1: get info from server
       String USR ="";
       String PWD = "";
       String token = AccessToken.fetchAccessToken(USR, PWD);
       AreaMeta am = getMeta(token);
       am.printout();
       
       // POINT QUERIES
       String start = "2025-09-19T15:00:00Z";
       String end = null;//"2025-09-19T17:00:00Z";
       double lat = 60.15385;
       double lon = 24.74636;
       boolean monthTest = true;
       boolean gridTest = false;
       boolean statsTest = true;
       
       //one hour data with full content
       PointSequence a = fetchResponse(token,lat,lon,start, end);
       a.printout();
       if (monthTest) {
        //one month data with limited content
        setGroupFilter(new String[]{GROUP_AP});
        setVariableFilter(null);//new String[]{"NO2","PM10"});
        a = fetchResponse(token,lat,lon,"2025-08-19T00:00:00Z", "2025-09-19T00:00:00Z");
        a.printout();
       }
       
       //statistics test
       if (statsTest) {
        setGroupFilter(new String[]{GROUP_AP});
        setVariableFilter(new String[]{"PM10"});
        Statistics st = Statistics.fetchStats(token, lat, lon, "2025-09-12T00:00:00Z", "2025-09-19T00:00:00Z");
       }
       
       //geo
       if (gridTest) {
            setVariableFilter(new String[]{"NO2","BC","PM10"});
            Boundaries b = new Boundaries(60.19,60.28,24.8,25.0);
            fetchGeoResponse(token,b,start,"D:/geoFetchTest.nc",true);
            fetchGeoResponse(token,b,start,"D:/geoFetchTest.tiff",false);
       }
   } 
   
   
   public static ArrayList<PointSequence> fetchResponses(String token,
           ArrayList<double[]> coords,String start, String end) {
       
       ArrayList<PointSequence> resps = new ArrayList<>();
       long t = System.currentTimeMillis();
       for (double[] c:coords) {  
          PointSequence a = null;
          int k=0;
          while ((a==null || a.failed) && k<10) {
            k++;
            if (k>1) System.out.println("RETRY --- "+ (k-1));
            a= fetchResponse(token,c[0],c[1],start, end);
          }
          resps.add(a);
       }
       
       long t2 = System.currentTimeMillis();
       System.out.println("API time taken: "+ (t2-t)/1000 +"s");
       return resps;
   }
   
   public static PointSequence fetchResponse(String token, double lat, double lon,
            String date1, String date2) {
       try {
                long t = System.currentTimeMillis();
                String resp = fetchRawResponse(token,lat, lon, date1, date2);
                long t2 = System.currentTimeMillis();
                if (TIME_LOG) System.out.println("Query took "+ ((t2-t)/1000) +"s to process.");
                 EnfuserLogger.sleep(WAIT_MS, EnfuserAPI.class);
                return new PointSequence(resp); 

       } catch (Exception ex) {
          ex.printStackTrace();
          EnfuserLogger.sleep(WAIT_MS, EnfuserAPI.class);
       } 
       return null;
   }
   
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
    private static String fetchRawResponse(String token, double lat, double lon,
            String date1, String date2) throws Exception {
        
        String address = URLBASE+"point-data?lat="
                +lat+"&lon="+lon;
        if (date1!=null) address+=  "&startTime="+date1;
        if (date2!=null) address+=  "&endTime="+date2;
         
        address+=GROUPS+VAR_FILTER;
        return httpsGet(address,token);
} 
    
    protected static String httpsGet(String address, String token) throws Exception {
        System.out.println("Opening connection to "+address);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(new URI(address))
        .header("Authorization","Bearer "+token)
        .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status code: "+ response.statusCode());
        return response.body();
    }
    
    
       public static boolean fetchGeoResponse(String token, Boundaries b,
            String date, String targetPath, boolean netCDF) {
       try {
                long t = System.currentTimeMillis();
                boolean ok = fetchGeo(token, b,date,targetPath,netCDF);
                long t2 = System.currentTimeMillis();
                if (TIME_LOG) System.out.println("Geo query took "+ ((t2-t)/1000) +"s to process.");
                 EnfuserLogger.sleep(WAIT_MS, EnfuserAPI.class);
                return ok; 

       } catch (Exception ex) {
          ex.printStackTrace();
          EnfuserLogger.sleep(200, EnfuserAPI.class);
       } 
       return false;
   }
    
    
    private static boolean fetchGeo(String token, Boundaries b,
            String date, String targetPath, boolean netCDF) throws Exception {
        
        if (VAR_FILTER== null || VAR_FILTER.length()<3) {
            System.out.println("Set variable list before using geo extraction.");
            return false;
        }  
        String enp ="geotiff";
        if (netCDF) enp = "netcdf";
        String address = URLBASE+enp+"?north="+b.latmax
                +"&west="+b.lonmin +"&south="+b.latmin +"&east="+b.lonmax
        +"&startTime="+date +VAR_FILTER;
       

        System.out.println("Opening connection to "+address);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(new URI(address))
        .header("Authorization","Bearer "+token)
        .build();
        
        
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, Path.of(targetPath), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File downloaded to: " + targetPath);
                return true;
            }
        } else {
            System.err.println("Failed to download file. HTTP status code: " + response.statusCode());
            return false;
        }
}     
   
              
}
