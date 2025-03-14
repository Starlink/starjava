package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;
import javax.swing.Icon;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plotter that plots a genuine density map (2-d histogram) on a regular grid.
 * It presents a single Decal, no glyph.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2017
 */
public class GridPlotter implements Plotter<GridPlotter.GridStyle> {

    private final boolean transparent_;
    private final boolean reportAuxKeys_;

    /** Weighting coordinate. */
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;

    /** ReportKey for actual X bin extent. */
    public static final ReportKey<Double> XBINWIDTH_KEY =
        createBinWidthReportKey( 'x' );

    /** ReportKey for actual Y bin extent. */
    public static final ReportKey<Double> YBINWIDTH_KEY =
        createBinWidthReportKey( 'y' );

    /** ReportKey for exported grid table. */
    public static final ReportKey<StarTable> GRIDTABLE_KEY =
        ReportKey.createTableKey( new ReportMeta( "grid_map", "Grid Map" ),
                                  true );

    /** Config key for X bin size configuration. */
    public static final ConfigKey<BinSizer> XBINSIZER_KEY =
        createBinSizerConfigKey( 'x', XBINWIDTH_KEY );

    /** Config key for Y bin size configuration. */
    public static final ConfigKey<BinSizer> YBINSIZER_KEY =
        createBinSizerConfigKey( 'y', YBINWIDTH_KEY );

    /** Config key for X bin phase. */
    public static final ConfigKey<Double> XPHASE_KEY = createPhaseKey( 'x' );

    /** Config key for Y bin phase. */
    public static final ConfigKey<Double> YPHASE_KEY = createPhaseKey( 'y' );

    /** Config key for combination mode. */
    public static final ConfigKey<Combiner> COMBINER_KEY =
        new OptionConfigKey<Combiner>(
            new ConfigMeta( "combine", "Combine" )
           .setShortDescription( "Value combination mode" )
           .setXmlDescription( new String[] {
               "<p>Defines how values contributing to the same grid cell",
               "are combined together to produce the value",
               "assigned to that cell, and hence its colour.",
               "The combined values are the weights, but if the",
               "<code>" + WEIGHT_COORD.getInput().getMeta().getShortName()
                        + "</code> coordinate",
               "is left blank, a weighting of unity is assumed.",
               "</p>",
            } )
        , Combiner.class, Combiner.getKnownCombiners(), Combiner.MEAN ) {
        public String getXmlDescription( Combiner combiner ) {
            return combiner.getDescription();
        }
    }.setOptionUsage()
     .addOptionsXml();

    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;
    private static final CoordGroup COORD_GROUP =
        CoordGroup
       .createCoordGroup( 1, new Coord[] { FloatingCoord.WEIGHT_COORD } );

    /** Tuning parameter, fractional external border for grid calculations. */
    private static final double PADDING = 0.8;

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     */
    public GridPlotter( boolean transparent ) {
        transparent_ = transparent;

        /* Set reportAuxKeys false, since the colour ramp config will
         * usually be controlled globally at the level of the plot. */
        reportAuxKeys_ = false;
    }

    public String getPlotterName() {
        return "Grid";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_GRID;
    }

    public CoordGroup getCoordGroup() {
        return COORD_GROUP;
    }

