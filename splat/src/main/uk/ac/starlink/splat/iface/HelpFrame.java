/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-NOV-2000 (Peter W. Draper):
 *       Original version.
 *     13-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.net.URL;

import javax.help.HelpSetException;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;

import uk.ac.starlink.splat.util.Utilities;

/**
 * Extends {@link uk.ac.starlink.help.HelpFrame} to create window that
 * displays the SPLAT HelpSet. The main extension is to configure the
 * SPLAT-wide HelpSet and always add the application help to any menus.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class HelpFrame
    extends uk.ac.starlink.help.HelpFrame
{
    private HelpFrame()
        throws HelpSetException
    {
        super();
    }

    /**
     * Initialise the SPLAT HelpSet.
     */
    static {
        URL url = HelpFrame.class.getResource( "/HelpSet.hs" );
        if ( url != null ) {
            uk.ac.starlink.help.HelpFrame.addHelpSet( url );
        }
        else {
            System.out.println( "Failed to initialize online help" );
        }
    }

    /**
     * Add a Help menu to a given menu bar and populate it with the
     * standard items and a named topic id. If a toolbar is given then
     * the topic item is added to it.
     *
     * @param topic the help system identifier for the named topic.
     * @param description description for the menu item
     * @param menuBar menubar to add the Help menu
     * @param toolBar toolbar to add topic to (null for don't)
     *
     * @return the "Help" JMenu created.
     */
    public static JMenu createButtonHelpMenu( String topic,
                                              String description,
                                              JMenuBar menuBar,
                                              ToolButtonBar toolBar )
    {
        JMenu menu = uk.ac.starlink.help.HelpFrame.
            createHelpMenu( topic, description, "splat-help",
                            "On " + Utilities.getReleaseName(),
                            menuBar, null, true );

        //  Need to add to the toolbar by hand.
        if ( toolBar != null ) {
            try {
                Component c = menu.getMenuComponent( menu.getItemCount() - 2 );
                if ( c instanceof JMenuItem ) {
                    Action action = ((JMenuItem)c).getAction();
                    toolBar.add( action );
                    action.putValue( Action.SHORT_DESCRIPTION, "Help on window" );
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return menu;
    }

    /**
     * Add a Help menu to a given menu bar and populate it with the
     * standard items and a named topic id. If a toolbar is given then
     * the topic item is added to it.
     *
     * @param topic the help system identifier for the named topic.
     * @param description description for the menu item
     * @param menuBar menubar to add the Help menu
     * @param toolBar toolbar to add topic to (null for don't)
     *
     * @return the "Help" JMenu created.
     */
    public static JMenu createHelpMenu( String topic,
                                        String description,
                                        JMenuBar menuBar,
                                        JToolBar toolBar )
    {
        JMenu menu = uk.ac.starlink.help.HelpFrame.
            createHelpMenu( topic, description, "splat-help",
                            "On " + Utilities.getReleaseName(),
                            menuBar, toolBar, true );
        return menu;
    }
}
