/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

/**
 * Component for displaying a series of JTables contained in
 * JScrollPane in a tabbed component. Each table is associated with a
 * unique name, attempts to repeat a name result in that tab being
 * re-created.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class MultiTableView
    extends JPanel
{
    /**
     * The tabbed pane.
     */
    protected JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Create an instance. The tabbed pane is empty at this point.
     */
    public MultiTableView()
    {
        setLayout( new BorderLayout() );
        add( tabbedPane, BorderLayout.CENTER );
    }

    /**
     * Add a tab and create a JTable to go within it. The tab is given
     * the name supplied, which must be unique. If the tab already
     * exists and overwrite is true then any existing tab with that
     * name is removed.
     *
     * @param name the name for the tab.
     * @param overwrite whether to overwrite an existing table.
     *
     * @return the JTable created or null if already exists and cannot
     *         overwrite.
     */
    public JTable add( String name, boolean overwrite )
    {
        boolean exists = exists( name );
        if ( exists && ! overwrite  ) {
            return null;
        }
        if ( exists ) {
            delete( name );
        }
        return create( name );
    }

    /**
     * Delete an existing tab, if it exists.
     *
     * @param name the name of the tab.
     *
     * @return true is the tab existed.
     */
    public boolean delete( String name )
    {
        if ( exists( name ) ) {
            tabbedPane.remove( tabbedPane.indexOfTab( name ) );
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a tab with the given name exists.
     *
     * @param name of the tab.
     *
     * @return true if the tab exists.
     */
    public boolean exists( String name )
    {
        return ( tabbedPane.indexOfTab( name ) != -1 );
    }

    /**
     * Create a new tab and add a new JTable. Assumes that the
     * tab does not exist.
     *
     * @param name name of the tab.
     *
     * @return the new JTable added to the new tab.
     */
    protected JTable create( String name )
    {
        //  Create a new table.
        JTable table = new JTable();

        //  Each table resides in a scrolled pane.
        JScrollPane scroller = new JScrollPane( table );

        //  Add the scroller to the tabbed pane.
        scroller.setName( name );
        tabbedPane.add( scroller );

        //  Return the table.
        return table;
    }

    /**
     * Access a table by name.
     *
     * @param name the name of the tab.
     *
     * @return reference to the JTable displayed.
     */
    public JTable get( String name )
    {
        JTable table = null;
        if ( exists( name ) ) {
            JScrollPane pane =
                (JScrollPane) tabbedPane.getComponentAt( tabbedPane.indexOfTab( name ) );
            table = (JTable) pane.getViewport().getView();
        }
        return table;
    }
}
