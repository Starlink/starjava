package uk.ac.starlink.frog.data;

import java.util.ArrayList;

import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 * This class is designed to handle multiple instances of TimeSeries
 * objects. Thus creating an apparent "composite" series, from
 * possibly several others.
 * <p>
 * This feature is intended, for instance, to allow the display
 * multiple series in a single plot and should be used when
 * referencing series data.
 * <p>
 * Note that the first series holds special status. This defines the
 * coordinate system that all other series should honour. It also is
 * reported first in constructs such as the name.
 * <p>
 * No alignment of coordinates using astConvert is attempted at present.
 *
 * @since $Date$
 * @since 21-SEP-2000
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see TimeSeries
 */
public class TimeSeriesComp implements TimeSeriesAccess 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /** 
     *  List of references to the series. Note that indexing this
     *  list isn't fixed, i.e. removing TimeSeries objects reshuffles
     *  the indices.  
     */
    protected ArrayList series = new ArrayList();

    /**
     *
     */
    protected int currentSeries;

    /**
     *  Create a TimeSeriesComp instance.
     */
    public TimeSeriesComp() 
    {
        //  Do nothing.
    }

    /**
     *  Create a TimeSeriesComp adding in the first series.
     */
    public TimeSeriesComp( TimeSeries inseries ) 
    {
        add( inseries );
        debugManager.print( "            TimeSeriesComp( TimeSeries )");
        debugManager.print( "              currentSeries = " + currentSeries );
    }

    /**
     *  Create a TimeSeriesComp adding in the first from a concrete
     *  implementation.
     */
    public TimeSeriesComp( TimeSeriesImpl inseries ) throws FrogException 
    {
        add( new TimeSeries( inseries ) );
        
        debugManager.print( "            TimeSeriesComp( TimeSeriesImpl )");
        debugManager.print( "              currentSeries = " + currentSeries );
    }

    /**
     *  Add a series to the managed list.
     * 
     *  @param inseries reference to a TimeSeries object that is to be
     *                added to the composite
     */
    public void add( TimeSeries inseries ) 
    {
        series.add( inseries );
    }

    /**
     *  Remove a series.
     *
     *  @param inseries reference to the series to remove.
     */
    public void remove( TimeSeries inseries ) 
    {
        series.remove( inseries );
        if ( currentSeries >= series.size() ) {
           currentSeries = series.size() - 1;
        }
    }

    /**
     *  Remove a series.
     *
     *  @param index the index of the series.
     */
    public void remove( int index ) 
    {
        series.remove( index );
        if ( currentSeries >= series.size() ) {
           currentSeries = series.size() - 1;
        }
    }
    
    /**
     * Set series which is the "current" series
     *
     * @param index the index of the series
     */
     public void setCurrentSeries( int index )
     {
        currentSeries = index;
     }
     
   /**
     * Get the "current" series index
     *
     * @return index Index of the current serie sin the stack
     */
     public int getCurrentSeries( )
     {
        return currentSeries;
     } 
     
   /**
     * Get the "current" series
     *
     * @return series The current time series
     */
     public TimeSeries getSeries( )
     {
        return (TimeSeries) series.get( currentSeries );
     }          
    

    /**
     *  Get a reference to a series.
     *
     *  @param index the index of the series.
     */
    public TimeSeries get( int index ) 
    {
        return (TimeSeries) series.get( index );
    }

    /**
     *  Get the index of a series.
     *
     *  @param index the index of the series.
     */
    public int indexOf( TimeSeries inseries ) 
    {
        return series.indexOf( inseries );
    }

    /**
     *  Get the number of series currently being handled.
     */
    public int count() 
    {
        return series.size();
    }

    /**
     *  Return if we already have a reference to a series.
     */
    public boolean have( TimeSeries s ) 
    {
        if ( series.indexOf( s ) > -1 ) {
            return true;
        } else {
            return false;
        }
    }

   /**
     *  Set the plotting style of the current series. The value should
     *  be one of the symbolic constants "POLYLINE" and "HISTOGRAM" or 
     *  "POINTS".
     *
     * @param style one of the symbolic contants TimeSeries.POLYLINE or
     *      TimeSeries.HISTOGRAM.
     */
     public void setPlotStyle( int style ) 
     {
          TimeSeries tmp = (TimeSeries)series.get(currentSeries);
          tmp.setPlotStyle( style );
     }     
          
   /**
     *  Set the marker style of the current series. The value should
     *  be one of the symbolic constants faound in DefaultGrfMarker
     *
     * @param marker e.g. DefaultGrfMarker.FILLEDSQUARE
     */
     public void setMarkerStyle( int style ) 
     {
          TimeSeries tmp = (TimeSeries)series.get(currentSeries);
          tmp.setMarkStyle( style );
     }   
       
   /**
     *  Set the marker size of the current series. 
     *
     * @param size default is 5.0
     */
     public void setMarkerStyle( double size ) 
     {
          TimeSeries tmp = (TimeSeries)series.get(currentSeries);
          tmp.setMarkSize( size );
     }   

     /**
     *  Find out wether the curretn plot has its error bars drawn
     *
     * @param state true to drawing errors, if possible
     */
     public boolean isDrawErrorBars( ) 
     {
          TimeSeries tmp = (TimeSeries)series.get(currentSeries);
          return tmp.isDrawErrorBars( );
     }   
          
     /**
     *  Set the current TimeSeries to display error bars if available
     *
     * @param state true to draw errors, if possible
     */
     public void setDrawErrorBars( boolean state ) 
     {
          TimeSeries tmp = (TimeSeries)series.get(currentSeries);
          tmp.setDrawErrorBars( state );
     }   
        
       
    /** 
     *  Get reference to AstUtilities object set up to specify the coordinate
     *  system. This always returns the AstUtilities object of the first
     *  series, so all other series must have a context that is
     *  valid within the coordinate system defined by it.  
     */
    public AstUtilities getAst() 
    {
        return ((TimeSeries)series.get(0)).getAst();
    }

    /**
     *  Get a symbolic name for all series.
     */
    public String getShortName() 
    {
        StringBuffer name = new StringBuffer( ((TimeSeries)series.get(0)).getShortName() );
        if ( series.size() > 1 ) {
            name.append("(+").append(series.size()-1).append(" others)");
        }
        return name.toString();
    }

    /**
     *  Get a full name for all series. Blank.
     */
    public String getFullName() 
    {
        return "";
    }

    /**
     *  Get the symbolic name of a series.
     */
    public String getShortName( int index ) 
    {
        return ((TimeSeries)series.get(index)).getShortName();
    }

    /**
     *  Get the full name of a series.
     */
    public String getFullName( int index ) 
    {
        return ((TimeSeries)series.get(index)).getFullName();
    }

    /**
     *  Get the data range of all the series
     */
    public double[] getRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < series.size(); i++ ) {
            double[] newrange = ((TimeSeries)series.get(i)).getRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
        }
        return range;
    }

    /**
     *  Get the full data range of all the series.
     */
    public double[] getFullRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < series.size(); i++ ) {
            double[] newrange = ((TimeSeries)series.get(i)).getFullRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
        }
        return range;
    }

    /**
     * Get the data range of the series, that should be used when
     * auto-ranging. Autoranging only uses series marked for this
     * purpose, unless there are no allowable series (in which case 
     * it would be bad to have no autorange). If errorbars are in use
     * then their range is also accomodated.
     */
    public double[] getAutoRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        TimeSeries spec = null;
        int count = series.size();
        int used = 0;
        double newrange[];
        for ( int i = 0; i < count; i++ ) {
            spec = (TimeSeries)series.get(i);
            if ( spec.isUseInAutoRanging() || count == 1 ) {
                if ( spec.isDrawErrorBars() ) {
                    newrange = spec.getFullRange();
                } 
                else {
                    newrange = spec.getRange();
                }
                range[0] = Math.min( range[0], newrange[0] );
                range[1] = Math.max( range[1], newrange[1] );
                range[2] = Math.min( range[2], newrange[2] );
                range[3] = Math.max( range[3], newrange[3] );
                used++;
            }
        }
        if ( used == 0 ) {
            range = getFullRange();
        }
        return range;
    }

    /**
     *  Draw all series using the graphics context provided.
     */
    public void drawSeries( Grf grf, Plot plot, double[] limits ) 
    {
        for ( int i = 0; i < series.size(); i++ ) {
            ((TimeSeries)series.get(i)).drawSeries( grf, plot, limits );
        }
    }

    /**
     *  Lookup the physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate.
     *  <p>
     *  Note that this only works for first series.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public double[] lookup( int xg, Plot plot ) 
    {
        return ((TimeSeries)series.get(0)).lookup( xg, plot );
    }

    /**
     *  Lookup the physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first series.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public String[] formatLookup( int xg, Plot plot ) 
    {
        return ((TimeSeries)series.get(0)).formatLookup( xg, plot );
    }

    /**
     *  Lookup interpolated physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first series.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public String[] formatInterpolatedLookup( int xg, Plot plot ) 
    {
        return ((TimeSeries)series.get(0)).formatInterpolatedLookup( xg, plot );
    }

    /**
     * Convert a formatted value into a floating value coordinates
     * (the input could be hh:mm:ss.s, in which case we get back
     * suitable radians). 
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the formatted value.
     *  @return the unformatted value.
     */
    public double unFormat( int axis, Plot plot, String value ) 
    {
        return ((TimeSeries)series.get(0)).unFormat( axis, plot, value );
    }

    /**
     *  Convert a floating point coordinate into a value formatted for
     *  a given axis.
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the value.
     *  @return the formatted value.
     */
    public String format( int axis, Plot plot, double value ) 
    {
        return ((TimeSeries)series.get(0)).format( axis, plot, value );
    }

    /**
     *  Get the size of the series (first only).
     */
    public int size() 
    {
        return ((TimeSeries)series.get(0)).size();
    }
    
    /**
     * Return a reference to this object
     *
     * @return refernce to this object
     */
     public TimeSeriesComp getContainer()
     {
        return this;
     }
     
}
