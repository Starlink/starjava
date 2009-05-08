/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.imagedata.NDFJ;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;

/**
 * NDFSpecDataImpl - implementation of SpecDataImpl to access NDF
 *                   spectra.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see "The Bridge Design Pattern"
 */
public class NDFSpecDataImpl
    extends AbstractSpecDataImpl
    implements FITSHeaderSource
{
//
// Implementation of abstract methods.
//
    /**
     * Constructor - open a NDF by file name.
     */
    public NDFSpecDataImpl( String fileName )
        throws SplatException
    {
        super( fileName );
        open( fileName );
    }

    /**
     * Constructor. Initialise this spectrum by cloning the content of
     * another spectrum (usual starting point for saving).
     */
    public NDFSpecDataImpl( String fileName, SpecData source )
        throws SplatException
    {
        super( fileName );

        //  For NDFs overwriting self is not allowed. Check for that.
        if ( fileName.equals( source.getFullName() ) ) {
            throw new SplatException( "Cannot overwrite an opened NDF" );
        }

        fullName = fileName;

        //  This works by creating a temporary NDF as backing. This
        //  can be made permanent by the save method. Don't want to
        //  save without user intervention as we could be overwriting
        //  an existing file.
        createTempClone( source );
    }

    /**
     * Return a copy of the spectrum data values.
     */
    public double[] getData()
    {
        return theNDF.get1DDouble( "data", true );
    }

    /**
     * Return a copy of the spectrum data errors.
     */
    public double[] getDataErrors()
    {
        if ( theNDF.has( "variance" ) ) {
            return theNDF.get1DDouble( "error", true );
        } else {
            return null;
        }
    }

    /**
     * Return the NDF shape.
     */
    public int[] getDims()
    {
        return theNDF.getDims();
    }

    /**
     * Return a symbolic name.
     */
    public String getShortName()
    {
        String name = theNDF.getCharComp( "Title" );
        if ( name == null || name.equals("") ) {
            return fullName;
        }
        return name;
    }

    /**
     * Return the full name of the NDF.
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return reference to NDF AST frameset.
     */
    public FrameSet getAst()
    {

        // The NDFJ object will annul this identifier when finalized, so we do
        // not need to keep a copy. Fix up the coordinate units for problems.
        FrameSet frameSet = theNDF.getAst();
        frameSet.setUnit
            ( 1, UnitUtilities.fixUpUnits( frameSet.getUnit( 1 ) ) );
        return frameSet;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "NDF";
    }

    /**
     * Save the spectrum to disk-file. Assumes that the spectrum hasn't
     * already been saved and is resident in a temporary NDF.
     * TODO: deal with situation when above isn't true.
     */
    public void save()
        throws SplatException
    {
        //  Create a copy of the current NDF (which should be a
        //  temporary one, as only cloned NDFs can really be saved).
        NDFJ newNDF = theNDF.getCopy( fullName );
        if ( newNDF != null ) {
            newNDF.saveAst( getAst() );
            theNDF = newNDF;

            // Close and re-open to flush to disk.
            theNDF.close();
            open( fullName );
        }
        else {
            throw new SplatException( "Failed to save spectrum to NDF " +
                                      fullName + " (already open?)" );
        }
    }

    /**
     * Return a keyed value from the NDF character components or FITS headers.
     * Returns "" if not found.
     */
    public String getProperty( String key )
    {
        String result = super.getProperty( key );
        if ( ! result.equals( "" ) ) {
            return result;
        }

        //  Check for the known keys in NDF.
        if ( ( key.equalsIgnoreCase( "label" ) ||
               key.equalsIgnoreCase( "units" ) ||
               key.equalsIgnoreCase( "title" ) ) && theNDF.has( key ) ) {
            return theNDF.getCharComp( key );
        }
        else {

            // Look for key in FITS headers.
            Header headers = getFitsHeaders();
            if ( headers != null ) {
                String scard = headers.findKey( key );
                if ( scard != null ) {
                    HeaderCard card = new HeaderCard( scard );
                    if ( card != null ) {
                        return card.getValue();
                    }
                }
            }
        }
        return "";
    }

//
// FITSHeaderSource implementation.
//
    /**
     * Return any FITS headers in the NDF as proper FITS header.
     */
    public Header getFitsHeaders()
    {
        if ( header == null ) {
            header = new Header();
            int size = theNDF.countFitsHeaders();
            if ( size > 0 ) {
                Cursor iter = header.iterator();
                HeaderCard card;
                for ( int i = 0; i < size; i++ ) {
                    card = new HeaderCard( theNDF.getFitsHeader( i ) );
                    if ( card.isKeyValuePair() ) {
                        iter.add( card.getKey(), card );
                    }
                    else {
                        iter.add( card );
                    }
                }
            }
        }
        return header;
    }

//
//  Implementation specific methods and variables.
//
    /**
     * Reference to NDFJ object for accessing the NDF.
     */
    protected NDFJ theNDF = new NDFJ();

    /**
     * Original specification of NDF.
     */
    protected String fullName;

    /**
     * Reference to the FITS headers.
     */
    protected Header header = null;

    /**
     * Finalise object. Free any resources associated with member
     * variables (not much to do here, NDFJ class frees AST frameset
     * and annuls NDF).
     */
    protected void finalize() throws Throwable
    {
        theNDF = null;
        fullName = null;
        super.finalize();
    }

    /**
     * Open an NDF.
     *
     * @param fileName file name of the NDF.
     */
    protected void open( String fileName )
        throws SplatException
    {
        if ( theNDF.open( fileName ) ) {
            fullName = fileName;
            if ( theNDF.has( "units" ) ) {
                setDataUnits( theNDF.getCharComp( "units" ) );
            }
            if ( theNDF.has( "label" ) ) {
                setDataLabel( theNDF.getCharComp( "label" ) );
            }
        }
        else {
            throw new SplatException( "Failed to open NDF: " + fileName );
        }
    }

    /**
     * Return a copy of this NDF.
     */
    public NDFJ getTempCopy()
    {
        return theNDF.getTempCopy();
    }

    /**
     * Create an temporary NDF that is a clone of an existing
     * spectrum. If this is an NDF then it is used as a template,
     * otherwise a new NDF is created.
     */
    protected void createTempClone( SpecData source )
        throws SplatException
    {
        //  For multi-dimensional sources we just create a new NDF, this
        //  avoids problems with matching the WCS and data components.
        if ( source.getSpecDataImpl().getDims().length == 1 ) {

            //  Create the NDF. Use a copy if source is an NDF.
            if ( source.getDataFormat().equals( "NDF" ) ) {
                theNDF = ((NDFSpecDataImpl)
                          source.getSpecDataImpl()).getTempCopy();
            }
            else {
                //  Look for a backing source that may be an NDF sometime
                //  back (only really works for EditableSpecData).
                SpecDataImpl parent = null;
                
                // Search all parents of parents etc., until we get an
                // NDF. Protect against circular loops by limiting look-back.
                parent = source.getSpecDataImpl().getParentImpl();
                if ( parent != null && !(parent instanceof NDFSpecDataImpl) ) {
                    for ( int i = 0; i < 1000; i++ ) {
                        parent = parent.getParentImpl();
                        if ( parent == null ) break;
                        if ( parent instanceof NDFSpecDataImpl ) break;
                    }
                }
                if ( parent != null && !(parent instanceof NDFSpecDataImpl) ) {
                    theNDF = ((NDFSpecDataImpl)parent).getTempCopy();
                }
                else {
                    theNDF = NDFJ.get1DTempDouble( source.size() );
                }
            }
        }
        else {
            //  Simple 1D NDF.
            theNDF = NDFJ.get1DTempDouble( source.size() );
        }

        if ( theNDF == null ) {
            throw new SplatException( "Failed to create a temporary copy " +
                                      "of " + source.getShortName() );
        }

        //  If source offer FITS headers, then we need to copy these.
        if ( source.getSpecDataImpl().isFITSHeaderSource() ) {
            Header headers =
                ((FITSHeaderSource)source.getSpecDataImpl()).getFitsHeaders();
            if ( headers != null ) {
                Cursor iter = headers.iterator();
                String[] cards = new String[headers.getNumberOfCards()];
                int i = 0;
                while ( iter.hasNext() ) {
                    cards[i++] = ((HeaderCard) iter.next()).toString();
                }
                if ( i > 0 ) {
                    theNDF.createFitsExtension( cards );
                }
            }
        }

        //  Update all data components etc.
        theNDF.set1DDouble( "data", source.getYData() );
        if ( source.getYDataErrors() != null ) {
            theNDF.set1DDouble( "error", source.getYDataErrors() );
        }
        else {
            //  TODO: clear any existing variance component.
        }

        //  Only set short name if changed from full name.
        if ( ! source.getShortName().equals( source.getFullName() ) ) {
            theNDF.setCharComp( "Title", source.getShortName() );
        }

        //  Set the AST component, note we get this from the implementation
        //  not the SpecData. Not written to NDF until saved.  If the
        //  dimensionality is not 1 then we pick out the first axis of the
        //  SpecData WCS.
        FrameSet frameSet = source.getAst().getRef();
        FrameSet backingFrameSet = source.getFrameSet();
        if ( backingFrameSet.getNaxes() != 1 ) {
            backingFrameSet = ASTJ.get1DFrameSet( frameSet, 1 );
        }
        theNDF.setAst( backingFrameSet );

        //  Set the Units and Label if we can.
        String label = frameSet.getC( "Label(2)" );
        String unit = frameSet.getC( "Unit(2)" );
        if ( label != null && ( ! label.equals( "" ) ) ) {
            setDataLabel( label );
            theNDF.setCharComp( "label", dataLabel );
        }
        if ( unit != null && ( ! unit.equals( "" ) ) ) {
            setDataUnits( unit );
            theNDF.setCharComp( "units", dataUnits );
        }
    }
}
