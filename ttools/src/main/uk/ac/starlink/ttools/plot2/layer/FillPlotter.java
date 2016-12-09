package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;
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

    private final boolean hasHorizontal_;

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
     *
     * @param  hasHorizontal  true iff horizontal fill is offered
     *                        (otherwise only vertical)
     */
    public FillPlotter( boolean hasHorizontal ) {
        super( "Fill", ResourceIcon.FORM_FILL, 1, new Coord[ 0 ] );
        hasHorizontal_ = hasHorizontal;
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
            "The filled area may alternatively be that above the curve"
            + ( hasHorizontal_ ? " or to its left or right." : "." ),
            "</p>",
            "<p>One example of its use is to reconstruct the appearance",
            "of a histogram plot from a set of histogram bins.",
            "For X,Y data which is not single-valued, the result",
            "may not be very useful.",
            "</p>"
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        if ( hasHorizontal_ ) {
            list.add( HORIZONTAL_KEY );
        }
        list.add( POSITIVE_KEY );
        return list.toArray( new ConfigKey[ 0 ] );
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
            final int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
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
                        return FillPlan.createPlan( surface, dataSpec, geom,
                                                    icPos, dataStore );
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
        Binner binner = plan.getBinner();
        final int[] xlos;
        final int[] xhis;
        final int[] ylos;
        final int[] yhis;
        final Point cpXlo;
        final Point cpXhi;
        final Gridder gridder;

        /* Horizontal plots can be handled with mostly the same logic,
         * as long as you tranpose X and Y in the grid indexing. */
        if ( isHorizontal ) {
            xlos = plan.getYlos();
            xhis = plan.getYhis();
            ylos = plan.getXlos();
            yhis = plan.getXhis();
            cpXlo = transposePoint( plan.getCpYlo() );
            cpXhi = transposePoint( plan.getCpYhi() );
            gridder = Gridder.transpose( plan.getGridder() );
        }
        else {
            xlos = plan.getXlos();
            xhis = plan.getXhis();
            ylos = plan.getYlos();
            yhis = plan.getYhis();
            cpXlo = plan.getCpXlo();
            cpXhi = plan.getCpXhi();
            gridder = plan.getGridder();
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

            /* If there are no points in the whole plot area, but there are
             * unplotted points outside of it, we need to work out how far
             * up the plot the fill will go, and copy it across the whole
             * width. */
            if ( kxlo < 0 && kxhi >= nx && cpXlo != null && cpXhi != null ) {
                for ( int ix = 0; ix < nx; ix++ ) {
                    for ( int iy = 0; iy < ny; iy++ ) {
                        Point closest = ( ix - cpXlo.x ) < ( cpXhi.x - ix )
                                      ? cpXlo : cpXhi;
                        boolean isFill = invert ^ ( iy > closest.y );
                        pixels[ gridder.getIndex( ix, iy ) ] =
                            isFill ? nlevel - 1 : 0;
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
     * Returns the transpose of the given graphics position.
     * Null input gives null result without error.
     *
     * @param  gp  input point
     * @return   transpose of gp
     */
    private static Point transposePoint( Point gp ) {
        return gp == null ? null
                          : new Point( gp.y, gp.x );
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
         * @param  color    colour
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
}
