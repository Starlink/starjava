package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import uk.ac.starlink.table.ValueInfo;

/**
 * PlotState subclass for use with stacked line plots.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesPlotState extends PlotState {

    private final int ngraph_;
    private SimpleValueInfo[] yAxes_;
    private String[] yAxisLabels_ = new String[ 0 ];
    private double[][] yRanges_;
    private boolean[] yLogFlags_ = new boolean[ 0 ];
    private boolean[] yFlipFlags_ = new boolean [ 0 ];
    private int[] graphIndices_;

    /**
     * Constructs a new state.
     *
     * @param   ngraph  number of graphs in the stack
     */
    public LinesPlotState( int ngraph ) {
        ngraph_ = ngraph;
    }

    /**
     * Returns the number of graphs in the stack.
     *
     * @return  graph count
     */
    public int getGraphCount() {
        return ngraph_;
    }

    /**
     * Sets the metadata for the different Y axes to be plotted
     * (one for each graph).  Note the submitted <code>axes</code> 
     * array is not used directly, the relevant information is 
     * abstracted from it and stored (subsequent calls of {@link #getYAxes}
     * will not return the same array or component objects).
     *
     * @param  yAxes  Y axis metadata array
     */
    public void setYAxes( ValueInfo[] yAxes ) {
        yAxes_ = new SimpleValueInfo[ yAxes.length ];
        for ( int i = 0; i < yAxes.length; i++ ) {
            if ( yAxes[ i ] != null ) {
                yAxes_[ i ] = new SimpleValueInfo( yAxes[ i ] );
            }
        }
    }

    /**
     * Sets the text labels for the different Y axes to be plotted
     * (one for each graph).
     *
     * @param  yAxisLabels  Y axis label array
     */
    public void setYAxisLabels( String[] yAxisLabels ) {
        yAxisLabels_ = yAxisLabels;
    }

    /**
     * Returns the text labels for the different Y axes to be plotted
     * (one for each graph).
     *
     * @return  Y axis label array
     */
    public String[] getYAxisLabels() {
        return yAxisLabels_;
    }

    /**
     * Returns the metatdata for the different Y axes to be plotted
     * (one for each graph).
     *
     * @return  Y axis metadata array
     */
    public ValueInfo[] getYAxes() {
        return yAxes_;
    }

    /**
     * Sets data ranges for each different Y axis (one for each graph).
     * <code>yRanges</code> is an N-element array of 2-element double arrays.
     * Each of its elements gives (low,high) limits for the data to be
     * considered.  Either or both elements may be NaN.
     * A non-NaN value is considered as a request to fix the lower/upper
     * limit of the indicated axis to the value given.  A NaN value
     * normally means that the limit should be determined dynamically
     * (by assessing the range of the available data points).
     *
     * @param   ranges   array of (low,high) fixed range limits
     */
    public void setYRanges( double[][] yRanges ) {
        yRanges_ = yRanges;
    }

    /**
     * Returns the data ranges for each different Y axis (one for each graph).
     *
     * @return   array of (low,high) fixed range limits
     * @see      #setRanges
     */
    public double[][] getYRanges() {
        return yRanges_;
    }

    /**
     * Sets flags for which Y axes will be plotted logarithmically 
     * (one for each graph).
     *
     * @param   yLogFlags   Y log flags 
     */
    public void setYLogFlags( boolean[] yLogFlags ) {
        yLogFlags_ = yLogFlags;
    }

    /**
     * Returns flags for which Y axes will be plotted logarithmically
     * (one for each graph).
     *
     * @return   Y log flags
     */
    public boolean[] getYLogFlags() {
        return yLogFlags_;
    }

    /**
     * Sets flags for which Y axes will be plotted inverted
     * (one for each graph).
     *
     * @param   yFlipFlags  Y flip flags
     */
    public void setYFlipFlags( boolean[] yFlipFlags ) {
        yFlipFlags_ = yFlipFlags;
    }

    /**
     * Returns flags for which Y axes will be plotted inverted
     * (one for each graph).
     *
     * @return   Y flip flags
     */
    public boolean[] getYFlipFlags() {
        return yFlipFlags_;
    }

    /**
     * Sets the mapping of subsets to graph indices.
     * This defines which graph each subset will be displayed in.
     * The <i>i</i>'th element of the array gives the index of the 
     * graph that subset <i>i</i> will be displayed in.
     *
     * @param   graphIndicies  subset to graph mapping
     */
    public void setGraphIndices( int[] graphIndices ) {
        graphIndices_ = graphIndices;
    }

    /**
     * Returns the mapping of subsets to graph indices.
     *
     * @return  subset to graph mapping
     */
    public int[] getGraphIndices() {
        return graphIndices_;
    }

    public boolean sameAxes( PlotState o ) {
        if ( super.sameAxes( o ) && o instanceof LinesPlotState ) {
            LinesPlotState other = (LinesPlotState) o;
            return Arrays.equals( yAxes_, other.yAxes_ )
                && equalRanges( yRanges_, other.yRanges_ )
                && Arrays.equals( graphIndices_, other.graphIndices_ )
                && Arrays.equals( yLogFlags_, other.yLogFlags_ )
                && Arrays.equals( yFlipFlags_, other.yFlipFlags_ );
        }
        else {
            return false;
        }
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof LinesPlotState ) {
            LinesPlotState other = (LinesPlotState) o;
            if ( ! getValid() && ! other.getValid() ) {
                return true;
            }
            return Arrays.equals( yAxes_, other.yAxes_ )
                && Arrays.equals( yAxisLabels_, other.yAxisLabels_ )
                && equalRanges( yRanges_, other.yRanges_ )
                && Arrays.equals( graphIndices_, other.graphIndices_ )
                && Arrays.equals( yLogFlags_, other.yLogFlags_ )
                && Arrays.equals( yFlipFlags_, other.yFlipFlags_ );
        }
        else {
            return false;
        }
    }

    String compare( PlotState o ) {
        StringBuffer sbuf = new StringBuffer( super.compare( o ) );
        if ( o instanceof LinesPlotState ) {
            LinesPlotState other = (LinesPlotState) o;
            sbuf.append( Arrays.equals( yAxes_, other.yAxes_ )
                         ? "" : " yAxes" );
            sbuf.append( equalRanges( yRanges_, other.yRanges_ )
                         ? "" : " yRanges" );
            sbuf.append( Arrays.equals( graphIndices_, other.graphIndices_ )
                         ? "" : " graphIndices" );
            sbuf.append( Arrays.equals( yLogFlags_, other.yLogFlags_ )
                         ? "" : " yLogFlags" );
            sbuf.append( Arrays.equals( yFlipFlags_, other.yFlipFlags_ )
                         ? "" : " yFlipFlags" );
        }
        return sbuf.toString();
    }

    public int hashCode() {
        int code = super.hashCode();
        for ( int i = 0; i < yAxes_.length; i++ ) {
            code = 23 * code + 
                   ( yAxes_[ i ] == null ? 0 : yAxes_[ i ].hashCode() );
        }
        for ( int i = 0; i < yAxisLabels_.length; ) {
            code = 23 * code + ( yAxisLabels_[ i ] == null 
                               ? 0 : yAxisLabels_[ i ].hashCode() );
        }
        for ( int i = 0; i < yRanges_.length; i++ ) {
            code = 23 * code
                 + Float.floatToIntBits( (float) yRanges_[ i ][ 0 ] );
            code = 23 * code
                 + Float.floatToIntBits( (float) yRanges_[ i ][ 1 ] );
        }
        for ( int i = 0; i < graphIndices_.length; i++ ) {
            code = 23 * code + graphIndices_[ i ];
        }
        for ( int i = 0; i < yLogFlags_.length; i++ ) {
            code = 23 * code + ( yLogFlags_[ i ] ? 1 : 2 );
        }
        for ( int i = 0; i < yFlipFlags_.length; i++ ) {
            code = 23 * code + ( yFlipFlags_[ i ] ? 1 : 2 );
        }
        return code;
    }
}
