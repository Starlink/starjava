package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.ArrayDataInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Table builder which can read files in 'fits-plus' format (as written
 * by {@link FitsPlusTableWriter}).  This looks for a primary header
 * in the FITS file which contains the VOTMETA header (in fact it is
 * quite inflexible about what it recognises as this format - 
 * see {@link #isMagic}) and tries to interpret the data array as a 
 * 1-d array of bytes representing the XML of a VOTable document.
 * This VOTable document should have one TABLE element with no DATA
 * content - the table data is got from the first extension HDU,
 * which must be a BINTABLE extension matching the metadata described
 * by the VOTable.
 *
 * <p>The point of all this is so that you can store a VOTable in
 * the efficient FITS format (it can be mapped if it's on local disk,
 * which makes table creation practically instantaneous, even for
 * random access) without sacrificing any of the metadata that you
 * want to encode in VOTable format.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Aug 2004
 */
public class FitsPlusTableBuilder implements TableBuilder {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {

        /* If the data source has a position, then we're being directed
         * to a particular HDU - not for us. */
        if ( datsrc.getPosition() != null ) {
            return null;
        }

        /* See if this looks like a fits-plus table. */
        if ( ! isMagic( datsrc.getIntro() ) ) {
            return null;
        }

        /* Get an input stream. */
        ArrayDataInput strm = FitsConstants.getInputStreamStart( datsrc );
        try {
    
            /* Read the header from the primary HDU to find out how long the
             * data array is. */
            Header hdr = new Header();
            int headsize = FitsConstants.readHeader( hdr, strm );
            int datasize = (int) FitsConstants.getDataSize( hdr );
            assert hdr.getIntValue( "NAXIS" ) == 1;
            assert hdr.getIntValue( "BITPIX" ) == 8;
            int nbyte = hdr.getIntValue( "NAXIS1" );

            /* Read the data from the primary HDU into a byte buffer. */
            byte[] vobuf = new byte[ nbyte ];
            strm.readFully( vobuf );

            /* Advance to the end of the primary HDU. */
            int pad = datasize - nbyte;
            IOUtils.skipBytes( strm, pad );

            /* Read XML from the byte buffer, performing a custom parse to 
             * DOM. */
            VOElementFactory vofact = new VOElementFactory();
            DOMSource domsrc = 
                vofact.transformToDOM( 
                    new StreamSource( new ByteArrayInputStream( vobuf ) ),
                    false );

            /* Obtain the TABLE element, which ought to be empty. */
            VODocument doc = (VODocument) domsrc.getNode();
            VOElement topel = (VOElement) doc.getDocumentElement();
            VOElement resel = topel.getChildByName( "RESOURCE" );
            if ( resel == null ) {
                logger.warning( "No RESOURCE element" );
                return null;
            }
            TableElement tabel = (TableElement) resel.getChildByName( "TABLE" );
            if ( tabel == null ) {
                logger.warning( "No TABLE element" );
                return null;
            }
            if ( tabel.getChildByName( "DATA" ) != null ) {
                logger.warning( "Found unexpected DATA element" );
                return null;
            }

            /* Now get the StarTable from the next HDU. */
            long[] pos = new long[ headsize + datasize ];
            StarTable starTable = FitsTableBuilder
                                 .attemptReadTable( strm, wantRandom,
                                                    datsrc, pos );

            /* Turn it into a TabularData element associated it with its
             * TABLE DOM element as if the DOM builder had found the table
             * data in a DATA element within the TABLE element. */
            tabel.setData( new TableBodies.StarTableTabularData( starTable ) );

            /* Now create and return a StarTable based on the TABLE element; 
             * its metadata comes from the VOTable, but its data comes from 
             * the FITS table we've just read. */
            VOStarTable startab = new VOStarTable( tabel );
            return startab;
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( NullPointerException e ) {
            throw (IOException)
                  new IOException( "Table not quite in fits-plus format" )
                 .initCause( e );
        }
    }

    /**
     * Returns <tt>true</tt> for a flavor with the MIME type "application/fits".
     */
    public boolean canImport( DataFlavor flavor ) {
        if ( flavor.getPrimaryType().equals( "application" ) &&
             flavor.getSubType().equals( "fits" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Tests whether a given buffer contains bytes which might be the
     * first few bytes of a FitsPlus table.
     * The criterion is that it looks like the start of a FITS header, 
     * and the first few cards look roughly like this:
     * <pre>
     *     SIMPLE  =              T
     *     BITPIX  =              8
     *     NAXIS   =              1
     *     NAXIS1  =            ???
     *     VOTMETA =              T
     * </pre>
     *
     * @param  buffer  byte buffer containing leading few bytes of data
     * @return  true  if it looks like a FitsPlus file
     */
    public static boolean isMagic( byte[] buffer ) {
        final int ntest = 5;
        HeaderCard[] cards = new HeaderCard[ ntest ];
        int pos = 0;
        int ncard = 0;
        boolean ok = true;
        for ( int il = 0; ok && il < ntest; il++ ) {
            if ( buffer.length > pos + 80 ) {
                char[] cbuf = new char[ 80 ];
                for ( int ic = 0; ic < 80; ic++ ) {
                    cbuf[ ic ] = (char) ( buffer[ pos++ ] & 0xff );
                }
                try {
                    HeaderCard card = new HeaderCard( new String( cbuf ) );
                    ok = ok && cardOK( il, card );
                }
                catch ( FitsException e ) {
                    ok = false;
                }
            }
        }
        return ok;
    }

    /**
     * Checks whether the i'th card looks like it should do for the file
     * to be readable by this handler.
     *
     * @param  icard  card index
     * @param  card   header card
     * @return  true  if <tt>card</tt> looks like the <tt>icard</tt>'th
     *          header card of a FitsPlus primary header should do
     */
    private static boolean cardOK( int icard, HeaderCard card )
            throws FitsException {
        String key = card.getKey();
        String value = card.getValue();
        switch ( icard ) {
            case 0:
                return "SIMPLE".equals( key ) && "T".equals( value );
            case 1:
                return "BITPIX".equals( key ) && "8".equals( value );
            case 2:
                return "NAXIS".equals( key ) && "1".equals( value );
            case 3:
                return "NAXIS1".equals( key );
            case 4:
                return "VOTMETA".equals( key );
            default:
                return true;
        }
    }
}
