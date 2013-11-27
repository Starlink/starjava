package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Bitmapped 3D PaperType for opaque pixels only.
 *
 * <p>It uses a Z-buffer to record the current frontmost colour at each 
 * pixel position and overwrites the colour only if a pixel at the
 * same or a smaller Z-coordinate is plotted at the same graphics position.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class ZBufferPaperType3D extends RgbPaperType3D {

    /**
     * Constructor.
     */
    public ZBufferPaperType3D() {
        super( "ZBuffer", true );
    }

    protected RgbPaper3D createPaper3D( Rectangle bounds ) {
        return new ZBufferPaper( this, bounds );
    }

    /**
     * Paper implementation for use with this class.
     */
    private static class ZBufferPaper extends RgbPaper3D {

        private final int[] rgbs_;
        private final float[] zs_;
        private Color lastColor_;
        private int lastRgb_;
    
        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public ZBufferPaper( PaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            rgbs_ = getRgbImage().getBuffer();
            zs_ = new float[ bounds.width * bounds.height ];
            Arrays.fill( zs_, Float.POSITIVE_INFINITY );
        }

        protected void placePixels( int xoff, int yoff, double dz,
                                    Pixer pixer, Color color ) {
            if ( color != lastColor_ ) {
                lastColor_ = color;
                lastRgb_ = color.getRGB();
            }
            int rgb = lastRgb_;
            float fz = (float) dz;
            while ( pixer.next() ) {
                int index = getPixelIndex( xoff, yoff, pixer );
                if ( fz <= zs_[ index ] ) {
                    zs_[ index ] = fz;
                    rgbs_[ index ] = rgb;
                }
            }
        }

        public void flush() {
        }
    }
}
