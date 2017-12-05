package uk.ac.starlink.ttools.plot2.layer;

/**
 * Maps positions on a 2-d grid to a 1-d index.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class Gridder {
    private final int nx_;
    private final int ny_;

    /**
     * Constructor.
     *
     * @param   nx  grid width
     * @param   ny  grid height
     */
    public Gridder( int nx, int ny ) {
        nx_ = nx;
        ny_ = ny;
    }

    /**
     * Returns grid width.
     *
     * @return width
     */
    public int getWidth() {
        return nx_;
    }

    /**
     * Returns grid height.
     *
     * @return  height
     */
    public int getHeight() {
        return ny_;
    }

    /**
     * Returns the 1-d index corresponding to a given x,y position.
     *
     * @param  ix  x position
     * @param  iy  y position
     * @return  array index
     */
    public int getIndex( int ix, int iy ) {
        return iy * nx_ + ix;
    }

    /**
     * Returns the X index corresponding to a pixel index.
     *
     * @param  index  1-d index
     * @return  X position
     */
    public int getX( int index ) {
        return index % nx_;
    }

    /**
     * Returns the Y index corresponding to a pixel index.
     *
     * @param  index  1-d index
     * @return  Y position
     */
    public int getY( int index ) {
        return index / nx_;
    }

    /**
     * Returns the number of points in the grid and array.
     *
     * @return  size
     */
    public int getLength() {
        return nx_ * ny_;
    }

    @Override
    public int hashCode() {
        int code = 5502432;
        code = 23 * code + nx_;
        code = 23 * code + ny_;
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Gridder ) {
            Gridder other = (Gridder) o;
            return this.nx_ == other.nx_
                && this.ny_ == other.ny_;
        }
        else {
            return false;
        }
    }

    /**
     * Returns a Gridder instance that is the transpose of the supplied one.
     * If the supplied instance does row-major indexing, the result does
     * column-major instead.
     *
     * @param   base  input gridder
     * @return   gridder with X and Y transposed
     */
    public static Gridder transpose( final Gridder base ) {
        return new Gridder( base.getHeight(), base.getWidth() ) {
            @Override
            public int getIndex( int ix, int iy ) {
                return base.getIndex( iy, ix );
            }
            @Override
            public int getX( int index ) {
                return base.getY( index );
            }
            @Override
            public int getY( int index ) {
                return base.getX( index );
            }
        };
    }
}
