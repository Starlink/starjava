package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
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
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plotter that shows a grid of vectors based on averaged
 * 2-d vector values on a plane.
 * It paints a single Decal, no Glyphs.
 *
 * @author   Mark Taylor
 * @since    24 Apr 2026
 */
public class VectorFieldPlotter
        implements Plotter<VectorFieldPlotter.VectorFieldStyle> {

    private final boolean transparent_;
    private final boolean hasWeight_;
    private final CoordGroup coordGrp_;

    /** ReportKey for actual X bin extent. */
    public static final ReportKey<Double> XBINWIDTH_REPKEY =
        GridPlotter.createBinWidthReportKey( 'x' );

    /** ReportKey for actual Y bin extent. */
    public static final ReportKey<Double> YBINWIDTH_REPKEY =
        GridPlotter.createBinWidthReportKey( 'y' );

    /** ReportKey for X relative offset value (when isRelative is true). */
    public static final ReportKey<Double> XOFFSET_REPKEY =
        ReportKey.createDoubleKey( new ReportMeta( "xoff", "X Offset" ), true );

    /** ReportKey for Y relative offset value (when isRelative is true). */
    public static final ReportKey<Double> YOFFSET_REPKEY =
        ReportKey.createDoubleKey( new ReportMeta( "yoff", "Y Offset" ), true );

    /** ReportKey for exported vector field table. */
    public static final ReportKey<StarTable> FIELDTABLE_REPKEY =
        ReportKey.createTableKey( new ReportMeta( "vector_map", "Vector Map" ),
                                  true );

    public static final ConfigKey<Color> COLOR_KEY = StyleKeys.COLOR;
    public static final ConfigKey<MultiPointShape> SHAPE_KEY =
        StyleKeys.VECTOR_SHAPE;
    public static final ConfigKey<Integer> THICK_KEY =
        createThicknessKey( SHAPE_KEY );
    public static final ConfigKey<Double> SCALE_KEY = createScaleKey();
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;
    public static final ConfigKey<Combiner> COMBINER_KEY = createCombinerKey();
    public static final ConfigKey<Boolean> RELATIVE_KEY = createRelativeKey();
    public static final ConfigKey<BinSizer> XBINSIZER_KEY =
        BinSizer.createSizerConfigKey( 'x', "vector", XBINWIDTH_REPKEY, 16 );
    public static final ConfigKey<BinSizer> YBINSIZER_KEY =
        BinSizer.createSizerConfigKey( 'y', "vector", YBINWIDTH_REPKEY, 16 );
    public static final ConfigKey<Double> XPHASE_KEY = GridPlotter.XPHASE_KEY;
    public static final ConfigKey<Double> YPHASE_KEY = GridPlotter.YPHASE_KEY;

    private static final FloatingCoord VX_COORD = createComponentCoord( false );
    private static final FloatingCoord VY_COORD = createComponentCoord( true );
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;

    /** Tuning parameter, fractional external border for grid calculations. */
    private static final double PADDING = 0.5;

    /** Quantile used for default scaling of painted arrows. */
    private static final double SCALING_QUANTILE = 0.90;

    /**
     * Constructor.
     */
    public VectorFieldPlotter( boolean hasWeight ) {
        hasWeight_ = hasWeight;
        transparent_ = false;
        List<Coord> extraCoords = new ArrayList<>();
        extraCoords.add( VX_COORD );
        extraCoords.add( VY_COORD );
        if ( hasWeight ) {
            extraCoords.add( WEIGHT_COORD );
        }
        coordGrp_ = CoordGroup.createCoordGroup( 1, extraCoords
                                                   .toArray( new Coord[ 0 ] ) );
    }

    public String getPlotterName() {
        return "VecField";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_VECTORS;
    }

    public CoordGroup getCoordGroup() {
        return coordGrp_;
    }

    public boolean hasReports() {
        return true;
    }

    public String getPlotterDescription() {
        return String.join( "\n",
            "<p>Plots a field of arrows on a regular grid",
            "corresponding to the aggregated (typically averaged)",
            "value of a 2-d vector quantity",
            "for the points in that grid cell.",
            "</p>",
            "<p>Currently, the X and Y components of the vectors are",
            "normalised separately and sized by default so that",
            "the arrows are shown with components comparable to",
            "the dimensions of the grid cells.",
            "This length can be adjusted by use of the",
            "<code>" + SCALE_KEY.getMeta().getShortName() + "</code> option.",
            "</p>",
            "<p>By default, the absolute values of the vectors are plotted.",
            "However, if the",
            "<code>" + RELATIVE_KEY.getMeta().getShortName() + "</code> option",
            "is set, the mean vector value averaged over the visible plot area",
            "will be calculated and subtracted from all the plotted vectors.",
            "The effect of this is to show local variations rather than",
            "absolute values of the vectors over the visible area.",
            "</p>",
            "<p>The X and Y dimensions of the grid cells",
            "(or equivalently histogram bins)",
            "can be configured either in terms of the data coordinates",
            "or relative to the plot dimensions.",
            "</p>"
        );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> keyList = new ArrayList<>();
        keyList.add( COLOR_KEY );
        keyList.add( SHAPE_KEY );
        keyList.add( THICK_KEY );
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        keyList.add( SCALE_KEY );
        keyList.add( RELATIVE_KEY );
        keyList.add( COMBINER_KEY );
        keyList.add( XBINSIZER_KEY );
        keyList.add( YBINSIZER_KEY );
        keyList.add( XPHASE_KEY );
        keyList.add( YPHASE_KEY );
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public VectorFieldStyle createStyle( ConfigMap config ) {
        Color color =
            StyleKeys.getAlphaColor( config, COLOR_KEY, TRANSPARENCY_KEY );
        MultiPointShape shape = config.get( SHAPE_KEY );
        int thick = config.get( THICK_KEY ).intValue();
        double scale = config.get( SCALE_KEY ).doubleValue();
        Combiner combiner = config.get( COMBINER_KEY );
        boolean isRelative = config.get( RELATIVE_KEY ).booleanValue();
        BinSizer xSizer = config.get( XBINSIZER_KEY );
        BinSizer ySizer = config.get( YBINSIZER_KEY );
        double xPhase = config.get( XPHASE_KEY ).doubleValue();
        double yPhase = config.get( YPHASE_KEY ).doubleValue();
        return new VectorFieldStyle( color, shape, thick, scale,
                                     xSizer, ySizer, xPhase, yPhase,
                                     combiner, isRelative );
    }

    public Object getRangeStyleKey( VectorFieldStyle style ) {
        return null;
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  VectorFieldStyle style ) {
        return new VectorFieldLayer( this, geom, dataSpec, style );
    }

    /**
     * Paints a supplied grid of vectors onto a plane surface.
     *
     * @param  g  graphics context
     * @param  surface  plotting surface
     * @param  scribe   arrow shape
     * @param  color   colour, may have a non-unity alpha channel
     * @param  scale  scaling factor for arrow size
     * @param  isRelative  if true, subtract the vector mean averaged over
     *                     the visible surface from the individual vectors
     * @param  pixer  defines the data grid
     * @param  xResult   X components of vectors per grid bin
     * @param  yResult   Y components of vectors per grid bin
     * @param  xoff    X vector component subtractive offset
     * @param  yoff    Y vector component subtractive offset
     * @param  gfactor   graphics scaling factor
     */
    private static void paintVectors( Graphics g, PlanarSurface surface,
                                      MultiPointScribe scribe, Color color,
                                      double scale, boolean isRelative,
                                      GridPixer pixer,
                                      BinList.Result xResult,
                                      BinList.Result yResult,
                                      double xoff, double yoff,
                                      double gfactor ) {
        Axis xaxis = surface.getAxes()[ 0 ];
        Axis yaxis = surface.getAxes()[ 1 ];
        int[] ixRange = pixer.getGridX().getBinRange( xaxis.getDataLimits() );
        int[] iyRange = pixer.getGridY().getBinRange( yaxis.getDataLimits() );
        int ixlo = ixRange[ 0 ];
        int ixhi = ixRange[ 1 ];
        int iylo = iyRange[ 0 ];
        int iyhi = iyRange[ 1 ];
        BinMapper xMapper = pixer.getGridX().getMapper();
        BinMapper yMapper = pixer.getGridY().getMapper();
        double[] dpos = new double[ 2 ];
        Point2D.Double gpos = new Point2D.Double();

        /* Paint vectors. */
        Color color0 = g.getColor();
        g.setColor( color );
        double[] dpos1 = new double[ 2 ];
        Point2D.Double gpos1 = new Point2D.Double();
        int[] xgoffs = new int[ 1 ];
        int[] ygoffs = new int[ 1 ];
        for ( int iy = iylo; iy <= iyhi; iy++ ) {
            for ( int ix = ixlo; ix <= ixhi; ix++ ) {
                int ibin = pixer.getBinIndex( ix, iy );
                assert ibin >= 0;
                double vx = xResult.getBinValue( ibin );
                double vy = yResult.getBinValue( ibin );
                if ( vx * vx + vy * vy > 0 ) {
                    double dx0 = xMapper.getBinMidpoint( ix );
                    double dy0 = yMapper.getBinMidpoint( iy );
                    double dmx2 = 0.5 * ( vx - xoff ) * gfactor;
                    double dmy2 = 0.5 * ( vy - yoff ) * gfactor;
                    dpos[ 0 ] = dx0 - dmx2;
                    dpos[ 1 ] = dy0 - dmy2;
                    if ( surface.dataToGraphics( dpos, false, gpos ) ) {
                        double dx1 = dx0 + dmx2;
                        double dy1 = dy0 + dmy2;
                        dpos1[ 0 ] = dx1;
                        dpos1[ 1 ] = dy1;
                        if ( surface.dataToGraphics( dpos1, false, gpos1 ) ) {
                            int px = (int) Math.floor( gpos.x );
                            int py = (int) Math.floor( gpos.y );
                            xgoffs[ 0 ] = (int) Math.floor( gpos1.x - gpos.x );
                            ygoffs[ 0 ] = (int) Math.floor( gpos1.y - gpos.y );
                            g.translate( px, py );
                            scribe.createGlyph( xgoffs, ygoffs )
                                  .paintGlyph( g );
                            g.translate( -px, -py );
                         }
                    }
                }
            }
        }
        g.setColor( color0 );
    }

    /**
     * Returns a table representing the gridded vector field map
     * as stored in a plan object.
     * Table contents are constructed lazily so this is not an expensive
     * operation.
     *
     * @param  plan   object containing vector field results
     * @param  plotter  plotter that produced the plan
     * @return  tabular representation of the vector field
     */
    private static StarTable createExportTable( VectorFieldPlan plan,
                                                VectorFieldPlotter plotter ) {
        CoordGroup cgrp = plotter.coordGrp_;
        GridPixer pixer = plan.pixer_;
        PlanarSurface surface = plan.surface_;
        DataSpec dataSpec = plan.dataSpec_;
        DataGeom geom = plan.geom_;
        Combiner combiner = plan.combiner_;

        /* Work out the bounds of the grid that the exported table will cover.
         * Note this is derived from the plot surface, it's not necessarily
         * the same as the grid from the GridPlan, which may be oversized. */
        double[][] dataLimits = surface.getDataLimits();
        BinMapper xmapper = pixer.getGridX().getMapper();
        BinMapper ymapper = pixer.getGridY().getMapper();
        int ixlo = xmapper.getBinIndex( dataLimits[ 0 ][ 0 ] );
        int ixhi = xmapper.getBinIndex( dataLimits[ 0 ][ 1 ] );
        int iylo = ymapper.getBinIndex( dataLimits[ 1 ][ 0 ] );
        int iyhi = ymapper.getBinIndex( dataLimits[ 1 ][ 1 ] );

        /* Prepare mappings from the exported table row indices
         * to the binned grid. */
        int tw = ixhi - ixlo + 1;
        int th = iyhi - iylo + 1;
        int nrow = tw * th;
        GridPlotter.XYMapper xyMapper = new GridPlotter.XYMapper() {
            public int getGridX( int irow ) {
                return ( irow % tw ) + ixlo;
            }
            public int getGridY( int irow ) {
                return ( irow / tw ) + iylo;
            }
        };

        /* Prepare columns giving the grid cell coordinates and their values.
         * Each exported table row corresponds to one cell of the grid. */
        int icPos = cgrp.getPosCoordIndex( 0, plan.geom_ );
        int icVx = cgrp.getExtraCoordIndex( 0, geom );
        int icVy = cgrp.getExtraCoordIndex( 1, geom );
        ValueInfo[] pxInfos = dataSpec.getUserCoordInfos( icPos + 0 );
        ValueInfo[] pyInfos = dataSpec.getUserCoordInfos( icPos + 1 );
        ValueInfo[] vxInfos = dataSpec.getUserCoordInfos( icVx );
        ValueInfo[] vyInfos = dataSpec.getUserCoordInfos( icVy );
        String pxlabel = pxInfos.length == 1 ? pxInfos[ 0 ].getName() : "X";
        String pylabel = pyInfos.length == 1 ? pyInfos[ 0 ].getName() : "Y";
        ValueInfo vxInfo = combiner.createCombinedInfo( vxInfos[ 0 ], null );
        ValueInfo vyInfo = combiner.createCombinedInfo( vyInfos[ 0 ], null );
        BinList.Result vxResult = plan.xResult_;
        BinList.Result vyResult = plan.yResult_;
        ColumnData[] cdatas = new ColumnData[] {
            GridPlotter.createCoordColumn( xyMapper, pixer, false, 0.5,
                                           pxlabel, "X bin central value" ),
            GridPlotter.createCoordColumn( xyMapper, pixer, true, 0.5,
                                           pylabel, "Y bin central value" ),
            new ColumnData( vxInfo ) {
                public Double readValue( long lrow ) {
                    int irow = (int) lrow;
                    int ibin = pixer.getBinIndex( xyMapper.getGridX( irow ),
                                                  xyMapper.getGridY( irow ) );
                    double val = ibin >= 0 ? vxResult.getBinValue( ibin )
                                           : Double.NaN;
                    return Double.isNaN( val ) ? null : Double.valueOf( val );
                }
            },
            new ColumnData( vyInfo ) {
                public Double readValue( long lrow ) {
                    int irow = (int) lrow;
                    int ibin = pixer.getBinIndex( xyMapper.getGridX( irow ),
                                                  xyMapper.getGridY( irow ) );
                    double val = ibin >= 0 ? vyResult.getBinValue( ibin )
                                           : Double.NaN;
                    return Double.isNaN( val ) ? null : Double.valueOf( val );
                }
            },
            GridPlotter.createCoordColumn( xyMapper, pixer, false, 0.0,
                                           "LO_" + pxlabel,
                                           "X bin lower bound" ),
            GridPlotter.createCoordColumn( xyMapper, pixer, false, 1.0,
                                           "HI_" + pxlabel,
                                           "X bin upper bound" ),
            GridPlotter.createCoordColumn( xyMapper, pixer, true, 0.0,
                                           "LO_" + pylabel,
                                           "Y bin lower bound" ),
            GridPlotter.createCoordColumn( xyMapper, pixer, true, 1.0,
                                           "HI_" + pylabel,
                                           "Y bin upper bound" ),
        };

        /* Turn the columns into a table and return it. */
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( nrow );
        for ( ColumnData cdata : cdatas ) {
            table.addColumn( cdata );
        }
        return table;
    }

    /**
     * Returns a combination object that can accumulate values and
     * provide a representative maximum size value.
     * This is used to assess the scale of the vectors to be drawn
     * so that they can be scaled to a suitable size for display.
     *
     * <p>The current implementation returns the 90th percentile of
     * the submitted values.
     *
     * @return   scaling combination container
     */
    public static Combiner.Container createScalingCombiner() {
        return Combiner
              .createQuantileCombiner( "scaling", null, SCALING_QUANTILE )
              .createContainer();
    }

    /**
     * Returns a config key for specifying combination method.
     *
     * @return  combiner key
     */
    private static ConfigKey<Combiner> createCombinerKey() {
        ConfigMeta meta = new ConfigMeta( "combine", "Combine" );
        meta.setShortDescription( "Combination mode for vector quantities"
                                + " in bin" );
        meta.setXmlDescription( new String[] {
            "<p>Defines how the vector quantities in each given grid cell",
            "will be combined together to produce the vector to be displayed",
            "in that cell.",
            "</p>",
        } );
        Combiner[] combiners = { Combiner.MEAN, Combiner.SUM };
        OptionConfigKey<Combiner> key =
                new OptionConfigKey<Combiner>( meta, Combiner.class,
                                               combiners ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Returns a configuration key for specifying arrow width.
     *
     * @return  thickness config key
     */
    public static ConfigKey<Integer>
            createThicknessKey( ConfigKey<MultiPointShape> shapeKey ) {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setShortDescription( "Line thickness for "
                                + shapeKey.getMeta().getShortName() );
        meta.setXmlDescription( new String[] {
            "<p>Controls the line thickness when drawing vectors.",
            "Zero, the default value, means a 1-pixel-wide line is used,",
            "and larger values make drawn lines thicker.",
            "May not affect all vector shapes.",
            "</p>",
        } );
        return StyleKeys.createPaintThicknessKey( meta, 3 );
    }

    /**
     * Returns a configuration key for specfying relative scale of painted
     * arrows.
     *
     * @return  scale config key
     */
    private static ConfigKey<Double> createScaleKey() {
        ConfigMeta meta = new ConfigMeta( "scale", "Scale" );
        meta.setStringUsage( "<number>" );
        meta.setShortDescription( "Size multiplier" );
        meta.setXmlDescription( new String[] {
            "<p>Scales the length of the arrows drawn.",
            "The default value, 1, attempts to draw them so that the",
            "longest arrows mostly fit inside the cells they represent,",
            "but adjusting this control will make all the arrows longer",
            "or shorter by the supplied factor.",
            "</p>",
        } );
        return new DoubleConfigKey( meta, 1.0 ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( 1e-2, 1e+2, true, 1.0, false,
                                            SliderSpecifier.TextOption
                                                           .ENTER_ECHO );
            }
        };
    }

    /**
     * Returns a configuration key for specifying whether the visible
     * field mean should be subtracted from values before display.
     *
     * @return  isRelative config key
     */
    private static ConfigKey<Boolean> createRelativeKey() {
        ConfigMeta meta = new ConfigMeta( "relative", "Relative" );
        meta.setShortDescription( "Subtract field mean from vectors?" );
        meta.setXmlDescription( new String[] {
            "<p>If true, the mean vector value for the visible part of",
            "the field will be subtracted from all displayed vectors.",
            "This will display local variations in the vector field",
            "rather than its absolute value.",
            "</p>",
        } );
        return new BooleanConfigKey( meta, false );
    }

    /**
     * Creates a coord for reading a vector component.
     *
     * @param  isY  true for Y component, false for X component
     * @retur   component coord
     */
    private static FloatingCoord createComponentCoord( boolean isY ) {
        String axName = isY ? "Y" : "X";
        String axname = isY ? "y" : "x";
        InputMeta meta = new InputMeta( "v" + axname, axName + " Component" );
        meta.setShortDescription( axName + " component of vector" );
        meta.setXmlDescription( new String[] {
            "<p>" + axName + " component of the vector field to be plotted.",
            "</p>",
        } );
        return FloatingCoord.createCoord( meta, true );
    }

    /**
     * Returns the representative bin size in data coordinates
     * of the bins on one axis.
     *
     * <p>Note this only works properly for linear scaling
     * on the axis concerned.
     *
     * @param  mapper  bin mapper
     * @param  axis    axis to which the mapper applies
     * @return   extent of bin in data coordinates
     */
    private static double getBinExtent( BinMapper mapper, Axis axis ) {
        double[] dlimits = axis.getDataLimits();
        double dmid = 0.5 * ( dlimits[ 0 ] + dlimits[ 1 ] );
        int ibin = mapper.getBinIndex( dmid );
        double[] binBounds = mapper.getBinLimits( ibin );
        return binBounds[ 1 ] - binBounds[ 0 ];
    }

    /**
     * Style implementation for this plotter.
     */
    public static class VectorFieldStyle implements Style {

        private final Color color_;
        private final MultiPointShape shape_;
        private final int thick_;
        private final double scale_;
        private final BinSizer xSizer_;
        private final BinSizer ySizer_;
        private final double xPhase_;
        private final double yPhase_;
        private final Combiner combiner_;
        private final boolean isRelative_;

        /**
         * Constructor.
         *
         * @param  color  colour, may have non-opaque alpha channel
         * @param  shape  defines arrow shape
         * @param  thick  link thickness for arrows
         * @param  scale  scaling factor for arrow size
         * @param  xSizer  defines bin extent on X axis
         * @param  ySizer  defines bin extent on Y axis
         * @param  xPhase  defines bin phase on X axis
         * @param  yPhase  defines bin phase on Y axis
         * @param  combiner  combination method
         * @param  isRelative  if true, subtract the vector mean averaged over
         *                     the visible surface from the individual vectors
         */
        public VectorFieldStyle( Color color, MultiPointShape shape,
                                 int thick, double scale,
                                 BinSizer xSizer, BinSizer ySizer,
                                 double xPhase, double yPhase, 
                                 Combiner combiner, boolean isRelative ) {
            color_ = color;
            shape_ = shape;
            thick_ = thick;
            scale_ = scale;
            xSizer_ = xSizer;
            ySizer_ = ySizer;
            xPhase_ = xPhase;
            yPhase_ = yPhase;
            combiner_ = combiner;
            isRelative_ = isRelative;
        }

        public Icon getLegendIcon() {
            return shape_.getLegendIcon( shape_.createScribe( thick_ ),
                                         new ErrorMode[] {
                                             ErrorMode.UPPER, ErrorMode.UPPER,
                                         }, 10, 10, 2, 2 );
        }

        @Override
        public int hashCode() {
            int code = 7900662;
            code = 23 * code + color_.hashCode();
            code = 23 * code + shape_.hashCode();
            code = 23 * code + thick_;
            code = 23 * code + Float.floatToIntBits( (float) scale_ );
            code = 23 * code + xSizer_.hashCode();
            code = 23 * code + ySizer_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) xPhase_ );
            code = 23 * code + Float.floatToIntBits( (float) yPhase_ );
            code = 23 * code + combiner_.hashCode();
            code = 23 * code + ( isRelative_ ? 19 : 41 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof VectorFieldStyle ) {
                VectorFieldStyle other = (VectorFieldStyle) o;
                return this.color_.equals( other.color_ )
                    && this.shape_.equals( other.shape_ )
                    && this.thick_ == other.thick_
                    && this.scale_ == other.scale_
                    && this.xSizer_.equals( other.xSizer_ )
                    && this.ySizer_.equals( other.ySizer_ )
                    && this.xPhase_ == other.xPhase_
                    && this.yPhase_ == other.yPhase_
                    && this.combiner_.equals( other.combiner_ )
                    && this.isRelative_ == other.isRelative_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for this plotter.
     */
    private static class VectorFieldLayer extends AbstractPlotLayer {

        private final VectorFieldPlotter plotter_;
        private final VectorFieldStyle style_;
        private final int icPos_;
        private final int icVx_;
        private final int icVy_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  plotter  plotter
         * @param  geom   geom
         * @param  dataSpec   data specification
         * @param  style  plotting style
         */
        VectorFieldLayer( VectorFieldPlotter plotter, DataGeom geom,
                          DataSpec dataSpec, VectorFieldStyle style ) {
            super( plotter, geom, dataSpec, style,
                   style.color_.getAlpha() == 255 ? LayerOpt.OPAQUE
                                                  : LayerOpt.NO_SPECIAL );
            plotter_ = plotter;
            style_ = style;
            CoordGroup cgrp = plotter.coordGrp_;
            icPos_ = cgrp.getPosCoordIndex( 0, geom );
            icVx_ = cgrp.getExtraCoordIndex( 0, geom );
            icVy_ = cgrp.getExtraCoordIndex( 1, geom );
            icWeight_ = plotter.hasWeight_ ? cgrp.getExtraCoordIndex( 2, geom )
                                           : -1;
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Span> auxSpans,
                                      PaperType ptype ) {
            return new VectorFieldDrawing( (PlanarSurface) surface, ptype );
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
            BinSizer[] sizers = { style_.xSizer_, style_.ySizer_ };
            double[] phases = { style_.xPhase_, style_.yPhase_ };
            Axis[] axes = surface.getAxes();
            for ( int i = 0; i < 2; i++ ) {
                Axis axis = axes[ i ];
                Scale scale = axis.getScale();
                double[] dataLimits = axis.getDataLimits();
                double dlo = dataLimits[ 0 ];
                double dhi = dataLimits[ 1 ];
                double[] drange =
                    PlotUtil.scaleRange( dlo, dhi, padder, scale );
                double reqWidth =
                    sizers[ i ].getScaleWidth( scale, dlo, dhi, true );

                /* Sub-pixel grids would be pointless and expensive. */
                double binWidth =
                    Math.max( reqWidth,
                              GridPlotter.getRoundedPixelScaleWidth( axis ) );
                double phase = phases[ i ];
                grids[ i ] = new GridSpec( scale, binWidth, phase, drange );
            }
            return new GridPixer( grids[ 0 ], grids[ 1 ] );
        }

        /**
         * Accumulates averaged vector values for each grid bin.
         *
         * @param  pixer  defines grid shape
         * @param  dataSpec  data specification
         * @param  dataStore  data store
         * @return   averaged vector components for each grid bin
         */
        private XYBinList readBins( GridPixer pixer, DataSpec dataSpec,
                                    DataStore dataStore ) {
            Combiner combiner = style_.combiner_;
            DataGeom geom = getDataGeom();
            int icw = dataSpec.isCoordBlank( icWeight_ ) ? -1 : icWeight_;
            BinCollector collector =
                new BinCollector( combiner, pixer, geom,
                                  icPos_, icVx_, icVy_, icw );
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
        private VectorFieldPlan getVectorFieldPlan( Object[] knownPlans,
                                                    GridPixer pixer ) {
            Combiner combiner = style_.combiner_;
            DataSpec dataSpec = getDataSpec();
            DataGeom geom = getDataGeom();
            return Arrays.stream( knownPlans )
                  .filter( p -> p instanceof VectorFieldPlan )
                  .map( p -> (VectorFieldPlan) p )
                  .filter( p -> p.matchesData( pixer, combiner,
                                               dataSpec, geom ) )
                  .findFirst()
                  .orElse( null );
        }

        /**
         * Drawing implementation for this plotter.
         */
        private class VectorFieldDrawing implements Drawing {

            private final PlanarSurface surface_;
            private final PaperType ptype_;

            /**
             * Constructor.
             *
             * @param  surface  plotting surface
             * @param  ptype  paper type
             */
            VectorFieldDrawing( PlanarSurface surface, PaperType ptype ) {
                surface_ = surface;
                ptype_ = ptype;
            }

            public VectorFieldPlan calculatePlan( Object[] knownPlans,
                                                  DataStore dataStore ) {

                /* Acquire a plan that has done the heavy lifting:
                 * iterating over all input data. */
                GridPixer pixer0 = createGridPixer( surface_, 0 );
                VectorFieldPlan knownPlan =
                    getVectorFieldPlan( knownPlans, pixer0 );
                final VectorFieldPlan basicPlan;
                if ( knownPlan != null ) {
                    basicPlan = knownPlan;
                }
                else {
                    DataSpec dataSpec = getDataSpec();
                    GridPixer pixer1 = createGridPixer( surface_, PADDING );
                    XYBinList xybins = readBins( pixer1, dataSpec, dataStore );
                    BinList.Result xResult =
                        xybins.xBinList_.getResult().compact();
                    BinList.Result yResult =
                        xybins.yBinList_.getResult().compact();
                    basicPlan = new VectorFieldPlan( pixer1, style_.combiner_,
                                                     dataSpec, getDataGeom(),
                                                     xResult, yResult );
                }

                /* Perform additional calculations that only need to iterate
                 * over every averaged cell.  The plan needs this information
                 * so it can supply it to the ReportMap. */
                return basicPlan
                      .getAdjustedPlan( surface_, style_.isRelative_ );
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                VectorFieldPlan vplan = (VectorFieldPlan) plan;
                MultiPointScribe scribe =
                    style_.shape_.createScribe( style_.thick_ );
                Color color = style_.color_;
                double gfactor = vplan.gscale_ * style_.scale_;
                boolean isOpaque = color.getAlpha() == 255;
                assert isOpaque || plotter_.transparent_;
                ptype_.placeDecal( paper, new Decal() {
                    public boolean isOpaque() {
                        return isOpaque;
                    }
                    public void paintDecal( Graphics g ) {
                        paintVectors( g, surface_, scribe, color, style_.scale_,
                                      style_.isRelative_, vplan.pixer_,
                                      vplan.xResult_, vplan.yResult_,
                                      vplan.xoff_, vplan.yoff_, gfactor );
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                VectorFieldPlan vplan = (VectorFieldPlan) plan;
                ReportMap report = new ReportMap();
                if ( vplan.isRelative_ ) {
                    report.put( XOFFSET_REPKEY, Double.valueOf( vplan.xoff_ ) );
                    report.put( YOFFSET_REPKEY, Double.valueOf( vplan.yoff_ ) );
                }
                report.put( FIELDTABLE_REPKEY,
                            createExportTable( vplan, plotter_ ) );
                GridPixer pixer = createGridPixer( surface_, 0.0 );
                report.put( XBINWIDTH_REPKEY,
                            Double.valueOf( pixer.getGridX().getBinWidth() ) );
                report.put( YBINWIDTH_REPKEY,
                            Double.valueOf( pixer.getGridY().getBinWidth() ) );
                return report;
            }
        }
    }

    /**
     * Accumulates vector components from data.
     */
    private static class BinCollector
            implements SplitCollector<TupleSequence,XYBinList> {

        private final Combiner combiner_;
        private final GridPixer pixer_;
        private final DataGeom geom_;
        private final int icPos_;
        private final int icVx_;
        private final int icVy_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  combiner  combination mode
         * @param  pixer      grid definition
         * @param  geom     datageom
         * @param  icPos    column index for positional coordinate
         * @param  icVx     column index for vector X component
         * @param  icVy     column index for vector Y component
         * @param  icWeight   column index for weight coordinate,
         *                    or -1 for unweighted
         */
        BinCollector( Combiner combiner, GridPixer pixer, DataGeom geom,
                      int icPos, int icVx, int icVy, int icWeight ) {
            combiner_ = combiner;
            pixer_ = pixer;
            geom_ = geom;
            icPos_ = icPos;
            icVx_ = icVx;
            icVy_ = icVy;
            icWeight_ = icWeight;
        }

        public XYBinList createAccumulator() {
            return new XYBinList( pixer_.getBinCount(), combiner_ );
        }

        public void accumulate( TupleSequence tseq, XYBinList xybins ) {
            double[] dpos = new double[ geom_.getDataDimCount() ];
            BinList xbins = xybins.xBinList_;
            BinList ybins = xybins.yBinList_;
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, icPos_, dpos ) ) {
                    int ibin = pixer_.getBinIndex( dpos );
                    if ( ibin >= 0 ) {
                        double vx = VX_COORD.readDoubleCoord( tseq, icVx_ );
                        double vy = VY_COORD.readDoubleCoord( tseq, icVy_ );
                        if ( !Double.isNaN( vx ) && !Double.isNaN( vy ) ) {
                            if ( icWeight_ >= 0 ) {
                                double w = WEIGHT_COORD
                                          .readDoubleCoord( tseq, icWeight_ );
                                if ( !Double.isNaN( w ) ) {
                                    xbins.submitToBin( ibin, vx * w );
                                    ybins.submitToBin( ibin, vy * w );
                                }
                            }
                            else {
                                xbins.submitToBin( ibin, vx );
                                ybins.submitToBin( ibin, vy );
                            }
                        }
                    }
                }
            }
        }

        public XYBinList combine( XYBinList xybins1, XYBinList xybins2 ) {
            xybins1.xBinList_.addBins( xybins2.xBinList_ );
            xybins1.yBinList_.addBins( xybins2.yBinList_ );
            return xybins1;
        }
    }

    /**
     * Plan containing pre-calculated information for plotting binned vectors.
     */
    private static class VectorFieldPlan {

        final GridPixer pixer_;
        final Combiner combiner_;
        final DataSpec dataSpec_;
        final DataGeom geom_;
        final BinList.Result xResult_;
        final BinList.Result yResult_;
        final PlanarSurface surface_;
        final boolean isRelative_;
        final double xoff_;
        final double yoff_;
        final double gscale_;

        /**
         * Constructs a skeleton plan from the binned vector X and Y components.
         * An instance constructed like this has the results of the
         * expensive calculations, but is not yet usable for plotting.
         *
         * @param  pixer  defines grid geometry
         * @param  combiner   combination method
         * @param  dataSpec  data specification
         * @param  geom   geom
         * @param  xResult  binned vector X components
         * @param  yResult  binned vector Y components
         */
        VectorFieldPlan( GridPixer pixer, Combiner combiner,
                         DataSpec dataSpec, DataGeom geom,
                         BinList.Result xResult, BinList.Result yResult ) {
            pixer_ = pixer;
            combiner_ = combiner;
            dataSpec_ = dataSpec;
            geom_ = geom;
            xResult_ = xResult;
            yResult_ = yResult;
            surface_ = null;
            isRelative_ = false;
            xoff_ = 0;
            yoff_ = 0;
            gscale_ = Double.NaN;
        }

        /**
         * Constructs a usable plan based on an existing (skeleton or usable)
         * template plus some additional information calculated from
         * the binned data.
         *
         * @param   plan  template plan supplying binned data
         * @param   surface  plotting surface
         * @param  isRelative  if true, subtract the vector mean averaged over
         *                     the visible surface from the individual vectors
         * @param  xoff   X component vector subtractive offset,
         *                non-zero only if isRelative is true
         * @param  yoff   Y component vector subtractive offset,
         *                non-zero only if isRelative is true
         * @param  gscale  graphical arrow drawing scale factor
         */
        VectorFieldPlan( VectorFieldPlan plan, PlanarSurface surface,
                         boolean isRelative, double xoff, double yoff,
                         double gscale ) {
            pixer_ = plan.pixer_;
            combiner_ = plan.combiner_;
            dataSpec_ = plan.dataSpec_;
            geom_ = plan.geom_;
            xResult_ = plan.xResult_;
            yResult_ = plan.yResult_;
            surface_ = surface;
            isRelative_ = isRelative;
            xoff_ = xoff;
            yoff_ = yoff;
            gscale_ = gscale;
        }

        /**
         * Indicates whether the skeleton of this plan can be used
         * for a given set of drawing requirements.
         *
         * @param  pixer   grid mapping object
         * @param  combiner  combination method for values
         * @param  dataSpec   data specification
         * @param  geom     geom
         */
        public boolean matchesData( GridPixer pixer,  Combiner combiner,
                                    DataSpec dataSpec, DataGeom geom ) {
            return pixer_.contains( pixer )
                && combiner_.equals( combiner )
                && dataSpec_.equals( dataSpec )
                && geom_.equals( geom );
        }

        /**
         * Using the binned data from this plan, acquires a plan suitable
         * for plotting on a given surface.  This may do some work, but
         * not as much as constructing a plan from scratch.
         *
         * @param   surface  plotting surface
         * @param  isRelative  if true, subtract the vector mean averaged over
         *                     the visible surface from the individual vectors
         * @return  plan for use with supplied surface
         */
        public VectorFieldPlan getAdjustedPlan( PlanarSurface surface,
                                                boolean isRelative ) {
            if ( surface.equals( surface_ ) && isRelative == isRelative_ ) {
                return this;
            }
            else {
                Axis xaxis = surface.getAxes()[ 0 ];
                Axis yaxis = surface.getAxes()[ 1 ];
                int[] ixRange =
                    pixer_.getGridX().getBinRange( xaxis.getDataLimits() );
                int[] iyRange =
                    pixer_.getGridY().getBinRange( yaxis.getDataLimits() );
                int ixlo = ixRange[ 0 ];
                int ixhi = ixRange[ 1 ];
                int iylo = iyRange[ 0 ];
                int iyhi = iyRange[ 1 ];
                double[] dpos = new double[ 2 ];
                Point2D.Double gpos = new Point2D.Double();
                final double xoff;
                final double yoff;

                /* If required, calculate the mean vector components over
                 * the field of view so we can subtract them to provide
                 * relative vectors. */
                if ( isRelative ) {
                    int count = 0;
                    double xsum = 0;
                    double ysum = 0;
                    for ( int iy = iylo; iy < iyhi; iy++ ) {
                        for ( int ix = ixlo; ix < ixhi; ix++ ) {
                            int ibin = pixer_.getBinIndex( ix, iy );
                            double vx = xResult_.getBinValue( ibin );
                            double vy = yResult_.getBinValue( ibin );
                            if ( vx * vx + vy * vy > 0 ) {
                                if ( surface.dataToGraphics( dpos, false,
                                                             gpos ) ) {
                                    count += 1;
                                    xsum += vx;
                                    ysum += vy;
                                }
                            }
                        }
                    }
                    xoff = count > 0 ? xsum / count : 0;
                    yoff = count > 0 ? ysum / count : 0;
                }
                else {
                    xoff = 0;
                    yoff = 0;
                }

                /* Calculate representative size of vectors for visual
                 * scaling purposes. There is currently no absolute scaling. */
                Combiner.Container xquantiler = createScalingCombiner();
                Combiner.Container yquantiler = createScalingCombiner();
                for ( int iy = iylo; iy <= iyhi; iy++ ) {
                    for ( int ix = ixlo; ix <= ixhi; ix++ ) {
                        int ibin = pixer_.getBinIndex( ix, iy );
                        assert ibin >= 0;
                        double vx = xResult_.getBinValue( ibin );
                        double vy = yResult_.getBinValue( ibin );
                        if ( vx * vx + vy * vy > 0 ) {
                            if ( surface.dataToGraphics( dpos, false, gpos ) ) {
                                xquantiler.submit( Math.abs( vx - xoff ) );
                                yquantiler.submit( Math.abs( vy - yoff ) );
                            }
                        }
                    }
                }
                double xquantile = xquantiler.getCombinedValue();
                double yquantile = yquantiler.getCombinedValue();

                /* This scaling factor only really works properly for
                 * linear axes.  But for nonlinear ones it's hard to know
                 * what's meant anyway: should the vector plotted components
                 * be constant in graphics space, or constant in data space?
                 * It depends what they represent.  For now leave it like
                 * this and wait for somebody to complain. */
                double xscale = 
                    getBinExtent( pixer_.getGridX().getMapper(), xaxis );
                double yscale =
                    getBinExtent( pixer_.getGridY().getMapper(), yaxis );
                double gscale = Math.min( xscale / xquantile,
                                          yscale / yquantile );

                /* Construct and return a new plan based on this one
                 * but including the surface-specific calulations. */
                return new VectorFieldPlan( this, surface, isRelative,
                                            xoff, yoff, gscale );
            }
        }
    }

    /**
     * Aggregates a bin list for X and Y vector components.
     */
    private static class XYBinList {

        final ArrayBinList xBinList_;
        final ArrayBinList yBinList_;

        /**
         * Constructor.
         *
         * @param  nbin  bin count
         * @param  combiner  combination method
         */
        XYBinList( int nbin, Combiner combiner ) {
            xBinList_ = combiner.createArrayBinList( nbin );
            yBinList_ = combiner.createArrayBinList( nbin );
        }
    }
}
