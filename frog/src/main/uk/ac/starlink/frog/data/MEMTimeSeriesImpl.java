package uk.ac.starlink.frog.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.frog.util.FrogException;

/**
 * This class provides an implementation of TimeSeriesImpl to access
 * series stored in existing memory. All values are copied into
 * arrays stored in memory.
 * <p>
 * The main use of this class is for temporary, generated and copied
 * series. 
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @since $Date$
 * @since 06-JAN-2001
 * @see TimeSeriesImpl
 * @see TimeSeries
 * @see "The Bridge Design Pattern" 
 */
public class MEMTimeSeriesImpl extends TimeSeriesImpl
{
//
//  Implementation of abstract methods.
//

    /**
     * Constructor - just take a symbolic name for the series, no
     * other significance. 
     *
     * @param name a symbolic name for the series.
     */
    public MEMTimeSeriesImpl( String name )
    {
        super( name );
        this.shortName = name;
        this.fullName = name;
    }

    /**
     * Constructor, clone from another series.
     *
     * @param name a symbolic name for the series.
     * @param series a TimeSeries object to copy.
     */
    public MEMTimeSeriesImpl( String name, TimeSeries series )
    {
        super( name, series );
        this.shortName = name;
        if ( series.haveYDataErrors() ) {
            setData( series.getYData(), series.getXData(),
                     series.getYDataErrors() );
        }
        else {
            setData( series.getYData(), series.getXData() );
        }
    }

    /**
     *  Return a copy of the series data values.
     *
     *  @return reference to the series data values.
     */
    public double[] getData()
    {
        return data;
    }

   /**
     * Return a copy of the series coord values.
     *
     * @return reference to the series coord values.
     */
    public double[] getTime()
    {
        return coords;
    }
    
    /**
     *  Return a copy of the series data errors.
     *
     *  @return reference to the series data values.
     */
    public double[] getDataErrors()
    {
        return errors;
    }

    /**
     * Return a symbolic name.
     *
     * @return a symbolic name for the series. Based on the filename.
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * Return the full name of series. For memory series this has
     * no real meaning (i.e. no disk file or URL) so always returns a
     * string reminding users that they need to save it.
     *
     *  @return the String "Memory series".
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return the data array dimensionality (always length of series).
     *
     * @return integer array of size 1 returning the number of data
     *                 values available.
     */
    public int[] getDims()
    {
        int dummy[] = new int[1];
        dummy[0] = data.length;
        return dummy;
    }

    /**
     * Return reference to AST frameset that defines the coordinate
     * relations used by this series.
     *
     * @return reference to a raw AST frameset.
     */
    public FrameSet getAst()
    {
        return astref;
    }

    /**
     * Return the data format.
     *
     * @return the String "MEMORY".
     */
    public String getDataFormat()
    {
        return "MEMORY";
    }

//
//  Implementation specific methods and variables.
//
    /**
     * Reference to coordinates.
     */
    protected double[] coords = null;

    /**
     * Reference to data values.
     */
    protected double[] data = null;

    /**
     * Reference to data errors.
     */
    protected double[] errors = null;

    /**
     * Reference to the symbolic name of series.
     */
    protected String shortName = "Memory series";

    /**
     * Reference to the full name of series.
     */
    protected String fullName = "Memory series";

    /**
     * Reference to AST frameset.
     */
    protected FrameSet astref = null;

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize() throws Throwable
    {
        coords = null;
        data = null;
        errors = null;
        data = null;
        shortName = null;
        super.finalize();
    }

    /**
     * Set the series data. No errors.
     *
     * @param data the series data values.
     * @param coords the series coordinates, one per data value.
     */
    public void setData( double[] data, double[] coords )
    {
        //  Create memory needed to store these coordinates.
        this.data = new double[data.length];
        this.coords = new double[data.length];

        //  Now copy data into arrays.
        for ( int i = 0; i < data.length; i++ ) {
            this.coords[i] = coords[i];
            this.data[i] = data[i];
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
       createAst();
    }

    /**
     * Set the series data. With errors.
     *
     * @param data the series data values.
     * @param coords the series coordinates, one per data value.
     * @param errors the errors of the series data values.
     */
    public void setData( double[] data, double[] coords, double[] errors )
    {
        setData( data, coords );
        this.errors = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            this.errors[i] = errors[i];
        }
    }

    /**
     * Create an AST frameset that relates the series coordinate to
     * data values positions.
     */
    protected void createAst()
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates (time).
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Data Counts" );
        Frame currentframe = new Frame( 1 );
        currentframe.set( "Label(1)=Time" );

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.
        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, lutmap, currentframe );
    }

    /**
     * Save is just a copy for this class.
     *
     * @exception FrogException never thrown for this implementation.
     */
    public void save() throws FrogException
    {
        if ( errors != null ) {
	   setData( data, coords, errors );
        }
        else {
	   setData( data, coords );
	}
    }
}
