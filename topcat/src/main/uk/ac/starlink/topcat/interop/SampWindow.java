package uk.ac.starlink.topcat.interop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.astrogrid.samp.gui.GuiHubConnector;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Window for display of SAMP operations.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2008
 */
public class SampWindow extends AuxWindow {

    private final GuiHubConnector connector_;

    /**
     * Constructor.
     *
     * @param   parent   parent component
     * @param   hubConnector  hub connector
     */
    @SuppressWarnings("this-escape")
    public SampWindow( Component parent, GuiHubConnector hubConnector ) {
        super( "SAMP Control", parent );
        connector_ = hubConnector;
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        main.add( hubConnector.createMonitorPanel(), BorderLayout.CENTER );

        /* Set up window-specific actions. */
        Action connectAct =
            hubConnector.createRegisterOrHubAction( this, null );
        connectAct.putValue( Action.SMALL_ICON, ResourceIcon.CONNECT );

        /* Connection menu. */
        JMenu connectMenu = new JMenu( "Connect" );
        connectMenu.setMnemonic( KeyEvent.VK_C );
        connectMenu.add( connectAct );
        getJMenuBar().add( connectMenu );

        /* Toolbar. */
        getToolBar().add( connectAct );
        getToolBar().addSeparator();

        addHelp( "SampWindow" );
    }
}
