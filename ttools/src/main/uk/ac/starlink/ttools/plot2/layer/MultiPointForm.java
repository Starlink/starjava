package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.Icon;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
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
    private final ConfigKey<Integer> thickKey_;
    private final ConfigKey<?>[] otherKeys_;

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
     * @param  otherKeys    additional config keys
     */
    public MultiPointForm( String name, Icon icon, String description,
                           MultiPointCoordSet extraCoordSet,
                           MultiPointConfigKey shapeKey,
                           ConfigKey<?>[] otherKeys ) {
        name_ = name;
        icon_ = icon;
        description_ = description;
        extraCoordSet_ = extraCoordSet;
        shapeKey_ = shapeKey;
        thickKey_ = createThicknessKey( shapeKey_ );
        otherKeys_ = otherKeys;
    }

    /**
     * Returns a fixed constant by which to scale all (autoscaled or not
     * autoscaled) offset values before plotting.
     *
     * @param  config  config map
     * @return  constant scaling factor
     */
    protected abstract double getScaleFactor( ConfigMap config );

    /**
     * Indicates whether autoscaling should be applied.
     * If true, before plotting is carried out a scan of all the
     * data values is performed to determine the range of values,
     * and the supplied offsets are scaled accordingly,
     * so that the largest ones are a reasonable size on the screen.
     *
     * @param  config  config map
     * @return   true for autoscaling false to use raw values
     */
    protected abstract boolean isAutoscale( ConfigMap config );

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
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.add( shapeKey_ );
        list.add( thickKey_ );
        list.addAll( Arrays.asList( otherKeys_ ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Outliner createOutliner( ConfigMap config ) {
        MultiPointShape shape = config.get( shapeKey_ );
        int nthick = config.get( thickKey_ ).intValue();
        ErrorMode[] errorModes = shapeKey_.getErrorModes();
        double scale = getScaleFactor( config );
        boolean isAutoscale = isAutoscale( config );
        return new MultiPointOutliner( shape, nthick, errorModes,
                                       scale, isAutoscale );
    }

    /**
     * Returns a MultiPointForm instance for drawing arrows from the
     * central position to another position.
     *
     * @param  name  form name
     * @param  extraCoordSet  nDataDim-element coord set that defines one
     *                        extra data position, the (unscaled) endpoint
     *                        of the vector
     * @param  canScale  whether to offer vector size scaling
     * @return  new vector form instance
     */
    public static MultiPointForm
                  createVectorForm( String name,
                                    MultiPointCoordSet extraCoordSet,
                                    boolean canScale ) {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots directed lines from the data position",
            "given delta values for the coordinates.",
            "The plotted markers are typically little arrows,",
            "but there are other options.",
            "</p>",
        } );
        if ( canScale ) {
            descrip += getDefaultScalingDescription( "vector" );
        }
        return createDefaultForm( name, ResourceIcon.FORM_VECTOR, descrip,
                                  extraCoordSet, StyleKeys.VECTOR_SHAPE,
                                  canScale );
    }

    /**
     * Returns XML text suitable for inclusion in a MultiPointForm description
     * explaining how the scaling of marker sizes is controlled.
     * This corresponds to the behaviour of the
     * {@link #createDefaultForm createDefaultForm} method.
     *
     * @param   shapename   human-readable name of the shape being plotted
     *                      by this form
     * @return  description text &lt;p&gt; element
     */
    public static String getDefaultScalingDescription( String shapename ) {
        return PlotUtil.concatLines( new String[] {
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
            "</p>",
        } );
    }

    /**
     * Returns a MultiPointForm for drawing error bars.
     *
     * @param  name  form name
     * @param  extraCoordSet  coord set specifying error bar position endpoints
     * @param  shapeKey   config key for specifying multipoint shape
     * @return  new error form instance
     */
    public static MultiPointForm
                  createErrorForm( String name,
                                   MultiPointCoordSet extraCoordSet,
                                   MultiPointConfigKey shapeKey ) {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots symmetric or asymmetric error bars in some or",
            "all of the plot dimensions.",
            "The shape of the error \"bars\" is quite configurable,",
            "including (for 2-d and 3-d errors)",
            "ellipses, rectangles etc aligned with the axes.",
            "</p>",
        } );
        return createDefaultForm( name, ResourceIcon.FORM_ERROR, descrip,
                                  extraCoordSet, shapeKey, false );
    }

    /**
     * Returns a new MultiPointForm with scaling in one of two default
     * configurations, depending on the value of the supplied canScale
     * parameter.  If true, then the StyleKeys SCALE and AUTOSCALE keys
     * are used to configure scaling, and if false, no scaling is provided.
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
     * @param  canScale   true for standard scaling configuration,
     *                    false for no scaling
     */
    public static MultiPointForm
            createDefaultForm( String name, Icon icon, String description, 
                               MultiPointCoordSet extraCoordSet,
                               MultiPointConfigKey shapeKey,
                               boolean canScale ) {
        if ( canScale ) {
            return new MultiPointForm( name, icon, description, extraCoordSet,
                                       shapeKey, new ConfigKey<?>[] {
                                           StyleKeys.SCALE, StyleKeys.AUTOSCALE,
                                       } ) {
                protected double getScaleFactor( ConfigMap config ) {
                    return config.get( StyleKeys.SCALE );
                }
                protected boolean isAutoscale( ConfigMap config ) {
                    return config.get( StyleKeys.AUTOSCALE );
                }
            };
        }
        else {
            return new MultiPointForm( name, icon, description, extraCoordSet,
                                       shapeKey, new ConfigKey<?>[ 0 ] ) {
                protected double getScaleFactor( ConfigMap config ) {
                    return 1;
                }
                protected boolean isAutoscale( ConfigMap config ) {
                    return false;
                }
            };
        }
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
    private static int getExtrasCoordIndex( DataGeom geom ) {
        return geom.getPosCoords().length;
    }

    /**
     * Outliner implementation for use with MultiPointForms.
     */
    private class MultiPointOutliner extends PixOutliner {
        private final MultiPointScribe scribe_;
        private final ErrorMode[] modes_;
        private final double scale_;
        private final boolean isAutoscale_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape   shape
         * @param  nthick  line thickness
         * @param  modes   used with shape to define icon
         * @param  scale   scaling adjustment factor
         * @param  isAutoscale  true if initial size scaling is done
         *                      from the data
         */
        public MultiPointOutliner( MultiPointShape shape, int nthick,
                                   ErrorMode[] modes, double scale,
                                   boolean isAutoscale ) {
            scribe_ = shape.createScribe( nthick );
            modes_ = modes;
            scale_ = scale;
            isAutoscale_ = isAutoscale;
            icon_ = shape.getLegendIcon( scribe_, modes, 14, 10, 1, 1 );
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            if ( isAutoscale_ ) {
                SizeScale scale = new SizeScale( this );
                map.put( scale, scale.createAuxReader( geom, extraCoordSet_ ) );
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
            final int nextra = extraCoordSet_.getPointCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nextra ][ ndim ];
            final Point2D.Double gpos0 = new Point2D.Double();
            final int icExtra = getExtrasCoordIndex( geom );
            double scale = scale_ * getBaseScale( surface, auxSpans );
            final Offsetter offsetter = createOffsetter( surface, scale );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos0 ) &&
                         surface.dataToGraphics( dpos0, true, gpos0 ) &&
                         extraCoordSet_.readPoints( tuple, icExtra, geom,
                                                    dpos0, dposExtras ) ) {
                        int[] xoffs = new int[ nextra ];
                        int[] yoffs = new int[ nextra ];
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
            final int nextra = extraCoordSet_.getPointCount();
            final double[] dpos0 = new double[ ndim ];
            final double[][] dposExtras = new double[ nextra ][ ndim ];
            final GPoint3D gpos0 = new GPoint3D();
            final int icExtra = getExtrasCoordIndex( geom );
            double scale = scale_ * getBaseScale( surface, auxSpans );
            final Offsetter offsetter = createOffsetter( surface, scale );
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos0 ) &&
                         surface.dataToGraphicZ( dpos0, true, gpos0 ) &&
                         extraCoordSet_.readPoints( tuple, icExtra, geom,
                                                    dpos0, dposExtras ) ) {
                        int[] xoffs = new int[ nextra ];
                        int[] yoffs = new int[ nextra ];
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
                return this.scribe_.equals( other.scribe_ )
                    && Arrays.equals( this.modes_, other.modes_ )
                    && this.isAutoscale_ == other.isAutoscale_
                    && this.scale_ == other.scale_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 3203;
            code = code * 23 + scribe_.hashCode();
            code = code * 23 + Arrays.hashCode( modes_ );
            code = code * 23 + ( isAutoscale_ ? 11 : 13 );
            code = code * 23 + Float.floatToIntBits( (float) scale_ );
            return code;
        }

        /**
         * Returns the base size scaling value.
         * Manual adjustment may be applied on top of this value.
         *
         * @param   surface  plot surface
         * @param   auxSpans  ranges calculated from data by request
         */
        private double getBaseScale( Surface surface,
                                     Map<AuxScale,Span> auxSpans ) {

            /* If no autoscale, just return 1. */
            if ( ! isAutoscale_ ) {
                return 1;
            }

            /* Otherwise, pick a scale so that the largest sized shape
             * painted will be a few tens of pixels long. */
            Span sizeSpan = auxSpans.get( new SizeScale( this ) );
            if ( sizeSpan != null ) {
                double[] bounds = sizeSpan.getFiniteBounds( false );
                double gmax = Math.max( -bounds[ 0 ], +bounds[ 1 ] );
                assert gmax >= 0;
                return gmax == 0 ? 1 : 32 / gmax;
            }
            else {
                assert false;
                return 1;
            }
        }

        /**
         * Returns an object that can calculate the actual graphics offset
         * positions for each data point.
         *
         * @param  surface  plot surface
         * @param  scale   scaling factor
         */
        private Offsetter createOffsetter( Surface surface,
                                           final double scale ) {
            int nextra = extraCoordSet_.getPointCount();
            return new Offsetter( surface, nextra, scale );
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
        private final boolean scaleFromVisible_;

        /**
         * Constructor.
         *
         * @param  outliner  outline drawing object
         */
        SizeScale( MultiPointOutliner outliner ) {
            super( "autosize" );
            outliner_ = outliner;
            scaleFromVisible_ = true;
        }

        AuxReader createAuxReader( final DataGeom geom,
                                   final MultiPointCoordSet extraCoordSet ) {
            final int ndim = geom.getDataDimCount();
            final int nextra = extraCoordSet.getPointCount();
            final int icExtra = getExtrasCoordIndex( geom );
            return new AuxReader() {
                public int getCoordIndex() {
                    return -1;
                }
                public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                    return null;
                }
                public Scaling getScaling() {
                    return null;
                }
                public void adjustAuxRange( final Surface surface,
                                            DataSpec dataSpec,
                                            DataStore dataStore, Object[] plans,
                                            Ranger ranger ) {
                    BiConsumer<TupleSequence,Ranger> rangeFiller =
                        (tseq, r) -> fillRange( tseq, r, surface );
                    dataStore.getTupleRunner()
                             .rangeData( rangeFiller, ranger,
                                         dataSpec, dataStore );
                }
                private void fillRange( TupleSequence tseq, Ranger ranger,
                                        Surface surface ) {
                    double[] dpos0 = new double[ ndim ];
                    double[][] dposExtras = new double[ nextra ][ ndim ];
                    Point2D.Double gpos0 = new Point2D.Double();
                    Point2D.Double gpos1 = new Point2D.Double();
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, 0, dpos0 ) &&
                             surface.dataToGraphics( dpos0, scaleFromVisible_,
                                                     gpos0 ) &&
                             PlotUtil.isPointFinite( gpos0 ) &&
                             extraCoordSet.readPoints( tseq, icExtra, geom,
                                                       dpos0, dposExtras ) ) {
                            for ( int ie = 0; ie < nextra; ie++ ) {
                                if ( surface
                                    .dataToGraphicsOffset( dpos0, gpos0,
                                                           dposExtras[ ie ],
                                                           false, gpos1 ) &&
                                    PlotUtil.isPointFinite( gpos1 ) ) {
                                    ranger.submitDatum( gpos1.x - gpos0.x );
                                    ranger.submitDatum( gpos1.y - gpos0.y );
                                }
                            }
                        }
                    }
                }
            };
        }

        public int hashCode() {
            return outliner_.hashCode();
        }

        public boolean equals( Object other ) {
            return other instanceof SizeScale
                && this.outliner_.equals( ((SizeScale) other).outliner_ );
        }
    }
}
