/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import nom.tam.fits.Header;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.ast.ASTJ;;

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
 * @see SpecDataImpl
 * @see EditableSpecData
 * @see "The Bridge Design Pattern"
 */
public class MEMSpecDataImpl
    extends AbstractEditableSpecDataImpl
    implements FITSHeaderSource
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
        throws SplatException
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
        throws SplatException
    {
        super( name, spectrum );
        this.shortName = name;

        //  Use the AST system from the spectrum, rather than creating
        //  our own.
        setAstCopy( spectrum.getFrameSet() );
        copyData( spectrum.getXData(), spectrum.getYData(), 
                  spectrum.getYDataErrors() );

        //  Record any source of header information.
        if ( spectrum.getSpecDataImpl().isFITSHeaderSource() )
        {
            headers = ((FITSHeaderSource)spectrum.getSpecDataImpl())
                .getFitsHeaders();
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
        astref = null;
        coords = null;
        data = null;
        errors = null;
        data = null;
        shortName = null;
        super.finalize();
    }

    /**
     * Create an AST frameset that relates the spectrum coordinate to
     * data values positions. This is straight-forward and just
     * creates a look-up-table to map the given coordinates to grid
     * positions. The lookup table mapping is simplified as this
     * should cause any linear transformations to be replaced with a
     * WinMap.
     */
    protected void createAst()
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates. Note we no longer
        //  label these as known.
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Data count" );
        Frame currentframe = new Frame( 1 );
        currentframe.set( "Label(1)=X coordinate" );

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.
        if ( coords == null ) {
            createCoords();
        }
        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );
        Mapping simple = lutmap.simplify();

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, simple, currentframe );
    }

    /**
     * Save is just a copy for this class.
     *
     * @exception SplatException never thrown for this implementation.
     */
    public void save() 
        throws SplatException
    {
        if ( errors != null ) {
	   setData( astref, data, errors );
        }
        else {
	   setData( astref, data );
	}
    }

    /**
     * Make local copies of any given coordinates, data and errors.
     * Caller is responsible for post-initialisation (i.e. calling
     * createAst, if no FrameSet is available) and ensuring that the
     * post-setup is valid (i.e. if coords and data lengths match, and
     * that data and coords or a WCS are present). Errors are reset if
     * none are given.
     */
    protected void copyData( double[] coords, double[] data, double[] errors )
        throws SplatException
    {
        try {
            if ( coords != null ) {
                this.coords = new double[coords.length];
                System.arraycopy( coords, 0, this.coords, 0, coords.length );
            }
            if ( data != null ) {
                this.data = new double[data.length];
                System.arraycopy( data, 0, this.data, 0, data.length );
            }
            
            //  Errors are special, null means none are present.
            if ( errors != null ) {
                this.errors = new double[data.length];
                System.arraycopy( errors, 0, this.errors, 0, data.length );
            }
            else {
                this.errors = null;
            }
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
    }

    //
    // FITSHeaderSource implementation.
    //

    /**
     * Any headers that we're carrying around.
     */
    protected Header headers = new Header();

    /**
     * Return any FITS headers we have accumilated.
     */
    public Header getFitsHeaders()
    {
        return headers;
    }

    //
    // Editable interface.
    //

    /**
     * Set the spectrum data and coordinates. No errors. Takes
     * complete copies of the data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     */
    public void setData( double[] coords, double[] data )
        throws SplatException
    {
        copyData( coords, data, null );
        createAst();
    }

    /**
     * Set the spectrum data and WCS. No errors. Takes complete copies
     * of the data.
     *
     * @param frameSet the FrameSet for mapping data indices to
     *                 coordinates.
     * @param data the spectrum data values.
     */
    public void setData( FrameSet frameSet, double[] data )
        throws SplatException
    {
        copyData( null, data, null );
        setAstCopy( frameSet );
    }

    /**
     * Set the spectrum data. No errors. Doesn't copy the data, just
     * keeps references (quick but less safe).
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     */
    public void setDataQuick( double[] coords, double[] data )
    {
        this.data = data;
        this.coords = coords;
        createAst();
    }

    /**
     * Set the spectrum data and WCS. No errors. Doesn't copy the
     * data, just keeps references (quick but less safe).
     *
     * @param frameSet the FrameSet for mapping data indices to
     *                 coordinates.
     * @param data the spectrum data values.
     */
    public void setDataQuick( FrameSet frameSet, double[] data )
    {
        this.data = data;
        setAstCopy( frameSet );
    }

    /**
     * Set the spectrum data. With errors.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values. Null for
     *               none.
     */
    public void setData( double[] coords, double[] data, double[] errors )
        throws SplatException
    {
        copyData( coords, data, errors );
        createAst();
    }

    /**
     * Set the spectrum data and WCS. With errors.
     *
     * @param frameSet the FrameSet for mapping data indices to
     *                 coordinates.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values. Null for
     *               none.
     */
    public void setData( FrameSet frameSet, double[] data, double[] errors )
        throws SplatException
    {
        copyData( null, data, errors );
        setAstCopy( frameSet );
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
    public void setDataQuick( double[] coords, double[] data, double[] errors )
    {
        setDataQuick( coords, data );
        this.errors = errors;
    }

    /**
     * Set the spectrum data and WCS. With errors. Doesn't copy the
     * data, just keeps references (quick but less safe).
     *
     * @param frameSet the FrameSet for mapping data indices to
     *                 coordinates.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values. Null for
     *               none.
     */
    public void setDataQuick( FrameSet frameSet, double[] data,
                              double[] errors )
    {
        this.data = data;
        this.errors = errors;
        setAstCopy( frameSet );
        createCoords();
    }

    /**
     * Change a coordinate value. Need to re-generate the AST frameset
     * after each change (for batch changes, use the setData methods).
     */
    public void setXDataValue( int index, double value )
        throws SplatException
    {
        if ( index < data.length ) {
            if ( coords == null ) {
                createCoords();
            }
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

    /**
     * Accept a new FrameSet. The existing FrameSet is annulled,
     * unless a reference to it is given.
     */
    public void setAst( FrameSet frameSet )
        throws SplatException
    {
        if ( frameSet != astref ) {
            astref = frameSet;

            //  Coordinates are now invalid and need re-generating.
            coords = null;
        }
    }

    /**
     * Accept a new FrameSet making a copy of it. The existing
     * FrameSet is annulled, unless a reference to it is given. If the
     * copy fails then a default FrameSet is created.
     */
    public void setAstCopy( FrameSet frameSet )
    {
        try {
            setAst( (FrameSet) frameSet.copy() );
        }
        catch (SplatException e ) {
            e.printStackTrace();

            //  Failed so try to use a default FrameSet.
            createAst();
        }
    }

    /**
     * Create a set of coordinates that match the current
     * FrameSet. This is necessary so that "createAst" can always
     * succeed and is necessary for single editable coordinates. This
     * may fail if the WCS isn't 1D, so we need to work around that
     * possibility.
     */
    protected void createCoords()
    {
        //  Generate positions along the data array
        coords = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            coords[i] = (double) ( i + 1 );
        }
        Mapping mapping = ASTJ.get1DFrameSet( astref, 1 );
        coords = ASTJ.astTran1( mapping, coords, true );
    }
}
