package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.ValueInfo;

/**
 * Characterises the details of how a plot is to be done.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotState {

    private final int ndim_;
    private boolean valid_;
    private ValueInfo[] axes_;
    private boolean[] logFlags_;
    private boolean[] flipFlags_;
    private boolean grid_;
    private PointSelection pointSelection_;

    /**
     * Constructor.
     *
     * @param  ndim  number of dimensions of the plot
     */
    public PlotState( int ndim ) {
        ndim_ = ndim;
    }

    /**
     * Returns the dimensionality of the plot.
     *
     * @return   number of dimensions of the plot
     */
    public int getNdim() {
        return ndim_;
    }

    /**
     * Sets whether this state should be used to attempt a successful plot.
     * If false, it is underspecified in some way.
     *
     * @param   valid  validity flag
     */
    public void setValid( boolean valid ) {
        valid_ = valid;
    }

    /**
     * Indicates whether this state can be used to attempt a successful plot.
     * If false, it is underspecified in some way.
     *
     * @return  validity flag
     */
    public boolean getValid() {
        return valid_;
    }

    /**
     * Sets the metadata for axes to be plotted.
     *
     * @param  axes  axis metadata array
     */
    public void setAxes( ValueInfo[] axes ) {
        if ( axes.length != ndim_ ) {
            throw new IllegalArgumentException();
        }
        axes_ = axes;
    }

    /**
     * Returns the metadata for the plotted axes.
     *
     * @return  axis metadata array
     */
    public ValueInfo[] getAxes() {
        return axes_;
    }

    /**
     * Sets flags for which axes will be plotted logarithmically.
     *
     * @param   logFlags   log flags
     */
    public void setLogFlags( boolean[] logFlags ) {
        if ( logFlags.length != ndim_ ) {
            throw new IllegalArgumentException();
        }
        logFlags_ = logFlags;
    }

    /**
     * Returns flags for which axes will be plotted logarithmically.
     *
     * @return   log flags
     */
    public boolean[] getLogFlags() {
        return logFlags_;
    }

    /**
     * Sets flags for which axes will be plotted inverted.
     *
     * @param   flipFlags  flip flags
     */
    public void setFlipFlags( boolean[] flipFlags ) {
        if ( flipFlags.length != ndim_ ) {
            throw new IllegalArgumentException();
        }
        flipFlags_ = flipFlags;
    }

    /**
     * Returns flags for which axes will be plotted inverted.
     *
     * @return  flip flags
     */
    public boolean[] getFlipFlags() {
        return flipFlags_;
    }

    /**
     * Sets whether a grid is to be plotted.
     *
     * @param   grid  whether to draw a grid
     */
    public void setGrid( boolean grid ) {
        grid_ = grid;
    }

    /**
     * Indicates whether a grid is to be plotted.
     *
     * @return   grid  whether to draw a grid
     */
    public boolean getGrid() {
        return grid_;
    }

    /**
     * Sets the point selection object for this state.
     *
     * @param   pointSelection  data selection object
     */
    public void setPointSelection( PointSelection pointSelection ) {
        pointSelection_ = pointSelection;
    }

    /**
     * Returns the point selection object for this state.
     *
     * @return  data selection object
     */
    public PointSelection getPointSelection() {
        return pointSelection_;
    }

    /**
     * Indicates whether the data required to plot this state differs 
     * from the data required to plot another.  Only if this method
     * returns true can two PlotStates be used with the same Points object.
     *
     * @param   state for comparison
     * @return  true if <code>other</code> can be used with the same Points
     */
    public boolean sameData( PlotState other ) {
        return other != null
            && ( pointSelection_ == null 
                    ? other.pointSelection_ == null
                    : pointSelection_.sameData( other.pointSelection_ ) );
    }

    /**
     * Indicates whether the axes of this plot state are the same as
     * those for <code>other</code>, that is whether the same data
     * range is covered.
     *
     * @param  other  comparison object
     * @return   true  if the axes match
     */
    public boolean sameAxes( PlotState other ) {
        return other != null
            && Arrays.equals( axes_, other.axes_ )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ );
    }

    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof PlotState ) ) {
            return false;
        }
        PlotState other = (PlotState) otherObject;
        return ndim_ == other.ndim_
            && valid_ == other.valid_
            && grid_ == other.grid_
            && Arrays.equals( axes_, other.axes_ )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ )
            && ( pointSelection_ == null 
                    ? other.pointSelection_ == null
                    : pointSelection_.equals( other.pointSelection_ ) );
    }

    public int hashCode() {
        int code = 555;
        code = 23 * code + ndim_;
        code = 23 * code + ( valid_ ? 99 : 999 );
        code = 23 * code + ( grid_ ? 11 : 17 );
        for ( int i = 0; i < ndim_; i++ ) {
            code = 23 * code + axes_[ i ].hashCode();
            code = 23 * code + ( logFlags_[ i ] ? 1 : 2 );
            code = 23 * code + ( flipFlags_[ i ] ? 1 : 2 );
        }
        code = 23 * code + ( pointSelection_ == null 
                                ? 0
                                : pointSelection_.hashCode() );
        return code;
    }
}
