package uk.ac.starlink.ttools.plot2.layer;

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
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
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

    /** ReportKey for actual X bin extent. */
    public static final ReportKey<Double> XBINWIDTH_KEY =
        createBinWidthReportKey( 'x' );

    /** ReportKey for actual Y bin extent. */
    public static final ReportKey<Double> YBINWIDTH_KEY =
        createBinWidthReportKey( 'y' );

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

    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;
    private static final CoordGroup COORD_GROUP =
        CoordGroup
       .createCoordGroup( 1, new Coord[] { FloatingCoord.WEIGHT_COORD } );

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
        return false;
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

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.add( XBINSIZER_KEY );
        keyList.add( YBINSIZER_KEY );
        keyList.add( StyleKeys.COMBINER );
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        keyList.add( XPHASE_KEY );
        keyList.add( YPHASE_KEY );
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    public GridStyle createStyle( ConfigMap config ) {
        BinSizer xSizer = config.get( XBINSIZER_KEY );
        BinSizer ySizer = config.get( YBINSIZER_KEY );
        double xPhase = config.get( XPHASE_KEY ).doubleValue();
        double yPhase = config.get( YPHASE_KEY ).doubleValue();
        Combiner combiner = config.get( StyleKeys.COMBINER );
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        Scaling scaling = ramp.getScaling();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new GridStyle( xSizer, ySizer, xPhase, yPhase,
                              scaling, shader, combiner );
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
        boolean rounding = true;
        boolean allowZero = false;
        return BinSizer.createSizerConfigKey( meta, widthRepKey, 30,
                                              rounding, allowZero );
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
        return DoubleConfigKey.createSliderKey( meta, 0, 0, 1, false );
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
     * Style for configuring the grid plot.
     */
    public static class GridStyle implements Style {

        private final BinSizer xSizer_;
        private final BinSizer ySizer_;
        private final double xPhase_;
        private final double yPhase_;
        private final Scaling scaling_;
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
         * @param  shader   colour map
         * @param  combiner  value combination mode for bin calculation
         */
        public GridStyle( BinSizer xSizer, BinSizer ySizer,
                          double xPhase, double yPhase,
                          Scaling scaling, Shader shader, Combiner combiner ) {
            xSizer_ = xSizer;
            ySizer_ = ySizer;
            xPhase_ = xPhase;
            yPhase_ = yPhase;
            scaling_ = scaling;
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
                                      Map<AuxScale,Range> auxRanges,
                                      PaperType ptype ) {
            return new GridDrawing( (PlanarSurface) surface,
                                    auxRanges.get( SCALE ), ptype );
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icWeight_;
                }
                public void adjustAuxRange( Surface surface, DataSpec dataSpec,
                                            DataStore dataStore, Object[] plans,
                                            Range range ) {
                    PlanarSurface psurf = (PlanarSurface) surface;
                    GridPlan gridPlan =
                        getGridPlan( plans, createGridPixer( psurf ) );
                    BinList.Result binResult =
                          gridPlan == null
                        ? readBins( psurf, dataSpec, dataStore ).getResult()
                        : gridPlan.result_;
                    PlotUtil.extendRange( range, binResult );
                }
            } );
            return map;
        }

        /**
         * Returns a GridPixer for a given surface suitable for this layer.
         *
         * @param  surface  plot surface
         * @return   pixer
         */
        GridPixer createGridPixer( PlanarSurface surface ) {
            BinMapper[] binMappers = new BinMapper[ 2 ];
            int[] ilobins = new int[ 2 ];
            int[] ihibins = new int[ 2 ];
            double[] binWidths = new double[ 2 ];
            BinSizer[] sizers = { gstyle_.xSizer_, gstyle_.ySizer_ };
            double[] phases = { gstyle_.xPhase_, gstyle_.yPhase_ };
            Axis[] axes = surface.getAxes();
            double[][] dataLimits = surface.getDataLimits();
            boolean[] logFlags = surface.getLogFlags();
            for ( int i = 0; i < 2; i++ ) {
                boolean isLog = logFlags[ i ];
                double dlo = dataLimits[ i ][ 0 ];
                double dhi = dataLimits[ i ][ 1 ];
                double binWidth = sizers[ i ].getWidth( isLog, dlo, dhi );
                binWidths[ i ] = binWidth;
                double phase = phases[ i ];
                BinMapper mapper =
                    BinMapper.createMapper( isLog, binWidth, phase, dlo );
                binMappers[ i ] = mapper;
                int ibin0 = mapper.getBinIndex( dlo );
                int ibin1 = mapper.getBinIndex( dhi );
                ilobins[ i ] = Math.min( ibin0, ibin1 );
                ihibins[ i ] = Math.max( ibin0, ibin1 );
            }
            return new GridPixer( binMappers[ 0 ], ilobins[ 0 ], ihibins[ 0 ],
                                  binWidths[ 0 ],
                                  binMappers[ 1 ], ilobins[ 1 ], ihibins[ 1 ],
                                  binWidths[ 1 ] );
        }

        /**
         * Returns the calculated histogram for this layer on a given
         * plot surface.
         *
         * @param  surface   plot surface
         * @param  dataSpec   data specification
         * @param  dataStore  data storage
         * @return   populated bin list
         */
        private BinList readBins( PlanarSurface surface, DataSpec dataSpec,
                                  DataStore dataStore ) {
            GridPixer pixer = createGridPixer( surface );
            int nbin = pixer.getBinCount();
            Combiner combiner = gstyle_.combiner_;
            BinList binList = combiner.createArrayBinList( nbin );
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
            Point2D.Double gp = new Point2D.Double();
            DataGeom geom = getDataGeom();
            double[] dpos = new double[ geom.getDataDimCount() ];

            /* Unweighted. */
            if ( dataSpec.isCoordBlank( icWeight_ ) ) {
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos_, dpos ) ) {
                        int ibin = pixer.getBinIndex( dpos );
                        if ( ibin >= 0 ) {
                            binList.submitToBin( ibin, 1 );
                        }
                    }
                }
            }

            /* Weighted. */
            else {
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos_, dpos ) ) {
                        int ibin = pixer.getBinIndex( dpos );
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
            return binList;
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
         * Drawing implementation for the density map.
         */
        private class GridDrawing implements Drawing {

            private final PlanarSurface surface_;
            private final Range auxRange_;
            private final PaperType ptype_;
            private final GridPixer gridPixer_;

            /**
             * Constructor.
             *
             * @param   surface   plotting surface
             * @param   auxRange  range defining colour scaling
             * @param   paperType  paper type
             */
            GridDrawing( PlanarSurface surface, Range auxRange,
                         PaperType ptype ) {
                surface_ = surface;
                auxRange_ = auxRange;
                ptype_ = ptype;
                gridPixer_ = createGridPixer( surface );
            }

            public GridPlan calculatePlan( Object[] knownPlans,
                                           DataStore dataStore ) {
                GridPlan knownPlan = getGridPlan( knownPlans, gridPixer_ );
                if ( knownPlan != null ) {
                    return knownPlan;
                }
                else {
                    DataSpec dataSpec = getDataSpec();
                    BinList.Result binResult =
                        readBins( surface_, dataSpec, dataStore ).getResult();
                    return new GridPlan( gridPixer_, gstyle_.combiner_,
                                         dataSpec, getDataGeom(), binResult );
                }
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                final GridPlan gplan = (GridPlan) plan;
                ptype_.placeDecal( paper, new Decal() {
                    public void paintDecal( Graphics g ) {
                        paintBins( g, gplan.result_ );
                    }
                    public boolean isOpaque() {
                        return false;
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                ReportMap report = new ReportMap();
                report.put( XBINWIDTH_KEY,
                            new Double( gridPixer_.xBinWidth_ ) );
                report.put( YBINWIDTH_KEY,
                            new Double( gridPixer_.yBinWidth_ ) );
                return report;
            }

            private void paintBins( Graphics g, BinList.Result binResult ) {

                /* Work out how to scale binlist values to turn into
                 * entries in a colour map.  The first entry in the colour map
                 * (index zero) corresponds to transparency. */
                Scaler scaler =
                    Scaling.createRangeScaler( gstyle_.scaling_, auxRange_ );
                IndexColorModel colorModel =
                    PixelImage.createColorModel( gstyle_.shader_, true );
                int ncolor = colorModel.getMapSize() - 1;

                /* Sample bin grid onto output pixel grid.
                 * There is a more efficient way to do this for the (common?)
                 * case in which bins are all the same shape and are
                 * larger than a screen pixel: draw onto a 1-pixel-per-bin
                 * image and paint it scaled to the graphics context
                 * (PixelImage.paintScaledPixels).  But that would be more
                 * vulnerable to pathological conditions (tiny bins). */
                Rectangle bounds = surface_.getPlotBounds();
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
                    double[] dpos = surface_.graphicsToData( gp, null );
                    int ib = gridPixer_.getBinIndex( dpos );
                    if ( ib >= 0 ) {
                        if ( ib != ib0 ) {
                            ib0 = ib;
                            double dval = binResult.getBinValue( ib );
                            sval = Double.isNaN( dval )
                                 ? 0
                                 : Math.min( 1 +
                                             (int) ( scaler .scaleValue( dval )
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
            return pixer_.equals( pixer )
                && combiner_.equals( combiner )
                && dataSpec_.equals( dataSpec )
                && geom_.equals( geom );
        }
    }

    /**
     * Defines the mapping of graphics coordinates to bin index.
     */
    @Equality
    private static class GridPixer {

        private final BinMapper xMapper_;
        private final BinMapper yMapper_;
        private final int ixlo_;
        private final int ixhi_;
        private final int iylo_;
        private final int iyhi_;
        private final Gridder gridder_;
        private final double dxlo_;
        private final double dxhi_;
        private final double dylo_;
        private final double dyhi_;
        final double xBinWidth_;
        final double yBinWidth_;

        /**
         * Constructor.
         *
         * @param  xMapper  mapper for X axis
         * @param  ixlo     lowest bin index for X axis
         * @param  ixhi     highest bin index for X axis
         * @param  xBinWidth   additive/multiplicative bin width for X axis
         * @param  yMapper  mapper for Y axis
         * @param  iylo     lowest bin index for Y axis
         * @param  iyhi     highest bin index for Y axis
         * @param  yBinWidth   additive/multiplicative bin width for Y axis
         */
        GridPixer( BinMapper xMapper, int ixlo, int ixhi, double xBinWidth,
                   BinMapper yMapper, int iylo, int iyhi, double yBinWidth ) {
            xMapper_ = xMapper;
            ixlo_ = ixlo;
            ixhi_ = ixhi;
            yMapper_ = yMapper;
            iylo_ = iylo;
            iyhi_ = iyhi;
            xBinWidth_ = xBinWidth;
            yBinWidth_ = yBinWidth;
            gridder_ = new Gridder( ixhi_ - ixlo_ + 1, iyhi_ - iylo_ + 1 );
            double[] xloLimits = xMapper_.getBinLimits( ixlo_ );
            double[] xhiLimits = xMapper_.getBinLimits( ixhi_ );
            double[] yloLimits = yMapper_.getBinLimits( iylo_ );
            double[] yhiLimits = yMapper_.getBinLimits( iyhi_ );
            dxlo_ = Math.min( xloLimits[ 0 ], xhiLimits[ 0 ] );
            dxhi_ = Math.max( xloLimits[ 1 ], xhiLimits[ 1 ] );
            dylo_ = Math.min( yloLimits[ 0 ], yhiLimits[ 0 ] );
            dyhi_ = Math.max( yloLimits[ 1 ], yhiLimits[ 1 ] );
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
            if ( dx >= dxlo_ && dx < dxhi_ && dy >= dylo_ && dy < dyhi_ ) {
                int ix = xMapper_.getBinIndex( dx );
                int iy = yMapper_.getBinIndex( dy );

                /* Unfortunately, small numerical errors in the mapper
                 * conversions can lead to values falling just outside the
                 * bins in any case.  So we have to check against the
                 * bin indices as well. */
                if ( ix >= ixlo_ && ix <= ixhi_ &&
                     iy >= iylo_ && iy <= iyhi_ ) {
                    return gridder_.getIndex( ix - ixlo_, iy - iylo_ );
                }
                else {
                    assert ix >= ixlo_ - 1 && ix <= ixhi_ + 1
                        && iy >= iylo_ - 1 && iy <= iyhi_ + 1;
                }
            }
            return -1;
        }

        @Override
        public int hashCode() {
            int code = 88721114;
            code = 23 * code + xMapper_.hashCode();
            code = 23 * code + yMapper_.hashCode();
            code = 23 * code + ixlo_;
            code = 23 * code + ixhi_;
            code = 23 * code + iylo_;
            code = 23 * code + iyhi_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof GridPixer ) {
                GridPixer other = (GridPixer) o;
                return this.xMapper_.equals( other.xMapper_ )
                    && this.yMapper_.equals( other.yMapper_ )
                    && this.ixlo_ == other.ixlo_
                    && this.ixhi_ == other.ixhi_
                    && this.iylo_ == other.iylo_
                    && this.iyhi_ == other.iyhi_;
            }
            else {
                return false;
            }
        }
    }
}
