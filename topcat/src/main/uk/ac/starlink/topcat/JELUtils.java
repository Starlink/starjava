package uk.ac.starlink.topcat;

import gnu.jel.Library;
import gnu.jel.DVMap;
import java.util.Date;
import java.util.Hashtable;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.  The methods provided here are static ones which
 * complement the per-row ones provided by the {@link JELRowReader} class.
 *
 * @author   Mark Taylor (Starlink)
 */
public class JELUtils {

    /**
     * This method is designed to be installed in a JEL library and
     * referenced by JEL expressions compiled at runtime.  It returns
     * an object which can be viewed as a numeric primitive by the
     * compiler but which generates a NullPointerException when 
     * evaluated.  It works using JEL's 'Automatic unwrapping of objects
     * to primitive types' functionality - JEL can view it as a
     * <tt>byte</tt> value which can be promoted to any other 
     * numeric primitive type since its declared type implements 
     * the {@link gnu.jel.reflect.Byte} interface.  It would be better
     * if it could implement all the <tt>gnu.jel.reflect</tt> 
     * reflection interfaces, but this is impossible(?) since each
     * declares a method with the same name and args (<tt>getValue()</tt>)
     * but a different return type.  Thus this will not work 
     * (it will generate a compilation error) if used in an expression
     * representing a <tt>char</tt> or <tt>boolean</tt> primitive.
     */
    public static gnu.jel.reflect.Byte NULL() {
        return null;
    }

    /**
     * Returns a JEL Library suitable for expression evaluation on this
     * column's table.  This method is provided as a utility for
     * classes which need to get a library, it is not for use by
     * compiled JEL expressions, hence it is not declared public.
     *
     * @param    rowReader  object which can read rows from the table to
     *           be used for expression evaluation
     * @return   a library
     */
    static Library getLibrary( JELRowReader rowReader ) {
        Class[] staticLib = new Class[] { Math.class, Integer.class,
                                          Float.class, Double.class,
                                          JELUtils.class };
        Class[] dynamicLib = new Class[] { JELRowReader.class };
        Class[] dotClasses = new Class[] { String.class, Date.class };
        DVMap resolver = rowReader;
        Hashtable cnmap = null;
        return new Library( staticLib, dynamicLib, dotClasses,
                            resolver, cnmap );
    }

}