    public boolean hasReports() {
        return true;
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots 2-d data aggregated into rectangular cells.\n" )
            .append( "You can optionally use a weighting for the points,\n" )
            .append( "and you can configure how the values are combined\n" )
            .append( "to produce the output pixel values (colours).\n" )
            .append( "You can use this plotter in various ways,\n" )
            .append( "including as a 2-d histogram or weighted density map,\n" )
            .append( "or to plot gridded data.\n" )
            .append( "</p>\n" )
            .append( "<p>The X and Y dimensions of the\n" )
            .append( "grid cells (or equivalently histogram bins)\n" )
            .append( "can be configured either\n" )
            .append( "in terms of the data coordinates\n" )
            .append( "or relative to the plot dimensions.\n" )
            .append( "</p>\n" );
        sbuf.append( "<p>" );
        if ( reportAuxKeys_ ) {
            sbuf.append( "There are additional options to adjust\n" )
                .append( "the way data values are mapped to colours.\n" );
        }
        else {
            sbuf.append( "The way that data values are mapped\n" )
                .append( "to colours is usually controlled by options\n" )
                .append( "at the level of the plot itself,\n" )
                .append( "rather than by per-layer configuration.\n" );
        }
        sbuf.append( "</p>\n" );
        return sbuf.toString();
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> keyList = new ArrayList<ConfigKey<?>>();
        keyList.add( XBINSIZER_KEY );
        keyList.add( YBINSIZER_KEY );
        keyList.add( COMBINER_KEY );
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        keyList.add( XPHASE_KEY );
        keyList.add( YPHASE_KEY );
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public GridStyle createStyle( ConfigMap config ) {
        BinSizer xSizer = config.get( XBINSIZER_KEY );
        BinSizer ySizer = config.get( YBINSIZER_KEY );
        double xPhase = config.get( XPHASE_KEY ).doubleValue();
        double yPhase = config.get( YPHASE_KEY ).doubleValue();
        Combiner combiner = config.get( COMBINER_KEY );
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new GridStyle( xSizer, ySizer, xPhase, yPhase,
                              scaling, dataclip, shader, combiner );
    }

    public Object getRangeStyleKey( GridStyle style ) {
        return null;
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  GridStyle style ) {
        return new GridLayer( this, geom, dataSpec, style );
    }
    
    /**
     * Constructs a ReportKey for identifying actual bin width on a given axis.
     *
     * @param  axname  axis identifier
     * @return   new report key
     */
    private static ReportKey<Double> createBinWidthReportKey( char axname ) {
        String sname = axname + "binwidth";
        String lname = toUpperString( axname ) + " Bin Width";
        return ReportKey.createDoubleKey( new ReportMeta( sname, lname ),
                                          false );
    }

    /**
     * Constructs a ConfigKey for configuring the bin width on a given axis.
     *
     * @param  axname  axis identifier
     * @param  widthRepKey   associated report key for reporting actual size
     * @return  new config key
     */
    private static ConfigKey<BinSizer>
            createBinSizerConfigKey( char axname,
                                     ReportKey<Double> widthRepKey ) {
        String axName = toUpperString( axname );
        ConfigMeta meta = new ConfigMeta( axname + "binsize",
                                          axName + " Bin Size" );
        meta.setStringUsage( "+<extent>|-<count>" );
        meta.setShortDescription( axName + " bin size specification" );
        meta.setXmlDescription( new String[] {
            "<p>Configures the extent of the density grid bins",
            "on the " + axName + " axis.",
            "</p>",
            "<p>If the supplied value is a positive number",
            "it is interpreted as a fixed size in data coordinates",
            "(if the " + axName + " axis is logarithmic,",
            "the value is a fixed factor).",
            "If it is a negative number, then it will be interpreted",
            "as the approximate number of bins to display across",
            "the plot in the " + axName + " direction",
            "(though an attempt is made to use only round numbers",
            "for bin sizes).",
            "</p>",
            "<p>When setting this value graphically,",
            "you can use either the slider to adjust the bin count",
            "or the numeric entry field to fix the bin size.",
            "</p>",
        } );
        boolean allowZero = false;
        return BinSizer.createSizerConfigKey( meta, widthRepKey, 30,
                                              allowZero );
    }

    /**
     * Constructs a ConfigKey for configuring bin phase on a given axis.
     *
     * @param  axname  axis identifier
     * @return   new config key
     */
    private static ConfigKey<Double> createPhaseKey( char axname ) {
        String axName = toUpperString( axname );
        ConfigMeta meta = new ConfigMeta( axname + "phase",
                                          axName + " Bin Phase" );
        meta.setShortDescription( axName + " axis bin offset" );
        meta.setXmlDescription( new String[] {
            "<p>Controls where the zero point on the " + axName + " axis",
            "is set.",
            "For instance if your bin size is 1,",
            "this value controls whether bin boundaries are at",
            "0, 1, 2, .. or 0.5, 1.5, 2.5, ... etc.",
            "</p>",
            "<p>A value of 0 (or any integer) will result in",
            "a bin boundary at X=0 (linear X axis)",
            "or X=1 (logarithmic X axis).",
            "A fractional value will give a bin boundary at",
            "that value multiplied by the bin width.",
            "</p>",
        } );
        return DoubleConfigKey
              .createSliderKey( meta, 0, 0, 1, false, false,
                                SliderSpecifier.TextOption.ENTER_ECHO );
    }

    /**
     * Utility method to turn a lower case character into a string
     * containing its upper-cased equivalent.
     *
     * @param  axname  character
     * @return   one-character string containing upper-cased character
     */
    private static String toUpperString( char axname ) {
        return Character.valueOf( Character.toUpperCase( axname ) ).toString();
    }

    /**
     * Returns the approximate extent of a graphics pixel
     * on an axis in scale units.
     * The idea of the rounding is that this should produce a result
     * that does not differ if the axis is simply translated;
     * that may be important if you want to use the grid size to label
     * subsequent grid data results, to avoid recalculating them
     * if the requirements have not significantly changed.
     *
     * @param  axis  axis
     * @return   extent of a graphics pixel in scale units
     */
    private static double getRoundedPixelScaleWidth( Axis axis ) {
        int glo = axis.getGraphicsLimits()[ 0 ];
        Scale scale = axis.getScale();
        double d1 = axis.graphicsToData( glo );
        double d2 = axis.graphicsToData( glo + 1 );
        double sextent =
            Math.abs( scale.dataToScale( d2 ) - scale.dataToScale( d1 ) );

        /* Try to round the result so that it's not sensitive to tiny
         * precision-related changes in the calculations.
         * As a blunt instrument, just cast it to float which should
         * chop off some significant figures.  I think that ought to work,
         * though it might lead to problems with small logarithmic pixels
         * (for which the result will be near to unity). */
        return (float) sextent;
    }

    /**
     * Does the actual painting.
     *
     * @param  g  graphics context
     * @param  pixer   defines pixel grid
     * @param  binResult  grid data
     * @param  ctype   combiner type
     * @param  scaler   scales bin values to unit range
     * @param  colorModel  colour map; index zero corresponds to transparency
     * @param  surface   plot surface
     */
    private static void paintBins( Graphics g, GridPixer pixer,
                                   BinList.Result binResult,
                                   Combiner.Type ctype,
                                   Scaler scaler, IndexColorModel colorModel,
                                   PlanarSurface surface ) {
        int ncolor = colorModel.getMapSize() - 1;
        double binFactor = pixer.getBinFactor( ctype );

        /* Sample bin grid onto output pixel grid. */
        if ( true ) {
            Rectangle bounds = surface.getPlotBounds();
            int nx = bounds.width;
            int ny = bounds.height;
            int x0 = bounds.x;
            int y0 = bounds.y;
            Gridder gridder = new Gridder( nx, ny );
            int npix = gridder.getLength();
            int[] grid = new int[ npix ];
            Point2D.Double gp = new Point2D.Double();
            int ib0 = -1;
            int sval = -1;
            for ( int ip = 0; ip < npix; ip++ ) {
                gp.x = x0 + gridder.getX( ip );
                gp.y = y0 + gridder.getY( ip );
                double[] dpos = surface.graphicsToData( gp, null );
                int ib = pixer.getBinIndex( dpos );
                if ( ib >= 0 ) {
                    if ( ib != ib0 ) {
                        ib0 = ib;
                        double dval = binFactor * binResult.getBinValue( ib );
                        sval = Double.isNaN( dval )
                             ? 0
                             : Math.min( 1 + (int) ( scaler.scaleValue( dval )
                                                     * ncolor ),
                                         ncolor - 1 );
                    }
                    grid[ ip ] = sval;
                }
            }

            /* Paint the pixel grid. */
            new PixelImage( new Dimension( nx, ny ), grid, colorModel )
               .paintPixels( g, new Point( x0, y0 ) );
        }

        /* Alternatively, paint each pixel separately.
         * I'd expect this to be much faster for large pixels, but in
         * interactive tests it didn't seem to be.  Also, there seem to
         * be numeric problems in some cases.
         * So stick to the other method for now. */
        else {
            double[][] dlims = surface.getDataLimits();
            int[] ixRange = pixer.xgrid_.getBinRange( dlims[ 0 ] );
            int[] iyRange = pixer.ygrid_.getBinRange( dlims[ 1 ] );
            int ixlo = ixRange[ 0 ];
            int ixhi = ixRange[ 1 ];
            int iylo = iyRange[ 0 ];
            int iyhi = iyRange[ 1 ];
            Color[] colors = new Color[ ncolor + 1 ];
            for ( int ic = 0; ic < ncolor + 1; ic++ ) {
                colors[ ic ] = new Color( colorModel.getRGB( ic ), true );
            }
            BinMapper xMapper = pixer.xgrid_.mapper_;
            BinMapper yMapper = pixer.ygrid_.mapper_;
            Axis xAxis = surface.getAxes()[ 0 ];
            Axis yAxis = surface.getAxes()[ 1 ];
            Color color0 = g.getColor();
            for ( int iy = iylo; iy <= iyhi; iy++ ) {
                for ( int ix = ixlo; ix <= ixhi; ix++ ) {
                    int ibin = pixer.getBinIndex( ix, iy );
                    assert ibin >= 0;
                    double dval = binFactor * binResult.getBinValue( ibin );
                    if ( ! Double.isNaN( dval ) ) {
                        int sval =
                            Math.min( 1 + (int) ( scaler.scaleValue( dval )
                                                  * ncolor ),
                                      ncolor - 1 );
                        g.setColor( colors[ sval ] );
                        int[] gxs = getGraphicsBounds( ix, xMapper, xAxis );
                        int[] gys = getGraphicsBounds( iy, yMapper, yAxis );
                        g.fillRect( gxs[ 0 ], gys[ 0 ],
                                    gxs[ 1 ] - gxs[ 0 ], gys[ 1 ] - gys[ 0 ] );
                    }
                }
            }
            g.setColor( color0 );
        }
    }

    /**
     * Calculates the 1-d bounds in graphics coordinates for a given grid bin.
     *
     * @param  ibin  bin index
     * @param  mapper   bin mapper
     * @param  axis    graphical axis
     * @return   2-element (lo,hi) bounds of bin in graphics coordinates
     */
    private static int[] getGraphicsBounds( int ibin, BinMapper mapper,
                                            Axis axis ) {
        double[] dlimits = mapper.getBinLimits( ibin );
        int g0 = (int) Math.floor( axis.dataToGraphics( dlimits[ 0 ] ) );
        int g1 = (int) Math.floor( axis.dataToGraphics( dlimits[ 1 ] ) );
        return g0 <= g1 ? new int[] { g0, g1 }
                        : new int[] { g1, g0 };
    }

    /**
     * Returns a table giving the contents of a given grid plan projected
     * on to a given surface.
     * Table contents are constructed lazily so this is not an expensive
     * operation.
     *
     * @param  surface  plotting surface
     * @param  gplan    plan containing grid data
     */
    private static StarTable createExportTable( PlanarSurface surface,
                                                GridPlan gplan ) {
        GridPixer pixer = gplan.pixer_;

        /* Work out the bounds of the grid that the exported table will cover.
         * Note this is derived from the plot surface, it's not necessarily
         * the same as the grid from the GridPlan, which may be oversized. */
        double[][] dataLimits = surface.getDataLimits();
        int ixlo = pixer.xgrid_.mapper_.getBinIndex( dataLimits[ 0 ][ 0 ] );
        int ixhi = pixer.xgrid_.mapper_.getBinIndex( dataLimits[ 0 ][ 1 ] );
        int iylo = pixer.ygrid_.mapper_.getBinIndex( dataLimits[ 1 ][ 0 ] );
        int iyhi = pixer.ygrid_.mapper_.getBinIndex( dataLimits[ 1 ][ 1 ] );

        /* Prepare mappings from the exported table row indices
         * to the binned grid. */
        int tw = ixhi - ixlo + 1;
        int th = iyhi - iylo + 1;
        int nrow = tw * th;
        XYMapper xyMapper = new XYMapper() {
            public int getGridX( int irow ) {
                return ( irow % tw ) + ixlo;
            }
            public int getGridY( int irow ) {
                return ( irow / tw ) + iylo;
            }
        };

        /* Prepare columns giving the grid cell coordinates and their values.
         * Each exported table row corresponds to one cell of the grid. */
        DataSpec dspec = gplan.dataSpec_;
        int icPos = COORD_GROUP.getPosCoordIndex( 0, gplan.geom_ );
        int icWeight = COORD_GROUP.getExtraCoordIndex( 0, gplan.geom_ );
        ValueInfo[] xInfos = dspec.getUserCoordInfos( icPos + 0 );
        ValueInfo[] yInfos = dspec.getUserCoordInfos( icPos + 1 );
        String xlabel = xInfos.length == 1 ? xInfos[ 0 ].getName() : "X";
        String ylabel = yInfos.length == 1 ? yInfos[ 0 ].getName() : "Y";
        BinList.Result binResult = gplan.result_;
        ValueInfo dataInfo =
            getCombinedInfo( dspec, icWeight, gplan.combiner_ );
        Class<?> dataClazz = dataInfo.getContentClass();
        final DoubleFunction<Number> dataFunc;
        if ( Integer.class.equals( dataClazz ) ) {
            dataFunc = d -> Integer.valueOf( (int) d );
        }
        else if ( Short.class.equals( dataClazz ) ) {
            dataFunc = d -> Short.valueOf( (short) d );
        }
        else if ( Long.class.equals( dataClazz ) ) {
            dataFunc = d -> Long.valueOf( (long) d );
        }
        else {
            assert Double.class.equals( dataClazz );
            dataFunc = d -> Double.valueOf( d );
        }
        ColumnData[] cdatas = new ColumnData[] {
            createCoordColumn( xyMapper, pixer, false, 0.5,
                               xlabel, "X bin central value" ),
            createCoordColumn( xyMapper, pixer, true, 0.5,
                               ylabel, "Y bin central value" ),
            new ColumnData( dataInfo ) {
                public Number readValue( long lrow ) {
                    int irow = (int) lrow;
                    int ibin = pixer.getBinIndex( xyMapper.getGridX( irow ),
                                                  xyMapper.getGridY( irow ) );
                    double val = ibin >= 0 ? binResult.getBinValue( ibin )
                                           : Double.NaN;
                    return Double.isNaN( val ) ? null : dataFunc.apply( val );
                }
            },
            createCoordColumn( xyMapper, pixer, false, 0.0,
                               "LO_" + xlabel, "X bin lower bound" ),
            createCoordColumn( xyMapper, pixer, false, 1.0,
                               "HI_" + xlabel, "X bin upper bound" ),
            createCoordColumn( xyMapper, pixer, true, 0.0,
                               "LO_" + ylabel, "Y bin lower bound" ),
            createCoordColumn( xyMapper, pixer, true, 1.0,
                               "HI_" + ylabel, "Y bin upper bound" ),
        };

        /* Turn the columns into a table and return it. */
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( nrow );
        for ( ColumnData cdata : cdatas ) {
            table.addColumn( cdata );
        }
        return table;
    }

    /**
     * Returns a column for an exported table giving an X or Y value in the
     * exported grid.
     *
     * @param  xyMapper  maps exported table row index to bin grid coords
     * @param  pixer     defines the geometry of the bin grid
     * @param  isY       true for Y coord, false for X coord
     * @param  frac      relative position in cell (0..1)
     * @param  name      column name
     * @param  descrip   column description
     * @return  column in exported grid table
     */
    private static ColumnData
            createCoordColumn( XYMapper xyMapper, GridPixer pixer,
                               boolean isY, double frac,
                               String name, String descrip ) {
        final GridSpec tgrid = isY ? pixer.ygrid_ : pixer.xgrid_;
        return new ColumnData( new ColumnInfo( name, Double.class, descrip ) ) {
            public Double readValue( long lrow ) {
                int irow = (int) lrow;
                double[] limits =
                    tgrid.mapper_
                         .getBinLimits( isY ? xyMapper.getGridY( irow )
                                            : xyMapper.getGridX( irow ) );
                double dval = PlotUtil.scaleValue( limits[ 0 ], limits[ 1 ],
                                                   frac, tgrid.scale_ );
                return Double.valueOf( dval );
            }
        };
    }

    /**
     * Returns the metadata for the combined values.
     *
     * @param  dataSpec  data specification
     * @param  icWeight  coord index in DataSpec for weight coordinate
     * @param  combiner  combination mode
     * @return   metadata for gridded cells
     */
    private static ValueInfo getCombinedInfo( DataSpec dataSpec, int icWeight,
                                              Combiner combiner ) {
        final ValueInfo weightInfo;
        if ( icWeight < 0 || dataSpec.isCoordBlank( icWeight ) ) {
            weightInfo = new DefaultValueInfo( "1", Double.class,
                                               "Weight unspecified"
                                               + ", taken as unity" );
        }
        else {
            ValueInfo[] winfos = dataSpec.getUserCoordInfos( icWeight );
            weightInfo = winfos != null && winfos.length == 1
                       ? winfos[ 0 ]
                       : new DefaultValueInfo( "Weight", Double.class );
        }
        Unit unit = new Unit( "unit", "unit area", "area", 1,
                              "X axis unit * Y axis unit" );
        return combiner.createCombinedInfo( weightInfo, unit );
    }

    /**
     * Style for configuring the grid plot.
     */
    public static class GridStyle implements Style {

        private final BinSizer xSizer_;
        private final BinSizer ySizer_;
        private final double xPhase_;
        private final double yPhase_;
        private final Scaling scaling_;
        private final Subrange dataclip_;
        private final Shader shader_;
        private final Combiner combiner_;

        /**
         * Constructor.
         *
         * @param  xSizer  determines X bin extent
         * @param  ySizer  determines Y bin extent
         * @param  xPhase  X axis bin reference point, 0..1
         * @param  yPhase  Y axis bin reference point, 0..1
         * @param  scaling   scaling function for mapping densities to
         *                   colour map entries
         * @param  dataclip  scaling input adjustment subrange
         * @param  shader   colour map
         * @param  combiner  value combination mode for bin calculation
         */
        public GridStyle( BinSizer xSizer, BinSizer ySizer,
                          double xPhase, double yPhase, Scaling scaling,
                          Subrange dataclip, Shader shader,
                          Combiner combiner ) {
            xSizer_ = xSizer;
            ySizer_ = ySizer;
            xPhase_ = xPhase;
            yPhase_ = yPhase;
            scaling_ = scaling;
            dataclip_ = dataclip;
            shader_ = shader;
            combiner_ = combiner;
        }

        public Icon getLegendIcon() {
            return Shaders.createShaderIcon( shader_, null, true, 16, 8, 2, 2 );
        }

        @Override
        public int hashCode() {
            int code = 27441;
            code = 23 * code + xSizer_.hashCode();
            code = 23 * code + ySizer_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) xPhase_ );
            code = 23 * code + Float.floatToIntBits( (float) yPhase_ );
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + combiner_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof GridStyle ) {
                GridStyle other = (GridStyle) o;
                return this.xSizer_.equals( other.xSizer_ )
                    && this.ySizer_.equals( other.ySizer_ )
                    && this.xPhase_ == other.xPhase_
                    && this.yPhase_ == other.yPhase_
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && this.shader_.equals( other.shader_ )
                    && this.combiner_.equals( other.combiner_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for grid plotter.
     */
    private static class GridLayer extends AbstractPlotLayer {

        private final GridStyle gstyle_;
        private final int icPos_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  plotter  plotter
         * @param  geom  geom
         * @param  dataSpec   data specification
         * @param  style   layer style
         */
        GridLayer( GridPlotter plotter, DataGeom geom, DataSpec dataSpec,
                   GridStyle style ) {
            super( plotter, geom, dataSpec, style, LayerOpt.NO_SPECIAL );
            gstyle_ = style;
            icPos_ = COORD_GROUP.getPosCoordIndex( 0, geom );
            icWeight_ = COORD_GROUP.getExtraCoordIndex( 0, geom ); 
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Span> auxSpans,
                                      PaperType ptype ) {
            return new GridDrawing( (PlanarSurface) surface,
                                    auxSpans.get( SCALE ), ptype );
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icWeight_;
                }
                public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                    return getCombinedInfo( dataSpec, icWeight_,
                                            gstyle_.combiner_ );
                }
                public Scaling getScaling() {
                    return gstyle_.scaling_;
                }
                public void adjustAuxRange( Surface surface, DataSpec dataSpec,
                                            DataStore dataStore, Object[] plans,
                                            Ranger ranger ) {

                    /* Work out the grid we need to sample over. */
                    PlanarSurface psurf = (PlanarSurface) surface;
                    GridPixer pixer0 = createGridPixer( psurf, 0 );

                    /* If there is a plan covering the current data use it,
                     * otherwise we need to re-grid. */
                    GridPlan gridPlan = getGridPlan( plans, pixer0 );
                    final GridPixer pixer;
                    final BinList.Result binResult;
                    if ( gridPlan != null ) {
                        binResult = gridPlan.result_;
                        pixer = gridPlan.pixer_;
                    }
                    else {
                        binResult = readBins( pixer0, dataSpec, dataStore )
                                   .getResult();
                        pixer = pixer0;
                    }

                    /* Use the grid pixer and binResult got by either means
                     * to adjust the aux range. */
                    extendRange( ranger, psurf, pixer, binResult );
                }
            } );
            return map;
        }

        /**
         * Returns a GridPixer for this layer suitable for gridding data
         * on a given plot surface.  A fractional padding parameter
         * indicates how far outside the visible plot bounds the grid
         * data should be gathered.  The point of this is that if a
         * large padding is chosen, then the data gathered can be reused
         * for a surface somewhat outside the bounds of the original one.
         * If you're not going to reuse the data on a different surface,
         * set <code>fpad=0</code>.
         *
         * @param   surface  plot surface
         * @param   fpad     fractional padding (0 is no padding)
         * @return   new grid pixer
         */
        GridPixer createGridPixer( PlanarSurface surface, double fpad ) {
            Subrange padder = new Subrange( 0.0 - fpad, 1.0 + fpad );
            GridSpec[] grids = new GridSpec[ 2 ];
            BinSizer[] sizers = { gstyle_.xSizer_, gstyle_.ySizer_ };
            double[] phases = { gstyle_.xPhase_, gstyle_.yPhase_ };
            Axis[] axes = surface.getAxes();
            double[][] dataLimits = surface.getDataLimits();
            for ( int i = 0; i < 2; i++ ) {
                Axis axis = axes[ i ];
                Scale scale = axis.getScale();
                double dlo = dataLimits[ i ][ 0 ];
                double dhi = dataLimits[ i ][ 1 ];
                double[] drange =
                    PlotUtil.scaleRange( dlo, dhi, padder, scale );
                double reqWidth =
                    sizers[ i ].getScaleWidth( scale, dlo, dhi, true );

                /* Avoid sub-pixel grids since it would both be expensive
                 * on memory and produce visually worse results. */
                double binWidth =
                    Math.max( reqWidth, getRoundedPixelScaleWidth( axis ) );
                double phase = phases[ i ];
                grids[ i ] = new GridSpec( scale, binWidth, phase, drange );
            }
            return new GridPixer( grids[ 0 ], grids[ 1 ] );
        }

        /**
         * Returns the calculated histogram for this layer on a given
         * plot surface.
         *
         * @param  pixer      grid definition
         * @param  dataSpec   data specification
         * @param  dataStore  data storage
         * @return   populated bin list
         */
        private BinList readBins( GridPixer pixer, DataSpec dataSpec,
                                  DataStore dataStore ) {
            Combiner combiner = gstyle_.combiner_;
            DataGeom geom = getDataGeom();
            int icw = dataSpec.isCoordBlank( icWeight_ ) ? -1 : icWeight_;
            BinCollector collector =
                new BinCollector( combiner, pixer, geom, icPos_, icw );
            return PlotUtil.tupleCollect( collector, dataSpec, dataStore );
        }

        /**
         * Identifies and returns a plan object that can be used for
         * this layer from a list of precalculated plans.
         * If none of the supplied plans is suitable, null is returned.
         *
         * @param  knownPlans  available pre-calculated plan objects
         * @param  pixer   grid pixer suitable for which the plan is required
         * @return  suitable GridPlan from supplied list, or null
         */
        private GridPlan getGridPlan( Object[] knownPlans, GridPixer pixer ) {
            Combiner combiner = gstyle_.combiner_;
            DataSpec dataSpec = getDataSpec();
            DataGeom geom = getDataGeom();
            for ( Object plan : knownPlans ) {
                if ( plan instanceof GridPlan ) {
                    GridPlan gplan = (GridPlan) plan;
                    if ( gplan.matches( pixer, combiner, dataSpec, geom ) ) {
                        return gplan;
                    }
                }
            }
            return null;
        }

        /**
         * Updates a given ranger object to accomodate values in a supplied
         * data grid.
         *
         * @param   ranger  ranger object to update
         * @param   surface   plot surface over which data is to be surveyed
         * @param   pixer    defines grid geometry
         * @param   binResult   accumulated grid data
         */
        private void extendRange( Ranger ranger, PlanarSurface surface,
                                  GridPixer pixer, BinList.Result binResult ) {
            double[][] dlims = surface.getDataLimits();
            double binFactor =
                pixer.getBinFactor( gstyle_.combiner_.getType() );
            int[] ixRange = pixer.xgrid_.getBinRange( dlims[ 0 ] );
            int[] iyRange = pixer.ygrid_.getBinRange( dlims[ 1 ] );
            int ixlo = ixRange[ 0 ];
            int ixhi = ixRange[ 1 ];
            int iylo = iyRange[ 0 ];
            int iyhi = iyRange[ 1 ];
            for ( int iy = iylo; iy <= iyhi; iy++ ) {
                for ( int ix = ixlo; ix <= ixhi; ix++ ) {
                    int ibin = pixer.getBinIndex( ix, iy );
                    assert ibin >= 0;
                    ranger.submitDatum( binFactor
                                      * binResult.getBinValue( ibin ) );
                }
            }
        }

        /**
         * Drawing implementation for the density map.
         */
        private class GridDrawing implements Drawing {

            private final PlanarSurface surface_;
            private final Span auxSpan_;
            private final PaperType ptype_;

            /**
             * Constructor.
             *
             * @param   surface   plotting surface
             * @param   auxSpan  range defining colour scaling
             * @param   paperType  paper type
             */
            GridDrawing( PlanarSurface surface, Span auxSpan,
                         PaperType ptype ) {
                surface_ = surface;
                auxSpan_ = auxSpan;
                ptype_ = ptype;
            }

            public GridPlan calculatePlan( Object[] knownPlans,
                                           DataStore dataStore ) {
                GridPlan knownPlan =
                    getGridPlan( knownPlans, createGridPixer( surface_, 0 ) );
                if ( knownPlan != null ) {
                    return knownPlan;
                }
                else {
                    DataSpec dataSpec = getDataSpec();
                    GridPixer pixer1 = createGridPixer( surface_, PADDING );
                    BinList.Result binResult =
                        readBins( pixer1, dataSpec, dataStore )
                       .getResult();
                    return new GridPlan( pixer1, gstyle_.combiner_, dataSpec,
                                         getDataGeom(), binResult );
                }
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                final GridPlan gplan = (GridPlan) plan;
                final Combiner.Type ctype = gstyle_.combiner_.getType();
                ptype_.placeDecal( paper, new Decal() {
                    public void paintDecal( Graphics g ) {
                        Scaler scaler =
                            auxSpan_.createScaler( gstyle_.scaling_,
                                                   gstyle_.dataclip_ );
                        IndexColorModel colorModel =
                            PixelImage.createColorModel( gstyle_.shader_,
                                                         true );
                        paintBins( g, gplan.pixer_, gplan.result_, ctype,
                                   scaler, colorModel, surface_ );
                    }
                    public boolean isOpaque() {
                        return false;
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                ReportMap report = new ReportMap();
                GridPixer pixer = createGridPixer( surface_, 0.0 );
                report.put( XBINWIDTH_KEY,
                            Double.valueOf( pixer.xgrid_.binWidth_ ) );
                report.put( YBINWIDTH_KEY,
                            Double.valueOf( pixer.ygrid_.binWidth_ ) );
                if ( plan instanceof GridPlan ) {
                    report.put( GRIDTABLE_KEY,
                                createExportTable( surface_,
                                                   (GridPlan) plan ) );
                }
                return report;
            }
        }
    }

    /**
     * Collector implementation for accumulating bins.
     */
    private static class BinCollector
            implements SplitCollector<TupleSequence,ArrayBinList> {

        private final Combiner combiner_;
        private final GridPixer pixer_;
        private final DataGeom geom_;
        private final int icPos_;
        private final int icWeight_;

       /**
         * Constructor.
         *
         * @param  combiner  combination mode
         * @param  pixer      grid definition
         * @param  geom     datageom
         * @param  icPos    column index for positional coordinate
         * @param  icWeight   column index for weight coordinate,
         *                    or -1 for unweighted
         */
        BinCollector( Combiner combiner, GridPixer pixer, DataGeom geom,
                      int icPos, int icWeight ) {
            combiner_ = combiner;
            pixer_ = pixer;
            geom_ = geom;
            icPos_ = icPos;
            icWeight_ = icWeight;
        }

        public ArrayBinList createAccumulator() {
            return combiner_.createArrayBinList( pixer_.getBinCount() );
        }

        public void accumulate( TupleSequence tseq, ArrayBinList binList ) {
            double[] dpos = new double[ geom_.getDataDimCount() ];

            /* Unweighted. */
            if ( icWeight_ < 0 ) {
                while ( tseq.next() ) {
                    if ( geom_.readDataPos( tseq, icPos_, dpos ) ) {
                        int ibin = pixer_.getBinIndex( dpos );
                        if ( ibin >= 0 ) {
                            binList.submitToBin( ibin, 1 );
                        }
                    }
                }
            }

            /* Weighted. */
            else {
                while ( tseq.next() ) {
                    if ( geom_.readDataPos( tseq, icPos_, dpos ) ) {
                        int ibin = pixer_.getBinIndex( dpos );
                        if ( ibin >= 0 ) {
                            double w = WEIGHT_COORD
                                      .readDoubleCoord( tseq, icWeight_ );
                            if ( ! Double.isNaN( w ) ) {
                                binList.submitToBin( ibin, w );
                            }
                        }
                    }
                }
            }
        }

        public ArrayBinList combine( ArrayBinList binList1,
                                     ArrayBinList binList2 ) {
            // This seems to be a pretty fast operation,
            // no requirement to pool bins.
            binList1.addBins( binList2 );
            return binList1;
        }
    }

    /**
     * Aggregates a BinList.Result with information that charaterises its
     * scope of applicability.
     */
    private static class GridPlan {
        final GridPixer pixer_;
        final Combiner combiner_;
        final DataSpec dataSpec_;
        final DataGeom geom_;
        final BinList.Result result_;

        /**
         * Constructor.
         *
         * @param  pixer   grid mapping object
         * @param  combiner  combination method for values
         * @param  dataSpec   data specification
         * @param  geom     geom
         * @param  result  contains accumulated weight data
         */
        GridPlan( GridPixer pixer, Combiner combiner, DataSpec dataSpec,
                  DataGeom geom, BinList.Result result ) {
            pixer_ = pixer;
            combiner_ = combiner;
            dataSpec_ = dataSpec;
            geom_ = geom;
            result_ = result;
        }

        /**
         * Indicates whether this plan can be used for a given set
         * of drawing requirements.
         *
         * @param  pixer   grid mapping object
         * @param  combiner  combination method for values
         * @param  dataSpec   data specification
         * @param  geom     geom
         */
        public boolean matches( GridPixer pixer,  Combiner combiner,
                                DataSpec dataSpec, DataGeom geom ) {
            return pixer_.contains( pixer )
                && combiner_.equals( combiner )
                && dataSpec_.equals( dataSpec )
                && geom_.equals( geom );
        }
    }

    /**
     * Defines the mapping of graphics coordinates to bin index.
     */
    private static class GridPixer {

        private final GridSpec xgrid_;
        private final GridSpec ygrid_;
        private final Gridder gridder_;

        /**
         * Constructor.
         *
         * @param  xgrid  1-d grid specification in X direction
         * @parma  ygrid  1-d grid specification in Y direction
         */
        GridPixer( GridSpec xgrid, GridSpec ygrid ) {
            xgrid_ = xgrid;
            ygrid_ = ygrid;
            gridder_ = new Gridder( xgrid.ihi_ - xgrid.ilo_ + 1,
                                    ygrid.ihi_ - ygrid.ilo_ + 1 );
        }

        /**
         * Returns the number of bins covered by this grid.
         *
         * @return   bin count
         */
        public int getBinCount() {
            return gridder_.getLength();
        }

        /**
         * Calculates the grid index for a given graphics position.
         *
         * @param   dpos  position in data coordinates
         * @return   grid index, or negative number if off-grid
         */
        public int getBinIndex( double[] dpos ) {
            double dx = dpos[ 0 ];
            double dy = dpos[ 1 ];

            /* Test against the data bounds we know apply for this GridPixer.
             * It might seem more straightforward to use the bin bounds here,
             * but this avoids having to calculate bin index for values
             * out of range, and and it also avoids some nasty problems
             * with bad data values (e.g. negative values for a
             * logarithmic mapper). */
            if ( xgrid_.containsDataPoint( dx ) &&
                 ygrid_.containsDataPoint( dy ) ) {
                int ix = xgrid_.mapper_.getBinIndex( dx );
                int iy = ygrid_.mapper_.getBinIndex( dy );

                /* Unfortunately, small numerical errors in the mapper
                 * conversions can lead to values falling just outside the
                 * bins in any case.  So it's necessary that the implementation
                 * of getBinIndex checks against bin indices as well. */
                assert xgrid_.nearlyContainsBin( ix )
                    && ygrid_.nearlyContainsBin( iy );
                return getBinIndex( ix, iy );
            }
            return -1;
        }

        /**
         * Returns the grid index given the X and Y grid indices.
         *
         * @param   ix  X gridder bin index
         * @param   iy  Y gridder bin index
         * @return   grid index for bin list, or negative number if off-grid
         */
        int getBinIndex( int ix, int iy ) {
            return xgrid_.containsBin( ix ) && ygrid_.containsBin( iy )
                 ? gridder_.getIndex( xgrid_.getBinOffset( ix ),
                                      ygrid_.getBinOffset( iy ) )
                 : -1;
        }

        /**
         * Returns the area of a bin in data units.
         * If either axis is non-linear, this will be a strange quantity.
         *
         * @param  ctype  combination type
         * @return  multiplication for bin values
         */
        double getBinFactor( Combiner.Type ctype ) {
            return ctype.getBinFactor( xgrid_.binWidth_ * ygrid_.binWidth_ );
        }

        /**
         * Indicates whether this pixer is a superset of the given pixer.
         *
         * @param  other  comparison object
         * @return   true iff this object contains all the information
         *           contained by <code>other</code>
         */
        boolean contains( GridPixer other ) {
            return xgrid_.contains( other.xgrid_ )
                && ygrid_.contains( other.ygrid_ );
        }
    }

    /**
     * Specifies bin geometry for a 1-dimensional grid.
     */
    private static class GridSpec {
        final Scale scale_;
        final double binWidth_;
        final double phase_;
        final double dlo_;
        final double dhi_;

        final BinMapper mapper_;
        final int ilo_;
        final int ihi_;

        /**
         * Constructor.
         *
         * @param  scale    axis scale
         * @param  binWidth  bin width in scale units
         * @param  phase   scaled phase (non-degenerate range is 0..1)
         * @param  drange  2-element [lo,hi] array giving required
         *                 minimum extent in data coordinates
         */
        GridSpec( Scale scale, double binWidth, double phase,
                  double[] drange ) {
            scale_ = scale;
            binWidth_ = binWidth;
            phase_ = phase;
            mapper_ = new BinMapper( scale, binWidth, phase, drange[ 0 ] );
            int i0 = mapper_.getBinIndex( drange[ 0 ] );
            int i1 = mapper_.getBinIndex( drange[ 1 ] );
            double[] dlimits0 = mapper_.getBinLimits( i0 );
            double[] dlimits1 = mapper_.getBinLimits( i1 );
            ilo_ = Math.min( i0, i1 );
            ihi_ = Math.max( i0, i1 );
            dlo_ = Math.min( dlimits0[ 0 ], dlimits1[ 0 ] );
            dhi_ = Math.max( dlimits0[ 1 ], dlimits1[ 1 ] );
        }

        /**
         * Indicates whether this specification is a superset of the given one.
         *
         * @param  other  comparison object
         * @return   true iff this object contains all the information
         *           contained by <code>other</code>
         */
        boolean contains( GridSpec other ) {
            return this.binWidth_ == other.binWidth_
                && this.phase_ == other.phase_
                && this.dlo_ <= other.dlo_
                && this.dhi_ >= other.dhi_;
        }

        /**
         * Tests whether a given value in data coordinates falls within this
         * grid's bounds.
         *
         * @param   d  test data point
         * @return   true iff d falls within the grid bounds of this object
         */
        boolean containsDataPoint( double d ) {
            return d >= dlo_ && d < dhi_;
        }

        /**
         * Tests whether a given bin index falls within this grid's bounds.
         *
         * @param   ibin   test bin index
         * @return  true iff ibin identifies one of this grid's bins
         */
        boolean containsBin( int ibin ) {
            return ibin >= ilo_ && ibin <= ihi_;
        }

        /**
         * Tests whether a given bin index either
         * falls within this grid's bounds or is just outside them.
         * 
         * @param  ibin  bin index
         * @return  true iff ibin is no more than one away from this grid
         */
        boolean nearlyContainsBin( int ibin ) {
            return ibin >= ilo_ - 1 && ibin <= ihi_ + 1;
        }

        /**
         * Returns the offset index for a given bin.
         *
         * @param  ibin  bin index as returned by the mapper
         * @return  offset bin index in the range 0..(ihi_-ilo_)
         */
        int getBinOffset( int ibin ) {
            return ibin - ilo_;
        }

        /**
         * Returns the range of bin indices corresponding to a range in
         * data coordinates for this grid.
         *
         * @param   dataRange  2-element array (lo,hi) of data values
         * @return  2-element array (lo,hi) of bin indices
         */
        int[] getBinRange( double[] dataRange ) {
            int i0 = mapper_.getBinIndex( dataRange[ 0 ] );
            int i1 = mapper_.getBinIndex( dataRange[ 1 ] );
            return new int[] { Math.min( i0, i1 ), Math.max( i0, i1 ) };
        }
    }

    /**
     * Maps a row index in an exported table
     * to an X, Y position in the bin grid.
     */
    private interface XYMapper {

        /**
         * Gets X grid coordinate for table row.
         *
         * @param  irow  exported table row index
         * @return   X coordinate of bin grid
         */
        int getGridX( int irow );

        /**
         * Gets Y grid coordinate for table row.
         *
         * @param  irow  exported table row index
         * @return   Y coordinate of bin grid
         */
        int getGridY( int irow );
    }
}
