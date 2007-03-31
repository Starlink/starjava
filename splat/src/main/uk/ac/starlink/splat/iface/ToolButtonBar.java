/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     10-JAN-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import org.tigris.toolbar.toolbutton.PopupToolBoxButton;

import uk.ac.starlink.splat.iface.images.ImageHolder;

/**
 * Creates and manages a dynamic {@link JToolBar} that arranges for any
 * {@link Action}s that cannot be shown (because of insufficient room in the
 * toolbar) to be made available in a sub-menu.
 * <p>
 * The menu is created using a {@link org.tigris.toolbar.toolbutton.ToolButton}
 * instance. A {@link Container} must be given for containing the toolbar,
 * normally this will have a BorderLayout manager and the toolbar will be
 * initially placed at BorderLayout.NORTH.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ToolButtonBar
{
    /** The JToolBar instance */
    private JToolBar toolBar = null;

    /** The container of the JToolBar */
    private Container container = null;

    /** List of Actions being used in the toolbar */
    private ArrayList actions = new ArrayList();

    /** Number of Actions to be displayed */
    private int display = 0;

    /**
     * Creates a new tool bar; orientation defaults to JScrollbar.HORIZONTAL.
     *
     * @param container the container for the toolbar which will be initially
     *                  placed at BorderLayout.NORTH.
     */
    public ToolButtonBar( Container container )
    {
        this( container, JToolBar.HORIZONTAL );
    }

    /**
     * Creates a new tool bar with the specified name. The name is used as the
     * title of the undocked tool bar. The default orientation is 
     * JToolBar.HORIZONTAL.
     *
     * @param container the container for the toolbar which will be initially
     *                  placed at BorderLayout.NORTH.
     * @param name the name of the toolbar
     */
    public ToolButtonBar( Container container, String name )
    {
        this( container, name, JToolBar.HORIZONTAL );
    }

    /**
     * Creates a new tool bar with the specified orientation. The orientation
     * must be either JToolBar.HORIZONTAL or JToolBar.VERTICAL.
     *
     * @param container the container for the toolbar which will be initially
     *                  placed at BorderLayout.NORTH.
     * @param orientation the initial orientation -- it must be either
     *                    JToolBar.HORIZONTAL or JToolBar.VERTICAL
     */
    public ToolButtonBar( Container container, int orientation )
    {
        this( container, null, orientation );
    }

    /**
     * Creates a new tool bar with a specified name and orientation. All other
     * constructors call this constructor. If orientation is an invalid value,
     * an exception will be thrown.
     *
     * @param container the container for the toolbar which will be initially
     *                  placed at BorderLayout.NORTH.
     * @param name  the name of the tool bar
     * @param orientation the initial orientation -- it must be either
     *                    JToolBar.HORIZONTAL or JToolBar.VERTICAL
     *
     */
    public ToolButtonBar( Container container, String name, int orientation )
    {
        this.container = container;
        createToolBar( name, orientation );
    }

    /**
     * Add an Action to the toolbar.
     */
    public void add( Action action )
    {
        actions.add( action );
        display++;
        updateToolBar();
    }

    /**
     * Add glue component to the toolbar. Everything added after this will be
     * right/bottom justified.
     */
    public void addGlue()
    {
        ComponentAction action = new ComponentAction( Box.createGlue() );

        //  Note no room made for it to be displayed.
        add( action );
        display--;
    }

    //  Create a toolbar for the current state and add it to the container.
    private void createToolBar( String name, int orientation )
    {
        toolBar = new JToolBar( name, orientation );
        updateToolBar();
        container.add( toolBar, BorderLayout.NORTH );

        //  Listen for changes in size of the toolbar. When resized we need to
        //  re-estimate the number of actions we can show and how many we need
        //  to display in the ToolButton and then re-create the toolbar. 
        //  This is all rough guess work.
        toolBar.addComponentListener( new ComponentAdapter()
            {
                public void componentResized( ComponentEvent e )
                {
                    //  If layout of parent isn't our BorderLayout then the
                    //  toolbar is detached and we should leave it alone.
                    if ( ! toolBar.getParent().equals( container ) ) {
                        return;
                    }

                    //  Actual size of toolbar.
                    Dimension pref = toolBar.getSize();

                    // If not realized do nothing.
                    if ( pref.width == 0 || pref.height == 0 ) return;

                    //  Size of the first button. Assume this is typical.
                    Dimension one = 
                        toolBar.getComponentAtIndex(0).getPreferredSize();

                    //  Number of buttons we can display, minus room for the
                    //  drop-down menu. Never less than 0, or greater than the
                    //  number of actions.
                    int d = 0;
                    if ( toolBar.getOrientation() == JToolBar.HORIZONTAL ) {
                        d = pref.width / one.width;
                    }
                    else {
                        d = pref.height / one.height;
                    }
                    d = Math.max( 0, Math.min( d - 2, actions.size() ) );
                    if ( d != display ) {
                        display = d;
                        updateToolBar();
                    }
                }
            });
    }

    //  Update the toolbar to display the current number of actions.
    private void updateToolBar()
    {
        if ( toolBar == null || actions.isEmpty() ) return;

        //  Remove any existing buttons.
        toolBar.removeAll();
        
        //  Add the Actions that are displayable.
        Action a = null;
        for ( int i = 0; i < display; i++ ) {
            a = (Action) actions.get( i );
            if ( a == null ) {
                toolBar.addSeparator();
            }
            else if ( a instanceof ComponentAction ) {
                toolBar.add( ((ComponentAction)a).getComponent() );
            }
            else {
                toolBar.add( a );
            }
        }

        //  Add remaining Actions to the drop-down menu.
        if ( display < actions.size() ) {
            int subitems = actions.size() - display;
            Action[] subActions = new Action[subitems];
            Action thisAction;
            for ( int j = 0, i = display; j < subitems; j++, i++ ) {
                thisAction = (Action) actions.get( i );
                if ( ! ( thisAction instanceof ComponentAction ) ) {
                    subActions[j] = thisAction;
                }
            }
            JButton button = buildPopupToolBoxButton( subActions );
            toolBar.add( button );
        }
    }

    //  Create the drop-down menu as pop-up menu of a button and populate it
    //  with a list of actions.
    private PopupToolBoxButton buildPopupToolBoxButton( Action[] actions )
    {
        PopupToolBoxButton toolBox = null;
        for ( int i = 0; i < actions.length; i++ ) {
            if ( actions[i] != null ) {
                if ( toolBox == null ) {
                    toolBox = new PopupToolBoxButton( new MoreAction(), 
                                                      0, 1, false );
                }
                toolBox.add( actions[i] );
            }
        }
        return toolBox;
    }

    //  The default action for the drop-down menu.
    class MoreAction 
        extends AbstractAction
    {
        public MoreAction()
        {
            super( "More" );
            ImageIcon icon = 
                new ImageIcon( ImageHolder.class.getResource( "more.gif" ));
            putValue( AbstractAction.SMALL_ICON, icon );
        }
        public void actionPerformed( ActionEvent e )
        {
            //  Do nothing.
        }
    }

    //  Action for a Component of some kind (glue).
    class ComponentAction
        extends AbstractAction
    {
        private Component component = null;
        public ComponentAction( Component component )
        {
            super( "Component" );
            this.component = component;
        }
        public void actionPerformed( ActionEvent e )
        {
            //  Do nothing.
        }
        public Component getComponent()
        {
            return component;
        }
    }
}
