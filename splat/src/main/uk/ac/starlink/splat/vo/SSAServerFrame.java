/*
 * Copyright (C) 2001-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     05-MAY-2005 (Peter W. Draper):
 *        Original version.
 *     22-NOV-2011 (Margarida Castro Neves mcneves@ari.uni-heidelberg.de)
 *         A new SSAP server can now be manually inserted to the server list.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.vo.MetaColumnModel;
import uk.ac.starlink.vo.RegistryTable;
import uk.ac.starlink.vo.ResourceTableModel;
import uk.ac.starlink.vo.RegResource;

/**
 * Class for interactively managing the list of SSAP servers. Allows the
 * selection of servers to be used, the update of the list from a Registry and
 * for lists of servers to be saved and restored from XML disk files.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAServerFrame
    extends JFrame
    implements PropertyChangeListener
{
    /**
     * The object that manages the actual list of servers.
     */
    private SSAServerList serverList = null;

    /**
     *  Panel for the central region.
     */
    protected JPanel centrePanel = new JPanel();

    /**
     *  The RegistryTable showing servers.
     */
    protected RegistryTable registryTable = null;

    /**
     * File chooser for storing and restoring server lists.
     */
    protected BasicFileChooser fileChooser = null;
    
    /**
     * Frame for adding a new server.
     */
    protected AddNewServerFrame addServerWindow = null;


    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAServerFrame( SSAServerList serverList )
    {
        initUI();
        initMenus();
        initFrame();
        setSSAServerList( serverList );
    }

    /**
     * Create an instance, where the visibility can be set.
     */
 /*   public SSAServerFrame( SSAServerList serverList, boolean visibility )
    {
        initUI();
       // initMenus();
        //initFrame();
   
        setSSAServerList( serverList );
        setVisible(visibility);
    }
    */
    
    /**
     * Get the SSAServerList that we are using.
     *
     * @return the SSAServerList
     */
    public SSAServerList getSSAServerList()
    {
        return serverList;
    }

    /**
     * Set the SSAServerList.
     *
     * @param serverList the SSAServerList reference.
     */
    public void setSSAServerList( SSAServerList serverList )
    {
        this.serverList = serverList;
        updateTable();
    }

    /**
     * Query a registry for any new SSAP servers. New servers must have a
     * different short name.
     */
    public void updateServers()
    {
        StarTable table = null;
        try {
            table = TableLoadPanel
                   .loadTable( this, new SSARegistryQueryDialog(),
                               new StarTableFactory() );
        }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return;
        }
        if ( table != null ) {
            if ( table instanceof BeanStarTable ) {
                Object[] resources = ((BeanStarTable)table).getData();
                for ( int i = 0; i < resources.length; i++ ) {
                    serverList.addServer( (RegResource)resources[i] );
                }
            }
            updateTable();
        }
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        getContentPane().setLayout( new BorderLayout() );

        //  RegistryTable of servers goes into a scrollpane in the center of
        //  window (along with a set of buttons, see initUI).
        registryTable = new RegistryTable( new ResourceTableModel( true ) );
        JScrollPane scroller = new JScrollPane( registryTable );

        centrePanel.setLayout( new BorderLayout() );
        centrePanel.add( scroller, BorderLayout.CENTER );
        getContentPane().add( centrePanel, BorderLayout.CENTER );
        centrePanel.setBorder( BorderFactory.createTitledBorder( "Servers" ) );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Select SSAP Servers" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 800, 500 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Action bars use BoxLayouts.
        JPanel topActionBar = new JPanel();
        topActionBar.setLayout(new BoxLayout(topActionBar, BoxLayout.X_AXIS));
        topActionBar.setBorder(BorderFactory.createEmptyBorder( 3, 3, 3, 3 ));

        JPanel botActionBar = new JPanel();
        botActionBar.setLayout(new BoxLayout(botActionBar, BoxLayout.X_AXIS));
        botActionBar.setBorder(BorderFactory.createEmptyBorder( 3, 3, 3, 3 ));

        //  Get icons.
        Icon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        Icon helpImage =
            new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        Icon readImage =
            new ImageIcon( ImageHolder.class.getResource( "read.gif" ) );
        Icon saveImage =
            new ImageIcon( ImageHolder.class.getResource( "save.gif" ) );

        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to do read a list of servers from disk file.
        ReadAction readAction = new ReadAction( "Restore", readImage );
        fileMenu.add( readAction );
        JButton readButton = new JButton( readAction );
        botActionBar.add( Box.createGlue() );
        botActionBar.add( readButton );
        readButton.setToolTipText( "Read server list back from a disk-file" );

        //  Add action to save the server list to disk file.
        SaveAction saveAction = new SaveAction( "Save", saveImage );
        fileMenu.add( saveAction );
        JButton saveButton = new JButton( saveAction );
        botActionBar.add( Box.createGlue() );
        botActionBar.add( saveButton );
        saveButton.setToolTipText( "Save server list to a disk-file" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        botActionBar.add( Box.createGlue() );
        botActionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        //  Create the Options menu.
        JMenu optionsMenu = new JMenu( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        //  Configure the proxy server.
        ProxyAction proxyAction =
            new ProxyAction( "Configure connection proxy..." );
        optionsMenu.add( proxyAction );

        //  Action to check a registry for additional/updated servers.
        QueryNewAction newAction = new QueryNewAction( "Query registry" );
        optionsMenu.add( newAction );
        JButton newButton = new JButton( newAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( newButton );
        newButton.setToolTipText( "Query registry for new SSAP servers" );
        
        //  Add action to manually add a new server to the list
        AddNewAction addNewAction = new AddNewAction( "New Server" );
        fileMenu.add( addNewAction );
        JButton addButton = new JButton( addNewAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( addButton );
        addButton.setToolTipText( "Add a new server to the list" );

        //  Remove selected servers from table.
        RemoveAction removeAction = new RemoveAction( "Remove selected" );
        optionsMenu.add( removeAction );
        JButton removeButton = new JButton( removeAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( removeButton );
        removeButton.setToolTipText
            ( "Remove selected servers from current list" );

        //  Remove all but the selected servers from table.
        RemoveUnAction removeUnAction = 
          new RemoveUnAction( "Remove unselected" );
        optionsMenu.add( removeUnAction );
        JButton removeUnButton = new JButton( removeUnAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( removeUnButton );
        removeUnButton.setToolTipText
            ( "Remove unselected servers from current list" );

        //  Add action to select all servers.
        SelectAllAction selectAllAction = new SelectAllAction( "Select all" );
        optionsMenu.add( selectAllAction );
        JButton selectAllButton = new JButton( selectAllAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( selectAllButton );
        selectAllButton.setToolTipText( "Select all servers" );

        //  Add action to just delete all servers.
        DeleteAction deleteAction = new DeleteAction( "Delete all" );
        optionsMenu.add( deleteAction );
        JButton deleteButton = new JButton( deleteAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( deleteButton );
        deleteButton.setToolTipText( "Delete all servers from current list" );

        //  Finish action bars.
        topActionBar.add( Box.createGlue() );
        botActionBar.add( Box.createGlue() );

        centrePanel.add( topActionBar, BorderLayout.SOUTH );
        getContentPane().add( botActionBar, BorderLayout.SOUTH );

        //  Add a Column menu that allows the choice of which registry
        //  query columns to show.
        JMenu columnsMenu = makeColumnVisibilityMenu( "Columns" );
        columnsMenu.setMnemonic( KeyEvent.VK_L );
        menuBar.add( columnsMenu );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "ssap-server-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Constructs a menu which allows the user to select which attributes
     * of each displayed resource are visible.
     *
     * @param   name  menu name
     */
    public JMenu makeColumnVisibilityMenu( String name )
    {
        return ((MetaColumnModel)registryTable.getColumnModel())
            .makeCheckBoxMenu( name );
    }

    /**
     * Set the proxy server and port.
     */
    protected void showProxyDialog()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.show();
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

 /*   public  JPanel getServerPanel() 
    {
        
        return (JPanel) this.getContentPane();
    }
*/
    /**
     * Update the RegistryTable to match the current list of servers.
     */
    public void updateTable()
    {
        registryTable.setData( serverList.getData() );
    }
    
    /**
     *  Delete all servers.
     */
    protected void deleteServers()
    {
        serverList.clear();
        updateTable();
    }

    /**
     *  Select all servers.
     */
    protected void selectAllServers()
    {
        registryTable.selectAll();
    }

    /**
     *  Remove selected servers.
     */
    protected void removeSelectedServers()
    {
        //  Get selected indices.
        int[] selected = registryTable.getSelectedRows();
        if ( selected != null && selected.length > 0 ) {

            //  And remove these from the server list.
            RegResource[] res = registryTable.getData();
            for ( int i = 0; i < selected.length; i++ ) {
                serverList.removeServer( res[selected[i]] );
            }
            updateTable();
        }
    }

    /**
     *  Remove unselected servers.
     */
    protected void removeUnSelectedServers()
    {
        //  Get selected indices.
        int[] selected = registryTable.getSelectedRows();
        if ( selected != null && selected.length > 0 ) {

            //  Clear the list and re-add the selected servers.
            RegResource[] res = registryTable.getData();
            serverList.clear();
            for ( int i = 0; i < selected.length; i++ ) {
                serverList.addServer( res[selected[i]] );
            }
            updateTable();
        }
    }

    /**
     *  Add new server to the server list
     */
    protected void addNewServer()
    {
        initAddServerWindow();
        addServerWindow.setVisible( true );
    }

    /**
     *  Restore servers from a previously saved server list.
     */
    protected void readServers()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                serverList.restoreServers( file );
                updateTable();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }

    /**
     *  Save server list to a disk file
     */
    protected void saveServers()
    {
        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                serverList.saveServers( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }

   
    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for XML files.
            BasicFileFilter xmlFileFilter =
                new BasicFileFilter( "xml", "XML files" );
            fileChooser.addChoosableFileFilter( xmlFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }
   /**
     * Initialise the window to insert a new server to the list.
     */
    protected void initAddServerWindow()
    {
        if ( addServerWindow == null ) {
            addServerWindow = new AddNewServerFrame();
            addServerWindow.addPropertyChangeListener(this);
        }
    }

    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        serverList.addServer(addServerWindow.getResource());
        updateTable();
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for adding a new server to the list
     */
    protected class AddNewAction
        extends AbstractAction
    {
        public AddNewAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            addNewServer();
        }
    }


    /**
     * Inner class defining action for reading a list of servers.
     */
    protected class ReadAction
        extends AbstractAction
    {
        public ReadAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            readServers();
        }
    }

    /**
     * Inner class defining action for saving a list of servers.
     */
    protected class SaveAction
        extends AbstractAction
    {
        public SaveAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            saveServers();
        }
    }

    /**
     * Inner class defining action for setting the proxy server.
     */
    protected class ProxyAction
        extends AbstractAction
    {
        public ProxyAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showProxyDialog();
        }
    }


    /**
     * Inner class defining action for query registry for new SSAP servers.
     */
    protected class QueryNewAction
        extends AbstractAction
    {
        public QueryNewAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            updateServers();
        }
    }

    /**
     * Inner class defining action for removing selected servers.
     */
    protected class RemoveAction
        extends AbstractAction
    {
        public RemoveAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            removeSelectedServers();
        }
    }

    /**
     * Inner class defining action for removing unselected servers.
     */
    protected class RemoveUnAction
        extends AbstractAction
    {
        public RemoveUnAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            removeUnSelectedServers();
        }
    }

    /**
     * Inner class defining action for selecting all known servers.
     */
    protected class SelectAllAction
        extends AbstractAction
    {
        public SelectAllAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            selectAllServers();
        }
    }

    /**
     * Inner class defining action for deleting all known servers.
     */
    protected class DeleteAction
        extends AbstractAction
    {
        public DeleteAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deleteServers();
        }
    }
    
}
