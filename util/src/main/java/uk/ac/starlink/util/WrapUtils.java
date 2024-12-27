package uk.ac.starlink.util;

/**
 * Utilities relating to the {@link Wrapper} class.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public class WrapUtils {

    /**
     * Private constructor prevents instantiation.
     */
    private WrapUtils() {
    }

    /**
     * Returns the object on which a given object is based.
     * If <code>obj</code> is a {@link Wrapper}, it is unwrapped as far
     * as possible and the base object is returned.
     * Otherwise <code>obj</code> itself is returned.
     *
     * @param  obj  test object
     * @return   ultimate base object of <code>obj</code>
     */
    public static Object getWrapped( Object obj ) {
        while ( obj instanceof Wrapper ) {
            obj = ((Wrapper) obj).getBase();
        }
        return obj;
    }

    /**
     * Attempts to return an object of a given class on which a given
     * object is based.
     * An object is unwrapped (see {@link Wrapper#getBase}) until an
     * object of class <code>clazz</code> is found, at which point it
     * is returned.  If no <code>clazz</code> object can be found,
     * <code>null</code> is returned.
     *
     * @param  obj   test object
     * @return   object within the wrapping hierarchy of class
     *           <code>clazz</code>, or null
     */
    public static Object getWrapped( Object obj, Class<?> clazz ) {
        while ( true ) {
            if ( clazz.isAssignableFrom( obj.getClass() ) ) {
                return obj;
            }
            else if ( obj instanceof Wrapper ) {
                obj = ((Wrapper) obj).getBase();
            }
            else {
                return null;
            }
        }
    }
}
