package uk.ac.starlink.topcat.interop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.xmlrpc.HubMode;
import uk.ac.starlink.topcat.AuxWindow;

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
    public SampWindow( Component parent, GuiHubConnector hubConnector ) {
        super( "SAMP Control", parent );
        connector_ = hubConnector;
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        main.add( hubConnector.createMonitorPanel(), BorderLayout.CENTER );

        JComponent buttonBox = Box.createVerticalBox();
        buttonBox.add( Box.createVerticalStrut( 5 ) );
        JComponent regLine = Box.createHorizontalBox();
        main.add( new JButton( hubConnector
                              .createRegisterOrHubAction( this, null ) ),
                  BorderLayout.SOUTH );

        addHelp( "SampWindow" );
    }
}
