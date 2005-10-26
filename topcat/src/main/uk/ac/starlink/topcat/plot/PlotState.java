package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Characterises the details of how a plot is to be done.
 * This class details only the basics concerning the plot axes;
 * it is intended to be subclassed for the specific needs of different
 * N-dimensional plot types.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotState {

    private final int ndim_;
    private StarTableColumn[] columns_;
    private boolean[] logFlags_;
    private boolean[] flipFlags_;
    private boolean grid_;
    private RowSubset[] usedSubsets_;
    private MarkStyle[] styles_;

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
     * Sets the columns which will be plotted on the axes.
     *
     * @param  columns  column array
     */
    public void setColumns( StarTableColumn[] columns ) {
        if ( columns.length != ndim_ ) {
            throw new IllegalArgumentException();
        }
        columns_ = columns;
    }

    /**
     * Returns the columns to be plotted on the axes.
     *
     * @return  column array
     */
    public StarTableColumn[] getColumns() {
        return columns_;
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
     * Sets the list of row subsets which should be represented in the plot,
     * along with the marker style for each one.
     *
     * @param  subsets  array of row subsets to be plotted
     * @param  styles   array of marker styles, one for each element of
     *                  <tt>subsets</tt>
     * @throws IllegalArgumentException if <tt>subsets</tt> and <tt>styles</tt>
     *         do not have the same number of elements
     */
    public void setSubsets( RowSubset[] subsets, MarkStyle[] styles ) {
        if ( subsets.length != styles.length ) {
            throw new IllegalArgumentException();
        }
        usedSubsets_ = subsets;
        styles_ = styles;
    }

    /**
     * Returns the list of row subsets which should be represented in the plot.
     * The array itself is returned, not a clone.
     *
     * @return  array of row subsets to be plotted
     *          (same length as {@link #getStyles})
     */
    public RowSubset[] getSubsets() {
        return usedSubsets_;
    }

    /**
     * Returns the list of marker styles corresponding to the subsets to
     * be plotted.
     * The array itself is returned, not a clone.
     *
     * @return  array of marker styles
     *          (same length as {@link #getSubsets})
     */
    public MarkStyle[] getStyles() {
        return styles_;
    }

    /**
     * Indicates whether the data represented by this plot is the same
     * as that of <tt>other</tt>.
     *
     * @param  other  state for comparison
     * @return  true iff the X and Y columns represent the same data
     */
    public boolean sameData( PlotState other ) {
        return other != null
            && Arrays.equals( columns_, other.columns_ );
    }

    /**
     * Indicates whether the axes of this plot state are the same as
     * those of <tt>other</tt>.
     *
     * @param  other  state for comparison
     * @return  true iff the X and Y columns represent the same data and
     *          have the same log flags between this and <tt>other</tt>
     */
    public boolean sameAxes( PlotState other ) {
        return sameData( other )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ );
    }

    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof PlotState ) ) {
            return false;
        }
        PlotState other = (PlotState) otherObject;
        return ndim_ == other.ndim_ 
            && grid_ == other.grid_
            && Arrays.equals( columns_, other.columns_ )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ )
            && Arrays.equals( usedSubsets_, other.usedSubsets_ )
            && Arrays.equals( styles_, other.styles_ );
    }

    public int hashCode() {
        int code = 555;
        code = 23 * code + ndim_;
        code = 23 * code + ( grid_ ? 11 : 17 );
        for ( int i = 0; i < ndim_; i++ ) {
            code = 23 * code + columns_[ i ].hashCode();
            code = 23 * code + ( logFlags_[ i ] ? 0 : 1 );
            code = 23 * code + ( flipFlags_[ i ] ? 0 : 1 );
        }
        for ( int i = 0; i < usedSubsets_.length; i++ ) {
            code = 23 * code + usedSubsets_[ i ].hashCode();
            code = 23 * code + styles_[ i ].hashCode();
        }
        return code;
    }
}
