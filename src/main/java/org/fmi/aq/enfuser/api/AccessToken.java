/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.fmi.aq.enfuser.api.EnfuserAPI.URL_ACCOUNT;
import static org.fmi.aq.enfuser.api.EnfuserAPI.URL_TOKEN;

/**
 *
 * @author johanssl
 */
public class AccessToken {
    
    protected final static String ERR_MSG =  ("Could NOT fetch access token from "+URL_TOKEN
                    +"!\n Check that your credentials (username and password) are correct."
            + "\naIf you haven't registered for the service yet, do this at:\n"+URL_ACCOUNT);
       
    
     /**
    * Fetch access token from Enfuser point service API using your user name
    * and password.
    * @param user username
    * @param pwd password
    * @return temporary access token information as JSON
    */ 
   public static String fetchAccessToken(String user, String pwd) {
        try {
            System.out.println("Fetching access token...");
            // URL for the request             
            URL url = new URL(URL_TOKEN);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();// Create a connection object
            connection.setRequestMethod("POST");// Set the request method to POST
            // Set headers
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            // Prepare the data for the body
            String requestBody = "grant_type=password" +
                                 "&username=" + user +
                                 "&password=" + pwd +
                                 "&client_id=point-service";
            // Send the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            // Print the response as a String
            String responseString = response.toString();
            connection.disconnect();
            
            //parse
            ObjectMapper objectMapper = new ObjectMapper();   
            Map<String, Object> map = objectMapper.readValue(responseString,new TypeReference<Map<String,Object>>(){});
            String token = map.get("access_token").toString();
            System.out.println("Access token fetched.");
            return token;
           
        } catch (Exception e) {
            System.out.println(ERR_MSG);
        }
        return null;
    } 
   
   
    
}
