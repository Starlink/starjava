package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.GridBagLayouter;

/**
 * Create a dialog window for displaying and modifying the current proxy
 * server configuration as found in a backing store.
 * <p>
 * This is basically an interface to changes the values of the system
 * properties:
 * <ul>
 *   <li><tt>http.proxySet</tt> A boolean (true or false) indicating
 *       whether to use the proxy.</li>
 *   <li><tt>http.proxyHost</tt> The proxy server name.</li>
 *   <li><tt>http.proxyPort</tt> The proxy server port.</li>
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
 * @since $Date$
 * @since 13-JUN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class ProxySetupFrame extends JFrame
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
    protected JButton acceptButton = new JButton();
    protected JButton cancelButton = new JButton();

    /**
     * Create an instance.
     */
    public ProxySetupFrame()
    {
        // If the backing store document hasn't been loaded yet, do it
        // now.
        if ( document == null ) {
            restore( null );
        }
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

        JLabel hostLabel = new JLabel( "Proxy Server:" );
        layouter.add( hostLabel, false );
        layouter.add( hostName, true );

        hostName.setToolTipText
            ( "Name of proxy server, e.g. wwwcache.mydomain" );

        JLabel portLabel = new JLabel( "Port:" );
        layouter.add( portLabel, false );
        layouter.add( portNumber, true );

        portNumber.setToolTipText( "Port number of proxy server, e.g. 8080" );

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
        setTitle();
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
     * Set the title of the frame.
     */
    protected void setTitle()
    {
        setTitle( Utilities.getTitle( "Set proxy server" ) );
    }

    /**
     * Match interface to the current state of the system properties.
     */
    protected void matchToProperties()
    {
        String proxySet = System.getProperty( "http.proxySet" );
        boolean set = false;
        if ( proxySet != null && 
             proxySet.compareToIgnoreCase( "true" ) == 0 ) {
            set = true;
        }
        needProxy.setSelected( set );

        String proxyHost = System.getProperty( "http.proxyHost" );
        if ( proxyHost != null ) {
            hostName.setText( proxyHost );
        }
        String proxyPort = System.getProperty( "http.proxyPort" );
        if ( proxyPort != null ) {
            portNumber.setText( proxyPort );
        }
        checkEntryStates();
    }

    /**
     * Match the system properties to the state of interface.
     */
    protected void matchToInterface()
    {
        boolean proxySet = needProxy.isSelected();
        System.setProperty( "http.proxySet", "" + proxySet );
        String proxyHost = hostName.getText();
        System.setProperty( "http.proxyHost", proxyHost );
        String proxyPort = portNumber.getText();
        System.setProperty( "http.proxyPort", proxyPort );
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
        store( null );
        this.hide();
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
// The backing store is only consulted twice in a typical session,
// once when the application loads the proxy information, and again if
// the user enters new proxy information. The system proxy variables
// are the primary source of this information in a live application.
//
    /**
     * Backing store document, only one per-application.
     */
    protected static Document document = null;

    /**
     * Document root Element. The actual configuration is saved as
     * children of this element.
     */
    protected static Element rootElement = null;

    /**
     * Restore from backing store, updating the system properties.
     *
     * @param target if not null, then this should be a
     *               ProxySetupFrame that will be initialise to match
     *               the state of the backing store contents. 
     */
    public static void restore( ProxySetupFrame target )
    {
        recoverFromBackingStore();
        decode( rootElement );
        if ( target != null ) {
            target.matchToProperties();
        }
    }

    /**
     * Save state of system properties to backing store.
     *
     * @param target if not null, then this should be a
     *               ProxySetupFrmae that has a setup that should be
     *               used in preference to the system properties. Note
     *               that after this method the system properties will
     *               be modified to reflect the stored state.
     */
    public static void store( ProxySetupFrame target )
    {
        if ( target != null ) {
            target.matchToInterface();
        }

        //  Clear existing children.
        rootElement = new Element( "proxy-state" );
        document.setRootElement( rootElement );

        encode( rootElement );
        writeToBackingStore();
    }

    /**
     * Initialise the local DOM from the backing store file. If not
     * possible then an empty document is created. The backing store
     * is expected to be in the application configuration file
     * "ProxySetup.xml".
     */
    protected static void recoverFromBackingStore()
    {
        //  Locate the backing store file.
        File backingStore = Utilities.getConfigFile( "ProxySetup.xml" );
        if ( backingStore.canRead() ) {

            //  And parse it into a Document.
            SAXBuilder builder = new SAXBuilder( false );
            try {
                document  = builder.build( backingStore );
            }
            catch (Exception e) {
                document = null;
                e.printStackTrace();
            }
        }

        //  If the document is still null create a default one.
        if ( document == null ) {
            rootElement = new Element( "proxy-state" );
            document = new Document( rootElement );
        }
        else {
            //  Locate the root Element.
            rootElement = document.getRootElement();
        }
    }

    /**
     * Save the local DOM to backing store. This is written to the
     * application configuration file "ProxySetup.xml".
     */
    protected static void writeToBackingStore()
    {
        File backingStore = Utilities.getConfigFile( "ProxySetup.xml" );
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( backingStore );
            r = new BufferedWriter( new OutputStreamWriter( f ) );
        } 
        catch ( Exception fatal ) {
            fatal.printStackTrace();
            return;
        }

        XMLOutputter out = new XMLOutputter( "   ", true );
        out.setTextNormalize( true );
        try {
            out.output( document, r );
        }
        catch ( Exception information ) {
            information.printStackTrace();
        }
        try {
            f.close();
        }
        catch ( Exception information ) {
            information.printStackTrace();
        }
    }

    /**
     * Encode the system proxy properties as a series of child
     * elements of a given root element. The encodings are simple
     * strings.
     *
     * @param rootElement an element of the DOM.
     */
    public static void encode( Element rootElement )
    {
        addChildElement( rootElement, "http.proxySet",
                         System.getProperty( "http.proxySet" ) );
        addChildElement( rootElement, "http.proxyHost",
                         System.getProperty( "http.proxyHost" ) );
        addChildElement( rootElement, "http.proxyPort",
                         System.getProperty( "http.proxyPort" ) );
    }

    /**
     * Add an element with String value as a child of another
     * element. 
     *
     * @param rootElement element of the DOM.
     * @param name name of the tag
     * @param value text of the new element.
     */
    protected static void addChildElement( Element rootElement, String name,
                                           String value )
    {
        Element newElement = new Element( name );
        if ( value != null ) {
            newElement.setText( value );
        }
        rootElement.addContent( newElement );
    }

    /**
     * Decode the proxy server state from an XML document that was
     * encoded by this class. The properties are assumed correct when
     * extracted in name and value.
     *
     * @param rootElement element of the DOM that has children written
     *                    by the encode method of this class.
     */
    public static void decode( Element rootElement )
    {
        java.util.List children = rootElement.getChildren();
        int size = children.size();
        Element element = null;
        for ( int i = 0; i < size; i++ ) {
            element = (Element) children.get( i );
            System.setProperty( element.getName(), element.getText() );
        }
    }
}
