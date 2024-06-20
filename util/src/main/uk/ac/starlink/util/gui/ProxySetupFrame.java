/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     13-JUN-2001 (Peter W. Draper):
 *       Original version.
 *     13-JUN-2003 (Peter W. Draper):
 *       Modified to use Preferences as the backing store. Was using a
 *       file "~/.splat/ProxyConfig.xml".
 *     17-JUN-2003 (Peter W. Draper):
 *       Refactored out of SPLAT and into UTIL.
 *     18-JUL-2003 (Peter W. Draper):
 *       Added nonProxyHosts.
 */
package uk.ac.starlink.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.util.images.ImageHolder;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.ProxySetup;

/**
 * Create a dialog window for displaying and modifying the current proxy
 * server configuration as found in a backing store.
 * <p>
 * This is basically an interface to changes the values of the system
 * properties:
 * <ul>
 *   <li><code>http.proxySet</code> A boolean (true or false) indicating
 *       whether to use the proxy.</li>
 *   <li><code>http.proxyHost</code> The proxy server name.</li>
 *   <li><code>http.proxyPort</code> The proxy server port.</li>
 *   <li><code>http.nonProxyHosts</code> A list of names that do not
 *       require the proxy server (e.g. *.dur.ac.uk|localhost)</li>
 * </ul>
 * A typical invocation would follow the sequence:
 * <pre>
 *     ProxySetupFrame.restore( null );
 *     ...
 *     ProxySetupFrame frame = new ProxySetupFrame();
 * </pre>
 * Which would restore the backing store configuration first and then
 * create a dialog to change or view it. Typically the restoration
 * would occur in an network aware applications startup code
 * (i.e. well before the creation of the frame itself).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ProxySetupFrame 
    extends JFrame
{
    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     * Other UI elements
     */
    protected JCheckBox needProxy = new JCheckBox();
    protected JTextField hostName = new JTextField( 15 );
    protected JTextField portNumber = new JTextField( 15 );
    protected JTextField nonHostNames = new JTextField( 15 );
    protected JButton acceptButton = new JButton();
    protected JButton cancelButton = new JButton();

    /**
     * The ProxySetup instance.
     */
    private ProxySetup proxySetup = null;

    /**
     * Create an instance.
     */
    public ProxySetupFrame()
    {
        // Restore any existing values.
        proxySetup = ProxySetup.getInstance();

        initUI();
        initMenus();
        initFrame();

        //  Make the interface reflect the system proxy properties.
        matchToProperties();
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        JComponent pane = (JComponent) getContentPane();
        GridBagLayouter layouter = 
            new GridBagLayouter( pane, GridBagLayouter.SCHEME3 );

        //  Add the descriptive text.
        pane.setBorder( BorderFactory.createTitledBorder
                        ( "Configure your proxy server:" ) ); 

        //  If not direct then need hostname and port.
        JLabel needLabel = new JLabel( "Use proxy server:" );
        layouter.add( needLabel, false );
        layouter.add( needProxy, true );

        needProxy.setToolTipText( "Select if your connection to " +
                                  "the internet needs a proxy server" );
        needProxy.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    checkEntryStates();
                }
            });

        JLabel hostLabel = new JLabel( "Proxy server:" );
        layouter.add( hostLabel, false );
        layouter.add( hostName, true );

        hostName.setToolTipText
            ( "Name of proxy server, e.g. wwwcache.mydomain" );

        JLabel portLabel = new JLabel( "Port:" );
        layouter.add( portLabel, false );
        layouter.add( portNumber, true );

        portNumber.setToolTipText( "Port number of proxy server, e.g. 8080" );

        JLabel nonHostsLabel = new JLabel( "Do not proxy:" );
        layouter.add( nonHostsLabel, false );
        layouter.add( nonHostNames, true );

        nonHostNames.setToolTipText
            ( "List of names to not proxy, e.g, *.mydomain|*.other.domain|localhost" );

        //  Configure and place the Accept and Cancel buttons.
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder( BorderFactory.createEmptyBorder(10,10,0,0));
        buttonPanel.setLayout( new BoxLayout( buttonPanel,
                                              BoxLayout.X_AXIS ) );
        buttonPanel.add( Box.createHorizontalGlue() );
        buttonPanel.add( acceptButton );
        buttonPanel.add( Box.createHorizontalGlue() );
        buttonPanel.add( cancelButton );
        buttonPanel.add( Box.createHorizontalGlue() );

        //  Set various close window buttons.
        acceptButton.setToolTipText
            ( "Press to accept changes and close window" );
        cancelButton.setToolTipText
            ( "Press to cancel changes and close window" );

        //  Add button panel.
        layouter.add( buttonPanel, true );
    }

    /**
     * Initialise frame properties (disposal, menus etc.).
     */
    protected void initFrame()
    {
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        pack();
        setTitle( "Set proxy server" );
        setVisible( true );
    }

   /**
     * Initialise the menu bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add an action to close the window.
        ImageIcon cancelImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        CancelAction cancelAction = new CancelAction( "Cancel", cancelImage );
        fileMenu.add( cancelAction );
        cancelButton.setAction( cancelAction );

        ImageIcon acceptImage =
            new ImageIcon( ImageHolder.class.getResource( "accept.gif" ) );
        AcceptAction acceptAction = new AcceptAction( "Accept", acceptImage );
        fileMenu.add( acceptAction );
        acceptButton.setAction( acceptAction );
    }

    /**
     * Match interface to the current state of the system properties.
     */
    protected void matchToProperties()
    {
        needProxy.setSelected( proxySetup.isProxySet() );

        String proxyHost = proxySetup.getProxyHost();
        if ( proxyHost != null ) {
            hostName.setText( proxyHost );
        }
        String proxyPort = proxySetup.getProxyPort();
        if ( proxyPort != null ) {
            portNumber.setText( proxyPort );
        }
        String nonProxyHosts = proxySetup.getNonProxyHosts();
        if ( nonProxyHosts != null ) {
            nonHostNames.setText( nonProxyHosts );
        }
        checkEntryStates();
    }

    /**
     * Match the system properties to the state of interface.
     */
    protected void matchToInterface()
    {
        proxySetup.setProxySet( needProxy.isSelected() );
        proxySetup.setProxyHost( hostName.getText() );
        proxySetup.setProxyPort( portNumber.getText() );
        proxySetup.setNonProxyHosts( nonHostNames.getText() );
        checkEntryStates();
    }

    /**
     * Check the entry states for the proxy hostname and port.
     * These are enabled when a proxy is required and disabled when
     * not.
     */
    protected void checkEntryStates()
    {
        hostName.setEnabled( needProxy.isSelected() );
        nonHostNames.setEnabled( needProxy.isSelected() );
        portNumber.setEnabled( needProxy.isSelected() );
    }

    /**
     *  Close the window by withdrawing it. 
     *
     *  @param accept if true then the system global properties are
     *  matched to those of the interface. Otherwise the interface is
     *  modified to match the system properties (thus "accepting" or
     *  "cancelling" modifications to the proxy).
     */
    protected void closeWindowEvent( boolean accept )
    {
        if ( accept ) {
            matchToInterface();
        }
        else {
            matchToProperties();
        }
        proxySetup.store();
        this.setVisible( false );
    }

    /**
     * Inner class defining Action for closing window and keeping changes.
     */
    protected class AcceptAction extends AbstractAction
    {
        public AcceptAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent( true );
        }
    }

    /**
     * Inner class defining Action for closing window and discarding
     * changes.
     */
    protected class CancelAction extends AbstractAction
    {
        public CancelAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent( false );
        }
    }

//
// Backing store handling code.
//
    /**
     * Restore from backing store, updating the system properties.
     *
     * @param target if not null, then this should be a
     *               ProxySetupFrame that will be initialise to match
     *               the state of the backing store contents. 
     */
    public static void restore( ProxySetupFrame target )
    {
        ProxySetup.getInstance().restore();
        if ( target != null ) {
            target.matchToProperties();
        }
    }

    /**
     * Save state of system properties to backing store.
     *
     * @param target if not null, then this should be a
     *               ProxySetupFrame that has a setup that should be
     *               used in preference to the system properties. Note
     *               that after this method the system properties will
     *               be modified to reflect the stored state.
     */
    public static void store( ProxySetupFrame target )
    {
        if ( target != null ) {
            target.matchToInterface();
        }
        ProxySetup.getInstance().store();
    }
}
