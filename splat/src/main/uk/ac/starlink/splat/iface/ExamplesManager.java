/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-MAR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class that makes any locally installed examples available and arranges for
 * them to be loaded into SPLAT. All examples are stored in SPLAT serialised
 * stacks that are contained in the "/examples" hierarchy.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ExamplesManager
    implements ActionListener
{
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.iface.ExamplesManager" );

    /**
     * The menu to populate with the available examples.
     */
    protected JMenu targetMenu = null;

    /**
     * A SplatBrowser, used to load and display the spectra.
     */
    protected SplatBrowser browser = null;

    /**
     * Constructor.
     *
     * @param targetMenu the menu to populate.
     * @param browser the SplatBrowser for displaying any spectra.
     */
    public ExamplesManager( JMenu targetMenu, SplatBrowser browser )
    {
        this.targetMenu = targetMenu;
        this.browser = browser;
        addExamples();
    }

    /**
     * Gather the available examples and create a menu system to represent
     * them.
     */
    protected void addExamples()
    {
        JMenu examplesMenu = new JMenu( "Example data" );
        if ( buildMenus( examplesMenu ) ) {
            targetMenu.add( examplesMenu );
        }
    }

    /**
     * Create a series of menus for each of the example datasets. Returns
     * false if menus are not created for some reason.
     */
    protected boolean buildMenus( JMenu menu )
    {
        //  Access the examples list.
        InputStream strm = 
            ExamplesManager.class.getResourceAsStream( "/examples/list" );
        if ( strm == null ) {
            return false;
        }
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( strm ) );

        List examplesList = new ArrayList();
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                examplesList.add( "/examples/" + line );
            }
            rdr.close();
        }
        catch ( IOException e ) {
            return false;
        }

        for ( int i = 0; i < examplesList.size(); i++ ) {
            JMenuItem item = new JMenuItem( (String) examplesList.get(i) );
            menu.add( item );
            item.addActionListener( this );
            item.setActionCommand( (String) examplesList.get(i) );
        }
        if ( examplesList.size() > 0 ) {
            return true;
        }
        return false;
    }

//
//  Implement the ActionListener interface for loading an example dataset.
//
    public void actionPerformed( ActionEvent e )
    {
        JMenuItem item = (JMenuItem) e.getSource();
        String uri = item.getActionCommand();
        InputStream strm = 
            ExamplesManager.class.getResourceAsStream( uri );
        browser.readStack( strm, true );
    }
}
