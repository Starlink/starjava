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
}
