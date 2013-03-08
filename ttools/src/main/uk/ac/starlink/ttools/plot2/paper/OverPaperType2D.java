package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Pixellator;

/**
 * Bitmapped 2D PaperType which just plots graphics over the top of any
 * previously plotted data.  No transparency or compositing is supported.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class OverPaperType2D extends RgbPaperType2D {

    /**
     * Constructor.
     */
    public OverPaperType2D() {
        super( "PixelOverlay", true );
    }

    protected RgbPaper2D createPaper2D( Rectangle bounds ) {
        return new OverPaper( this, bounds );
    }

    /**
     * Paper implementation for use with this PaperType.
     */
    private static class OverPaper extends RgbPaper2D {

        private Color lastColor_;
        private int lastRgb_;
        private final int[] rgbs_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public OverPaper( PaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            rgbs_ = getRgbImage().getBuffer();
        }

        protected void placePixels( int xoff, int yoff, Pixellator pixer,
                                    Color color ) {
            if ( color != lastColor_ ) {
                lastColor_ = color;
                lastRgb_ = color.getRGB();
            }
            int rgb = lastRgb_;
            for ( pixer.start(); pixer.next(); ) {
                int index = getPixelIndex( xoff, yoff, pixer );
                rgbs_[ index ] = rgb;
            }
        }

        public void flush() {
        }
    }
}
