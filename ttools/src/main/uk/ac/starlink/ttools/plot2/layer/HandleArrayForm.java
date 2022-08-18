package uk.ac.starlink.ttools.plot2.layer;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
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
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Form for drawing markers to identify X/Y array data.
 * These markers can be used as point locations for plotted lines,
 * which otherwise do not have a localised position so that they can't
 * otherwise be used for activation actions etc.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2022
 */
public class HandleArrayForm implements ShapeForm {

    private final FloatingArrayCoord xsCoord_;
    private final FloatingArrayCoord ysCoord_;
    private final int icXs_;
    private final int icYs_;

    /** Config key for handle placement. */
    public static final ConfigKey<XYArrayPlacement> PLACEMENT_KEY =
         createPlacementKey();

    /** Config key for handle shape. */
    public static final ConfigKey<MarkerShape> SHAPE_KEY = createShapeKey();

    /** Config key for handle size. */
    public static final ConfigKey<Integer> SIZE_KEY = createSizeKey();

    private static final HandleArrayForm instance_ = new HandleArrayForm();

    /**
     * Private constructor prevents external instantiation of singleton class.
     */
    private HandleArrayForm() {
        xsCoord_ = FloatingArrayCoord.X;
        ysCoord_ = FloatingArrayCoord.Y;
        icXs_ = 0;
        icYs_ = 1;
    }

