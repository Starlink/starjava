package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Writes the data for a single column of a FITS BINTABLE.
 *
 * @author   Mark Taylor
 * @since    10 Jul 2008
 */
interface ColumnWriter {

    /**
     * Writes a value to an output stream.
     *
     * @param  stream to squirt the value's byte serialization into
     * @param  value  the value to write into <code>stream</code>
     */
    void writeValue( DataOutput stream, Object value ) throws IOException;

    /**
     * Returns the TFORM string appropriate for this writer.
     *
     * @return  format string
     */
    String getFormat();

    /**
     * Returns the format character which describes the FITS data type 
     * written by this writer.  This is always one of the actual data types,
     * never 'P' or 'Q'.
     *
     * @return   format character
     */
    char getFormatChar();

    /**
     * Returns the number of bytes that <code>writeValue</code> will write.
     *
     * @return number of bytes written to stream for each write
     */
    int getLength();

    /**
     * Returns the dimensionality (in FITS terms) of the values
     * that this writes.  Null for scalars.
     *
     * @return   dims
     */
    int[] getDims();

    /**
     * Returns zero offset to be used for interpreting values this writes.
     *
     * @param  zero value
     */
    BigDecimal getZero();

    /**
     * Returns the scale factor to be used for interpreting values this
     * writes.
     *
     * @param  scale factor
     */
    double getScale();

    /**
     * Returns the number to be used for blank field output (TNULLn).
     * Only relevant for integer scalar items.
     *
     * @return  magic bad value
     */
    Number getBadNumber();
}
