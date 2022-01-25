package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;

/**
 * CombineArrayPlotter that plots a marker at each aggregated position.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2022
 */
public class MarkCombineArrayPlotter
        extends CombineArrayPlotter
                <MarkCombineArrayPlotter.MarkCombineArrayStyle> {

    /** Sole instance of this class. */
    public static final MarkCombineArrayPlotter INSTANCE =
        new MarkCombineArrayPlotter();

    /** Config key for marker size. */
    public static final ConfigKey<Integer> SIZE_KEY =
        StyleKeys.createMarkSizeKey(
             new ConfigMeta( "size", "Size" )
            .setStringUsage( "<pixels>" )
            .setShortDescription( "Marker size in pixels" )
            .setXmlDescription( new String[] {
                 "<p>Size of the markers.",
                 "The unit is pixels, in most cases the marker",
                 "is approximately twice the size",
                 "of the supplied value.",
                 "</p>" } ),
             4 );

    /**
     * Private constructor prevents instantiation of singleton class.
     */
    private MarkCombineArrayPlotter() {
        super( "StatMark", ResourceIcon.PLOT_STATMARK );
    }

    public String getPlotterDescription() {
        return String.join( "\n",
            "<p>Plots a set of markers based on a combination",
            "(typically the mean) of input array-valued coordinates.",
            "The input X and Y coordinates must be fixed-length arrays",
            "of length N;",
            "N markers are plotted, each one representing",
            "the mean (or median, minimum, maximum, ...)",
            "of all the input array elements at the corresponding position.",
            "</p>",
        "" );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<>();
        list.add( XCOMBINER_KEY );
        list.add( YCOMBINER_KEY );
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.MARKER_SHAPE );
        list.add( SIZE_KEY );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public MarkCombineArrayStyle createStyle( ConfigMap config ) {
        Combiner xCombiner = config.get( XCOMBINER_KEY );
        Combiner yCombiner = config.get( YCOMBINER_KEY );
        Color color = config.get( StyleKeys.COLOR );
        MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
        int size = config.get( SIZE_KEY );
        return new MarkCombineArrayStyle( xCombiner, yCombiner, color,
                                          shape, size );
    }

    /**
     * Style for use with this plotter.
     */
    public static class MarkCombineArrayStyle
            extends CombineArrayPlotter.CombineArrayStyle {

        private final Color color_;
        private final MarkerShape shape_;
        private final int size_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  xCombiner  combiner for elements of X array values
         * @param  yCombiner  combiner for elements of Y array values
         * @param  color  colour
         * @param  shape  marker shape
         * @param  size   marker size
         */
        public MarkCombineArrayStyle( Combiner xCombiner, Combiner yCombiner,
                                      Color color, MarkerShape shape,
                                      int size ) {
            super( xCombiner, yCombiner, new LayerOpt( color, true ) );
            color_ = color;
            shape_ = shape;
            size_ = size;
            icon_ = shape.getStyle( color,
                                    Math.max( size, MarkForm.MIN_LEGEND_SIZE ) )
                         .getLegendIcon();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public void paintPoints( PlanarSurface surface,
                                 PaperType paperType, Paper paper,
                                 Point2D.Double[] gpoints ) {
            Rectangle bounds = surface.getPlotBounds();
            if ( paperType instanceof PaperType2D ) {
                PaperType2D ptype2 = (PaperType2D) paperType;
                Glyph glyph = MarkForm.createMarkGlyph( shape_, size_, true );
                for ( Point2D.Double gp : gpoints ) {
                    if ( bounds.contains( gp ) ) {
                        ptype2.placeGlyph( paper, gp.getX(), gp.getY(), glyph,
                                           color_ );
                    }
                }
            }
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + color_.hashCode();
            code = 23 * code + shape_.hashCode();
            code = 23 * code + size_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MarkCombineArrayStyle ) {
                MarkCombineArrayStyle other = (MarkCombineArrayStyle) o;
                return super.equals( other )
                    && this.color_.equals( other.color_ )
                    && this.shape_.equals( other.shape_ )
                    && this.size_ == other.size_;
            }
            else { 
                return false;
            }
        }
    }
}
