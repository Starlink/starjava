package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Form for drawing an array of error bars per row, given array-valued
 * X, Y and X/Y error offsets.  Currently only supports X-Y plotting.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2021
 */
public class ErrorArrayForm implements ShapeForm {

    private final boolean hasX_;
    private final Coord[] extraCoords_;
    private final FloatingArrayCoord xsCoord_;
    private final FloatingArrayCoord ysCoord_;
    private final FloatingArrayCoord xsPosCoord_;
    private final FloatingArrayCoord ysPosCoord_;
    private final FloatingArrayCoord xsNegCoord_;
    private final FloatingArrayCoord ysNegCoord_;
    private final int icXs_;
    private final int icYs_;
    private final int icPxs_;
    private final int icNxs_;
    private final int icPys_;
    private final int icNys_;
    private final MultiPointConfigKey shapeKey_;
    private final ConfigKey<Integer> thickKey_;

    /** ErrorArrayForm instance for Y-only error bars. */
    public static final ErrorArrayForm Y = new ErrorArrayForm( false );

    /** ErrorArrayForm instance for X, Y error bars. */
    public static final ErrorArrayForm XY = new ErrorArrayForm( true );

    /**
     * Constructor.
     *
     * @param  hasX  whether error bars on the X axis are provided
     *               as well as on the Y axis
     */
    protected ErrorArrayForm( boolean hasX ) {
        hasX_ = hasX;
        xsCoord_ = FloatingArrayCoord.X;
        ysCoord_ = FloatingArrayCoord.Y;
        int ic = 0;
        icXs_ = ic++;
        icYs_ = ic++;
        List<Coord> extraCoords = new ArrayList<>();
        if ( hasX ) {
            extraCoords.add( xsPosCoord_ = createErrorsCoord( "X", true ) );
            extraCoords.add( xsNegCoord_ = createErrorsCoord( "X", false ) );
            icPxs_ = ic++;
            icNxs_ = ic++;
        }
        else {
            xsPosCoord_ = null;
            xsNegCoord_ = null;
            icPxs_ = -1;
            icNxs_ = -1;
        }
        extraCoords.add( ysPosCoord_ = createErrorsCoord( "Y", true ) );
        extraCoords.add( ysNegCoord_ = createErrorsCoord( "Y", false ) );
        icPys_ = ic++;
        icNys_ = ic++;
        extraCoords_ = extraCoords.toArray( new Coord[ 0 ] );
        assert ic == extraCoords_.length + 2;
        shapeKey_ = hasX ? StyleKeys.ERROR_SHAPE_2D : StyleKeys.ERROR_SHAPE_1D;
        thickKey_ = MultiPointForm.createThicknessKey( shapeKey_ );
    }

    public int getBasicPositionCount() {
        return 0;
    }

    public String getFormName() {
        return hasX_ ? "XYErrors" : "YErrors";
    }

    public String getFormDescription() {
        return String.join( "\n",
            "<p>Plots <em>N</em> error bars in the",
            ( hasX_ ? "X and Y directions"
                    : "Y direction" ),
            "for each input row,",
            "with the X, Y and error bar extents each supplied",
            "by <em>N</em>-element array values.",
            "</p>",
            ""
        );
    }

    public Icon getFormIcon() {
        return hasX_ ? ResourceIcon.FORM_ERROR
                     : ResourceIcon.FORM_ERROR1;
    }

    public Coord[] getExtraCoords() {
        return extraCoords_.clone();
    }

    public int getExtraPositionCount() {
        return 0;
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            shapeKey_,
            thickKey_,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        MultiPointShape shape = config.get( shapeKey_ );
        int nthick = config.get( thickKey_ ).intValue();
        return new ErrorsOutliner( shape, nthick );
    }

