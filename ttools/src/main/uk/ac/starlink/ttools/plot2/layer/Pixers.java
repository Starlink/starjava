package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Point;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.util.IntList;

/**
 * Utility class for use with Pixers.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2013
 */
public class Pixers {

    /** Pixer with no pixels. */
    public static final Pixer EMPTY = new Pixer() {
        public boolean next() {
            return false;
        }
        public int getX() {
            throw new IllegalStateException();
        }
        public int getY() {
            throw new IllegalStateException();
        }
    };

    /**
     * Private constructor prevents instantiation.
     */
    private Pixers() {
    }

    /**
     * Returns a new pixer that iterates over a given list of X,Y coordinates.
     *
     * @param  xs  array of X values
     * @param  ys  array of Y values
     * @param  np  number of points (xs and ys must be at least this length)
     * @return  new pixel iterator
     */
    public static Pixer createArrayPixer( final int[] xs, final int[] ys,
                                          final int np ) {
        return new Pixer() {
            int ip = -1;
            public boolean next() {
                return ++ip < np;
            }
            public int getX() {
                return xs[ ip ];
            }
            public int getY() {
                return ys[ ip ];
            }
        };
    }

    /**
     * Returns a new pixer that iterates over an array of Points.
     * Iteration is done in place, so the content of these points
     * should not be altered for the lifetime of this pixer.
     *
     * @param  points  point array
     * @return  pixel iterator
     */
    public static Pixer createPointsPixer( final Point[] points ) {
        final int np = points.length;
        return new Pixer() {
            int ip = -1;
            public boolean next() {
                return ++ip < np;
            }
            public int getX() {
                return points[ ip ].x;
            }
            public int getY() {
                return points[ ip ].y;
            }
        };
    }

    /**
     * Returns an efficient copy of the given factory.
     * This may be suitable if it is known that many copies will be
     * required of a Pixer.
     *
     * @param  base  base implementation
     * @return  new PixerFactory functionally equivalent to base implementation
     */
    public static PixerFactory copy( PixerFactory base ) {
        final int xmin = base.getMinX();
        final int xmax = base.getMaxX();
        final int ymin = base.getMinY();
        final int ymax = base.getMaxY();
        final int npix = base.getPixelCount();
        final int[] xs = new int[ npix ];
        final int[] ys = new int[ npix ];
        int ip = 0;
        for ( Pixer pixer = base.createPixer(); pixer.next(); ) {
            xs[ ip ] = pixer.getX();
            ys[ ip ] = pixer.getY();
            ip++;
        }
        assert ip == npix;
        return new PixerFactory() {
            public int getMinX() {
                return xmin;
            }
            public int getMaxX() {
                return xmax;
            }
            public int getMinY() {
                return ymin;
            }
            public int getMaxY() {
                return ymax;
            }
            public int getPixelCount() {
                return npix;
            }
            public Pixer createPixer() {
                return createArrayPixer( xs, ys, npix );
            }
        };
    }

    /**
     * Takes a given pixer and copies its data, returning an object that
     * can issue pixers that behave the same as the original.
     * Since pixers are one-use iterators, this may be a useful caching
     * operation for pixer generation methods that are potentially expensive
     * to create and may be consumed multiple times.
     *
     * @param  pixer  input pixer
     * @return   factory to create copies of pixer
     */
    public static PixerFactory createPixerCopier( Pixer pixer ) {
        IntList xlist = new IntList();
        IntList ylist = new IntList();
        int ip = 0;
        int xmin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;
        while ( pixer.next() ) {
            int x = pixer.getX();
            int y = pixer.getY();
            xlist.add( x );
            ylist.add( y );
            xmin = Math.min( xmin, x );
            xmax = Math.max( xmax, x );
            ymin = Math.min( ymin, y );
            ymax = Math.max( ymax, y );
            ip++;
        }
        final int[] xs = xlist.toIntArray();
        final int[] ys = ylist.toIntArray();
        final int np = ip;
        final int xmin0 = xmin;
        final int xmax0 = xmax;
        final int ymin0 = ymin;
        final int ymax0 = ymax;
        return new PixerFactory() {
            public Pixer createPixer() {
                return createArrayPixer( xs, ys, np );
            }
            public int getPixelCount() {
                return np;
            }
            public int getMinX() {
                return xmin0;
            }
            public int getMaxX() {
                return xmax0;
            }
            public int getMinY() {
                return ymin0;
            }
            public int getMaxY() {
                return ymax0;
            }
        };
    }

    /**
     * Returns a translated version of a pixel iterator.
     *
     * @param  base  base pixel iterator
     * @param  tx   offset in X direction
     * @param  ty   offset in Y direction
     * @return  translated pixel iterator
     */
    public static Pixer translate( final Pixer base,
                                   final int tx, final int ty ) {
        return new Pixer() {
            public boolean next() {
                return base.next();
            }
            public int getX() {
                return base.getX() + tx;
            }
            public int getY() {
                return base.getY() + ty;
            }
        };
    }

    /**
     * Returns a clipped version of a given pixel iterator whose extent
     * is not known.
     *
     * @param  base  base pixel iterator
     * @param  clip   clipping rectangle
     * @return  clipped pixel iterator
     */
    public static Pixer clip( Pixer base, Rectangle clip ) {
        return new ClipPixer( base, clip.x, clip.x + clip.width - 1,
                                    clip.y, clip.y + clip.height - 1 );
    }

