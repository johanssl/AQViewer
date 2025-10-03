/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package org.fmi.aq.enfuser.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import org.fmi.aq.addons.visualization.FigureData;
import static org.fmi.aq.enfuser.api.EnfuserAPI.GROUP_AP;
import static org.fmi.aq.enfuser.api.EnfuserAPI.GROUP_MET;
import org.fmi.aq.enfuser.ftools.FileOps;
import org.fmi.aq.enfuser.ftools.FuserTools;
import static org.fmi.aq.enfuser.ftools.FuserTools.findFileThatContains;
import org.fmi.aq.essentials.geoGrid.Dtime;
import org.fmi.aq.essentials.geoGrid.Boundaries;
import org.fmi.aq.essentials.geoGrid.GeoGrid;
import static org.fmi.aq.essentials.netCdf.NCreader.fetch;
import org.fmi.aq.addons.visualization.VisualOptions;
import org.fmi.aq.enfuser.ftools.ConsoleControl;
import org.fmi.aq.enfuser.ftools.GuiFeel;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 *
 * @author johanssl
 */
public class ViewerGUI extends javax.swing.JFrame {

    public static int MAX_ZOOM = 19;
    MapImageOverlay overlay;
    JXMapViewer mapViewer;

    //click locations
    private GeoPosition leftClickPosition = null;
    private GeoPosition rightClickPosition = null;
    Boundaries b = null;
    
    //loaded dataset to display
    GeoGrid grid=null;
    String grVar="";
    String grUnits ="";
    File grSource= null;
    
    
    //meta 
    String token;
    long tokenSecs = -1;
    AreaMeta am;
    Boundaries curr_b= null;//for current selected area
    Dtime start= null;//for current selected area's data availability
    Dtime end = null;
    