    /**
     * Returns a glyph depicting a particular error bar.
     *
     * @param   surface  plot surface
     * @param   scribe   error shape
     * @param   dpos0   base position in data coordinates
     * @param   gpos0   base position in graphics coordinates
     * @param   ip      index into value arrays giving point/error coordinates
     * @param   xsPos   positive direction X error bar extent array, or null
     * @param   ysPos   positive direction Y error bar extent array
     * @param   xsNeg   negative direction X error bar extent array, or null
     * @param   ysNeg   negative direction Y error bar extent array
     * @return   glyph depicting errors
     */
    private Glyph createErrorGlyph( Surface surface, MultiPointScribe scribe,
                                    double[] dpos0, Point2D.Double gpos0,
                                    int ip, 
                                    double[] xsPos, double[] ysPos,
                                    double[] xsNeg, double[] ysNeg ) {

        /* Prepare data coordinate arrays corresponding to the points at
         * the ends of the error bars. */
        double dx0 = dpos0[ 0 ];
        double dy0 = dpos0[ 1 ];
        final double[][] dp1s;
        if ( ! hasX_ ) {
            double[] yerrs = getErrors( ysPos, ysNeg, ip );
            if ( yerrs != null ) {
                dp1s = new double[][] {
                    { dx0, dy0 + yerrs[ 0 ] },
                    { dx0, dy0 - yerrs[ 1 ] },
                };
            }
            else {
                return null;
            }
        }
        else {
            double[] xerrs = getErrors( xsPos, xsNeg, ip );
            double[] yerrs = getErrors( ysPos, ysNeg, ip );
            if ( xerrs != null && yerrs != null ) {
                dp1s = new double[][] {
                    { dx0, dy0 + yerrs[ 0 ] },
                    { dx0, dy0 - yerrs[ 1 ] },
                    { dx0 + xerrs[ 0 ], dy0 },
                    { dx0 - xerrs[ 1 ], dy0 },
                };
            }
            else if ( yerrs != null ) {
                dp1s = new double[][] {
                    { dx0, dy0 + yerrs[ 0 ] },
                    { dx0, dy0 - yerrs[ 1 ] },
                };
            }
            else if ( xerrs != null ) {
                dp1s = new double[][] {
                    { dx0 + xerrs[ 0 ], dy0 },
                    { dx0 - xerrs[ 1 ], dy0 },
                };
            }
            else {
                return null;
            }
        }

        /* Return a glyph based on these positions. */
        return createErrorGlyph( surface, dpos0, gpos0, dp1s, scribe );
    }

    /**
     * Returns a glyph depicting an error bar for given end point
     * positions.
     *
     * @param   surface  plot surface
     * @param   dpos0   base position in data coordinates
     * @param   gpos0   base position in graphics coordinates
     * @param   dp1s    2*n positions giving the end positions of error bars
     * @param   scribe  error shape
     * @return   glyph depicting errors
     */
    private static Glyph createErrorGlyph( Surface surface,
                                           double[] dpos0, Point2D.Double gpos0,
                                           double[][] dp1s,
                                           MultiPointScribe scribe ) {
        int np = dp1s.length;
        int[] xoffs = new int[ np ];
        int[] yoffs = new int[ np ];
        Point2D.Double gpos1 = new Point2D.Double();
        for ( int ip = 0; ip < np; ip++ ) {
            if ( ! ( surface.dataToGraphicsOffset( dpos0, gpos0, dp1s[ ip ],
                                                   false, gpos1 ) &&
                     PlotUtil.isPointReal( gpos1 ) ) ) {
                return null;
            }
            xoffs[ ip ] = (int) Math.round( gpos1.x - gpos0.x );
            yoffs[ ip ] = (int) Math.round( gpos1.y - gpos0.y );
        }
        return scribe.createGlyph( xoffs, yoffs );
    }

