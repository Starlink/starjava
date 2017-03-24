package uk.ac.starlink.fits;

/**
 * Understands how per-column metadata is stored in the headers
 * of a FITS BINTABLE extension.
 *
 * @author   Mark Taylor
 * @since    21 Mar 2017
 */
public abstract class BintableColumnHeader {

    /**
     * Constructor.
     */
    protected BintableColumnHeader() {
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
     * Returns the string value of a header card
     * for this object's column.
     *
     * @param  cards     header collection
     * @param  stdName   standard base name for the metadata item
     *                   (for instance "TFORM" for TFORMnnn)
     * @return  string value, or null for absent header
     */
    public String getStringValue( HeaderCards cards, String stdName ) {
        String key = getKeyName( stdName );
        return key == null ? null : cards.getStringValue( key );
    }

    /**
     * Returns the long integer value of a header card
     * for this object's column.
     *
     * @param  cards     header collection
     * @param  stdName   standard base name for the metadata item
     *                   (for instance "TFORM" for TFORMnnn)
     * @return  long value, or null for absent header
     */
    public Long getLongValue( HeaderCards cards, String stdName ) {
        String key = getKeyName( stdName );
        return key == null ? null : cards.getLongValue( key );
    }

    /**
     * Returns the double precision value of a header card
     * for this object's column.
     *
     * @param  cards     header collection
     * @param  stdName   standard base name for the metadata item
     *                   (for instance "TFORM" for TFORMnnn)
     * @return  double value, or null for absent header
     */
    public Double getDoubleValue( HeaderCards cards, String stdName ) {
        String key = getKeyName( stdName );
        return key == null ? null : cards.getDoubleValue( key );
    }

    /**
     * Indicates whether a given header card is present
     * for this object's column.
     *
     * @param  cards     header collection
     * @param  stdName   standard base name for the metadata item
     *                   (for instance "TFORM" for TFORMnnn)
     * @return   true iff header is present
     */
    public boolean containsKey( HeaderCards cards, String stdName ) {
        String key = getKeyName( stdName );
        return key != null && cards.containsKey( key );
    }

    /**
     * Returns an instance of this class for use with standard FITS BINTABLE
     * headers.
     *
     * @param   jcol    column index (first column has value 1)
     * @return   new instance
     */
    public static BintableColumnHeader createStandardHeader( int jcol ) {
        final String jcolStr = Integer.toString( jcol );
        return new BintableColumnHeader() {
            public String getKeyName( String stdName ) {
                return stdName + jcolStr;
            }
        };
    }
}
