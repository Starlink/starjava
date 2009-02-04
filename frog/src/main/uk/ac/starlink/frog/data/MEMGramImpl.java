package uk.ac.starlink.frog.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.frog.util.FrogException;

/**
 * This class provides an implementation of GramImpl to access
 * gram stored in existing memory. All values are copied into
 * arrays stored in memory.
 * <p>
 * The main use of this class is for temporary, generated and copied
 * gram. 
 *
 * @author Alasdair Allan
 * @version $Id$
 * @since $Date$
 * @see "The Bridge Design Pattern" 
 */
public class MEMGramImpl extends GramImpl
{
//
//  Implementation of abstract methods.
//

    /**
     * Constructor - just take a symbolic name for the gram, no
     * other significance. 
     *
     * @param name a symbolic name for the gram.
     */
    public MEMGramImpl( String name )
    {
        super( name );
        this.shortName = name;
        this.fullName = name;
    }

    /**
     * Constructor, clone from another gram.
     *
     * @param name a symbolic name for the gram.
     * @param gram a Gram object to copy.
     */
    public MEMGramImpl( String name, Gram gram )
    {
        super( name, gram );
        this.shortName = name;
        if ( gram.haveYDataErrors() ) {
            setData( gram.getYData(), gram.getXData(),
                     gram.getYDataErrors() );
        }
        else {
            setData( gram.getYData(), gram.getXData() );
        }
    }

    /**
     *  Return a copy of the gram data values.
     *
     *  @return reference to the gram data values.
     */
    public double[] getData()
    {
        return data;
    }

   /**
     * Return a copy of the gram coord values.
     *
     * @return reference to the gram coord values.
     */
    public double[] getTime()
    {
        return coords;
    }
    
    /**
     *  Return a copy of the gram data errors.
     *
     *  @return reference to the gram data values.
     */
    public double[] getDataErrors()
    {
        return errors;
    }

    /**
     * Return a symbolic name.
     *
     * @return a symbolic name for the gram. Based on the filename.
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * Return the full name of gram. For memory gram this has
     * no real meaning (i.e. no disk file or URL) so always returns a
     * string reminding users that they need to save it.
     *
     *  @return the String "Memory gram".
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return the data array dimensionality (always length of gram).
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
     * relations used by this gram.
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
     * Reference to the symbolic name of gram.
     */
    protected String shortName = "Memory gram";

    /**
     * Reference to the full name of gram.
     */
    protected String fullName = "Memory gram";

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
     * Set the gram data. No errors.
     *
     * @param data the gram data values.
     * @param coords the gram coordinates, one per data value.
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
     * Set the gram data. With errors.
     *
     * @param data the gram data values.
     * @param coords the gram coordinates, one per data value.
     * @param errors the errors of the gram data values.
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
     * Create an AST frameset that relates the gram coordinate to
     * data values positions.
     */
    protected void createAst()
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates (time).
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Power" );
        Frame currentframe = new Frame( 1 );
        currentframe.set( "Label(1)=Frequency" );

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
