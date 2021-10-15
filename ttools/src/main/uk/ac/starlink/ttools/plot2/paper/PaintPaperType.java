package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.PictureImageIcon;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.layer.MarkerShape;

/**
 * Vector paper type abstract superclass.
 * All graphics are painted rather than laid down pixel by pixel,
 * which gives more beautiful results on a non-bitmapped output medium.
 * It may be slower (for large datasets, perhaps much slower)
 * than one of the bitmapped options.
 * Whether it supports transparency depends on whether the graphics
 * context does.  In any case the details of the transparency rendering
 * may not be identical to the way it is done by the other paper types,
 * since in some cases they handle the compositing in a non-standard fashion,
 * for instance opacity boost for very transparent points.
 * Note that some of the output may in any case look pixellated when using this
 * output format, if that's how the plot layers have decided to render it
 * (bitmapped decals rather than glyphs).
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class PaintPaperType implements PaperType {

    private final String name_;
    private final boolean upLayer_;

    private static final GraphicsConfiguration HEADLESS_GC =
        createHeadlessGraphicsConfig( BufferedImage.TYPE_INT_RGB );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.paper" );

    /**
     * Constructor.
     *
     * @param  name  paper type name
     * @param  upLayer  true to render layers in ascending order,
     *                  false to do them in descending order
     */
    protected PaintPaperType( String name, boolean upLayer ) {
        name_ = name;
        upLayer_ = upLayer;
    }

    /**
     * Returns false.
     */
    public boolean isBitmap() {
        return false;
    }

    public Icon createDataIcon( Surface surface, Drawing[] drawings,
                                Object[] plans, DataStore dataStore,
                                boolean cached ) {
        PaintIcon icon =
            new PaintIcon( surface, drawings, plans, dataStore, this );
        if ( cached ) {

            /* This is supposed to work fast by painting to a volatile image.
             * At time of writing I've never benchmarked it though,
             * and I don't really understand the rendering characteristics
             * of volatile images though, so I don't really know if it does. */
            BufferedImage image = 
                PictureImageIcon.createImage( PlotUtil.toPicture( icon ),
                                              HEADLESS_GC, Color.WHITE, null );
            return new ImageIcon( image );
        }
        else {
            return icon;
        }
    }

    /**
     * Creates a paper instance for use with this PaperType.
     *
     * @param   g   graphics context to which paper should output
     * @param  bounds  plot bounds
     * @return  new paper instance
     */
    protected abstract Paper createPaper( Graphics g, Rectangle bounds );

    /**
     * Called when all the layers have been painted.
     *
     * @param  paper   graphics destination
     */
    protected abstract void flushPaper( Paper paper );

    public String toString() {
        return name_;
    }

    /**
     * Icon whose paint method iterates over layer drawings
     * and paints their content.
     */
    private static class PaintIcon implements Icon {
        private final Surface surface_;
        private final Drawing[] drawings_;
        private final Object[] plans_;
        private final DataStore dataStore_;
        private final PaintPaperType paperType_;
        private final Rectangle plotBounds_;

        /**
         * Constructor.
         *
         * @param  surface   plot surface
         * @param  drawings  drawings to paint
         * @param  plans   array of plans, one for each drawing
         * @param  dataStore  data storage object
         * @param  paperType  paper type which created this paper
         */
        PaintIcon( Surface surface, Drawing[] drawings, Object[] plans,
                   DataStore dataStore, PaintPaperType paperType ) {
            surface_ = surface;
            drawings_ = drawings;
            plans_ = plans;
            dataStore_ = dataStore;
            paperType_ = paperType;
            plotBounds_ = surface.getPlotBounds();
        }

        public int getIconWidth() {
            return plotBounds_.width;
        }

        public int getIconHeight() {
            return plotBounds_.height;
        }

        @Slow
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint( MarkerShape.OUTLINE_CIRCLE_HINT,
                                 Boolean.valueOf( paperType_.isBitmap() ) );
            g2.translate( x - plotBounds_.x, y - plotBounds_.y );
            g2.clipRect( plotBounds_.x, plotBounds_.y,
                         plotBounds_.width, plotBounds_.height );
            surface_.paintBackground( g2 );
            Paper paper = paperType_.createPaper( g2, plotBounds_ );
            int nlayer = drawings_.length;
            for ( int il = 0; il < nlayer; il++ ) {
                int jl = paperType_.upLayer_ ? il
                                             : nlayer - 1 - il;
                drawings_[ jl ].paintData( plans_[ jl ], paper, dataStore_ );
            }
            long startFlush = System.currentTimeMillis();
            paperType_.flushPaper( paper );
            PlotUtil.logTimeFromStart( logger_, "Flush", startFlush );
        }
    }

    /**
     * Returns a graphics configuration that does not rely on a display.
     *
     * @param  imtype   image type as used by
     *                  {@link java.awt.image.BufferedImage}
     * @return  graphics config
     */
    public static GraphicsConfiguration
            createHeadlessGraphicsConfig( int imtype ) {
        BufferedImage im = new BufferedImage( 1, 1, imtype );
        Graphics2D g = im.createGraphics();
        GraphicsConfiguration gconf = g.getDeviceConfiguration();
        g.dispose();
        return gconf;
    }
}
