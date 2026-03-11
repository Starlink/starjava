package uk.ac.starlink.fits;

import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Implementation of the <code>TableBuilder</code> interface which reads
 * tables stored in column-oriented FITS binary table format.
 * The table data is stored in a BINTABLE extension which has a single row;
 * each cell in this row contains the data for an entire column of the
 * represented table.
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
public class ColFitsTableBuilder extends DocumentedTableBuilder {

    private final WideFits wide_;

    /**
     * Default constructor.
     */
    public ColFitsTableBuilder() {
        this( WideFits.DEFAULT );
    }

    /**
     * Constructor.
     *
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     */
    public ColFitsTableBuilder( WideFits wide ) {
        super( new String[] { "colfits" } );
        wide_ = wide;
    }

    public String getFormatName() {
        return "colfits-basic";
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream from colFITS format" );
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( ! FitsUtil.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Doesn't look like a FITS file" );
        }

        /* Interpret HDU position. */
        final int ihdu;
        String spos = datsrc.getPosition();
        if ( spos != null && spos.trim().length() > 0 ) {

            /* If it looks like an integer, treat it as an HDU index. */
            if ( spos.matches( "[1-9][0-9]*" ) ) {
                try {
                    ihdu = Integer.parseInt( spos );
                }
                catch ( NumberFormatException e ) {
                    throw new TableFormatException( "Bad HDU index " + spos );
                }
            }

            /* Otherwise treat it as an extension name. */
            else {
                try ( InputStream in = datsrc.getInputStream() ) {
                    return findNamedTable( in, datsrc, spos, wide_ );
                }
                catch ( EOFException e ) {
                    throw new TableFormatException( "No extension found with "
                                                  + "EXTNAME or EXTNAME-EXTVER "
                                                  + "\"" + spos + "\"", e );
                }
            }
        }
        else {
            ihdu = 1;
        }

        /* Read the table at the identified HDU index. */
        try ( InputStream in = datsrc.getInputStream() ) {
            long pos = 0;
            pos += FitsUtil.skipHDUs( in, ihdu );
            FitsHeader hdr = FitsUtil.readHeader( in );
            pos += hdr.getHeaderByteCount();
            return new ColFitsStarTable( datsrc, hdr, pos, false, wide_ );
        }
        catch ( EOFException e ) {
            throw new TableFormatException( "No extensions", e );
        }
    }

    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "ColFitsTableBuilder.xml" );
    }

    /**
     * Looks through the HDUs in a given FITS stream and if it finds one
     * which matches a given name, attempts to make a table out of it.
     *
     * @param  in  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  datsrc  a DataSource which can supply the data
     *         in <code>strm</code>
     * @param  name  target extension name or name-version
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @return  a new table if successful
     */
    private static StarTable findNamedTable( InputStream in, DataSource datsrc,
                                             String name, WideFits wide )
            throws IOException {
        long pos = 0;
        while ( true ) {
            FitsHeader hdr = FitsUtil.readHeader( in );
            long headsize = hdr.getHeaderByteCount();
            long datasize = hdr.getDataByteCount();
            long datpos = pos + headsize;
            if ( FitsUtil.matchesHeaderName( hdr, name ) ) {
                return new ColFitsStarTable( datsrc, hdr, datpos, false, wide );
            }
            else {
                IOUtils.skip( in, datasize );
            }
            pos += headsize + datasize;
        }
    }
}
