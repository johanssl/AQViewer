package org.fmi.aq.enfuser.api;

import java.awt.*;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.painter.CompoundPainter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.fmi.aq.essentials.geoGrid.Boundaries;

public class MapImageOverlay {

    private final JXMapViewer mapViewer;

    // painter we create for the image
    private Painter<JXMapViewer> imagePainter;

    // preserve whatever overlay the map had before (note the ? super)
    private Painter<? super JXMapViewer> originalOverlayPainter;

    public MapImageOverlay(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
    }

    /**
     * Show an image stretched between two geo-coordinates (lower-left and upper-right).
     * @param img the image to draw
     * @param b
     */
    public void showImageOnMap(BufferedImage img, Boundaries b, float alpha) {
        
        final GeoPosition geoLL = new GeoPosition(b.latmin, b.lonmin); // lower-left (south-west)
        final GeoPosition geoUR = new GeoPosition(b.latmax, b.lonmax); // upper-right (north-east)

        // create the image painter
        imagePainter = new Painter<JXMapViewer>() {
            @Override
            public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
                if (img == null) return;

                // geo -> world pixel (returns Point2D)
                Point2D ptLL = map.getTileFactory().geoToPixel(geoLL, map.getZoom());
                Point2D ptUR = map.getTileFactory().geoToPixel(geoUR, map.getZoom());

                Rectangle viewport = map.getViewportBounds();

                int x = (int) Math.round(ptLL.getX() - viewport.getX());
                int y = (int) Math.round(ptUR.getY() - viewport.getY()); // top edge
                int w = (int) Math.round(ptUR.getX() - ptLL.getX());
                int h = (int) Math.round(ptLL.getY() - ptUR.getY());

                if (w <= 0 || h <= 0) return;

                Composite old = g.getComposite();
                try {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g.drawImage(img, x, y, w, h, null);
                } finally {
                    g.setComposite(old);
                }
            }
        };

        // <-- FIXED LINE: correctly capture the existing overlay painter (may be null)
        originalOverlayPainter = mapViewer.getOverlayPainter();

        // build a compound painter that contains the original (if any) + our painter
        CompoundPainter<JXMapViewer> compound;
        if (originalOverlayPainter != null) {
            // unchecked cast: originalOverlayPainter is usually a Painter<JXMapViewer> at runtime
            @SuppressWarnings("unchecked")
            Painter<JXMapViewer> orig = (Painter<JXMapViewer>) originalOverlayPainter;
            compound = new CompoundPainter<>(orig, imagePainter);
        } else {
            compound = new CompoundPainter<>(imagePainter);
        }

        mapViewer.setOverlayPainter(compound);
        mapViewer.repaint();
    }

    /** Remove the previously shown image and restore the original overlay painter. */
    public void removeImage() {
        if (imagePainter == null) return;

        // restore original (may be null)
        mapViewer.setOverlayPainter(originalOverlayPainter);

        imagePainter = null;
        originalOverlayPainter = null;
        mapViewer.repaint();
    }
}