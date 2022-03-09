package uk.ac.starlink.oldfits;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedFile;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.ArrayBuilder;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.Converter;
import uk.ac.starlink.array.ConvertArrayImpl;
import uk.ac.starlink.array.Function;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.TypeConverter;
import uk.ac.starlink.util.URLUtils;

/**
 * Turns URLs which reference FITS array resources into NDArray objects.
 * <p>
 * URLs are given in the format
 * <blockquote>
 *    <i>fits-url.fit</i>
 * </blockquote>
 * or
 * <blockquote>
 *    <i>fits-url.fit</i><tt>[</tt><i>hdu-num</i><tt>]</tt>
 * </blockquote>
 * where the <tt>[]</tt> represent literal square brackets 
 * or
 * <blockquote>
 *    <i>fits-url.fit</i><tt>#</tt><i>hdu-num</i>
 * </blockquote>
 * where the <tt>#</tt> represents a literal hash sign.
 * The <i>fits-url.fit</i> represents the full absolute or relative URL 
 * of a FITS file, and the <i>hdu-num</i>, if present, is the index
 * of the HDU within it.  If no HDU is given, the first HDU 
 * (<i>hdu-num</i>=0) is understood.
 * <p>
 * When writing a new NDArray, if <i>hdu-num</i>==0 then any existing 
 * FITS file of the same name will be erased.  It is possible to 
 * write to HDUs after the first one by specifying the appropriate
 * <i>hdu-num</i>, but only if this refers to the first non-existent
 * HDU in an existing FITS file.
 * <p>
 * This is a singleton class; use {@link #getInstance} to get an instance.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsArrayBuilder implements ArrayBuilder {

    /** Sole instance of the class. */
    private static FitsArrayBuilder instance = new FitsArrayBuilder();

    /** Maximum size of MappedFile (NIO mapped buffer). */
    private static final int MAPPED_MAX_SIZE = Integer.MAX_VALUE;

    private List<String> extensions = 
        new ArrayList<String>( FitsConstants.defaultFitsExtensions() );
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.oldfits" );

    /**
     * Private sole constructor.
     */
    private FitsArrayBuilder() {}

    /**
     * Returns a FitsArrayBuilder.
     *
     * @return  the sole instance of this class
     */
    public static FitsArrayBuilder getInstance() {
        return instance;
    }
    

    /**
     * Returns an ArrayDataInput corresponding to the given URL.
     * It will be positioned at the right place to read the given URL,
     * which may not be the start of the file if it's not the first HDU.
     * Returns a null if the URL does not refer to a FITS stream.
     */
    ArrayDataInput getReadableStream( URL url, AccessMode mode ) 
            throws IOException {

        /* Parse the URL as a reference to a FITS HDU, or bail out
         * if this is not possible. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return null;
        }
        URL container = furl.getContainer();
        int hdu = furl.getHDU();

        ArrayDataInput stream = null;

        /* If we're on the local filesystem we can make one which supports
         * random access. */
        if ( container.getProtocol().equals( "file" ) ) {
            String modechars = ( mode == AccessMode.READ ) ? "r" : "rw";
            
            /*  Get filename of container. Use round trip to File and back
                to deal with escaped characters in URL? */
            File file = new File( URLUtils.urlToUri( container ) );
            String filename = file.getPath();

            /* Work out the start and size of the relevant HDU. */
            BufferedFile bstrm = new BufferedFile( filename );
            FitsConstants.skipHDUs( bstrm, hdu );
            long start = bstrm.getFilePointer();
            FitsConstants.skipHDUs( bstrm, 1 );
            long leng = bstrm.getFilePointer() - start;
            bstrm.close();

            /* Make a suitable ArrayInputStream; a MappedFile if it's 
             * not too big (more efficient for random access), otherwise
             * a BufferedFile. */
            if ( leng <= MAPPED_MAX_SIZE ) {
                stream = new MappedFile( filename, modechars, start,
                                         (int) leng );
            }
            else {
                stream = new BufferedFile( filename, modechars );
                ((BufferedFile) stream).seek( start );
            }
        }

        /* Fall back to a simple input stream if necessary. */
        if ( stream == null ) {
            if ( mode != AccessMode.READ ) {
                throw new IOException(
                    "Access mode " + mode + " not supported for " + url );
            }
            InputStream istrm = container.openStream();
            stream = new BufferedDataInputStream( istrm );

            /* Advance to the correct point in the stream. */
            FitsConstants.skipHDUs( stream, hdu );
        }
        return stream;
    }

    public NDArray makeNDArray( URL url, AccessMode mode ) throws IOException {

        /* Get an ArrayDataInput from which to construct the FITS NDArray. */
        ArrayDataInput stream = getReadableStream( url, mode );
        if ( stream == null ) {
            return null;
        }

        return makeNDArray( stream, mode, url );
    }

    /**
     * Makes a readable NDArray from a data input stream. 
     * It will have no URL.  Update access may be possible, but only
     * if <tt>stream</tt> implements {@link nom.tam.util.ArrayDataOutput}.
     *
     * @param  stream the input stream supplying the HDU at which the
     *                FITS array data can be found
     * @param  mode   read/write/update access mode
     * @return  the NDArray constructed from <tt>stream</tt>
     * @throws IOException  if some I/O error occurs or the stream does
     *                      not contain FITS array data
     */
    public NDArray makeNDArray( ArrayDataInput stream, AccessMode mode ) 
            throws IOException {
        return makeNDArray( stream, mode, null );
    }

    /**
     * Makes a readable NDArray from an input stream.  A URL may be
     * supplied but serves only to set the URL of the resulting NDArray,
     * no connection is made to it to get data.
     */
    private NDArray makeNDArray( ArrayDataInput stream, AccessMode mode, 
                                 URL url ) throws IOException {

        /* Make the ArrayImpl. */
        ArrayImpl impl;
        try {
            impl = new ReadableFitsArrayImpl( stream, mode );
        }
        catch ( FitsException e ) {
            throw new IOException( e.getMessage() );
        }

        /* See if we need to scale the array using BSCALE/BZERO. */
        Type type = impl.getType();
        boolean scaled = false;
        Header hdr = ((ReadableFitsArrayImpl) impl).getHeader();
        double bscale = 1.0;
        double bzero = 0.0;
        int digits = 0;
        if ( hdr.containsKey( "BSCALE" ) ) {
            bscale = hdr.getDoubleValue( "BSCALE" );
        }
        if ( hdr.containsKey( "BZERO" ) ) {
            bzero = hdr.getDoubleValue( "BZERO" );
        }

        /* Scaling is necessary; wrap the NDArray in a scaling wrapper. */
        if ( bscale != 1.0 || bzero != 0.0 ) {
            Type stype = ( bscale - (float) bscale == 0.0 &&
                           bzero - (float) bzero == 0.0 ) ? Type.FLOAT
                                                          : Type.DOUBLE;
            final double scale = bscale;
            final double invscale = 1.0 / bscale;
            final double zero = bzero;
            Function scaler = new Function() {
                public double forward( double x ) {
                    return zero + scale * x;
                }
                public double inverse( double y ) {
                    return ( y - zero ) * invscale;
                }
            };

            /* Create a new NDArray which consists of the old one wrapped
             * in a scaling function.  Note the new one is given the URL,
             * not the wrapped one.  Although the new one is virtual, this
             * is correct, since turning the URL into an NDArray (via this 
             * factory) will always result in the scaled NDArray, since 
             * this step is undergone each time. */
            NDArray nda = new BridgeNDArray( impl );
            Converter conv =
                new TypeConverter( nda.getType(), nda.getBadHandler(), stype,
                                   stype.defaultBadHandler(), scaler );
            impl = new ConvertArrayImpl( nda, conv );
        }

        /* Return the NDArray object. */
        return new BridgeNDArray( impl, url );
    }

    /**
     * Makes a new HDU at a given URL containing an NDArray with the 
     * shape and type as specified.  If the URL represents the first 
     * HDU in a FITS
     * file, or leaves the HDU index unspecified, any existing FITS
     * file will be overwritten by a new single-HDU file.
     * An HDU index greater than 0 may be specified only if the 
     * URL has the <tt>file:</tt> protocol, and if it is equal to
     * the number of HDUs currently in the FITS file.
     *
     * @param  url    the URL at which the resource backing the NDArray is
     *                to be written
     * @param  shape  the shape of the new NDArray to construct
     * @param  type   the primitive data type of the new NDArray to construct
     * @param  bh     requested bad value handling policy
     * @return   the new NDArray, or <tt>null</tt> if the URL doesn't look
     *           like a FITS file
     * @throws   IOException  if the URL is a FITS URL but the requested
     *                        NDArray cannot be constructed for some reason
     */
    public NDArray makeNewNDArray( URL url, NDShape shape, Type type,
                                   BadHandler bh ) 
            throws IOException {

        /* Parse the URL as a reference to a FITS HDU, or bail out
         * if this is not possible. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return null;
        }
        URL container = furl.getContainer();
        int hdu = furl.getHDU();

        /* Get an ArrayDataOutput from which to construct the FITS NDArray. */
        ArrayDataOutput stream;

        /* If we're on the local filesystem we can make it one which 
         * supports random access. */
        if ( container.getProtocol().equals( "file" ) ) {
            String filename = container.getPath();

            /* First HDU - this will erase any existing data in that file. */
            if ( hdu == 0 ) {
                if ( new File( filename ).delete() ) {
                    logger.info( "Deleted existing file " + filename + 
                                 " prior to rewriting" );
                }
                stream = new BufferedFile( filename, "rw" );
            }

            /* HDU other than first - this can be done, but only if it 
             * follows the final existing HDU. */
            else {
                assert hdu > 0;
                BufferedFile bstrm = new BufferedFile( filename, "r" );
                long pos = 0;
                int ihdu = 0;
                long leng = bstrm.length();
                while ( pos < leng && ihdu < hdu ) {
                    bstrm.seek( pos );
                    Header hdr;
                    try {
                        hdr = new Header( bstrm );
                    }
                    catch ( TruncatedFileException e ) {
                        throw (IOException) 
                              new IOException( "Cannot create new HDU "
                                             + "except at end of FITS file" )
                             .initCause( e );
                    }
                    long dsize = FitsConstants.getDataSize( hdr );
                    pos = bstrm.getFilePointer() + dsize;
                    ihdu++;
                }
                bstrm.close();
                if ( pos == leng && ihdu == hdu ) {
                    stream = new BufferedFile( filename, "rw" );
                    ((BufferedFile) stream).seek( pos );
                }
                else {
                    throw new IOException( 
                        "Cannot create new HDU except at end of FITS file" );
                }
            }
        }

        /* Fall back to a simple output stream if necessary. */
        else {
            if ( hdu > 0 ) {
                throw new IOException(
                    "Can't access HDU after first one in non-seekable stream" );
            }
            URLConnection conn = container.openConnection();
            conn.setDoInput( false );
            conn.setDoOutput( true );

            /* The following may throw a java.net.UnknownServiceException
             * (which is-a IOException) - in fact it almost certiainly will,
             * since I don't know of any URL protocols (including file) 
             * which support output streams. */
            conn.connect();
            OutputStream ostrm = conn.getOutputStream();
            stream = new BufferedDataOutputStream( ostrm );
        }

        /* Make the implementation. */
        boolean primary = hdu == 0;
        Number badval = getBlankValue( type, bh );
        ArrayImpl impl = new WritableFitsArrayImpl( shape, type, badval,
                                                    stream, primary, null );

        /* Return an NDArray based on this. */
        return new BridgeNDArray( impl, url );
    }

    /**
     * Makes a new HDU written into a given stream containing an NDArray 
     * with the type and shape as specified.
     *
     * @param  stream  the stream down which the NDArray is to be written
     * @param  shape  the shape of the new NDArray to construct
     * @param  type   the primitive data type of the new NDArray to construct
     * @param  bh     requested bad value handling policy
     * @param  primary  whether this is the primary HDU (first in file)
     * @param  cards  array of additional FITS header cards to add - may be null
     * @return the new NDArray object
     * @throws IOException  if there is some I/O error
     */
    public NDArray makeNewNDArray( OutputStream stream, NDShape shape,
                                   Type type, BadHandler bh, boolean primary,
                                   HeaderCard[] cards )
            throws IOException {
        if ( ! ( stream instanceof BufferedOutputStream ) ) {
            stream = new BufferedOutputStream( stream );
        }
        ArrayDataOutput strm = new BufferedDataOutputStream( stream );
        Number badval = getBlankValue( type, bh );
        ArrayImpl impl = new WritableFitsArrayImpl( shape, type, badval,
                                                    strm, primary, cards );
        return new BridgeNDArray( impl );
    }


    /**
     * Returns a BLANK value for a given type based on a given BadHandler
     * representing a request for bad value handling policy.
     * An exact match may not be possible, since only integer values are
     * allowed to specify blank values.
     *
     * @param  type  the numerical type
     * @param  bh    the bad handler constituting the preferred handling policy
     * @return   a number representing the BLANK value to use
     */
    private static Number getBlankValue( Type type, BadHandler bh ) {
        if ( bh == null ) {
            return type.defaultBadValue();
        }
        else if ( type.isFloating() ) {
            return type.defaultBadValue();
        }
        else {
            return bh.getBadValue();
        }
    }


}
