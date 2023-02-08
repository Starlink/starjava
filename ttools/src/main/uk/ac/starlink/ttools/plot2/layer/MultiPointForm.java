package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.ErrorMode;
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
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that draws shapes based on a single main
 * position, and a number of additional positions supplied as
 * {@link ShapeForm#getExtraCoords extra} coordinates.
 * The extra coordinates required (defining one or more non-central
 * data positions) are defined by a supplied {@link MultiPointCoordSet}
 * and those coordinates are then plotted by a corresponding
 * {@link MultiPointShape}.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public abstract class MultiPointForm implements ShapeForm {

    private final String name_;
    private final Icon icon_;
    private final String description_;
    private final MultiPointCoordSet extraCoordSet_;
    private final MultiPointConfigKey shapeKey_;
    private final ConfigKey<Double> scaleKey_;
    private final ConfigKey<Integer> thickKey_;
    private final ConfigKey<?>[] otherKeys_;

    /** Size of longest/characteristic shape in pixels when autoscaled. */
    public static final int AUTOSCALE_PIXELS = 32;

    /**
     * Constructor.
     *
     * @param  name   shapeform name
     * @param  icon   shapeform icon
     * @param  description  XML description
     * @param  extraCoordSet  defines the extra positional coordinates 
     *                        used to plot multipoint shapes
     * @param  shapeKey  config key for the shape; provides option to
     *                   vary the shape, but any shape specified by it
     *                   must be expecting data corresponding to the
     *                   <code>extraCoordSet</code> parameter
     * @param  scaleKey  config key for scaling graphical display,
     *                   or null if no scaling is available
     * @param  otherKeys    additional config keys
     */
    public MultiPointForm( String name, Icon icon, String description,
                           MultiPointCoordSet extraCoordSet,
                           MultiPointConfigKey shapeKey,
                           ConfigKey<Double> scaleKey,
                           ConfigKey<?>[] otherKeys ) {
        name_ = name;
        icon_ = icon;
        description_ = description;
        extraCoordSet_ = extraCoordSet;
        shapeKey_ = shapeKey;
        scaleKey_ = scaleKey;
        thickKey_ = createThicknessKey( shapeKey_ );
        otherKeys_ = otherKeys;
    }

    /**
     * Returns an object that can read shapes for use by this form.
     *
     * @param  config  configuration options
     * @return   reader
     */
    protected abstract MultiPointReader createReader( ConfigMap config );

    public int getBasicPositionCount() {
        return 1;
    }

    public String getFormName() {
        return name_;
    }

    public Icon getFormIcon() {
        return icon_;
    }

    public String getFormDescription() {
        return description_;
    }

    public Coord[] getExtraCoords() {
        return extraCoordSet_.getCoords();
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
        list.add( shapeKey_ );
        list.add( thickKey_ );
        if ( scaleKey_ != null ) {
            list.add( scaleKey_ );
        }
        list.addAll( Arrays.asList( otherKeys_ ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Outliner createOutliner( ConfigMap config ) {
        MultiPointShape shape = config.get( shapeKey_ );
        int nthick = config.get( thickKey_ ).intValue();
        ErrorMode[] errorModes = shapeKey_.getErrorModes();
        double scale = scaleKey_ == null
                     ? 1
                     : config.get( scaleKey_ ).doubleValue();
        return new MultiPointOutliner( createReader( config ),
                                       shape, nthick, errorModes, scale );
    }

    /**
     * Returns XML text suitable for inclusion in a MultiPointForm description
     * explaining how the scaling of marker sizes is controlled.
     *
     * @param   shapename   human-readable name of the shape being plotted
     *                      by this form
     * @return  description text &lt;p&gt; element
     */
    public static String getDefaultScalingDescription( String shapename ) {
        return String.join( "\n",
            "<p>In some cases the supplied data values",
            "give the actual extents in data coordinates",
            "for the plotted " + shapename + "s",
            "but sometimes the data is on a different scale",
            "or in different units to the positional coordinates.",
            "As a convenience for this case, the plotter can optionally",
            "scale the magnitudes of all the " + shapename + "s",
            "to make them a reasonable size on the plot,",
            "so by default the largest ones are a few tens of pixels long.",
            "This auto-scaling is turned off by default,",
            "but it can be activated with the",
            "<code>" + StyleKeys.AUTOSCALE.getMeta().getShortName() + "</code>",
            "option.",
            "Whether autoscaling is on or off, the",
            "<code>" + StyleKeys.SCALE.getMeta().getShortName() + "</code>",
            "option can be used to apply a fixed scaling factor.",
            "</p>"
        );
    }

    /**
     * Creates a config key for line thickness to be used with
     * MultiPointShapes.
     *
     * @param  shapeKey  configured shape to which this relates
     * @return  key for line thickness
     */
    public static ConfigKey<Integer>
            createThicknessKey( MultiPointConfigKey shapeKey ) {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setShortDescription( "Line thickness for "
                                + shapeKey.getMeta().getShortDescription() );
        meta.setXmlDescription( new String[] {
            "<p>Controls the line thickness used when drawing shapes.",
            "Zero, the default value, means a 1-pixel-wide line is used.",
            "Larger values make drawn lines thicker,",
            "but note changing this value will not affect all shapes,",
            "for instance filled rectangles contain no line drawings.",
            "</p>",
        } );
        return StyleKeys.createPaintThicknessKey( meta, 3 );
    }

    /**
     * Returns the column index in a tuple sequence at which the
     * extra (multi-point) coordinates start.
     *
     * @param  geom  data position geometry
     * @return  first non-central position coordinate index
     *          (others follow contiguously)
     */
    public static int getExtrasCoordIndex( DataGeom geom ) {
        return geom.getPosCoords().length;
    }

    /**
     * Outliner implementation for use with MultiPointForms.
     */
    private static class MultiPointOutliner extends PixOutliner {

        private final MultiPointReader reader_;
        private final MultiPointScribe scribe_;
        private final ErrorMode[] modes_;
        private final double scale_;
        private final Icon icon_;
        private final SizeScale sizeScale_;
        private final int nExtra_;

        /**
         * Constructor.
         *
         * @param  reader  data reader
         * @param  shape   shape
         * @param  nthick  line thickness
         * @param  modes   used with shape to define icon
         * @param  scale   scaling adjustment factor
         */
        public MultiPointOutliner( MultiPointReader reader,
                                   MultiPointShape shape, int nthick,
                                   ErrorMode[] modes, double scale ) {
            reader_ = reader;
            scribe_ = shape.createScribe( nthick );
            modes_ = modes;
            scale_ = scale;
            icon_ = shape.getLegendIcon( scribe_, modes, 14, 10, 1, 1 );
            sizeScale_ = new SizeScale( this );
            nExtra_ = reader.getExtraCoordSet().getPointCount();
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<>();
            if ( reader_.isAutoscale() ) {
                map.put( sizeScale_, reader_.createSizeReader( geom ) );
            }
            return map;
        }

        public boolean canPaint( DataSpec dataSpec ) {
            return true;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            int ndim = surface.getDataDimCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nExtra_ ][ ndim ];
            final Point2D.Double gpos0 = new Point2D.Double();
            Span sizeSpan = auxSpans.get( sizeScale_ );
            assert reader_.isAutoscale() == ( sizeSpan != null );
            final MultiPointReader.ExtrasReader reader =
                reader_.createExtrasReader( geom, sizeSpan );
            double scale = scale_ * reader_.getBaseScale( surface, sizeSpan );
            Offsetter offsetter = new Offsetter( surface, nExtra_, scale );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos0 ) &&
                         surface.dataToGraphics( dpos0, true, gpos0 ) &&
                         reader.readPoints( tuple, dpos0, dposExtras ) ) {
                        int[] xoffs = new int[ nExtra_ ];
                        int[] yoffs = new int[ nExtra_ ];
                        offsetter.calculateOffsets( dpos0, gpos0, dposExtras,
                                                    xoffs, yoffs );
                        Glyph glyph = scribe_.createGlyph( xoffs, yoffs);
                        paperType.placeGlyph( paper, gpos0.x, gpos0.y,
                                              glyph, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            int ndim = surface.getDataDimCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nExtra_ ][ ndim ];
            final GPoint3D gpos0 = new GPoint3D();
            Span sizeSpan = auxSpans.get( sizeScale_ );
            assert reader_.isAutoscale() == ( sizeSpan != null );
            final MultiPointReader.ExtrasReader reader =
                reader_.createExtrasReader( geom, sizeSpan );
            double scale = scale_ * reader_.getBaseScale( surface, sizeSpan );
            Offsetter offsetter = new Offsetter( surface, nExtra_, scale );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos0 ) &&
                         surface.dataToGraphicZ( dpos0, true, gpos0 ) &&
                         reader.readPoints( tuple, dpos0, dposExtras ) ) {
                        int[] xoffs = new int[ nExtra_ ];
                        int[] yoffs = new int[ nExtra_ ];
                        offsetter.calculateOffsets( dpos0, gpos0, dposExtras,
                                                    xoffs, yoffs );
                        Glyph glyph = scribe_.createGlyph( xoffs, yoffs);
                        paperType.placeGlyph( paper, gpos0.x, gpos0.y, gpos0.z,
                                              glyph, color );
                    }
                }
            };
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MultiPointOutliner ) {
                MultiPointOutliner other = (MultiPointOutliner) o;
                return this.reader_.equals( other.reader_ )
                    && this.scribe_.equals( other.scribe_ )
                    && Arrays.equals( this.modes_, other.modes_ )
                    && this.scale_ == other.scale_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 3203;
            code = code * 23 + reader_.hashCode();
            code = code * 23 + scribe_.hashCode();
            code = code * 23 + Arrays.hashCode( modes_ );
            code = code * 23 + Float.floatToIntBits( (float) scale_ );
            return code;
        }
    }

    /**
     * Calculates the actual graphics positions at each row
     * for a given multipoint shape.
     */
    private static class Offsetter {
        final Surface surface_;
        final int nextra_;
        final double scale_;
        final Point2D.Double gp_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  nextra  number of non-central data coordinates
         */
        Offsetter( Surface surface, int nextra, double scale ) {
            surface_ = surface;
            nextra_ = nextra;
            scale_ = scale;
            gp_ = new Point2D.Double();
        }

        /**
         * Converts data values read from the tuple sequence into a list
         * of graphics coordinates suitable for feeding to the MultiPointShape.
         * The result is returned by filling a supplied pair of arrays
         * giving X and Y offsets in graphics coordinates from the
         * (supplied) central point, so (0,0) indicates an extra point
         * on top of the central one.
         *
         * @param  dpos0  nDataDim-element array giving coordinates in data
         *                space of the central position
         * @param  gpos0  central position in graphics coordinates 
         * @param  dposExtras   [nExtra][nDataDim]-shaped array containing the
         *                      coordinates in data space of the extra
         *                      (non-central) positions
         * @param  xoffs  nExtra-element array to receive graphics X offsets
         * @param  yoffs  nExtra-element array to receive graphics Y offsets
         */
        void calculateOffsets( double[] dpos0, Point2D.Double gpos0,
                               double[][] dposExtras,
                               int[] xoffs, int[] yoffs ) {
            double gx0 = gpos0.x;
            double gy0 = gpos0.y;
            for ( int ie = 0; ie < nextra_; ie++ ) {
                final int gx;
                final int gy;
                if ( surface_.dataToGraphicsOffset( dpos0, gpos0,
                                                    dposExtras[ ie ], false,
                                                    gp_ ) &&
                     PlotUtil.isPointReal( gp_ ) ) {
                    gx = (int) Math.round( ( gp_.x - gx0 ) * scale_ );
                    gy = (int) Math.round( ( gp_.y - gy0 ) * scale_ );
                }
                else {
                    gx = 0;
                    gy = 0;
                }
                xoffs[ ie ] = gx;
                yoffs[ ie ] = gy;
            }
        }
    }

    /**
     * AuxScale key for calculating multipoint shape size ranges.
     * Currently this is non-shared, but it could be shared for
     * global size ranging if required, since it just looks at the
     * graphics offsets and works out the size from them.
     */
    private static class SizeScale extends AuxScale {
        private final MultiPointOutliner outliner_;

        /**
         * Constructor.
         *
         * @param  outliner  outline drawing object
         */
        SizeScale( MultiPointOutliner outliner ) {
            super( "autosize" );
            outliner_ = outliner;
        }

        @Override
        public int hashCode() {
            return outliner_.hashCode();
        }

        @Override
        public boolean equals( Object other ) {
            return other instanceof SizeScale
                && this.outliner_.equals( ((SizeScale) other).outliner_ );
        }
    }
}
