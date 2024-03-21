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
 * @deprecated  Use {@link uk.ac.starlink.votable.UnifiedFitsTableWriter
 *                         uk.ac.starlink.votable.UnifiedFitsTableWriter}
 *              instead
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
@Deprecated
public class VariableFitsTableWriter extends AbstractFitsTableWriter {

    private Boolean longIndexing_;
    private StoragePolicy storagePolicy_;

    /**
     * Constructs a writer with default characteristics.
     * It chooses sensibly between using 'P' and 'Q' format.
     */
    public VariableFitsTableWriter() {
        super( "fits-var" );
        storagePolicy_ = StoragePolicy.getDefaultPolicy();
    }

    /**
     * Deprecated custom constructor.
     *
     * @deprecated  allows some configuration options but not others;
     *              use no-arg constructor and configuration methods instead
     */
    @Deprecated
    public VariableFitsTableWriter( Boolean longIndexing,
                                    boolean allowSignedByte, WideFits wide ) {
        this();
        setLongIndexing( longIndexing );
        setAllowSignedByte( allowSignedByte );
        setWide( wide );
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
     * Sets whether this writer will use P or Q format descriptors
     * for writing variable-length arrays.
     *
     * @param  longIndexing  TRUE for 'Q' (64-bit) indexing into the heap,
     *                      FALSE for 'P' (32-bit) indexing into the heap,
     *                       null to make a sensible choice
     */
    public void setLongIndexing( Boolean longIndexing ) {
        longIndexing_ = longIndexing;
    }

    /**
     * Indicates whether this writer will use P or Q format descriptors
     * for writing variable-length arrays.
     *
     * @return  TRUE for 'Q' (64-bit) indexing into the heap,
     *         FALSE for 'P' (32-bit) indexing into the heap,
     *          null to make a sensible choice
     */
    public Boolean getLongIndexing() {
        return longIndexing_;
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
            new VariableFitsTableSerializer( getConfig(), table,
                                             storagePolicy_ );
        if ( longIndexing_ != null ) {
            fitser.set64BitMode( longIndexing_.booleanValue() );
        }
        return fitser;
    }
}
