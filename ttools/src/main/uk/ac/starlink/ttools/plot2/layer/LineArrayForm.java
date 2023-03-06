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
import java.util.Comparator;
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
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
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

    /** Config key for point sequence pre-sorting. */
    public static final ConfigKey<AxisOpt> SORTAXIS_KEY = createSortAxisKey();

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

    public int getBasicPositionCount() {
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

    public int getExtraPositionCount() {
        return 0;
    }

    public DataGeom adjustGeom( DataGeom geom, DataSpec dataSpec,
                                ShapeStyle style ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        List<ConfigKey<?>> list = new ArrayList<>();
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( SORTAXIS_KEY );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Outliner createOutliner( ConfigMap config ) {
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        AxisOpt sortaxis = config.get( SORTAXIS_KEY );
        return new LineArrayOutliner( stroke, sortaxis );
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
     * Sorts an XYArrayData according to the value of a supplied option.
     *
     * @param  xyData  input array data
     * @param  sortaxis   determines whether and how sorting is done
     * @return  output array data
     */
    private static XYArrayData sortXY( XYArrayData xyData, AxisOpt sortaxis ) {
        if ( sortaxis == null || xyData == null ) {
            return xyData;
        }
        int n = xyData.getLength();
        Integer[] indices = new Integer[ n ];
        for ( int i = 0; i < n; i++ ) {
            indices[ i ] = Integer.valueOf( i );
        }
        Comparator<Integer> comparator =
            Comparator.comparingDouble( index ->
                           sortaxis.getAxisValue( xyData, index.intValue() ) );
        Arrays.sort( indices, comparator );
        return new XYArrayData() {
            public int getLength() {
                return n;
            }
            public double getX( int i ) {
                return xyData.getX( indices[ i ].intValue() );
            }
            public double getY( int i ) {
                return xyData.getY( indices[ i ].intValue() );
            }
        };
    }

    /**
     * Returns a config key for choosing a sort axis.
     *
     * @return  sort axis key for use with this class
     */
    private static ConfigKey<AxisOpt> createSortAxisKey() {
        ConfigMeta meta = new ConfigMeta( "sortaxis", "Sort Axis" );
        meta.setShortDescription( "Sort order for plotted points" );
        meta.setStringUsage( "[" + AxisOpt.X.toString()
                           + "|" + AxisOpt.Y.toString() + "]" );
        meta.setXmlDescription( new String[] {
            "<p>May be set to",
            "\"<code>" + AxisOpt.X.toString() + "</code>\" or",
            "\"<code>" + AxisOpt.Y.toString() + "</code>\"",
            "to ensure that the points for each line",
            "are plotted in ascending order",
            "of the corresponding coordinate.",
            "This will ensure that the plotted line resembles a",
            "function of the corresponding coordinate rather than",
            "a scribble.",
            "The default (null) value causes the points for each line",
            "to be joined",
            "in the sequence in which they appear in the arrays.",
            "If the points already appear in the arrays sorted",
            "according to the corresponding coordinate,",
            "this option has no visible effect,",
            "though it may slow things down.",
            "</p>",
        } );
        AxisOpt[] opts = new AxisOpt[] { null, AxisOpt.X, AxisOpt.Y };
        return new OptionConfigKey<AxisOpt>( meta, AxisOpt.class, opts,
                                             (AxisOpt) null, true ) {
            public String valueToString( AxisOpt axis ) {
                return axis == null ? LinePlotter.NOSORT_TXT : axis.toString();
            }
            public String getXmlDescription( AxisOpt axis ) {
                if ( axis == null ) {
                    return "No pre-sorting is performed";
                }
                else {
                    return "Sorting is performed on the "
                         + axis.toString() + " axis";
                }
            }
        };
    }

    /**
     * Outliner implementation for LineArrayForm.
     */
    private class LineArrayOutliner extends PixOutliner {

        private final Stroke stroke_;
        private final AxisOpt sortaxis_;
        private final Icon legendIcon_;

        /**
         * Constructor.
         *
         * @param  stroke  line stroke
         * @param  sortaxis   axis along which to sort data points, or null
         */
        public LineArrayOutliner( Stroke stroke, AxisOpt sortaxis ) {
            stroke_ = stroke;
            sortaxis_ = sortaxis;
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

        public boolean canPaint( DataSpec dataSpec ) {
            return createXYArrayReader( dataSpec ) != null;
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
            final boolean antialias = false;
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    XYArrayData xyData =
                        sortXY( xyReader.apply( tuple ), sortaxis_ );
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
                         * inefficient and means antialiasing won't work,
                         * but I don't currently have a better way
                         * to paint thick multilines with correct joins 
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
                                                        antialias, ng,
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
            code = 23 * code + PlotUtil.hashCode( sortaxis_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LineArrayOutliner ) {
                LineArrayOutliner other = (LineArrayOutliner) o;
                return this.stroke_.equals( other.stroke_ )
                    && PlotUtil.equals( this.sortaxis_, other.sortaxis_ );
            }
            else {
                return false;
            }
        }
    }
}
