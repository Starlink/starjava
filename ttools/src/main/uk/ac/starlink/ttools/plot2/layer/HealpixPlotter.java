package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Icon;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.cone.HealpixTiling;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.gui.ResourceIcon;
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
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ComboBoxSpecifier;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.HealpixDataGeom;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter for plotting lists of HEALPix tiles.
 *
 * @author   Mark Taylor
 * @since    31 Mar 2016
 */
public class HealpixPlotter
        extends AbstractPlotter<HealpixPlotter.HealpixStyle> {

    private final boolean transparent_;
    private final boolean reportAuxKeys_;
    private final int icHealpix_;
    private final int icValue_;

    /** Currently always works with HEALPix NESTED scheme. */
    public static final boolean IS_NEST = true;

    /** Maximum HEALPix level supported by this plotter. */
    public static final int MAX_LEVEL = HealpixTiling.MAX_LEVEL;

    /** Coordinate for value determining tile colours. */
    public static final FloatingCoord VALUE_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "value", "Value" )
           .setShortDescription( "Tile value" )
           .setXmlDescription( new String[] {
                "<p>Value of HEALPix tile, determining the colour",
                "which will be plotted.",
                "</p>",
            } )
        , true );

    /** ConfigKey for HEALPix level corresponding to data HEALPix indices. */
    public static final ConfigKey<Integer> DATALEVEL_KEY = createDataLevelKey();

    /** ConfigKey for Sky System corresponding to data HEALPix indices. */
    public static final ConfigKey<SkySys> DATASYS_KEY =
        new SkySysConfigKey(
            new ConfigMeta( "datasys", "Data Sky System" )
           .setShortDescription( "Sky system of HEALPix grid" )
           .setXmlDescription( new String[] {
                "<p>The sky coordinate system to which the HEALPix grid",
                "used by the input pixel file refers.",
                "</p>",
            } )
        , false, true );


    /** Config key for scaling angle unit. */
    public static final ConfigKey<SolidAngleUnit> ANGLE_KEY =
        SkyDensityPlotter.ANGLE_KEY;

    /** Config key for HEALPix level degradation. */
    public static final ConfigKey<Integer> DEGRADE_KEY;

    /** Config key for degrade combination mode. */
    public static final ConfigKey<Combiner> COMBINER_KEY;

    /** Config key for transparency. */
    private static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;

    private static final AuxScale SCALE = AuxScale.COLOR;

    static {
        ConfigMeta degradeMeta = new ConfigMeta( "degrade", "Degrade" );
        ConfigMeta combineMeta = new ConfigMeta( "combine", "Combine" );
        degradeMeta.setShortDescription( "HEALPix level degradation" );
        degradeMeta.setXmlDescription( new String[] {
            "<p>Allows the HEALPix grid to be drawn at a less detailed",
            "level than the level at which the input data are supplied.",
            "A value of zero (the default) means that the HEALPix tiles",
            "are painted with the same resolution as the input data,",
            "but a higher value will degrade resolution of the plot tiles;",
            "each plotted tile will correspond to",
            "4^<code>" + degradeMeta.getShortName() + "</code> input tiles.",
            "The way that values are combined within each painted tile",
            "is controlled by the",
            "<code>" + combineMeta.getShortName() + "</code> value.",
            "</p>"
        } );
        combineMeta.setShortDescription( "Tile degrade combination mode" );
        combineMeta.setXmlDescription( new String[] {
            "<p>Defines how values degraded to a lower HEALPix level",
            "are combined together to produce the value assigned to the",
            "larger tile, and hence its colour.",
            "This is mostly useful in the case that",
            "<code>" + degradeMeta.getShortName() + "</code>&gt;0.",
            "</p>",
            "<p>For density-like values",
            "(<code>" + Combiner.DENSITY + "</code>,",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code>)",
            "the scaling is additionally influenced by the",
            "<code>" + ANGLE_KEY.getMeta().getShortName() + "</code>",
            "parameter.",
            "</p>",
        } );
        DEGRADE_KEY =
            IntegerConfigKey.createSpinnerKey( degradeMeta, 0, 0, MAX_LEVEL );
        COMBINER_KEY =
                new OptionConfigKey<Combiner>( combineMeta, Combiner.class,
                                               Combiner.getKnownCombiners(),
                                               Combiner.MEAN ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        }.setOptionUsage()
         .addOptionsXml();
    }

    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     */
    public HealpixPlotter( boolean transparent ) {
        super( "Healpix", ResourceIcon.PLOT_HEALPIX,
               CoordGroup.createCoordGroup( 1, new Coord[] { VALUE_COORD } ),
               false );
        icHealpix_ = 0;
        icValue_ = 1;
        transparent_ = transparent;
        reportAuxKeys_ = false;
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots a table representing HEALPix pixels " )
            .append( "on the sky.\n" )
            .append( "Each row represents a single HEALPix tile,\n" )
            .append( "and a value from that row is used to colour\n" )
            .append( "the corresponding region of the sky plot.\n" )
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
        keyList.add( DATALEVEL_KEY );
        keyList.add( DATASYS_KEY );
        keyList.add( DEGRADE_KEY );
        keyList.add( COMBINER_KEY );
        keyList.add( ANGLE_KEY );
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public HealpixStyle createStyle( ConfigMap config ) {

        /* Acquire config items from global config.
         * Note that the plotting UI (STILTS or TOPCAT) has to make
         * special arrangements to ensure that VIEWSYS_KEY is included
         * in the config map, since it is a global value for the plot,
         * not specified per layer, and therefore is not declared
         * as one of this plotter's style keys. */
        SkySys viewSys = config.get( SkySurfaceFactory.VIEWSYS_KEY );
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int dataLevel = config.get( DATALEVEL_KEY );
        SkySys dataSys = config.get( DATASYS_KEY );
        int degrade = config.get( DEGRADE_KEY );
        Combiner combiner = config.get( COMBINER_KEY );
        SolidAngleUnit angle = config.get( ANGLE_KEY );
        Rotation rotation = Rotation.createRotation( dataSys, viewSys );
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new HealpixStyle( dataLevel, degrade, rotation, scaling,
                                 dataclip, shader, combiner, angle );
    }

    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  HealpixStyle style ) {
        if ( ! (geom instanceof HealpixDataGeom) ) {
            throw new IllegalArgumentException( "DataGeom not Healpix: "
                                              + geom );
        }
        StarTable table = dataSpec.getSourceTable();
        List<DescribedValue> tparams = table.getParameters();
        HealpixTableInfo hpxInfo = HealpixTableInfo.isHealpix( tparams )
                                 ? HealpixTableInfo.fromParams( tparams )
                                 : null;
        int dataLevel = -1;
        if ( dataLevel < 0 && style.dataLevel_ >= 0 ) {
            dataLevel = style.dataLevel_;
        }
        if ( dataLevel < 0 && hpxInfo != null ) {
            dataLevel = hpxInfo.getLevel();
        }
        if ( dataLevel < 0 ) {
            dataLevel = guessDataLevel( table.getRowCount() );
        }
        if ( dataLevel >= 0 ) {
            final int nside = 1 << dataLevel;
            IndexReader rdr =
                  dataSpec.isCoordBlank( icHealpix_ )
                ? new IndexReader() {
                      public long getHealpixIndex( Tuple tuple ) {
                          return tuple.getRowIndex();
                      }
                  }
                : new IndexReader() {
                      public long getHealpixIndex( Tuple tuple ) {
                          return HealpixDataGeom.HEALPIX_COORD
                                .readLongCoord( tuple, icHealpix_ );
                      }
                  };
            return new BinsHealpixLayer( geom, dataSpec, style,
                                         dataLevel, rdr );
        }

        /* Can't determine or guess HEALPix level.
         * We have no choice but to refuse to plot.
         * Unfortunately this doesn't give much useful user feedback. */
        else {
            return null;
        }
    }

    /**
     * Attempts to guess the HEALPix level given a row count.
     * If a reasonable stab can be made at the answer, it is returned.
     * If we have no idea, -1 is returned.
     *
     * @param   nrow   row count; negative if not known
     * @return   probable healpix level, or -1 if no idea
     */
    private static int guessDataLevel( long nrow ) {
        if ( nrow > 0 ) {
            for ( int il = 0; il <= MAX_LEVEL; il++ ) {
                long hprow = 12 * ( 1L << ( 2 * il ) );

                /* If there are the same number of rows as healpix pixels,
                 * or the same plus an extra row for a blank index,
                 * or nearly enough (a few blank ones), guess it's right. */
                if ( nrow == hprow ||
                     nrow == hprow + 1 ||
                     ( nrow <= hprow && nrow >= 0.95 * hprow ) ) {
                    return il;
                }
            }
        }
        return -1;
    }

    /**
     * Constructs the config key for supplying HEALPix level at which
     * index coordinate values must be interpreted.
     *
     * @return  HEALPix data level key
     */
    private static ConfigKey<Integer> createDataLevelKey() {
        ConfigMeta meta = new ConfigMeta( "datalevel", "HEALPix Data Level" );
        meta.setShortDescription( "HEALPix level of tile index" );
        meta.setXmlDescription( new String[] {
            "<p>HEALPix level of the (implicit or explicit) tile indices.",
            "Legal values are between 0 (12 pixels) and",
            Integer.toString( MAX_LEVEL ),
            "(" + Long.toString( 12 * (long) Math.pow( 4, MAX_LEVEL ) )
                + " pixels).",
            "If a negative value is supplied (the default),",
            "then an attempt is made to determine the correct level",
            "from the data.",
            "</p>",
        } );
        final Collection<Integer> levelOptions = new ArrayList<Integer>();
        levelOptions.add( new Integer( -1 ) );
        for ( int i = 0; i <= MAX_LEVEL; i++ ) {
            levelOptions.add( new Integer( i ) );
        }
        ConfigKey<Integer> key = new IntegerConfigKey( meta, -1 ) {
            public Specifier<Integer> createSpecifier() {
                return new ComboBoxSpecifier<Integer>( Integer.class,
                                                       levelOptions );
            }
        };
        return key;
    }

    /**
     * Style for configuring the HEALPix plot.
     */
    public static class HealpixStyle implements Style {
        private final int dataLevel_;
        private final int degrade_;
        private final Rotation rotation_;
        private final Scaling scaling_;
        private final Subrange dataclip_;
        private final Shader shader_;
        private final Combiner combiner_;
        private final SolidAngleUnit angle_;

        /**
         * Constructor.
         *
         * @param   dataLevel HEALPix level at which the pixel index coordinates
         *                    must be interpreted; if negative, automatic
         *                    detection will be used
         * @param   degrade   HEALPix levels by which to degrade view grid
         * @param   rotation  sky rotation to be applied before plotting
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   dataclip  scaling input range adjustment
         * @param   shader   colour map
         * @param   combiner  combiner, only relevant if degrade is non-zero
         * @param   angle      solid angle configuration for scaling
         */
        public HealpixStyle( int dataLevel, int degrade, Rotation rotation,
                             Scaling scaling, Subrange dataclip, Shader shader,
                             Combiner combiner, SolidAngleUnit angle ) {
            dataLevel_ = dataLevel;
            degrade_ = degrade;
            rotation_ = rotation;
            scaling_ = scaling;
            dataclip_ = dataclip;
            shader_ = shader;
            combiner_ = combiner;
            angle_ = angle;
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
            return createHealpixIcon( shader_, 18, 16, 1, 1 );
        }

        @Override
        public int hashCode() {
            int code = 553227;
            code = 23 * code + dataLevel_;
            code = 23 * code + degrade_;
            code = 23 * code + rotation_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + combiner_.hashCode();
            code = 23 * code + angle_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HealpixStyle ) {
                HealpixStyle other = (HealpixStyle) o;
                return this.dataLevel_ == other.dataLevel_
                    && this.degrade_ == other.degrade_
                    && this.rotation_.equals( other.rotation_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && this.shader_.equals( other.shader_ )
                    && this.combiner_.equals( other.combiner_ )
                    && this.angle_.equals( other.angle_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for plotting layers that work with bin lists.
     * A whole HEALPix bin list is calculated from the input data (as a plan).
     */
    private class BinsHealpixLayer extends AbstractPlotLayer {

        private final HealpixStyle hstyle_;
        private final int dataLevel_;
        private final int viewLevel_;
        private final IndexReader indexReader_;

        /**
         * Constructor.
         *
         * @param  geom   data geom
         * @param  dataSpec   data specification
         * @param  hstyle   style
         * @param  dataLevel   definite HEALPix level of data tiles
         * @param  indexReader   determines pixel index from data
         */
        BinsHealpixLayer( DataGeom geom, DataSpec dataSpec, HealpixStyle hstyle,
                          int dataLevel, IndexReader indexReader ) {
            super( HealpixPlotter.this, geom, dataSpec, hstyle,
                   hstyle.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL );
            hstyle_ = hstyle;
            dataLevel_ = dataLevel;
            indexReader_ = indexReader;
            viewLevel_ = Math.max( 0, dataLevel_ - hstyle_.degrade_ );
            assert hstyle.degrade_ >= 0;
        }

        @Override
        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icValue_;
                }
                public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                    return getCombinedInfo( dataSpec );
                }
                public Scaling getScaling() {
                    return hstyle_.scaling_;
                }
                public void adjustAuxRange( Surface surface, DataSpec dataSpec,
                                            DataStore dataStore,
                                            Object[] knownPlans,
                                            Ranger ranger ) {
                    TilePlan tplan = getTilePlan( knownPlans );
                    BinList.Result binResult =
                        tplan == null ? readBins( dataSpec, dataStore )
                                      : tplan.binResult_;
                    createTileRenderer( surface )
                   .extendAuxRange( ranger, binResult );
                }
            } );
            return map;
        }

        public Drawing createDrawing( Surface surf,
                                      Map<AuxScale,Span> auxSpans,
                                      final PaperType paperType ) {
            final DataSpec dataSpec = getDataSpec();
            final Combiner combiner = hstyle_.combiner_;
            final Shader shader = hstyle_.shader_;
            final Scaler scaler =
                auxSpans.get( SCALE )
                        .createScaler( hstyle_.scaling_, hstyle_.dataclip_ );
            final SkyTileRenderer renderer = createTileRenderer( surf );
            return new Drawing() {
                public Object calculatePlan( Object[] knownPlans,
                                             DataStore dataStore ) {
                    TilePlan knownPlan = getTilePlan( knownPlans );
                    if ( knownPlan != null ) {
                        return knownPlan;
                    }
                    else {
                        BinList.Result binResult =
                            readBins( dataSpec, dataStore ).compact();
                        return new TilePlan( dataLevel_, viewLevel_, combiner,
                                             dataSpec, binResult );
                    }
                }
                public void paintData( Object plan, Paper paper,
                                       DataStore dataStore ) {
                    final BinList.Result binResult =
                        ((TilePlan) plan).binResult_;
                    paperType.placeDecal( paper, new Decal() {
                        public void paintDecal( Graphics g ) {
                            renderer.renderBins( g, binResult, shader, scaler );
                        }
                        public boolean isOpaque() {
                            return hstyle_.isOpaque();
                        }
                    } );
                }
                public ReportMap getReport( Object plan ) {
                    return null;
                }
            };
        }

        /**
         * Returns a SkyTileRenderer instance suitable for use with
         * this layer and a given surface.
         *
         * @param  surf  sky surface
         * @return  tile renderer
         */
        private SkyTileRenderer createTileRenderer( Surface surf ) {
            double binExtent = Tilings.healpixSqdeg( viewLevel_ )
                             / hstyle_.angle_.getExtentInSquareDegrees();
            Combiner.Type ctype = hstyle_.combiner_.getType();
            double binFactor = ctype.getBinFactor( binExtent );
            return SkyTileRenderer
                  .createRenderer( (SkySurface) surf, hstyle_.rotation_,
                                   viewLevel_, binFactor );
        }

        /** 
         * Identifies and retrieves a tile plan that can be used for
         * this layer from a list of precalculated plans.
         * If none of the supplied plans is suitable, null is returned.
         *
         * @param  knownPlans  available pre-calculated plans
         * @return   suitable tile plan from supplied list, or null
         */
        private TilePlan getTilePlan( Object[] knownPlans ) {
            DataSpec dataSpec = getDataSpec();
            for ( Object plan : knownPlans ) {
                if ( plan instanceof TilePlan ) {
                    TilePlan tplan = (TilePlan) plan;
                    if ( tplan.matches( dataLevel_, viewLevel_,
                                        hstyle_.combiner_, dataSpec ) ) {
                        return tplan;
                    }
                }
            }
            return null;
        }

        /**
         * Constructs and populates a bin list (tile index -&gt; value map)
         * suitable for plotting this layer.
         *
         * @param   dataSpec   data specification
         * @param   dataStore  data storage
         * @return   value map
         */
        private BinList.Result readBins( DataSpec dataSpec,
                                         DataStore dataStore ) {
            int degrade = dataLevel_ - viewLevel_;
            assert degrade >= 0;
            final int shift = degrade * 2;
            long dataNpix = 12 * ( 1L << ( 2 * dataLevel_ ) );
            long viewNbin = 12 * ( 1L << ( 2 * viewLevel_ ) );
            Combiner combiner = hstyle_.combiner_;
            BinListCollector collector =
                    new BinListCollector( combiner, viewNbin ) {
                public void accumulate( TupleSequence tseq, BinList binList ) {
                    while ( tseq.next() ) {
                        double value = tseq.getDoubleValue( icValue_ );
                        if ( ! Double.isNaN( value ) ) {
                            long hpx = indexReader_.getHealpixIndex( tseq );
                            if ( hpx >= 0 && hpx < dataNpix ) {
                                long ibin = hpx >> shift;
                                binList.submitToBin( ibin, value );
                            }
                        }
                    }
                }
            };

            /* Although the code is set up to assemble the bin list in
             * parallel, in some cases it's better to do it sequentially
             * instead.  The reason is that although the data scanning
             * is accelerated by parallel processing, the (current) BinList
             * implementations have data structure sizes which scale
             * with thread count as well as with tile count (each thread
             * has its own accumulator of a size scaling with 4^viewLevel).
             * The additional memory required can exceed JVM heap limits,
             * and its usage can also slow things down more than offsetting
             * the gain from multithreading (e.g. managing bin insertion
             * in multiple parallel hashmaps).  A sequential scan is usually
             * not all that slow in any case at reasonable dataLevels.
             * So if memory usage is in danger of getting out of hand,
             * stick to sequential processing.
             * This policy is obviously a bit ad hoc.  In its current form
             * it was tested using a level=11 Healpix all-sky file with a
             * 4Gb or 8Gb heap, for which parallel processing grinds to a
             * halt but sequential processing works fine. */
            final boolean forceSequential = viewLevel_ > 9
                                         || combiner.hasBigBin();
            return ( forceSequential ? TupleRunner.SEQUENTIAL
                                     : dataStore.getTupleRunner() )
                  .collect( collector,
                            () -> dataStore.getTupleSequence( dataSpec ) )
                  .getResult();
        }

        /**
         * Returns the metadata for the tile values.
         *
         * @param  dataSpec  data specification
         * @return   metadata for tile values
         */
        private ValueInfo getCombinedInfo( DataSpec dataSpec ) {
            final ValueInfo weightInfo;
            if ( icValue_ < 0 || dataSpec.isCoordBlank( icValue_ ) ) {
                weightInfo = new DefaultValueInfo( "1", Double.class,
                                                   "Weight unspecified"
                                                 + ", taken as unity" );
            }
            else {
                ValueInfo[] winfos =
                    dataSpec.getUserCoordInfos( icValue_ );
                weightInfo = winfos != null && winfos.length == 1
                           ? winfos[ 0 ]
                           : new DefaultValueInfo( "Weight", Double.class );
            }
            return hstyle_.combiner_
                  .createCombinedInfo( weightInfo, hstyle_.angle_ );
        }
    }

    /**
     * Plot layer plan for use with HealpixLayer.
     */
    private static class TilePlan {
        final int dataLevel_;
        final int viewLevel_;
        final Combiner combiner_;
        final DataSpec dataSpec_;
        final BinList.Result binResult_;

        /**
         * Constructor.
         *
         * @param  dataLevel  HEALPix level at which data was supplied
         * @param  viewLevel  HEALPix level at which pixels were calculated
         * @param  combiner   combination method
         * @param  dataSpec   data spec
         * @param  binResult   tile map
         */
        TilePlan( int dataLevel, int viewLevel, Combiner combiner,
                  DataSpec dataSpec, BinList.Result binResult ) {
            dataLevel_ = dataLevel;
            viewLevel_ = viewLevel;
            combiner_ = combiner;
            dataSpec_ = dataSpec;
            binResult_ = binResult;
        }

        /**
         * Indicates whether this plan can be used for a given plot
         * specification.
         *
         * @param  dataLevel  HEALPix level at which data is supplied
         * @param  viewLevel  HEALPix level at which pixels is calculated
         * @param  combiner   combination method for degraded pixels
         * @param  dataSpec   data spec
         * @return  true iff this plan can be used for the given parameters
         */
        public boolean matches( int dataLevel, int viewLevel, Combiner combiner,
                                DataSpec dataSpec ) {
            return dataLevel_ == dataLevel
                && viewLevel_ == viewLevel
                && combiner_ == combiner
                && dataSpec_.equals( dataSpec );
        }
    }

    /**
     * Returns an icon suitable for use in a legend that represents
     * painting HEALPix tiles.
     *
     * @param  shader   shader
     * @param  width    total icon width in pixels
     * @param  height   total icon height in pixels
     * @param  xpad     internal padding in the X direction in pixels
     * @param  ypad     internal padding in the Y direction in pixels
     * @return   legend icon
     */
    private static Icon createHealpixIcon( final Shader shader,
                                           final int width, final int height,
                                           final int xpad, final int ypad ) {
        final double xd = ( width - 2 * xpad ) * 0.25;
        final double yd = ( width - 2 * ypad ) * 0.25;
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                int xoff = x + xpad;
                int yoff = y + ypad;
                g.translate( xoff, yoff );
                Color color0 = g.getColor();
                paintDiamond( g, 1./8., 2, 0 );
                paintDiamond( g, 5./8., 1, 1 );
                paintDiamond( g, 7./8., 3, 1 );
                paintDiamond( g, 3./8., 2, 2 );
                g.setColor( color0 );
                g.translate( -xoff, -yoff );
            }
            private void paintDiamond( Graphics g, double shade,
                                       int ix, int iy ) {
                float[] rgba = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
                shader.adjustRgba( rgba, (float) shade );
                g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                       rgba[ 2 ], rgba[ 3 ] ) );
                int[] xs = new int[] {
                    (int) xd * ix,
                    (int) xd * ( ix - 1 ),
                    (int) xd * ix,
                    (int) xd * ( ix + 1 ),
                };
                int[] ys = new int[] {
                    (int) yd * iy,
                    (int) yd * ( iy + 1 ),
                    (int) yd * ( iy + 2 ),
                    (int) yd * ( iy + 1 ),
                };
                g.fillPolygon( xs, ys, 4 );
            }
        };
    }

    /**
     * Defines how pixel index is acquired from a tuple sequence.
     */
    private interface IndexReader {

        /**
         * Acquires the HEALPix index corresponding to a tuple.
         *
         * @param  tuple  tuple
         * @return  healpix index
         */
        long getHealpixIndex( Tuple tuple );
    }
}
