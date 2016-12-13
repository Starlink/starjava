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
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
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

    /** Report key for smoothing width. */
    public static final ReportKey<Double> SMOOTHWIDTH_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "smoothwidth",
                                                   "Smoothing Width" ),
                                   false );

    /** Config key for smoothing width configuration. */
    public static final ConfigKey<BinSizer> SMOOTHSIZER_KEY =
        BinSizer.createSizerConfigKey(
            new ConfigMeta( "smooth", "Smoothing" )
           .setStringUsage( "+<width>|-<count>" ) 
           .setShortDescription( "Smoothing width specification" )
           .setXmlDescription( new String[] {
                "<p>Configures the smoothing width.",
                "This is the characteristic width of the kernel function",
                "to be convolved with the density in one dimension",
                "to smooth the quantile function.",
                "</p>",
                BinSizer.getConfigKeyDescription(),
            } )
        , SMOOTHWIDTH_KEY, 100, false, true );

    /** Config key for smoothing kernel shape. */
    public static final ConfigKey<Kernel1dShape> KERNEL_KEY =
        new OptionConfigKey<Kernel1dShape>(
            new ConfigMeta( "kernel", "Kernel" )
           .setShortDescription( "Smoothing kernel functional form" )
           .setXmlDescription( new String[] {
                "<p>The functional form of the smoothing kernel.",
                "The functions listed refer to the unscaled shape;",
                "all kernels are normalised to give a total area of unity.",
                "</p>",
            } )
        , Kernel1dShape.class,
        StandardKernel1dShape.getStandardOptions(),
        StandardKernel1dShape.EPANECHNIKOV ) {
            public String getXmlDescription( Kernel1dShape kshape ) {
                return kshape.getDescription();
            }
        }
       .setOptionUsage()
       .addOptionsXml();

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
            "binned within each pixel column (or row) of a plot.",
            "The line is optionally smoothed",
            "using a configurable kernel and width,",
            "to even out noise arising from the pixel binning.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        list.add( QUANTILE_KEY );
        list.add( THICK_KEY );
        list.add( SMOOTHSIZER_KEY );
        list.add( KERNEL_KEY );
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
        Kernel1dShape kernelShape = config.get( KERNEL_KEY );
        BinSizer smoothSizer = config.get( SMOOTHSIZER_KEY );
        return new TraceStyle( color, isHorizontal, thickness, quantile,
                               kernelShape, smoothSizer );
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
                final PlanarSurface psurf = (PlanarSurface) surface;
                final ReportMap report = createReport( style, psurf );
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
                                paintTrace( psurf, fplan, style, g );
                            }
                            public boolean isOpaque() {
                                return isOpaque;
                            }
                        } );
                    }
                    public ReportMap getReport( Object plan ) {
                        return report;
                    }
                };
            }
        };
    }

    /**
     * Prepares a report characterising the state of the plot when
     * a given style is applied to a given plotting surface.
     *
     * @param  style  style
     * @param  psurf  surface
     * @return  report
     */
    private ReportMap createReport( TraceStyle style, PlanarSurface psurf ) {
        int iax = style.isHorizontal_ ? 0 : 1;
        Axis xAxis = psurf.getAxes()[ iax ];
        boolean xLog = psurf.getLogFlags()[ iax ];
        double[] dlims = xAxis.getDataLimits();
        double w = style.smoothSizer_.getWidth( xLog, dlims[ 0 ], dlims[ 1 ] );
        ReportMap report = new ReportMap();
        report.put( SMOOTHWIDTH_KEY, new Double( w ) );
        return report;
    }

    /**
     * Performs the actual painting of a trace plot onto a graphics context.
     *
     * @param  surface  plot surface
     * @param  plan    plan object appropriate for plot
     * @param  style   trace style
     * @param  g     target graphics context
     */
    private void paintTrace( PlanarSurface surface, FillPlan plan,
                             TraceStyle style, Graphics g ) {
        boolean isHorizontal = style.isHorizontal_;
        int thickness = style.thickness_;
        final double quantile;
        Binner binner = plan.getBinner();
        final int[] xlos;
        final int[] xhis;
        final Gridder gridder;

        /* Horizontal and vertical plots can be handled with basically
         * the same logic, as long as you tranpose X and Y
         * in the grid indexing. */
        if ( isHorizontal ) {
            xlos = plan.getXlos();
            xhis = plan.getXhis();
            gridder = plan.getGridder();
            quantile = 1.0 - style.quantile_;
        }
        else {
            xlos = plan.getYlos();
            xhis = plan.getYhis();
            gridder = Gridder.transpose( plan.getGridder() );
            quantile = style.quantile_;
        }
        int iax = isHorizontal ? 0 : 1;
        Axis xAxis = surface.getAxes()[ iax ];
        boolean xLog = surface.getLogFlags()[ iax ];

        /* Smooth data in X direction as required by convolving with 1d kernel. 
         * NOTE: there will be edge effects associated with the kernel
         * operating at the left and right edges of the grid.
         * I could fix these as in the KDE plots by requiring a large
         * enough grid in the plan that the edge is outside of the
         * visible part of the plot.  Currently, I'm too lazy. */
        Kernel1d kernel =
            Pixel1dPlotter
           .createKernel( style.kernelShape_, style.smoothSizer_, xAxis, xLog );
        GridData gdata = createGridData( kernel, xlos, xhis, binner, gridder );

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
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        double[] line = new double[ ny ];
        for ( int ix = 0; ix < nx; ix++ ) {
            double xlo = gdata.getSumBelow( ix );
            double xhi = gdata.getSumAbove( ix );

            /* Store the data for this pixel column and count the total
             * number of values in it. */
            double count = xlo;
            for ( int iy = 0; iy < ny; iy++ ) {
                double c = gdata.getSample( ix, iy );
                line[ iy ] = c;
                count += c;
            }
            count += xhi;

            /* Identify the Y coordinate corresponding to the desired
             * quantile. */
            int jy = getRankIndex( xlo, xhi, count, line, quantile );

            /* Plot a marker. */
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
    private static int getRankIndex( double xlo, double xhi, double count,
                                     double[] line, double quantile ) {
        double rank = quantile * count;
        if ( count > 0 && rank >= xlo && rank <= count - xhi ) {
            double s = xlo;
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
     * Provides a GridData object from count data.
     *
     * @param  kernel  smoothing kernel
     * @param  xlos   sums of values below supplied grid per column
     * @param  xhis   sums of values above supplied grid per column
     * @param  binner   grided count data
     * @param  gridder   contains grid geometry
     */
    private static GridData createGridData( final Kernel1d kernel,
                                            final int[] xlos,
                                            final int[] xhis,
                                            final Binner binner,
                                            final Gridder gridder ) {
 
        /* If no smoothing is required, just return an adapter
         * implementation. */
        if ( kernel.getExtent() < 1 ) {
            return new GridData() {
                public double getSumBelow( int ix ) {
                    return xlos[ ix ];
                }
                public double getSumAbove( int ix ) {
                    return xhis[ ix ];
                }
                public double getSample( int ix, int iy ) {
                    return binner.getCount( gridder.getIndex( ix, iy ) );
                }
            };
        }

        /* Otherwise, copy and convolve the data columns with the
         * supplied kernel, and return a GridData implementation that is
         * a view on the result. */
        else {
            int nx = gridder.getWidth();
            int ny = gridder.getHeight();

            /* Convolve grid in the horizontal direction. */
            final double[] cdata = new double[ nx * ny ];
            double[] line = new double[ nx ];
            for ( int iy = 0; iy < ny; iy++ ) {
                for ( int ix = 0; ix < nx; ix++ ) {
                    line[ ix ] = binner.getCount( gridder.getIndex( ix, iy ) );
                }
                line = kernel.convolve( line );
                for ( int ix = 0; ix < nx; ix++ ) {
                    cdata[ gridder.getIndex( ix, iy ) ] = line[ ix ];
                }
            }

            /* Convolve above/below sums in the horizontal direction. */
            double[] dxlos = new double[ nx ];
            double[] dxhis = new double[ nx ];
            for ( int ix = 0; ix < nx; ix++ ) {
                dxlos[ ix ] = xlos[ ix ];
                dxhis[ ix ] = xhis[ ix ];
            }
            final double[] cxlos = kernel.convolve( dxlos );
            final double[] cxhis = kernel.convolve( dxhis );
            return new GridData() {
                public double getSumBelow( int ix ) {
                    return cxlos[ ix ];
                }
                public double getSumAbove( int ix ) {
                    return cxhis[ ix ];
                }
                public double getSample( int ix, int iy ) {
                    return cdata[ gridder.getIndex( ix, iy ) ];
                }
            };
        }
    }

    /**
     * Defines the ready-to-use grid data for calculating column medians.
     */
    private interface GridData {

        /**
         * Returns the sum of all the accumulated values below the
         * sample grid for a given column.
         *
         * @param   ix  notional horizontal coordinate
         * @return   sum of grid values below
         */
        double getSumBelow( int ix );

        /**
         * Returns the sum of all the accumulated values above the
         * sample grid for a given column.
         *
         * @param   ix  notional horizontal coordinate
         * @return   sum of grid values above
         */
        double getSumAbove( int ix );

        /**
         * Returns the accumulated value in a given grid cell.
         *
         * @param   ix  notional horizontal coordinate
         * @param   iy  notional vertical coordinate
         * @return  grid value
         */
        double getSample( int ix, int iy );
    }

    /**
     * Style for trace plot.
     */
    public static class TraceStyle implements Style {
        private final Color color_;
        private final boolean isHorizontal_;
        private final int thickness_;
        private final double quantile_;
        private final Kernel1dShape kernelShape_;
        private final BinSizer smoothSizer_;

        /**
         * Constructor.
         *
         * @param  color    colour
         * @param  isHorizontal  true for horizontal fill, false for vertical
         * @param  thickness   line thickness
         * @param  quantile   target quantile in range 0..1
         * @param  kernelShape     smoothing kernel
         * @param  smoothSizer     smoothing extent control
         */
        public TraceStyle( Color color, boolean isHorizontal, int thickness,
                           double quantile, Kernel1dShape kernelShape,
                           BinSizer smoothSizer ) {
            color_ = color;
            isHorizontal_ = isHorizontal;
            thickness_ = thickness;
            quantile_ = quantile;
            kernelShape_ = kernelShape;
            smoothSizer_ = smoothSizer;
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
            code = 23 * code + PlotUtil.hashCode( kernelShape_ );
            code = 23 * code + smoothSizer_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof TraceStyle ) {
                TraceStyle other = (TraceStyle) o;
                return this.color_.equals( other.color_ )
                    && this.isHorizontal_ == other.isHorizontal_
                    && this.thickness_ == other.thickness_
                    && this.quantile_ == other.quantile_
                    && PlotUtil.equals( this.kernelShape_, other.kernelShape_)
                    && this.smoothSizer_.equals( other.smoothSizer_ );
            }
            else {
                return false;
            }
        }
    }
}
