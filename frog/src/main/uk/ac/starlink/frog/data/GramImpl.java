package uk.ac.starlink.frog.data;

import java.io.Serializable;

import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.ast.FrameSet;

/**
 * ImeSeriesImpl - abstract base class for accessing periodogram
 *                 stored in various data formats.
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @since 09-JAN-2003
 */
public abstract class GramImpl implements Serializable
{
    /**
     * Constructor - create instance of class.
     *
     * @param name The specification of the periodogram (disk file name
     * etc.).
     */
    public GramImpl( String name ) {}
    
    /**
     * Constructor, clone from another periodogram.
     *
     * @param name a symbolic name for the periodogram.
     * @param gram the periodogram to clone.
     */
    public GramImpl( String name, Gram gram ) {}

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
     * Get the dimensionality of the periodogram.
     */
    abstract public int[] getDims();

    /**  
     * Return a symbolic name for the periodogram. 
     */
    abstract public String getShortName();

    /**  
     * Return a full name for the periodogram (this should be the disk
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
     * Ask periodogram if it can save itself to disk file. Throws
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
