package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
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
 * Form for drawing an array of markers per row, given array-valued X and Y
 * coordinates.  Currently only supports X-Y plotting.
 *
 * <p>Singleton class.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2021
 */
public class MarkArrayForm implements ShapeForm {

    private final FloatingArrayCoord xsCoord_;
    private final FloatingArrayCoord ysCoord_;
    private final int icXs_;
    private final int icYs_;

    /** Config key for marker size. */
    public static final ConfigKey<Integer> SIZE_KEY =
        StyleKeys.createMarkSizeKey(
             new ConfigMeta( "size", "Size" )
            .setStringUsage( "<pixels>" )
            .setShortDescription( "Marker size in pixels" )
            .setXmlDescription( new String[] {
                 "<p>Size of the scatter plot markers.",
                 "The unit is pixels, in most cases the marker",
                 "is approximately twice the size",
                 "of the supplied value.",
                 "</p>" } ),
             1 );

    private static final MarkArrayForm instance_ = new MarkArrayForm();

    /**
     * Private constructor prevents external instantiation of singleton class.
     */
    private MarkArrayForm() {
        xsCoord_ = FloatingArrayCoord.X;
        ysCoord_ = FloatingArrayCoord.Y;
        icXs_ = 0;
        icYs_ = 1;
    }

    public int getBasicPositionCount() {
        return 0;
    }

    public String getFormName() {
        return "Marks";
    }

    public String getFormDescription() {
        return String.join( "\n",
            "<p>Plots <em>N</em> markers for each input row,",
            "with the X and Y coordinate values each supplied",
            "by an <em>N</em>-element array value.",
            "</p>",
            ""
        );
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_MARK;
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public int getExtraPositionCount() {
        return 0;
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            StyleKeys.MARKER_SHAPE,
            SIZE_KEY,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
        int size = config.get( SIZE_KEY );
        return new MarksOutliner( shape, size );
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
    public static MarkArrayForm getInstance() {
        return instance_;
    }

    /**
     * Outliner implementation for MarkArrayForm.
     * This class is currently implemented by subclassing PixOutliner,
     * meaning that the bin plan is got by painting to a bit map.
     * It would be possible to implement the calculateBinPlan method
     * directly as is done by MarkForm, which ought to be more efficient.
     * However, the number of points plotted is probably not going to be
     * all that great, so don't do expend the extra effort to do this
     * unless it becomes clear that there is a performance issue.
     */
    private class MarksOutliner extends PixOutliner {

        private final MarkerStyle style_;
        private final Glyph glyph_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size
         */
        public MarksOutliner( MarkerShape shape, int size ) {
            style_ = MarkForm.createMarkStyle( shape, size );
            glyph_ = MarkForm.createMarkGlyph( shape, size, true );
            icon_ = MarkForm.createLegendIcon( shape, size );
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<AuxScale,AuxReader>();
        }

        public boolean canPaint( DataSpec dataSpec ) {
            return createXYArrayReader( dataSpec ) != null;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            final Function<Tuple,XYArrayData> xyReader =
                createXYArrayReader( dataSpec );
            return new ShapePainter() {
                final double[] dpos = new double[ 2 ];
                final Point2D.Double gpos = new Point2D.Double();
                public void paintPoint( Tuple tuple, Color color, Paper paper ){
                    XYArrayData xyData = xyReader.apply( tuple );
                    if ( xyData != null ) {
                        int np = xyData.getLength();
                        for ( int ip = 0; ip < np; ip++ ) {
                            dpos[ 0 ] = xyData.getX( ip );
                            dpos[ 1 ] = xyData.getY( ip );
                            if ( surface.dataToGraphics( dpos, true, gpos ) ) {
                                paperType.placeGlyph( paper, gpos.x, gpos.y,
                                                      glyph_, color );
                            }
                        }
                    }
                }
            };
        }

        /**
         * @throws  UnsupportedOperationException
         */
        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            throw new UnsupportedOperationException( "No 3d" );
        }

        @Override
        public int hashCode() {
            int code = 69834;
            code = 23 * code + style_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MarksOutliner ) {
                MarksOutliner other = (MarksOutliner) o;
                return this.style_.equals( other.style_ );
            }
            else {
                return false;
            }
        }
    }
}
