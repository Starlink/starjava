package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * CombineArrayPlotter that plots a line between aggregated positions.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2022
 */
public class LineCombineArrayPlotter
        extends CombineArrayPlotter
                <LineCombineArrayPlotter.LineCombineArrayStyle> {

    /** Sole instance of this class. */
    public static final LineCombineArrayPlotter INSTANCE =
        new LineCombineArrayPlotter();

    /** Config key for line thickness. */
    public static ConfigKey<Integer> THICK_KEY =
        StyleKeys.createThicknessKey( 3 );

    /**
     * Private constructor prevents instantiation of singleton class.
     */
    private LineCombineArrayPlotter() {
        super( "StatLine", ResourceIcon.PLOT_STATLINE );
    }

    public String getPlotterDescription() {
        return String.join( "\n",
            "<p>Plots a single line based on a combination",
            "(typically the mean) of input array-valued coordinates.",
            "The input X and Y coordinates must be fixed-length arrays",
            "of length N;",
            "a line with N points is plotted, each point representing",
            "the mean (or median, minimum, maximum, ...)",
            "of all the input array elements at the corresponding position.",
            "</p>",
            getXYCombineComment(),
        "" );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<>();
        list.add( XCOMBINER_KEY );
        list.add( YCOMBINER_KEY );
        list.add( StyleKeys.COLOR );
        list.add( THICK_KEY );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public LineCombineArrayStyle createStyle( ConfigMap config ) { 
        Combiner xCombiner = config.get( XCOMBINER_KEY );
        Combiner yCombiner = config.get( YCOMBINER_KEY );
        int thick = config.get( THICK_KEY );
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = new BasicStroke( thick, BasicStroke.CAP_ROUND,
                                         BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        return new LineCombineArrayStyle( xCombiner, yCombiner, color,
                                          stroke, antialias );
    }

    /**
     * Style for use with this plotter.
     */
    public static class LineCombineArrayStyle
            extends CombineArrayPlotter.CombineArrayStyle {

        private final Color color_;
        private final Stroke stroke_;
        private final boolean isAntialias_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  xCombiner  combiner for elements of X array values
         * @param  yCombiner  combiner for elements of Y array values
         * @param  color  colour
         * @param  stroke  line stroke
         * @param  isAntialias  true for antialiasing
         */
        public LineCombineArrayStyle( Combiner xCombiner, Combiner yCombiner,
                                      Color color, Stroke stroke,
                                      boolean isAntialias ) {
            super( xCombiner, yCombiner, new LayerOpt( color, ! isAntialias ) );
            color_ = color;
            stroke_ = stroke;
            isAntialias_ = isAntialias;
            icon_ = new LineStyle( color, stroke, isAntialias ).getLegendIcon();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public void paintPoints( PlanarSurface surface,
                                 PaperType paperType, Paper paper,
                                 Point2D.Double[] gpoints ) {
            paperType.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    LineTracer tracer =
                        new LineTracer( g, surface.getPlotBounds(), stroke_,
                                        isAntialias_, gpoints.length + 1,
                                        paperType.isBitmap() );
                    for ( Point2D.Double gp : gpoints ) {
                        tracer.addVertex( gp.getX(), gp.getY(), color_ );
                    }
                    tracer.flush();
                }
                public boolean isOpaque() {
                    return ! isAntialias_;
                }
            } );
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + color_.hashCode();
            code = 23 * code + stroke_.hashCode();
            code = 23 * code + ( isAntialias_ ? 99 : 101 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LineCombineArrayStyle ) {
                LineCombineArrayStyle other = (LineCombineArrayStyle) o;
                return super.equals( other )
                    && this.color_.equals( other.color_ )
                    && this.stroke_.equals( other.stroke_ )
                    && this.isAntialias_ == other.isAntialias_;
            }
            else {
                return false;
            }
        }
    }
}
