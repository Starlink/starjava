package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;

/**
 * Ganger for use with a square matrix of Plane plots.
 * Histogram-like plots are expected on the diagonal, and scatter-plot
 * like plots on the off-diagonal cells.
 * For a matrix of linear size N, there are only N spatial coordinates,
 * all the X coordinates in the same column and all the Y coordinates
 * in the same row are the same, with the exception of the count-dimension
 * of diagonal (histogram-like) cells.
 *
 * @author   Mark Taylor
 * @since    1 Jun 2023
 */
public class MatrixGanger
        implements Ganger<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private final MatrixShape shape_;
    private final Padding padding_;
    private final boolean isSquares_;
    private final int cellGap_;
    private final int cxlo_;
    private final int cxhi_;
    private final int cylo_;
    private final int cyhi_;
    private static final int PAD = PlotPlacement.PAD;

    /**
     * True if the histogram-like plots on the diagonal are considered
     * to share their X coordinate with other cells in the same column;
     * false if they are considered to share their Y coordinate with
     * other cells in the same row.  Has to be true, since known
     * histogram-like layers assume the data coordinate is horizontal.
     */
    public static final boolean XDIAG = true;

    /**
     * True if the matrix Y coordinate increases in the upwards direction,
     * false if it increases in the downwards direction.
     */
    private static final boolean YUP = false;

    /**
     * Constructor.
     *
     * @param  shape   defines which cells contain plots
     * @param  padding   padding round the outside of the gang
     * @param  isSquares   true if all zones are constrained to be square
     * @param  cellGap    gap in pixels between grid cells
     */
    public MatrixGanger( MatrixShape shape, Padding padding,
                         boolean isSquares, int cellGap ) {
        shape_ = shape;
        padding_ = padding;
        isSquares_ = isSquares;
        cellGap_ = cellGap;
        int nw = shape.getWidth();
        int cxmin = nw;
        int cxmax = -1;
        int cymin = nw;
        int cymax = -1;
        for ( MatrixShape.Cell cell : shape_ ) {
            int x = cell.getX();
            int y = cell.getY();
            cxmin = Math.min( cxmin, x );
            cxmax = Math.max( cxmax, x );
            cymin = Math.min( cymin, y );
            cymax = Math.max( cymax, y );
        }
        cxlo_ = cxmin;
        cxhi_ = cxmax + 1;
        cylo_ = cymin;
        cyhi_ = cymax + 1;
    }

    /**
     * Returns the shape used by this ganger.
     *
     * @return  matrix shape
     */
    public MatrixShape getShape() {
        return shape_;
    }

    public int getZoneCount() {
        return shape_.getCellCount();
    }

    public boolean isTrimmingGlobal() {
        return true;
    }

    public boolean isShadingGlobal() {
        return true;
    }

    public Gang createGang( Rectangle[] zonePlotBounds ) {
        return new MatrixGang( zonePlotBounds );
    }

    public Gang createApproxGang( Rectangle gangExtBox ) {
        Insets insets = Padding.padInsets( padding_, new Insets( 0, 0, 0, 0 ) );
        return createGang( PlotUtil.subtractInsets( gangExtBox, insets ) );
    }

    public Gang createGang( Rectangle gangExtBox,
                            SurfaceFactory<PlaneSurfaceFactory.Profile,
                                           PlaneAspect> surfFact,
                            ZoneContent<PlaneSurfaceFactory.Profile,
                                        PlaneAspect>[] zoneContents,
                            Trimming[] trimmings, ShadeAxis[] shadeAxes,
                            boolean withScroll ) {

        /* Calculate surrounding space required for decorations that
         * do not depend (much) on the size of the plot bounds. */
        ZoneContent<PlaneSurfaceFactory.Profile,PlaneAspect> zc0 =
            zoneContents.length > 0 ? zoneContents[ 0 ] : null;
        Supplier<Captioner> captSupplier =
              zc0 == null
            ? () -> null
            : () -> surfFact.createSurface( gangExtBox, zc0.getProfile(),
                                            zc0.getAspect() )
                            .getCaptioner();
        Surround decSurround =
            PlotPlacement
           .calculateApproxDecorationSurround( gangExtBox, trimmings[ 0 ],
                                               shadeAxes[ 0 ], captSupplier );
        Rectangle decBox =
            PlotUtil.subtractInsets( gangExtBox, decSurround.toInsets() );

        /* Calculate how much space is required for axis labels. */
        Surround axisSurround =
            calculateAxisSurround( decBox, surfFact, zoneContents );

        /* When scrolling, ignore the under/overhang parts of the
         * Surround, since they can cause internal realignment
         * of the grid when some of the zones are scrolled,
         * which can lead to annoying jiggling.
         * The downside is that some of the axis labelling in the
         * overhang regions may get clipped. */
        Insets axisInsets = withScroll ? axisSurround.toExtentInsets()
                                       : axisSurround.toInsets();
        Insets gangInsets = Padding.padInsets( padding_, axisInsets );
        return createGang( PlotUtil.subtractInsets( decBox, gangInsets ) );
    }

    public PlaneAspect[] adjustAspects( PlaneAspect[] aspects, int iz0 ) {
        assert aspects.length == shape_.getCellCount();
        int nw = shape_.getWidth();
        int nz = shape_.getCellCount();
        Extent[] xextents = new Extent[ nw ];
        Extent[] yextents = new Extent[ nw ];
        final PlaneAspect aspect0;
        final int x0;
        final int y0;
        if ( iz0 >= 0 ) {
            aspect0 = aspects[ iz0 ];
            MatrixShape.Cell cell0 = shape_.getCell( iz0 );
            x0 = cell0.getX();
            y0 = cell0.getY();
        }
        else {
            aspect0 = null;
            x0 = -1;
            y0 = -1;
        }

        /* We need to ensure that the X and Y ranges of each cell are
         * mutually consistent; there are only N distinct coordinates,
         * and each one should have a single range.  Each one appears
         * as the X axis of all the cells in column I and the Y axis
         * of all cells in row I.
         * Diagonal elements are a special case, since one of the
         * coordinates is not spatial, it represents counts in a histogram
         * or something similar, so ignore those axes.
         * First Record a representative range for each one, possibly
         * corresponding to the designated refernce cell. */
        for ( int i = 0; i < nw; i++ ) {
            Extent xextent;
            Extent yextent;
            if ( i == x0 && ( x0 != y0 || XDIAG ) ) {
                xextent = getExtent( aspect0, false );
                yextent = xextent;
            }
            else if ( i == y0 && ( x0 != y0 || !XDIAG ) ) {
                yextent = getExtent( aspect0, true );
                xextent = yextent;
            }
            else {
                xextent = getExtent( getLineAspects( aspects, i, false ),
                                     false );
                yextent = getExtent( getLineAspects( aspects, i, true ),
                                     true );
            }
            xextents[ i ] = xextent;
            yextents[ i ] = yextent;
        }

        /* Now force the ranges for all the other corresponding axes
         * to be the same as the representative one we have recorded
         * in each case. */
        PlaneAspect[] newAspects = new PlaneAspect[ nz ];
        for ( int iz = 0; iz < nz; iz++ ) {
            MatrixShape.Cell cell = shape_.getCell( iz );
            int ix = cell.getX();
            int iy = cell.getY();
            boolean isDiag = ix == iy;
            PlaneAspect asp = aspects[ iz ];
            double[] xlimits =
                  !isDiag || XDIAG
                ? xextents[ ix ].getLimits()
                : new double[] { asp.getXMin(), asp.getXMax() };
            double[] ylimits =
                  !isDiag || !XDIAG
                ? yextents[ iy ].getLimits()
                : new double[] { asp.getYMin(), asp.getYMax() };
            newAspects[ iz ] = new PlaneAspect( xlimits, ylimits );
        }
        return newAspects;
    }

    public PlaneSurfaceFactory.Profile[]
            adjustProfiles( PlaneSurfaceFactory.Profile[] profiles ) {

        /* This adjusts the profile for each cell so that it does not
         * display any annotations on the internal edges, only on
         * those edges that are not adjacent to other cells. */
        assert profiles.length == shape_.getCellCount();
        int nz = shape_.getCellCount();
        int nw = shape_.getWidth();
        PlaneSurfaceFactory.Profile[] newProfiles =
            new PlaneSurfaceFactory.Profile[ nz ];
        boolean hasLower = shape_.hasLower();
        boolean hasUpper = shape_.hasUpper();
        boolean hasDiag = shape_.hasDiagonal();
        for ( int iz = 0; iz < nz; iz++ ) {
            MatrixShape.Cell cell = shape_.getCell( iz );
            int ix = cell.getX();
            int iy = cell.getY();
            boolean left = ( hasUpper || hasDiag && ! hasLower )
                        && ( ix == cxlo_ )
                        && ( ix != iy || !XDIAG );
            boolean right = ( hasLower && ! hasUpper )
                         && ( ix == cxhi_ - 1 )
                         && ( ix != iy || !XDIAG );
            boolean top = ( hasLower && ! hasUpper )
                       && ( iy == ( YUP ? cyhi_ - 1 : cylo_ ) )
                       && ( ix != iy || XDIAG );
            boolean bottom = ( hasUpper || hasDiag && ! hasLower )
                          && ( iy == ( YUP ? cylo_ : cyhi_ - 1 ) )
                          && ( ix != iy || XDIAG );
            PlaneSurfaceFactory.Profile profile = profiles[ iz ];
            SideFlags annotateFlags =
                new SideFlags( bottom, left, top, right );

            /* Force a secondary axis if the top or right side is exposed,
             * otherwise the axes won't be displayed there. */
            boolean addSecondary = right || top;
            newProfiles[ iz ] =
                profiles[ iz ].fixAnnotation( annotateFlags, addSecondary );
        }
        return newProfiles;
    }

    /**
     * Calculates the insets required to accommodate axis decorations
     * for a gang.
     *
     * @param  gangExtBox  total area available for all zones and associated
     *                     decorations
     * @param  surfFact   surface factory
     * @param  zoneContents  plot content for each zone
     * @return     required surround
     */
    private Surround
            calculateAxisSurround( Rectangle gangExtBox,
                                   SurfaceFactory<PlaneSurfaceFactory.Profile,
                                                  PlaneAspect> surfFact,
                                   ZoneContent<PlaneSurfaceFactory.Profile,
                                               PlaneAspect>[] zoneContents ) {
        Surround surround =
            Surround.fromInsets( new Insets( PAD, PAD, PAD, PAD ) );
        int nz = shape_.getCellCount();
        int cw0 = gangExtBox.width / ( cxhi_ - cxlo_ );
        int ch0 = gangExtBox.height / ( cyhi_ - cylo_ );
        boolean zoneWithScroll = false;
        Padding zonePadding = null;
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneContent<PlaneSurfaceFactory.Profile,PlaneAspect>
                content = zoneContents[ iz ];
            Rectangle zoneExtBox = new Rectangle( 0, 0, cw0, ch0 );
            Surround zoneSurround =
                PlotPlacement
               .createPlacement( zoneExtBox, zonePadding, surfFact,
                                 content.getProfile(),
                                 content.getAspect(), zoneWithScroll,
                                 (Trimming) null, (ShadeAxis) null )
               .getSurface()
               .getSurround( zoneWithScroll );
            surround = surround.union( zoneSurround );
        }
        return surround;
    }

    /**
     * Creates a gang given the bounds of the cells excluding any
     * space for decorations.
     *
     * @param  gangInBox  bounding box for plot internal bounds only
     * @return   new gang
     */
    private MatrixGang createGang( Rectangle gangInBox ) {
        int ncx = cxhi_ - cxlo_;
        int ncy = cyhi_ - cylo_;
        int cw = ( gangInBox.width + cellGap_ ) / ncx;
        int ch = ( gangInBox.height + cellGap_ ) / ncy;
        int xoff = gangInBox.x;
        int yoff = gangInBox.y;
        if ( isSquares_ ) {
            int c = Math.min( cw, ch );
            cw = c;
            ch = c;
            xoff += ( gangInBox.width - ( ncx * cw - cellGap_ ) ) / 2;
            yoff += ( gangInBox.height - ( ncy * ch - cellGap_ ) ) / 2;
        }
        int ncell = shape_.getCellCount();
        Rectangle[] boxes = new Rectangle[ ncell ];
        for ( int icell = 0; icell < ncell; icell++ ) {
            MatrixShape.Cell cell = shape_.getCell( icell );
            int ix = cell.getX() - cxlo_;
            int iy = cell.getY() - cylo_;
            int rx = xoff + ix * cw;
            int ry = yoff + ( YUP ? ncy - 1 - iy : iy ) * ch;
            int rw = cw - cellGap_;
            int rh = ch - cellGap_;
            boxes[ icell ] = new Rectangle( rx, ry, rw, rh );
        }
        return new MatrixGang( boxes );
    }

    /**
     * Returns a selection of aspects extracted from a list of all aspects
     * in the matrix shape, corresponding to the ones in the same
     * column/row as a given index.
     *
     * @param  aspects  array of aspects one for each cell in shape
     * @param  iw       row/column index of interest
     * @param  isY      false if iw is an X index,
     *                  true if iw is a Y index
     * @return  array of aspects in row/column of interest
     */
    private PlaneAspect[] getLineAspects( PlaneAspect[] aspects, int iw,
                                          boolean isY ) {
        int nz = shape_.getCellCount();
        List<PlaneAspect> lineAspects = new ArrayList<>();
        for ( int iz = 0; iz < nz; iz++ ) {
            MatrixShape.Cell cell = shape_.getCell( iz );
            int x = cell.getX();
            int y = cell.getY();
            if ( iw == ( isY ? y : x ) && ( x != y || isY == !XDIAG ) ) {
                lineAspects.add( aspects[ iz ] );
            }
        }
        return lineAspects.toArray( new PlaneAspect[ 0 ] );
    }

    /**
     * Returns the extent of a given aspect in one or other direction.
     *
     * @param  aspect  aspect
     * @param  isY   true to get Y extent, false to get X extent
     * @return  extent in direction of interest
     */
    private static Extent getExtent( PlaneAspect aspect, boolean isY ) {
        return isY ? new Extent( aspect.getYMin(), aspect.getYMax() )
                   : new Extent( aspect.getXMin(), aspect.getXMax() );
    }

    /**
     * Returns the combined (maximal) extent of an array of aspects
     * in one or other direction.
     *
     * @param  aspects   array of aspects
     * @param  isY   true to get Y extent, false to get X extent
     * @return  maximal extent in direction of interest
     */
    private static Extent getExtent( PlaneAspect[] aspects, boolean isY ) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for ( PlaneAspect aspect : aspects ) {
            min = Math.min( min, isY ? aspect.getYMin() : aspect.getXMin() );
            max = Math.max( max, isY ? aspect.getYMax() : aspect.getXMax() );
        }
        return new Extent( min, max );
    }

    /**
     * Returns a measure of the perpendicular distance from a point
     * to a rectangle.
     *
     * <p>The return value is
     * 0 if the point is inside or on the edge of the box;
     * -1 if there is no perpendicular line from the point to the box;
     * or a positive value giving the minimum horizontal or vertical
     * distance to the box.
     *
     * @param  box  rectangle
     * @param  pos  test point
     * @return  distance to box, or -1 if not near
     */
    private static int getDistanceOutside( Rectangle box, Point pos ) {
        int dx1 = pos.x - box.x;
        int dx2 = pos.x - box.x - box.width;
        int dy1 = pos.y - box.y;
        int dy2 = pos.y - box.y - box.height;
        boolean inX = dx1 * dx2 <= 0;
        boolean inY = dy1 * dy2 <= 0;
        if ( inX && inY ) {
            return 0;
        }
        else if ( inX ) {
            return Math.min( Math.abs( dy1 ), Math.abs( dy2 ) );
        }
        else if ( inY ) {
            return Math.min( Math.abs( dx1 ), Math.abs( dx2 ) );
        }
        else {
            return -1;
        }
    }

    /**
     * Gang implementation for use with this class.
     */
    private static class MatrixGang implements Gang {

        private final Rectangle[] cellBoxes_;

        /**
         * Constructor.
         *
         * @cellBoxes  array of rectangles, one for each cell of matrix shape
         */
        MatrixGang( Rectangle[] cellBoxes ) {
            cellBoxes_ = cellBoxes;
        }

        public int getZoneCount() {
            return cellBoxes_.length;
        }

        public Rectangle getZonePlotBounds( int iz ) {
            return new Rectangle( cellBoxes_[ iz ] );
        }

        public int getNavigationZoneIndex( Point pos ) {
            int minDist = 10000;
            int ibest = -1;
            for ( int i = 0; i < cellBoxes_.length; i++ ) {
                int dist = getDistanceOutside( cellBoxes_[ i ], pos );
                if ( dist == 0 ) {
                    return i;
                }
                else if ( dist > 0 && dist < minDist ) {
                    minDist = dist;
                    ibest = i;
                }
            }
            return ibest;
        }
    }

    /**
     * Represents a range in one dimension.
     */
    private static class Extent {
        final double min_;
        final double max_;

        /**
         * Constructor.
         *
         * @param  min  minimum value
         * @param  max  maximum value
         */
        Extent( double min, double max ) {
            min_ = min;
            max_ = max;
        }

        /**
         * Returns the extent limits as a 2-element array.
         *
         * @return  (min,max) array
         */
        double[] getLimits() {
            return new double[] { min_, max_ };
        }
    }
}