    /**
     * Returns the positive and negative error extents corresponding to
     * input error value array elements.
     *
     * @param   posErrs   array of error extents in the positive direction
     * @param   negErrs   array of error extents in the negative direction
     * @param   ip    array index
     * @return   2-element array giving (negative, positive) error bar extents,
     *           or null for no error bars; if non-null, neither of the
     *           elements will be negative, and at least one will be positive
     */
    private double[] getErrors( double[] posErrs, double[] negErrs, int ip ) {
        if ( posErrs.length == 0 ) {
            return null;
        }
        double pErr = posErrs[ ip ];
        if ( Double.isNaN( pErr ) ) {
            return null;
        }

        /* If the negative error extents are missing, they are assumed to
         * take the same value as the positive ones. */
        final double mErr;
        if ( negErrs.length > 0 ) {
            double me = negErrs[ ip ];
            mErr = Double.isNaN( me ) ? pErr : me;
        }
        else {
            mErr = pErr;
        }
        return pErr > 0 || mErr > 0 ? new double[] { pErr, mErr } : null;
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
     * Returns a Coord specifier representing error extents.
     * Negative values may be missing, in which case they take their
     * values from the positive one.
     *
     * @param  axId  capitalised short (1-character?) axis identifier
     * @param  isPositive  true for positive error extent,
     *                     false for negative error extent
     * @return  array-valued coord
     */
    private static FloatingArrayCoord createErrorsCoord( String axId,
                                                         boolean isPositive ) {
        String axid = axId.toLowerCase();
        InputMeta meta =
            new InputMeta( axid + ( isPositive ? "errhis" : "errlos" ),
                           axId + ( isPositive ? " Positive" : " Negative" )
                                + " Errors" );
        meta.setValueUsage( "array" );
        meta.setShortDescription( "Errors in " + axId + " "
                                + ( isPositive ? "positive" : "negative" )
                                + " direction" );
        final String xmlDesc;
        if ( isPositive ) {
            xmlDesc = String.join( "\n",
                "<p>Array of errors in the " + axId + " coordinates",
                "in the positive direction.",
                "If no corresponding negative value is supplied,",
                "then this value is also used in the negative direction,", 
                "i.e. in that case errors are assumed to be symmetric.",
                "Error exents must be positive; negative array elements",
                "are ignored.",
                "</p>",
                ""
            );
        }
        else {
            xmlDesc = String.join( "\n",
                "<p>Array of errors in the " + axId + " coordinates",
                "in the negative direction.",
                "If left blank, it is assumed to take the same value",
                "as in the positive direction.",
                "Error extents must be positive; negative array elements",
                "are ignored.",
                "</p>",
                ""
            );
        }
        meta.setXmlDescription( xmlDesc );
        return FloatingArrayCoord.createCoord( meta, false );
    }

    /**
     * Outliner implementation for ErrorArrayForm.
     */
    private class ErrorsOutliner extends PixOutliner {
        private final MultiPointScribe scribe_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape  knows how to draw shape
         * @param  nthick  line drawing thickness
         */
        ErrorsOutliner( MultiPointShape shape, int nthick ) {
            scribe_ = shape.createScribe( nthick );
            ErrorMode[] emodes = {
                hasX_ ? ErrorMode.SYMMETRIC : ErrorMode.NONE,
                ErrorMode.SYMMETRIC,
            };
            icon_ = shape.getLegendIcon( scribe_, emodes, 14, 12, 1, 1 );
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
            final Function<Tuple,XYArrayData> xyReader =
                createXYArrayReader( dataSpec );

            /* There are possibilities to improve efficiency here,
             * by setting up a calculation object to re-use workspace
             * instead of allocating new workspace for every row or
             * every point. */
            return new ShapePainter() {
                final double[] dpos0 = new double[ 2 ];
                final Point2D.Double gpos0 = new Point2D.Double();
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    XYArrayData xyData = xyReader.apply( tuple );
                    if ( xyData != null ) {
                        int np = xyData.getLength();
                        double[] xsPos =
                            hasX_ ? xsPosCoord_.readArrayCoord( tuple, icPxs_ )
                                  : null;
                        double[] ysPos =
                            ysPosCoord_.readArrayCoord( tuple, icPys_);
                        double[] xsNeg =
                            hasX_ ? xsNegCoord_.readArrayCoord( tuple, icNxs_ )
                                  : null;
                        double[] ysNeg =
                            ysNegCoord_.readArrayCoord( tuple, icNys_);
                        if ( ( ( xsPos != null && xsPos.length == np ) ||
                               ( ysPos != null && ysPos.length == np ) ) ) {
                            for ( int ip = 0; ip < np; ip++ ) {
                                dpos0[ 0 ] = xyData.getX( ip );
                                dpos0[ 1 ] = xyData.getY( ip );
                                if ( surface
                                    .dataToGraphics( dpos0, true, gpos0 ) ) {
                                    Glyph glyph =
                                        createErrorGlyph( surface, scribe_,
                                                          dpos0, gpos0,
                                                          ip,
                                                          xsPos, ysPos,
                                                          xsNeg, ysNeg );
                                    if ( glyph != null ) {
                                        paperType.placeGlyph( paper,
                                                              gpos0.x, gpos0.y,
                                                              glyph, color );
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        /**
         * @throws  UnsupportedOperationException
         */
        public ShapePainter create3DPainter( CubeSurface surface, DataGeom geom,
                                             DataSpec dataSpec,
                                             Map<AuxScale,Span> auxSpans,
                                             PaperType3D paperType ) {
            throw new UnsupportedOperationException( "No 3D" );
        }

        @Override
        public int hashCode() {
            int code = 53169;
            code = 23 * code + scribe_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ErrorsOutliner ) {
                ErrorsOutliner other = (ErrorsOutliner) o;
                return this.scribe_.equals( other.scribe_ );
            }
            else {
                return false;
            }
        }
    }
}
