package uk.ac.starlink.ttools.plot2.layer;

import java.awt.geom.Point2D;
import java.util.function.BiConsumer;
import javax.swing.Icon;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * MultiPointForm for use with Cartesian coordinates.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2023
 */
public class CartesianMultiPointForm extends MultiPointForm {

    private final CartesianMultiPointCoordSet extraCoordSet_;
    private final boolean canScale_;

    /**
     * Constructs a new MultiPointForm with scaling in one of two default
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
    public CartesianMultiPointForm( String name, Icon icon, String description,
                                    CartesianMultiPointCoordSet extraCoordSet,
                                    MultiPointConfigKey shapeKey,
                                    boolean canScale ) {
        super( name, icon, description, extraCoordSet, shapeKey,
               canScale ? StyleKeys.SCALE : null,
               canScale ? new ConfigKey<?>[] { StyleKeys.AUTOSCALE }
                        : new ConfigKey<?>[ 0 ] );
        extraCoordSet_ = extraCoordSet;
        canScale_ = canScale;
    }

    protected MultiPointReader createReader( ConfigMap config ) {
        final boolean isAutoscale =
              canScale_ && config.get( StyleKeys.AUTOSCALE ).booleanValue();
        return new CartesianMultiPointReader( extraCoordSet_, isAutoscale );
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
                                   CartesianMultiPointCoordSet extraCoordSet,
                                   MultiPointConfigKey shapeKey ) {
        String descrip = String.join( "\n",
            "<p>Plots symmetric or asymmetric error bars in some or",
            "all of the plot dimensions.",
            "The shape of the error \"bars\" is quite configurable,",
            "including (for 2-d and 3-d errors)",
            "ellipses, rectangles etc aligned with the axes.",
            "</p>"
        );
        return new CartesianMultiPointForm( name, ResourceIcon.FORM_ERROR,
                                            descrip, extraCoordSet, shapeKey,
                                            false );
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
                                    CartesianMultiPointCoordSet extraCoordSet,
                                    boolean canScale ) {
        String descrip = String.join( "\n",
            "<p>Plots directed lines from the data position",
            "given delta values for the coordinates.",
            "The plotted markers are typically little arrows,",
            "but there are other options.",
            "</p>"
        );
        if ( canScale ) {
            descrip += getDefaultScalingDescription( "vector" );
        }
        return new CartesianMultiPointForm( name, ResourceIcon.FORM_VECTOR,
                                            descrip, extraCoordSet,
                                            StyleKeys.VECTOR_SHAPE, canScale );
    }

    /**
     * Reader implementation for use with CartesianMultiPointForm.
     */
    private static class CartesianMultiPointReader implements MultiPointReader {

        private final CartesianMultiPointCoordSet extraCoordSet_;
        private final boolean isAutoscale_;

        /**
         * Constructor.
         *
         * @param  extraCoordSet  coord set describing shape
         * @param  isAutoscale  true for autoscaling of shape sizes,
         *                      false for absolute sizes
         */
        CartesianMultiPointReader( CartesianMultiPointCoordSet extraCoordSet,
                                   boolean isAutoscale ) {
            extraCoordSet_ = extraCoordSet;
            isAutoscale_ = isAutoscale;
        }

        public boolean isAutoscale() {
            return isAutoscale_;
        }

        public CartesianMultiPointCoordSet getExtraCoordSet() {
            return extraCoordSet_;
        }

        public double getBaseScale( Surface surface, Span sizeSpan ) {

            /* If no autoscale, just return 1. */
            if ( sizeSpan == null ) {
                return 1;
            }

            /* Otherwise, pick a scale so that the largest sized shape
             * painted will be a few tens of pixels long. */
            else {
                double[] bounds = sizeSpan.getFiniteBounds( false );
                double gmax = Math.max( -bounds[ 0 ], +bounds[ 1 ] );
                assert gmax >= 0;
                return gmax == 0 ? 1 : AUTOSCALE_PIXELS / gmax;
            }
        }

        public ExtrasReader createExtrasReader( DataGeom geom, Span sizeSpan ) {
            final int icExtra = getExtrasCoordIndex( geom );
            return new ExtrasReader() {
                public boolean readPoints( Tuple tuple, double[] dpos0,
                                           double[][] dposExtras ) {
                    return extraCoordSet_
                          .readPoints( tuple, icExtra, dpos0, dposExtras );
                }
            };
        }

        public AuxReader createSizeReader( DataGeom geom ) {
            final boolean scaleFromVisible = true;
            Span dummySpan = null;
            final ExtrasReader extrasReader =
                createExtrasReader( geom, dummySpan );
            final int ndim = geom.getDataDimCount();
            final int nextra = extraCoordSet_.getPointCount();
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
                             surface.dataToGraphics( dpos0, scaleFromVisible,
                                                     gpos0 ) &&
                             PlotUtil.isPointFinite( gpos0 ) &&
                             extrasReader.readPoints( tseq, dpos0,
                                                      dposExtras ) ) {
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

        @Override
        public int hashCode() {
            int code = 88165;
            code = 23 * extraCoordSet_.hashCode();
            code = 23 * ( isAutoscale_ ? 23 : 27 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CartesianMultiPointReader ) {
                CartesianMultiPointReader other = (CartesianMultiPointReader) o;
                return this.extraCoordSet_.equals( other.extraCoordSet_ )
                    && this.isAutoscale_ == other.isAutoscale_;
            }
            else {
                return false;
            }
        }
    }
}
