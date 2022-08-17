package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.CaptionerKeySet;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.StringCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plotter that writes a text label at each graphics position.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public abstract class LabelPlotter extends AbstractPlotter<LabelStyle> {

    private final boolean isArea_;

    private static final StringCoord LABEL_COORD =
        new StringCoord(
            new InputMeta( "label", "Label" )
           .setShortDescription( "Content of label" )
           .setXmlDescription( new String[] {
                "<p>Column or expression giving the text of the label",
                "to be written near the position being labelled.",
                "Label values may be of any type (string or numeric)",
                "</p>",
            } )
        , true );
    private static final int MAX_CROWDLIMIT = Byte.MAX_VALUE / 2 - 1;

    /** Config key to control X pixel offset. */
    public static final ConfigKey<Integer> XOFF_KEY = createOffsetKey( "X" );

    /** Config key to control Y pixel offset. */
    public static final ConfigKey<Integer> YOFF_KEY = createOffsetKey( "Y" );

    /** Config key to control minimum pixel label spacing. */
    public static final ConfigKey<Integer> SPACING_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "spacing", "Spacing Threshold" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Minimum size in pixels for label group" )
           .setXmlDescription( new String[] {
                "<p>Determines the closest that labels can be spaced.",
                "If a group of labels is closer to another group",
                "than the value of this parameter,",
                "they will not be drawn, to avoid the display becoming",
                "too cluttered.",
                "The effect is that you can see individual labels",
                "when you zoom in, but not when there are many labelled points",
                "plotted close together on the screen.",
                "Set the value higher for less cluttered labelling.",
                "</p>",
            } )
        , 12, 0.5, 200, true );

    /** Config key to control max label count in spacing region. */
    public static final ConfigKey<Integer> CROWDLIMIT_KEY =
        IntegerConfigKey.createSpinnerKey(
            new ConfigMeta( "crowdlimit", "Crowding Limit" )
           .setStringUsage( "<n>" )
           .setShortDescription( "Maximum labels per group" )
           .setXmlDescription( new String[] {
                "<p>Sets the maximum number of labels in a label group.",
                "This many labels can appear closely spaced without being",
                "affected by the label spacing parameter.",
                "</p>",
                "<p>It is useful for instance if you are looking at",
                "pairs of points, which will always be close together;",
                "if you set this value to 2, an isolated pair of labels",
                "can be seen, but if it's 1 then they will only be plotted",
                "when they are distant from each other,",
                "which may only happen at very high magnifications.",
                "</p>",
            } )
        , 2, 1, MAX_CROWDLIMIT );

    /** Config key set for configuring text font. */
    public static final CaptionerKeySet CAPTIONER_KEYSET =
        new CaptionerKeySet();

    /** Instance of this class for use with point positions. */
    public static final LabelPlotter POINT_INSTANCE = createPointLabelPlotter();

    /** Instance of this class for use with Plane geometry Area positions. */
    public static final LabelPlotter AREA_PLANE_INSTANCE =
        createAreaLabelPlotter( AreaCoord.PLANE_COORD, null );

    /** Instance of this class for use with Sky geometry Area positions. */
    public static final LabelPlotter AREA_SKY_INSTANCE =
        createAreaLabelPlotter( AreaCoord.SKY_COORD, null);

    /** Instance of this class for use with Sphere geometry Area positions. */
    public static final LabelPlotter AREA_SPHERE_INSTANCE =
        createAreaLabelPlotter( AreaCoord.SPHERE_COORD, AreaForm.RADIAL_COORD );

    /**
     * Constructor.
     *
     * @param  cgrp  coord group
     * @param  isArea  true for area coordinates, false for point coords
     */
    protected LabelPlotter( CoordGroup cgrp, boolean isArea ) {
        super( isArea ? "AreaLabel" : "Label",
               isArea ? ResourceIcon.PLOT_AREALABEL : ResourceIcon.PLOT_LABEL,
               cgrp, false );
        isArea_ = isArea;
    }

    /**
     * Provides a DataGeom to be used by the layer this form makes,
     * given a DataGeom that characterises the plotting environment.
     * The output should be similar to the input, for instance
     * implementing the same plotType-specific DataGeom subtype.
     *
     * <p>In most cases the supplied instance can be returned unchanged,
     * but instances with special requirements may want to adjust
     * how the data is interpreted.
     *
     * @param  baseGeom   context geom
     * @return   geom to use for data interpretation,
     *           the same or similar to the input
     */
    protected abstract DataGeom adjustGeom( DataGeom baseGeom );

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Draws a text label " )
            .append( isArea_ ? "near the center of each area."
                             : "at each position.\n" )
            .append( "You can select the font,\n" )
            .append( "where the labels appear " )
            .append( "in relation to the point positions, and\n" )
            .append( "how crowded the points have to get " )
            .append( "before they are suppressed.\n" )
            .append( "</p>\n" );
        if ( isArea_ ) {
            sbuf.append( "<p>This is just like a normal Label plot,\n" )
                .append( "but the positions are taken from " )
                .append( "an Area coordinate\n" )
                .append( "rather than normal positional coordinates.\n" )
                .append( "</p>\n" );
        }
        return sbuf.toString();
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.addAll( Arrays.asList( CAPTIONER_KEYSET.getKeys() ) );
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.ANCHOR,
            StyleKeys.COLOR,
            XOFF_KEY,
            YOFF_KEY,
            SPACING_KEY,
            CROWDLIMIT_KEY,
        } ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public LabelStyle createStyle( ConfigMap config ) throws ConfigException {
        int iclimit = config.get( CROWDLIMIT_KEY );
        if ( iclimit < 1 || iclimit > MAX_CROWDLIMIT ) {
            throw new ConfigException( CROWDLIMIT_KEY,
                                       iclimit + " out of range "
                                     + 1 + ".." + MAX_CROWDLIMIT );
        }
        byte crowdLimit = (byte) iclimit;
        assert crowdLimit == iclimit;
        Point offset = new Point( config.get( XOFF_KEY ).intValue(),
                                  - config.get( YOFF_KEY ).intValue() );
        return new LabelStyle( CAPTIONER_KEYSET.createValue( config ),
                               config.get( StyleKeys.ANCHOR ),
                               config.get( StyleKeys.COLOR ),
                               config.get( SPACING_KEY ), crowdLimit, offset );
    }

    public PlotLayer createLayer( DataGeom geom0,
                                  final DataSpec dataSpec,
                                  final LabelStyle style ) {
        final DataGeom geom = adjustGeom( geom0 );
        LayerOpt opt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          PaperType paperType ) {
                if ( paperType instanceof PaperType2D ) {
                    return new LabelDrawing2D( geom, dataSpec, style, surface,
                                               (PaperType2D) paperType );
                }
                else if ( paperType instanceof PaperType3D ) {
                    return new LabelDrawing3D( geom, dataSpec, style, surface,
                                               (PaperType3D) paperType );
                }
                else {
                    throw new IllegalArgumentException( "paper type" );
                }
            }
        };
    }

    /**
     * Returns the coordinate index in the DataSpec at which a given
     * coordinate can be found.
     *
     * @param   coordinate to locate
     * @param   dataSpec  data specification
     * @return   index in dataSpec and tuples at which coord value is given,
     *           or -1 if not found
     */
    private static int getCoordIndex( Coord coord, DataSpec dataSpec ) {
        int nc = dataSpec.getCoordCount();
        for ( int ic = 0; ic < nc; ic++ ) {
            if ( dataSpec.getCoord( ic ) == coord ) {
                return ic;
            }
        }
        return -1;
    }

    /**
     * Abstract Drawing implementation for writing labels.
     * The plan it creates is a map of labels indexed by screen position.
     * The number of entries in this map cannot be greater than the
     * number of pixels in the plot, and if spacing is set non-zero then
     * it will be (probably much) smaller.  This limits the number of
     * text strings that need to be drawn.
     *
     * <p>The parameterised type <code>T</code> is the type of the
     * label object stored; it differs according to plot (PaperType)
     * dimensionality.
     */
    private static abstract class LabelDrawing<T extends Comparable<T>>
            implements Drawing {
        final DataSpec dataSpec_;
        final LabelStyle style_;
        final Surface surface_;
        final Class<T> clazz_;
        final DataGeom geom_;
        final int icPos_;
        final int icLabel_;

        /**
         * Constructor.
         *
         * @param  geom  data geometry
         * @param  dataSpec  full data specification
         * @param  style  style
         * @param  surface  plot surface
         * @param  clazz  parameterising class
         */
        LabelDrawing( DataGeom geom, DataSpec dataSpec, LabelStyle style,
                      Surface surface, Class<T> clazz ) {
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            surface_ = surface;
            clazz_ = clazz;
            icPos_ = 0;
            icLabel_ = getCoordIndex( LABEL_COORD, dataSpec );
        }

        /**
         * Constructs a map of screen positions to label contents.
         *
         * @param  dataStore  data storage object
         * @param  mask  indicates at which positions labels may appear
         * @return   position label map
         */
        Map<Point,T> createMap( DataStore dataStore, GridMask mask ) {
            return PlotUtil.tupleCollect( createLabelCollector( mask ),
                                          dataSpec_, dataStore );
        }

        /**
         * Returns a LabelCollector instance suitable for this drawing.
         *
         * @param  mask  indicates at which positions labels may appear
         */
        abstract LabelCollector<T> createLabelCollector( GridMask mask );

        /**
         * Renders the contents of the label map to the paper.
         *
         * @param  map  map created by {@link #createMap}
         * @param  paper  graphics destination
         */
        abstract void paintMap( Map<Point,T> map, Paper paper );

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            int spacing = style_.getSpacing();
            byte crowdLimit = style_.getCrowdLimit();
            for ( int i = 0; i < knownPlans.length; i++ ) {
                Object plan = knownPlans[ i ];
                if ( plan instanceof LabelPlan &&
                    ((LabelPlan<?>) plan).matches( geom_, dataSpec_, surface_,
                                                   spacing, crowdLimit,
                                                   clazz_ ) ) {
                    return plan;
                }
            }
            PointCloud cloud =
                new PointCloud( new SubCloud( geom_, dataSpec_, icPos_ ) );
            BinPlan binPlan =
                BinPlan.calculatePointCloudPlan( cloud, surface_, dataStore,
                                                 knownPlans );
            GridMask gridMask =
                calculateGridMask( binPlan, spacing, crowdLimit, surface_ );
            Map<Point,T> map = createMap( dataStore, gridMask );
            return new LabelPlan<T>( geom_, dataSpec_, surface_, spacing,
                                     crowdLimit, map, clazz_ );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            @SuppressWarnings("unchecked")
            LabelPlan<T> labelPlan = (LabelPlan<T>) plan;
            paintMap( labelPlan.map_, paper );
        }

        public ReportMap getReport( Object plan ) {
            return null;
        }
    }

    /**
     * From a bin plan, works out the positions on the plot pixel grid
     * at which a label is permitted.  Labels are blocked if they are
     * too close, as determined by the <code>spacing</code> argument.
     *
     * @param  binPlan  grid of point counts per pixel
     * @param  spacing  minimum spacing in pixels between points to permit a
     *                  label to be drawn
     * @param  crowdLimit  number of labels allowed within spacing
     * @param  surface  plot surface
     */
    private static GridMask calculateGridMask( BinPlan binPlan, int spacing,
                                               byte crowdLimit,
                                               Surface surface ) {

        /* If spacing is zero, there are no restrictions; any grid position
         * is permitted. */
        if ( spacing == 0 ) {
            return new GridMask() {
                public boolean isFree( Point gp ) {
                    return true;
                }
            };
        }

        /* Otherwise, form a grid giving the count of points within
         * spacing pixels of each pixel.  For efficiency, the count value
         * is actually truncated at a fixed crowdLimit1, since any value above
         * that delivers the same behaviour (label forbidden). */
        final byte crowdLimit1 = (byte) ( crowdLimit + 1 );
        assert crowdLimit1 < Byte.MAX_VALUE / 2;  // else overflow possibilities
        final Gridder gridder = binPlan.getGridder();
        Binner binner = binPlan.getBinner();
        Rectangle plotBounds = surface.getPlotBounds();
        int count = gridder.getLength();
        int nx = gridder.getWidth();
        int ny = gridder.getHeight();

        /* Copy values from the bin plan to a mask array
         * (truncate at crowdLimit1). */
        byte[] mask0 = new byte[ count ];
        for ( int i = 0; i < count; i++ ) {
            mask0[ i ] = (byte) Math.min( binner.getCount( i ), crowdLimit1 );
        }

        /* Convolve the original mask with a kernel corresponding to a
         * horizontal line of pixels of length 2*spacing,
         * centered on the origin. */
        byte[] mask1 = new byte[ count ];
        for ( int ix = 0; ix < nx; ix++ ) {
            int x1 = Math.max( 0, ix - spacing );
            int x2 = Math.min( nx, ix + spacing );
            for ( int iy = 0; iy < ny; iy++ ) {
                int itarget = gridder.getIndex( ix, iy );
                for ( int jx = x1; jx < x2 && mask1[ itarget ] < crowdLimit1;
                      jx++ ) {
                    mask1[ itarget ] += mask0[ gridder.getIndex( jx, iy ) ];
                }
            }
        }

        /* Convolve the result of that step with a kernel corresponding to a
         * vertical line of pixels of length 2*spacing,
         * centered on the origin. */
        Arrays.fill( mask0, (byte) 0 );
        final byte[] mask2 = mask0;
        for ( int iy = 0; iy < ny; iy++ ) {
            int y1 = Math.max( 0, iy - spacing );
            int y2 = Math.min( ny, iy + spacing );
            for ( int ix = 0; ix < nx; ix++ ) {
                int itarget = gridder.getIndex( ix, iy );
                for ( int jy = y1; jy < y2 && mask2[ itarget ] < crowdLimit1;
                      jy++ ) {
                    mask2[ itarget ] += mask1[ gridder.getIndex( ix, jy ) ];
                }
            }
        }

        /* The result is a convolution of the original mask with a square
         * kernel with sides 2*spacing, centered on the origin.
         * The pixel counts in the result are the sums of the input values,
         * truncated at crowdLimit1.  The result is that any grid position
         * in the result mask with value >=crowdLimit1 has >=crowdLimit1
         * points plotted within spacing pixels of itself
         * (including itself). */
        final int gx0 = plotBounds.x;
        final int gy0 = plotBounds.y;
        return new GridMask() {
            public boolean isFree( Point gp ) {
                return mask2[ gridder.getIndex( gp.x - gx0, gp.y - gy0 ) ]
                     < crowdLimit1;
            }
        };
    }

    /**
     * Creates an instance of this class for use with point plotters.
     *
     * @return  instance
     */
    private static LabelPlotter createPointLabelPlotter() {
        CoordGroup cgrp = 
            CoordGroup.createCoordGroup( 1, new Coord[] { LABEL_COORD } );
        return new LabelPlotter( cgrp, false ) {
            protected DataGeom adjustGeom( DataGeom geom ) {
                return geom;
            }
        };
    }

    /**
     * Returns a config key for specifying pixel offset in a named direction.
     *
     * @param  axName  axis name
     * @return  config key
     */
    private static ConfigKey<Integer> createOffsetKey( String axisName ) {
        String axName = axisName.toUpperCase();
        String axname = axisName.toLowerCase();
        ConfigMeta meta = new ConfigMeta( axname + "off", axName + " Offset" );
        meta.setStringUsage( "<pixels>" );
        meta.setShortDescription( "Pixel offset in " + axName + " direction" );
        meta.setXmlDescription( new String[] {
            "<p>Allows fine adjustment of label positioning in the",
            axName + " direction.",
            "The value is a positive or negative pixel offset",
            "applied to the position of each plotted label.",
            "</p>",
        } );
        return IntegerConfigKey.createSpinnerKey( meta, 0, -999, 999 );
    }

    /**
     * Creates an instance of this class for use with Area plotters.
     * The position is acquired from the area values not explicitly
     * from position coordinates.
     *
     * @param   areaCoord   coord instance for acquiring area values
     * @param   extraCoord  optional additional positional coordinate, or null
     * @return  instance
     */
    private static <DG extends DataGeom> LabelPlotter
            createAreaLabelPlotter( AreaCoord<DG> areaCoord,
                                    Coord extraCoord ) {
        List<Coord> extraCoords = new ArrayList<Coord>();
        extraCoords.add( areaCoord );
        if ( extraCoord != null ) {
            extraCoords.add( extraCoord );
        }
        extraCoords.add( LABEL_COORD );
        CoordGroup cgrp =
            CoordGroup
           .createCoordGroup( 0, extraCoords.toArray( new Coord[ 0 ] ), 1 );
        return new LabelPlotter( cgrp, true ) {
            protected DataGeom adjustGeom( DataGeom geom ) {
                @SuppressWarnings("unchecked")
                DG tgeom = (DG) geom;
                return areaCoord.getAreaDataGeom( tgeom );
            }
        };
    }

    /**
     * Drawing implementation for doing labels on a 2D surface.
     * The map values are just the label strings.
     */
    private static class LabelDrawing2D extends LabelDrawing<String> {
        final PaperType2D paperType_;

        /**
         * Constructor.
         *
         * @param  geom  data geometry
         * @param  dataSpec  full data specification
         * @param  style  style
         * @param  surface  plot surface
         * @param  paperType  2D paper type
         */
        LabelDrawing2D( DataGeom geom, DataSpec dataSpec, LabelStyle style,
                        Surface surface, PaperType2D paperType ) {
            super( geom, dataSpec, style, surface, String.class );
            paperType_ = paperType;
        }

        @Override
        LabelCollector<String> createLabelCollector( final GridMask gridMask ) {
            return new LabelCollector<String>() {
                public void accumulate( TupleSequence tseq,
                                        Map<Point,String> map ) {
                    double[] dpos = new double[ surface_.getDataDimCount() ];
                    Point2D.Double gp = new Point2D.Double();
                    Point gpi = new Point();
                    while ( tseq.next() ) {
                        if ( geom_.readDataPos( tseq, icPos_, dpos ) &&
                             surface_.dataToGraphics( dpos, true, gp ) ) {
                            PlotUtil.quantisePoint( gp, gpi );
                            if ( gridMask.isFree( gpi ) ) {
                                String label =
                                    LABEL_COORD
                                   .readStringCoord( tseq, icLabel_ );
                                if ( label != null &&
                                     label.trim().length() > 0 ) {
                                    map.put( new Point( gpi ), label );
                                }
                            }
                        }
                    }
                }
            };
        }

        @Override
        void paintMap( final Map<Point,String> map, Paper paper ) {

            /* Antialiased rendering for LaTeX captioner could be
             * provided by painting this in a decal rather than glyphs.
             * Only for 2D though. */
            for ( Map.Entry<Point,String> entry : map.entrySet() ) {
                Point gp = entry.getKey();
                String label = entry.getValue();
                Glyph glyph = new LabelGlyph( label, style_ );
                paperType_.placeGlyph( paper, gp.x, gp.y, glyph,
                                       style_.getColor() );
            }
        }
    }

    /**
     * Drawing implementation for doing labels on a 3d surface.
     * The map values are String,Z-coord pairs.
     */
    private static class LabelDrawing3D extends LabelDrawing<DepthString> {
        final PaperType3D paperType_;

        /**
         * Constructor.
         *
         * @param  geom  data geometry
         * @param  dataSpec  full data specification
         * @param  style  style
         * @param  surface  plot surface
         * @param  paperType  3D paper type
         */
        LabelDrawing3D( DataGeom geom, DataSpec dataSpec, LabelStyle style,
                        Surface surface, PaperType3D paperType ) {
            super( geom, dataSpec, style, surface, DepthString.class );
            paperType_ = paperType;
        }

        @Override
        LabelCollector<DepthString>
                createLabelCollector( final GridMask gridMask ) {
            return new LabelCollector<DepthString>() {
                public void accumulate( TupleSequence tseq,
                                        Map<Point,DepthString> map ) {
                    double[] dpos = new double[ surface_.getDataDimCount() ];
                    GPoint3D gp = new GPoint3D();
                    Point gpi = new Point();
                    CubeSurface surf = (CubeSurface) surface_;
                    while ( tseq.next() ) {
                        if ( geom_.readDataPos( tseq, icPos_, dpos ) &&
                             surf.dataToGraphicZ( dpos, true, gp ) ) {
                            PlotUtil.quantisePoint( gp, gpi );
                            if ( gridMask.isFree( gpi ) ) {
                                String label =
                                    LABEL_COORD
                                   .readStringCoord( tseq, icLabel_ );
                                if ( label != null &&
                                     label.trim().length() > 0 ) {
                                    double depth = gp.z;
                                    if ( ! map.containsKey( gp ) ||
                                         depth < map.get( gpi ).depth_ ) {
                                        map.put( new Point( gpi ),
                                                 new DepthString( label,
                                                                  depth ) );
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        @Override
        void paintMap( Map<Point,DepthString> map, Paper paper ) {
            for ( Map.Entry<Point,DepthString> entry : map.entrySet() ) {
                Point gp = entry.getKey();
                DepthString value = entry.getValue();
                final String label = value.label_;
                Glyph glyph = new LabelGlyph( label, style_ );
                double depth = value.depth_;
                paperType_.placeGlyph( paper, gp.x, gp.y, depth, glyph,
                                       style_.getColor() );
            }
        }
    }

    /**
     * Aggregates a text string and a Z coordinate.
     */
    private static class DepthString implements Comparable<DepthString> {
        final String label_;
        final float depth_;

        /**
         * Constructor.
         *
         * @param  label  text
         * @param  depth  Z coordinate
         */
        DepthString( String label, double depth ) {
            label_ = label;
            depth_ = (float) depth;
        }

        public int compareTo( DepthString other ) {
            int cmp = Float.compare( this.depth_, other.depth_ );
            return cmp == 0 ? this.label_.compareTo( other.label_ )
                            : cmp;
        }
    }

    /**
     * Partial SplitCollector implementation for use with LabelPlotter.
     * This just deals with creating and merging Map objects for use
     * as accumulators.
     */
    private static abstract class LabelCollector<T extends Comparable<T>>
            implements SplitCollector<TupleSequence,Map<Point,T>> {

        public Map<Point,T> createAccumulator() {
            return new HashMap<Point,T>();
        }

        public Map<Point,T> combine( Map<Point,T> map1, Map<Point,T> map2 ) {

            /* Merge the smaller into the larger map for efficiency.
             * Do it in a deterministic way, so taht when you're plotting
             * similar scenes as part of a navigation, the points that
             * are prlotted are generally the same ones rather than
             * changing unpredictably between frames. */
            if ( map1.size() < map2.size() ) {
                addAllLower( map2, map1 );
                return map2;
            }
            else {
                addAllLower( map1, map2 );
                return map1;
            }
        }

        /**
         * Merges the contents of a source map into a destination map.
         * This is like Map.addAll, but it's deterministic;
         * lower values will not be replaced by higher ones (according to
         * Comparable).
         *
         * @param  dest  destination map, possibly augmented on exit
         * @param  src   source map, unaffected on exit
         */
        private static <T extends Comparable<T>>
                void addAllLower( Map<Point,T> dest, Map<Point,T> src ) {
            for ( Map.Entry<Point,T> entry : src.entrySet() ) {
                Point key = entry.getKey();
                T srcValue = entry.getValue();
                if ( srcValue != null ) {
                    T destValue = dest.get( key );
                    if ( destValue == null ||
                         srcValue.compareTo( destValue ) < 0 ) {
                        dest.put( key, srcValue );
                    }
                }
            }
        }
    }

    /**
     * Drawing plan implementation.
     */
    private static class LabelPlan<T> {
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Surface surface_;
        final int spacing_;
        final byte crowdLimit_;
        final Map<Point,T> map_;
        final Class<T> clazz_;

        /**
         * Constructor.
         *
         * @param  geom  data geom
         * @param  dataSpec  data specfication
         * @param  surface  plot surface
         * @param  spacing  minimum distance in pixels between labels
         * @param  crowdLimit  maximum number of labels in spacing area
         * @param  map  plan payload - a map from screen position to
         *              placeable label
         * @param  clazz  class parameterising map values
         */
        LabelPlan( DataGeom geom, DataSpec dataSpec, Surface surface,
                   int spacing, byte crowdLimit, Map<Point,T> map,
                   Class<T> clazz ) {
            geom_ = geom;
            dataSpec_ = dataSpec;
            surface_ = surface;
            spacing_ = spacing;
            crowdLimit_ = crowdLimit;
            map_ = map;
            clazz_ = clazz;
        }

        /**
         * Indicates whether this LabelPlan can be used as the plan for
         * a drawing with a given set of constraints.
         *
         * @param  geom  data geom
         * @param  dataSpec  data specfication
         * @param  surface  plot surface
         * @param  spacing  minimum distance in pixels between labels
         * @param  crowdLimit  maximum number of labels in spacing area
         * @param  parameterising class
         */
        boolean matches( DataGeom geom, DataSpec dataSpec, Surface surface,
                         int spacing, byte crowdLimit, Class<?> clazz ) {
            return geom.equals( geom_ )
                && dataSpec.equals( dataSpec_ )
                && surface.equals( surface_ )
                && spacing == spacing_
                && crowdLimit == crowdLimit_
                && clazz.equals( clazz_ );
        }
    }

    /**
     * Characterises grid point availability.
     */
    private static interface GridMask {

        /**
         * Indicates whether given pixel positions on the plot grid are
         * permitted to receive a text label.
         * 
         * @param   gp  pixel position
         * @return  true iff a text label is permitted at <code>gp</code>
         */
        boolean isFree( Point gp );
    }

    /**
     * Glyph implementation that draws text labels.
     */
    private static class LabelGlyph implements Glyph {
        private final String label_;
        private final LabelStyle style_;

        /**
         * Constructor.
         *
         * @param  label   text
         * @param  style  style
         */
        LabelGlyph( String label, LabelStyle style ) {
            label_ = label;
            style_ = style;
        }

        public void paintGlyph( Graphics g ) {
            style_.drawLabel( g, Caption.createCaption( label_ ) );
        }

        public Pixer createPixer( Rectangle clip ) {
            Caption caption = Caption.createCaption( label_ );
            Rectangle labelBox = style_.getCaptionBounds( caption );
            Rectangle drawBox = labelBox.intersection( clip );
            if ( drawBox.isEmpty() ) {
                return null;
            }
            GreyImage bitmap =
                GreyImage.createGreyImage( drawBox.width, drawBox.height );

            /* We don't do anything clever with antialiased text. */
            Graphics g = bitmap.getImage().createGraphics();
            g.translate( -labelBox.x, -labelBox.y );
            style_.drawLabel( g, caption );
            return Pixers.translate( bitmap.createPixer(),
                                     drawBox.x, drawBox.y );
        }
    }
}
