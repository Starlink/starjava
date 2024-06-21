package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.ColFitsStarTable;
import uk.ac.starlink.fits.FitsHeader;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.fits.ParsedCard;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Implementation of the <code>TableBuilder</code> interface which reads
 * tables stored in column-oriented FITS binary table format.
 * The table data is stored in a BINTABLE extension which has a single row;
 * each cell in this row contains the data for an entire column of the
 * represented table.  The primary HDU contains a byte[] array giving 
 * the table metadata as a VOTable, as for {@link FitsPlusTableBuilder}.
 * If the VOTMETA card in the primary HDU does not have the value T,
 * the VOTable metadata array is ignored.
 *
 * <p>This rather specialised format may provide good performance for
 * certain operations on very large, especially very wide, tables.
 * Although it is FITS and can therefore be used in principle for data
 * interchange, in practice most non-STIL processors are unlikely to
 * be able to do much useful with it.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2006
 */
public class ColFitsPlusTableBuilder implements TableBuilder {

    private final WideFits wide_;

    /**
     * Default constructor.
     */
    public ColFitsPlusTableBuilder() {
        this( WideFits.DEFAULT );
    }

    /**
     * Constructor.
     *
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     */
    public ColFitsPlusTableBuilder( WideFits wide ) {
        wide_ = wide;
    }

    public String getFormatName() {
        return "colfits-plus";
    }

    public boolean looksLikeFile( String location ) {
        return location.toLowerCase().endsWith( ".colfits" );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream from " + getFormatName()
                                      + " format" );
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws IOException {

        /* If the data source has a position, then we're being directed
         * to a particular HDU - not for us. */
        if ( datsrc.getPosition() != null ) {
            throw new TableFormatException( "Can't locate numbered HDU" );
        }

        /* See if the data looks like colfits format. */
        if ( ! isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException(
                "Doesn't look like a " + getFormatName() + " file" );
        }

        /* Try to read the table metadata from the primary HDU. */
        InputStream in = datsrc.getInputStream();
        long dataPos;
        TableElement tableMeta;
        FitsHeader hdr;
        try {
            long[] pos = new long[ 1 ];
            tableMeta = readMetadata( in, pos );

            /* Read the table data from the next HDU. */
            hdr = FitsUtil.readHeader( in );
            pos[ 0 ] += hdr.getHeaderByteCount();
            dataPos = pos[ 0 ];
        }
        finally {
            in.close();
        }

        /* Get the table itself from the next HDU. */
        StarTable tableData =
            new ColFitsStarTable( datsrc, hdr, dataPos, false, wide_ );

        /* If we got a TABLE element, combine the metadata from that and
         * the data from the FITS table to provide the output table. */
        if ( tableMeta != null ) {

            /* Turn it into a TabularData element associated with its
             * TABLE DOM element as if the DOM builder had found the table
             * data in a DATA element within the TABLE element. */
            tableMeta.setData( new TableBodies
                                  .StarTableTabularData( tableData ) );

            /* Now create and return a StarTable based on the TABLE element;
             * its metadata comes from the VOTable, but its data comes from
             * the BINTABLE HDU. */
            return new VOStarTable( tableMeta );
        }

        /* Otherwise, just return the BINTABLE table. */
        else {
            return tableData;
        }
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
     *     COLFITS =              T
     *     VOTMETA =              T
     * </pre>
     *
     * @param  buffer  byte buffer containing leading few bytes of data
     * @return  true  if it looks like a FitsPlus file
     */
    public static boolean isMagic( byte[] buffer ) {
        final int ntest = 6;
        if ( buffer.length < ntest * 80 ) {
            return false;
        }
        byte[] cbuf = new byte[ 80 ];
        for ( int il = 0; il < ntest; il++ ) {
            System.arraycopy( buffer, il * 80, cbuf, 0, 80 );
            ParsedCard<?> card = FitsUtil.parseCard( cbuf );
            if ( ! primaryHeaderCardOK( il, card ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the i'th card looks like it should do for the file
     * to be readable by this handler.
     *
     * @param  icard  card index
     * @param  card   header card
     * @return  true  if <code>card</code> looks like the <code>icard</code>'th
     *          header card of a FitsPlus primary header should do
     */
    private static boolean primaryHeaderCardOK( int icard,
                                                ParsedCard<?> card ) {
        String key = card.getKey();
        Object value = card.getValue();
        switch ( icard ) {
            case 4:
                return "COLFITS".equals( key ) && Boolean.TRUE.equals( value );
            case 5:
                return "VOTMETA".equals( key );
            default:
                return FitsPlusTableBuilder.primaryHeaderCardOK( icard, card );
        }
    }

    /**
     * Reads the primary HDU of a FITS stream and returns the VOTable
     * TABLE element which is encoded in it.  On successful exit,
     * the stream will be positioned at the start of the first 
     * non-primary HDU (which should contain a BINTABLE).
     * If the primary HDU does not contain the card VOTMETA with the
     * value T[rue], then null will be returned; this indicates that
     * table metadata should be got directly from the BINTABLE HDU.
     *
     * @param   in    stream holding the data (positioned at the start)
     * @param   pos   1-element array for returning the number of bytes read
     *                into the stream
     * @return  TABLE element in the primary HDU
     */
    private TableElement readMetadata( InputStream in, long[] pos )
            throws IOException {

        /* Read a FITS header.  This should be the Primary header. */
        FitsHeader hdr = FitsUtil.readHeader( in );
        long headsize = hdr.getHeaderByteCount();
        long datasize = hdr.getDataByteCount();
        pos[ 0 ] = headsize + datasize;
        int nbyte = hdr.getRequiredIntValue( "NAXIS1" );

        /* Read the data from the primary HDU into a byte buffer. */
        byte[] vobuf = IOUtils.readBytes( in, nbyte );
        if ( vobuf.length < nbyte ) {
            throw new TableFormatException( "Primary HDU truncated" );
        }

        /* Advance to the end of the primary HDU. */
        int pad = (int) ( datasize - nbyte );
        IOUtils.skip( in, pad );

        /* If there's no VOTMETA = T card in the header, just return
         * a null element now. */
        if ( ! Boolean.TRUE.equals( hdr.getBooleanValue( "VOTMETA" ) ) ) {
            return null;
        }

        /* Read XML from the byte buffer, performing a custom
         * parse to DOM. */
        VOElementFactory vofact = new VOElementFactory();
        final DOMSource domsrc;
        try {
            domsrc = vofact.transformToDOM(
                new StreamSource( new ByteArrayInputStream( vobuf ) ), false );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( "VOTable parse failed", e );
        }

        /* Obtain the TABLE element, which ought to be empty. */
        VODocument doc = (VODocument) domsrc.getNode();
        VOElement topel = (VOElement) doc.getDocumentElement();
        NodeList tabelList = topel.getElementsByVOTagName( "TABLE" );
        int ntabel = tabelList.getLength();
        if ( ntabel == 0 ) {
            throw new TableFormatException(
                "Embedded VOTable document has no TABLE element" );
        }
        else if ( ntabel > 1 ) {
            throw new TableFormatException(
                  "Embedded VOTable document has multiple"
                + "(" + ntabel + ") TABLE elements" );
        }
        TableElement tabel = (TableElement) tabelList.item( 0 );
        if ( tabel.getChildByName( "DATA" ) != null ) {
            throw new TableFormatException(
                "Embedded VOTable document has unexpected DATA element" );
        }
        return tabel;
    }
}
