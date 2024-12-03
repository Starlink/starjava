package uk.ac.starlink.parquet;

/**
 * Runtime exception indicating that an attempt was made to write
 * parquet array data in a non-LIST/MAP enclosed column
 * in a way that is not permitted by the parquet specification.
 * This usually means that an attempt has been made to write a
 * null array value or null array element.
 *
 * <p>This exception is caught and wrapped in an IOException by 
 * the {@link ParquetIO} class.
 *
 * @author   Mark Taylor
 * @since    4 Dec 2024
 */
class NullsWithoutGroupArrayException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param  msg  error message
     */
    public NullsWithoutGroupArrayException( String msg ) {
        super( msg );
    }
}