    String tempDir;
    String root;
    /**
     * Creates new form ViewerGUI
     */
    public ViewerGUI() {
        initComponents();
        
                // Setup tile factory (OpenStreetMap)
                TileFactoryInfo info = new TileFactoryInfo(
                        0, MAX_ZOOM, MAX_ZOOM, 256, true, true,
                        "https://tile.openstreetmap.org",
                        "x", "y", "z") {
                            @Override
                            public String getTileUrl(int x, int y, int zoom) {
                                // OpenStreetMap expects: /{z}/{x}/{y}.png
                                //return String.format("%s/%d/%d/%d.png", this.baseURL, zoom, x, y);
                                int invZoom = MAX_ZOOM - zoom;
                                String url = this.baseURL + "/" + invZoom + "/" + x + "/" + y + ".png";
                                return url;
                            }
                        };
                
                DefaultTileFactory tileFactory = new DefaultTileFactory(info);
                
                // Create map viewer
                mapViewer = new JXMapViewer();
                mapViewer.setTileFactory(tileFactory);
                
                
                // Set initial focus
                GeoPosition initLoc = new GeoPosition(60.2, 25.0);
                mapViewer.setZoom(7);
                mapViewer.setAddressLocation(initLoc);
                
                // Add mouse interactions
                MouseInputListener panListener = new PanMouseInputListener(mapViewer);
                mapViewer.addMouseListener(panListener);
                mapViewer.addMouseMotionListener(panListener);
                mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
                mapViewer.addMouseListener(new CenterMapListener(mapViewer));
                
                
                //----------------------
                // Remove default mouse interactions
                for (MouseListener ml : mapViewer.getMouseListeners()) {
                    mapViewer.removeMouseListener(ml);
                }
                for (MouseMotionListener mml : mapViewer.getMouseMotionListeners()) {
                    mapViewer.removeMouseMotionListener(mml);
                }
                for (MouseWheelListener mwl : mapViewer.getMouseWheelListeners()) {
                    mapViewer.removeMouseWheelListener(mwl);
                }

                // Add only the interactions you want, such as panning with mouse drag
                PanMouseInputListener mia = new PanMouseInputListener(mapViewer);
                mapViewer.addMouseListener(mia);
                mapViewer.addMouseMotionListener(mia);

                // Add mouse wheel zooming (optional)
                mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
                
                mapViewer.addPropertyChangeListener("zoom", evt -> removeInfoBox(mapViewer));
                mapViewer.addPropertyChangeListener("center", evt -> removeInfoBox(mapViewer));
                //----------------------
                
                
                // Update coordinates on mouse move
                mapViewer.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        Point p = e.getPoint();
                        GeoPosition geo = mapViewer.convertPointToGeoPosition(p);
                        double lat = geo.getLatitude();
                        double lon = geo.getLongitude();
                        String add ="";
                        if (curr_b!=null && curr_b.intersectsOrContains(lat, lon)) add =" (in)";
                        jL_cc.setText(String.format("Lat: %.5f, Lon: %.5f" + add,
                                geo.getLatitude(), geo.getLongitude()));
                        
                        if (grid!=null && grid.gridBounds.intersectsOrContains(lat, lon)) {
                            double val = grid.getValue_closest(lat, lon);
                            jL_gridCons.setText(grVar +" [" + grUnits+"]: "+FuserTools.editPrecision(val, 3));
                        } else {
                            jL_gridCons.setText("");
                        }
                    }
                });
                
                // Add mouse listener for clicks
                mapViewer.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // Convert clicked point to GeoPosition
                        Point clickedPoint = e.getPoint();
                        GeoPosition geo = mapViewer.convertPointToGeoPosition(clickedPoint);

                        // Determine which button was clicked
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            
                            
                            //double-click?
                            if (e.getClickCount() == 2 && !e.isConsumed()) {
                                e.consume();
                                // Add the annotation component
                                
                                String message = popupPointQuery(geo);
                                //addAnnotation(mapViewer, clickedPoint, infoText);
                                if (message!=null)showInfoBox(mapViewer, geo, message);
                            } else {
                               leftClickPosition = geo;
                                //System.out.println("Left click at: " + geo);
                                boxUpdate(); 
                            }
                            
                            
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            rightClickPosition = geo;
                            //System.out.println("Right click at: " + geo);
                            boxUpdate();
                        }
                    }
                });
                
                JP_map.setLayout(new BorderLayout());
                JP_map.add(mapViewer, BorderLayout.CENTER);
                overlay = new MapImageOverlay(mapViewer);
 
                // testing credentials and paths
                File r = new File("");
                this.root = r.getAbsolutePath() + File.separator;
                System.out.println("Root directory: "+root);
                this.tempDir =root +"viewerTemp"+ File.separator;
                
                File sets = new File(root +"viewer_settings.txt");
                if (sets.exists()) {
                    HashMap<String, String> settings = FileOps.readKeyValueHash(new File(root +"viewer_settings.txt"), true,true);
                    String usr = settings.get("user");
                    String pwd = settings.get("pwd");
                    String temp = settings.get("temp");
                    if (usr!=null){
                        jT_usr.setText(usr);
                        System.out.println("Read default user from settings.");
                    }
                    if (pwd!=null){
                        jT_pwd.setText(pwd);
                        System.out.println("Read default password from settings.");
                    }
                    if (temp!=null) {
                        this.tempDir = temp;
                        System.out.println("Read temp path from settings: "+temp);
                    }
                }
                File f = new File(this.tempDir);
                if (!f.exists()) f.mkdirs();
                System.out.println("Directory for temporary files: "+ f.getAbsolutePath());
                
                
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jT_usr = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jT_pwd = new javax.swing.JPasswordField();
        jLabel2 = new javax.swing.JLabel();
        jB_fetchMeta = new javax.swing.JButton();
        jL_cc = new javax.swing.JLabel();
        jC_areas = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jL_vars = new javax.swing.JList<>();
        jT_time = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jT_hoursForward = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jT_box = new javax.swing.JTextField();
        jB_pointQ = new javax.swing.JButton();
        jB_geoQ = new javax.swing.JButton();
        jL_timeRange = new javax.swing.JLabel();
        jL_gridCons = new javax.swing.JLabel();
        jS_opac = new javax.swing.JSlider();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jC_contentFilt = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jC_pointFormat = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jB_minusH = new javax.swing.JButton();
        jB_plusH = new javax.swing.JButton();
        jB_now = new javax.swing.JButton();
        JP_map = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Enfuser API Viewer (Lasse Johansson, FMI, 2025)");

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jT_usr.setText("email");

        jLabel1.setText("User");

        jT_pwd.setText("jPasswordField1");

        jLabel2.setText("Password");

        jB_fetchMeta.setText("Establish connection");
        jB_fetchMeta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_fetchMetaActionPerformed(evt);
            }
        });

        jL_cc.setText("Map coordinates: -,-");

        jC_areas.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Areas (connect first)" }));
        jC_areas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jC_areasActionPerformed(evt);
            }
        });

        jL_vars.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Modelling variables" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jL_vars.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jL_varsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jL_vars);

        jT_time.setText("time");

        jLabel3.setText("Time (start)");

        jT_hoursForward.setText("23");

        jLabel4.setText("Hours forward");

        jT_box.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jT_box.setText("Search box");

        jB_pointQ.setText("Point query");
        jB_pointQ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_pointQActionPerformed(evt);
            }
        });

        jB_geoQ.setText("Get and show geographic data");
        jB_geoQ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_geoQActionPerformed(evt);
            }
        });

        jL_timeRange.setText("TimeRange:");

        jL_gridCons.setText(":-");

        jS_opac.setValue(75);

        jLabel5.setText("Opacity");

        jButton1.setText("Clear image layer");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jC_contentFilt.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Just pollutants", "include meteorology", "all" }));

        jLabel6.setText("Content filter");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("Point queries");

        jC_pointFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "CSV", "CSV (with Excel)", "JSON", "console" }));

        jLabel8.setText("Output format");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Raster queries");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Variables, location and time");

        jButton2.setText("Hide / Show console");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jB_minusH.setText("-1h");
        jB_minusH.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_minusHActionPerformed(evt);
            }
        });

        jB_plusH.setText("+1h");
        jB_plusH.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_plusHActionPerformed(evt);
            }
        });

        jB_now.setText("Now");
        jB_now.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jB_nowActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jB_fetchMeta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jT_pwd, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                            .addComponent(jT_usr))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jL_cc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jC_areas, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jT_hoursForward, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jB_pointQ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jB_geoQ, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jL_timeRange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jL_gridCons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jS_opac, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE))
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jC_pointFormat, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jC_contentFilt, javax.swing.GroupLayout.Alignment.LEADING, 0, 145, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jT_box)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jT_time)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jB_plusH)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jB_minusH, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jB_now, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jT_usr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jT_pwd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jB_fetchMeta)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jC_areas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel10)
                .addGap(3, 3, 3)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jB_plusH)
                    .addComponent(jB_minusH)
                    .addComponent(jB_now))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jT_time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jT_box, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7)
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jT_hoursForward, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jB_pointQ)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jC_contentFilt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jC_pointFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jB_geoQ)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jS_opac, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 82, Short.MAX_VALUE)
                .addComponent(jL_gridCons)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jL_timeRange)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jL_cc)
                .addContainerGap())
        );

        javax.swing.GroupLayout JP_mapLayout = new javax.swing.GroupLayout(JP_map);
        JP_map.setLayout(JP_mapLayout);
        JP_mapLayout.setHorizontalGroup(
            JP_mapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 929, Short.MAX_VALUE)
        );
        JP_mapLayout.setVerticalGroup(
            JP_mapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JP_map, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(JP_map, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    void boxUpdate() {
        double latmax =-90;
        double latmin = 90;
        double lonmax = -180;
        double lonmin = 180;
        
        GeoPosition[] pos = {leftClickPosition,rightClickPosition};
        for (GeoPosition p:pos) {
            if (p==null) continue;
            latmax = Math.max(latmax,p.getLatitude());
            latmin = Math.min(latmin,p.getLatitude());
            lonmax = Math.max(lonmax,p.getLongitude());
            lonmin = Math.min(lonmin,p.getLongitude());
        }
        b = new Boundaries(latmin,latmax,lonmin,lonmax);
        jT_box.setText(b.toText_fileFriendly(4).replace("_", ","));
    }
    
    private void refreshToken() {
        long t = System.currentTimeMillis();
        long dff_min = (t-this.tokenSecs)/60000;
        System.out.println("Token refreshed "+dff_min +"minutes ago.");
        if (dff_min > 20) {
            System.out.println("Refreshing token now.");
            String usr = jT_usr.getText();
            String pwd = jT_pwd.getText();
            this.token = AccessToken.fetchAccessToken(usr, pwd);
            tokenSecs = System.currentTimeMillis();
        }
    }
    
    private void jB_fetchMetaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_fetchMetaActionPerformed
      String usr = jT_usr.getText();
      String pwd = jT_pwd.getText();
      this.token = AccessToken.fetchAccessToken(usr, pwd);
     
      if (token!=null) {
        tokenSecs = System.currentTimeMillis();
        this.am = AreaMeta.getMeta(token); 
        String[] arNames = new String[am.areas.size()];
        for (int i =0;i<am.areas.size();i++) {
            arNames[i] = am.areas.get(i);
        }
        jC_areas.setModel(new DefaultComboBoxModel(arNames));

        am.printout();
        jC_areas.setSelectedIndex(0);
      } else {
          showCustomMessage(AccessToken.ERR_MSG,false);
      }
    }//GEN-LAST:event_jB_fetchMetaActionPerformed

    private void showCustomMessage(String s, boolean sout) {
        Point p = new Point(0,0);
        GeoPosition geo = mapViewer.convertPointToGeoPosition(p);
        this.showInfoBox(mapViewer, geo, s);
        if (sout)System.out.println(s);
    }
    
    private void jC_areasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jC_areasActionPerformed
        if (this.am==null) return;
        String area = jC_areas.getSelectedItem().toString();
        int ind = am.getIndex(area);
        this.curr_b = am.getBounds(area);
        System.out.println(curr_b.toText());
        
        // Set initial focus
        GeoPosition initLoc = new GeoPosition(curr_b.getMidLat(), curr_b.getMidLon());
        mapViewer.setZoom(8);
        mapViewer.setAddressLocation(initLoc);
        
        //time
        String[] dates = am.timeRange.get(ind);
        start = new Dtime(dates[0].replace("Z", ""));
        end = new Dtime(dates[1].replace("Z", ""));
        
        Dtime dt = end.clone();
        dt.addDays(-1);
        jT_time.setText(dt.getStringDate_noTS()+"Z");
        jL_timeRange.setText("["+start.getStringDate_YYYY_MM_DDTHH() +" - "+end.getStringDate_YYYY_MM_DDTHH()+"]");
        
        //variables
        String[] vars = am.getPollutantVars(ind);
        jL_vars.setModel(new DefaultComboBoxModel(vars));
    }//GEN-LAST:event_jC_areasActionPerformed

   
    private JLabel infoLabel;
    private GeoPosition infoGeoPosition;  
  private void showInfoBox(JXMapViewer mapViewer, GeoPosition position, String text) {
    // Remove old label if exists
    if (infoLabel != null) {
        mapViewer.remove(infoLabel);
    }

    infoGeoPosition = position;
    text = text.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
    String htmlText = "<html>" + text.replace("\n", "<br>") + "</html>";
    infoLabel = new JLabel(htmlText);
    infoLabel.setOpaque(true);
    infoLabel.setBackground(new Color(255, 255, 204));
    infoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    infoLabel.setSize(infoLabel.getPreferredSize());

    mapViewer.setLayout(null);
    mapViewer.add(infoLabel);
    updateInfoBoxPosition(mapViewer);

    mapViewer.repaint();
}  
  
  private void removeInfoBox(JXMapViewer mapViewer) {
    if (infoLabel != null) {
        mapViewer.remove(infoLabel);
        infoLabel = null;
        infoGeoPosition = null;
        mapViewer.repaint();
    }
}
   
 private void updateInfoBoxPosition(JXMapViewer mapViewer) {
    if (infoLabel != null && infoGeoPosition != null) {
        Point2D pt = mapViewer.getTileFactory().geoToPixel(infoGeoPosition, mapViewer.getZoom());
        Point mapCenter = mapViewer.getViewportBounds().getLocation();
        int x = (int)(pt.getX() - mapCenter.getX());
        int y = (int)(pt.getY() - mapCenter.getY());
        infoLabel.setLocation(x, y);
    }
} 
    
    private void jB_geoQActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_geoQActionPerformed
        if (this.am==null) {
            this.showCustomMessage("List available data first.",true);
            return;
        }
        
        if (this.leftClickPosition==null || this.rightClickPosition==null) {
            this.showCustomMessage("Click on the map to choose the box first."
                    + "\n Left-click to set a corner, and then right-click for another.",true);
            return;
        }
       
        if (jL_vars.getSelectedIndices()==null || jL_vars.getSelectedIndices().length==0) {
            this.showCustomMessage("Select one or more variables first.",true);
            return;
        }
        
        refreshToken();
        
        EnfuserAPI.setGroupFilter(new String[]{GROUP_AP});
        List<String> varList = jL_vars.getSelectedValuesList();
        String[] vars = new String[varList.size()];
        varList.toArray(vars);
        EnfuserAPI.setVariableFilter(vars);
        
        Dtime dt = new Dtime(jT_time.getText().replace("Z", ""));
        String[] search = new String[vars.length+3];
        search[0] = dt.getStringDate_YYYY_MM_DDTHH()+"_";
        search[1] = b.toText_fileFriendly(4);
        search[search.length-1] = ".nc";
        int k = 1;
        for (String var:vars) {
            k++;
            search[k]= var+"_";
        }
       

        File f = findFileThatContains(this.tempDir,search);
        if (f!=null) {
            System.out.println("Applicable gridded data has already been downloaded: "+f.getAbsolutePath());
        } else {
            String fname = "";
            for (String s:search) fname+=s;
            f = new File(tempDir+fname);
            System.out.println("Extracting "+ f.getAbsolutePath());
            String qstart = jT_time.getText();
            EnfuserAPI.fetchGeoResponse(token, b, qstart, f.getAbsolutePath(), true);
        }
        
        draw(f, vars[0]);
        
    }//GEN-LAST:event_jB_geoQActionPerformed

    private void draw(File f, String var) {
        overlay.removeImage();
        Dtime dt = new Dtime(jT_time.getText().replace("Z", ""));
        grid = fetch(f,var, dt);
        this.grVar = var;
        this.grUnits = grid.getMeta("units");
        this.grSource = f;
        
        VisualOptions vops = VisualOptions.darkSteam();
        vops.colBarStyle = VisualOptions .CBAR_NONE;
        vops.bannerStyle = VisualOptions.BANNER_NONE;
        vops.scaleProgression = 1.3;
        FigureData fd = new FigureData(grid,vops);
        vops.transparency = FigureData.TRANSPARENCY_OFF;
        vops.colorScheme = FigureData.COLOR_BLACKTEMP;
        
        //variable customization
        if (var.contains("AQI")) {
            vops.minMax = new double[]{1,5};
            vops.scaleProgression = 1;
            vops.colorScheme = FigureData.COLOR_PASTEL_GR;
        } else if (var.contains("O3")) {
            vops.colorScheme = FigureData.COLOR_WHITE_TO_BLUE;
            vops.scaleProgression = 0.7;
        } else if (var.contains("LDSA") || var.contains("PNC")) {
            vops.colorScheme = FigureData.COLOR_FLAME;
        } else if (var.contains("PM")) {
            vops.colorScheme = FigureData.COLOR_BLACKGR;
        }
        
        double alpha = (double)jS_opac.getValue()/100.0;
        overlay.showImageOnMap(fd.getBufferedImage(), grid.gridBounds,(float)alpha);
    }
    
    private Dtime getTime(boolean returnIfOob) {
        try {
            String qstart = jT_time.getText();
            Dtime dt = new Dtime(qstart.replace("Z", ""));
            
            if (this.start!=null && end!=null) {
                if (dt.systemHours()< start.systemHours() || dt.systemHours()> end.systemHours()) {
                    showCustomMessage("Selected time (start) is not within area data range:"
                            +"\n"+start.getStringDate_noTS() +"Z to "+end.getStringDate_noTS()+"Z",true);
                    if (returnIfOob)return dt;
                    return null;
                }
            }
            
            return dt;
        } catch (Exception e) {
            showCustomMessage("Incorrect time format.\nUse YYYY-MM-ddThh:mm:ssZ\nFor example:'2025-01-01T00:00:00Z'",true);
            return null;
        }
    }
    
    private void jB_pointQActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_pointQActionPerformed
        if (this.am==null) {
            showCustomMessage("List available data first.",true);
            return;
        }
        
        if (this.leftClickPosition==null) {
            showCustomMessage("Left click on the map to choose location first.",true);
            return;
        }

        refreshToken();
        
        if (jC_contentFilt.getSelectedIndex()==2) {
            EnfuserAPI.setGroupFilter(null);//all
        } else if (jC_contentFilt.getSelectedIndex()==1) {
            EnfuserAPI.setGroupFilter(new String[]{GROUP_AP, GROUP_MET});
        } else {
            EnfuserAPI.setGroupFilter(new String[]{GROUP_AP});
        }

        if (jL_vars.getSelectedIndices()==null || jL_vars.getSelectedIndices().length==0) {
            EnfuserAPI.setVariableFilter(null);
        } else {
            List<String> varList = jL_vars.getSelectedValuesList();
            String[] vars = new String[varList.size()];
            varList.toArray(vars);
            EnfuserAPI.setVariableFilter(vars);
        }

        double lat = this.leftClickPosition.getLatitude();
        double lon = this.leftClickPosition.getLongitude();
        String qstart = jT_time.getText();
        Dtime dt = getTime(false);
        if (dt==null) return;
        
        int hours = Integer.parseInt(jT_hoursForward.getText());
        dt.addSeconds(3600*hours);
        String qend = dt.getStringDate_noTS()+"Z";

        PointSequence resp = EnfuserAPI.fetchResponse(token, lat,lon, qstart, qend);
        if (resp.failed) {
            showCustomMessage(resp.warnings,false);
            return;
        }
        
        
        int oi = jC_pointFormat.getSelectedIndex();
        if (oi<2) {
            File f = resp.saveToFile(this.tempDir,true);
            if (oi==1) {//open it
                try {
                    Desktop.getDesktop().open(f);
                } catch (IOException ex) {
                    Logger.getLogger(ViewerGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (oi ==2) {
          File json = resp.saveToFile(this.tempDir,false);  
        } else {
            resp.printout();
        }
        //let's make a CSV out of it
        
        

    }//GEN-LAST:event_jB_pointQActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
      overlay.removeImage();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jL_varsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jL_varsValueChanged
      
       if (jL_vars.getSelectedIndices().length==1 && this.grid!=null && this.grSource !=null) {
           String varnow = jL_vars.getSelectedValue();
           if (grSource.getName().contains(varnow+"_")) {//we can swap the visualization immediately without downloading new data
               this.draw(grSource, varnow);
           }
       }
        
    }//GEN-LAST:event_jL_varsValueChanged

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
       ConsoleControl.toggleConsole();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jB_nowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_nowActionPerformed
      Dtime dt = Dtime.getSystemDate_FH();
      jT_time.setText(dt.getStringDate_noTS()+"Z");
    }//GEN-LAST:event_jB_nowActionPerformed

    private void jB_plusHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_plusHActionPerformed
       Dtime dt = this.getTime(true);
       if (dt==null) return;
       dt.addSeconds(3600);
      jT_time.setText(dt.getStringDate_noTS()+"Z");
    }//GEN-LAST:event_jB_plusHActionPerformed

    private void jB_minusHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jB_minusHActionPerformed
       Dtime dt = this.getTime(true);
       if (dt==null) return;
       dt.addSeconds(-3600);
      jT_time.setText(dt.getStringDate_noTS()+"Z");
    }//GEN-LAST:event_jB_minusHActionPerformed

    private String popupPointQuery(GeoPosition geo) {
        if (this.am==null) return null;
        Dtime dt = this.getTime(false);
        if (dt==null) return null;
        
        refreshToken();
        
        String qstart = jT_time.getText();
        EnfuserAPI.setGroupFilter(new String[]{GROUP_AP, EnfuserAPI.GROUP_MET});
        EnfuserAPI.setVariableFilter(null);
        double lat = geo.getLatitude();
        double lon = geo.getLongitude();
        PointSequence resp = EnfuserAPI.fetchResponse(token, lat, lon, qstart, null);
        if (resp.failed) {
            return resp.warnings;
        }
        
        return resp.shortInfo(qstart);
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        GuiFeel.set(null);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ViewerGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel JP_map;
    private javax.swing.JButton jB_fetchMeta;
    private javax.swing.JButton jB_geoQ;
    private javax.swing.JButton jB_minusH;
    private javax.swing.JButton jB_now;
    private javax.swing.JButton jB_plusH;
    private javax.swing.JButton jB_pointQ;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jC_areas;
    private javax.swing.JComboBox<String> jC_contentFilt;
    private javax.swing.JComboBox<String> jC_pointFormat;
    private javax.swing.JLabel jL_cc;
    private javax.swing.JLabel jL_gridCons;
    private javax.swing.JLabel jL_timeRange;
    private javax.swing.JList<String> jL_vars;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSlider jS_opac;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jT_box;
    private javax.swing.JTextField jT_hoursForward;
    private javax.swing.JPasswordField jT_pwd;
    private javax.swing.JTextField jT_time;
    private javax.swing.JTextField jT_usr;
    // End of variables declaration//GEN-END:variables
}
