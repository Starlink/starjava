package uk.ac.starlink.topcat.plot;

import java.awt.Point;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Records the positions of a set of plotted points in such a way that
 * it can be interrogated later (and reasonably efficiently) to locate
 * points at or near a given location.
 * 
 * <p>To use an instance of this class:
 * <ol>
 * <li>Call {@link #addPoint} for each point that you wish to register
 * <li>Call {@link #ready} to indicate that all the points are in place
 * <li>Call {@link #getPoints} (or other interrogation methods?) to enquire
 *     about where some of the points are
 * </ol>
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Jul 2004
 */
public class PointRegistry {

    private ArrayList pointList_ = new ArrayList(); // only available pre-ready
    private IdentifiedPoint[] points_;              // only avaialble post-ready
    private boolean ready_;
    
    /**
     * Adds a new plotted point to the list of ones known by this 
     * class.
     */
    public void addPoint( int ip, Point p ) {
        if ( ready_ ) {
            throw new IllegalStateException( "Too late to add new points" );
        }
        pointList_.add( new IdentifiedPoint( ip, p ) );
    }

    /**
     * Indicates that all the plotted points have been added and we're
     * ready to accept interrogation.
     * Any call to {@link #addPoint} must be done <em>before</em> this
     * call; any call to {@link #getPoints} must be done <em>after</em> it.
     */
    public void ready() {
        points_ = (IdentifiedPoint[]) 
                  pointList_.toArray( new IdentifiedPoint[ 0 ] );
        pointList_ = null;
        Arrays.sort( points_ );
        ready_ = true;
    }

    /**
     * Returns a bit vector containing with bits set for every point index
     * which falls within a given shape on the screen.
     *
     * @param  shape  shape defining inclusion criterion
     * @return  bit vector locating points inside <tt>shape</tt>
     */
    public BitSet getContainedPoints( Shape shape ) {
  return null;
    }
    
    /**
     * Returns an array giving the indices of all the plotted points within
     * an error box of a given point on the screen.
     *
     * @param  p  screen point near which plotted points should be located
     * @param  error  number of pixels in any direction which defines the
     *         error box within which points will be found
     * @return array of indices of points which fall in the defined error box
     */
    public int[] getNearbyPoints( Point p, int error ) {
        IdentifiedPoint[] ipoints = getNearbyIPoints( p, error );
        int npoint = ipoints.length;
        int[] pointIndices = new int[ npoint ];
        for ( int i = 0 ; i < npoint; i++ ) {
            pointIndices[ i ] = ipoints[ i ].id_;
        }
        return pointIndices;
    }

    /**
     * Returns the index of the closest plotted point to a given screen point.
     * Only points within a given error box are eligible; if none can 
     * be found, -1 is returned.
     *
     * @param  p  screen point near which plotted points should be located
     * @param  error  number of pixels in any direction which defines the
     *         error box within which a point may be found
     * @return index of closest point to <tt>p</tt>, or -1 if none are nearby
     */
    public int getClosestPoint( Point p, int error ) {
        IdentifiedPoint[] ipoints = getNearbyIPoints( p, error );
        int npoint = ipoints.length;
        if ( npoint == 0 ) {
            return -1;
        }
        else if ( npoint == 1 ) {
            return ipoints[ 0 ].id_;
        }
        else {
            assert npoint > 1;
            int x = p.x;
            int y = p.y;
            int maxr2 = Integer.MAX_VALUE;
            IdentifiedPoint closest = null;
            for ( int i = 0; i < npoint; i++ ) {
                IdentifiedPoint ipoint = ipoints[ i ];
                int dx = x - ipoint.x_;
                int dy = y - ipoint.y_;
                int r2 = dx * dx + dy + dy;
                if ( r2 < maxr2 ) {
                    maxr2 = r2;
                    closest = ipoint;
                }
            }
            assert closest != null;
            return closest.id_;
        }
    }

    /**
     * Returns all the registered IdentifiedPoints within an error box
     * of a given point on the screen.
     *
     * @param  p  screen point near which plotted points should be located
     * @param  error  number of pixels in any direction which defines the
     *         error box within which points will be found
     * @return array of IdentifiedPoint objects in the box
     */
    private IdentifiedPoint[] getNearbyIPoints( Point p, int error ) {
        if ( ! ready_ ) {
            throw new IllegalStateException( "Not ready!" );
        }
        int px = p.x;
        int py = p.y;

        /* Identify the acceptable range of values. */
        int loX = px - error;
        int hiX = px + error;
        int loY = py - error;
        int hiY = py + error;

        /* Locate a point in the sorted array of plotted points corresponding
         * to the first point with an X coordinate equal to the lower bound
         * of acceptable values. */
        Point loPoint = new Point( loX, Integer.MIN_VALUE );
        IdentifiedPoint dummyPoint = new IdentifiedPoint( -1, loPoint );
        int loIndex = - Arrays.binarySearch( points_, dummyPoint ) - 1;

        /* Loop through all the plotted points in the right range of
         * X coordinate. */
        int np = points_.length;
        List foundPoints = new ArrayList( 100 );
        for ( int i = loIndex; i < np; i++ ) {
            IdentifiedPoint point = points_[ i ];
            int x = point.x_;
            if ( x > hiX ) {
                break;
            }
            int y = point.y_;
            if ( y >= loY && y <= hiY ) {
                foundPoints.add( point );
            }
        }

        /* Return an array of the located points. */
        return (IdentifiedPoint[])
               foundPoints.toArray( new IdentifiedPoint[ 0 ] );
    }

    /**
     * Helper class which encapsulates a point and its associated index
     * (sequence number in the data set).
     */
    private static class IdentifiedPoint implements Comparable {

        final int id_;
        final int x_;
        final int y_;

        IdentifiedPoint( int id, Point p ) {
            id_ = id;
            x_ = p.x;
            y_ = p.y;
        }

        /**
         * Collation order ranks by X coordinate first, and in case of 
         * X equality, by Y coordinate.
         */
        public int compareTo( Object o ) {
            IdentifiedPoint other = (IdentifiedPoint) o;
            int x1 = this.x_;
            int x2 = other.x_;
            if ( x1 == x2 ) {
                int y1 = this.y_;
                int y2 = other.y_;
                if ( y1 == y2 ) {
                    return 0;
                }
                else {
                    return ( y1 < y2 ) ? -1 : +1;
                }
            }
            else {
                return ( x1 < x2 ) ? -1 : +1;
            }
        }
    }

}
