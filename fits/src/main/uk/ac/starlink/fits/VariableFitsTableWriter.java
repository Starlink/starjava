package uk.ac.starlink.fits;

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
    private final boolean allowSignedByte_;
    private final WideFits wide_;
    private StoragePolicy storagePolicy_;

    /**
     * Constructs a writer with default characteristics.
     * It chooses sensibly between using 'P' and 'Q' format.
     */
    public VariableFitsTableWriter() {
        this( null, true, WideFits.DEFAULT );
    }

    /**
     * Constructs a writer with custom characteristics.
     *
     * @param  longIndexing  TRUE for 'Q' (64-bit) indexing into the heap,
     *                      FALSE for 'P' (32-bit) indexing into the heap,
     *                       null to make a sensible choice
     * @param   allowSignedByte  if true, bytes written as FITS signed bytes
     *          (TZERO=-128), if false bytes written as signed shorts
     * @param   wide   convention for representing over-wide tables;
     *                 null to avoid this convention
     */
    public VariableFitsTableWriter( Boolean longIndexing,
                                    boolean allowSignedByte, WideFits wide ) {
        super( "fits-var" );
        longIndexing_ = longIndexing;
        allowSignedByte_ = allowSignedByte;
        wide_ = wide;
        storagePolicy_ = StoragePolicy.getDefaultPolicy();
    }

    /**
     * Sets the storage policy which will be used for temporary storage
     * during writing.  Temporary storage is required for the heap
     * contents while the table body itself is being written.
     * By default the system default storage policy is used.
     *
     * @param  storagePolicy   policy to use
     */
    public void setStoragePolicy( StoragePolicy storagePolicy ) {
        storagePolicy_ = storagePolicy;
    }

    /**
     * Always returns false.
     */
    public boolean looksLikeFile( String location ) {
        return false;
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        VariableFitsTableSerializer fitser =
            new VariableFitsTableSerializer( table, storagePolicy_,
                                             allowSignedByte_, wide_ );
        if ( longIndexing_ != null ) {
            fitser.set64BitMode( longIndexing_.booleanValue() );
        }
        return fitser;
    }
}
