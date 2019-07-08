package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.Icon;
import javax.swing.JFrame;

/**
 * TopcatToolAction implementation that instantiates a window of a given
 * class when invoked.  The window will be instantiated via a constructor
 * taking a single {@link java.awt.Component} argument;
 * such a constructor must therefore exist.
 * This parent component is just used to position the window,
 * it's permissible, though not encouraged, to just ignore that argument.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2013
 */
public class TopcatWindowAction<W extends JFrame>
            extends BasicAction
            implements TopcatToolAction {

    private final Constructor<? extends W> constructor_;
    private Component parent_;

    /**
     * Constructor.
     *
     * @param  name  action name
     * @param  icon  action icon
     * @param  shortdesc  action short description
     * @param  winClazz  class of window to instantiate;
     *                   must have a constructor that takes a
     *                   java.awt.Component giving the window parent
     */
    public TopcatWindowAction( String name, Icon icon, String shortdesc,
                               Class<? extends W> winClazz ) {
        super( name, icon, shortdesc );
        try {
            constructor_ = winClazz.getConstructor( new Class<?>[] {
                Component.class,
            } );
        }
        catch ( NoSuchMethodException e ) {
            String msg = new StringBuffer()
               .append( "No constructor <init>(" )
               .append( Component.class.getName() )
               .append( ")" )
               .append( " for class " )
               .append( winClazz.getName() )
               .toString();
            throw (IllegalArgumentException)
                  new IllegalArgumentException( msg ).initCause( e );
        }
    }

    /**
     * Creates an instance of the window class used by this action.
     *
     * @return  window initialised with parent component
     */
    protected W createWindow() {
        try {
            Object[] args = new Object[] { parent_ };
            try {
                return constructor_.newInstance( args );
            }
            catch ( InvocationTargetException e ) {
                throw e.getCause();
            }
        }
        catch ( RuntimeException e ) {
            throw e;
        }
        catch ( Error e ) {
            throw e;
        }
        catch ( Throwable e ) {
            throw new RuntimeException( "Window creation failed??", e );
        }
    }

    /**
     * Performs the action.
     * The default immplementation just calls {@link #createWindow}
     * and sets it visible.   This may be overridden.
     */
    public void actionPerformed( ActionEvent evt ) {
        W window = createWindow();
        window.setVisible( true );
    }

    /**
     * Sets the parent component to use for initialising windows
     * created by this action.
     *
     * @param  parent  parent component
     */
    public void setParent( Component parent ) {
        parent_ = parent;
    }
}
