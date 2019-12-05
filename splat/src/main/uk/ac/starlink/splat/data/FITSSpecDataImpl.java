/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *       Original version.
 *     08-MAR-2002 (Peter W. Draper):
 *       Changed to nom.tam.fits version 0.93
 */
package uk.ac.starlink.splat.data;

import java.io.FileOutputStream;
import java.io.PrintStream;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.ast.ASTFITSChan;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.splat.util.Utilities;

/**
 *  FITSSpecDataImpl - implementation of SpecDataImpl to access FITS
 *                     spectra.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see "The Bridge Design Pattern"
 */
public class FITSSpecDataImpl
    extends AbstractSpecDataImpl 
    implements FITSHeaderSource
{
//
//  Implementation of SpecDataImpl methods.
//
    /**
     * Constructor - open a FITS file by file name.
     */
    public FITSSpecDataImpl( String fileName )
        throws SplatException
    {
        this(fileName, 0);
    }
	
	/**
     * Constructor - open a FITS file by file name.
     */
    public FITSSpecDataImpl( String fileName, int hdunum )
        throws SplatException
    {
        super( fileName );
        this.hdunum = hdunum;
        openForRead( fileName );
    }

    /**
     * Constructor, creating an object by cloning another.
     */
    public FITSSpecDataImpl( String fileName, SpecData source )
        throws SplatException
    {
        this(fileName, source, 0);
    }
    
    /**
     * Constructor, creating an object by cloning another.
     */
    public FITSSpecDataImpl( String fileName, SpecData source, int hdunum )
        throws SplatException
    {
        super( fileName );
        this.hdunum = hdunum;
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
        }
        else {
            try {
                return getDataCopy();
            }
            catch ( FitsException e ) {
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
        }
        else {
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
        }
        else {
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
        if ( cloned || astref != null ) {
            return astref;
        }
        else {
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
    public void save()
        throws SplatException
    {
        saveToFile();
    }

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found.
     */
    public String getProperty( String key )
    {
        String result = super.getProperty( key );
        if ( ! "".equals( result ) ) {
            return result;
        }

        if ( getFitsHeaders() != null ) {
            // All FITS keywords are uppercase.
            String scard = getFitsHeaders().findKey( key.toUpperCase() );
            if ( scard != null ) {
                HeaderCard card = HeaderCard.create( scard );
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
            if ( header == null ) {
                if ( hdurefs != null ) {
                    if ( hdurefs[hdunum] != null ) {
                        header = hdurefs[hdunum].getHeader();
                    }
                }
            }
            if ( header == null ) {
                header = new Header();
            }
            return header;
        }
        return clonedHeader;
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
     * Reference to cloned data values.
     */
    protected double[] data = null;

    /**
     * Reference to cloned data errors.
     */
    protected double[] errors = null;

    /**
     * Reference to the header cards.
     */
    protected Header header = null;

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
    protected final static int MAX_HEADER_VALUE = 68;

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize() throws Throwable
    {
        astref = null;
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
        throws SplatException
    {
        //  Parse the name to extract the HDU reference.
        PathParser namer = new PathParser( fileName );
        if (hdunum <= 0) // if hdunum is >0, it means it was required explicitly (assuming user knows what he's doing) 
        	hdunum = namer.fitshdunum();
        String name = null;
        if ( namer.type().equals( ".sdf" ) && ! namer.path().equals( "" ) ) {
            // Probably not a ".fits" or ".fit" extension. Shouldn't
            // be an NDF.
            name = namer.ndfname();
        }
        else {
            name = namer.fullname();
        }

        try {
            fitsref = new Fits( name );
        }
        catch ( Exception e ) {
            fitsref = null;
            throw new SplatException( e );
        }

        try {
            hdurefs = fitsref.read();
        }
        catch ( Exception e ) {
            hdurefs = null;
            throw new SplatException( e );
        }

        //  Get short name.
        String shortName = hdurefs[0].getObject();
        if ( shortName != null ) {
            this.shortName = shortName;
        }
        else {
            shortName = fileName;
        }
        fullName = fileName;

        //  And make guesses at the units/label.
        if ( getFitsHeaders() != null ) {
            String scard = getFitsHeaders().findKey( "BUNIT" );
            if ( scard == null ) {
                scard = getFitsHeaders().findKey( "BUNITS" );
            }
            if ( scard == null ) {
                scard = getFitsHeaders().findKey( "UNITS" );
            }
            if ( scard != null ) {
                HeaderCard card = HeaderCard.create( scard );
                if ( card != null ) {
                    setDataUnits( card.getValue() );
                }
            }

            scard = getFitsHeaders().findKey( "LABEL" );
            if ( scard == null ) {
                scard = getFitsHeaders().findKey( "OBJECT" );
            }
            if ( scard != null ) {
                HeaderCard card = HeaderCard.create( scard );
                if ( card != null ) {
                    setDataLabel( card.getValue() );
                }
            }
        }
    }

    /**
     * @return A new FITS HDU using the current configuration to
     * populate it. Will only succeed for clone spectra.
     */
    
    public BasicHDU makeHDU() throws FitsException {
    	return makeHDU(true);
    }
    
    public BasicHDU makeHDU(boolean useStandardIterator) throws FitsException {
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
        
        String objectKey = "OBJECT";
        String objectComment = "Symbolic name";
        
        if (useStandardIterator) {
        	hiter.add( objectKey, new HeaderCard( objectKey, validName,
            		objectComment ) );
        }
        else {
        	Cursor iter = header.iterator();
            iter.setKey( "CUNIT1A" );
            if ( iter.hasNext() ) {
                iter.next();
            }
            iter.add( objectKey, new HeaderCard( objectKey, validName,
            		objectComment ) );
        }
        
        //  Save the AST description of the coordinates.
        saveAst( header );
        
        return hdu;
    }
    
    /**
     * Create a new FITS file using the current configuration to
     * populate it. Will only succeed for clone spectra.
     */
    protected void saveToFile()
        throws SplatException
    {
        //  Parse the name to extract the HDU reference and container
        //  file name. Extension number is ignored for now.
        PathParser namer = new PathParser( fullName );
        hdunum = namer.fitshdunum();
        String container = null;
        if ( namer.type().equals( ".sdf" ) && ! namer.path().equals( "" ) ) {
            container = namer.ndfname();
        }
        else {
            container = namer.fullname();
        }
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
            fitsref = null;
            throw new SplatException( e );
        }

        //  No longer a memory clone, backing file is created.
        cloned = false;
        data = null;
        errors = null;
        clonedHeader = null;

        //  So open properly.
        openForRead( fullName );
    }

    /**
     * Write an AST frameset that describes the coordinates of the
     * spectrum into the file.
     */
    protected void saveAst( Header header )
    {
        if ( astref != null ) {

            //  Make sure frameset is 1D to match the output data.
            int sigaxis = 1;
            int dims[] = getDims();
            if ( dims.length > 1 ) {
                for ( int i = 0; i < dims.length; i++ ) {
                    if ( dims[i] > 1 ) {
                        sigaxis = i + 1;
                        break;
                    }
                }
            }
            astref = ASTJ.get1DFrameSet( astref, sigaxis );

            //  Create a AST FITS channel to read the headers.
            ASTFITSChan chan = new ASTFITSChan( "FITS-WCS" );

            //  Set the NAXIS and NAXIS1 cards so that AST knows
            //  how far to check the transformation for linearity.
            chan.add( "NAXIS   =  1" );
            chan.add( "NAXIS1  = " + data.length );

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
                    if ( buffer != null && ! "".equals( buffer ) ) {
                        iter.add( HeaderCard.create( buffer ) );
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
                    if ( buffer != null && ! "".equals( buffer ) ) {
                        card = HeaderCard.create( buffer );
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
    protected double[] getDataCopy()
        throws FitsException
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
            long blank = 0;
            boolean haveblank = true;
            try {
                blank = hdurefs[hdunum].getBlankValue();
            }
            catch ( FitsException e ) {
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
                   }
                   else {
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
                       }
                       else {
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
                       }
                       else {
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
                           }
                           else {
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
                           }
                           else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   }
                   else {
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
                           }
                           else {
                               spectrum[i] = arr[i] * bscale + bzero;
                           }
                       }
                   }
                   else {
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

        //  Check for known non-standard formats (far too many of those it
        //  seems).
        astref = checkForNonStandardFormat( header );

        //  If not non-standard then proceed with simple AST method.
        if ( astref == null ) {
            //  Create a AST FITS channel to read the headers.
            ASTFITSChan chan = new ASTFITSChan();

            //  Loop over all cards pushing these into the channel.
            Cursor iter = header.iterator();
            while ( iter.hasNext() ) {
                chan.add( ((HeaderCard) iter.next()).toString() );
            }
            chan.rewind();

            //  Now get the ASTFrameSet.
            try {
                astref = chan.read();
            }
            catch (AstException e) {
                e.printStackTrace();
                astref = null;
            }
            
            if ( astref == null ) {
            	
                //  Read failed for some reason, most likely no coordinates
                //  (other than the pixel one) are defined. Just create a
                //  dummy frameset.
                astref = dummyAstSet();
            }
           
          
        }

        //  Try to repair any dodgy units strings.
        String unit = astref.getUnit( 1 );
        if ( unit != null && ! unit.equals( "" ) ) {
            astref.setUnit( 1, UnitUtilities.fixUpUnits( unit ) );
        }
        return astref;
    }

    /**
     *  Check for known non-standard formats (far too many of those it
     *  seems). For now we recognise SDSS, which uses a log(wavelength)
     *  scale.
     */
    protected FrameSet checkForNonStandardFormat( Header header )
    {
    	FrameSet frameset = null;
    	
    //	if (isSDSSFITSHeader() ) {
    		Header header0 = hdurefs[0].getHeader();
    	//	HeaderCard ycard= header0.findCard("BUNIT");
    	//	String yunits ==ycard.getValue();
    		//  SDSS format, in fact these may just need CTYPE1=WAVE-LOG
    		//  but that didn't seem to work (presumably conflict with
    		//  another header).
    		HeaderCard cpix = header.findCard("CRPIX1");
    		HeaderCard c0 = header.findCard( "COEFF0" );
    		HeaderCard c1 = header.findCard( "COEFF1" );
    		String cp1 = cpix==null?"0":cpix.getValue();
    		
    		if ( c0 != null && c1 != null ) {
    			String c0s = c0.getValue();
    			String c1s = c1.getValue();
    			

    			//  Formulae are w = 10**(c0+c1*i)
    			//               i = (log(w)-c0)/c1
    			String fwd[] = {
    					"w = 10**(" + c0s + " + ( (i -"+cp1+") * " + c1s + " ) )" };
    			String inv[] = {
    					"i = ( log10( w ) - " + c0s + ")/" + c1s + "+" + cp1 };

  			
    			MathMap logMap = new MathMap( 1, 1 , fwd, inv );
    			
    			frameset = new FrameSet(new Frame(1));
    			frameset.addFrame( 1, logMap, new SpecFrame() );
    			
    			return frameset;
    		}

    //	}
    	return null;
    }

    

	

	/** 
     * Check if it's a SDSS FITS format
     */
    public boolean isSDSSFITSHeader() {
    	
        return isSDSSFITSHeader(hdurefs[0].getHeader());
    }
    
    public static boolean isSDSSFITSHeader(Header header) {
    	HeaderCard testCard = header.findCard( "TELESCOP" );
        if ( testCard != null ) {
            if ( testCard.getValue().startsWith( "SDSS" )) 
           
            	return true;
        }
        return false;
    }
    

    /**
     * Create a dummy AST frameset for the current HDU. Used when FITS headers
     * fail to describe any additional coordinates and reproduce just an
     * N-dimensional grid based coordinate system, where N is the
     * dimensionality of the underlying data.
     *
     *  @return reference to dummy AST frameset.
     */
    protected FrameSet dummyAstSet()
    {
        Frame frame = new Frame( getDataDims().length );
        FrameSet frameset = new FrameSet( frame );
        return frameset;
    }

    /**
     * Return an array of with length the number of dimensions of
     * the original data and with each element set to the size of that
     * dimension. Note in SPLAT multidimensional data is assumed to be in
     * column major order, so we need to reverse the order of these
     * dimensions.
     */
    protected int[] getDataDims()
    {
        if ( hdunum < hdurefs.length ) {

            //  Query the current HDU.
            try {
                int[] dims = hdurefs[hdunum].getAxes();
                if ( dims != null ) {
                    int ndims = dims.length;
                    if ( ndims > 1 ) {
                        int[] reorder = new int[ndims];
                        for ( int i = 0, j = ndims - 1; i < ndims; i++, j-- ) {
                            reorder[i] = dims[j];
                        }
                        dims = reorder;
                    }
                    return dims;
                }
            }
            catch (FitsException e) {
                //  Just ignore and return dummy dimension.
            }
        }

        //  Also fall-through when dims == null.
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
        errors = source.getYDataErrors();
        astref = source.getFrameSet();

        //  If the source spectrum provides access to a set of FITS
        //  headers then we should preserve them.
        if ( source.getSpecDataImpl().isFITSHeaderSource() ) {
            clonedHeader =
                ((FITSHeaderSource) source.getSpecDataImpl()).getFitsHeaders();
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
