package uk.ac.starlink.ttools.plot;

import java.util.Arrays;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.convert.ValueConverter;

/**
 * Characterises the details of how a plot is to be done.
 * An instance of this class contains all the information which a 
 * plot component needs to draw a plot.
 * There are specific subclasses for the various different plot types.
 *
 * <p>Some of the items held by this object are arrays with one element
 * per axis.  Where appropriate these can be used to hold values for
 * the main axes, followed by values for any visible auxiliary axes.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotState {

    private boolean valid_;
    private int mainNdim_;
    private SimpleValueInfo[] axes_;
    private ValueConverter[] converters_;
    private boolean[] logFlags_;
    private boolean[] flipFlags_;
    private boolean grid_;
    private boolean antialias_;
    private PlotData plotData_;
    private double[][] ranges_ = new double[ 0 ][];
    private String[] axisLabels_ = new String[ 0 ];
    private Shader[] shaders_ = new Shader[ 0 ];

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
     * Sets the number of non-auxiliary axes represented by this state.
     *
     * @param   mainNdim  number of normal axes
     */
    public void setMainNdim( int mainNdim ) {
        mainNdim_ = mainNdim;
    }

    /**
     * Returns the number of non-auxiliary axes represented by this state.
     *
     * @return  number of normal axes
     */
    public int getMainNdim() {
        return mainNdim_;
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
     * Sets whether antialiasing hint is preferred for drawing lines.
     *
     * @param  antialias   true to antialias, false not
     */
    public void setAntialias( boolean antialias ) {
        antialias_ = antialias;
    }

    /**
     * Determines whether antialiasing is preferred for drawing lines.
     *
     * @return  true to antialias, false not
     */
    public boolean getAntialias() {
        return antialias_;
    }

    /**
     * Sets data ranges for each axis.
     * <code>ranges</code> is an N-element array of 2-element double arrays.
     * Each of its elements gives (low,high) limits for one dimension 
     * of the region to be displayed.
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
     * Sets the shader objects to use for modifying the colour of plotted
     * points according to auxiliary axis data.  The length of this array
     * defines the number of auxiliary axes in use.
     *
     * @param   shaders   shaders, one per auxiliary axis
     */
    public void setShaders( Shader[] shaders ) {
        shaders_ = shaders;
    }

    /**
     * Returns the shader objects for using auxiliary axis data.
     * 
     * @return  shaders, one per auxiliary axis
     */
    public Shader[] getShaders() {
        return shaders_;
    }

    /**
     * Sets an array of numeric converter objects, one for each axis.
     * The {@link uk.ac.starlink.ttools.convert.ValueConverter#unconvert}
     * method of these should convert a numeric value back to the
     * formatted (text) version of a value on the corresponding axis.
     * Any of the elements may be null if the value is numeric anyway.
     *
     * @param  converters  numeric converter array, one for each axis
     */
    public void setConverters( ValueConverter[] converters ) {
        converters_ = converters;
    }

    /**
     * Returns the array of numeric converter objects, one for each axis.
     *
     * @return  numeric converter array
     */
    public ValueConverter[] getConverters() {
        return converters_;
    }

    /**
     * Sets the plot data object for this state.
     *
     * @param   plotData  plot data object
     */
    public void setPlotData( PlotData plotData ) {
        plotData_ = plotData;
    }

    /**
     * Returns the plot data object for this state.
     *
     * @return  plot data object
     */
    public PlotData getPlotData() {
        return plotData_;
    }

    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof PlotState ) ) {
            return false;
        }
        PlotState other = (PlotState) otherObject;
        return valid_ == other.valid_
            && mainNdim_ == other.mainNdim_
            && grid_ == other.grid_
            && antialias_ == other.antialias_
            && Arrays.equals( axes_, other.axes_ )
            && Arrays.equals( logFlags_, other.logFlags_ )
            && Arrays.equals( flipFlags_, other.flipFlags_ )
            && Arrays.equals( axisLabels_, other.axisLabels_ )
            && Arrays.equals( shaders_, other.shaders_ )
            && Arrays.equals( converters_, other.converters_ )
            && equalRanges( ranges_, other.ranges_ )
            && ( plotData_ == null 
                    ? other.plotData_ == null
                    : plotData_.equals( other.plotData_ ) );
    }

    /**
     * Returns a string giving a list of items in which this state differs
     * from a given state <code>o</code>.  This method is used only for
     * debugging purposes, and may not be fully implemented at any given
     * time.
     *
     * @param  o  state for comparison with this one
     * @return   text summary of differences
     */
    public String compare( PlotState o ) {
        StringBuffer sbuf = new StringBuffer( "Mismatches:" );
        sbuf.append( valid_ == o.valid_ ? "" : " valid" );
        sbuf.append( mainNdim_ == o.mainNdim_ ? "" : " mainNdim" );
        sbuf.append( grid_ == o.grid_ ? "" : " grid" );
        sbuf.append( antialias_ == o.antialias_ ? "" : " antialias" );
        sbuf.append( Arrays.equals( axes_, o.axes_ ) ? "" : " axes" );
        sbuf.append( Arrays.equals( logFlags_, o.logFlags_ ) ? "" : " log" );
        sbuf.append( Arrays.equals( flipFlags_, o.flipFlags_  ) ? "" :" flip ");
        sbuf.append( Arrays.equals( axisLabels_, o.axisLabels_ )
                     ? "" : " axisLabels" );
        sbuf.append( Arrays.equals( shaders_, o.shaders_ ) ? "" : " shaders" );
        sbuf.append( Arrays.equals( converters_, o.converters_ ) 
                     ? "" : " converters" );
        sbuf.append( equalRanges( ranges_, o.ranges_ ) ? "" : " ranges" );
        sbuf.append( ( plotData_ == null ? o.plotData_ == null
                                         : plotData_.equals( o.plotData_ ) )
                     ? "" : " plotData" );
        return sbuf.toString();
    }

    public int hashCode() {
        int code = 555;
        code = 23 * code + ( valid_ ? 99 : 999 );
        code = 23 * code + mainNdim_;
        code = 23 * code + ( grid_ ? 11 : 17 );
        code = 23 * code + ( antialias_ ? 109 : 901 );
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
                               ? 0 : axisLabels_[ i ].hashCode() );
        }
        for ( int i = 0; i < shaders_.length; i++ ) {
            code = 23 * code + ( shaders_[ i ] == null
                               ? 0 : shaders_[ i ].hashCode() );
        }
        for ( int i = 0; i < converters_.length; i++ ) {
            code = 23 * code + ( converters_[ i ] == null
                               ? 0 : converters_[ i ].hashCode() );
        }
        for ( int i = 0; i < ranges_.length; i++ ) {
            code = 23 * code
                 + Float.floatToIntBits( (float) ranges_[ i ][ 0 ] );
            code = 23 * code
                 + Float.floatToIntBits( (float) ranges_[ i ][ 1 ] );
        }
        code = 23 * code + ( plotData_ == null 
                                ? 0
                                : plotData_.hashCode() );
        return code;
    }

    static boolean equalRanges( double[][] r1, double[][] r2 ) {
        if ( r1.length == r2.length ) {
            for ( int i = 0; i < r1.length; i++ ) {
                if ( r1[ i ] != null && r2[ i ] != null ) {
                    double a10 = r1[ i ][ 0 ];
                    double a11 = r1[ i ][ 1 ];
                    double a20 = r2[ i ][ 0 ];
                    double a21 = r2[ i ][ 1 ];
                    if ( ! ( a10 == a20 || ( Double.isNaN( a10 ) &&
                                             Double.isNaN( a20 ) ) ) ||
                         ! ( a11 == a21 || ( Double.isNaN( a11 ) &&
                                             Double.isNaN( a21 ) ) ) ) {
                        return false;
                    }
                }
                else if ( r1[ i ] != null ^ r2[ i ] != null ) {
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
