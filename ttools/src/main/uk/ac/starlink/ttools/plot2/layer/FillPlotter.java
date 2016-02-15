package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that fills the area under (or above, or to the left/right)
 * the curve defined by a set of X,Y points.
 * This is suitable for drawing something that looks like a histogram
 * from a set of histogram endpoints.  It works with large datasets;
 * alpha-shading is used for bars that would be thin.
 *
 * @author   Mark Taylor
 * @since    15 Jan 2015
 */
public class FillPlotter extends AbstractPlotter<FillPlotter.FillStyle> {

    /** Key to configure whether fill is vertical or horizontal. */
    public static final ConfigKey<Boolean> HORIZONTAL_KEY = 
        new BooleanConfigKey(
            new ConfigMeta( "horizontal", "Horizontal" )
           .setShortDescription( "Horizontal fill?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the filling is vertical",
                "(suitable for functions of the horizontal variable),",
                "or horizontal",
                "(suitable for functions of the vertical variable).",
                "If false, the fill is vertical (to the X axis),",
                "and if true, the fill is horizontal (to the Y axis).",
                "</p>",
            } )
        , false );

    /** Key to configure positive/negative direction of fill. */
    public static final ConfigKey<Boolean> POSITIVE_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "positive", "Positive" )
           .setShortDescription( "Fill in positive direction?" )
           .setXmlDescription( new String[] {
                "<p>Determines the directional sense of the filling.",
                "If false, the fill is between the data points",
                "and negative infinity along the relevant axis",
                "(e.g. down from the data points to the bottom of the plot).",
                "If true, the fill is in the other direction.",
                "</p>",
            } )
        , false );

    /**
     * Constructor.
     */
    public FillPlotter() {
        super( "Fill", ResourceIcon.FORM_FILL, 1, new Coord[ 0 ] );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>If a two-dimensional dataset represents a single-valued",
            "function, this plotter will fill the area underneath the",
            "function's curve with a solid colour.",
            "Parts of the surface which would only be partially covered",
            "(because of rapid function variation within the width",
            "of a single pixel)",
            "are represented using appropriate alpha-blending.",
            "The filled area may alternatively be that above the curve",
            "or to its left or right.",
            "</p>",
            "<p>One example of its use is to reconstruct the appearance",
            "of a histogram plot from a set of histogram bins.",
            "For X,Y data which is not single-valued, the result",
            "may not be very useful.",
            "</p>"
        } );
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
            StyleKeys.TRANSPARENCY,
            HORIZONTAL_KEY,
            POSITIVE_KEY,
        };
    }

    public FillStyle createStyle( ConfigMap config ) {
        Color color = StyleKeys.getAlphaColor( config, StyleKeys.COLOR,
                                               StyleKeys.TRANSPARENCY );
        boolean isHorizontal = config.get( HORIZONTAL_KEY );
        boolean isPositive = config.get( POSITIVE_KEY );
        return new FillStyle( color, isHorizontal, isPositive );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final FillStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        Color color = style.color_;
        LayerOpt layerOpt = new LayerOpt( color, false );
        return new AbstractPlotLayer( this, geom, dataSpec, style, layerOpt ) {
            final boolean isHorizontal = style.isHorizontal_;
            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          final PaperType paperType ) {
                return new Drawing() {
                    public Object calculatePlan( Object[] knownPlans,
                                                 DataStore dataStore ) {
                        for ( Object plan : knownPlans ) {
                            if ( plan instanceof FillPlan &&
                                 ((FillPlan) plan)
                                .matches( geom, dataSpec, surface ) ) {
                                return plan;
                            }
                        }
                        return createPlan( surface, dataSpec, geom, dataStore );
                    }
                    public void paintData( Object plan, Paper paper,
                                           DataStore dataStore ) {
                        final FillPlan fplan = (FillPlan) plan;
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintFill( surface, fplan, style, g );
                            }
                            public boolean isOpaque() {
                                return false;
                            }
                        } );
                    }
                    public ReportMap getReport( Object plan ) {
                        return null;
                    }
                };
            }
        };
    }

    /**
     * Creates a plan object for these plots.
     * The form of the plan is (surprisingly?) not dependent
     * on the isHorizontal or isPositive flags.
     *
     * @param   surface  plot surface
     * @param  dataSpec  data specification
     * @param  geom   data geom
     * @param  dataStore   data store
     * @return  new plan object
     */
    private FillPlan createPlan( Surface surface, DataSpec dataSpec,
                                 DataGeom geom, DataStore dataStore ) {
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        Rectangle bounds = surface.getPlotBounds();
        Gridder gridder = new Gridder( bounds.width, bounds.height );
        Binner binner = new Binner( gridder.getLength() );
        int x0 = bounds.x;
        int y0 = bounds.y;
        int nx = bounds.width;
        int ny = bounds.height;
        int[] xlos = new int[ nx ];
        int[] xhis = new int[ nx ];
        int[] ylos = new int[ ny ];
        int[] yhis = new int[ ny ];
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) &&
                 surface.dataToGraphics( dpos, false, gp ) ) {
                int x = (int) gp.x - x0;
                int y = (int) gp.y - y0;
                boolean inX = x >= 0 && x < nx;
                boolean inY = y >= 0 && y < ny;
                if ( inX && inY ) {
                    binner.increment( gridder.getIndex( x, y ) );
                }
                else if ( inX ) {
                    ( y < 0 ? xlos : xhis )[ x ]++;
                }
                else if ( inY ) {
                    ( x < 0 ? ylos : yhis )[ y ]++;
                }
            }
        }
        return new FillPlan( binner, gridder, xlos, xhis, ylos, yhis,
                             geom, dataSpec, surface );
    }

    /**
     * Performs the actual painting of a fill plot onto a graphics context.
     *
     * @param  surface  plot surface
     * @param  plan    plan object appropriate for plot
     * @param  style   fill style
     * @param  g     target graphics context
     */
    private void paintFill( Surface surface, FillPlan plan, FillStyle style,
                            Graphics g ) {
        IndexColorModel colorModel = createAlphaColorModel( style.color_ );
        int nlevel = colorModel.getMapSize();
        boolean isHorizontal = style.isHorizontal_;
        boolean invert = isHorizontal ^ style.isPositive_;
        Binner binner = plan.binner_;
        final int[] xlos;
        final int[] xhis;
        final int[] ylos;
        final int[] yhis;
        final Gridder gridder;

        /* Horizontal plots can be handled with mostly the same logic,
         * as long as you tranpose X and Y in the grid indexing. */
        if ( isHorizontal ) {
            xlos = plan.ylos_;
            xhis = plan.yhis_;
            ylos = plan.xlos_;
            yhis = plan.xhis_;
            gridder = Gridder.transpose( plan.gridder_ );
        }
        else {
            xlos = plan.xlos_;
            xhis = plan.xhis_;
            ylos = plan.ylos_;
            yhis = plan.yhis_;
            gridder = plan.gridder_;
        }
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();

        /* Prepare the pixel grid for turning into an image.
         * Pixel values correspond to a scaled alpha value (0..nlevel)
         * in the painted image; zero if above the curve
         * (above all points landing in this pixel column),
         * and nlevel if below the curve (below all points landing in
         * this pixel column).  If it's above some points and below
         * others, it will be somewhere in between. */
        int[] pixels = new int[ gridder.getLength() ];
        boolean[] hasDatas = new boolean[ nx ];
        for ( int ix = 0; ix < nx; ix++ ) {

            /* Count how many points total in this pixel column. */
            int count = xlos[ ix ];
            for ( int iy = 0; iy < ny; iy++ ) {
                count += binner.getCount( gridder.getIndex( ix, iy ) );
            }
            count += xhis[ ix ];

            /* If there are any, work out if the current pixel is
             * above/below them. */
            if ( count > 0 ) {
                hasDatas[ ix ] = true;
                double factor = ( nlevel - 1 ) / (double) count;
                int sum = (invert ? xhis : xlos)[ ix ];
                for ( int iy = 0; iy < ny; iy++ ) {
                    int jy = invert ? ny - 1 - iy : iy;
                    int ig = gridder.getIndex( ix, jy );
                    sum += binner.getCount( ig );
                    pixels[ ig ] = (int) ( factor * sum );
                }
            }
        }

        /* Fill in gaps between plotted columns; if there are columns
         * with no data, fill them in as copies of the nearest column
         * that does have data. */
        int kx = -1;
        for ( int ix = 0; ix < nx; ix++ ) {
            if ( hasDatas[ ix ] ) {
                if ( kx != ix - 1 && kx >= 0 ) {
                    int mx = kx + ( ix - kx ) / 2;
                    for ( int jx = kx + 1; jx < ix; jx++ ) {
                        int lx = jx <= mx ? kx : ix;
                        for ( int iy = 0; iy < ny; iy++ ) {
                            pixels[ gridder.getIndex( jx, iy ) ] =
                                pixels[ gridder.getIndex( lx, iy ) ];
                        }
                    }
                }
                kx = ix;
            }
        }

        /* Work out whether there are any points outside the X range.
         * If so we will need to fill in columns at the left or right end. */
        boolean hasUnplottedLo = false;
        boolean hasUnplottedHi = false;
        for ( int iy = 0; iy < ny; iy++ ) {
            hasUnplottedLo = hasUnplottedLo || ylos[ iy ] > 0;
            hasUnplottedHi = hasUnplottedHi || yhis[ iy ] > 0;
        }
        if ( hasUnplottedLo || hasUnplottedHi ) {

            /* Work out the lowest and highest pixel columns with data. */
            int kxlo = -1;
            int kxhi = nx;
            for ( int ix = 0; ix < nx; ix++ ) {
                if ( hasDatas[ ix ] ) {
                    kxhi = ix;
                    if ( kxlo < 0 ) {
                        kxlo = ix;
                    }
                }
            }

            /* If there is unplotted data to the low end of the plot, copy
             * the lowest populated pixel column all the way to the left. */
            if ( hasUnplottedLo && kxlo > 0 ) {
                for ( int ix = 0; ix < kxlo; ix++ ) {
                    for ( int iy = 0; iy < ny; iy++ ) {
                        pixels[ gridder.getIndex( ix, iy ) ] =
                            pixels[ gridder.getIndex( kxlo, iy ) ];
                    }
                }
            }

            /* And do the same at the upper end. */
            if ( hasUnplottedHi && kxhi < nx - 1 ) {
                for ( int ix = kxhi + 1; ix < nx; ix++ ) {
                    for ( int iy = 0; iy < ny; iy++ ) {
                        pixels[ gridder.getIndex( ix, iy ) ] =
                            pixels[ gridder.getIndex( kxhi, iy ) ];
                    }
                }
            }
        }

        /* Turn the density map into a colour mapped image,
         * and paint it to the graphics context. */
        Rectangle bounds = surface.getPlotBounds();
        new PixelImage( bounds.getSize(), pixels, colorModel )
           .paintPixels( g, bounds.getLocation() );
    }

    /**
     * Constructs an indexed colour model representing all alpha values
     * up to the alpha of a supplied colour.
     *
     * @param  color   colour, may or may not have alpha
     * @return  colour map containing more-transparent versions of
     *          the supplied colour (entry 0 is alpha=0)
     */
    private static IndexColorModel createAlphaColorModel( Color color ) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();
        int size = alpha + 1;
        byte[] r = new byte[ size ];
        byte[] g = new byte[ size ];
        byte[] b = new byte[ size ];
        byte[] a = new byte[ size ];
        for ( int i = 0; i < size; i++ ) {
            r[ i ] = (byte) red;
            g[ i ] = (byte) green;
            b[ i ] = (byte) blue;
            a[ i ] = (byte) i;
        }
        return new IndexColorModel( 8, size, r, g, b, a );
    }

    /**
     * Style for fill plot.
     */
    public static class FillStyle implements Style {
        private final Color color_;
        private final boolean isHorizontal_;
        private final boolean isPositive_;

        /**
         * Constructor.
         *
         * @param  isHorizontal  true for horizontal fill, false for vertical
         * @param  isPositive   true to fill to positive infinity,
         *                      false to fill to negative infinity
         */
        public FillStyle( Color color, boolean isHorizontal,
                          boolean isPositive ) {
            color_ = color;
            isHorizontal_ = isHorizontal;
            isPositive_ = isPositive;
        }

        public Icon getLegendIcon() {
            final int[] data = { 2, 5, 7, 9, 10, 11, 10, 9, 7, 5, 2, };
            final int h = 12;
            return new Icon() {
                public int getIconWidth() {
                    return isHorizontal_ ? data.length : h;
                }
                public int getIconHeight() {
                    return isHorizontal_ ? h : data.length;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Color color0 = g.getColor();
                    g.setColor( color_ );
                    for ( int i = 0; i < data.length; i++ ) {
                        int d = data[ i ];
                        if ( isHorizontal_ ) {
                            g.drawRect( x + ( isPositive_ ? h - d : 0 ), y + i,
                                        d, 1 );
                        }
                        else {
                            g.drawRect( x + i, y + ( isPositive_ ? 0 : h - d ),
                                        1, d );
                        }
                    }
                    g.setColor( color0 );
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 222552;
            code = 23 * code + color_.hashCode();
            code = 23 * code + ( isHorizontal_ ? 3 : 5 );
            code = 23 * code + ( isPositive_ ? 7 : 11 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FillStyle ) {
                FillStyle other = (FillStyle) o;
                return this.color_.equals( other.color_ )
                    && this.isHorizontal_ == other.isHorizontal_
                    && this.isPositive_ == other.isPositive_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Plan object for fill plots.
     * This is an unweighted pixel density map (2d histogram),
     * plus a row of bins along each of the 4 edges containing counts
     * of all the points that are outside the map itself.
     */
    private static class FillPlan {
        final Binner binner_;
        final Gridder gridder_;
        final int[] xlos_;
        final int[] xhis_;
        final int[] ylos_;
        final int[] yhis_;
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Surface surface_;

        /**
         * Constructor.
         *
         * @param  binner  contains density map pixel counts
         * @param  gridder   encapsulates geometry for grid indexing
         * @param  xlos    bins counting all points above each pixel column
         * @param  xhis    bins counting all points below each pixel column
         * @param  ylos    bins counting all points to left of each pixel row
         * @param  yhis    bins counding all points to right of each pixel row
         * @param  geom   data geom
         * @param  dataSpec  data specification
         * @param  surface  plot surface
         */
        FillPlan( Binner binner, Gridder gridder,
                  int[] xlos, int[] xhis, int[] ylos, int[] yhis,
                  DataGeom geom, DataSpec dataSpec, Surface surface ) {
            binner_ = binner;
            gridder_ = gridder;
            xlos_ = xlos;
            xhis_ = xhis;
            ylos_ = ylos;
            yhis_ = yhis;
            geom_ = geom;
            dataSpec_ = dataSpec;
            surface_ = surface;
        }

        /**
         * Indicates wether this map's data is valid for a particular context.
         *
         * @param  geom   data geom
         * @param  dataSpec  data specification
         * @param  surface  plot surface
         * @return   true iff this map can be used for the given params
         */
        public boolean matches( DataGeom geom, DataSpec dataSpec,
                                Surface surface ) {
            return geom_.equals( geom )
                && dataSpec_.equals( dataSpec )
                && surface_.equals( surface );
        }
    }
}
