package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Characterises the details of how a plot is to be done.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotState {

    private boolean valid_;
    private SimpleValueInfo[] axes_;
    private boolean[] logFlags_;
    private boolean[] flipFlags_;
    private boolean grid_;
    private PointSelection pointSelection_;
    private double[][] ranges_ = new double[ 0 ][];
    private String[] axisLabels_ = new String[ 0 ];

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
     * Sets the metadata for axes to be plotted.  Note the submitted
     * <code>axes</code> array is not used directly, the relevant information
     * is abstracted from it and stored (subsequent calls of {@link #getAxes}
     * will not return the same array or component objects).
     *
     * @param  axes  axis metadata array
     */
    public void setAxes( ValueInfo[] axes ) {
        axes_ = new SimpleValueInfo[ axes.length ];
        for ( int i = 0; i < axes.length; i++ ) {
            axes_[ i ] = new SimpleValueInfo( axes[ i ] );
        }
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
     * Sets data ranges for each axis.
     * <code>ranges</code> is an N-element array of 2-element double arrays.
     * Each of its elements gives (low,high) limits for the data to be 
     * considered.  Either or both elements may be NaN.
     * A non-NaN value is considered as a request to fix the lower/upper
     * limit of the indicated axis to the value given.  A NaN value
     * normally means that the limit should be determined dynamically
     * (by assessing the range of the available data points).
     *
     * @param   ranges   array of (low,high) fixed range limits
     */
    public void setRanges( double[][] ranges ) {
        ranges_ = ranges;
    }

    /**
     * Returns the data ranges for each axis.
     *
     * @return   array of (low,high) fixed range limits
     * @see      #setRanges
     */
    public double[][] getRanges() {
        return ranges_;
    }

    /**
     * Sets the text labels to use for annotating axes.
     *
     * @param   labels   axis annotation strings, one for each axis that
     *          needs labelling
     */
    public void setAxisLabels( String[] labels ) {
        axisLabels_ = labels;
    }
 
    /**
     * Returns the labels to use for annotating axes.
     *
     * @return  axis annotation strings, one for each axis that needs
     *          labelling
     */
    public String[] getAxisLabels() {
        return axisLabels_;
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
            && Arrays.equals( flipFlags_, other.flipFlags_ )
            && equalRanges( ranges_, other.ranges_ );
    }

    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof PlotState ) ) {
            return false;
        }
        PlotState other = (PlotState) otherObject;
        return valid_ == other.valid_
            && grid_ == other.grid_
            && Arrays.equals( axes_, other.axes_ )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ )
            && Arrays.equals( axisLabels_, other.axisLabels_ )
            && ( pointSelection_ == null 
                    ? other.pointSelection_ == null
                    : pointSelection_.equals( other.pointSelection_ ) );
    }

    String compare( PlotState o ) {
        StringBuffer sbuf = new StringBuffer( "Mismatches:" );
        sbuf.append( valid_ == o.valid_ ? "" : " valid" );
        sbuf.append( grid_ == o.grid_ ? "" : " grid" );
        sbuf.append( Arrays.equals( axes_, o.axes_ ) ? "" : " axes" );
        sbuf.append( Arrays.equals( logFlags_, o.logFlags_ ) ? "" : " log" );
        sbuf.append( Arrays.equals( flipFlags_, o.flipFlags_  ) ? "" :" flip ");
        sbuf.append( ( pointSelection_ == null 
                           ? o.pointSelection_ == null
                           : pointSelection_.equals( o.pointSelection_ ) )
                        ? "" : " pointSelection" );
        return sbuf.toString();
    }

    public int hashCode() {
        int code = 555;
        code = 23 * code + ( valid_ ? 99 : 999 );
        code = 23 * code + ( grid_ ? 11 : 17 );
        for ( int i = 0; i < axes_.length; i++ ) {
            code = 23 * code + axes_[ i ].hashCode();
        }
        for ( int i = 0; i < logFlags_.length; i++ ) {
            code = 23 * code + ( logFlags_[ i ] ? 1 : 2 );
        }
        for ( int i = 0; i < flipFlags_.length; i++ ) {
            code = 23 * code + ( flipFlags_[ i ] ? 1 : 2 );
        }
        for ( int i = 0; i < axisLabels_.length; i++ ) {
            code = 23 * code + ( axisLabels_[ i ] == null
                               ? 0 :axisLabels_[ i ].hashCode() );
        }
        for ( int i = 0; i < ranges_.length; i++ ) {
            code = 23 * code + 
                  (int) Double.doubleToLongBits( ranges_[ i ][ 0 ] );
            code = 23 * code +
                  (int) Double.doubleToLongBits( ranges_[ i ][ 1 ] );
        }
        code = 23 * code + ( pointSelection_ == null 
                                ? 0
                                : pointSelection_.hashCode() );
        return code;
    }

    private static boolean equalRanges( double[][] r1, double[][] r2 ) {
        if ( r1.length == r2.length ) {
            for ( int i = 0; i < r1.length; i++ ) {
                double a10 = r1[ i ][ 0 ];
                double a11 = r1[ i ][ 1 ];
                double a20 = r2[ i ][ 0 ];
                double a21 = r2[ i ][ 1 ];
                if ( ! ( a10 == a20 ||
                         ( Double.isNaN( a10 ) && Double.isNaN( a20 ) ) ) ||
                     ! ( a11 == a21 ||
                         ( Double.isNaN( a11 ) && Double.isNaN( a21 ) ) ) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * ValueInfo implementation which ignores information that's not
     * relevant to plotting.  The point of using this is so that we can
     * implement its equals method suitably.
     */
    protected static class SimpleValueInfo extends DefaultValueInfo {

        public SimpleValueInfo( ValueInfo baseInfo ) {
            super( baseInfo.getName(), baseInfo.getContentClass() );
            String units = baseInfo.getUnitString();
            String desc = baseInfo.getDescription();
            setUnitString( units == null ? "" : units );
            setDescription( desc == null ? "" : desc );
        }

        public boolean equals( Object o ) {
            if ( o instanceof SimpleValueInfo ) {
                SimpleValueInfo other = (SimpleValueInfo) o;
                return getName().equals( other.getName() )
                    && getContentClass().equals( other.getContentClass() )
                    && getUnitString().equals( other.getUnitString() )
                    && getDescription().equals( other.getDescription() );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 5555;
            code = 23 * code + getName().hashCode();
            code = 23 * code + getContentClass().hashCode();
            code = 23 * code + getUnitString().hashCode();
            code = 23 * code + getDescription().hashCode();
            return code;
        }
    }
}
