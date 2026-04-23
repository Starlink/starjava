package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
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
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plotter that shows a grid of vectors based on averaged
 * vector values on the sky.  The gridding is done using HEALPix tiles.
 * It paints a single Decal, no Glyphs.
 *
 * <p>Note it only works with a SkySurface.
 *
 * @author   Mark Taylor
 * @since    23 Apr 2026
 */
public class SkyVectorFieldPlotter
        implements Plotter<SkyVectorFieldPlotter.VectorFieldStyle> {

    private final boolean hasWeight_;
    private final CoordGroup coordGrp_;
    private final boolean transparent_;

    private static final ReportKey<Integer> ABSLEVEL_REPKEY =
        ReportKey
       .createIntegerKey( new ReportMeta( "abs_level", "HEALPix Level" ),
                          false );
    private static final ReportKey<Integer> RELLEVEL_REPKEY =
        ReportKey
       .createIntegerKey( new ReportMeta( "rel_level",
                                          "Relative HEALPix Level" ),
                          false );
    public static final ReportKey<Double> LONOFFSET_REPKEY =
        ReportKey
       .createDoubleKey( new ReportMeta( "lonoff", "Lon(*) Offset" ), true );

    public static final ReportKey<Double> LATOFFSET_REPKEY =
        ReportKey
       .createDoubleKey( new ReportMeta( "latoff", "Lat Offset" ), true );
    public static final ReportKey<StarTable> FIELDTABLE_REPKEY =
        ReportKey.createTableKey( new ReportMeta( "vector_map", "Vector Map" ),
                                  true );

    public static final ConfigKey<Color> COLOR_KEY = StyleKeys.COLOR;
    public static final ConfigKey<MultiPointShape> SHAPE_KEY =
        StyleKeys.VECTOR_SHAPE;
    public static final ConfigKey<Integer> THICK_KEY = 
        VectorFieldPlotter.createThicknessKey( SHAPE_KEY );
    public static final ConfigKey<Double> SCALE_KEY =
        VectorFieldPlotter.SCALE_KEY;
    public static final ConfigKey<Integer> LEVEL_KEY = createLevelKey();
    public static final ConfigKey<Combiner> COMBINER_KEY =
        VectorFieldPlotter.COMBINER_KEY;
    public static final ConfigKey<Boolean> RELATIVE_KEY =
        VectorFieldPlotter.RELATIVE_KEY;
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;

    private static final FloatingCoord VLON_COORD =
        createVectorComponentCoord( false );
    private static final FloatingCoord VLAT_COORD =
        createVectorComponentCoord( true );
    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.WEIGHT_COORD;

    /**
     * Constructor.
     *
     * @param  hasWeight  if true, vector values may be weighted
     */
    public SkyVectorFieldPlotter( boolean hasWeight ) {
        hasWeight_ = hasWeight;
        transparent_ = false;
        List<Coord> extraCoords = new ArrayList<>();
        extraCoords.add( VLON_COORD );
        extraCoords.add( VLAT_COORD );
        if ( hasWeight ) {
            extraCoords.add( WEIGHT_COORD );
        }
        coordGrp_ = CoordGroup.createCoordGroup( 1, extraCoords
                                                   .toArray( new Coord[ 0 ] ) );
    }

    public String getPlotterName() {
        return "SkyVecField";
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
            "<p>Plots a field of arrows on a HEALPix grid",
            "corresponding to the aggregated (typically averaged)",
            "value of a 2-d vector quantity",
            "such as proper motion",
            "for the points in each cell.",
            "</p>",
            "<p>Currently, the visible length of the vectors is dependent",
            "on the size of the grid; they are sized to appear with a",
            "length comparable to the size of the HEALPix cells.",
            "Units of the values specified are therefore not relevant.",
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
        keyList.add( LEVEL_KEY );
        keyList.add( COMBINER_KEY );
        keyList.add( RELATIVE_KEY );
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public VectorFieldStyle createStyle( ConfigMap config ) {
        Color color =
            StyleKeys.getAlphaColor( config, COLOR_KEY, TRANSPARENCY_KEY );
        MultiPointShape shape = config.get( SHAPE_KEY );
        int thick = config.get( THICK_KEY ).intValue();
        double scale = config.get( SCALE_KEY ).doubleValue();
        int level = config.get( LEVEL_KEY ).intValue();
        Combiner combiner = config.get( COMBINER_KEY );
        boolean isRelative = config.get( RELATIVE_KEY ).booleanValue();
        return new VectorFieldStyle( color, shape, thick, scale, level,
                                     combiner, isRelative );
    }

    public Object getRangeStyleKey( VectorFieldStyle style ) {
        return null;
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  VectorFieldStyle style ) {
        return new VectorFieldLayer( this, (SkyDataGeom) geom, dataSpec,
                                     style );
    }

    /**
     * Paints a grid of vectors onto a sky surface.
     *
     * @param   g  graphics context
     * @param   surf  sky surface
     * @param   geom   sky geom
     * @param   scribe  defines the shape of the vectors to paint
     * @param   color   colour to paint the vectors
     * @param   scale   scales the vectors
     * @param   isRelative  if true, subtract the vector mean averaged over
     *                      the visible surface from the individual vectors
     * @param   level    HEALPix order
     * @param   lonResult   maps HEALPix index to longitude part
     *                      of combined vector
     * @param   latResult   maps HEALPix index to latitude part
     *                      of combined vector
     * @param   lonOff    subtractive offset in longitude
     * @param   latOff    subtractive offset in latitude
     * @param   vecScale  nominal maximum size of vectors to plot
     */
    private static void paintVectors( Graphics g, SkySurface surf,
                                      SkyDataGeom geom,
                                      MultiPointScribe scribe, Color color,
                                      double scale, boolean isRelative,
                                      int level,
                                      BinList.Result lonResult,
                                      BinList.Result latResult,
                                      double lonOff, double latOff,
                                      double vecScale ) {
        SkySurfaceTiler tiler =
            new SkySurfaceTiler( surf, Rotation.IDENTITY, level );
        double tileSize = Math.sqrt( Tilings.healpixSteradians( level ) );
        double factor = scale * tileSize / vecScale;
        double[] dpos1 = new double[ 3 ];
        Point2D.Double gpos = new Point2D.Double();
        Point2D.Double gpos1 = new Point2D.Double();
        int[] xgoffs = new int[ 1 ];
        int[] ygoffs = new int[ 1 ];
        Color color0 = g.getColor();
        g.setColor( color );
        for ( Long visIndex : tiler.visiblePixels() ) {
            long index = visIndex.longValue();
            double vlon = lonResult.getBinValue( index );
            double vlat = latResult.getBinValue( index );
            if ( ! Double.isNaN( vlon ) && ! Double.isNaN( vlat ) ) {
                double[] dpos = tiler.getTileCenterVector( index );
                new TangentPlaneTransformer( dpos, geom )
                   .displace( 0.5 * ( vlon - lonOff ) * factor,
                              0.5 * ( vlat - latOff ) * factor,
                              dpos1 );
                if ( surf.dataToGraphics( dpos, false, gpos ) &&
                     surf.dataToGraphicsOffset( dpos, gpos,
                                                dpos1, false, gpos1 ) ) {
                    int dx = (int) Math.floor( gpos1.x - gpos.x );
                    int dy = (int) Math.floor( gpos1.y - gpos.y );
                    int px = (int) Math.floor( gpos.x - dx );
                    int py = (int) Math.floor( gpos.y - dy );
                    xgoffs[ 0 ] = (int) Math.floor( 2 * dx );
                    ygoffs[ 0 ] = (int) Math.floor( 2 * dy );
                    g.translate( px, py );
                    scribe.createGlyph( xgoffs, ygoffs ).paintGlyph( g );
                    g.translate( -px, -py );
                }
            }
        }
        g.setColor( color0 );
    }

    /**
     * Returns a StarTable containing binned vector information based on
     * a given plan.  Calling this method is not expensive, though
     * serializing the resulting table may be.
     *
     * @param  plan   plan represnting the plotted vector field
     * @param  plotter   plotter instance responsible for the plot
     * @return  table suitable for export
     */
    private static StarTable createExportTable( VectorFieldPlan plan,
                                                SkyVectorFieldPlotter plotter) {
        int level = plan.level_;
        BinList.Result lonResult = plan.lonResult_;
        BinList.Result latResult = plan.latResult_;
        DataSpec dataSpec = plan.dataSpec_;
        SkyDataGeom geom = plan.geom_;
        Combiner combiner = plan.combiner_;
        SkyPixer pixer = new SkyPixer( level );
        boolean isNested = pixer.isNested();
        HealpixTableInfo.HpxCoordSys csys =
            HealpixSys.fromGeom( geom.getViewSystem() );
        long nsky = pixer.getPixelCount();
        long ndata = lonResult.getBinCount();
        assert ndata == latResult.getBinCount();
        CoordGroup cgrp = plotter.coordGrp_;
        int icVlon = cgrp.getExtraCoordIndex( 0, geom );
        int icVlat = cgrp.getExtraCoordIndex( 1, geom );
        ValueInfo[] vlonInfos = dataSpec.getUserCoordInfos( icVlon );
        ValueInfo[] vlatInfos = dataSpec.getUserCoordInfos( icVlat );
        ValueInfo vlonInfo = combiner.createCombinedInfo( vlonInfos[ 0 ], null);
        ValueInfo vlatInfo = combiner.createCombinedInfo( vlatInfos[ 0 ], null);
        ColumnData vlonCol =
            BinResultColumnData.createInstance( vlonInfo, lonResult, 1.0 );
        ColumnData vlatCol =
            BinResultColumnData.createInstance( vlatInfo, latResult, 1.0 );

        /* Full sky table. */
        if ( ndata * 1.0 / nsky > 0.75 && nsky <= Integer.MAX_VALUE ) {
            int nrow = (int) nsky;
            assert nrow == nsky;
            ColumnStarTable table =
                ColumnStarTable.makeTableWithRows( nrow );
            table.addColumn( vlonCol );
            table.addColumn( vlatCol );
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
                vlonCol.getColumnInfo(),
                vlatCol.getColumnInfo(),
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
                        new BinRowIterator( lonResult.indexIterator(),
                                            vlonCol, vlatCol, isLong );
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

    /**
     * Returns a coord for one of the vector components.
     *
     * @param  isLat  false for longitude, true for latitude
     * @return  new coord
     */
    private static FloatingCoord createVectorComponentCoord( boolean isLat ) {
        InputMeta meta =
            new InputMeta( "v" + ( isLat ? "lat" : "lon" ),
                           ( isLat ? "Lat" : "Lon(*)" ) + " Component" );
        String shortDesc = ( isLat ? "Latitude" : "Longitude" )
                         + " component of vector";
        String xmlDesc = ( isLat ? "Latitude" : "Longitude" )
                       + " component of the plotted vector.";
        if ( !isLat ) {
            shortDesc += " premultiplied by cos(lat)";
            xmlDesc += " The supplied value <strong>is</strong> considered"
                     + " to be premultiplied by cos(Latitude).";
        }
        meta.setShortDescription( shortDesc );
        meta.setXmlDescription( "<p>" + xmlDesc + "</p>" );
        return FloatingCoord.createCoord( meta, true );
    }

    /**
     * Returns a config key for specifying HEALPix level at which vectors
     * are averaged.
     *
     * @return  new config key
     */
    private static ConfigKey<Integer> createLevelKey() {
        ConfigMeta meta = new ConfigMeta( "level", "HEALPix Level" );
        meta.setStringUsage( "<-rel-level|+abs-level>" );
        meta.setShortDescription( "HEALPix level, negative for relative" );
        meta.setXmlDescription( new String[] {
            "<p>Determines the HEALPix level of pixels which are averaged",
            "over to calculate vectors.",
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
        } );
        assert ABSLEVEL_REPKEY != null && RELLEVEL_REPKEY != null;
        return IntegerConfigKey
              .createSpinnerPairKey( meta, -4, 29, -7, "Abs", "Rel",
                                     ABSLEVEL_REPKEY, RELLEVEL_REPKEY );
    }

    /**
     * PlotLayer implementation for use with this class.
     */
    private static class VectorFieldLayer extends AbstractPlotLayer {

        private final SkyVectorFieldPlotter plotter_;
        private final SkyDataGeom geom_;
        private final VectorFieldStyle style_;
        private final int icPos_;
        private final int icVlon_;
        private final int icVlat_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param  plotter  plotter
         * @param  geom     geom
         * @param  dataSpec   data specification
         * @param  style    plotting style
         */
        VectorFieldLayer( SkyVectorFieldPlotter plotter, SkyDataGeom geom,
                          DataSpec dataSpec, VectorFieldStyle style ) {
            super( plotter, geom, dataSpec, style,
                   style.color_.getAlpha() == 255 ? LayerOpt.OPAQUE
                                                  : LayerOpt.NO_SPECIAL );
            plotter_ = plotter;
            geom_ = geom;
            style_ = style;
            CoordGroup cgrp = plotter.coordGrp_;
            icPos_ = cgrp.getPosCoordIndex( 0, geom );
            icVlon_ = cgrp.getExtraCoordIndex( 0, geom );
            icVlat_ = cgrp.getExtraCoordIndex( 1, geom );
            icWeight_ = plotter.hasWeight_
                      ? cgrp.getExtraCoordIndex( 2, geom )
                      : -1;
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Span> auxSpans,
                                      PaperType ptype ) {
            SkySurface ssurf = (SkySurface) surface;
            int level = getLevel( ssurf );
            return new VectorFieldDrawing( ssurf, ptype, level );
        }

        /**
         * Returns the HEALPix level that this layer will plot pixels at
         * for a given plot surface.
         *
         * @param  surface   plot surface
         * @return   HEALPix grid level
         */
        private int getLevel( SkySurface surface ) {
            int pixLevel = SkyDensityPlotter.getPixelLevel( surface );
            return style_.level_ >= 0
                 ? Math.min( style_.level_, pixLevel )
                 : Math.max( 0, pixLevel + style_.level_ );
        }

        /**
         * Accumulates vector values from the input data into an
         * object containing the binned data.
         *
         * @param  surface   plotting surface
         * @param  dataSpec   data specification
         * @param  dataStore   data store
         * @return   binned data for plotting
         */
        private LonLatBinList readBins( SkySurface surface, DataSpec dataSpec,
                                        DataStore dataStore ) {
            int level = getLevel( surface );
            Combiner combiner = style_.combiner_;
            int icw = dataSpec.isCoordBlank( icWeight_ ) ? -1 : icWeight_;
            BinCollector collector =
                new BinCollector( combiner, level, geom_,
                                  icPos_, icVlon_, icVlat_, icw );
            return PlotUtil.tupleCollect( collector, dataSpec, dataStore );
        }

        /**
         * Identifies and returns a plan object that can be used for
         * this layer from a list of precalculated plans.
         * If none of the supplied plans is suitable, null is returned.
         *           
         * @param  knownPlans  available pre-calculated plans
         * @param  level   HEALPix level giving desired sky pixel resolution
         * @return   suitable typed plan from given list, or null
         */
        private VectorFieldPlan getVectorFieldPlan( Object[] knownPlans,
                                                    int level ) {
            Combiner combiner = style_.combiner_;
            DataSpec dataSpec = getDataSpec();
            return Arrays.stream( knownPlans )
                  .filter( p -> p instanceof VectorFieldPlan )
                  .map( p -> (VectorFieldPlan) p )
                  .filter( p -> p.matchesData( level, combiner,
                                               dataSpec, geom_ ) )
                  .findFirst()
                  .orElse( null );
        }

        /**
         * Drawing implementation for this class.
         */
        private class VectorFieldDrawing implements Drawing {
            private final SkySurface surface_;
            private final PaperType ptype_;
            private final int level_;

            /**
             * Constructor.
             *
             * @param  surface  plotting surface
             * @param  ptype   paper type
             * @param  level  HEALPix level
             */
            VectorFieldDrawing( SkySurface surface, PaperType ptype,
                                int level ) {
                surface_ = surface;
                ptype_ = ptype;
                level_ = level;
            }

            public VectorFieldPlan calculatePlan( Object[] knownPlans,
                                                  DataStore dataStore ) {
                VectorFieldPlan knownPlan =
                    getVectorFieldPlan( knownPlans, level_ );
                final VectorFieldPlan basicPlan;
                if ( knownPlan != null ) {
                    basicPlan = knownPlan;
                }
                else {
                    DataSpec dataSpec = getDataSpec();
                    LonLatBinList lonlatBins =
                        readBins( surface_, dataSpec, dataStore );
                    BinList.Result lonResult =
                        lonlatBins.lonBinList_.getResult().compact();
                    BinList.Result latResult =
                        lonlatBins.latBinList_.getResult().compact();
                    basicPlan = new VectorFieldPlan( level_, style_.combiner_,
                                                     dataSpec, geom_,
                                                     lonResult, latResult );
                }

                /* Perform additional calculations that only need to iterate
                 * over every averaged cell.  The plan needs this information
                 * so it can supply it to the ReportMap. */
                return basicPlan.getAdjustedPlan( surface_,
                                                  style_.isRelative_ );
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                VectorFieldPlan vplan = (VectorFieldPlan) plan;
                MultiPointScribe scribe =
                    style_.shape_.createScribe( style_.thick_ );
                Color color = style_.color_;
                boolean isOpaque = color.getAlpha() == 255;
                assert isOpaque || plotter_.transparent_;
                double scale = style_.scale_;
                boolean isRelative = style_.isRelative_;
                ptype_.placeDecal( paper, new Decal() {
                    public boolean isOpaque() {
                        return isOpaque;
                    }
                    public void paintDecal( Graphics g ) {
                        paintVectors( g, surface_, geom_, scribe, color,
                                      scale, isRelative, level_,
                                      vplan.lonResult_, vplan.latResult_,
                                      vplan.lonOff_, vplan.latOff_,
                                      vplan.vecScale_ );
                    }
                } );
            }

            public ReportMap getReport( Object plan ) {
                ReportMap report = new ReportMap();
                if ( plan instanceof VectorFieldPlan ) {
                    VectorFieldPlan vplan = (VectorFieldPlan) plan;
                    if ( vplan.isRelative_ ) {
                        report.put( LONOFFSET_REPKEY,
                                    Double.valueOf( vplan.lonOff_ ) );
                        report.put( LATOFFSET_REPKEY,
                                    Double.valueOf( vplan.latOff_ ) );
                    }
                    int pixelLevel =
                        SkyDensityPlotter.getPixelLevel( vplan.surface_ );
                    int absLevel = vplan.level_;
                    int relLevel = absLevel - pixelLevel;
                    report.put( ABSLEVEL_REPKEY, Integer.valueOf( absLevel ) );
                    report.put( RELLEVEL_REPKEY, Integer.valueOf( relLevel ) );
                    report.put( FIELDTABLE_REPKEY,
                                createExportTable( vplan, plotter_ ) );
                }
                return report;
            }
        }
    }

    /**
     * Collector for accumulating combined vector components.
     */
    private static class BinCollector
            implements SplitCollector<TupleSequence,LonLatBinList> {

        private final Combiner combiner_;
        private final int level_;
        private final SkyDataGeom geom_;
        private final int icPos_;
        private final int icVlon_;
        private final int icVlat_;
        private final int icWeight_;

        /**
         * Constructor.
         *
         * @param    combiner  combination method
         * @param    level   HEALPix level
         * @param    geom   geom
         * @param    icPos  coordinate index for position coord
         * @param    icVlon  coordinate index for vector longitude coord
         * @param    icVlat  coordinate index for vector latitude coord
         * @param    icWeight  coordinate index for weighting coordinate
         */
        BinCollector( Combiner combiner, int level, SkyDataGeom geom,
                      int icPos, int icVlon, int icVlat, int icWeight ) {
            combiner_ = combiner;
            level_ = level;
            geom_ = geom;
            icPos_ = icPos;
            icVlon_ = icVlon;
            icVlat_ = icVlat;
            icWeight_ = icWeight;
        }

        public LonLatBinList createAccumulator() {
            return new LonLatBinList( level_, combiner_ );
        }

        public void accumulate( TupleSequence tseq, LonLatBinList lonlatBins ) {
            double[] dpos = new double[ geom_.getDataDimCount() ];
            double[][] xyzExtras = new double[ 1 ][ geom_.getDataDimCount() ];
            double[] lonlatOff = new double[ 2 ];
            Point2D.Double gpos = new Point2D.Double();
            BinList lonBins = lonlatBins.lonBinList_;
            BinList latBins = lonlatBins.latBinList_;
            SkyPixer pixer = lonlatBins.pixer_;
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, icPos_, dpos ) ) {
                    long ibin = pixer.getIndex( dpos );
                    if ( ibin >= 0 ) {
                        double vlon =
                            VLON_COORD.readDoubleCoord( tseq, icVlon_ );
                        double vlat =
                            VLAT_COORD.readDoubleCoord( tseq, icVlat_ );
                        if ( !Double.isNaN( vlon ) && !Double.isNaN( vlat ) ) {
                            if ( icWeight_ >= 0 ) {
                                double w = WEIGHT_COORD
                                          .readDoubleCoord( tseq, icWeight_ );
                                if ( !Double.isNaN( w ) ) {
                                    lonBins.submitToBin( ibin, vlon * w );
                                    latBins.submitToBin( ibin, vlat * w );
                                }
                            }
                            else {
                                lonBins.submitToBin( ibin, vlon );
                                latBins.submitToBin( ibin, vlat );
                            }
                        }
                    }
                }
            }
        }

        public LonLatBinList combine( LonLatBinList lonlatBins1,
                                      LonLatBinList lonlatBins2 ) {
            lonlatBins1.lonBinList_.addBins( lonlatBins2.lonBinList_ );
            lonlatBins1.latBinList_.addBins( lonlatBins2.latBinList_ );
            return lonlatBins1;
        }
    }

    /**
     * Iterates over non-empty HEALPix bins, returning a 3-element object:
     * HEALPix index, longitude component of vector, latitude component
     * of vector.
     */
    private static class BinRowIterator implements Iterator<Object[]> {
        private final Iterator<Long> indexIt_;
        private final ColumnData vlonCol_;
        private final ColumnData vlatCol_;
        private final boolean isLong_;

        /**
         * Constructor.
         *
         * @param  indexIt  iterator over HEALPix indices
         * @param  vlonCol  column data representing longitude
         * @param  vlatCol  column data represengint latitude
         * @param  isLong   true for long output index, false for int
         */
        BinRowIterator( Iterator<Long> indexIt, ColumnData vlonCol,
                        ColumnData vlatCol, boolean isLong ) {
            indexIt_ = indexIt;
            vlonCol_ = vlonCol;
            vlatCol_ = vlatCol;
            isLong_ = isLong;
        }

        public Object[] next() {
            Long index = indexIt_.next();
            long ix = index.longValue();
            final Number ixObj;

            /* Careful: silent unboxing can do horrible things here.
             * Evaluating "ixObj = isLong_ ? index : Integer.valueOf((int) ix)"
             * gives you a Long even when isLong_ false!! */
            if ( isLong_ ) {
                ixObj = index;
            }
            else {
                ixObj = Integer.valueOf( (int) ix );
            }

            final Object vlon;
            final Object vlat;
            try {
                vlon = vlonCol_.readValue( ix );
                vlat = vlatCol_.readValue( ix );
            }
            catch ( IOException e ) {  // shouldn't happen
                throw new IteratorRowSequence.PackagedIOException( e );
            }
            return new Object[] { ixObj, vlon, vlat };
        }

        public boolean hasNext() {
            return indexIt_.hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Stores per-tile combined vector coordinates.
     * The data is held in two BinLists; the assumption is that these
     * will be updated in parallel and therefore have equal key sets.
     */
    private static class LonLatBinList {
        final SkyPixer pixer_;
        final HashBinList lonBinList_;
        final HashBinList latBinList_;

        /**
         * Constructor.
         *
         * @param  level  HEALPix level
         * @param  combiner  combination method
         */
        LonLatBinList( int level, Combiner combiner ) {
            pixer_ = new SkyPixer( level );
            long nbin = pixer_.getPixelCount();
            lonBinList_ = new HashBinList( nbin, combiner );
            latBinList_ = new HashBinList( nbin, combiner );
        }
    }

    /**
     * Style for SkyVectorFieldPlotter.
     */
    public static class VectorFieldStyle implements Style {

        private final Color color_;
        private final MultiPointShape shape_;
        private final int thick_;
        private final double scale_;
        private final int level_;
        private final Combiner combiner_;
        private final boolean isRelative_;

        /**
         * Constructor.
         *
         * @param  color  arrow colour, may have a non-unity alpha channel
         * @param  shape  arrow shape
         * @param  thick  arrow line thickness
         * @param  scale  arrow length scaling
         * @param  level  HEALPix level for combination
         * @param  combiner  combination method
         * @param  isRelative  whether to subtract field mean from vectors
         */
        public VectorFieldStyle( Color color, MultiPointShape shape, int thick,
                                 double scale, int level, Combiner combiner,
                                 boolean isRelative ) {
            color_ = color;
            shape_ = shape;
            thick_ = thick;
            scale_ = scale;
            level_ = level;
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
            int code = 866152;
            code = 23 * code + color_.hashCode();
            code = 23 * code + shape_.hashCode();
            code = 23 * code + thick_;
            code = 23 * Float.floatToIntBits( (float) scale_ );
            code = 23 * code + level_;
            code = 23 * code + combiner_.hashCode();
            code = 23 * code + ( isRelative_ ? 19 : 29 );
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
                    && this.level_ == other.level_
                    && this.combiner_.equals( other.combiner_ )
                    && this.isRelative_ == other.isRelative_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Plot plan implementation for this class.
     */
    private static class VectorFieldPlan {
        final int level_;
        final Combiner combiner_;
        final DataSpec dataSpec_;
        final SkyDataGeom geom_;
        final BinList.Result lonResult_;
        final BinList.Result latResult_;
        final SkySurface surface_;
        final boolean isRelative_;
        final double lonOff_;
        final double latOff_;
        final double vecScale_;

        /**
         * Constructs a skeleton plan from the binned data components.
         * An instance constructed like this has the results of the
         * expensive calculations, but is not yet usable for plotting.
         *
         * @param  level   HEALPix level
         * @param  combiner  combination method
         * @param  dataSpec   data specification
         * @param  geom   geom
         * @param  lonResult  binned longitude components of combined data
         * @param  latResult  binned latitude components of combined data
         */
        VectorFieldPlan( int level, Combiner combiner,
                         DataSpec dataSpec, SkyDataGeom geom,
                         BinList.Result lonResult, BinList.Result latResult ) {
            level_ = level;
            combiner_ = combiner;
            dataSpec_ = dataSpec;
            geom_ = geom;
            lonResult_ = lonResult;
            latResult_ = latResult;
            surface_ = null;
            isRelative_ = false;
            lonOff_ = 0;
            latOff_ = 0;
            vecScale_ = Double.NaN;
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
         * @param  lonOff   subtractive offset in longitude
         * @param  latOff   subtractive offset in latitude
         * @param   vecScale  nominal maximum size of vectors to plot
         */
        VectorFieldPlan( VectorFieldPlan plan, SkySurface surface,
                         boolean isRelative, double lonOff, double latOff,
                         double vecScale ) {
            level_ = plan.level_;
            combiner_ = plan.combiner_;
            dataSpec_ = plan.dataSpec_;
            geom_ = plan.geom_;
            lonResult_ = plan.lonResult_;
            latResult_ = plan.latResult_;
            surface_ = surface;
            isRelative_ = isRelative;
            lonOff_ = lonOff;
            latOff_ = latOff;
            vecScale_ = vecScale;
        }

        /**
         * Indicates whether this plan can be used for a given plot
         * specification.
         *
         * @param  level  HEALPix level
         * @param  combiner  combination mode
         * @param  dataSpec   data specification
         * @param  geom   geom
         */
        public boolean matchesData( int level, Combiner combiner,
                                    DataSpec dataSpec, DataGeom geom ) {
            return level_ == level
                && combiner_ == combiner
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
        public VectorFieldPlan getAdjustedPlan( SkySurface surface,
                                                boolean isRelative ) {
            if ( surface.equals( surface_ ) && isRelative == isRelative_ ) {
                return this;
            }
            else {
                SkySurfaceTiler tiler =
                    new SkySurfaceTiler( surface, Rotation.IDENTITY, level_ );

                /* Calculate mean over field of view so it can be subtracted,
                 * if required. */
                final double lonOff;
                final double latOff;
                if ( isRelative ) {
                    int count = 0;
                    double lonSum = 0;
                    double latSum = 0;
                    for ( Long visIndex : tiler.visiblePixels() ) {
                        long index = visIndex.longValue();
                        double vlon = lonResult_.getBinValue( index );
                        double vlat = latResult_.getBinValue( index );
                        if ( ! Double.isNaN( vlon ) &&
                             ! Double.isNaN( vlat ) ) {
                            count += 1;
                            lonSum += vlon;
                            latSum += vlat;
                        }
                    }
                    lonOff = count > 0 ? lonSum / count : 0;
                    latOff = count > 0 ? latSum / count : 0;
                }
                else {
                    lonOff = 0;
                    latOff = 0;
                }

                /* Calculate representative size of vectors
                 * for visual scaling purposes.
                 * There is currently no absolute scaling. */
                Combiner.Container quantiler =
                    VectorFieldPlotter.createScalingCombiner();
                for ( Long visIndex : tiler.visiblePixels() ) {
                    long index = visIndex.longValue();
                    double vlon = lonResult_.getBinValue( index );
                    double vlat = latResult_.getBinValue( index );
                    if ( ! Double.isNaN( vlon ) && ! Double.isNaN( vlat ) ) {
                        quantiler.submit( Math.hypot( vlon - lonOff,
                                                      vlat - latOff ) );
                    }
                }
                double vecScale = quantiler.getCombinedValue();
                return new VectorFieldPlan( this, surface, isRelative,
                                            lonOff, latOff, vecScale );
            }
        }
    }
}
