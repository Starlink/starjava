package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.table.ValueInfo;

/**
 * Defines a conversion from one data type to another.
 *
 * <p>Instances of this class should be thread-safe.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2006
 */
public interface ValueConverter {

    /**
     * Returns metadata describing the values on the input end of the
     * conversion.
     *
     * @return  input info
     */
    ValueInfo getInputInfo();

    /**
     * Returns metadata describing the values on the output end of the
     * conversion.
     *
     * @return  output info
     */
    ValueInfo getOutputInfo();

    /**
     * Performs a conversion from the input type to the output type.
     * In general if the conversion cannot be done, a null value should
     * be returned rather than throwing an unchecked exception.
     *
     * @param  in   object of input type
     * @return   corresponding object of output type
     */
    Object convert( Object in );

    /**
     * Performs a reverse conversion, from the output type to the input type.
     * In general if the conversion cannot be done, a null value should
     * be returned rather than throwing an unchecked exception.
     *
     * @param  out  object of output type
     * @return  corresponding object of input type
     */
    Object unconvert( Object out );
}
