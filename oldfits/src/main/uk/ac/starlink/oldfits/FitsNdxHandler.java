package uk.ac.starlink.oldfits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
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
import org.w3c.dom.Element;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.DummyNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.hdx.HdxDocument;
import uk.ac.starlink.hdx.HdxDocumentFactory;
import uk.ac.starlink.hdx.HdxDOMImplementation;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxImpl;
import uk.ac.starlink.ndx.XMLNdxHandler;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * Turns URLs which reference FITS files or HDUs into Ndx objects.
 *
 * <h3>FITS file format for NDXs</h3>
 *
 * When writing an NDX into FITS format, the image array is written
 * as the primary HDU.  The headers of this HDU also contain a
 * card with the name "NDX_XML", whose value is a relative URL 
 * (of the form '#<i>n</i>', where <i>n</i> is an HDU number)
 * pointing to the HDU in the same FITS file in which XML metadata
 * concerning the NDX's structure can be found.
 * This XML is stored as the sole (character) element of a table extension.
 * Other HDUs may be written if more are needed, for instance the 
 * variance and quality arrays.  The resulting FITS file is therefore
 * a self-contained copy of the NDX's data (array components)
 * and metadata (XML stored in a table component).
 * Software which is not NDX-aware can see the data just by looking at
 * the primary HDU.
 * <p>
 * When reading an NDX from a FITS file, the handler will look for the
 * NDX_XML header; if one is found it will retrieve the metadata from
 * the XML stored in the referenced table extension as described above.
 * If this header is not present, it will make an NDX with no components
 * apart from the image array it is pointed at and any WCS defined by
 * FITS WCS headers in that HDU in the normal way.
 * <p>
 * The coordinate system information of an NDX written to a FITS file
 * is currently written to the Image HDU for use by non-NDX-aware 
 * software.  However, the WCS is read from the XML if present, so 
 * in the case of discrepancies between the two the WCS represented
 * in the FITS headers may be out of date.
 *
 * <h3>URL format</h3>
 *
 * URLs are given in the same format as for the {@link FitsArrayBuilder}
 * class. If an HDU other than the first one is referenced, that is
 * where the NDX_XML header will be sought.
 *
 * <p>
 * This is a singleton class; use {@link #getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 * @author    Norman Gray
 * @version   $Id$
 */
public class FitsNdxHandler
        implements NdxHandler, HdxDocumentFactory {

    /** Sole instance of the class. */
    private static FitsNdxHandler instance = new FitsNdxHandler();
    // register ourselves as an HdxDocumentFactory, as described in HdxFactory
    static {
        HdxFactory.registerHdxDocumentFactory(getInstance());
    }

    private List<String> extensions = 
        new ArrayList<String>( FitsConstants.defaultFitsExtensions() );
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.oldfits" );

    

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

    public Ndx makeNdx( URL url, AccessMode mode ) throws IOException {

        /* Return null if it's not a FITS-like URL. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return null;
        }

        /* Get the header block. */
        FitsArrayBuilder fab = FitsArrayBuilder.getInstance();
        ArrayDataInput strm = fab.getReadableStream( url, AccessMode.READ );
        Header hdr;
        try {
            hdr = Header.readHeader( strm );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        strm.close();

        /* If there is XML information here, read it to construct the NDX. */
        if ( hdr.containsKey( FitsConstants.NDX_XML ) ) {
            String loc = hdr.getStringValue( FitsConstants.NDX_XML );
            URL xurl = new URL( url, loc );
            ArrayDataInput xstrm =
                fab.getReadableStream( xurl, AccessMode.READ );
            try {
                Header xhdr = new Header( xstrm );
                AsciiTable tab = new AsciiTable( xhdr );
                tab.read( xstrm );
                String[] cellval = (String[]) tab.getElement( 0, 0 );
                String xdat = cellval[ 0 ];
                Source xsrc = new StreamSource( new StringReader( xdat ),
                                                xurl.toString() );

                /* Construct the NDX from the XML description found. */
                Ndx ndx = XMLNdxHandler.getInstance().makeNdx( xsrc, mode );

                // Omit this check - URLs may not be identical because of
                // path aliases like './' etc.
                //
                // /* Check that the image URL from the resultant NDX matches
                //  * the one we were first given.  Issue a warning if not. */
                // URL iurl = ndx.getImage().getURL();
                // FitsURL fiurl = FitsURL.parseURL( iurl, extensions );
                // if ( ! fiurl.equals( furl ) ) {
                //     logger.warning( "URL of image does not match URL of NDX:"
                //                   + " <" + iurl + "> != <" + url + ">" );
                // }

                /* In any case, return the NDX. */
                return ndx;
            }
            catch ( FitsException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        /* Otherwise, construct a default NDX based only on this data array
         * and any extant WCS headers. */
        else {

            /* Try to get an image NDArray at this URL. */
            final NDArray image = fab.makeNDArray( url, mode );

            /* Get Units information from the headers. */
            String units = hdr.containsKey( "BUNIT" ) 
                         ? hdr.getStringValue( "BUNIT" )
                         : null;

            /* Get the WCS information, if possible. */
            final FrameSet wcs;
            if ( AstPackage.isAvailable() ) {
                final Iterator<HeaderCard> cardIt =
                    FitsConstants.headerIterable( hdr ).iterator();
                Iterator<String> lineIt = new Iterator<String>() {
                    public boolean hasNext() { 
                        return cardIt.hasNext();
                    }
                    public String next() {
                        return cardIt.next().toString();
                    }
                    public void remove() { 
                        throw new UnsupportedOperationException();
                    }
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
            }

            /* AST subsystem is not present, do without it. */
            else {
                wcs = null;
            }

            /* Return an NDX based on the image array and WCS we have got. */
            MutableNdx ndx = new DefaultMutableNdx( image );
            if ( wcs != null ) {
                ndx.setWCS( wcs );
            }
            if ( units != null ) {
                ndx.setUnits( units );
            }
            return ndx;
        }
    }

    public boolean makeBlankNdx( URL url, Ndx template ) throws IOException {
        MutableNdx ndx = new DefaultMutableNdx( template );
        ndx.setImage( makeFitsDummyArray( template.getImage() ) );
        if ( template.hasVariance() ) {
            ndx.setVariance( makeFitsDummyArray( template.getVariance() ) );
        }
        if ( template.hasQuality() ) {
            ndx.setQuality( makeFitsDummyArray( template.getQuality() ) );
        }
        return outputNdx( url, ndx );
    }

    public boolean outputNdx( URL url, Ndx ndx ) throws IOException {

        /* Return false if it's not a FITS-like URL. */
        FitsURL furl = FitsURL.parseURL( url, extensions );
        if ( furl == null ) {
            return false;
        }
        if ( furl.getHDU() != 0 ) {
            throw new IOException( 
                "Cannot write an NDX at HDU > 0 in FITS file (" + url + ")" );
        }

        /* Open a stream into which the NDX can be written. */
        URL container = furl.getContainer();
        ArrayDataOutput strm;
        if ( container.getProtocol().equals( "file" ) ) {
            String filename = container.getPath();
            if ( new File( filename ).delete() ) {
                logger.info( "Deleted existing file " + filename +
                             " prior to rewriting" );
            }
            strm = new BufferedFile( filename, "rw" );
        }
        else {
            URLConnection conn = container.openConnection();
            conn.setDoInput( false );
            conn.setDoOutput( true );

            /* The following may throw a java.net.UnknownServiceException
             * (which is-a IOException) - in fact it almost certainly will,
             * since I don't know of any URL protocols (including file)
             * which support output streams. */
            conn.connect();
            OutputStream stream = conn.getOutputStream();
            strm = new BufferedDataOutputStream( stream );
        }

        try {
            outputNdx( strm, url, ndx );
        }
        finally {
            strm.close();
        }
        return true;
    }

    /**
     * Writes an NDX to a given output stream.  The stream is not closed
     * following the write.
     *
     * @param  strm  the stream to which the NDX should be written
     * @param  url   the URL represented by the stream; may be <tt>null</tt>
     * @param  ndx  the ndx to write
     */
    public void outputNdx( ArrayDataOutput strm, URL url, Ndx ndx ) 
            throws IOException {

        /* Construct Header object containing information about this NDX. */
        int nhdu = 0;
        int ihdu;
        int vhdu = 0;
        int qhdu = 0;
        int xhdu;
        HeaderCard[] cards;
        try {
            List<HeaderCard> cardlist = new ArrayList<HeaderCard>();

            cardlist.add( commentCard( "DATA component of NDX structure" ) );

            /* Work out what additional HDUs will go where. */
            ihdu = nhdu++;
            if ( ndx.hasVariance() ) {
                vhdu = nhdu++;
            }
            if ( ndx.hasQuality() ) {
                qhdu = nhdu++;
            }
            xhdu = nhdu++;

            /* Write a header giving units if we have them. */
            if ( ndx.hasUnits() ) {
                cardlist.add( new HeaderCard( "BUNIT", ndx.getUnits(),
                                              "Image array units" ) );
            }

            /* Write a header indicating where the XML HDU can be found. */
            cardlist.add( 
                new HeaderCard( FitsConstants.NDX_XML, "#" + xhdu,
                                "Location of NDX XML representation" ) );

            /* Write the WCS into the FITS headers. */
            if ( ndx.hasWCS() ) {
                FitsChan fchan = new FitsChan();
                fchan.setEncoding( FitsConstants.WCS_ENCODING );
                fchan.write( ndx.getAst() );
                @SuppressWarnings("unchecked")
                Iterator<String> fchit = (Iterator<String>) fchan.iterator();
                while ( fchit.hasNext() ) {
                    cardlist.add( FitsConstants
                                 .createHeaderCard( fchit.next() ) );
                }
            }
            cards = cardlist.toArray( new HeaderCard[ 0 ] );
        }
        catch ( HeaderCardException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Write the image array, containing all the main NDX headers. */
        NDArray im = ndx.getImage();
        Type itype = im.getType();
        Number ibadval = itype.isFloating() ? itype.defaultBadValue()
                                            : im.getBadHandler().getBadValue();
        ArrayImpl iimpl = 
            new WritableFitsArrayImpl( im.getShape(), itype, ibadval,
                                       strm, true, cards );
        URL iurl;
        if ( url != null ) {
            try {
                iurl = new URL( url, "#" + ihdu );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError( e );
            }
        }
        else {
            iurl = null;
        }
        NDArray inda = new BridgeNDArray( iimpl, iurl );
        NDArrays.copy( im, inda ); 

        /* Write the variance array if there is one. */
        NDArray vnda = null;
        if ( ndx.hasVariance() ) {
            NDArray var = ndx.getVariance();
            Type vtype = var.getType();
            Number vbadval = 
                vtype.isFloating() ? vtype.defaultBadValue()
                                   : var.getBadHandler().getBadValue();
            List<HeaderCard> vcardlist = new ArrayList<HeaderCard>();
            vcardlist.add( 
                commentCard( "VARIANCE component of NDX structure" ) );
            if ( ndx.hasUnits() ) {
                String vunits = "(" + ndx.getUnits() + ")**2";
                try {
                    vcardlist.add( new HeaderCard( "BUNIT", vunits,
                                                   "Variance array units" ) );
                }
                catch ( HeaderCardException e ) {
                    logger.warning( "Failed to write variance unit string " 
                                  + vunits );
                }
            }
            HeaderCard[] vcards = vcardlist.toArray( new HeaderCard[ 0 ] );
            ArrayImpl vimpl = 
                new WritableFitsArrayImpl( var.getShape(), vtype, vbadval, 
                                           strm, false, vcards );
            URL vurl;
            if ( url != null ) {
                try {
                    vurl = new URL( url, "#" + vhdu );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError( e );
                }
            }
            else {
                vurl = null;
            }
            vnda = new BridgeNDArray( vimpl, vurl );
            NDArrays.copy( var, vnda );
        }

        /* Write the quality array if there is one. */
        NDArray qnda = null;
        if ( ndx.hasQuality() ) {
            NDArray qual = ndx.getQuality();
            HeaderCard[] qcards = new HeaderCard[] { 
                commentCard( "QUALITY component of NDX structure" ) };
            Type qtype = qual.getType();
            ArrayImpl qimpl =
                new WritableFitsArrayImpl( qual.getShape(), qtype, null,
                                           strm, false, qcards );
            URL qurl;
            if ( url != null ) {
                try {
                    qurl = new URL( url, "#" + qhdu );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError( e );
                }
            }
            else {
                qurl = null;
            }
            qnda = new BridgeNDArray( qimpl, qurl );
            NDArrays.copy( qual, qnda );
        }

        /* Construct an Ndx object which matches the one we were given
         * but references its array components at the URLs we have just
         * written them to. */
        MutableNdx ondx = new DefaultMutableNdx( ndx );
        ondx.setImage( inda );
        ondx.setVariance( vnda );
        ondx.setQuality( qnda );

        /* Write the XML as bytes in an ASCII table extension. */
        try {
            uk.ac.starlink.hdx.HdxContainer hdx
                    = HdxFactory
                    .getInstance()
                    .newHdxContainer( ondx.getHdxFacade() );

            ByteArrayOutputStream bufos = new ByteArrayOutputStream();
            URI uri = url == null ? null 
                                  : URLUtils.urlToUri( url );
            new SourceReader().writeSource( hdx.getSource( uri ), bufos );

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
            strm.flush();
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        } catch (uk.ac.starlink.hdx.HdxException e) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    private static NDArray makeFitsDummyArray( NDArray nda ) {
        OrderedNDShape oshape = new OrderedNDShape( nda.getShape(),
                                                    Order.COLUMN_MAJOR );
        Type type = nda.getType();
        BadHandler bh = BadHandler.getHandler( type, null );
        return new DummyNDArray( oshape, type, bh );
        
    }

    private static HeaderCard commentCard( String text ) {
        String image = "COMMENT " + text;
        if ( image.length() > 80 ) {
            image = image.substring( 0, 80 );
        }
        return FitsConstants.createHeaderCard( image );
    }

    // implement HdxDocumentFactory

    public org.w3c.dom.Document makeHdxDocument( java.net.URL url )
            throws HdxException {
        try {
            Ndx fitsNdx = makeNdx( url, AccessMode.READ );
            if ( fitsNdx == null )
                // nothing to do with us
                return null;
        
            HdxDocument doc = (HdxDocument)HdxDOMImplementation
                    .getInstance()
                    .createDocument( null, "hdx", null );
            Element el = doc.createElement( "hdx" );
            doc.appendChild( el );
            Element ndxEl = doc.createElement( fitsNdx.getHdxFacade() );
            el.appendChild( ndxEl );
            return doc;
        } catch ( IOException ex ) {
            /*
             * Method makeNdx thought it should have been able to
             * handle this, but processing failed.  We reprocess this
             * into an HdxException.
             */
            throw new HdxException( "Failed to handle URL " + url
                                    + " (" + ex + ")");
        }
    }

    public javax.xml.transform.Source makeHdxSource( java.net.URL url )
            throws HdxException {
        return new DOMSource( makeHdxDocument( url ) );
    }
}
