package uk.ac.starlink.ttools.plot;

import java.awt.Point;
import java.awt.Shape;
import java.util.BitSet;

/**
 * Iterates over the points which have actually been plotted to the screen.
 *
 * @author   Mark Taylor
 * @since    19 Jan 2006
 */
public abstract class PointIterator {

    private int ip_;
    private int xp_;
    private int yp_;

    /** PointIterator instance with no points. */
    public static PointIterator EMPTY = new PointIterator() {
        protected int[] nextPoint() {
            return null;
        }
        public String toString() {
            return "Empty PointIterator";
        }
    };

    /**
     * Returns a triple giving point index, screen X coordinate and 
     * screen Y coordinate.  Returns null if there are no more points.
     * It is permissible to return the same <code>int[]</code> 
     * array with different contents each time.
     * Invoked by {@link #readNextPoint}.
     *
     * @return   ip, xp, yp triple
     */
    protected abstract int[] nextPoint();

    /**
     * Returns the most recently read point index.
     *
     * @return  ip
     */
    public int getIndex() {
        return ip_;
    }

    /**
     * Returns the most recently read screen X coordinate.
     *
     * @return  xp
     */
    public int getX() {
        return xp_;
    }

    /**
     * Returns the most recently read Y coordinate.
     *
     * @return   yp
     */
    public int getY() {
        return yp_;
    }

    /**
     * Loads the data for the next point if there is one.
     *
     * @return   true  if the data are loaded for the next point;
     *           false if the iteration is at an end
     */
    public boolean readNextPoint() {
        int[] items = nextPoint();
        if ( items != null ) {
            ip_ = items[ 0 ];
            xp_ = items[ 1 ];
            yp_ = items[ 2 ];
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Returns a bit vector with bits set for every point index
     * which falls within a given shape on the screen.
     *
     * @param  shape  shape defining inclusion criterion
     * @return  bit vector locating points inside <code>shape</code>
     */
    public BitSet getContainedPoints( Shape shape ) {
        BitSet inside = new BitSet();
        while ( readNextPoint() ) {
            if ( shape.contains( (double) getX(), (double) getY() ) ) {
                inside.set( getIndex() );
            }
        }
        return inside;
    }

    /**
     * Returns a bit vector with bits set for every point index
     * visited by this iterator.
     *
     * @return   bit vector locating included points
     */
    public BitSet getAllPoints() {
        BitSet included = new BitSet();
        while ( readNextPoint() ) {
            included.set( getIndex() );
        }
        return included;
    }

    /**
     * Returns the index of the closest plotted point to a given screen point.
     * Only points within a given error box are eligible; if none can
     * be found, -1 is returned.
     *
     * @param  p  screen point near which plotted points should be located
     * @param  error  number of pixels in any direction which defines the
     *         error box within which a point may be found
     * @return index of closest point to <code>p</code>,
     *         or -1 if none are nearby
     */
    public int getClosestPoint( Point p, int error ) {
        int ifound = -1;
        int maxr2 = error * error + 1;
        while ( readNextPoint() ) {
            int dx = getX() - p.x;
            int dy = getY() - p.y;
            int r2 = dx * dx + dy * dy;
            if ( r2 == 0 ) {
                return ip_;
            }
            else if ( r2 < maxr2 ) {
                maxr2 = r2;
                ifound = ip_;
            }
        }
        return ifound;
    }
}
