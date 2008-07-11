package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;

/**
 * TableWriter which writes FITS BINTABLEs with variable-length arrays
 * where appropriate.
 * Array-valued columns in the input StarTable which are declared
 * with variable shapes (the last element of <code>ColumnInfo.getShape()</code>
 * is negative) will be written in the output FITS file using the
 * 'P' or 'Q' data type specifier with the actual data written in the
 * BINTABLE extension heap - see the FITS document for details.
 * Any other columns will get written in just the same way as by the
 * {@link FitsTableWriter}.
 *
 * <p>Strings and String arrays ('A' descriptor) are not currently 
 * written in variable-length form.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public class VariableFitsTableWriter extends AbstractFitsTableWriter {

    private final Boolean longIndexing_;

    /**
     * Constructs a writer which chooses sensibly between using 'P' and 'Q'
     * format.  'Q' (64-bit) indexing is only used if the heap is going to
     * be larger than 2^31 bytes.
     */
    public VariableFitsTableWriter() {
        this( null );
    }

    /**
     * Constructs a writer forcing use of either 'P' or 'Q' format for
     * variable-length array columns.

     * @param  longIndexing  true for 'Q' (64-bit) indexing into the heap,
     *                      false for 'P' (32-bit) indexing into the heap
     */
    public VariableFitsTableWriter( boolean longIndexing ) {
        this( Boolean.valueOf( longIndexing ) );
    }

    /**
     * Private constructor.
     *
     * @param  longIndexing  TRUE for 'Q' (64-bit) indexing into the heap,
     *                      FALSE for 'P' (32-bit) indexing into the heap,
     *                       null to make a sensible choice
     */
    private VariableFitsTableWriter( Boolean longIndexing ) {
        super( "fits-var" );
        longIndexing_ = longIndexing;
    }

    /**
     * Always returns false.
     */
    public boolean looksLikeFile( String location ) {
        return false;
    }

    public void writePrimaryHDU( StarTable startab, FitsTableSerializer fitser,
                                 DataOutput out )
            throws IOException {
        FitsConstants.writeEmptyPrimary( out );
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        VariableFitsTableSerializer fitser =
            new VariableFitsTableSerializer( table,
                                             StoragePolicy.getDefaultPolicy() );
        if ( longIndexing_ != null ) {
            fitser.set64BitMode( longIndexing_.booleanValue() );
        }
        return fitser;
    }
}
