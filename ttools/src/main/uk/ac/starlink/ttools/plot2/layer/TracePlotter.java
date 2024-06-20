package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
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
import uk.ac.starlink.ttools.plot2.RangeCollector;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ComboBoxSpecifier;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.SubrangeConfigKey;
import uk.ac.starlink.ttools.plot2.config.UnitRangeSpecifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plots a line through a cloud of points in 2d, tracing a given
 * quantile or pair of quantiles at each column (or row) of pixels.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2016
 */
public abstract class TracePlotter
        extends AbstractPlotter<TracePlotter.TraceStyle> {

    private final boolean hasVertical_;
    private final String basicDescription_;
    private final ConfigKey<QJoin> joinKey_;

    /** Key to configure whether trace is vertical or horizontal. */
    public static final ConfigKey<Boolean> HORIZONTAL_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "horizontal", "Horizontal" )
           .setShortDescription( "Horizontal trace?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the trace bins are horizontal",
                "or vertical.",
                "If <code>true</code>, <m>y</m> quantiles are calculated",
                "for each pixel column, and",
                "if <code>false</code>, <m>x</m> quantiles are calculated",
                "for each pixel row.",
                "</p>",
            } )
        , true );

    /** Key to configure line thickness. */
    public static final ConfigKey<Integer> THICK_KEY =
        createThicknessKey();

    /** Key to configure target quantile range. */
    public static final ConfigKey<Subrange> QUANTILES_KEY =
        createQuantilesKey();

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
        , SMOOTHWIDTH_KEY, 0, true );

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

    private static final String QUANTILES_NAME = "quantiles";

    /**
     * Constructor.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   coordGrp  coordinate group
     * @param  hasVertical  true iff vertical fill is offered
     *                      (otherwise only horizontal)
     * @param  basicDescription  main part of XML description,
     *                           may be augmented by the base class
     * @param  dfltJoin  default join mode
     */
    public TracePlotter( String name, Icon icon, CoordGroup coordGrp,
                         boolean hasVertical, String basicDescription,
                         QJoin dfltJoin ) {
        super( name, icon, coordGrp, false );
        hasVertical_ = hasVertical;
        basicDescription_ = basicDescription;
        joinKey_ = createJoinKey( dfltJoin );
    }

    /**
     * Returns the config key for the Join Mode used by this plotter.
     *
     * @return  join mode config key
     */
    public ConfigKey<QJoin> getJoinModeKey() {
        return joinKey_;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            basicDescription_,
            "<p>Note: in the current implementation,",
            "depending on the details of the configuration and the data,",
            "there may be some distortions or missing graphics",
            "near the edges of the plot.",
            "This may be improved in future releases, depending on feedback.",
            "</p>",
        } );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        list.add( QUANTILES_KEY );
        list.add( THICK_KEY );
        list.add( SMOOTHSIZER_KEY );
        list.add( KERNEL_KEY );
        list.add( joinKey_ );
        if ( hasVertical_ ) {
            list.add( HORIZONTAL_KEY );
        }
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public TraceStyle createStyle( ConfigMap config ) {
        Color color = StyleKeys.getAlphaColor( config, StyleKeys.COLOR,
                                               StyleKeys.TRANSPARENCY );
        boolean isHorizontal = config.get( HORIZONTAL_KEY );
        Subrange qrange = config.get( QUANTILES_KEY );
        int thickness = config.get( THICK_KEY );
        Kernel1dShape kernelShape = config.get( KERNEL_KEY );
        BinSizer smoothSizer = config.get( SMOOTHSIZER_KEY );
        QJoin join = config.get( joinKey_ );
        return new TraceStyle( color, isHorizontal, thickness,
                               qrange.getLow(), qrange.getHigh(),
                               kernelShape, smoothSizer, join );
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
            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Span> auxSpans,
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
                        return createFillPlan( surface, dataSpec, geom, style,
                                               dataStore );
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
     * Creates a plan object suitable for this layer.
     *
     * @param  surface  plot surface
     * @param  dataSpec  data spec
     * @param  geom   geom
     * @param  style  style
     * @param  dataStore   data store
     * @return  populated plan
     */
    @Slow
    protected abstract FillPlan
            createFillPlan( Surface surface, DataSpec dataSpec, DataGeom geom,
                            TraceStyle style, DataStore dataStore );

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
        double w = style.smoothSizer_.getWidth( xLog, dlims[ 0 ], dlims[ 1 ],
                                                (Rounding) null );
        ReportMap report = new ReportMap();
        report.put( SMOOTHWIDTH_KEY, Double.valueOf( w ) );
        return report;
    }

    /**
     * Calculates the per-pixel-column quantile range values that will
     * form the basis of the plot.
     *
     * @param  surface  plot surface
     * @param  plan    plan object appropriate for plot
     * @param  style   trace style
     * @return  ordered array of QRange objects giving quantile ranges
     *          for each pixel column
     */
    private QRange[] getRanges( PlanarSurface surface, FillPlan plan,
                                TraceStyle style ) {
        boolean isHorizontal = style.isHorizontal_;
        int minThick = style.thickness_;
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
        }
        else {
            xlos = plan.getYlos();
            xhis = plan.getYhis();
            gridder = Gridder.transpose( plan.getGridder() );
        }
        int iXax = isHorizontal ? 0 : 1;
        int iYax = isHorizontal ? 1 : 0;
        Axis xAxis = surface.getAxes()[ iXax ];
        boolean xLog = surface.getLogFlags()[ iXax ];
        boolean yFlip = surface.getFlipFlags()[ iYax ];
        boolean yInvert = isHorizontal ^ yFlip;
        double qlo = yInvert ? 1.0 - style.qhi_ : style.qlo_;
        double qhi = yInvert ? 1.0 - style.qlo_ : style.qhi_;

        /* Smooth data in X direction as required by convolving with 1d kernel. 
         * NOTE: there will be edge effects associated with the kernel
         * operating at the left and right edges of the grid.
         * I could fix these as in the KDE plots by requiring a large
         * enough grid in the plan that the edge is outside of the
         * visible part of the plot.  Currently, I'm too lazy. */
        Kernel1d kernel =
            Pixel1dPlotter
           .createKernel( style.kernelShape_, style.smoothSizer_, xAxis, xLog,
                          false );
        GridData gdata = createGridData( kernel, xlos, xhis, binner, gridder );

        /* Iterate over each column of pixels. */
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();
        double[] line = new double[ ny ];
        List<QRange> qranges = new ArrayList<>();
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

            /* Identify the Y graphics coordinates corresponding to the
             * desired quantile range. */
            if ( count > 0 ) {
                int jylo = getRankIndex( xlo, xhi, count, line, qlo );
                int jyhi = qlo == qhi
                         ? jylo
                         : getRankIndex( xlo, xhi, count, line, qhi );
                int natThick = jyhi - jylo;
                assert natThick >= 0 : jylo + " - " + jyhi;
                int jy0;
                int thick;
                if ( natThick >= minThick ||
                     ( jylo < 0 && jyhi < 0 || jylo >= ny && jyhi >= ny ) ) {
                    jy0 = jylo;
                    thick = natThick;
                }
                else {
                    jy0 = jylo - minThick / 2;
                    thick = minThick;
                }
                qranges.add( new QRange( ix, jy0, jy0 + thick ) );
            }
        }
        return qranges.toArray( new QRange[ 0 ] );
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
        QRange[] ranges = getRanges( surface, plan, style );
        Color color0 = g.getColor();
        g.setColor( style.color_ );
        Rectangle bounds = surface.getPlotBounds();
        int x0 = bounds.x;
        int y0 = bounds.y;
        g.translate( x0, y0 );
        style.join_.paintTrace( ranges, style, g );
        g.translate( -x0, -y0 );
        g.setColor( color0 );
    }

    /**
     * Creates a TracePlotter instance for standard 2-d point cloud data.
     *
     * @param  hasVertical  true iff vertical fill is offered
     *                      (otherwise only horizontal)
     * @return   plotter instance
     */
    public static TracePlotter createPointsTracePlotter( boolean hasVertical ) {
        String descrip = String.join( "\n",
            "<p>Plots a line through a given quantile of the values",
            "binned within each pixel column (or row) of a plot.",
            "The line is optionally smoothed",
            "using a configurable kernel and width,",
            "to even out noise arising from the pixel binning.",
            "Instead of a simple line through a given quantile,",
            "it is also possible to fill the region between two quantiles.",
            "</p>",
            "<p>One way to use this is to draw a line estimating a function",
            "<m>y=f(x)</m> (or <m>x=g(y)</m>) sampled by a noisy set",
            "of data points in two dimensions.",
            "</p>",
        "" );
        final CoordGroup cgrp =
            CoordGroup.createCoordGroup( 1, new Coord[ 0 ] );
        return new TracePlotter( "Quantile", ResourceIcon.FORM_QUANTILE, cgrp,
                                 hasVertical, descrip, QJoin.NO_JOIN ) {
            protected FillPlan
                    createFillPlan( Surface surface, DataSpec dataSpec,
                                    DataGeom geom, TraceStyle style,
                                    DataStore dataStore ) {
                int icPos = cgrp.getPosCoordIndex( 0, geom );
                return FillPlan
                      .createPlan( surface, dataSpec, geom, icPos, dataStore );
            }
        };
    }

    /**
     * Creates a TracePlotter instance for X/Y array data.
     *
     * @param  hasVertical  true iff vertical fill is offered
     *                      (otherwise only horizontal)
     * @return   plotter instance
     */
    public static TracePlotter createArraysTracePlotter( boolean hasVertical ) {
        final FloatingArrayCoord xsCoord = FloatingArrayCoord.X;
        final FloatingArrayCoord ysCoord = FloatingArrayCoord.Y;
        final int icXs = 0;
        final int icYs = 1;
        String descrip = String.join( "\n",
            "<p>Displays a quantile or quantile range",
            "for a set of plotted X/Y array pairs.",
            "If a table contains one spectrum per row in array-valued",
            "wavelength and flux columns,",
            "this plotter can be used to display a median of all the spectra,",
            "or a range between two quantiles.",
            "Smoothing options are available to even out noise arising from",
            "the pixel binning.",
            "</p>",
            "<p>For each row, the",
            "<code>" + xsCoord.getInputs()[ 0 ].getMeta().getShortName()
                     + "</code> and",
            "<code>" + ysCoord.getInputs()[ 0 ].getMeta().getShortName()
                     + "</code> arrays",
            "must be the same length as each other,",
            "but this plot type does not require all the arrays",
            "to be sampled into the same bins.",
            "</p>",
            "<p>The algorithm calculates quantiles for all the X,Y points",
            "plotted in each column of pixels.",
            "This means that more densely sampled spectra have more influence",
            "on the output than sparser ones.",
            "</p>",
        "" );
        CoordGroup cgrp =
            CoordGroup
           .createPartialCoordGroup( new Coord[] { xsCoord, ysCoord },
                                     new boolean[] { true, true } );
        return new TracePlotter( "ArrayQuantile", ResourceIcon.FORM_QUANTILE,
                                 cgrp, hasVertical, descrip, QJoin.POLYGON ) {
            @Override
            public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                          TraceStyle style ) {
                Function<Tuple,XYArrayData> xyReader =
                    createXYArrayReader( dataSpec, style );
                if ( xyReader == null ) {
                    return null;
                }
                PlotLayer baseLayer = super.createLayer( geom, dataSpec, style);
                if ( baseLayer == null ) {
                    return null;
                }
                return new WrapperPlotLayer( baseLayer ) {
                    @Override
                    public void extendCoordinateRanges( Range[] ranges,
                                                        boolean[] logFlags,
                                                        DataStore dataStore ) {
                        super.extendCoordinateRanges( ranges, logFlags,
                                                      dataStore );
                        RangeCollector<TupleSequence> rangeCollector =
                                new RangeCollector<TupleSequence>( 2 ) {
                            public void accumulate( TupleSequence tseq,
                                                    Range[] ranges ) {
                                Range xRange = ranges[ 0 ];
                                Range yRange = ranges[ 1 ];
                                while ( tseq.next() ) {
                                    XYArrayData xyData = xyReader.apply( tseq );
                                    if ( xyData != null ) {
                                        int np = xyData.getLength();
                                        for ( int i = 0; i < np; i++ ) {
                                            xRange.submit( xyData.getX( i ) );
                                            yRange.submit( xyData.getY( i ) );
                                        }
                                    }
                                }
                            }
                        };
                        Range[] arrayRanges =
                            dataStore
                           .getTupleRunner()
                           .collect( rangeCollector,
                                     () -> dataStore
                                          .getTupleSequence( dataSpec ) );
                        rangeCollector.mergeRanges( ranges, arrayRanges );
                    }
                };
            }
            protected FillPlan
                    createFillPlan( Surface surface, DataSpec dataSpec,
                                    DataGeom geom, TraceStyle style,
                                    DataStore dataStore ) {
                return FillPlan
                      .createPlanArrays( surface, dataSpec, geom,
                                         createXYArrayReader( dataSpec, style ),
                                         dataStore );
            }

            /**
             * Returns a reader for matched X/Y array data.
             * If null is returned from this function,
             * no plotting should be done.
             *
             * @param  dataSpec  data specification
             * @param  style   plot style
             * @return  function to map tuples to XYArrayData;
             *          the function returns null for tuples
             *          that should not be plotted/accumulated
             */
            Function<Tuple,XYArrayData>
                    createXYArrayReader( DataSpec dataSpec, TraceStyle style ) {
                boolean isHorizontal = style.isHorizontal_;
                boolean hasX = ! dataSpec.isCoordBlank( icXs );
                boolean hasY = ! dataSpec.isCoordBlank( icYs );
                if ( hasX && hasY ) {
                    return tuple -> {
                        double[] xs = xsCoord.readArrayCoord( tuple, icXs );
                        double[] ys = ysCoord.readArrayCoord( tuple, icYs );
                        return xs != null && ys != null &&
                               xs.length == ys.length
                             ? new XYArrayData() {
                                   public int getLength() {
                                       return xs.length;
                                   }         
                                   public double getX( int i ) {
                                       return xs[ i ]; 
                                   } 
                                   public double getY( int i ) { 
                                       return ys[ i ];
                                   }
                               } 
                             : null; 
                    };       
                }
                else if ( hasY && isHorizontal ) {
                    return tuple -> {
                        double[] ys = ysCoord.readArrayCoord( tuple, icYs );
                        return ys != null
                             ? new XYArrayData() {
                                   public int getLength() {
                                       return ys.length;
                                   }
                                   public double getX( int i ) {
                                       return (double) i;
                                   }
                                   public double getY( int i ) {
                                       return ys[ i ];
                                   }
                               }
                             : null;
                    };
                }
                else if ( hasX && ! isHorizontal ) {
                    return tuple -> {
                        double[] xs = xsCoord.readArrayCoord( tuple, icXs );
                        return xs != null
                             ? new XYArrayData() {
                                   public int getLength() {
                                       return xs.length;
                                   }
                                   public double getX( int i ) {
                                       return xs[ i ];
                                   }
                                   public double getY( int i ) {
                                       return (double) i;
                                   }
                               }
                             : null;
                    };
                }
                else {
                    return null;
                }
            }
        };
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
     * @return  index within line corresponding to rank;
     *          -1 means before the first and line.length means after the last
     */
    private static int getRankIndex( double xlo, double xhi, double count,
                                     double[] line, double quantile ) {
        double rank = quantile * count;
        if ( ! ( count > 0 ) ) {
            return -1;
        }
        else if ( rank < xlo ) {
            return -1;
        }
        else if ( rank > count - xhi ) {
            return line.length;
        }
        else {
            double s = xlo;
            for ( int iy = 0; iy < line.length; iy++ ) {
                if ( s >= rank && s > 0 ) {
                    return iy;
                }
                s += line[ iy ];
            }
            return line.length;
        }
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
     * Returns a config key for minimal line thickness.
     *
     * @return  minimal thickness key
     */
    private static ConfigKey<Integer> createThicknessKey() {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setStringUsage( "<pixels>" );
        meta.setShortDescription( "Minimum line thickness in pixels" );
        meta.setXmlDescription( new String[] {
            "<p>Sets the minimum extent of the markers that are plotted",
            "in each pixel column (or row) to indicate the designated",
            "value range.",
            "If the range is zero sized",
            "(<code>" + QUANTILES_NAME + "</code>",
            "specifies a single value rather than a pair)",
            "this will give the actual thickness of the plotted line.",
            "If the range is non-zero however, the line may be thicker",
            "than this in places according to the quantile positions.",
            "</p>",
        } );
        return new IntegerConfigKey( meta, 3 ) {
            public Specifier<Integer> createSpecifier() {
                return new ComboBoxSpecifier<Integer>(
                               Integer.class,
                               new ThicknessComboBox( 7 ) );
            }
        };
    }

    /**
     * Returns a config key for target quantile range.
     *
     * @return  quantile range key
     */
    private static ConfigKey<Subrange> createQuantilesKey() {
        ConfigMeta meta = new ConfigMeta( QUANTILES_NAME, "Quantiles" );
        meta.setStringUsage( "<low-frac>[,<high-frac>]" );
        meta.setShortDescription( "Target quantile value or range" );
        meta.setXmlDescription( new String[] {
            "<p>Defines the quantile or quantile range",
            "of values that should be marked in each pixel column (or row).",
            "The value may be a single number in the range 0..1",
            "indicating the quantile which should be marked.",
            "Alternatively, it may be a pair of numbers,",
            "each in the range 0..1,",
            "separated by commas (<code>&lt;lo&gt;,&lt;hi&gt;</code>)",
            "indicating two quantile lines bounding an area to be filled.",
            "A pair of equal values \"<code>a,a</code>\"",
            "is equivalent to the single value \"<code>a</code>\".",
            "The default is <code>0.5</code>,",
            "which means to mark the median value in each column,",
            "and could equivalently be specified <code>0.5,0.5</code>.",
            "</p>",
        } );
        final Subrange dflt = new Subrange( 0.5, 0.5 );  // median
        return new SubrangeConfigKey( meta, dflt, 0, 1 ) {
            @Override
            public String valueToString( Subrange range ) {
                double lo = range.getLow();
                double hi = range.getHigh();
                return lo == hi ? format( lo )
                                : format( lo ) + "," + format( hi );
            }
            @Override
            public Subrange stringToValue( String txt ) throws ConfigException {
                Subrange r0;
                try {
                    double v0 = Double.parseDouble( txt.trim() );
                    r0 = new Subrange( v0, v0 );
                }
                catch ( NumberFormatException e ) {
                    r0 = null;
                }
                Subrange range = r0 == null ? super.stringToValue( txt ) : r0;
                double lo = range.getLow();
                double hi = range.getHigh();
                if ( ! ( lo >= 0 ) ) {
                    throw new ConfigException( this,
                                               "Bad lower bound: "
                                             + lo + " < 0" );
                }
                if ( ! ( hi <= 1 ) ) {
                    throw new ConfigException( this,
                                               "Bad upper bound: "
                                             + hi + " > 1" );
                }
                return range;
            }
            @Override
            public Specifier<Subrange> createSpecifier() {
                return new UnitRangeSpecifier( dflt );
            }
            private String format( double val ) {
                String txt = Double.toString( val );
                txt.replaceAll( "\\.0$", "" );
                return txt;
            }
        };
    }

    /**
     * Returns a config key for selecting sample join mode.
     *
     * @param  dfltJoin   default value
     * @return  new config key
     */
    private static ConfigKey<QJoin> createJoinKey( QJoin dfltJoin ) {
        ConfigMeta meta = new ConfigMeta( "join", "Join Mode" );
        meta.setShortDescription( "Drawing style for joining samples" );
        meta.setXmlDescription( new String[] {
            "<p>Defines the graphical style for connecting",
            "distinct quantile values.",
            "If smoothed samples are packed more closely than the pixel grid",
            "the option chosen here doesn't make much difference,",
            "but if there are gaps in the data along the sampled axis,",
            "it's useful to have a guide to the eye",
            "to join one quantile determination to the next.",
            "</p>",
        } );
        OptionConfigKey<QJoin> key =
                new OptionConfigKey<QJoin>( meta, QJoin.class, QJoin.OPTIONS,
                                            dfltJoin ) {
            public String getXmlDescription( QJoin join ) {
                return join.description_;
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    };

    /**
     * Defines the graphial style for joining sparse samples.
     */
    public static abstract class QJoin {

        final String name_;
        final String description_;

        /** No graphical elements between samples. */
        public static final QJoin NO_JOIN;

        /** Fill between upper and lower quantiles all along. */
        public static final QJoin POLYGON;

        /* Draw a line between centers of each quantile pair. */
        public static final QJoin LINES;

        /** All available options. */
        public static final QJoin[] OPTIONS = {
            NO_JOIN = new QJoin( "None",
                                 "displayed quantile ranges are not joined" ) {
                public void paintTrace( QRange[] ranges, TraceStyle style,
                                        Graphics g ) {
                    boolean isHorizontal = style.isHorizontal_;
                    for ( QRange qr : ranges ) {
                        int yleng = qr.iyhi_ - qr.iylo_;
                        if ( isHorizontal ) {
                            g.fillRect( qr.ix_, qr.iylo_, 1, yleng );
                        }
                        else {
                            g.fillRect( qr.iylo_, qr.ix_, yleng, 1 );
                        }
                    }
                }
            },
            POLYGON = new QJoin( "Polygon",
                                 "the area between "
                               + "a line connecting the upper quantiles and "
                               + "a line connecting the lower quantiles "
                               + "is filled" ) {
                public void paintTrace( QRange[] ranges, TraceStyle style,
                                        Graphics g ) {
                    boolean isHorizontal = style.isHorizontal_;
                    int n = ranges.length;
                    int[] xs = new int[ 2 * n ];
                    int[] ys = new int[ 2 * n ];
                    for ( int i = 0; i < n; i++ ) {
                        QRange qr = ranges[ i ];
                        int i1 = i;
                        int i2 = 2 * n - 1 - i;
                        if ( isHorizontal ) {
                            xs[ i1 ] = qr.ix_;
                            xs[ i2 ] = qr.ix_;
                            ys[ i1 ] = qr.iylo_;
                            ys[ i2 ] = qr.iyhi_;
                        }
                        else {
                            ys[ i1 ] = qr.ix_;
                            ys[ i2 ] = qr.ix_;
                            xs[ i1 ] = qr.iylo_;
                            xs[ i2 ] = qr.iyhi_;
                        }
                    }
                    g.fillPolygon( xs, ys, 2 * n );
                }
            },
            LINES = new QJoin( "Lines",
                               "a line of thickness given by "
                             + "<code>" + THICK_KEY.getMeta().getShortName()
                                        + "</code> "
                             + "is drawn from the center of each "
                             + "quantile range to the next" ) {
                public void paintTrace( QRange[] ranges, TraceStyle style,
                                        Graphics g ) {
                    boolean isHorizontal = style.isHorizontal_;
                    int thick = style.thickness_;
                    int nr = ranges.length;
                    for ( int ir = 0; ir < nr; ir++ ) {
                        QRange qr = ranges[ ir ];
                        int yleng = qr.iyhi_ - qr.iylo_;
                        if ( isHorizontal ) {
                            g.fillRect( qr.ix_, qr.iylo_, 1, yleng );
                        }
                        else {
                            g.fillRect( qr.iylo_, qr.ix_, yleng, 1 );
                        }
                    }
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( new BasicStroke( thick, BasicStroke.CAP_ROUND,
                                                   BasicStroke.JOIN_ROUND ) );
                    for ( int ir = 1; ir < nr; ir++ ) {
                        QRange qr0 = ranges[ ir - 1 ];
                        QRange qr1 = ranges[ ir ];
                        int y0 = ( qr0.iylo_ + qr0.iyhi_ ) / 2;
                        int y1 = ( qr1.iylo_ + qr1.iyhi_ ) / 2;
                        if ( isHorizontal ) {
                            g2.drawLine( qr0.ix_, y0, qr1.ix_, y1 );
                        }
                        else {
                            g2.drawLine( y0, qr0.ix_, y1, qr1.ix_ );
                        }
                    }
                    g2.setStroke( stroke0 );
                }
            },
        };

        /**
         * Constructor.
         *
         * @param  name  option name
         * @param  description   option description in XML
         */
        public QJoin( String name, String description ) {
            name_ = name;
            description_ = description;
        }

        /**
         * Paints graphics representing a set of quantile pairs.
         *
         * @param  ranges  quantile pair data
         * @param  style   plot style
         * @param  g       graphics context for output
         */
        public abstract void paintTrace( QRange[] ranges, TraceStyle style,
                                         Graphics g );

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Describes a calculated per-column quantile range.
     */
    private static class QRange {
        int ix_;
        int iylo_;
        int iyhi_;

        /**
         * Constructor.
         *
         * @param  ix     pixel horizontal index
         * @param  iylo   pixel vertical index of lower quantile
         * @param  iyhi   pixel vertical index of upper quantile
         */
        QRange( int ix, int iylo, int iyhi ) {
            ix_ = ix;
            iylo_ = iylo;
            iyhi_ = iyhi;
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
        private final double qlo_;
        private final double qhi_;
        private final Kernel1dShape kernelShape_;
        private final BinSizer smoothSizer_;
        private final QJoin join_;
        private final int[] TDATA = new int[] {
             3,  3,  3,  4,  5,  5,  6, 6, 6, 5, 5, 4, 4, 3, 3, 4, 4, 4, 3, 3,
        };
        private final int[] YDATA = new int[] {
            -1, -1, -2, -2, -2, -1, -1, 0, 1, 1, 2, 2, 1, 1, 0, 0 ,0 -1, -1, 0,
        };

        /**
         * Constructor.
         *
         * @param  color    colour
         * @param  isHorizontal  true for horizontal fill, false for vertical
         * @param  thickness   line thickness
         * @param  qlo   target lower quantile in range 0..1
         * @param  qhi   target upper quantile in range 0..1
         * @param  kernelShape     smoothing kernel
         * @param  smoothSizer     smoothing extent control
         * @param  join           join type
         */
        public TraceStyle( Color color, boolean isHorizontal, int thickness,
                           double qlo, double qhi, Kernel1dShape kernelShape,
                           BinSizer smoothSizer, QJoin join ) {
            color_ = color;
            isHorizontal_ = isHorizontal;
            thickness_ = thickness;
            qlo_ = qlo;
            qhi_ = qhi;
            kernelShape_ = kernelShape;
            smoothSizer_ = smoothSizer;
            join_ = join;
        }

        public Icon getLegendIcon() {
            final int width = MarkerStyle.LEGEND_ICON_WIDTH;
            final int height = MarkerStyle.LEGEND_ICON_HEIGHT;
            return new Icon() {
                public int getIconWidth() {
                    return width;
                }
                public int getIconHeight() {
                    return height;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Color color0 = g.getColor();
                    g.setColor( color_ );
                    for ( int ix = 0; ix < width; ix++ ) {
                        int yd = YDATA[ Math.min( ix, YDATA.length - 1 ) ];
                        int td;
                        if ( qlo_ == qhi_ ) {
                            td = thickness_;
                        }
                        else {
                            td = TDATA[ Math.min( ix, TDATA.length - 1 ) ];
                            if ( td < thickness_ ) {
                                td = thickness_;
                            }
                        }
                        g.fillRect( x + ix, y + ( height - td ) / 2 + yd ,
                                    1, td );
                    }
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
            code = 23 * code + Float.floatToIntBits( (float) qlo_ );
            code = 23 * code + Float.floatToIntBits( (float) qhi_ );
            code = 23 * code + PlotUtil.hashCode( kernelShape_ );
            code = 23 * code + smoothSizer_.hashCode();
            code = 23 * code + join_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof TraceStyle ) {
                TraceStyle other = (TraceStyle) o;
                return this.color_.equals( other.color_ )
                    && this.isHorizontal_ == other.isHorizontal_
                    && this.thickness_ == other.thickness_
                    && this.qlo_ == other.qlo_
                    && this.qhi_ == other.qhi_
                    && PlotUtil.equals( this.kernelShape_, other.kernelShape_)
                    && this.smoothSizer_.equals( other.smoothSizer_ )
                    && this.join_.equals( other.join_ );
            }
            else {
                return false;
            }
        }
    }
}
