package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;

/**
 * Window for displaying and managing global variables.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2025
 */
public class VariableWindow extends AuxWindow {

    /**
     * Constructor
     *
     * @param  parent  parent component
     * @param   vpanel  variable panel
     */
    @SuppressWarnings("this-escape")
    public VariableWindow( Component parent, VariablePanel vpanel ) {
        super( "Global Variables", parent );
        JComponent mainPanel = getMainArea();
        mainPanel.setLayout( new BorderLayout() );
        mainPanel.add( vpanel );
        Action addAct = vpanel.getAddVariableAction();
        Action removeAct = vpanel.getRemoveVariableAction();
        getToolBar().add( addAct );
        getToolBar().add( removeAct );
        getToolBar().addSeparator();
        JMenu varMenu = new JMenu( "Variables" );
        varMenu.add( addAct );
        varMenu.add( removeAct );
        getJMenuBar().add( varMenu );
        addHelp( "VariableWindow" );
    }
}
