package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
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
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that plots a genuine density map on a SkySurface.
 * It paints a single Decal, no Glyphs.
 *
 * <p>Note it only works with a SkySurface.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public class SkyDensityPlotter
             implements Plotter<SkyDensityPlotter.SkyDenseStyle> {

    private final boolean transparent_;
    private final CoordGroup coordGrp_;
    private final FloatingCoord weightCoord_;
    private final boolean reportAuxKeys_;

    /** Weighting coordinate. */
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;

    /** Report key for HEALPix tile area in square degrees. */
    public static final ReportKey<Double> TILESIZE_REPKEY =
        ReportKey.createDoubleKey( new ReportMeta( "tile_sqdeg",
                                                   "Tile size/sq.deg" ),
                                   true );

    private static final ReportKey<Integer> ABSLEVEL_REPKEY =
        ReportKey
       .createIntegerKey( new ReportMeta( "abs_level",
                                          "HEALPix Level" ),
                          true );
    private static final ReportKey<Integer> RELLEVEL_REPKEY =
        ReportKey
       .createIntegerKey( new ReportMeta( "rel_level",
                                          "Relative HEALPix Level" ),
                          false );
    private static final ReportKey<StarTable> HPXTABLE_REPKEY =
        ReportKey.createTableKey( new ReportMeta( "hpx_map", "HEALPix Map" ),
                                  true );

    private static final AuxScale SCALE = AuxScale.COLOR;

    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<Integer> LEVEL_KEY =
        IntegerConfigKey.createSpinnerPairKey(
            new ConfigMeta( "level", "HEALPix Level" )
           .setStringUsage( "<-rel-level|+abs-level>" )
           .setShortDescription( "HEALPix level, negative for relative" )
           .setXmlDescription( new String[] {
                "<p>Determines the HEALPix level of pixels which are averaged",
                "over to calculate density.",
                "</p>",
                "<p>If the supplied value is a non-negative integer,",
                "it gives the absolute level to use;",
                "at level 0 there are 12 pixels on the sky, and",
                "the count multiplies by 4 for each increment.",
                "</p>",
                "<p>If the value is negative, it represents a relative level;",
                "it is approximately the (negative) number of screen pixels",
                "along one side of a HEALPix sky pixel.",
                "In this case the actual HEALPix level will depend on",
                "the current zoom.",
                "</p>",
            } )
        , -3, 29, -8, "Abs", "Rel", ABSLEVEL_REPKEY, RELLEVEL_REPKEY );

    /** Config key for transparency. */
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;

    /** Config key for combination mode. */
    public static final ConfigKey<Combiner> COMBINER_KEY;

    /** Config key for solid angle units. */
    public static final PerUnitConfigKey<SolidAngleUnit> ANGLE_KEY;

    static {
        ConfigMeta angleMeta = new ConfigMeta( "perunit", "Per Unit" );
        ConfigMeta combineMeta = new ConfigMeta( "combine", "Combine" );
        angleMeta.setShortDescription( "Solid angle unit for densities" );
        angleMeta.setXmlDescription( new String[] {
            "<p>Defines the unit of sky area used for scaling density-like",
            "combinations",
            "(e.g. <code>" + combineMeta.getShortName() + "</code>=" +
            "<code>" + Combiner.DENSITY + "</code> or",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code>).",
            "If the Combination mode is calculating values per unit area,",
            "this configures the area scale in question.", 
            "For non-density-like combination modes",
            "(e.g. <code>" + combineMeta.getShortName() + "</code>=" +
            "<code>" + Combiner.SUM + "</code> or ",
            "<code>" + Combiner.MEAN + "</code>)",
            "it has no effect.",
            "</p>", 
        } );
        combineMeta.setShortDescription( "Value combination mode" );
        combineMeta.setXmlDescription( new String[] {
            "<p>Defines how values contributing to the same density map bin",
            "are combined together to produce the value assigned to that bin,",
            "and hence its colour.",
            "The combined values are the weights, but if the",
            "<code>" + WEIGHT_COORD.getInput().getMeta().getShortName()
                     + "</code>",
            "is left blank, a weighting of unity is assumed.",
            "</p>",
            "<p>For density-like values",
            "(<code>" + Combiner.DENSITY + "</code>,",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code>)",
            "the scaling is additionally influenced by the",
            "<code>" + angleMeta.getShortName() + "</code> parameter.",
            "</p>",
        } );
        ANGLE_KEY =
            new PerUnitConfigKey<SolidAngleUnit>(
                    angleMeta, SolidAngleUnit.class,
                    SolidAngleUnit.getKnownUnits(),
                    SolidAngleUnit.DEGREE2 );
        COMBINER_KEY =
                new OptionConfigKey<Combiner>( combineMeta, Combiner.class,
                                               Combiner.getKnownCombiners(),
                                               Combiner.WEIGHTED_DENSITY ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        }.setOptionUsage()
         .addOptionsXml();
    }
    private static final ReportKey<Combiner.Type> CTYPE_REPKEY =
        ANGLE_KEY.getCombinerTypeReportKey();

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     * @param  hasWeight    if true, an optional weight coordinate will
     *                      be solicited alongside the positional coordinates
     */
    public SkyDensityPlotter( boolean transparent, boolean hasWeight ) {
        transparent_ = transparent;
        weightCoord_ = hasWeight ? FloatingCoord.WEIGHT_COORD : null;
        Coord[] extraCoords = weightCoord_ == null
                            ? new Coord[ 0 ]
                            : new Coord[] { weightCoord_ };
        coordGrp_ = CoordGroup.createCoordGroup( 1, extraCoords );

        /* Set reportAuxKeys false, since the colour ramp config will
         * usually be controlled globally at the level of the plot. */
        reportAuxKeys_ = false;
    }

    public String getPlotterName() {
        return "SkyDensity";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_SKYDENSITY;
    }

    public CoordGroup getCoordGroup() {
        return coordGrp_;
    }

    public boolean hasReports() {
        return true;
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots a density map on the sky.\n" )
            .append( "The grid on which the values are drawn uses\n" )
            .append( "the HEALPix tesselation,\n" )
            .append( "with a configurable resolution.\n" )
            .append( "You can optionally use a weighting for the points,\n" )
            .append( "and you can configure how the points are combined\n" )
            .append( "to produce the output pixel values.\n" )
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
        keyList.add( LEVEL_KEY );
        if ( weightCoord_ != null ) {
            keyList.add( COMBINER_KEY );
            keyList.add( ANGLE_KEY );
        }
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public SkyDenseStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int level = config.get( LEVEL_KEY );
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        Combiner combiner = weightCoord_ == null
                          ? Combiner.COUNT
                          : config.get( COMBINER_KEY );
        SolidAngleUnit unit = config.get( ANGLE_KEY );
        return new SkyDenseStyle( level, scaling, dataclip, shader, combiner,
                                  unit );
    }

    public Object getRangeStyleKey( SkyDenseStyle style ) {
        return null;
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  SkyDenseStyle style ) {
        return new SkyDensityLayer( (SkyDataGeom) geom, dataSpec, style );
    }

    /**
     * Calculates the HEALPix level whose pixels are of approximately
     * the same size as the screen pixels for a given SkySurface.
     * There is not an exact correspondance here.
     * An attempt is made to return the result for the "largest" screen pixel
     * (the one covering more of the sky than any other).
     *
     * @param  surface  plot surface
     * @return  approximately corresponding HEALPix level
     */
    public static int getPixelLevel( SkySurface surface ) {

        /* Identify the graphics pixel at the center of the sky projection.
         * It may be off the currently visible part of the screen;
         * that doesn't matter.  This is likely to be the largest
         * screen pixel. */
        Point p = surface.getSkyCenter();
        double[] p1 =
            surface.graphicsToData( new Point( p.x - 1, p.y - 1 ), null );
        double[] p2 =
            surface.graphicsToData( new Point( p.x + 1, p.y + 1 ), null );
        double pixTheta = vectorSeparation( p1, p2 ) / Math.sqrt( 4 + 4 );
        return Tilings.healpixK( Math.toDegrees( pixTheta ) );
    }

    /**
     * Angle in radians between two (not necessarily unit) vectors.
     * The code follows that of SLA_SEPV from SLALIB.
     * The straightforward thing to do would just be to use the cosine rule,
     * but that may suffer numeric instabilities for small angles,
     * so this more complicated approach is more robust.
     *
     * @param  p1  first input vector
     * @param  p2  second input vector
     * @return   angle between p1 and p2 in radians
     */
    public static double vectorSeparation( double[] p1, double[] p2 ) {
        double modCross = Matrices.mod( Matrices.cross( p1, p2 ) );
        double dot = Matrices.dot( p1, p2 );
        return modCross == 0 && dot == 0 ? 0 : Math.atan2( modCross, dot );
    }

    /**
     * Style for configuring with the sky density plot.
     */
    public static class SkyDenseStyle implements Style {

        private final int level_;
        private final Scaling scaling_;
        private final Subrange dataclip_;
        private final Shader shader_;
        private final Combiner combiner_;
        private final SolidAngleUnit unit_;

        /**
         * Constructor.
         *
         * @param   level   HEALPix level defining the requested map resolution;
         *                  note the actual resolution at which the densities
         *                  are calculated may be different from this,
         *                  in particular if the screen pixel grid is coarser
         *                  than that defined by this level
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   dataclip  scaling function adjustment subrange
         * @param   shader   colour map
         * @param   combiner  value combination mode for bin calculation
         * @param   unit     solid angle unit for density combinations
         */
        public SkyDenseStyle( int level, Scaling scaling, Subrange dataclip,
                              Shader shader, Combiner combiner,
                              SolidAngleUnit unit ) {
            level_ = level;
            scaling_ = scaling;
            dataclip_ = dataclip;
            shader_ = shader;
            combiner_ = combiner;
            unit_ = unit;
        }

        /**
         * Indicates whether this style has any transparency.
         *
         * @return   if true, the colours painted by this shader within
         *           the plot's geometric region of validity (that is,
         *           on the sky) are guaranteed always to have an alpha
         *           value of 1
         */
        boolean isOpaque() {
            return ! Shaders.isTransparent( shader_ );
        }

        public Icon getLegendIcon() {
            return Shaders.createShaderIcon( shader_, null, true, 16, 8, 2, 2 );
        }

        @Override
        public int hashCode() {
            int code = 23443;
            code = 23 * code + level_;
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + combiner_.hashCode();
            code = 23 * code + unit_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SkyDenseStyle ) {
                SkyDenseStyle other = (SkyDenseStyle) o;
                return this.level_ == other.level_
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && this.shader_.equals( other.shader_ )
                    && this.combiner_.equals( other.combiner_ )
                    && this.unit_.equals( other.unit_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for sky density plotter.
     */
    private class SkyDensityLayer extends AbstractPlotLayer {

        private final SkyDenseStyle dstyle_;
        private final SkyDataGeom geom_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  geom  geom
         * @param  dataSpec   data specification
         * @param  style   layer style
         */
        SkyDensityLayer( SkyDataGeom geom, DataSpec dataSpec,
                         SkyDenseStyle style ) {
            super( SkyDensityPlotter.this, geom, dataSpec, style,
                   style.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL );
            dstyle_ = style;
            geom_ = geom;
            icWeight_ = weightCoord_ == null
                      ? -1
                      : coordGrp_.getExtraCoordIndex( 0, geom );
            assert weightCoord_ == null ||
                   weightCoord_ == dataSpec.getCoord( icWeight_ );
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Span> auxSpans,
                                      PaperType paperType ) {
            SkySurface ssurf = (SkySurface) surface;
            return new SkyDensityDrawing( ssurf, createTileRenderer( ssurf ),
                                          auxSpans.get( SCALE ), paperType );
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icWeight_;
                }
                public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                    return getCombinedInfo( dataSpec );
                }
                public Scaling getScaling() {
                    return dstyle_.scaling_;
                }
                public void adjustAuxRange( Surface surface, DataSpec dataSpec,
                                            DataStore dataStore,
                                            Object[] knownPlans,
                                            Ranger ranger ) {
                    SkySurface ssurf = (SkySurface) surface;
                    int level = getLevel( ssurf );

                    /* If we have a cached plan, use that for the ranging.
                     * This will be faster than rebinning the data for
                     * large datasets (no data scan required), but it's
                     * not all that efficient, so will probably be slower
                     * for small datasets.
                     * This is a possible target for future optimisation. */
                    SkyDensityPlan splan =
                        getSkyPlan( knownPlans, level, dataSpec );
                    if ( splan != null ) {
                        splan.extendVisibleRange( ranger, ssurf,
                                                  getBinFactor( level ) );
                    }
                    else {
                        BinList.Result binResult =
                            readBins( ssurf, dataSpec, dataStore )
                           .getResult();
                        createTileRenderer( ssurf )
                       .extendAuxRange( ranger, binResult );
                    }
                }
            } );
            return map;
        }

        /**
         * Returns a tile renderer that can plot healpix tiles on a given
         * sky surface for this layer.
         *
         * @param  surface  sky surface
         * @return  tile renderer
         */
        private SkyTileRenderer createTileRenderer( SkySurface surface ) {
            int level = getLevel( surface );
            return SkyTileRenderer
                  .createRenderer( surface, Rotation.IDENTITY, level,
                                   getBinFactor( level ) );
        }

        /**
         * Returns the HEALPix level that this layer will plot pixels at
         * for a given plot surface.
         *
         * @param  surface   plot surface
         * @return   HEALPix grid level
         */
        private int getLevel( SkySurface surface ) {
            int pixLevel = getPixelLevel( surface );
            return dstyle_.level_ >= 0
                   ? Math.min( dstyle_.level_, pixLevel )
                   : Math.max( 0, pixLevel + dstyle_.level_ );
        }

        /**
         * Returns the value by which all bin result values should be
         * multiplied for a given level.
         *
         * @param   level  HEALPix level for plotting
         * @return   bin multiplication factor
         */
        private double getBinFactor( int level ) {
            double binExtent = Tilings.healpixSqdeg( level )
                             / dstyle_.unit_.getExtentInSquareDegrees();
            Combiner.Type ctype = dstyle_.combiner_.getType();
            return ctype.getBinFactor( binExtent );
        }

        /**
         * Constructs and populates a bin list (weighted histogram) 
         * suitable for plotting this layer on a given surface.
         *
         * @param   surface   target plot surface
         * @param   dataSpec   data specification
         * @param   dataStore  data storage
         * @return   populated bin list
         * @slow
         */
        private BinList readBins( SkySurface surface, DataSpec dataSpec,
                                  DataStore dataStore ) {
            final int level = getLevel( surface );
            final long npix = new SkyPixer( level ).getPixelCount();
            final Combiner combiner = dstyle_.combiner_;
            final int icPos = coordGrp_.getPosCoordIndex( 0, geom_ );
            final boolean isUnweighted =
                icWeight_ < 0 || dataSpec.isCoordBlank( icWeight_ );
            BinListCollector collector =
                    new BinListCollector( combiner, npix ) {
                public void accumulate( TupleSequence tseq, BinList binList ) {
                    SkyPixer skyPixer = new SkyPixer( level );
                    double[] v3 = new double[ 3 ];
                    if ( isUnweighted ) {
                        while ( tseq.next() ) {
                            if ( geom_.readDataPos( tseq, icPos, v3 ) ) {
                                long ihpx = skyPixer.getIndex( v3 );
                                binList.submitToBin( ihpx, 1 );
                            }
                        }
                    }
                    else {
                        while ( tseq.next() ) {
                            if ( geom_.readDataPos( tseq, icPos, v3 ) ) {
                                double w = weightCoord_
                                          .readDoubleCoord( tseq, icWeight_ );
                                if ( ! Double.isNaN( w ) ) {
                                    long ihpx = skyPixer.getIndex( v3 );
                                    binList.submitToBin( ihpx, w );
                                }
                            }
                        }
                    }
                }
            };
            return PlotUtil.tupleCollect( collector, dataSpec, dataStore );
        }

        /**
         * Identifies and returns a plan object that can be used for
         * this layer from a list of precalculated plans.
         * If none of the supplied plans is suitable, null is returned.
         *
         * @param  knownPlans  available pre-calculated plans
         * @param  level   HEALPix level giving desired sky pixel resolution
         * @param  dataSpec  specification for plotted weith data
         * @return   suitable SkyDensityPlan from supplied list, or null
         */
        private SkyDensityPlan getSkyPlan( Object[] knownPlans, int level,
                                           DataSpec dataSpec ) {
            Combiner combiner = dstyle_.combiner_;
            for ( Object plan : knownPlans ) {
                if ( plan instanceof SkyDensityPlan ) {
                    SkyDensityPlan skyPlan = (SkyDensityPlan) plan;
                    if ( skyPlan.matches( level, combiner, dataSpec, geom_ ) ) {
                        return skyPlan;
                    }
                }
            }
            return null;
        }

       /**
         * Returns the metadata for the combined values.
         *
         * @param  dataSpec  data specification
         * @return   metadata for gridded cells
         */
        private ValueInfo getCombinedInfo( DataSpec dataSpec ) {
            final ValueInfo weightInfo;
            if ( icWeight_ < 0 || dataSpec.isCoordBlank( icWeight_ ) ) {
                weightInfo = new DefaultValueInfo( "1", Double.class,
                                                   "Weight unspecified"
                                                 + ", taken as unity" ); 
            }
            else {
                ValueInfo[] winfos = dataSpec.getUserCoordInfos( icWeight_ );
                weightInfo = winfos != null && winfos.length == 1
                           ? winfos[ 0 ]
                           : new DefaultValueInfo( "Weight", Double.class );
            }
            return dstyle_.combiner_
                  .createCombinedInfo( weightInfo, dstyle_.unit_ );
        }

        /**
         * Drawing implementation for the sky density map.
         */
        private class SkyDensityDrawing implements Drawing {

            private final SkySurface surface_;
            private final SkyTileRenderer renderer_;
            private final Span auxSpan_;
            private final PaperType paperType_;
            private final int level_;
            private final int pixelLevel_;

            /**
             * Constructor.
             *
             * @param   surface  plot surface
             * @param   renderer  can plot healpix tiles
             * @param   auxSpan   colour scaling range
             * @param   paperType  paper type
             */
            SkyDensityDrawing( SkySurface surface, SkyTileRenderer renderer,
                               Span auxSpan, PaperType paperType ) {
                surface_ = surface;
                renderer_ = renderer;
                auxSpan_ = auxSpan;
                paperType_ = paperType;
                level_ = getLevel( surface );
                pixelLevel_ = getPixelLevel( surface );
            }

            public Object calculatePlan( Object[] knownPlans,
                                         DataStore dataStore ) {
                SkyDensityPlan plan =
                    calculateBasicPlan( knownPlans, dataStore );
                plan.pixelLevel_ = pixelLevel_;
                return plan;
            }

            private SkyDensityPlan calculateBasicPlan( Object[] knownPlans,
                                                       DataStore dataStore ) {
                DataSpec dataSpec = getDataSpec();
                Combiner combiner = dstyle_.combiner_;
                SkyDensityPlan knownPlan =
                    getSkyPlan( knownPlans, level_, dataSpec );
                if ( knownPlan != null ) {
                    return knownPlan;
                }
                else {
                    BinList.Result binResult =
                        readBins( surface_, dataSpec, dataStore )
                       .getResult().compact();
                    return new SkyDensityPlan( level_, combiner, binResult,
                                               dataSpec, geom_ );
                }
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                final BinList.Result binResult =
                    ((SkyDensityPlan) plan).binResult_;
                final Scaler scaler =
                    auxSpan_.createScaler( dstyle_.scaling_,
                                           dstyle_.dataclip_ );
                final Shader shader = dstyle_.shader_;
                paperType_.placeDecal( paper, new Decal() {
                    public void paintDecal( Graphics g ) {
                        renderer_.renderBins( g, binResult, shader, scaler );
                    }
                    public boolean isOpaque() {
                        return dstyle_.isOpaque();
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                ReportMap map = new ReportMap();
                if ( plan instanceof SkyDensityPlan ) {
                    SkyDensityPlan splan = (SkyDensityPlan) plan;
                    int absLevel = splan.level_;
                    int relLevel = absLevel - splan.pixelLevel_;
                    double tileSize = Tilings.healpixSqdeg( absLevel );
                    map.put( ABSLEVEL_REPKEY, new Integer( absLevel ) );
                    map.put( RELLEVEL_REPKEY, new Integer( relLevel ) );
                    map.put( TILESIZE_REPKEY, new Double( tileSize ) );
                    map.put( HPXTABLE_REPKEY, createExportTable( splan ) );
                    map.put( CTYPE_REPKEY, splan.combiner_.getType() );
                }
                return map;
            }
        }

        /**
         * Returns a StarTable containing the HEALPix bin information
         * based on a given plan.  Calling this method is not expensive,
         * though serializing the resulting table may be.
         *
         * <p>Note that the exported table is based on the HEALPix level
         * currently plotted, which may not be what the user asked for
         * (since it doesn't bother to prepare pixels that are too small
         * to see).
         *
         * @param   splan  plan representing plotted density map
         * @return   table suitable for export
         */
        private StarTable createExportTable( SkyDensityPlan splan ) {
            int level = splan.level_;
            final BinList.Result binResult = splan.binResult_;
            SkyPixer pixer = new SkyPixer( level );
            boolean isNested = pixer.isNested();
            HealpixTableInfo.HpxCoordSys csys =
                HealpixSys.fromGeom( geom_.getViewSystem() );
            DataSpec dataSpec = splan.dataSpec_;
            long nsky = pixer.getPixelCount();
            final long ndata = binResult.getBinCount();
            final ColumnData dataCol =
                BinResultColumnData
               .createInstance( getCombinedInfo( dataSpec ), binResult,
                                getBinFactor( level ) );

            /* Full sky table. */
            if ( ndata * 1.0 / nsky > 0.5 && nsky <= Integer.MAX_VALUE ) {
                int nrow = (int) nsky;
                assert nrow == nsky;
                ColumnStarTable table =
                    ColumnStarTable.makeTableWithRows( nrow );
                table.addColumn( dataCol );
                HealpixTableInfo hpxInfo =
                    new HealpixTableInfo( level, isNested, null, csys );
                table.getParameters()
                     .addAll( Arrays.asList( hpxInfo.toParams() ) );
                return table;
            }

            /* Partial sky table. */
            else {
                String indexDescrip = "HEALPix index, level " + level + ", "
                                    + ( isNested ? "Nested" : "Ring" )
                                    + " scheme";
                final boolean isLong = nsky > Integer.MAX_VALUE;
                ColumnInfo indexInfo =
                    new ColumnInfo( "hpx" + level,
                                    isLong ? Long.class : Integer.class,
                                    indexDescrip );
                final ColumnInfo[] infos = new ColumnInfo[] {
                    indexInfo,
                    dataCol.getColumnInfo(),
                };
                StarTable table = new AbstractStarTable() {
                    public ColumnInfo getColumnInfo( int icol ) {
                        return infos[ icol ];
                    }
                    public int getColumnCount() {
                        return infos.length;
                    }
                    public long getRowCount() {
                        return ndata;
                    }
                    public RowSequence getRowSequence() {
                        Iterator<Object[]> rowIt =
                            new BinRowIterator( binResult.indexIterator(),
                                                dataCol, isLong );
                        return new IteratorRowSequence( rowIt );
                    }
                };
                HealpixTableInfo hpxInfo =
                    new HealpixTableInfo( level, isNested, indexInfo.getName(),
                                          csys );
                table.getParameters()
                     .addAll( Arrays.asList( hpxInfo.toParams() ) );
                return table;
            }
        }
    }

    /**
     * Iterator over Object[2] values giving an (index, value) pair
     * for each non-blank entry in a BinList.Result.
     * Suitable for use in an IteratorRowSequence;
     */
    private static class BinRowIterator implements Iterator<Object[]> {
        private final Iterator<Long> indexIt_;
        private final ColumnData dataCol_;
        private final boolean isLong_;

        /**
         * Constructor.
         *
         * @param  indexIt  iterator over pixel indices to include
         * @param  dataCol  contains data values at the row of each pixel index
         * @param  isLong   if true, output index number is a Long;
         *                  if false, it's an Integer
         */
        BinRowIterator( Iterator<Long> indexIt, ColumnData dataCol,
                        boolean isLong ) {
            indexIt_ = indexIt;
            dataCol_ = dataCol;
            isLong_ = isLong;
        }

        public Object[] next() {
            Long index = indexIt_.next();
            long ix = index.longValue();
            final Object ixObj;

            /* Careful: silent unboxing can do horrible things here.
             * Evaluating "ixObj = isLong_ ? index : new Integer( (int) ix )"
             * gives you a Long even when isLong_ false!! */
            if ( isLong_ ) {
                ixObj = index;
            }
            else {
                ixObj = new Integer( (int) ix );
            }

            final Object dataObj;
            try {
                dataObj = dataCol_.readValue( ix );
            }
            catch ( IOException e ) {  // shouldn't happen
                throw new IteratorRowSequence.PackagedIOException( e );
            }
            return new Object[] { ixObj, dataObj };
        }
        public boolean hasNext() {
            return indexIt_.hasNext();
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Plot layer plan for the sky density map.
     * Note the basic data cached in the plan is currently the sky pixel
     * grid, not the screen pixel grid.  That means that drawing the
     * plot will take a little bit of time (though it will scale only
     * with plot pixel count, not with dataset size).
     */
    private static class SkyDensityPlan {
        final int level_;
        final Combiner combiner_;
        final BinList.Result binResult_;
        final DataSpec dataSpec_;
        final SkyDataGeom geom_;
        int pixelLevel_;

        /**
         * Constructor.
         *
         * @param   level   HEALPix level
         * @param   combiner  combination method for input values
         * @param   binResult  data structure containing sky pixel values
         * @param   dataSpec  data specification used to generate binList
         * @param   geom   sky geometry used to generate binList
         */
        SkyDensityPlan( int level, Combiner combiner, BinList.Result binResult,
                        DataSpec dataSpec, SkyDataGeom geom ) {
            level_ = level;
            combiner_ = combiner;
            binResult_ = binResult;
            dataSpec_ = dataSpec;
            geom_ = geom;
            pixelLevel_ = Integer.MIN_VALUE;
        }

        /**
         * Indicates whether this plan can be used for a given plot
         * specification.
         *
         * @param   level  HEALPix level giving sky pixel resolution
         * @param   combiner  value combination mode
         * @param   dataSpec  input data specification
         * @param   geom    sky geometry
         */
        public boolean matches( int level, Combiner combiner,
                                DataSpec dataSpec, SkyDataGeom geom ) {
             return level_ == level
                 && combiner_.equals( combiner )
                 && dataSpec_.equals( dataSpec )
                 && geom_.equals( geom );
        }

        /**
         * Adjusts a supplied Ranger object to reflect the pixel value range
         * represented by this plan over
         * only that part of the sky visible in a given sky surface.
         * This may not be very fast, but it scales with the number of
         * pixels on the surface rather than the number of data rows,
         * so it's not too slow either.
         * However in some cases (bin-count&lt;&lt;pixel-count)
         * it could definitely be done more efficiently.
         *
         * @param  ranger     ranger to submit values to
         * @param  surface   viewing surface
         * @param  binFactor   factor for multiplying bin result values
         */
        public void extendVisibleRange( Ranger ranger, SkySurface surface,
                                        double binFactor ) {
            Rectangle bounds = surface.getPlotBounds();
            int nx = bounds.width;
            int ny = bounds.height;
            Gridder gridder = new Gridder( nx, ny );
            int npix = gridder.getLength();
            Point2D.Double point = new Point2D.Double();
            double x0 = bounds.x + 0.5;
            double y0 = bounds.y + 0.5;
            SkyPixer skyPixer = new SkyPixer( level_ );
            for ( int ip = 0; ip < npix; ip++ ) {
                point.x = x0 + gridder.getX( ip );
                point.y = y0 + gridder.getY( ip );
                double[] dpos = surface.graphicsToData( point, null );
                if ( dpos != null ) {
                    double dval =
                        binFactor *
                        binResult_.getBinValue( skyPixer.getIndex( dpos ) );
                    ranger.submitDatum( dval );
                }
            }
        }
    }
}
