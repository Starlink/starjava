package uk.ac.starlink.ndx;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Provides a browser widget which presents the hierarchy of available
 * nodes graphically and allows an Ndx to be selected.
 * <p>
 * This is currently implemented on top of classes in the
 * <tt>uk.ac.starlink.treeview</tt> package; there is no guarantee that
 * these will be available at compile time, so it's all done via
 * reflection.  Hence the interface is deliberately sparse.
 * In due course the package hierarchy ought to be reorganised so that
 * the widget on which the one provided here is based will be available
 * to application packages directly.
 * <p>
 * In the mean time, classes which know that they have access to the
 * classes in the package uk.ac.starlink.treeview may be better off
 * using those.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxChooser {

    private static Class chooserClass;
    private static Constructor chooserConstructor;
    private static Method chooseMethod;
    private static Method minMethod;
    private static Method maxMethod;
    private static Boolean isAvailable;

    private Object chooserObject;

    /** The name of the class which does the work for this one. */
    static final String CHOOSER_CLASS =
        "uk.ac.starlink.treeview.NdxNodeChooser";
    static final String CHOOSE_METHOD = "chooseNdx";
    static final String MIN_METHOD = "setMinDims";
    static final String MAX_METHOD = "setMaxDims";

    /**
     * Constructs a new chooser object for choosing NDXs of restricted
     * dimensionalities if the requisite classes are available.
     *
     * @param  minDims  the smallest number of dimensions that this
     *         chooser will accept in an NDX
     * @param  maxDims  the largest number of dimensions that this
     *         chooser will accept in an NDX
     * @return  a new chooser object, or <tt>null</tt> if the classes are
     *          not available
     */
    public static NdxChooser newInstance( int minDims, int maxDims ) {
        try {
            if ( isAvailable() ) {
                Object chooserObject =
                    chooserConstructor.newInstance( new Object[ 0 ] );
                minMethod.invoke( chooserObject,
                                  new Object[] { new Integer( minDims ) } );
                maxMethod.invoke( chooserObject,
                                  new Object[] { new Integer( maxDims ) } );
                return new NdxChooser( chooserObject );
            }
            else {
                return null;
            }
        }
        catch ( InstantiationException e ) {
            throw new AssertionError( e );
        }
        catch ( IllegalAccessException e ) {
            throw new AssertionError( e );
        }
        catch ( InvocationTargetException e ) {
            Throwable e2 = e.getTargetException();
            if ( e2 instanceof Error ) {
                throw (Error) e2;
            }
            else if ( e2 instanceof RuntimeException ) {
                throw (RuntimeException) e2;
            }
            else {
                throw new AssertionError( e2 );
            }
        }
   }

    /**
     * Constructs a new chooser object for choosing NDXs of any
     * dimensionality if the requisite classes are available.
     *
     * @return  a new chooser object, or <tt>null</tt> if the classes are
     *          not available
     */
   public static NdxChooser newInstance() {
       return newInstance( 0, Integer.MAX_VALUE );
   }

    private NdxChooser( Object chooserObject ) {
        this.chooserObject = chooserObject;
    }

    /**
     * Pops up a modal dialog to choose an Ndx from this chooser, with
     * default characteristics.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <tt>null</tt> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return a table corresponding to the selected DataNode,
     *         or <tt>null</tt> if none was selected or there was an error
     *         in converting it to a table
     */
    public Ndx chooseNdx( Component parent ) {
        try {
            return (Ndx) chooseMethod.invoke( chooserObject,
                                              new Object[] { parent } );
        }
        catch ( IllegalAccessException e ) {
            throw new AssertionError( e );
        }
        catch ( InvocationTargetException e ) {
            Throwable e2 = e.getTargetException();
            if ( e2 instanceof Error ) {
                throw (Error) e2;
            }
            else if ( e2 instanceof RuntimeException ) {
                throw (RuntimeException) e2;
            }
            else {
                throw new AssertionError( e2 );
            }
        }
    }

    /**
     * Indicates whether it will be possible to construct an
     * NdxChooser object.  It may not be if the requisite classes
     * are not in place.
     *
     * @return  true  iff {@link #newInstance} can be expected to
     *          return a new chooser
     */
    public static boolean isAvailable() {
        if ( isAvailable == null ) {
            try {
                reflect();
            }
            catch ( ClassNotFoundException e ){
                isAvailable = Boolean.FALSE;
            }
            catch ( LinkageError e ) {
                isAvailable = Boolean.FALSE;
            }
            catch ( NoSuchMethodException e ) {
                isAvailable = Boolean.FALSE;
            }

            if ( isAvailable == null ) {
                int clMods = chooserClass.getModifiers();
                int cMods = chooserConstructor.getModifiers();
                int mMods = chooseMethod.getModifiers();
                int minMods = minMethod.getModifiers();
                int maxMods = maxMethod.getModifiers();
                if ( chooseMethod.getReturnType() == Ndx.class &&
                     Modifier.isPublic( mMods ) &&
                     Modifier.isPublic( minMods ) &&
                     Modifier.isPublic( maxMods ) &&
                     Modifier.isPublic( cMods ) &&
                     ! Modifier.isAbstract( mMods ) &&
                     ! Modifier.isAbstract( minMods ) &&
                     ! Modifier.isAbstract( maxMods ) &&
                     ! chooserClass.isInterface() &&
                     ! Modifier.isAbstract( clMods ) ) {
                    isAvailable = Boolean.TRUE;
                }
                else {
                    isAvailable = Boolean.FALSE;
                }
            }
        }
        return isAvailable.booleanValue();
    }

    private static void reflect()
            throws ClassNotFoundException, LinkageError, NoSuchMethodException {
        chooserClass = Class.forName( CHOOSER_CLASS, true,
                             Thread.currentThread().getContextClassLoader() );
        chooserConstructor = chooserClass.getConstructor( new Class[ 0 ] );
        chooseMethod = chooserClass
                      .getMethod( CHOOSE_METHOD,
                                  new Class[] { Component.class } );
        minMethod = chooserClass
                   .getMethod( MIN_METHOD, new Class[] { int.class } );
        maxMethod = chooserClass
                   .getMethod( MAX_METHOD, new Class[] { int.class } );
    }
}
