package uk.ac.starlink.splat.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.splat.util.SplatException;

/**
 * This class provides an implementation of SpecDataImpl to access
 * spectra stored in existing memory. All values are copied into
 * arrays stored in memory.
 * <p>
 * The main use of this class is for temporary, generated and copied
 * spectra. It also provides the main facilities for spectra that can
 * be modified.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since $Date$
 * @since 06-JAN-2001
 * @see SpecDataImpl
 * @see EditableSpecData
 * @see "The Bridge Design Pattern" 
 */
public class MEMSpecDataImpl 
    extends EditableSpecDataImpl
{
//
//  Implementation of abstract methods.
//
    /**
     * Constructor - just take a symbolic name for the spectrum, no
     * other significance. 
     *
     * @param name a symbolic name for the spectrum.
     */
    public MEMSpecDataImpl( String name )
    {
        super( name );
        this.shortName = name;
    }

    /**
     * Constructor, clone from another spectrum.
     *
     * @param name a symbolic name for the spectrum.
     * @param spectrum a SpecData object to copy.
     */
    public MEMSpecDataImpl( String name, SpecData spectrum )
    {
        super( name, spectrum );
        this.shortName = name;
        if ( spectrum.haveYDataErrors() ) {
            setData( spectrum.getYData(), spectrum.getXData(),
                     spectrum.getYDataErrors() );
        }
        else {
            setData( spectrum.getYData(), spectrum.getXData() );
        }
    }

    /**
     *  Return a copy of the spectrum data values.
     *
     *  @return reference to the spectrum data values.
     */
    public double[] getData()
    {
        return data;
    }

    /**
     *  Return a copy of the spectrum data errors.
     *
     *  @return reference to the spectrum data values.
     */
    public double[] getDataErrors()
    {
        return errors;
    }

    /**
     * Return a symbolic name.
     *
     * @return a symbolic name for the spectrum. Based on the filename.
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * Return the full name of spectrum. For memory spectra this has
     * no real meaning (i.e. no disk file or URL) so always returns a
     * string reminding users that they need to save it.
     *
     *  @return the String "Memory spectrum".
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return the data array dimensionality (always length of
     * spectrum).
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
     * relations used by this spectrum.
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
     * Reference to the symbolic name of spectrum.
     */
    protected String shortName = "Memory spectrum";

    /**
     * Reference to the full name of spectrum.
     */
    protected String fullName = "Memory spectrum";

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
        if ( astref != null ) {
            astref.annul();
        }
        coords = null;
        data = null;
        errors = null;
        data = null;
        shortName = null;
        super.finalize();
    }

    /**
     * Create an AST frameset that relates the spectrum coordinate to
     * data values positions.
     */
    protected void createAst()
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates (wavelength).
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Data Counts" );
        Frame currentframe = new Frame( 1 );
        currentframe.set( "Label(1)=Wavelength" );

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.
        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, lutmap, currentframe );

        //  Free intermediary products.
        baseframe.annul();
        currentframe.annul();
        lutmap.annul();
    }

    /**
     * Save is just a copy for this class.
     *
     * @exception SplatException never thrown for this implementation.
     */
    public void save() throws SplatException
    {
        if ( errors != null ) {
	   setData( data, coords, errors );
        }
        else {
	   setData( data, coords );
	}
    }

    // 
    // Editable interface.
    //

    /**
     * Set the spectrum data. No errors. Takes complete copies of the
     * data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    public void setData( double[] data, double[] coords )
    {
        //  Create memory needed to store these coordinates.
        this.data = new double[data.length];
        this.coords = new double[data.length];

        //  Now copy data into arrays.
        System.arraycopy( coords, 0, this.coords, 0, data.length );
        System.arraycopy( data, 0, this.data, 0, data.length );

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
       createAst();
    }

    /**
     * Set the spectrum data. No errors. Doesn't copy the data, just
     * keeps references (quick but less safe).
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    public void setDataQuick( double[] data, double[] coords )
    {
        this.data = data;
        this.coords = coords;

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
       createAst();
    }

    /**
     * Set the spectrum data. With errors. Doesn't copy the data, just
     * keeps references (quick but less safe).
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values. Null for
     *               none. 
     */
    public void setData( double[] data, double[] coords, double[] errors )
    {
        setData( data, coords );
        if ( errors != null ) {
            this.errors = new double[data.length];
            System.arraycopy( errors, 0, this.errors, 0, data.length );
        }
        else {
            this.errors = null;
        }
    }

    /**
     * Set the spectrum data. With errors. Doesn't copy the data, just
     * keeps references (quick but less safe).
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values. Null for
     *               none. 
     */
    public void setDataQuick( double[] data, double[] coords, double[] errors )
    {
        setDataQuick( data, coords );
        this.errors = errors;
    }

    /**
     * Change a coordinate value. Need to re-generate the AST frameset
     * after each change (for batch changes, use the setData methods).
     */
    public void setXDataValue( int index, double value )
        throws SplatException
    {
        if ( index < data.length ) {
            coords[index] = value;
            createAst();
        }
        else {
            throw new SplatException( "Array index out of bounds" );
        }
    }

    /**
     * Change a data value.
     */
    public void setYDataValue( int index, double value )
        throws SplatException
    {
        if ( index < data.length ) {
            data[index] = value;
        }
        else {
            throw new SplatException( "Array index out of bounds" );
        }
    }

    /**
     * Change a data error value.
     */
    public void setYDataErrorValue( int index, double value )
        throws SplatException
    {
        if ( index < data.length ) {
            if ( value > 0.0 ) {
                errors[index] = value;
            }
            else {
                throw new SplatException
                    ( "Illegal data error. Must be greater than zero" );
            }
        }
        else {
            throw new SplatException( "Array index out of bounds" );
        }
    }
}
