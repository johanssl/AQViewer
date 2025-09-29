# AQViewer
This Java program can be used to:
- Establish connection to a service that provides timely AQ data for selected urban areas in Finland (Enfuser API)
- Create AQ queries and recieve the requested data (both point queries and raster queries are supported).
- Show raster queries (pollutant concentrations in an area) on interactive OpenStreetMap
- Download longer AQ timeseries on CSV or JSON to local drive

<img width="1110" height="793" alt="image" src="https://github.com/user-attachments/assets/89e78f82-ee28-40cd-b5d2-5ee371a21aa6" />

**How to use - Basics**
To establish a connection (and to obtain a token) one needs to register first to the service (https://epk.2.rahtiapp.fi/realms/enfuser-portal/account). Using the provided user ID and your chosen password you can "Establish connection" using the upper left section of the GUI.

Once a connection has been established the lists for modelling areas and modelling variable will update automatically. Select one of the modelling areas of interrest from the list (the map should navigate to the area automatically).

**Map controls:**
- Basic zooming and panning features with mouse
- Left click: update point location (point queries) and one of the box coordinates (geographic, raster data)
- Right click: update the second bounding box coordinates (geographic, raster data)
- Double left click: show air quality information on the map for the click location and the specified time ("Time start" -infobox).


  **Point queries:**
  Once the time and location (left-click) has been set, define the number of hours from the origin the time series should extend to. (Note: a point queries with all meta-data and variables should not exceed the length of one month). Use the "Content filter" to limit the query content. E.g., there is no need for a emission source component split or meteorology then select "Just pollutants" option. As default the query provides data for all pollutant species if no "Variables" have been selected. To target a specific set of pollutant species then select one or more variables from the list manually (the list support basic mouse and keyboard functions such as CTRL + A). Finally, select the output format and press "Point query".

  
  
