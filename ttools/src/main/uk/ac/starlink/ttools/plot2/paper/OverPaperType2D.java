package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.BitSet;
import uk.ac.starlink.ttools.plot2.Pixer;

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
        private final BitSet mask_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public OverPaper( PaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            rgbs_ = getRgbImage().getBuffer();
            mask_ = new BitSet( rgbs_.length );
        }

        public boolean canMerge() {
            return true;
        }

        public Paper createSheet() {
            return new OverPaper( getPaperType(), getBounds() );
        }

        public void mergeSheet( Paper other ) {
            OverPaper paper1 = (OverPaper) other;
            int[] rgbs1 = paper1.rgbs_;
            BitSet mask1 = paper1.mask_;
            int n = rgbs_.length;
            for ( int i = 0; i < n; i++ ) {
                if ( mask1.get( i ) ) {
                    rgbs_[ i ] = rgbs1[ i ];
                    mask_.set( i );
                }
            }
        }

        protected void placePixels( int xoff, int yoff, Pixer pixer,
                                    Color color ) {
            if ( color != lastColor_ ) {
                lastColor_ = color;
                lastRgb_ = color.getRGB();
            }
            int rgb = lastRgb_;
            while ( pixer.next() ) {
                int index = getPixelIndex( xoff, yoff, pixer );
                rgbs_[ index ] = rgb;
                mask_.set( index );
            }
        }

        public void flush() {
        }
    }
}
