package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Range;
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
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plots a line through a cloud of points in 2d, tracing a given
 * quantile at each column (or row) of pixels.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2016
 */
public class TracePlotter extends AbstractPlotter<TracePlotter.TraceStyle> {

    private final boolean hasVertical_;

    /** Key to configure whether trace is vertical or horizontal. */
    public static final ConfigKey<Boolean> HORIZONTAL_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "horizontal", "Horizontal" )
           .setShortDescription( "Horizontal trace?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the trace bins are horizontal",
                "or vertical.",
                "</p>",
            } )
        , true );

    /** Key to configure line thickness. */
    public static final ConfigKey<Integer> THICK_KEY =
        StyleKeys.createThicknessKey( 3 );

    /** Key to configure target quantile. */
    public static final ConfigKey<Double> QUANTILE_KEY =
        new DoubleConfigKey( 
            new ConfigMeta( "quantile", "Quantile" )
           .setShortDescription( "Target quantile" )
           .setXmlDescription( new String[] {
                "<p>Value between 0 and 1 for the quantile",
                "at which a value should be plotted.",
                "</p>",
            } ),
        0.5 ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( 0, 1, false, 0.5, false,
                                            SliderSpecifier
                                           .TextOption.ENTER_ECHO );
            }
        };

    /**
     * Constructor.
     *
     * @param  hasVertical  true iff vertical fill is offered
     *                      (otherwise only horizontal)
     */
    public TracePlotter( boolean hasVertical ) {
        super( "Quantile", ResourceIcon.FORM_QUANTILE, 1, new Coord[ 0 ] );
        hasVertical_ = hasVertical;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a line through a given quantile of the values",
            "in each pixel column of a plot.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        list.add( QUANTILE_KEY );
        list.add( THICK_KEY );
        if ( hasVertical_ ) {
            list.add( HORIZONTAL_KEY );
        }
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public TraceStyle createStyle( ConfigMap config ) {
        Color color = StyleKeys.getAlphaColor( config, StyleKeys.COLOR,
                                               StyleKeys.TRANSPARENCY );
        boolean isHorizontal = config.get( HORIZONTAL_KEY );
        double quantile = config.get( QUANTILE_KEY );
        int thickness = config.get( THICK_KEY );
        return new TraceStyle( color, isHorizontal, thickness, quantile );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final TraceStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        Color color = style.color_;
        final boolean isOpaque = color.getAlpha() == 255;
        LayerOpt layerOpt = new LayerOpt( color, isOpaque );
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
                                paintTrace( surface, fplan, style, g );
                            }
                            public boolean isOpaque() {
                                return isOpaque;
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
     * Performs the actual painting of a trace plot onto a graphics context.
     *
     * @param  surface  plot surface
     * @param  plan    plan object appropriate for plot
     * @param  style   trace style
     * @param  g     target graphics context
     */
    private void paintTrace( Surface surface, FillPlan plan, TraceStyle style,
                             Graphics g ) {
        boolean isHorizontal = style.isHorizontal_;
        int thickness = style.thickness_;
        final double quantile;
        Binner binner = plan.getBinner();
        final int[] xlos;
        final int[] xhis;
        final int[] ylos;
        final int[] yhis;
        final Gridder gridder;

        /* Horizontal plots can be handled with mostly the same logic,
         * as long as you tranpose X and Y in the grid indexing. */
        if ( isHorizontal ) {
            xlos = plan.getXlos();
            xhis = plan.getXhis();
            ylos = plan.getYlos();
            yhis = plan.getYhis();
            gridder = plan.getGridder();
            quantile = 1.0 - style.quantile_;
        }
        else {
            xlos = plan.getYlos();
            xhis = plan.getYhis();
            ylos = plan.getXlos();
            yhis = plan.getXhis();
            gridder = Gridder.transpose( plan.getGridder() );
            quantile = style.quantile_;
        }
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();

        /* Prepare to paint lines. */
        Color color0 = g.getColor();
        g.setColor( style.color_ );
        Rectangle bounds = surface.getPlotBounds();
        int x0 = bounds.x;
        int y0 = bounds.y;
        if ( isHorizontal ) {
            y0 -= thickness / 2;
        }
        else {
            x0 -= thickness / 2;
        }

        /* Iterate over each column of pixels. */
        int[] line = new int[ ny ];
        for ( int ix = 0; ix < nx; ix++ ) {
            int xlo = xlos[ ix ];
            int xhi = xhis[ ix ];

            /* Store the data for this pixel column and count the total
             * number of values in it. */
            int count = xlo;
            for ( int iy = 0; iy < ny; iy++ ) {
                int c = binner.getCount( gridder.getIndex( ix, iy ) );
                line[ iy ] = c;
                count += c;
            }
            count += xhi;

            /* Identify the Y coordinate corresponding to the desired
             * quantile. */
            int jy = getRankIndex( xlo, xhi, count, line, quantile );

            /* Plot a line to mark it. */
            if ( jy >= 0 ) {
                if ( isHorizontal ) {
                    g.fillRect( x0 + ix, y0 + jy, 1, thickness );
                }
                else {
                    g.fillRect( x0 + jy, y0 + ix, thickness, 1 );
                }
            }
        }

        /* Restore graphics context. */
        g.setColor( color0 );
    }

    /**
     * Identifies the index within a line of counts at which a given
     * value is exceeded.
     *
     * @param  xlo   sum of counts before line starts
     * @param  xhi   sum of counts after line ends
     * @param  count   total of all counts before, during and after line
     * @param  line    array of count values in grid
     * @param  quantile   value between 0 and 1 indicating target item
     */
    private static int getRankIndex( int xlo, int xhi, int count, int[] line,
                                     double quantile ) {
        int rank = (int) Math.round( quantile * count );
        if ( count > 0 && rank >= xlo && rank <= count - xhi ) {
            int s = xlo;
            for ( int iy = 0; iy < line.length; iy++ ) {
                if ( s >= rank && s > 0 ) {
                    return iy;
                }
                s += line[ iy ];
            }
        }
        return -1;
    }

    /**
     * Style for trace plot.
     */
    public static class TraceStyle implements Style {
        private final Color color_;
        private final boolean isHorizontal_;
        private final int thickness_;
        private final double quantile_;

        /**
         * Constructor.
         *
         * @param  color    colour
         * @param  isHorizontal  true for horizontal fill, false for vertical
         * @param  thickness   line thickness
         * @param  quantile   target quantile in range 0..1
         */
        public TraceStyle( Color color, boolean isHorizontal, int thickness,
                           double quantile ) {
            color_ = color;
            isHorizontal_ = isHorizontal;
            thickness_ = thickness;
            quantile_ = quantile;
        }

        public Icon getLegendIcon() {
            final int width = MarkStyle.LEGEND_ICON_WIDTH;
            final int height = MarkStyle.LEGEND_ICON_HEIGHT;
            return new Icon() {
                public int getIconWidth() {
                    return width;
                }
                public int getIconHeight() {
                    return height;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Color color0 = g.getColor();
                    int y1 = y + ( height - thickness_ ) / 2;
                    g.setColor( color_ );
                    g.fillRect( x, y + ( height - thickness_ ) / 2,
                                width, thickness_ );
                    g.setColor( color0 );
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 4422621;
            code = 23 * code + color_.hashCode();
            code = 23 * code + ( isHorizontal_ ? 3 : 5 );
            code = 23 * code + thickness_;
            code = 23 * code + Float.floatToIntBits( (float) quantile_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof TraceStyle ) {
                TraceStyle other = (TraceStyle) o;
                return this.color_.equals( other.color_ )
                    && this.isHorizontal_ == other.isHorizontal_
                    && this.thickness_ == other.thickness_
                    && this.quantile_ == other.quantile_;
            }
            else {
                return false;
            }
        }
    }
}
