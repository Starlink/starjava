/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 *     21-JAN-2004 (Peter W. Draper):
 *       Refactored to offer general, non-AST storage.
 */
package uk.ac.starlink.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import org.w3c.dom.Element;

import uk.ac.starlink.util.images.ImageHolder;

/**
 * A top-level window that offers controls for saving, restoring and
 * deleting configuration data stored in XML files. The configurations
 * are stored in a permanent file which has each configuration
 * characterised by a description (created by the user) and a date
 * that the configuration was created (or maybe last updated).
 * <p>
 * An instance of this class should be associated with a 
 * {@link StoreSource} implementation that acts as a view for the restored
 * configuration and a model for the current configuration. The actual
 * interaction with the XML store is performed by a StoreConfiguration
 * object. 
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see StoreConfiguration
 * @see StoreSource
 */
public class StoreControlFrame 
    extends JFrame
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Container for view of currently saved states.
     */
    protected JPanel statusView = new JPanel();

    /**
     * The table showing the stored configurations.
     */
    protected JTable statusTable = new JTable();

    /**
     * The StoreSource object, this understands the configuration
     * data and how to encode and decode it from XML.
     */
    protected StoreSource storeSource = null;

    /**
     * Object that mediates to the actual store.
     */
    protected StoreConfiguration store = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu( "File" );

    /**
     * Create an instance.
     */
    @SuppressWarnings("this-escape")
    public StoreControlFrame( StoreSource storeSource )
    {
        this.storeSource = storeSource;
        contentPane = (JPanel) getContentPane();
        initMenus();
        initUI();
        initFrame();
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );
        menuBar.add( fileMenu );
        fileMenu.setMnemonic( KeyEvent.VK_F );

        //  Action bar uses a BoxLayout.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Add actions to the action bar to create a new
        //  configuration (which is initialised to the current setup).

        ImageIcon addImage = new ImageIcon(
            ImageHolder.class.getResource( "add.gif" ) );
        AddAction addAction = new AddAction( "Store", addImage );
        JButton addButton = new JButton( addAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( addButton );
        addButton.setToolTipText( "Store the current configuration" );

        ImageIcon updateImage = new ImageIcon(
            ImageHolder.class.getResource( "update.gif" ) );
        UpdateAction updateAction = new UpdateAction( "Update", updateImage );
        JButton updateButton = new JButton( updateAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( updateButton );
        updateButton.setToolTipText( "Update the selected configuration" );

        ImageIcon restoreImage = new ImageIcon(
            ImageHolder.class.getResource( "accept.gif" ) );
        RestoreAction restoreAction = new RestoreAction( "Restore",
                                                         restoreImage );
        JButton restoreButton = new JButton( restoreAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( restoreButton );
        restoreButton.setToolTipText
            ( "Restore the selected configuration" );

        ImageIcon deleteImage = new ImageIcon(
            ImageHolder.class.getResource( "delete.gif" ) );
        DeleteAction deleteAction = new DeleteAction( "Delete", deleteImage );
        JButton deleteButton = new JButton( deleteAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( deleteButton );
        deleteButton.setToolTipText
            ( "Delete the selected configurations" );

        // Close the window, saving the full configuration.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window and save configurations" );

        actionBar.add( Box.createGlue() );
    }


    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( "Save or restore configurations" );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent evt ) {
                    closeWindowEvent();
                }
            });
        contentPane.add( statusView, BorderLayout.CENTER );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 600, 300 ) );
        setVisible( true );
    }

    /**
     * Initialise the user interface. This is the action bar and the
     * status view.
     */
    protected void initUI()
    {
        contentPane.setLayout( new BorderLayout() );

        //  Create the store. This updates itself from the backing
        //  file.
        store = new StoreConfiguration( storeSource.getApplicationName(),
                                        storeSource.getStoreName() );

        //  The status view is a JTable with a model of the content of
        //  the XML file (this also provides the interaction with the
        //  XML file).
        statusTable.setModel( store );
        statusView.setBorder( BorderFactory.createTitledBorder
                   ( "Previous configurations in store" ) );
        statusView.setLayout( new BorderLayout() );
        JScrollPane scrollPane = new JScrollPane( statusTable );
        statusView.add( scrollPane, BorderLayout.CENTER );
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();

        //  Get the configuration to write itself out to backing
        //  store.
        store.writeToBackingStore();
    }

    /**
     * Create a new configuration entry and store the current
     * configuration in it.
     */
    public void storeCurrentConfiguration()
    {
        Element newRoot = store.newState( storeSource.getTagName(), 
                                          "New configuration" );
        storeSource.saveState( newRoot );
        store.stateCompleted( newRoot );
    }

    /**
     * Update the current configuration.
     */
    public void updateCurrentConfiguration()
    {
        int[] rows = statusTable.getSelectedRows();
        if ( rows.length > 0 ) {
            Element newRoot = store.reGetState( rows[0] );
            storeSource.saveState( newRoot );
            store.setDateStamp( rows[0] );
        }
    }

    /**
     * Delete all the selected configurations, or none if none are
     * selected.
     */
    public void deleteSelectedConfigurations()
    {
        int[] rows = statusTable.getSelectedRows();
        if ( rows.length > 0 ) {
            for ( int i = rows.length - 1; i >=0; i-- ) {
                store.removeState( i );
            }
        }
    }

    /**
     * Restore the selected configuration, if any.
     */
    public void restoreSelectedConfiguration()
    {
        int selected = statusTable.getSelectedRow();
        if ( selected >= 0 ) {
            Element state = store.getState( selected );
            storeSource.restoreState( state );
        }
        else {
            JOptionPane.showMessageDialog
                ( this,
                  "You have not selected a configuration to restore",
                  "Not restored" ,
                  JOptionPane.INFORMATION_MESSAGE );
            return;

        }
    }

    /**
     * Inner class defining action to create a new configuration entry
     * adding the current configuration to it.
     */
    protected class AddAction extends AbstractAction
    {
        public AddAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            storeCurrentConfiguration();
        }
    }

    /**
     * Inner class defining action to update a configuration entry.
     */
    protected class UpdateAction extends AbstractAction
    {
        public UpdateAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            updateCurrentConfiguration();
        }
    }

    /**
     * Inner class defining action to delete the selected
     * configuration entries.
     */
    protected class DeleteAction extends AbstractAction
    {
        public DeleteAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            deleteSelectedConfigurations();
        }
    }

    /**
     * Inner class defining Action for closing window and keeping fit.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining Action for closing window and keeping fit.
     */
    protected class RestoreAction extends AbstractAction
    {
        public RestoreAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            restoreSelectedConfiguration();
        }
    }
}
