package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Characterises the details of a plot.
 */
public class PlotState {

    private StarTableColumn xCol_;
    private StarTableColumn yCol_;
    private boolean xLog_;
    private boolean yLog_;
    private boolean xFlip_;
    private boolean yFlip_;
    private boolean grid_;
    private RowSubset[] usedSubsets_;
    private MarkStyle[] styles_;
    private boolean[] regressions_;

    /**
     * Constructs a PlotState giving the columns which it will plot.
     *
     * @param  xcol  X coordinate column
     * @param  ycol  Y coordinate column
     */
    public PlotState( StarTableColumn xcol, StarTableColumn ycol ) {
        xCol_ = xcol;
        yCol_ = ycol;
    }

    /**
     * Returns the table column plotted on the X axis.
     *
     * @return   X coordinate column
     */
    public StarTableColumn getXColumn() {
        return xCol_;
    }

    /**
     * Returns the table column plotted on the Y axis.
     *
     * @param  Y coordinate column
     */
    public StarTableColumn getYColumn() {
        return yCol_;
    }

    /**
     * Sets whether the X axis should be plotted logarithmically.
     *
     * @param  xlog  true iff X axis should have a logarithmic scale
     */
    public void setXLog( boolean xlog ) {
        xLog_ = xlog;
    }

    /**
     * Determines whether the X axis is plotted logarithmically.
     *
     * @return  true iff X axis has a logarithmic scale
     */
    public boolean isXLog() {
        return xLog_;
    }

    /**
     * Sets whether the Y axis should be plotted logarithmically. 
     *
     * @param  ylog  true iff Y axis should have a logarithmic scale
     */
    public void setYLog( boolean ylog ) {
        yLog_ = ylog;
    }

    /**
     * Determines whether the Y axis is plotted logarithmically.
     *
     * @return  true iff the Y axis has a logarithmic scale
     */
    public boolean isYLog() {
        return yLog_;
    }

    /**
     * Sets whether the X axis should be plotted reversed.
     * 
     * @param  xflip  true iff X axis should be flipped
     */
    public void setXFlip( boolean xflip ) {
        xFlip_ = xflip;
    }

    /**
     * Determines whether the X axis is plotted reversed.
     *
     * @return  true iff X axis is flipped
     */
    public boolean isXFlip() {
        return xFlip_;
    }

    /**
     * Sets whether the Y axis should be plotted reversed.
     * 
     * @param  yflip  true iff Y axis should be flipped
     */
    public void setYFlip( boolean yflip ) {
        yFlip_ = yflip;
    }

    /**
     * Determines whether the Y axis is plotted reversed.
     *
     * @return  true iff Y axis is flipped
     */
    public boolean isYFlip() {
        return yFlip_;
    }

    /**
     * Sets whether an axis grid should be plotted.
     *
     * @param  grid  true iff a grid should be plotted
     */
    public void setGrid( boolean grid ) {
        grid_ = grid;
    }

    /**
     * Determines whether an axis grid will be plotted.
     *
     * @return  true iff an axis grid is required
     */
    public boolean hasGrid() {
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
    public void setSubsets( RowSubset[] subsets, MarkStyle[] styles,
                            boolean[] regressions ) {
        int nset = subsets.length;
        if ( styles.length != nset || regressions.length != nset ) {
            throw new IllegalArgumentException( "Unequal arrays lengths" );
        }
        usedSubsets_ = subsets;
        styles_ = styles;
        regressions_ = regressions;
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
     * Returns an array of flags indicating whether a regression line is
     * to be drawn for each subset.
     * The array itself is returned, not a clone.
     *
     * @return  array of regression line flags
     *          (same length as {@link #getSubsets})
     */
    public boolean[] getRegressions() {
        return regressions_;
    }

    public boolean equals( Object otherObject ) {
        if ( otherObject instanceof PlotState ) {
            PlotState other = (PlotState) otherObject;
            return other instanceof PlotState
                && xCol_.equals( other.xCol_ )
                && yCol_.equals( other.yCol_ )
                && xLog_ == other.xLog_
                && yLog_ == other.yLog_
                && xFlip_ == other.xFlip_
                && yFlip_ == other.yFlip_
                && grid_ == other.grid_
                && Arrays.equals( usedSubsets_, other.usedSubsets_ )
                && Arrays.equals( styles_, other.styles_ )
                && Arrays.equals( regressions_, other.regressions_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = 5;
        code = 23 * code + ( xCol_ == null ? 99 : xCol_.hashCode() );
        code = 23 * code + ( yCol_ == null ? 99 : yCol_.hashCode() );
        code = 23 * code + ( xLog_ ? 0 : 1 );
        code = 23 * code + ( yLog_ ? 0 : 1 );
        code = 23 * code + ( xFlip_ ? 0 : 1 );
        code = 23 * code + ( yFlip_ ? 0 : 1 );
        code = 23 * code + ( grid_ ? 0 : 1 );
        for ( int i = 0; i < usedSubsets_.length; i++ ) {
            code = 23 * code + usedSubsets_[ i ].hashCode();
            code = 23 * code + styles_[ i ].hashCode();
            code = 23 * code + ( regressions_[ i ] ? 0 : 1 );
        }
        return code;
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
            && xLog_ == other.xLog_
            && yLog_ == other.yLog_
            && xFlip_ == other.xFlip_
            && yFlip_ == other.yFlip_;
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
            && xCol_ == other.xCol_
            && yCol_ == other.yCol_;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer()
            .append( "xCol=" )
            .append( xCol_ )
            .append( "," )
            .append( "yCol=" )
            .append( yCol_ )
            .append( "," )
            .append( "xLog=" )
            .append( xLog_ )
            .append( "," )
            .append( "yLog=" )
            .append( yLog_ )
            .append( "," )
            .append( "xFlip=" )
            .append( xFlip_ )
            .append( "," )
            .append( "yFlip=" )
            .append( yFlip_ )
            .append( "," )
            .append( "grid=" )
            .append( grid_ )
            .append( "(" );
        for ( int i = 0; i < usedSubsets_.length; i++ ) {
            sbuf.append( usedSubsets_[ i ].getName() );
            if ( i > 0 ) {
                sbuf.append( "," );
            }
        }
        sbuf.append( ")" );
        return sbuf.toString();
    }
}
