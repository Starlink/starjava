package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Form for drawing one line per row, given array-valued X and Y
 * coordinates.
 * <p>Singleton class.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2021
 */
public class LineArrayForm implements ShapeForm {

    private final FloatingArrayCoord xsCoord_;
    private final FloatingArrayCoord ysCoord_;
    private final int icXs_;
    private final int icYs_;

    private static final LineArrayForm instance_ = new LineArrayForm();

    /**
     * Private constructor prevents external instantiation of singleton class.
     */
    private LineArrayForm() {
        xsCoord_ = FloatingArrayCoord.X;
        ysCoord_ = FloatingArrayCoord.Y;
        icXs_ = 0;
        icYs_ = 1;
    }

    public int getPositionCount() {
        return 0;
    }

    public String getFormName() {
        return "Lines";
    }

    public Icon getFormIcon() {
        return ResourceIcon.PLOT_LINE;
    }

    public String getFormDescription() {
        return String.join( "\n",
            "<p>Plots an <em>N</em>-segment line for each input row,",
            "with the X and Y coordinate arrays each supplied by an",
            "<em>N</em>-element array value.",
            "</p>",
            ""
        );
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        List<ConfigKey<?>> list = new ArrayList<>();
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Outliner createOutliner( ConfigMap config ) {
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        return new LineArrayOutliner( stroke, antialias );
    }

   /**
     * Returns a reader for matched X/Y array data for use with array plotters.
     * If null is returned from this function, no plotting should be done.
     *
     * @param  dataSpec  data specification
     * @return  thread-safe function to map tuples to XYArrayData;
     *          the function returns null for tuples
     *          that should not be plotted/accumulated
     */
    private Function<Tuple,XYArrayData> 
            createXYArrayReader( DataSpec dataSpec ) {
        return ArrayShapePlotter 
              .createXYArrayReader( xsCoord_, ysCoord_, icXs_, icYs_,
                                    dataSpec );
    }

    /**
     * Returns the sole instance of this singleton class.
     *
     * @return  instance
     */
    public static LineArrayForm getInstance() {
        return instance_;
    }

    /**
     * Outliner implementation for LineArrayForm.
     */
    private class LineArrayOutliner extends PixOutliner {

        private final Stroke stroke_;
        private final boolean antialias_;
        private final Icon legendIcon_;

        /**
         * Constructor.
         *
         * @param  stroke  line stroke
         * @param  antialias  antialiasing flag; note this may be ignored
         *                    depending on the ShapeMode with which it is used
         */
        public LineArrayOutliner( Stroke stroke, boolean antialias ) {
            stroke_ = stroke;
            antialias_ = antialias;
            legendIcon_ = new Icon() {
                final int width = MarkerStyle.LEGEND_ICON_WIDTH;
                final int height = MarkerStyle.LEGEND_ICON_HEIGHT;
                public int getIconWidth() {
                    return width;
                }
                public int getIconHeight() {
                    return height;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( stroke_ );
                    int y1 = y + height / 2;
                    g2.drawLine( x, y1, x + width, y1 );
                    g2.setStroke( stroke0 );
                }
            };
        }

        public Icon getLegendIcon() {
            return legendIcon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<>();
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            final Rectangle bounds = surface.getPlotBounds();
            final boolean isBitmap = paperType.isBitmap();
            final double[] dpos = new double[ 2 ];
            final Point2D.Double gpos = new Point2D.Double();
            final Function<Tuple,XYArrayData> xyReader =
                createXYArrayReader( dataSpec );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    XYArrayData xyData = xyReader.apply( tuple );
                    if ( xyData != null ) {
                        int np = xyData.getLength();
                        double[] gxs = new double[ np ];
                        double[] gys = new double[ np ];
                        int gxlo = bounds.x + bounds.width;
                        int gxhi = bounds.x;
                        int gylo = bounds.y + bounds.height;
                        int gyhi = bounds.y;
                        int jp = 0;
                        for ( int ip = 0; ip < np; ip++ ) {
                            dpos[ 0 ] = xyData.getX( ip );
                            dpos[ 1 ] = xyData.getY( ip );
                            if ( surface.dataToGraphics( dpos, false, gpos ) &&
                                 PlotUtil.isPointReal( gpos ) ) {
                                gxs[ jp ] = gpos.x;
                                gys[ jp ] = gpos.y;
                                jp++;
                                int gx = (int) gpos.x;
                                int gy = (int) gpos.y;
                                gxlo = Math.min( gx, gxlo );
                                gxhi = Math.max( gx, gxhi );
                                gylo = Math.min( gy, gylo );
                                gyhi = Math.max( gy, gyhi );
                            }
                        }
                        final int ng = jp;

                        /* The current implementation constructs a glyph
                         * by drawing onto a bitmap, which could be the
                         * size of the whole plot surface.  This is quite
                         * inefficient, but I don't currently have a better
                         * way to paint thick multilines with correct joins 
                         * onto a bitmap, which is required in order to allow
                         * ShapeMode-type shading options.
                         * For this plot form, there probably will not be
                         * a large number of rows (if there are the plot
                         * will be visually incomprehensible), so it will
                         * hopefully not be a problem in practise. */
                        if ( gxhi >= gxlo && gyhi >= gylo && jp > 0 ) {
                            Rectangle gbounds =
                                new Rectangle( gxlo, gylo,
                                               gxhi - gxlo + 1,
                                               gyhi - gylo + 1 );
                            Glyph glyph = new GraphicsGlyph( gbounds ) {
                                public void paintGlyph( Graphics g ) {
                                    LineTracer tracer =
                                        new LineTracer( g, bounds, stroke_,
                                                        antialias_, ng,
                                                        isBitmap );
                                    Color gColor = g.getColor();
                                    for ( int ip = 0; ip < ng; ip++ ) {
                                        tracer.addVertex( gxs[ ip ], gys[ ip ],
                                                          gColor );
                                    }
                                    tracer.flush();
                                }
                            };
                            paperType.placeGlyph( paper, 0, 0, glyph, color );
                        }
                    }
                }
            };
        }

        /**
         * @throws  UnsupportedOperationException
         */
        public ShapePainter create3DPainter( CubeSurface surf, DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             PaperType3D paperType ) {
            throw new UnsupportedOperationException( "no 3D" );
        }

        @Override
        public int hashCode() {
            int code = 886301;
            code = 23 * code + stroke_.hashCode();
            code = 23 * code + ( antialias_ ? 7 : 11 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LineArrayOutliner ) {
                LineArrayOutliner other = (LineArrayOutliner) o;
                return this.stroke_.equals( other.stroke_ )
                    && this.antialias_ == other.antialias_;
            }
            else {
                return false;
            }
        }
    }
}
