package uk.ac.starlink.fits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import nom.tam.fits.AsciiTable;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.BufferedFile;
import org.w3c.dom.Node;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ndx.ArraysBulkDataImpl;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.ndx.BulkDataImpl;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxImpl;
import uk.ac.starlink.util.SourceReader;

/**
 * Turns URLs which reference FITS files or HDUs into Ndx objects.
 * <p>
 * When writing an NDX into FITS format, the image array is written
 * as the first HDU.  Accompanying this in the headers is a FITS
 * representation of the WCS and possibly other headers beginning
 * "SNDX_" giving information about the location of other data arrays
 * (variance, quality) and possibly other metadata - these cards
 * are defined by the {@link FitsConstants} class.
 * Additional HDUs which may be written subsequent to the main one
 * are currently a numeric variance array, an unsigned byte quality array,
 * and a 1-d byte array giving the XML text of the NDX extensions.
 * <p>
 * When reading an NDX from a FITS file or image HDU, the HDU
 * (or first HDU in the FITS file) will be interpreted as the
 * image array of the NDX.  The WCS will be read from the headers of
 * that HDU.  If any of the "SNDX_" headers are present they will
 * be interpreted appropriately, otherwise you just get an NDX with
 * an image array and possibly WCS.
 * <p>
 * This is a singleton class; use {@link #getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 */
public class FitsNdxHandler implements NdxHandler {

    /** Sole instance of the class. */
    private static FitsNdxHandler instance = new FitsNdxHandler();

    private List extensions = 
        new ArrayList( FitsConstants.defaultFitsExtensions() );
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Private sole constructor.
     */
    private FitsNdxHandler() {}

    /**
     * Returns a FitsNdxHandler.
     *
     * @return  the sole instance of this class
     */
    public static FitsNdxHandler getInstance() {
        return instance;
    }

