package org.fmi.aq.enfuser.api;


import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import javax.imageio.ImageIO;
import org.jxmapviewer.viewer.TileCache;

public class SaturationAdjustedTileCache extends TileCache {

    private final float saturationRatio; // 0.0f = Grayscale, 1.0f = Full Color

    public SaturationAdjustedTileCache(float saturationRatio) {
        super();
        this.saturationRatio = Math.max(0.0f, Math.min(1.0f, saturationRatio));
    }

    @Override
    public synchronized void put(URI uri, byte[] b, BufferedImage img) {
        // If saturation is 1.0f, pass it through completely untouched
        if (saturationRatio >= 1.0f) {
            super.put(uri, b, img);
            return;
        }

        BufferedImage sourceImg = img;
        
        // Safety check: Ensure we have a valid BufferedImage to process
        if (sourceImg == null && b != null) {
            try {
                sourceImg = ImageIO.read(new ByteArrayInputStream(b));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (sourceImg != null) {
            // 1. Process the image to adjust saturation
            BufferedImage filteredImg = adjustSaturation(sourceImg, saturationRatio);
            
            // 2. RE-ENCODE the filtered image back into a valid byte array!
            // This prevents the NullPointerException in TileCache.get()
            byte[] filteredBytes = b; 
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(filteredImg, "png", baos);
                filteredBytes = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                // Fallback to original bytes if encoding fails to protect runtime
                if (filteredBytes == null) filteredBytes = new byte[0]; 
            }

            // 3. Send both the processed image and the processed bytes down to the parent cache
            super.put(uri, filteredBytes, filteredImg);
        } else {
            super.put(uri, b, img);
        }
    }

    private BufferedImage adjustSaturation(BufferedImage source, float ratio) {
        int w = source.getWidth();
        int h = source.getHeight();
        
        // Convert to pure grayscale using Java's hardware-accelerated transformation matrix
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(source, gray);

        // If absolute grayscale is wanted, skip blending
        if (ratio <= 0.0f) {
            return gray;
        }

        // Blend original color back over grayscale if a muted tone is desired
        BufferedImage blended = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = blended.createGraphics();
        g2d.drawImage(gray, 0, 0, null);
        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, ratio));
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        
        return blended;
    }

}