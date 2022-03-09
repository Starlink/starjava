package uk.ac.starlink.fits;

/**
 * Understands how per-column metadata is stored in the headers
 * of a FITS BINTABLE extension.
 *
 * @author   Mark Taylor
 * @since    21 Mar 2017
 */
public abstract class BintableColumnHeader {

    private final CardFactory cardFactory_;

    /**
     * Constructor.
     *
     * @param  cardFactory  object which should be used to construct
     *                      header cards associated with this header
     */
    protected BintableColumnHeader( CardFactory cardFactory ) {
        cardFactory_ = cardFactory;
    }

    /**
     * Returns the card factory to use when constructing header cards
     * associated with this header.
     *
     * @return  header card factory
     */
    public CardFactory getCardFactory() {
        return cardFactory_;
    }

    /**
     * Gives the name of the actual FITS header card for the column
     * managed by this object and a standard FITS BINTABLE base header name.
     *
     * @param  stdName   standard base name for the metadata item
     *                   (for instance "TFORM" for TFORMnnn)
     * @return  complete FITS header card key name
     */
    public abstract String getKeyName( String stdName );

    /**
     * Returns an instance of this class for use with standard FITS BINTABLE
     * headers.
     *
     * @param   jcol    column index (first column has value 1)
     * @return   new instance
     */
    public static BintableColumnHeader createStandardHeader( int jcol ) {
        final String jcolStr = Integer.toString( jcol );
        return new BintableColumnHeader( CardFactory.DEFAULT ) {
            public String getKeyName( String stdName ) {
                return stdName + jcolStr;
            }
        };
    }
}
