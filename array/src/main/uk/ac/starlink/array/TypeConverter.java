package uk.ac.starlink.array;

/**
 * A Converter which converts between primitive types, optionally passing
 * the values through a real function.  Bad value propagation is taken
 * care of, and conversion overflows lead to bad values in the output 
 * data.
 * 
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class TypeConverter implements Converter {

    private final Type type1;
    private final Type type2;
    private final BadHandler badHandler1;
    private final BadHandler badHandler2;
    private final ConvertWorker worker12;
    private final ConvertWorker worker21;

    /**
     * Creates a converter from one primitive type to another, which will
     * pass the values through a real function prior to doing the
     * type conversion.
     *
     * @param   type1        primitive type for data at end 1 of the conversion
     * @param   handler1     a bad value handler for the data at end 1 of
     *                       the conversion
     * @param   type2        primitive type for data at end 2 of the conversion
     * @param   handler2     a bad value handler for the data at end 2 of
     *                       the conversion
     * @param   func         a Function object representing an additional 
     *                       <code>double</code> function to apply 
     *                       to values in addition to
     *                       the type conversion.  The forward mapping will
     *                       be used for 1->2 conversions, and the inverse 
     *                       one for 2->1 conversions.  If null, a unit 
     *                       function is used (efficiently)
     */
    public TypeConverter( Type type1, BadHandler handler1, 
                          Type type2, BadHandler handler2,
                          final Function func ) {
        this.type1 = type1;
        this.type2 = type2;
        this.badHandler1 = handler1;
        this.badHandler2 = handler2;
        ConvertWorker.Mapper mapper12;
        ConvertWorker.Mapper mapper21;
        if ( func == null ) {
            mapper12 = null;
            mapper21 = null;
        }
        else {
            mapper12 = new ConvertWorker.Mapper() {
                final public double func( double x ) {
                    return func.forward( x );
                }
            };
            mapper21 = new ConvertWorker.Mapper() {
                final public double func( double x ) {
                    return func.inverse( x );
                }
            };
        }
        worker12 = ConvertWorker.makeWorker( type1, handler1, 
                                             type2, handler2, mapper12 );
        worker21 = ConvertWorker.makeWorker( type2, handler2,
                                             type1, handler1, mapper21 );
    }

 
    /**
     * Creates a converter from one primitive type to another.
     *
     * @param   type1        primitive type for data at end 1 of the conversion
     * @param   handler1     a bad value handler for the data at end 1 of
     *                       the conversion
     * @param   type2        primitive type for data at end 2 of the conversion
     * @param   handler2     a bad value handler for the data at end 2 of
     *                       the conversion
     */
    public TypeConverter( Type type1, BadHandler handler1,
                          Type type2, BadHandler handler2 ) {
        this( type1, handler1, type2, handler2, null );
    }

    /**
     * Gets the primitive type for end 1 of the converter.
     *
     * @return type 1
     */
    public Type getType1() {
        return type1;
    }

    /**
     * Gets the primitive type for end 2 of the converter.
     *
     * @return type 2
     */
    public Type getType2() {
        return type2;
    }

    /**
     * Gets the bad value handler for end 1 of the converter.
     *
     * @return  bad value handler 1
     */
    public BadHandler getBadHandler1() {
        return badHandler1;
    }

    /**
     * Gets the bad value handler for end 2 of the converter.
     *
     * @return  bad value handler 2
     */
    public BadHandler getBadHandler2() {
        return badHandler2;
    }

    /**
     * Indicates whether the conversion from type 1 to type 2 is known to
     * be a unit transformation.
     *
     * @return  true if the output of 1->2 conversion always equals its input
     */
    public boolean isUnit12() {
        return worker12.isUnit();
    }

    /**
     * Indicates whether the conversion from type 2 to type 1 is known to
     * be a unit transformation.
     *
     * @return  true if the output of 2->1 conversion always equals its input
     */
    public boolean isUnit21() {
        return worker21.isUnit();
    }

    /**
     * Converts a sequence of elements in an array of type 1 and places the
     * results in a sequence of elements in an array of type 2.
     *
     * @param   src1    array of type 1 containing input values
     * @param   srcPos  starting position of elements to convert in src1
     * @param   dest2   array of type 2 to reaceive output values
     * @param   destPos starting position of elements to write in dest2
     * @param   length  number of elements to convert
     */
    public void convert12( Object src1, int srcPos, 
                           Object dest2, int destPos, int length ) {
        worker12.convert( src1, srcPos, dest2, destPos, length );
    }

    /**
     * Converts a sequence of elements in an array of type 2 and places the
     * results in a sequence of elements in an array of type 1.
     *
     * @param   src2    array of type 2 containing input values
     * @param   srcPos  starting position of elements to convert in src2
     * @param   dest1   array of type 1 to reaceive output values
     * @param   destPos starting position of elements to write in dest1
     * @param   length  number of elements to convert
     */
    public void convert21( Object src2, int srcPos,
                           Object dest1, int destPos, int length ) {
        worker21.convert( src2, srcPos, dest1, destPos, length );
    }
}
