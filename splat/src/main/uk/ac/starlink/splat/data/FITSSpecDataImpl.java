package uk.ac.starlink.splat.data;

import java.io.FileOutputStream;
import java.io.PrintStream;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTFITSChan;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;

/**
 *  FITSSpecDataImpl - implementation of SpecDataImpl to access FITS
 *                     spectra.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 01-SEP-2000
 * @see "The Bridge Design Pattern"
 *
 * @history 08-MAR-2002 Changed to nom.tam.fits version 0.93
 */
public class FITSSpecDataImpl extends SpecDataImpl
    implements FITSHeaderSource
{
//
//  Implementation of SpecDataImpl methods.
//
    /**
     * Constructor - open a FITS file by file name.
     */
    public FITSSpecDataImpl( String fileName )
    {
        super( fileName );
        hdunum = 0;
        openForRead( fileName );
    }

    /**
     * Constructor, creating an object by cloning another.
     */
    public FITSSpecDataImpl( String fileName, SpecData source )
    {
        super( fileName );
        hdunum = 0;
        fullName = fileName;
        makeMemoryClone( source );
    }

    /**
     * Return a copy of the spectrum data values.
     */
    public double[] getData()
    {
        if ( cloned ) {
            return data;
        } else {
            try {
                return getDataCopy();
            } catch ( FitsException e ) {
                return null;
            }
        }
    }

    /**
     * Return a copy of the spectrum data errors. TODO: need some
     * mechanism for getting errors from another extension or FITS file.
     */
    public double[] getDataErrors()
    {
        if ( cloned ) {
            return errors;
        } else {
            return null;
        }
    }

    /**
     * Return the data array dimensionality.
     */
    public int[] getDims()
    {
        if ( cloned ) {
            int dummy[] = new int[1];
            dummy[0] = data.length;
            return dummy;
        } else {
            return getDataDims();
        }
    }

    /**
     * Return a symbolic name. This is value of the "OBJECT" card, if
     * present.
     */
    public String getShortName()
    {
        //  Note in order to be save this should not exceed
        //  MAX_HEADER_VALUE characters.
        return shortName;
    }

    /**
     * Return the file name.
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return reference to AST frameset describing coordinates of
     * FITS headers.
     */
    public FrameSet getAst()
    {
        if ( cloned ) {
            return astref;
        } else {
            return createAstSet();
        }
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "FITS";
    }

    /**
     * Save spectrum to the associated disk-file.
     */
    public void save() throws SplatException
    {
        saveToFile();
    }

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found.
     */
    public String getProperty( String key )
    {
        if ( getFitsHeaders() != null ) {
            String scard = getFitsHeaders().findKey( key );
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
//  Implementation of FITSHeaderSource.
//
    public Header getFitsHeaders()
    {
        if ( clonedHeader == null ) {
            if ( hdurefs != null ) {
                if ( hdurefs[hdunum] != null ) {
                    return hdurefs[hdunum].getHeader();
                }
            }
            return null;
        }
        else {
            return clonedHeader;
        }
    }

//
//  Implementation specific methods and variables.
//

    /**
     * Reference to Fits object.
     */
    protected Fits fitsref = null;

    /**
     * HDU references for the current Fits object.
     */
    protected BasicHDU[] hdurefs;

    /**
     * Current HDU number.
     */
    protected int hdunum = 0;

    /**
     * The state of the object. Cloned is special and indicates that
     * all values are memory resident, rather than backed up by the
     * file.
     */
    protected boolean cloned = false;

    /**
     * Reference to cloned coordinates.
     */
    protected double[] coords = null;

    /**
     * Reference to cloned data values.
     */
    protected double[] data = null;

    /**
     * Reference to cloned data errors.
     */
    protected double[] errors = null;

    /**
     * Reference to cloned header cards.
     */
    protected Header clonedHeader = null;

    /**
     * Default symbolic name.
     */
    protected String shortName = "Spectrum";

    /**
     * The FITS file name.
     */
    protected String fullName;

    /**
     * Reference to the AST frameset (also used when cloned).
     */
    protected FrameSet astref = null;

    /**
     * Maxiumum number of characters in a header string.
     */
    protected static int MAX_HEADER_VALUE = 68;

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize() throws Throwable
    {
        if ( astref != null ) {
            astref.annul();
        }
        fitsref = null;
        hdurefs = null;
        shortName = null;
        fullName = null;
        super.finalize();
    }

    /**
     * Open a named FITS file for reading.
     *
     * @param fileName The name of the FITS file, including any HDU
     *                 reference (format file.fits[0]).
     */
    protected void openForRead( String fileName )
    {
        //  Parse the name to extract the HDU reference.
        InputNameParser namer = new InputNameParser( fileName );
        hdunum = namer.fitshdunum();
        boolean ok = true;
        try {
            fitsref = new Fits( namer.fullname() );
        }
        catch (Exception e ) {
            e.printStackTrace();
            fitsref = null;
            ok = false;
        }
        if ( ok ) {
            try {
                hdurefs = fitsref.read();
            }
            catch (Exception e ) {
                e.printStackTrace();
                hdurefs = null;
                ok = false;
            }
            if ( ok ) {

                //  Get short name.
                String shortName = hdurefs[0].getObject();
                if ( shortName != null ) {
                    this.shortName = shortName;
                } else {
                    shortName = fileName;
                }
                fullName = fileName;
            }
        }
    }

    /**
     * Create a new FITS file using the current configuration to
     * populate it. Will only succeed for clone spectra.
     */
    protected void saveToFile()
    {
        //  Parse the name to extract the HDU reference and container
        //  file name. Extension number is ignored for now.
        InputNameParser namer = new InputNameParser( fullName );
        hdunum = namer.fitshdunum();
        String container = namer.fullname();

        boolean ok = true;
        try {
            // Create a null FITS object (TODO: deal with prior existence?).
            fitsref = new Fits();

            //  Create the HDU that we want to add our headers and
            //  data to it. Note we avoid overwriting the headers
            //  created for the data array and deal with COMMENT and
            //  HISTORY cards.
            BasicHDU hdu = Fits.makeHDU( data );
            Header header = hdu.getHeader();
            Cursor hiter = getStandardIterator( header );

            if ( clonedHeader != null ) {
                HeaderCard card;
                String key;

                Cursor citer = getStandardIterator( clonedHeader );
                while ( citer.hasNext() ) {
                    card = (HeaderCard) citer.next();
                    key = card.getKey();
                    if ( key.equals( "COMMENT" ) || key.equals( "HISTORY" ) ) {
                        hiter.add( card );
                    }
                    else if ( ! header.containsKey( key ) ) {
                        hiter.add( key, card );
                    }
                }
            }

            //  Set the object keyword to the shortname.
            String validName = shortName;
            if ( shortName.length() > MAX_HEADER_VALUE ) {
                validName = shortName.substring( shortName.length() - 
                                                 MAX_HEADER_VALUE );
            }
            hiter.add( "OBJECT", new HeaderCard( "OBJECT", validName,
                                                 "Symbolic name" ) );

            //  Save the AST description of the coordinates.
            saveAst( header );

            //  Write the HDU (data and header) to the file.
            FileOutputStream fo = new FileOutputStream( container );
            BufferedDataOutputStream os = new BufferedDataOutputStream( fo );
            fitsref.addHDU( hdu );
            fitsref.write( os );
        }
        catch (Exception e ) {
            e.printStackTrace();
            fitsref = null;
            ok = false;
        }

        //  No longer a memory clone, backing file is created.
        cloned = false;
        data = null;
        coords = null;
        errors = null;
        clonedHeader = null;
    }

    /**
     * Write an AST frameset that describes the coordinates of the
     * spectrum into the file.
     */
    protected void saveAst( Header header )
    {
        if ( astref != null ) {

            //  Make sure frameset is 1D/.
            astref = ASTJ.get1DFrameSet( astref );

            //  Create a AST FITS channel to read the headers.
            ASTFITSChan chan = new ASTFITSChan( "FITS-WCS" );

            //  Write the AST object into it.
            boolean isNative = false;
            if ( ! chan.write( astref ) ) {
                chan = new ASTFITSChan( "Native" );
                if ( ! chan.write( astref ) ) {
                    return;  // Failed completely.
                }
                isNative = true;
            }
            chan.rewind();

            //  Get an iterator so we can write whole cards to the
            //  headers.
            Cursor iter = getStandardIterator( header );

            //  Loop over all cards reading and adding to the header.
            boolean ok = true;
            String buffer;
            if ( isNative ) {

                //  Don't replace existing cards when native.
                while ( ok ) {
                    buffer = chan.nextCard();
                    if ( ! "".equals( buffer ) ) {
                        iter.add( new HeaderCard( buffer ) );
                    }
                    else {
                        ok = false;
                    }
                }
            }
            else {

                //  Not AST native, must be a standard encoding of
                //  which there can just be one.
                String key;
                HeaderCard card;
                while ( ok ) {
                    buffer = chan.nextCard();
                    if ( ! "".equals( buffer ) ) {
                        card = new HeaderCard( buffer );
                        key = card.getKey();

                        // Removes existing card and position either
                        // there or at end if not found.
                        iter.setKey( key );
                        if ( iter.hasNext() ) {
                            iter.next();
                            iter.remove();
                        }
                        iter.add( key, card );
                    } else {
                        ok = false;
                    }
                }
            }
        }
    }

    /**
     * Get a copy of the FITS spectrum data in double precision.
     */
    protected double[] getDataCopy() throws FitsException
    {
        double[] spectrum = null;
        if ( hdunum < hdurefs.length ) {

            //  Get the data, converting to a 1D array if needed.
            ImageData fitsdata = (ImageData) hdurefs[hdunum].getData();
            Object rawdata = fitsdata.getData();
            Object rawspec = ArrayFuncs.flatten( rawdata );

            //  Get the data format.
            int bitpix = 0;
            bitpix = hdurefs[hdunum].getBitPix();

            //  Get the BSCALE and BZERO values.
            double bscale = hdurefs[hdunum].getBScale();
            double bzero = hdurefs[hdunum].getBZero();

            //  Get the BLANK value, note if none present.
            int blank = 0;
            boolean haveblank = true;
            try {
                blank = hdurefs[hdunum].getBlankValue();
            } catch ( FitsException e ) {
                haveblank = false;
            }

            //  Convert the data to double, applying BSCALE and BZERO
            //  and converting BLANK and NaN values into BAD.
            switch ( bitpix ) {
               case BasicHDU.BITPIX_BYTE: {
                   byte[] arr = (byte[]) rawspec;
                   spectrum = new double[arr.length];
                   if ( haveblank ) {
                       for ( int i = 0; i < arr.length; i++ ) {
                           if ( arr[i] == blank ) {
                               spectrum[i] = SpecData.BAD;
                           } else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   } else {
                       for ( int i = 0; i < arr.length; i++ ) {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               case BasicHDU.BITPIX_DOUBLE: {
                   double[] arr = (double[]) rawspec;
                   spectrum = new double[arr.length];
                   for ( int i = 0; i < arr.length; i++ ) {
                       if ( Double.isNaN( arr[i] ) ) {
                           spectrum[i] = SpecData.BAD;
                       } else {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               case BasicHDU.BITPIX_FLOAT: {
                   float[] arr = (float[]) rawspec;
                   spectrum = new double[arr.length];
                   for ( int i = 0; i < arr.length; i++ ) {
                       if ( Float.isNaN( arr[i] ) ) {
                           spectrum[i] = SpecData.BAD;
                       } else {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               case BasicHDU.BITPIX_INT: {
                   int[] arr = (int[]) rawspec;
                   spectrum = new double[arr.length];
                   if ( haveblank ) {
                       for ( int i = 0; i < arr.length; i++ ) {
                           if ( arr[i] == blank ) {
                               spectrum[i] = SpecData.BAD;
                           } else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   } else {
                       for ( int i = 0; i < arr.length; i++ ) {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               case BasicHDU.BITPIX_LONG: {
                   long[] arr = (long[]) rawspec;
                   spectrum = new double[arr.length];
                   if ( haveblank ) {
                       for ( int i = 0; i < arr.length; i++ ) {
                           if ( arr[i] == blank ) {
                               spectrum[i] = SpecData.BAD;
                           } else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   } else {
                       for ( int i = 0; i < arr.length; i++ ) {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               case BasicHDU.BITPIX_SHORT: {
                   short[] arr = (short[]) rawspec;
                   spectrum = new double[arr.length];
                   if ( haveblank ) {
                       for ( int i = 0; i < arr.length; i++ ) {
                           if ( arr[i] == blank ) {
                               spectrum[i] = SpecData.BAD;
                           } else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   } else {
                       for ( int i = 0; i < arr.length; i++ ) {
                           spectrum[i] = arr[i] * bscale + bzero;
                       }
                   }
               }
               break;
               default: {
               }
            }
        }
        return spectrum;
    }

    /**
     * Create an AST frameset from the FITS headers. If fails then a
     * simple null mapping is used instead.
     */
    protected FrameSet createAstSet()
    {
        //  Access the FITS header block (TODO: merge with primary?).
        Header header = hdurefs[hdunum].getHeader();

        //  Create a AST FITS channel to read the headers.
        ASTFITSChan chan = new ASTFITSChan();

        //  Loop over all cards pushing these into the channel.
        Cursor iter = header.iterator();
        while ( iter.hasNext() ) {
            chan.add( ((HeaderCard) iter.next()).toString() );
        }
        chan.rewind();

        //  Now get the ASTFrameSet. Remembering to free any previous
        //  copies (from other HDUs).
        if ( astref != null ) {
            astref.annul();
        }
        astref = chan.read();
        if ( astref == null ) {

            //  Read failed for some reason. Just create a dummy
            //  frameset.
            astref = dummyAstSet();
        }
        return astref;
    }

    /**
     * Create a dummy AST frameset for the current HDU. Used when
     * FITS headers fail to describe any additional coordinates and
     * reproduce just a 1 dimensional grid based coordinate system.
     *
     *  @return reference to dummy AST frameset.
     */
    protected FrameSet dummyAstSet()
    {
        Frame frame = new Frame( 1 );
        FrameSet frameset = new FrameSet( frame );
        frame.annul();
        return frameset;
    }

    /**
     * Return an array of with length the number of dimensions of
     * the original data and with each element set to the size of that
     * dimension.
     */
    protected int[] getDataDims()
    {
        if ( hdunum < hdurefs.length ) {

            //  Query the current HDU.
            try {
                return hdurefs[hdunum].getAxes();
            } catch (FitsException e) {
                //  Just ignore and return dummy dimension.
            }
        }
        int[] dummy = new int[1];
        dummy[0] = 0;
        return dummy;
    }

    /**
     * Set up this object as a clone of another spectrum. The
     * resultant state is a memory-only resident one, until the save
     * method is called, when the actual file is created and the
     * contents are written.
     */
    protected void makeMemoryClone( SpecData source )
    {
        cloned = true;
        shortName = source.getShortName();
        data = source.getYData();
        coords = source.getXData();
        errors = source.getYDataErrors();
        astref = source.getAst().getRef();

        //  If the source spectrum provides access to a set of FITS
        //  headers then we should preserve them.
        if ( source.getSpecDataImpl() instanceof FITSHeaderSource ) {
            clonedHeader = ((FITSHeaderSource) source.getSpecDataImpl()).getFitsHeaders();
        }
    }

    /**
     * Get a Cursor iterator that is positioned after the standard
     * FITS keywords. If none are found then the iterator is
     * positioned at the end.
     *
     * @param header Header that you want an iterator for.
     *
     * @return a Cursor iterator positioned at the end of the standard
     *         headers.
     */
    public static Cursor getStandardIterator( Header header )
    {
        Cursor iter = header.iterator();
        
        //  Standard headers complete (for images) after the last
        //  NAXIS<n> card.
        int naxes = header.getIntValue( "NAXIS" );
        iter.setKey( "NAXIS" + naxes );
        if ( iter.hasNext() ) {
            iter.next();
        }
        return iter;
    }

    /** 
     * Class utility: Print the header to a given stream.
     * 
     * @param ps the stream to which the card images are dumped.
     */
    public static void dumpHeader( Header header, PrintStream ps ) 
    {
        Cursor iter = header.iterator();
        ps.println( "Header dump begins" );
        ps.println( "==================" );
        while ( iter.hasNext() ) {
            ps.println( (HeaderCard)iter.next() );
        }
        ps.println( "==================" );
    }
}