    public int getBasicPositionCount() {
        return 0;
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public int getExtraPositionCount() {
        return 1;
    }

    public String getFormName() {
        return "Handles";
    }

    public Icon getFormIcon() {
        return ResourceIcon.FORM_HANDLES;
    }

    public String getFormDescription() {
        return String.join( "\n",
           "<p>Draws a symbol representing the position of an X/Y array plot.",
           "Although this may not do a good job of showing the position",
           "for a whole X/Y array, which is line-like rather than point-like,",
           "it provides a visible reference position for the plotted row.",
           "</p>",
           "<p>This plot type is therefore mostly useful in",
           "interactive environments like TOPCAT,",
           "where the plotted marker can be used for activating or identifying",
           "the corresponding table row.",
           "</p>",
           ""
        );
    }

    public DataGeom adjustGeom( DataGeom geom, DataSpec dataSpec,
                                ShapeStyle style ) {

        /* This is where the main work of this class is done;
         * by supplying a customised DataGeom, we can persuade the plotting
         * system that the array coordinates used by this form
         * correspond to definite positions in data space.
         * Exactly how those positions are defined given the array values
         * is determined by the configurable XYArrayPlacement object. */
        XYArrayPlacement placement =
            ((HandleOutliner) style.getOutliner()).placement_;
        Function<Tuple,XYArrayData> xyReader = createXYArrayReader( dataSpec );
        return xyReader == null ? geom
                                : new XYArrayDataGeom( placement, xyReader );
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            PLACEMENT_KEY,
            SIZE_KEY,
            SHAPE_KEY,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        XYArrayPlacement placement = config.get( PLACEMENT_KEY );
        int size = config.get( SIZE_KEY );
        MarkerShape shape = config.get( SHAPE_KEY );
        return new HandleOutliner( placement, shape, size );
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
    public static HandleArrayForm getInstance() {
        return instance_;
    }

    /**
     * Returns a config key for configuring handle placement.
     *
     * @return  new config key
     */
    private static ConfigKey<XYArrayPlacement> createPlacementKey() {
        ConfigMeta meta = new ConfigMeta( "placement", "Placement" );
        meta.setXmlDescription( new String[] {
            "<p>Determines where the handle will be positioned",
            "in relation to the X/Y array values.",
            "</p>",
        } );

        /* The default value (first in the list) may not be the most useful,
         * but it is one of the cheap ones, since it just picks a fixed
         * X/Y position from the input arrays.  The extremum and mean ones
         * might give a better result, but they could slow things down
         * substantially if there are many arrays being plotted.
         * The plotting system is not generally expecting DataGeom.readDataPos,
         * which is where the relevant operations are going to get invoked,
         * to be a slow operation. */
        XYArrayPlacement[] options = new XYArrayPlacement[] {
            XYArrayPlacement.MID,
            XYArrayPlacement.FIRST,
            XYArrayPlacement.LAST,
            XYArrayPlacement.YMAX,
            XYArrayPlacement.YMIN,
            XYArrayPlacement.XMAX,
            XYArrayPlacement.XMIN,
            XYArrayPlacement.XYMEAN,
        };
        OptionConfigKey<XYArrayPlacement> key =
                new OptionConfigKey<XYArrayPlacement>( meta,
                                                       XYArrayPlacement.class,
                                                       options ) {
            public String getXmlDescription( XYArrayPlacement placement ) {
                return placement.getDescription();
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Returns a config key for configuring marker shape.
     *
     * @return  new config key
     */
    private static ConfigKey<MarkerShape> createShapeKey() {
        ConfigMeta meta = new ConfigMeta( "shape", "Shape" );
        meta.setShortDescription( "Marker shape" );
        meta.setXmlDescription( new String[] {
            "<p>Sets the shape of the marker that is drawn",
            "to identify the handle position.",
            "</p>",
        } );
        MarkerShape dfltShape = FatMarkerShapes.FAT_SQUARE;
        return StyleKeys.createMarkerShapeKey( meta, dfltShape );
    }

    /**
     * Returns a config key for configuring handle size.
     *
     * @return  new config key
     */
    private static ConfigKey<Integer> createSizeKey() {
        ConfigMeta meta = new ConfigMeta( "size", "Size" );
        meta.setStringUsage( "<pixels>" );
        meta.setShortDescription( "Marker size in pixels" );
        meta.setXmlDescription( new String[] {
            "<p>Sets the size of the marker that is drawn",
            "to identify the handle position.",
            "The unit is pixels, in most cases the marker",
            "is approximately twice the size of the supplied value.",
            "</p>",
        } );
        return StyleKeys.createMarkSizeKey( meta, 4 );
    }

    /**
     * Outliner implementation for use with this form.
     */
    private class HandleOutliner extends PixOutliner {

        private final XYArrayPlacement placement_;
        private final MarkerStyle markerStyle_;
        private final Glyph glyph_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  placement  defines how to map X/Y array values to a
         *                    definite position in data coordinates
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         */
        HandleOutliner( XYArrayPlacement placement, MarkerShape shape,
                        int size ) {
            placement_ = placement;
            markerStyle_ = MarkForm.createMarkStyle( shape, size );
            glyph_ = MarkForm.createMarkGlyph( shape, size, true );
            icon_ = MarkForm.createLegendIcon( shape, size );
        }

        public Icon getLegendIcon() {
            return icon_;
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
            final Point2D.Double gp = new Point2D.Double();
            final double[] dpos = new double[ 2 ];
            return ( tuple, color, paper ) -> {
                if ( geom.readDataPos( tuple, 0, dpos ) &&
                     surface.dataToGraphics( dpos, true, gp ) ) {
                    paperType.placeGlyph( paper, gp.x, gp.y,
                                          glyph_, color );
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
            int code = 766025;
            code = 23 * code + placement_.hashCode();
            code = 23 * code + markerStyle_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HandleOutliner ) {
                HandleOutliner other = (HandleOutliner) o;
                return this.placement_.equals( other.placement_ )
                    && this.markerStyle_.equals( other.markerStyle_ );
             
            }
            else {
                return false;
            }
        }
    }

    /**
     * Custom DataGeom implementation that can extract positions
     * from XY array coordinates, which under normal circumstances
     * would not have a well-defined position.
     */
    @Equality
    private class XYArrayDataGeom implements DataGeom {

        private final XYArrayPlacement placement_;
        private final Function<Tuple,XYArrayData> xyReader_;

        /**
         * Constructor.
         *
         * @param  placement  defines how to map X/Y array values to a
         *                    definite position in data coordinates
         * @param  xyReader  defines how to map tuples to X/Y array values
         */
        XYArrayDataGeom( XYArrayPlacement placement,
                         Function<Tuple,XYArrayData> xyReader ) {
            placement_ = placement;
            xyReader_ = xyReader;
        }

        public int getDataDimCount() {
            return 2;
        }

        public Coord[] getPosCoords() {
            return new Coord[] { xsCoord_, ysCoord_ };
        }

        public String getVariantName() {
            return "Arrays" + placement_;
        }

        public boolean readDataPos( Tuple tuple, int icol, double[] dpos ) {
            XYArrayData xyData = xyReader_.apply( tuple );
            return xyData != null && placement_.readPosition( xyData, dpos );
        }

        @Override
        public int hashCode() {
            return placement_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof XYArrayDataGeom ) {
                XYArrayDataGeom other = (XYArrayDataGeom) o;
                return this.placement_.equals( other.placement_ );
            }
            else {
                return false;
            }
        }
    }
}
