/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import javax.swing.Icon;

/**
 * Import any parts of Treeview that we can make use of. Keep this
 * loosely coupled so that if Treeview is not available we can live
 * without it (this also means that we can compile before Treeview,
 * which also depends on SPLAT).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class TreeviewAccess
{
    //  The instance.
    private static TreeviewAccess instance = null;

    //  Whether Treeview is available.
    private boolean available = false;

    //  Classes of Treeview that we need.
    private Class iconFactory = null;

    //  Only one instance needed.
    private TreeviewAccess()
    {
        // Do the initialisations.
        try {
            iconFactory = Class.forName("uk.ac.starlink.treeview.IconFactory");
            available = true;
        }
        catch ( ClassNotFoundException e ) {
            available = false;
        }
    }

    /**
     * Get access to the singleton.
     */
    public static TreeviewAccess getInstance()
    {
        if ( instance == null ) {
            instance = new TreeviewAccess();
        }
        return instance;
    }

    /**
     * Return whether Treeview classes and icons are available.
     */
    public boolean isAvailable()
    {
        return available;
    }

    /**
     * Return a Treeview Icon if available.
     *
     * @param name the name of the icon as defined in IconFactory
     *             ("FITS" etc. not file names).
     */
    public Icon getTreeviewIcon( String name )
    {
        if ( ! available ) {
            return null;
        }
        try {
            // Equivalent Java
            // IconFactory.getIcon( IconFactory.XXXX );

            Field field = iconFactory.getField( name );
            Object[] args = new Object[1];
            args[0] = field.get( instance );

            Class[] classArgs = new Class[1];
            classArgs[0] = Short.TYPE;
            Method method = iconFactory.getMethod( "getIcon", classArgs );

            return (Icon) method.invoke( instance, args );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
