package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.StringCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/** 
 * Plotter that writes a text label at each graphics position.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class LabelPlotter extends AbstractPlotter<LabelStyle> {

    private static final StringCoord LABEL_COORD =
        new StringCoord( "Text",
                         "Column or expression giving the text "
                       + "to be written near the position being labelled",
                         true );

    /**
     * Constructor.
     */
    public LabelPlotter() {
        super( "Label", ResourceIcon.PLOT_LABEL, 1,
               new Coord[] { LABEL_COORD } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.addAll( Arrays.asList( StyleKeys.getCaptionerKeys() ) );
        list.addAll( Arrays.asList( new ConfigKey[] {
            StyleKeys.ANCHOR,
            StyleKeys.COLOR,
        } ) );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LabelStyle createStyle( ConfigMap config ) {
        return new LabelStyle( StyleKeys.createCaptioner( config ),
                               config.get( StyleKeys.ANCHOR ),
                               config.get( StyleKeys.COLOR ) );
    }

    public PlotLayer createLayer( final DataGeom geom,
                                  final DataSpec dataSpec,
                                  final LabelStyle style ) {
        LayerOpt opt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
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
     * Abstract Drawing implementation for writing labels.
     * The plan it creates is a map of labels indexed by screen position.
     * Doing it like this incurs the cost of storing that map.
     * However, it has the benefit that if there are many labels plotted
     * at the same point, only one gets drawn.  There are two advantages
     * to that: first, it's faster for crowded fields (drawing millions of
     * labels could be extremely slow), and second, the top one may
     * actually be legible in the eventual plot; if many are overplotted
     * *on the same point* they will almost certainly be illegible.
     *
     * <p>The parameterised type <code>T</code> is the type of the
     * label object stored; it differs according to plot (PaperType)
     * dimensionality.
     */
    private static abstract class LabelDrawing<T> implements Drawing {
        final DataSpec dataSpec_;
        final LabelStyle style_;
        final Surface surface_;
        final DataGeom geom_;
        final int icLabel_;

        /**
         * Constructor.
         *
         * @param  geom  data geometry
         * @param  dataSpec  full data specification
         * @param  style  style
         * @param  surface  plot surface
         */
        LabelDrawing( DataGeom geom, DataSpec dataSpec, LabelStyle style,
                      Surface surface ) {
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            surface_ = surface;
            icLabel_ = geom.getPosCoords().length;
        }

        /**
         * Constructs a map of screen positions to label contents.
         *
         * @param  dataStore  data storage object
         * @return   position label map
         */
        abstract Map<Point,T> createMap( DataStore dataStore );

        /**
         * Renders the contents of the label map to the paper.
         *
         * @param  map  map created by {@link #createMap}
         * @param  paper  graphics destination
         */
        abstract void paintMap( Map<Point,T> map, Paper paper );

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            for ( int i = 0; i < knownPlans.length; i++ ) {
                Object plan = knownPlans[ i ];
                if ( plan instanceof LabelPlan &&
                     ((LabelPlan) plan).matches( geom_, dataSpec_,
                                                 surface_ ) ) {
                    return plan;
                }
            }
            Map<Point,T> map = createMap( dataStore );
            return new LabelPlan<T>( geom_, dataSpec_, surface_, map );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            @SuppressWarnings("unchecked")
            LabelPlan<T> labelPlan = (LabelPlan<T>) plan;
            paintMap( labelPlan.map_, paper );
        }
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
            super( geom, dataSpec, style, surface );
            paperType_ = paperType;
        }

        @Override
        Map<Point,String> createMap( DataStore dataStore ) {
            Map<Point,String> map = new HashMap<Point,String>();
            double[] dpos = new double[ surface_.getDataDimCount() ];
            Point gp = new Point();
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, 0, dpos ) &&
                     surface_.dataToGraphics( dpos, true, gp ) ) {
                    String label =
                        LABEL_COORD.readStringCoord( tseq, icLabel_ );
                    if ( label != null && label.trim().length() > 0 ) {
                        map.put( new Point( gp ), label );
                    }
                }
            }
            return map;
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
            super( geom, dataSpec, style, surface );
            paperType_ = paperType;
        }

        @Override
        Map<Point,DepthString> createMap( DataStore dataStore ) {
            Map<Point,DepthString> map = new HashMap<Point,DepthString>();
            double[] dpos = new double[ surface_.getDataDimCount() ];
            Point gp = new Point();
            double[] depthArr = new double[ 1 ];
            CubeSurface surf = (CubeSurface) surface_;
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, 0, dpos ) &&
                    surf.dataToGraphicZ( dpos, true, gp, depthArr ) ) {
                    String label =
                        LABEL_COORD.readStringCoord( tseq, icLabel_ );
                    if ( label != null && label.trim().length() > 0 ) {
                        double depth = depthArr[ 0 ];
                        if ( ! map.containsKey( gp ) ||
                             depth < map.get( gp ).depth_ ) {
                            map.put( new Point( gp ),
                                     new DepthString( label, depth ) );
                        }
                    }
                }
            }
            return map;
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
    private static class DepthString {
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
    }

    /**
     * Drawing plan implementation.
     */
    private static class LabelPlan<T> {
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Surface surface_;
        final Map<Point,T> map_;

        /**
         * Constructor.
         *
         * @param  geom  data geom
         * @param  dataSpec  data specfication
         * @param  surface  plot surface
         * @param  map  plan payload - a map from screen position to
         *              placeable label
         */
        LabelPlan( DataGeom geom, DataSpec dataSpec, Surface surface,
                   Map<Point,T> map ) {
            geom_ = geom;
            dataSpec_ = dataSpec;
            surface_ = surface;
            map_ = map;
        }

        /**
         * Indicates whether this LabelPlan can be used as the plan for
         * a drawing with a given set of constraints.
         *
         * @param  geom  data geom
         * @param  dataSpec  data specfication
         * @param  surface  plot surface
         */
        boolean matches( DataGeom geom, DataSpec dataSpec, Surface surface ) {
            return geom.equals( geom_ )
                && dataSpec.equals( dataSpec_ )
                && surface.equals( surface_ );
        }
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
            style_.drawLabel( g, label_ );
        }

        public Pixellator getPixelOffsets( Rectangle clip ) {

            /* Could be more efficient in the case of large labels
             * by passing the clip to the style instead. */
            return ClipPixellator.clip( style_.getPixelOffsets( label_ ),
                                        clip );
        }
    }
}
