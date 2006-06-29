package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedFile;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.ColFitsStarTable;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
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

    private static final ColFitsPlusTableWriter writer_ =
        new ColFitsPlusTableWriter();

    public String getFormatName() {
        return "colfits-plus";
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

        /* See if the data source is an uncompressed file. */
        if ( ! ( datsrc instanceof FileDataSource ) ||
             datsrc.getCompression() != Compression.NONE ) {
            throw new TableFormatException( "Not uncompressed file on disk" );
        }
        File file = ((FileDataSource) datsrc).getFile();

        /* See if the data looks like colfits format. */
        if ( ! isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException(
                "Doesn't look like a " + getFormatName() + " file" );
        }

        /* Try to read the table metadata from the primary HDU. */
        Header hdr = new Header();
        long dataPos;
        TableElement tableMeta;
        BufferedFile in = new BufferedFile( file.toString() );
        try {
            long[] pos = new long[ 1 ];
            tableMeta = readMetadata( in, pos );

            /* Read the table data from the next HDU. */
            hdr = new Header();
            pos[ 0 ] += FitsConstants.readHeader( hdr, in );
            dataPos = pos[ 0 ];
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( "FITS read error" )
                               .initCause( e );
        }
        finally {
            in.close();
        }

        /* Get the table itself from the next HDU. */
        StarTable tableData = new ColFitsStarTable( file, hdr, dataPos );

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

    public static boolean isMagic( byte[] buffer ) {
        return writer_.isMagic( buffer );
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
     * @param   strm  stream holding the data (positioned at the start)
     * @param   pos   1-element array for returning the number of bytes read
     *                into the stream
     * @return  TABLE element in the primary HDU
     */
    private TableElement readMetadata( ArrayDataInput in, long[] pos )
            throws IOException {

        /* Read the first FITS block from the stream into a buffer.
         * This should contain the entire header of the primary HDU. */
        byte[] headBuf = new byte[ FitsConstants.FITS_BLOCK ];
        in.readFully( headBuf );
        try {

            /* Turn it into a header and find out the length of the
             * data unit. */
            Header hdr = new Header();
            ArrayDataInput hstrm =
                new BufferedDataInputStream(
                    new ByteArrayInputStream( headBuf ) );
            int headsize = FitsConstants.readHeader( hdr, hstrm );
            int datasize = (int) FitsConstants.getDataSize( hdr );
            pos[ 0 ] = headsize + datasize;
            int nbyte = hdr.getIntValue( "NAXIS1" );

            /* Read the data from the primary HDU into a byte buffer. */
            byte[] vobuf = new byte[ nbyte ];
            in.readFully( vobuf );

            /* Advance to the end of the primary HDU. */
            int pad = datasize - nbyte;
            IOUtils.skipBytes( in, pad );

            /* If there's no VOTMETA = T card in the header, just return
             * a null element now. */
            if ( ! hdr.getBooleanValue( "VOTMETA" ) ) {
                return null;
            }

            /* Read XML from the byte buffer, performing a custom
             * parse to DOM. */
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
                throw new TableFormatException(
                    "Embedded VOTable document has no RESOURCE element" );
            }
            TableElement tabel = (TableElement) resel.getChildByName( "TABLE" );
            if ( tabel == null ) {
                throw new TableFormatException(
                    "Embedded VOTable document has no TABLE element" );
            }
            if ( tabel.getChildByName( "DATA" ) != null ) {
                throw new TableFormatException(
                    "Embedded VOTable document has unexpected DATA element" );
            }
            return tabel;
        }
        catch ( FitsException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }
}
