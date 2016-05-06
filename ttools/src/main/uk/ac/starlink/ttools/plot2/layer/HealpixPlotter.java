package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
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
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
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
import uk.ac.starlink.ttools.plot2.data.IntegerCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
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

    /** Maximum HEALPix level supported by this plotter. */
    public static final int MAX_LEVEL = 13;

    /** Coordinate for HEALPix index. */
    public static final IntegerCoord HEALPIX_COORD =
        new IntegerCoord(
            new InputMeta( "healpix", "HEALPix index" )
           .setShortDescription( "HEALPix index" )
           .setXmlDescription( new String[] {
                "<p>HEALPix index indicating the sky position of the tile",
                "whose value is plotted.",
                "If not supplied, the assumption is that the supplied table",
                "contains one row for each HEALPix tile at a given level,",
                "in ascending order.",
                "</p>",
            } )
        , false, IntegerCoord.IntType.INT );

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
        , false );

    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final String BLUR_NAME = "blur";
    private static final String COMBINER_NAME = "combiner";
    private static final ConfigKey<Integer> BLUR_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "blur", "Pixel Blur" )
           .setShortDescription( "HEALPix level degradation" )
           .setXmlDescription( new String[] {
                "<p>Allows the HEALPix grid to be drawn at a less detailed",
                "level than the level at which the input data are supplied.",
                "A value of zero (the default) means that the HEALPix tiles",
                "are painted with the same resolution as the input data,",
                "but a higher value will degrade resolution of the plot tiles;",
                "each plotted tile will correspond to",
                "4^<code>" + BLUR_NAME + "</code> input tiles.",
                "The way that values are combined within each painted tile",
                "is controlled by the",
                "<code>" + COMBINER_NAME + "</code> value.",
                "</p>"
            } )
        , 0, 0, MAX_LEVEL );
    private static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<SkySys> VIEWSYS_KEY =
        SkySurfaceFactory.VIEWSYS_KEY;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /** ConfigKey for blurring combiner. */
    public static final ConfigKey<Combiner> COMBINER_KEY = createCombinerKey();

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     */
    public HealpixPlotter( boolean transparent ) {
        super( "Healpix", ResourceIcon.FORM_SKYDENSITY,
               CoordGroup.createCoordGroup( 0, new Coord[] { HEALPIX_COORD,
                                                             VALUE_COORD } ),
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

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.add( DATALEVEL_KEY );
        keyList.add( DATASYS_KEY );
        keyList.add( VIEWSYS_KEY );
        keyList.add( BLUR_KEY );
        keyList.add( COMBINER_KEY );
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    public HealpixStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int dataLevel = config.get( DATALEVEL_KEY );
        SkySys dataSys = config.get( DATASYS_KEY );
        SkySys viewSys = config.get( VIEWSYS_KEY );
        int blur = config.get( BLUR_KEY );
        Combiner combiner = config.get( COMBINER_KEY );
        Rotation rotation = Rotation.createRotation( dataSys, viewSys );
        Scaling scaling = ramp.getScaling();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new HealpixStyle( dataLevel, blur, rotation, scaling, shader,
                                 combiner );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  HealpixStyle style ) {
        int dataLevel = style.dataLevel_ >= 0
                  ? style.dataLevel_
                  : guessDataLevel( dataSpec.getSourceTable().getRowCount() );
        if ( dataLevel >= 0 ) {
            IndexReader rdr =
                  dataSpec.isCoordBlank( icHealpix_ )
                ? new IndexReader() {
                      public long getHealpixIndex( TupleSequence tseq ) {
                          return tseq.getRowIndex();
                      }
                  }
                : new IndexReader() {
                      public long getHealpixIndex( TupleSequence tseq ) {
                          return HEALPIX_COORD.readIntCoord( tseq, icHealpix_ );
                      }
                  };
            return style.blur_ == 0 && dataLevel <= 6
                 ? new SequenceHealpixLayer( geom, dataSpec, style,
                                             dataLevel, rdr )
                 : new BinsHealpixLayer( geom, dataSpec, style,
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
                long hprow = 12 * 1 << ( 2 * il );

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
                return new ComboBoxSpecifier( levelOptions );
            }
        };
        return key;
    }

    /**
     * Constructs the config key for configuring the Combiner object
     * used when blurring pixels.
     *
     * @return   combiner key
     */
    private static ConfigKey<Combiner> createCombinerKey() {
        ConfigMeta meta = new ConfigMeta( COMBINER_NAME, "Combine");
        meta.setShortDescription( "Pixel blur combination mode" );
        meta.setXmlDescription( new String[] {
            "<p>Defines how pixel values will be combined if they are",
            "blurred to a lower resolution than the data HEALPix level.",
            "This only has any effect if",
            "<code>" + BLUR_KEY + "</code>&gt;0.",
            "</p>",
        } );
        Combiner[] options = new Combiner[] {
            Combiner.SUM,
            Combiner.MEAN,
            Combiner.MIN,
            Combiner.MAX,
        };
        return new OptionConfigKey<Combiner>( meta, Combiner.class, options,
                                              Combiner.SUM ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        };
    }

    /**
     * Constructs a bin list for a given combiner, given also the data
     * HEALPix level and the blurring factor.
     * Note that the combination semantics of some of the combiners
     * (those representing intensive, rather than extensive, quantities)
     * is somewhat changed by the context in which they are used here;
     * the submission count is implicitly that of the number of HEALPix
     * subpixels corresponding to the blur, rather than just the number
     * of data values actually submitted.
     *
     * @param  combiner  basic combiner
     * @param  dataLevel   HEALPix level at which data is supplied
     * @param  viewLevel   HEALPix level at which pixels are to be calculated
     * @return  bin list for accumulating pixel values
     */
    private static BinList createBinList( final Combiner combiner,
                                          int dataLevel, int viewLevel ) {
        int blur = dataLevel - viewLevel;
        long nbin = 12 * ( 1 << ( 2 * viewLevel ) );
        boolean isFew = nbin < 1e6;
        if ( Combiner.MEAN.equals( combiner ) ) {
            final double factor = 1.0 / ( 1 << ( 2 * blur ) );
            final BinList baseList =
                isFew ? Combiner.SUM.createArrayBinList( (int) nbin )
                      : Combiner.SUM.createHashBinList( nbin );
            return new BinList() {
                public Combiner getCombiner() {
                    return combiner;
                }
                public long getSize() {
                    return baseList.getSize();
                }
                public void submitToBin( long index, double datum ) {
                    baseList.submitToBin( index, datum );
                }
                public BinList.Result getResult() {
                    final BinList.Result baseResult = baseList.getResult();
                    return new BinList.Result() {
                        public double getBinValue( long index ) {
                            return factor * baseResult.getBinValue( index );
                        }
                        public double[] getValueBounds() {
                            double[] bounds =
                                baseResult.getValueBounds().clone();
                            bounds[ 0 ] *= factor;
                            bounds[ 1 ] *= factor;
                            return bounds;
                        }
                    };
                }
            };
        }
        else { 
            if ( ! Arrays.asList( new Combiner[] {
                       Combiner.SUM, Combiner.MIN, Combiner.MAX,
                   } ).contains( combiner ) ) {
                logger_.warning( "Unexpected combiner: " + combiner );
            }
            return isFew ? combiner.createArrayBinList( (int) nbin )
                         : combiner.createHashBinList( nbin );
        }
    }

    /**
     * Style for configuring the HEALPix plot.
     */
    public static class HealpixStyle implements Style {
        private final int dataLevel_;
        private final int blur_;
        private final Rotation rotation_;
        private final Scaling scaling_;
        private final Shader shader_;
        private final Combiner combiner_;

        /**
         * Constructor.
         *
         * @param   dataLevel HEALPix level at which the pixel index coordinates
         *                    must be interpreted; if negative, automatic
         *                    detection will be used
         * @param   blur      HEALPix levels by which to degrade view grid
         * @param   rotation  sky rotation to be applied before plotting
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   shader   colour map
         * @param   combiner  combiner, only relevant if blur is non-zero
         */
        public HealpixStyle( int dataLevel, int blur, Rotation rotation,
                             Scaling scaling, Shader shader,
                             Combiner combiner ) {
            dataLevel_ = dataLevel;
            blur_ = blur;
            rotation_ = rotation;
            scaling_ = scaling;
            shader_ = shader;
            combiner_ = combiner;
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
            code = 23 * code + blur_;
            code = 23 * code + rotation_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + shader_.hashCode();
            code = 23 * code + ( blur_ == 0 ? 0 : combiner_.hashCode() );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HealpixStyle ) {
                HealpixStyle other = (HealpixStyle) o;
                return this.dataLevel_ == other.dataLevel_
                    && this.blur_ == other.blur_
                    && this.rotation_.equals( other.rotation_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.shader_.equals( other.shader_ )
                    && ( blur_ == 0 ||
                         this.combiner_.equals( other.combiner_ ) );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation that goes through all the tiles in the input,
     * and paints a tile for each one.  Only works for blur=0 (no level
     * degradation).
     */
    private class SequenceHealpixLayer extends AbstractPlotLayer {

        private final HealpixStyle hstyle_;
        private final int dataLevel_;
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
        SequenceHealpixLayer( DataGeom geom, DataSpec dataSpec,
                              HealpixStyle hstyle, int dataLevel,
                              IndexReader indexReader ) {
            super( HealpixPlotter.this, geom, dataSpec, hstyle,
                   hstyle.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL );
            hstyle_ = hstyle;
            dataLevel_ = dataLevel;
            indexReader_ = indexReader;
            assert hstyle_.blur_ == 0;
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icValue_;
                }
                public void adjustAuxRange( Surface surface, TupleSequence tseq,
                                            Range range ) {
                    SkySurfaceTiler tiler = createTiler( (SkySurface) surface );
                    while ( tseq.next() ) {
                        long hpx = indexReader_.getHealpixIndex( tseq );
                        if ( tiler.isCenterVisible( hpx ) ) {
                            double value =
                                VALUE_COORD.readDoubleCoord( tseq, icValue_ );
                            range.submit( value );
                        }
                    }
                }
            } );
            return map;
        }

        public Drawing createDrawing( Surface surf,
                                      Map<AuxScale,Range> auxRanges,
                                      final PaperType paperType ) {
            final SkySurface ssurf = (SkySurface) surf;
            final DataSpec dataSpec = getDataSpec();
            final Shader shader = hstyle_.shader_;
            final Scaler scaler =
                Scaling.createRangeScaler( hstyle_.scaling_,
                                           auxRanges.get( SCALE ) );
            final SkySurfaceTiler tiler = createTiler( ssurf );
            return new UnplannedDrawing() {
                protected void paintData( Paper paper,
                                          final DataStore dataStore ) {
                    paperType.placeDecal( paper, new Decal() {
                        public void paintDecal( Graphics g ) {
                            TupleSequence tseq =
                                dataStore.getTupleSequence( dataSpec );
                            paintTiles( g, tseq, tiler, scaler, shader );
                        }
                        public boolean isOpaque() {
                            return hstyle_.isOpaque();
                        }
                    } );
                }
            };
        }

        /**
         * Returns a SkySurfaceTiler for a given sky surface.
         *
         * @param  surf  sky surface
         * @retrun   tiler
         */
        private SkySurfaceTiler createTiler( SkySurface surf ) {
            return new SkySurfaceTiler( surf, dataLevel_, hstyle_.rotation_ );
        }

        /**
         * Paints HEALPix pixels on a graphics context.
         *
         * @param  g   graphics context
         * @param  tseq  tuple sequence yielding pixel indices and data values
         * @param  tiler  handles HEALPix tile geometry on the plotting surface
         * @param  scaler  scales data values to unit interval
         * @param  shader  determines colours from unit interval
         */
        private void paintTiles( Graphics g, TupleSequence tseq,
                                 SkySurfaceTiler tiler,
                                 Scaler scaler, Shader shader ) {
            Color color0 = g.getColor();
            float[] rgba = new float[ 4 ];
            while ( tseq.next() ) {
                double value = tseq.getDoubleValue( icValue_ );
                if ( ! Double.isNaN( value ) ) {
                    long hpx = indexReader_.getHealpixIndex( tseq );
                    Shape shape = tiler.getTileShape( hpx );
                    if ( shape != null ) {
                        rgba[ 0 ] = 0.5f;
                        rgba[ 1 ] = 0.5f;
                        rgba[ 2 ] = 0.5f;
                        rgba[ 3 ] = 1.0f;
                        float sval = (float) scaler.scaleValue( value );
                        shader.adjustRgba( rgba, sval );
                        g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                               rgba[ 2 ], rgba[ 3 ] ) );
                        tiler.fillTile( g, shape );
                    }
                }
            }
            g.setColor( color0 );
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
        private final SkyPixer skyPixer_;
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
            viewLevel_ = Math.max( 0, dataLevel_ - hstyle.blur_ );
            final Rotation unrotation = hstyle_.rotation_.invert();
            skyPixer_ = new SkyPixer( viewLevel_ ) {
                public long getIndex( double[] v3 ) {
                    unrotation.rotate( v3 );
                    return super.getIndex( v3 );
                }
            };
            assert hstyle.blur_ >= 0;
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icValue_;
                }
                public void adjustAuxRange( Surface surface, TupleSequence tseq,
                                            Range range ) {
                    // this is nasty because I need to iterate over the data
                    // to get the aux range; the plan has that information,
                    // (which means the actual painting doesn't need the data),
                    // but I can't see it here.
                    double[] bounds = createTileRenderer( surface )
                                     .calculateAuxRange( readBins( tseq ) )
                                     .getBounds();
                    range.submit( bounds[ 0 ] );
                    range.submit( bounds[ 1 ] );
                }
            } );
            return map;
        }

        public Drawing createDrawing( Surface surf,
                                      Map<AuxScale,Range> auxRanges,
                                      final PaperType paperType ) {
            final DataSpec dataSpec = getDataSpec();
            final Shader shader = hstyle_.shader_;
            final Scaler scaler =
                Scaling.createRangeScaler( hstyle_.scaling_,
                                           auxRanges.get( SCALE ) );
            final TileRenderer renderer = createTileRenderer( surf );
            return new Drawing() {
                public Object calculatePlan( Object[] knownPlans,
                                             DataStore dataStore ) {
                    for ( Object plan : knownPlans ) {
                        if ( plan instanceof TilePlan ) {
                            TilePlan tplan = (TilePlan) plan;
                            if ( tplan.matches( dataLevel_, viewLevel_,
                                                dataSpec ) ) {
                                return tplan;
                            }
                        }
                    }
                    BinList.Result binResult =
                        readBins( dataStore.getTupleSequence( dataSpec ) );
                    return new TilePlan( dataLevel_, viewLevel_, dataSpec,
                                         binResult );
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
         * Constructs and populates a bin list (tile index -&gt; value map)
         * suitable for plotting this layer.
         *
         * @param  tseq   row iterator
         * @return   value map
         */
        private BinList.Result readBins( TupleSequence tseq ) {
            int blur = dataLevel_ - viewLevel_;
            assert blur >= 0;
            int shift = blur * 2;
            BinList binList =
                createBinList( hstyle_.combiner_, dataLevel_, viewLevel_ );
            while ( tseq.next() ) {
                double value = tseq.getDoubleValue( icValue_ );
                if ( ! Double.isNaN( value ) ) {
                    long hpx = indexReader_.getHealpixIndex( tseq );
                    long ibin = hpx >> shift;
                    binList.submitToBin( ibin, value );
                }
            }
            return binList.getResult();
        }

        /**
         * Returns a TileRenderer suitable for use on a given sky surface.
         *
         * @param  surface  sky surface
         * @return   tile renderer
         */
        private TileRenderer createTileRenderer( Surface surface ) {
            return new ResampleTileRenderer( (SkySurface) surface, skyPixer_ );
        }
    }

    /**
     * TileRenderer that resamples values, interrogating the bin list
     * for each screen pixel.
     * This is correct if bins are bigger than screen pixels
     * (otherwise averaging is not done correctly - should be drizzled
     * in that case really), and it is efficient f bins are not too much
     * bigger than screen pixels (in that case painting would be faster).
     */
    private static class ResampleTileRenderer implements TileRenderer {
        private final SkySurface surface_;
        private final SkyPixer skyPixer_;
        ResampleTileRenderer( SkySurface surface, SkyPixer skyPixer ) {
            surface_ = surface;
            skyPixer_ = skyPixer;
        }
        public Range calculateAuxRange( BinList.Result binResult ) {
            Rectangle bounds = surface_.getPlotBounds();
            Gridder gridder = new Gridder( bounds.width, bounds.height );
            int npix = gridder.getLength();
            Point2D.Double point = new Point2D.Double();
            double x0 = bounds.x + 0.5;
            double y0 = bounds.y + 0.5;
            long hpix0 = -1;
            Range range = new Range();
            for ( int ip = 0; ip < npix; ip++ ) {
                point.x = x0 + gridder.getX( ip );
                point.y = y0 + gridder.getY( ip );
                double[] dpos = surface_.graphicsToData( point, null );
                if ( dpos != null ) {
                    long hpix = skyPixer_.getIndex( dpos );
                    if ( hpix != hpix0 ) {
                         hpix0 = hpix;
                        range.submit( binResult.getBinValue( hpix ) );
                    }
                }
            }
            return range;
        }
        public void renderBins( Graphics g, BinList.Result binResult,
                                Shader shader, Scaler scaler ) {
            SkyDensityPlotter.paintBins( g, binResult, surface_, skyPixer_,
                                         shader, scaler );
        }
    }

    /**
     * Defines the strategy for rendering tiles to the graphics context.
     */
    private interface TileRenderer {

        /**
         * Returns the range of aux values found within a given surface.
         *
         * @param  binResult   tile bin contents
         * @return  range of pixel values
         */
        Range calculateAuxRange( BinList.Result binResult );

        /**
         * Performs the rendering of a prepared bin list on a graphics surface.
         *
         * @param  g  graphics context
         * @param  binResult   histogram containing sky pixel values
         * @param  shader   colour shading
         * @param  scaler   value scaling
         */
        void renderBins( Graphics g, BinList.Result binResult,
                         Shader shader, Scaler scaler );
    }

    /**
     * Plot layer plan for use with HealpixLayer.
     */
    private static class TilePlan {
        final int dataLevel_;
        final int viewLevel_;
        final DataSpec dataSpec_;
        final BinList.Result binResult_;

        /**
         * Constructor.
         *
         * @param  dataLevel  HEALPix level at which data was supplied
         * @param  viewLevel  HEALPix level at which pixels were calculated
         * @param  dataSpec   data spec
         * @param  binResult   tile map
         */
        TilePlan( int dataLevel, int viewLevel, DataSpec dataSpec,
                  BinList.Result binResult ) {
            dataLevel_ = dataLevel;
            viewLevel_ = viewLevel;
            dataSpec_ = dataSpec;
            binResult_ = binResult;
        }

        /**
         * Indicates whether this plan can be used for a given plot
         * specification.
         *
         * @param  dataLevel  HEALPix level at which data is supplied
         * @param  viewLevel  HEALPix level at which pixels is calculated
         * @param  dataSpec   data spec
         * @return  true iff this plan can be used for the given parameters
         */
        public boolean matches( int dataLevel, int viewLevel,
                                DataSpec dataSpec ) {
            return dataLevel_ == dataLevel
                && viewLevel_ == viewLevel
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
         * Acquires the HEALPix index corresponding to the current row of
         * a tuple sequence.
         *
         * @param  tseq  tuple sequence positioned at row of interest
         * @param  healpix index at current sequence position
         */
        long getHealpixIndex( TupleSequence tseq );
    }
}
