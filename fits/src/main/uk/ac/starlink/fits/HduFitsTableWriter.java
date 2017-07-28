package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * TableWriter which writes a single extension BINTABLE HDU containing the 
 * table.  It differs from {@link FitsTableWriter} in that it does not
 * write a primary HDU, so the result will only form a legal FITS file
 * if it is appended to an existing FITS file which already has a primary
 * HDU, and possibly other extension HDUs already.
 * This class can be used to generate a multi-extension FITS file.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2009
 */
public class HduFitsTableWriter extends AbstractFitsTableWriter {

    private final boolean allowSignedByte_;
    private final WideFits wide_;

    /**
     * Default constructor.
     */
    public HduFitsTableWriter() {
        this( "fits-hdu", true, WideFits.DEFAULT );
    }

    /**
     * Custom constructor.
     *
     * @param   name   writer name
     * @param   allowSignedByte  if true, bytes written as FITS signed bytes
     *          (TZERO=-128), if false bytes written as signed shorts
     * @param   wide   convention for representing over-wide tables;
     *                 null to avoid this convention
     */
    public HduFitsTableWriter( String name, boolean allowSignedByte,
                               WideFits wide ) {
        super( name );
        allowSignedByte_ = allowSignedByte;
        wide_ = wide;
    }

    /**
     * Does nothing.
     */
    public void writePrimaryHDU( DataOutput out ) {
        // no action
    }

    /**
     * Returns false.
     */
    public boolean looksLikeFile( String location ) {
        return false;
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        return new StandardFitsTableSerializer( table, allowSignedByte_,
                                                wide_ );
    }
}