    /**
     * Returns a pixer that results from applying a clip rectangle to
     * the output of a given PixerFactory.
     *
     * @param  pfact  pixer factory
     * @param  clip   clipping rectangle
     * @return   pixer contiaining only points within the clip,
     *           or null if no points fall within it
     */
    public static Pixer createClippedPixer( PixerFactory pfact,
                                            Rectangle clip ) {
        int xminBase = pfact.getMinX();
        int xmaxBase = pfact.getMaxX();
        int xminClip = clip.x;
        int xmaxClip = clip.x + clip.width - 1;
        int xcmp = compare( xminClip, xmaxClip, xminBase, xmaxBase );
        if ( xcmp == 0 ) {
            return null;
        }
        int yminBase = pfact.getMinY();
        int ymaxBase = pfact.getMaxY();
        int yminClip = clip.y;
        int ymaxClip = clip.y + clip.height - 1;
        int ycmp = compare( yminClip, ymaxClip, yminBase, ymaxBase );
        if ( ycmp == 0 ) {
            return null;
        }
        if ( xcmp == 1 && ycmp == 1 ) {
            return pfact.createPixer();
        }
        else {
            int xmin = Math.max( xminClip, xminBase );
            int xmax = Math.min( xmaxClip, xmaxBase );
            int ymin = Math.max( yminClip, yminBase );
            int ymax = Math.min( ymaxClip, ymaxBase );
            return new ClipPixer( pfact.createPixer(), xmin, xmax, ymin, ymax );
        }
    }

    /**
     * Returns a clipped version of a given pixel iterator whose extent
     * is known.
     * May return null if the clipped pixer would dispense no pixels.
     * {x,y}{min,max}Base are the extreme values of the base pixer;
     * they do not account for the extent of a single pixel
     * (so a single pixel at the origin would have all values set to zero).
     *
     * @param  base  base pixel iterator
     * @param  clip   clipping rectangle
     * @param  xminBase  lower limit of X values dispensed by base pixer
     * @param  xmaxBase  upper limit of X values dispensed by base pixer
     * @param  yminBase  lower limit of Y values dispensed by base pixer
     * @param  ymaxBase  upper limit of Y values dispensed by base pixer
     * @return   clipped pixel iterator, or null if no pixels
     */
    public static Pixer clip( Pixer base, Rectangle clip,
                              int xminBase, int xmaxBase,
                              int yminBase, int ymaxBase ) {
        if ( base == null ) {
            return null;
        }
        int xminClip = clip.x;
        int xmaxClip = clip.x + clip.width - 1;
        int yminClip = clip.y;
        int ymaxClip = clip.y + clip.height - 1;
        int xcmp = compare( xminClip, xmaxClip, xminBase, xmaxBase );
        if ( xcmp == 0 ) {
            return null;
        }
        int ycmp = compare( yminClip, ymaxClip, yminBase, ymaxBase );
        if ( ycmp == 0 ) {
            return null;
        }
        if ( xcmp == 1 && ycmp == 1 ) {
            return base;
        }
        else {
            int xmin = Math.max( xminClip, xminBase );
            int xmax = Math.min( xmaxClip, xmaxBase );
            int ymin = Math.max( yminClip, yminBase );
            int ymax = Math.min( ymaxClip, ymaxBase );
            return new ClipPixer( base, xmin, xmax, ymin, ymax );
        }
    }

    /**
     * Compares two intervals and returns a flag indicating their
     * relative positioning.
     *
     * @param  minOuter  lower bound of outer interval, inclusive
     * @param  maxOuter  upper bound of outer interval, inclusive
     * @param  minInner  lower bound of inner interval, inclusive
     * @param  maxInner  upper bound of inner interval, inclusive
     * @return  +1: outer contains inner;
     *          -1: partial overlap;
     *           0: no overlap
     */
    private static int compare( int minOuter, int maxOuter,
                                int minInner, int maxInner ) {

        /* No overlap. */
        if ( minInner > maxOuter || maxInner < minOuter ) {
            return 0;
        }

        /* Complete containment. */
        else if ( minInner >= minOuter && maxInner <= maxOuter ) {
            return +1;
        }

        /* Partial overlap. */
        else {
            return -1;
        }
    }

    /**
     * Pixel iterator that excludes values outside of a given rectangle.
     */
    private static class ClipPixer implements Pixer {
        private final Pixer base_;
        private final int xmin_;
        private final int xmax_;
        private final int ymin_;
        private final int ymax_;

        /**
         * Constructor.
         *
         * @param  base  base pixel iterator
         * @param  xmin  lowest permissible X value
         * @param  xmax  highest permissible Y value
         * @param  ymin  lowest permissible Y value
         * @param  ymax  highest permissible Y value
         */
        ClipPixer( Pixer base, int xmin, int xmax, int ymin, int ymax ) {
            xmin_ = xmin;
            xmax_ = xmax;
            ymin_ = ymin;
            ymax_ = ymax;
            base_ = base;
        }

        public boolean next() {
            while ( base_.next() ) {
                if ( contains( xmin_, xmax_, base_.getX() ) &&
                     contains( ymin_, ymax_, base_.getY() ) ) {
                    return true;
                }
            }
            return false;
        }

        public int getX() {
            return base_.getX();
        }

        public int getY() {
            return base_.getY();
        }

        /**
         * Determines whether a value is within an inclusively specified range.
         *
         * @param   min  lowest permitted value
         * @param   max  highest permitted value
         * @param   value  value to test
         * @return   true iff value is within range
         */
        private static boolean contains( int min, int max, int value ) {
            return value >= min && value <= max;
        }
    }
}
