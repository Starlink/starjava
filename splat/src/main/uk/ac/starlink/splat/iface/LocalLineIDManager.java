/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import uk.ac.starlink.util.gui.BasicFileFilter;

/**
 * Class that makes any locally installed line identification files
 * available as a series of items in a sub-menu.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LocalLineIDManager 
    implements ActionListener
{
    /**
     * The menu to populate with the available line identifiers.
     */
    protected JMenu targetMenu = null;

    /** 
     * A SplatBrowser, used to create the spectrum and add to global lists.
     */
    protected SplatBrowser browser = null;

    /**
     * Constructor. Single argument is the menu to populate
     *
     * @param targetMenu the menu to populate.
     */
    public LocalLineIDManager( JMenu targetMenu, SplatBrowser browser )
    {
        this.targetMenu = targetMenu;
        this.browser = browser;
        addLineIDs();
    }

    /**
     * Gather the available IDs and create a menu system to represent
     * them.
     */
    protected void addLineIDs()
    {
        // Check for any lines, before starting.
        Properties props = System.getProperties();
        if ( ! props.containsKey( "splat.etc.ids" ) ) {
            return;
        }
        String idsdir = props.getProperty( "splat.etc.ids" );
        File idsfile = new File( idsdir );
        if ( ! idsfile.isDirectory() ) {
            return;
        }
        String[] files = idsfile.list();
        if ( files.length == 0 ) {
            return;
        }

        JMenu lineIDMenu = new JMenu( "Line identifiers" );
        targetMenu.add( lineIDMenu );

        buildMenus( idsfile, lineIDMenu );
    }

    /**
     * Filter to view only ".ids" files.
     */
    protected static BasicFileFilter idsFilter = new BasicFileFilter( "ids" );

    /** 
     * Create a series of menus for the a directory. If any
     * directories are encountered these are added as submenus (unless
     * they are empty).
     */
    protected void buildMenus( File dir, JMenu menu )
    {
        File[] files = dir.listFiles( idsFilter );

        // Apply some kind of sorting.
        Arrays.sort( files );

        for ( int i = 0; i < files.length; i++ ) {
            if ( files[i].isDirectory() ) {
                String[] fileNames = files[i].list( idsFilter );
                if ( fileNames.length > 0 ) {
                    JMenu newMenu = new JMenu( files[i].getName() );
                    menu.add( newMenu );
                    buildMenus( files[i], newMenu );
                }
            }
            else {
                JMenuItem item = new JMenuItem( files[i].getName() );
                menu.add( item );
                item.addActionListener( this );
                item.setActionCommand( files[i].getPath() );
            }
        }
    }

//
//  Implement the ActionListener interface for theme changes.
//
    public void actionPerformed( ActionEvent e )
    {
        JMenuItem item = (JMenuItem) e.getSource();
        String file = item.getActionCommand();
        browser.addSpectrum( file );
    }
}
