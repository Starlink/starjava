package uk.ac.starlink.frog.data;

import java.io.Serializable;

import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.ast.FrameSet;

/**
 * ImeSeriesImpl - abstract base class for accessing time series
 *                 stored in various data formats.
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @since 09-JAN-2003
 */
public abstract class TimeSeriesImpl implements Serializable
{
    /**
     * Constructor - create instance of class.
     *
     */
    public TimeSeriesImpl( ) {}
     
     /**
     * Constructor - create instance of class.
     *
     * @param name The specification of the time series (disk file name
     * etc.).
     */
    public TimeSeriesImpl( String name ) {}
    
    /**
     * Constructor, clone from another time series.
     *
     * @param name a symbolic name for the time series.
     * @param series the time series to clone.
     */
    public TimeSeriesImpl( String name, TimeSeries series ) {}

    /**  
     * Return a complete array of data values. 
     */
    abstract public double[] getData();

   /**  
     * Return a complete array of time values. 
     */
    abstract public double[] getTime();

    /**  
     * Return a complete array of data error values. 
     */
    abstract public double[] getDataErrors();

    /**
     * Get the dimensionality of the time series.
     */
    abstract public int[] getDims();

    /**  
     * Return a symbolic name for the time series. 
     */
    abstract public String getShortName();

    /**  
     * Return a full name for the time series (this should be the disk
     * file is associated, otherwise a URL or null).
     */
    abstract public String getFullName();

    /** 
     * Return reference to AST frameset that specifies the
     * coordinates associated with the data value positions.
     */
    abstract public FrameSet getAst();

    /**
     * Return the data format as a recognisable short name.
     */
    abstract public String getDataFormat();

    /**
     * Ask time series if it can save itself to disk file. Throws
     * FrogException is this fails, or is not available.
     */
    abstract public void save() throws FrogException;

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found. This default implementation returns "";
     */
    public String getProperty( String key )
    {
        return "";
    }
}
