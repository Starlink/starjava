package uk.ac.starlink.array;

/**
 * Converts values between primitive types. 
 * The conversion will typically be like a typecast, though conversions
 * involving scaling or other functions may equally be provided.
 * A given Converter object converts, in both directions, between 
 * two primitive types which are labelled 1 and 2.
 * Bad value processing is part of the interface; a bad value at the input
 * end of the conversion will result in a bad value at the output end,
 * and a non-bad value at the input end may result in a bad value at
 * the output end, for instance in the case of overflow.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface Converter {

    /**
     * Returns the data type of type 1.
     *
     * @return  Type object representing primitive type 1
     */
    public Type getType1();

    /**
     * Returns the data type of type 2.
     *
     * @return  Type object representing primitive type 2
     */
    public Type getType2();

    /**
     * Returns the bad value handler used for type 1.
     *
     * @return  the type 1 BadHandler object
     */
    public BadHandler getBadHandler1();

    /**
     * Returns the bad value handler used for type 2.
     *
     * @return  the type 2 BadHandler object
     */
    public BadHandler getBadHandler2();

    /**
     * Indicates whether conversion from type 1 to type 2 does any work.
     * This will return true only if type 1 and type 2 are equivalent,
     * and all values converted from type 1 to type 2 are guaranteed to
     * be element-for-element identical.
     *
     * @return   true if no useful work is done by the convert12 method
     */
    public boolean isUnit12();

    /**
     * Indicates whether conversion from type 2 to type 1 does any work.
     * This will return true only if type 1 and type 2 are equivalent,
     * and all values converted from type 2 to type 1 are guaranteed to
     * be element-for-element identical.
     *
     * @return   true if no useful work is done by the convert21 method
     */
    public boolean isUnit21();

    /**
     * Converts a sequence of values of type 1 to a sequence of 
     * values of type 2.
     * 
     * @param  src1    the source array, which must be an array of 
     *                 primitive type 1 with at least srcPos+length elements
     * @param  srcPos  the position in the source array of the first element 
     *                 to be converted
     * @param  dest2   the destination array, which must be an array of
     *                 primitive type 2 with at least destPos+length elements.
     *                 If type 1 and type 2 are the same, it is permissible 
     *                 for src1 and dest2 to be references to the same object
     * @param  destPos the position in the destination array at which the 
     *                 first converted element will be written
     * @param  length  the number of elements to convert
     *
     * @throws IndexOutOfBoundsException  if access outside the bounds of the
     *                                    source or destination array is
     *                                    attempted
     */
    public void convert12( Object src1, int srcPos,
                           Object dest2, int destPos, int length );

    /**
     * Converts a sequence of values of type 2 to a sequence of 
     * values of type 1.
     *
     * @param  src2    the source array, which must be an array of
     *                 primitive type 2 with at least srcPos+length elements
     * @param  srcPos  the position in the source array of the first element
     *                 to be converted
     * @param  dest1   the destination array, which must be an array of
     *                 primitive type 1 with at least destPos+length elements.
     *                 If type 1 and type 2 are the same, it is permissible 
     *                 for src2 and dest1 to be references to the same object
     * @param  destPos the position in the destination array at which the
     *                 first converted element will be written
     * @param  length  the number of elements to convert
     *
     * @throws IndexOutOfBoundsException  if access outside the bounds of the
     *                                    source or destination array is
     *                                    attempted
     */
    public void convert21( Object src2, int srcPos,
                           Object dest1, int destPos, int length );
}
