package uk.ac.starlink.fits;

/**
 * Defines a convention for storing extended column data in a FITS
 * BINTABLE extension.  The general idea assumes storing the
 * BINTABLE data as if there were no 999 column limit, with a
 * container column containing the byte data for all the extended
 * columns and some non-standard way to record column metadata
 * for the columns beyond the container.
 * For practical purposes it only makes sense to use a container
 * column index of 999, the maximum allowed standard column index
 * allowed by FITS BINTABLE.  But the interface provides the
 * option to use a smaller value, perhaps for testing purposes.
 *
 * <p>There are various options for filling in the details,
 * in particular how the metadata for the extended columns
 * is stored in the FITS headers.  These are defined by the
 * implementation(s) in the {@link AbstractWideFits} class.
 *
 * <p>This convention is based on an idea suggested by William Pence
 * on the FITSBITS list in June 2012, and by Francois-Xavier Pineau (CDS)
 * in 2016.
 * It was discussed at some length on the FITSBITS mailing list in
 * July 2017 in the thread
 * <a href="https://listmgr.nrao.edu/pipermail/fitsbits/2017-July/002967.html"
 *    >BINTABLE convention for &gt;999 columns</a>
 *
 * @author   Mark Taylor
 * @since    27 Jul 2017
 */
public interface WideFits {

    /** Maximum number of standard columns, imposed by FITS standard (999). */
    public static final int MAX_NCOLSTD = 999;

    /** Default WideFits instance; currently TFORMaaa using column 999. */
    public static final WideFits DEFAULT =
        AbstractWideFits.createHierarchWideFits( 999 );

    /**
     * Returns the index of the column used (if any) by this specification
     * for extended column data.  The result is the FITS column index,
     * so the first column in the table is 1.
     * This value is equal to the number of standard columns
     * if extended columns are included.
     *
     * <p>Under normal circumstances, this returns 999 ({@link #MAX_NCOLSTD}).
     *
     * @return  1-based index of container column
     */
    int getContainerColumnIndex();

    /**
     * Maximum extended number of columns that can be represented by
     * this convention.  This value includes the standard columns.
     *
     * @return  maximum extended column count
     */
    int getExtColumnMax();

    /**
     * Returns an array of per-HDU header cards that announce the operation
     * of this convention in a BINTABLE HDU.
     *
     * @param   ncolExt   total column count including extended
     * @return  cards to append to BINTABLE header
     */
    CardImage[] getExtensionCards( int ncolExt );

    /**
     * Returns an array of per-HDU header cards
     * that describe the single container column (with index icolContainer)
     * used to implement this convention in a BINTABLE HDU.
     *
     * @param   nbyteExt  number of bytes per row in container column
     * @param   nslice  if &gt;0 this will result in a TDIMnnn header
     *                  that gives a 2-element shape, with the supplied
     *                  value being the second element;
     *                  if you don't want TDIMnnn, use 0
     * @return  cards to append to BINTABLE header
     */
    CardImage[] getContainerColumnCards( long nbyteExt, long nslice );

    /**
     * Returns the total number of columns, including extended ones,
     * described by a FITS header according to this convention.
     *
     * @param   hdr   FITS header
     * @param   ncolStd  number of 'standard' BINTABLE columns,
     *          got from TFIELDS header
     * @return   extended number of columns, includes standard ones
     */
    int getExtendedColumnCount( FitsHeader hdr, int ncolStd );

    /**
     * Creates a BintableColumnHeader instance suitable for use with
     * the extended column convention defined by this class.
     *
     * @param  icolContainer   1-based index of container column used for
     *                         storing extended column data
     * @param  jcol   1-based column index for an extended column
     * @return  new column header instance
     */
    BintableColumnHeader createExtendedHeader( int icolContainer, int jcol );
}
