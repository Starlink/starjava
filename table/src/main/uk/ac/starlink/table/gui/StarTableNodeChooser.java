package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import uk.ac.starlink.table.StarTable;

/**
 * Provides a browser widget which presents the hierarchy of available
 * nodes graphically and allows a StarTable to be selected.
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
public class StarTableNodeChooser {

    private static Class chooserClass;
    private static Constructor chooserConstructor;
    private static Method chooseMethod;
    private static Method setRootNodeMethod;
    private static Method setRootObjectMethod;
    private static Boolean isAvailable;

    private Object chooserObject;
    
    /** The name of the class does the work for this one. */
    static final String CHOOSER_CLASS = 
        "uk.ac.starlink.treeview.TableNodeChooser";
    static final String CHOOSE_METHOD = "chooseStarTable";
    static final String SETROOTOBJECT_METHOD = "setRootObject";
    static final String SETROOTNODE_METHOD = "setRootNode";

    /**
     * Constructs a new chooser object if the requisite classes are 
     * available.
     *
     * @return  a new chooser object, or <tt>null</tt> if the classes are
     *          not available
     */
    public static StarTableNodeChooser newInstance() {
        try {
            return isAvailable()
                   ? new StarTableNodeChooser( chooserConstructor
                                              .newInstance( new Object[ 0 ] ) )
                   : null;
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

    private StarTableNodeChooser( Object chooserObject ) {
        this.chooserObject = chooserObject;
    }

    /**
     * Pops up a modal dialog to choose a table from this chooser, with
     * default characteristics.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <tt>null</tt> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return a table corresponding to the selected DataNode,
     *         or <tt>null</tt> if none was selected or there was an error
     *         in converting it to a table
     */
    public StarTable chooseStarTable( Component parent ) {
        try {
            return (StarTable) chooseMethod.invoke( chooserObject,
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
     * Sets the root object of the chooser.
     *
     * @param   root  an object which will be turned into a 
     *          uk.ac.starlink.treeview.DataNode and then installed as
     *          the root of the chooser tree
     */
    public void setRootObject( Object root ) throws IOException {
        try {
            setRootObjectMethod.invoke( chooserObject, new Object[] { root } );
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
            else if ( e2.getClass().getName()
                   .equals( "uk.ac.starlink.treeview.NoSuchDataException" ) ) {
                throw (IOException) new IOException( e2.getMessage() )
                                   .initCause( e2 );
            }
            else {
                throw new AssertionError( e2 );
            }
        }
    }

    /**
     * Sets the root node fo the chooser.
     *
     * @param  node  new root node
     */
    public void setRootNode( Object dataNode ) {
        try {
            setRootNodeMethod.invoke( chooserObject, 
                                      new Object[] { dataNode } );
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
     * Indicates whether it will be possible to construct a 
     * StarTableNodeChooser object.  It may not be if the requisite classes
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
            catch ( ClassNotFoundException e ) {
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
                if ( chooseMethod.getReturnType() == StarTable.class && 
                     Modifier.isPublic( mMods ) && 
                     Modifier.isPublic( cMods ) && 
                     ! Modifier.isAbstract( mMods ) && 
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

    static void reflect()
            throws ClassNotFoundException, LinkageError, NoSuchMethodException {
        chooserClass = Class.forName( CHOOSER_CLASS, true,
                                      Thread.currentThread()
                                            .getContextClassLoader());
        Class nodeClass = StarTableNodeChooser.class
                         .forName( "uk.ac.starlink.treeview.DataNode" );
        chooserConstructor = chooserClass.getConstructor( new Class[ 0 ] );
        chooseMethod = chooserClass
                      .getMethod( CHOOSE_METHOD, 
                                  new Class[] { Component.class } );
        setRootObjectMethod = chooserClass
                             .getMethod( SETROOTOBJECT_METHOD, 
                                         new Class[] { Object.class } );
        setRootNodeMethod = chooserClass
                           .getMethod( SETROOTNODE_METHOD,
                                       new Class[] { nodeClass } );
    }
}
