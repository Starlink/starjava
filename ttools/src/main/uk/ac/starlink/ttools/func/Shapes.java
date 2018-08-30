// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Functions useful for working with shapes in the (X, Y) plane.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2018
 */
public class Shapes {

    /**
     * Private constructor prevents instantiation.
     */
    private Shapes() {
    }

    /**
     * Function of <code>x</code> defined by straight line segments between a
     * specified list of vertices.
     * The vertices are specified as a sequence of
     * X<sub>i</sub>, Y<sub>i</sub> pairs,
     * for which the X<sub>i</sub> values must be monotonic.
     * The line segment at each end of the specified point sequence
     * is considered to be extended to infinity.
     * If only two points are specified, this is the equation of a
     * straight line between those points.
     * As a special case, if only one point is specified, the line
     * is considered to be a horizontal line (equal to the sole specified
     * Y<sub>i</sub> coordinate for all <code>x</code>).
     *
     * <p>By reversing the X<sub>i</sub> and Y<sub>i</sub> values,
     * this function can equally be used to represent a function
     * <code>X(y)</code> rather than <code>Y(x)</code>.
     *
     * <p>If the number of coordinates is odd,
     * or the X<sub>i</sub> values are not monotonic,
     * behaviour is undefined.
     *
     * @example   <code>polyLine(5, 0,0, 2,2) = 5</code>
     *
     * @param  x     X value at which function is to be evaluated
     * @param  xys   2N arguments (<code>x1</code>, <code>y1</code>,
     *                             <code>x2</code>, <code>y2</code>, ...,
     *                             <code>xN</code>, <code>yN</code>)
     *               giving vertices of an N-point line
     *               with monotonically increasing or decreasing X values
     * @return  Y coordinate of poly-line for specified <code>x</code>
     */
    public static double polyLine( double x, double... xys ) {
        int np2 = xys.length;
        if ( np2 % 2 != 0 || np2 < 2 ) {
            return Double.NaN;
        }
        if ( np2 == 2 ) {
            return xys[ 1 ];
        }
        else if ( np2 == 4 ) {
            return lineSegment( x, xys, 0 );
        }
        else {
            double xA = xys[ 0 ];
            double xB = xys[ np2 - 2 ];
            if ( ( xys[ 2 ] - x ) * ( xB - xA ) >= 0 ) {
                return lineSegment( x, xys, 0 );
            }
            if ( ( xys[ np2 - 4 ] - x ) * ( xA - xB ) >= 0 ) {
                return lineSegment( x, xys, np2 - 4 );
            }
            for ( int ip2 = 2; ip2 < np2 - 4; ip2 += 2 ) {
                if ( ( x - xys[ ip2 + 0 ] ) * ( x - xys[ ip2 + 2 ] ) <= 0 ) {
                    return lineSegment( x, xys, ip2 );
                }
            }
            return Double.NaN;
        }
    }

    /**
     * Indicates whether a given test point is inside a polygon
     * defined by specified list of vertices.
     * The vertices are specified as a sequence of
     * X<sub>i</sub>, Y<sub>i</sub> pairs.
     *
     * <p>If the number of coordinates is odd,
     * the behaviour is not defined.
     *
     * @example  <code>isInside(0.5,0.5, 0,0, 0,1, 1,1, 1,0) = true</code>
     * @example  <code>isInside(0,0, array(10,20, 20,20, 20,10)) = false</code>
     *
     * @param  x   X coordinate of test point
     * @param  y   Y coordinate of test point
     * @param  xys   2N arguments (<code>x1</code>, <code>y1</code>,
     *                             <code>x2</code>, <code>y2</code>, ...,
     *                             <code>xN</code>, <code>yN</code>)
     *               giving vertices of an N-sided polygon
     * @return  true iff test point is inside, or on the border of,
     *          the polygon
     */
    public static boolean isInside( double x, double y, double... xys ) {
        int np2 = xys.length;
        return np2 % 2 == 0 && np2 > 4
             ? windingNumber( x, y, xys ) != 0
             : false;
    }

    /**
     * Function of a line defined by two points, given by four
     * adjacent coordinates (x1,y1,x2,y2) taken from a given array.
     * 
     * @param  x  value at which to evaluate function
     * @param  xys   sequence of xN,yN coordinates defining points
     * @param  i   index of first X coordinate in array
     * @return    line function evaluated at x
     */
    private static double lineSegment( double x, double[] xys, int i ) {
        double x1 = xys[ i + 0 ];
        double y1 = xys[ i + 1 ];
        double x2 = xys[ i + 2 ];
        double y2 = xys[ i + 3 ];
        return ( y2 - y1 ) / ( x2 - x1 ) * ( x - x1 ) + y1;
    }

    /**
     * Calculates the winding number of a closed polygon.
     *
     * @param  x0  X coordinate of test point
     * @param  y0  Y coordinate of test point
     * @param  xys   2N arguments (<code>x1</code>, <code>y1</code>,
     *                             <code>x2</code>, <code>y2</code>, ...,
     *                             <code>xN</code>, <code>yN</code>)
     *               giving coords of vertices of an N-sided polygon, N&gt;1
     * @return   zero for x0, y0 outside the polygon, non-zero for inside
     */
    private static int windingNumber( double x0, double y0, double[] xys ) {
    
        // This routine was coded with reference to:
        //
        //    http://geomalgorithms.com/a03-_inclusion.html
        //
        // That code includes the following statement:
        //    Copyright 2000 softSurfer, 2012 Dan Sunday
        //    This code may be freely used and modified for any purpose
        //    providing that this copyright notice is included with it.
        //    SoftSurfer makes no warranty for this code, and cannot be held
        //    liable for any real or imagined damage resulting from its use.
        //    Users of this code must verify correctness for their application.

        int nw = 0;
        int np2 = xys.length;
        double xA = xys[ np2 - 2 ];
        double yA = xys[ np2 - 1 ];
        for ( int ip2 = 0; ip2 < np2; ip2 += 2 ) {
            double xB = xys[ ip2 + 0 ];
            double yB = xys[ ip2 + 1 ];
            if ( yA <= y0 ) {
                if ( yB > y0 && sideCode( x0, y0, xA, yA, xB, yB ) > 0 ) {
                    nw++;
                }
            }
            else {
                if ( yB <= y0 && sideCode( x0, y0, xA, yA, xB, yB ) < 0 ) {
                    nw--;
                }
            }
            xA = xB;
            yA = yB;
        }
        return nw;
    }

    /**
     * Indicates whether a given point is left of a line defined by
     * two points.
     *
     * @param  x0  test point X coord
     * @param  y0  test point Y coord
     * @param  x1  line first vertex X coord
     * @param  y1  line first vertex Y coord
     * @param  x2  line second vertex X coord
     * @param  y2  line second vertex Y coord
     * @return  negative, zero, positive for test point left, on, right of line
     */
    private static double sideCode( double x0, double y0,
                                    double x1, double y1,
                                    double x2, double y2 ) {
        return ( x2 - x1 ) * ( y0 - y1 ) - ( x0 - x1 ) * ( y2 - y1 );
    }
}
