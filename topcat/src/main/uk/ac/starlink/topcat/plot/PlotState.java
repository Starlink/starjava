package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Characterises the details of a plot.
 */
public class PlotState {

    /** Column for X coordinate. */
    public StarTableColumn xCol_;

    /** Column for Y coordinate. */
    public StarTableColumn yCol_;

    /** Whether X axis is a log axis. */
    public boolean xLog_;

    /** Whether Y axis is a log axis. */
    public boolean yLog_;

    /** Whether a grid should be plotted over the plotting surface. */
    public boolean grid_;

    /** List of RowSubsets for which point sets should be plotted. */
    public RowSubset[] usedSubsets_;

    /** List of marker styles, corresponding to <tt>usedSubsets_</tt>. */
    public MarkStyle[] styles_;

    public boolean equals( Object otherObject ) {
        if ( otherObject instanceof PlotState ) {
            PlotState other = (PlotState) otherObject;
            return other instanceof PlotState
                && xCol_.equals( other.xCol_ )
                && yCol_.equals( other.yCol_ )
                && xLog_ == other.xLog_
                && yLog_ == other.yLog_
                && grid_ == other.grid_
                && Arrays.equals( usedSubsets_, other.usedSubsets_ )
                && Arrays.equals( styles_, other.styles_ );
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
        code = 23 * code + ( grid_ ? 0 : 1 );
        for ( int i = 0; i < usedSubsets_.length; i++ ) {
            code = 23 * code + usedSubsets_[ i ].hashCode();
            code = 23 * code + styles_[ i ].hashCode();
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
            && yLog_ == other.yLog_;
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
