package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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

import org.jdom.Element;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Controller for saving, restoring and deleting plot configuration
 * data stored in XML files. The configurations are stored in a
 * permanent file (PlotConfigs.xml) which has each configuration
 * characterised by a description (created by the user) and a date
 * that the configuration was created (or maybe last updated).
 * <p>
 * An instance of this class should be associated with a
 * PlotConfigFrame object that acts as a view for the restored
 * configuration and a model for the current configuration. The actual
 * interaction with the XML store is performed by a PlotConfigStore
 * object.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see #PlotConfig, #PlotConfigFrame
 */
public class PlotConfigStoreFrame extends JFrame
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
     * Visible configuration object. Mediates to the actual stores.
     */
    protected PlotConfigFrame config = null;

    /**
     * Object that mediates to the actual store.
     */
    protected PlotConfigStore store = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu( "File" );

    /**
     * Create an instance.
     */
    public PlotConfigStoreFrame( PlotConfigFrame config )
    {
        this.config = config;
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

        //  Action bar uses a BoxLayout.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        
        //  Add actions to the action bar to create a new
        //  configuration (which is initialised to the current setup).

        ImageIcon addImage = new ImageIcon(
            ImageHolder.class.getResource( "add.gif" ) );
        AddAction addAction = new AddAction( "Add", addImage );
        JButton addButton = new JButton( addAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( addButton );
        addButton.setToolTipText
            ( "Add the current configuration as a new entry" );

        ImageIcon restoreImage = new ImageIcon(
            ImageHolder.class.getResource( "accept.gif" ) );
        RestoreAction restoreAction = new RestoreAction( "Restore",
                                                         restoreImage );
        JButton restoreButton = new JButton( restoreAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( restoreButton );
        restoreButton.setToolTipText
            ( "Restore the selected configuration to the plot" );

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
        fileMenu.add( closeAction );
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
        setTitle( Utilities.getTitle( "Save or Restore Plot Configurations" ) );
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
        store = new PlotConfigStore();

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
        Element newRoot = store.newState( "New configuration" );
        config.getConfig().encode( newRoot );
        store.stateCompleted( newRoot );
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
            config.getConfig().decode( state );
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
