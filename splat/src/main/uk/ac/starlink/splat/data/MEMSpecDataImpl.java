/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     06-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.splat.ast.ASTJ;
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
        clone( spectrum );
    }

    /**
     * Clone this spectrum from another spectrum.
     */
    protected void clone( SpecData spectrum )
        throws SplatException
    {
        //  Use the AST system from the spectrum, rather than creating our
        //  own. Pick the first axis of the SpecData FrameSet. This should be
        //  1D and match the physical to data coordinates, but note we are
        //  losing any connection to the original WCS and dimensionality.
        FrameSet mainFrameSet = spectrum.getAst().getRef();
        FrameSet frameSet = ASTJ.get1DFrameSet( mainFrameSet, 1 );
        setAstCopy( frameSet );
        copyData( spectrum.getXData(), spectrum.getYData(),
                  spectrum.getYDataErrors() );

        //  Record any source of header information.
        if ( spectrum.getSpecDataImpl().isFITSHeaderSource() ) {
            headers =
               ((FITSHeaderSource)spectrum.getSpecDataImpl()).getFitsHeaders();
        }

        //  Retain the data units and label.
        setDataUnits( mainFrameSet.getC( "unit(2)" ) );
        setDataLabel( mainFrameSet.getC( "label(2)" ) );
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

    /**
     * Return a known descriptive label. The only fully supported values are
     * "title" (shortname), "label" and "units". These may be retained from a
     * spectrum used to initialise this one. Other values may be queried from
     * any attached FITS headers.
     */
    public String getProperty( String prop )
    {
        String result = super.getProperty( prop );
        if ( ! "".equals( result ) ) {
            return result;
        }
        if ( prop.equalsIgnoreCase( "title" ) ) {
            return shortName;
        }

        if ( getFitsHeaders() != null ) {
            String scard = getFitsHeaders().findKey( prop );
            if ( scard != null ) {
                HeaderCard card = new HeaderCard( scard );
                if ( card != null ) {
                    return card.getValue();
                }
            }
        }
        return "";
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
     * Create an AST {@link FrameSet} that relates the spectrum coordinate to
     * data values positions. Using the given FrameSet to define the
     * coordinate systems of the coordinates and data (the coordinate system is
     * the current {@link Frame} and the data the base Frame). The FrameSet
     * should be 1D and if null then default systems will be created.
     */
    protected void createAst( FrameSet sourceSet )
    {
        if ( sourceSet != null ) {
            createAst( (Frame) sourceSet.getFrame( FrameSet.AST__BASE ).copy(),
                       (Frame) sourceSet.getFrame( FrameSet.AST__CURRENT ).copy() );
        }
        else {
            createAst();
        }
    }

    /**
     * Create an default AST FrameSet when we have no information about the
     * coordinate system or data units.
     */
    protected void createAst()
    {
        createAst( null, null );
    }

    /**
     * Create an AST {@link FrameSet} that relates the spectrum coordinate to
     * data values positions. Using the given {@link Frame}'s to define the
     * coordinate systems of the coordinates and data (the coordinate system
     * is the current and base the data). The Frames should be 1D and if null
     * then default systems will be created.
     */
    protected void createAst( Frame baseframe, Frame currentframe )
    {
        if ( baseframe == null ) {
            baseframe = new Frame( 1 );
            baseframe.set( "Label(1)=Data count" );
        }
        if ( currentframe == null ) {
            currentframe = new Frame( 1 );
            currentframe.set( "Label(1)=X coordinate" );
        }

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.
        if ( coords == null ) {
            createCoords();
        }
        if ( coords.length == 1 ) {
            //  Single point LutMaps are not allowed.
            double[] newCoords = new double[2];
            newCoords[0] = coords[0];
            newCoords[1] = coords[0];
            coords = newCoords;
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
            setFullData( astref, dataUnits, data, errors );
        }
        else {
	   setFullData( astref, dataUnits, data );
	}
    }

    /**
     * Make local copies of any given coordinates, data and errors.  Caller is
     * responsible for post-initialisation (i.e. calling createAst, if no
     * FrameSet is available) and ensuring that the post-setup is valid
     * (i.e. if coords and data lengths match, and that data and coords or a
     * WCS are present). Errors are reset if none are given.
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
     * Return any FITS headers we have accumulated.
     */
    public Header getFitsHeaders()
    {
        return headers;
    }

    //
    // Editable interface.
    //

    public void setSimpleData( double[] coords, String dataUnits,
                               double[] data )
        throws SplatException
    {
        copyData( coords, data, null );
        setDataUnits( dataUnits );
        createAst();
    }

    public void setSimpleUnitData( FrameSet sourceSet, double[] coords,
                                   String dataUnits, double[] data )
        throws SplatException
    {
        copyData( coords, data, null );
        setDataUnits( dataUnits );
        createAst( sourceSet );
    }

    public void setFullData( FrameSet frameSet, String dataUnits, 
                             double[] data )
        throws SplatException
    {
        copyData( null, data, null );
        setDataUnits( dataUnits );
        setAstCopy( frameSet );
    }

    public void setSimpleDataQuick( double[] coords, String dataUnits,
                                    double[] data )
    {
        this.data = data;
        this.coords = coords;
        setDataUnits( dataUnits );
        createAst();
    }

    public void setSimpleUnitDataQuick( FrameSet sourceSet, double[] coords,
                                        String dataUnits, double[] data )
    {
        this.data = data;
        this.coords = coords;
        setDataUnits( dataUnits );
        createAst( sourceSet );
    }

    public void setFullDataQuick( FrameSet frameSet, String dataUnits,
                                  double[] data )
    {
        this.data = data;
        setDataUnits( dataUnits );
        setAstCopy( frameSet );
    }

    public void setSimpleData( double[] coords, String dataUnits, 
                               double[] data, double[] errors )
        throws SplatException
    {
        copyData( coords, data, errors );
        setDataUnits( dataUnits );
        createAst();
    }

    public void setSimpleUnitData( FrameSet sourceSet, double[] coords,
                                   String dataUnits, double[] data, 
                                   double[] errors )
        throws SplatException
    {
        copyData( coords, data, errors );
        setDataUnits( dataUnits );
        createAst( sourceSet );
    }

    public void setFullData( FrameSet frameSet, String dataUnits,
                             double[] data, double[] errors )
        throws SplatException
    {
        copyData( null, data, errors );
        setDataUnits( dataUnits );
        setAstCopy( frameSet );
    }

    public void setSimpleDataQuick( double[] coords, String dataUnits,
                                    double[] data, double[] errors )
    {
        setSimpleDataQuick( coords, dataUnits, data );
        setDataUnits( dataUnits );
        this.errors = errors;
    }

    public void setSimpleUnitDataQuick( FrameSet sourceSet, double[] coords,
                                        String dataUnits, double[] data, 
                                        double[] errors )
    {
        setSimpleUnitDataQuick( sourceSet, coords, dataUnits, data );
        this.errors = errors;
    }

    public void setFullDataQuick( FrameSet frameSet, String dataUnits,
                                  double[] data, double[] errors )
    {
        this.data = data;
        this.errors = errors;
        setDataUnits( dataUnits );
        setAstCopy( frameSet );
        createCoords();
    }

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
     * Accept a new FrameSet making a copy of it. If the copy fails then a
     * default FrameSet is created.
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
     * Create a set of coordinates that match the current FrameSet. This is
     * necessary so that "createAst" can always succeed and is necessary for
     * single editable coordinates. This may fail if the WCS isn't 1D, so we
     * need to work around that possibility.
     */
    protected void createCoords()
    {
        //  Generate positions along the data array
        coords = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            coords[i] = (double) ( i + 1 );
        }
        Mapping mapping = ASTJ.get1DMapping( astref, 1 );
        coords = ASTJ.astTran1( mapping, coords, true );
    }
}