    public Ndx makeNdx( URL url ) throws IOException {

        /* Return null if it's not a FITS-like URL. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return null;
        }

        /* Try to get an image NDArray at this URL. */
        FitsArrayBuilder fab = FitsArrayBuilder.getInstance();
        final NDArray image = fab.makeNDArray( url, AccessMode.READ );

        /* Get the header block. */
        Header hdr = ((ReadableFitsArrayImpl) ((BridgeNDArray) image).getImpl())
                    .getHeader();

        /* Get the title. */
        final String title;
        if ( hdr.containsKey( FitsConstants.NDX_TITLE ) ) {
            title = hdr.getStringValue( FitsConstants.NDX_TITLE )
                       .replaceAll( "''", "'" );
        }
        else {
            title = null;
        }

        /* Get the bad bits mask. */
        final byte badbits;
        if ( hdr.containsKey( FitsConstants.NDX_BADBITS ) ) {
            badbits = (byte) hdr.getIntValue( FitsConstants.NDX_TITLE );
        }
        else {
            badbits = (byte) 0;
        }

        /* Get the variance array. */
        final NDArray variance;
        if ( hdr.containsKey( FitsConstants.NDX_VARIANCE ) ) {
            String loc = hdr.getStringValue( FitsConstants.NDX_VARIANCE );
            URL vurl = new URL( url, loc );
            variance = fab.makeNDArray( vurl, AccessMode.READ );
        }
        else {
            variance = null;
        }

        /* Get the quality array. */
        final NDArray quality;
        if ( hdr.containsKey( FitsConstants.NDX_QUALITY ) ) {
            String loc = hdr.getStringValue( FitsConstants.NDX_QUALITY );
            URL qurl = new URL( url, loc );
            quality = fab.makeNDArray( qurl, AccessMode.READ );
        }
        else {
            quality = null;
        }

        /* Get the WCS information. */
        final FrameSet wcs;
        final Iterator cardIt = hdr.iterator();
        Iterator lineIt = new Iterator() {
            public boolean hasNext() { return cardIt.hasNext(); }
            public Object next() { return cardIt.next().toString(); }
            public void remove() { throw new UnsupportedOperationException(); }
        };
        FitsChan fchan = new FitsChan( lineIt );
        AstObject aobj;
        try {
            aobj = fchan.read();
        }
        catch ( AstException e ) {
            aobj = null;
        }
        if ( aobj instanceof FrameSet ) {
            wcs = (FrameSet) aobj;
        }
        else {
            wcs = null;
        }

        /* Get the extensions object. */
        final Node etc;
        if ( hdr.containsKey( FitsConstants.NDX_ETC ) ) {
            String loc = hdr.getStringValue( FitsConstants.NDX_ETC );
            URL xurl = new URL( url, loc );
            ArrayDataInput strm =
                fab.getReadableStream( xurl, AccessMode.READ );
            if ( strm != null ) {
                try {
                    Header xhdr = new Header( strm );
                    AsciiTable tab = new AsciiTable( xhdr );
                    tab.read( strm );
                    String[] cellval = (String[]) tab.getElement( 0, 0 );
                    String xdat = cellval[ 0 ];
                    Source xsrc = 
                        new StreamSource( new StringReader( xdat ) );
                    etc = new SourceReader().getDOM( xsrc );
                }
                catch ( FitsException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
                catch ( TransformerException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
            }
            else {
                etc = null;
            }
        }
        else {
            etc = null;
        }

        /* Create and return a new Ndx. */
        NdxImpl impl = new NdxImpl() {
            public byte getBadBits() { return badbits; }
            public boolean hasEtc() { return etc != null; }
            public Source getEtc() { return new DOMSource( etc ); }
            public boolean hasTitle() { return title != null; }
            public String getTitle() { return title; }
            public boolean hasWCS() { return wcs != null; }
            public Object getWCS() { return wcs; }
            public BulkDataImpl getBulkData() { 
                return new ArraysBulkDataImpl( image, variance, quality );
            }
        };
        return new BridgeNdx( impl );
    }

    public boolean outputNdx( URL url, Ndx ndx ) throws IOException {

        /* Return false if it's not a FITS-like URL. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return false;
        }
        if ( furl.getHDU() != 1 ) {
            throw new IOException( 
                "Cannot write an NDX at HDU > 1 in FITS file (" + url + ")" );
        }

        /* Construct Header object containing information about this NDX. */
        int nhdu = 1;
        HeaderCard[] cards;
        try {
            List cardlist = new ArrayList();

            cardlist.add( commentCard( "DATA component of NDX structure" ) );
            if ( ndx.hasTitle() ) {
                String title = ndx.getTitle();
                title = title.replaceAll( "'", "''" );
                String comm = "NDX title";
                int maxleng = 80 - 15 - comm.length();
                if ( title.length() > maxleng ) {
                    title = title.substring( 0, maxleng - 2 ) + "..";
                }
                cardlist.add( new HeaderCard( FitsConstants.NDX_TITLE, 
                                              title, comm ) );
            }
            int badbits = ndx.getBadBits();
            if ( badbits != 0 ) {
                cardlist.add( new HeaderCard( FitsConstants.NDX_BADBITS, 
                                              badbits, "NDX bad bits mask" ) );
            }
            if ( ndx.hasVariance() ) {
                nhdu++;
                cardlist.add( 
                    new HeaderCard( FitsConstants.NDX_VARIANCE, "#" + nhdu,
                                    "Location of NDX Variance array" ) );
            }
            if ( ndx.hasQuality() ) {
                nhdu++;
                cardlist.add( 
                    new HeaderCard( FitsConstants.NDX_QUALITY, "#" + nhdu,
                                    "Location of NDX Quality array" ) );
            }
            if ( ndx.hasEtc() ) {
                nhdu++;
                cardlist.add(
                    new HeaderCard( FitsConstants.NDX_ETC, "#" + nhdu,
                                    "Location of NDX extensions block" ) );
            }

            /* Write the WCS into the FITS headers. */
            FitsChan fchan = new FitsChan();
            fchan.setEncoding( FitsConstants.WCS_ENCODING );
            fchan.write( ndx.getWCS() );
            for ( Iterator it = fchan.iterator(); it.hasNext(); ) {
                cardlist.add( new HeaderCard( (String) it.next() ) );
            }
            cards = (HeaderCard[]) cardlist.toArray( new HeaderCard[ 0 ] );
        }
        catch ( HeaderCardException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Open a stream into which the NDX can be written. */
        URL container = furl.getContainer();
        ArrayDataOutput strm;
        if ( container.getProtocol().equals( "file" ) ) {
            String filename = container.getPath();
            if ( new File( filename ).delete() ) {
                logger.warning( "Deleted existing file " + filename +
                                " prior to rewriting" );
            }
            strm = new BufferedFile( filename, "rw" );
        }
        else {
            URLConnection conn = container.openConnection();
            conn.setDoInput( false );
            conn.setDoOutput( true );

            /* The following may throw a java.net.UnknownServiceException
             * (which is-a IOException) - in fact it almost certiainly will,
             * since I don't know of any URL protocols (including file)
             * which support output streams. */
            conn.connect();
            OutputStream stream = conn.getOutputStream();
            strm = new BufferedDataOutputStream( stream );
        }

        /* Write the image array, containing all the main NDX headers. */
        NDArray im = ndx.getImage();
        ArrayImpl iimpl = 
            new WritableFitsArrayImpl( im.getShape(), im.getType(),
                                       im.getBadHandler().getBadValue(),
                                       strm, true, cards );
        NDArrays.copy( im, new BridgeNDArray( iimpl ) ); 

        /* Write the variance array if there is one. */
        if ( ndx.hasVariance() ) {
            NDArray var = ndx.getVariance();
            HeaderCard[] vcards = new HeaderCard[] { 
                commentCard( "VARIANCE component of NDX structure" ) };
            ArrayImpl vimpl = 
                new WritableFitsArrayImpl( var.getShape(), var.getType(),
                                           var.getBadHandler().getBadValue(),
                                           strm, false, vcards );
            NDArrays.copy( var, new BridgeNDArray( vimpl ) );
        }

        /* Write the quality array if there is one. */
        if ( ndx.hasQuality() ) {
            NDArray qual = ndx.getQuality();
            HeaderCard[] qcards = new HeaderCard[] { 
                commentCard( "QUALITY component of NDX structure" ) };
            Type qtype = qual.getType();
            ArrayImpl qimpl =
                new WritableFitsArrayImpl( qual.getShape(), qtype, null,
                                           strm, false, qcards );
            NDArrays.copy( qual, new BridgeNDArray( qimpl ) );
        }

        /* Write the Etc as bytes in an ASCII table extension. */
        if ( ndx.hasEtc() ) {
            try {
                ByteArrayOutputStream bufos = new ByteArrayOutputStream();
                new SourceReader().writeSource( ndx.getEtc(), bufos );
                byte[] bytes = bufos.toByteArray();
                int nchar = bytes.length;
                AddableHeader hdr = new AddableHeader();
                hdr.addValue( "XTENSION", "TABLE", "ASCII table extension" );
                hdr.addValue( "BITPIX", 8, "Character values" );
                hdr.addValue( "NAXIS", 2, "Two-dimensional table" );
                hdr.addValue( "NAXIS1", nchar,
                              "Number of characters in sole row" );
                hdr.addValue( "NAXIS2", 1, "Single row" );
                hdr.addValue( "PCOUNT", 0, "No additional parameters" );
                hdr.addValue( "GCOUNT", 1, "Single table" );
                hdr.addValue( "TFIELDS", 1, "Single field" );
                hdr.addValue( "TBCOL1", 1, "Sole field starts at first column" );
                hdr.addValue( "TFORM1", "A" + nchar, "Text field" );
                hdr.addValue( "TTYPE1", "XML representation of NDX extensions",
                              "Field header" );
                hdr.insertComment( "XML text encoded as table entry" );
                hdr.addLine( FitsConstants.END_CARD );
                hdr.write( strm );
                strm.write( bytes );
                int over = nchar % FitsConstants.FITS_BLOCK;
                if ( over > 0 ) {
                    strm.write( new byte[ FitsConstants.FITS_BLOCK - over ] );
                }
            }
            catch ( FitsException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            catch ( TransformerException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        strm.close();
        return true;
    }

    private static HeaderCard commentCard( String text ) {
        String image = "COMMENT " + text;
        if ( image.length() > 80 ) {
            image = image.substring( 0, 80 );
        }
        return new HeaderCard( image );
    }

}
