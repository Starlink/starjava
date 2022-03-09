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
        try ( InputStream in = datsrc.getInputStream() ) {
            long pos = 0;
            pos += FitsUtil.skipHDUs( in, 1 );
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
}
