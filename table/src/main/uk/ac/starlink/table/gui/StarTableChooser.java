package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.ErrorDialog;
import uk.ac.starlink.util.Loader;

/**
 * Window which permits the user to select an existing {@link StarTable}
 * from file browsers or other external sources.
 * The most convenient way to use it is via the 
 * {@link #getTable} method which pops up a modal
 * dialogue and return any table selected.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    26 Nov 2004
 */
public class StarTableChooser extends JPanel {

    private final JTextField locField_;
    private StarTableFactory tableFactory_;
    private ComboBoxModel formatModel_;
    private TransferHandler transferHandler_;
    private JDialog dialog_;
    private StarTable selectedTable_;
    private Icon queryIcon_;
    private TableLoadDialog[] dialogs_;

    public static final String LOAD_DIALOGS_PROPERTY = "startable.load.dialogs";

    /**
     * Constructs a new chooser window with a default table factory.
     */
    public StarTableChooser() {
        this( new StarTableFactory() );
    }

    /**
     * Constructs a new chooser window with a specified table factory.
     *
     * @param  factory  factory to use for creating tables
     */
    public StarTableChooser( StarTableFactory factory ) {
        tableFactory_ = factory;
        formatModel_ = makeFormatBoxModel( factory );
        Border emptyBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Box actionBox = Box.createVerticalBox();
        actionBox.setBorder( emptyBorder );
        setLayout( new BorderLayout() );
        add( actionBox, BorderLayout.CENTER );
        
        /* Construct and place format selector. */
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Format: " ) );
        formatBox.add( Box.createHorizontalStrut( 5 ) );
        JComboBox fcombo = new JComboBox( formatModel_ );
        fcombo.setMaximumSize( fcombo.getPreferredSize() );
        formatBox.add( fcombo );
        formatBox.setAlignmentX( LEFT_ALIGNMENT );
        actionBox.add( formatBox );

        /* Construct and place location text entry field. */
        Action locAction = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                submitLocation( locField_.getText() );
            }
        };
        JComponent locBox = Box.createHorizontalBox();
        locField_ = new JTextField( 25 );
        locField_.addActionListener( locAction );
        locBox.add( new JLabel( "Location: " ) );
        locBox.add( Box.createHorizontalStrut( 5 ) );
        locBox.add( locField_ );
        Dimension locSize = locBox.getPreferredSize();
        locSize.width = 1024;
        locBox.setMaximumSize( locSize );
        locBox.setAlignmentX( LEFT_ALIGNMENT );
        locBox.add( Box.createHorizontalStrut( 5 ) );
        locBox.add( new JButton( locAction ) );
        actionBox.add( Box.createVerticalStrut( 5 ) );
        actionBox.add( locBox );
        
        /* Create buttons for each of the pluggable dialog options. */
        TableLoadDialog[] options = getDialogs();
        int nopt = options.length;
        JButton[] buttons = new JButton[ options.length ];
        for ( int i = 0; i < nopt; i++ ) {
            final TableLoadDialog tld = options[ i ];
            Action act = new AbstractAction( tld.getName() ) {
                public void actionPerformed( ActionEvent evt ) {
                    StarTable table = 
                        tld.loadTableDialog( StarTableChooser.this, 
                                             tableFactory_, formatModel_ );
                        setSelectedTable( table );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION, tld.getDescription() );
            act.setEnabled( tld.isEnabled() );
            buttons[ i ] = new JButton( act );
        }

        /* Position buttons. */
        int buttw = 0;
        for ( int i = 0; i < nopt; i++ ) {
            buttw = Math.max( buttw, buttons[ i ].getPreferredSize().width );
        }
        JComponent dialogBox = Box.createVerticalBox();
        for ( int i = 0; i < nopt; i++ ) {
            Dimension max = buttons[ i ].getMaximumSize();
            max.width = buttw;
            buttons[ i ].setMaximumSize( max );
            if ( i > 0 ) {
                dialogBox.add( Box.createVerticalStrut( 5 ) );
            }
            dialogBox.add( buttons[ i ] );
        }
        JPanel dialogLine = 
            new JPanel( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
        dialogLine.add( dialogBox );
     
        dialogLine.setAlignmentX( LEFT_ALIGNMENT );
        actionBox.add( Box.createVerticalStrut( 10 ) );
        actionBox.add( dialogLine );

        /* Configure drag'n'drop operation. */
        transferHandler_ = new LoadTransferHandler();
        setTransferHandler( transferHandler_ );
    }

    /**
     * Pops up a modal dialogue which invites the user to select a table.
     * If the user selects a valid <tt>StarTable</tt> it is returned,
     * if he declines, then <tt>null</tt> will be returned.
     * The user will be informed of any errors and asked to reconsider
     * (so this method should not normally be invoked in a loop).
     *
     * @param  parent  the parent component, used for window positioning etc
     * @return  a selected table, or <tt>null</tt>
     */
    public StarTable getTable( Component parent ) {
        dialog_ = createDialog( parent );
        selectedTable_ = null;
        dialog_.show();
        dialog_ = null;
        return selectedTable_;
    }

    /**
     * Returns the factory object which this chooser
     * uses to resolve files into <tt>StarTable</tt>s.
     *
     * @return  the factory
     */
    public StarTableFactory getStarTableFactory() {
        return tableFactory_;
    }

    /**
     * Sets the factory object which this chooser
     * uses to resove files into <tt>StarTable</tt>s.
     *
     * @param  the factory
     */
    public void setStarTableFactory( StarTableFactory factory ) {
        tableFactory_ = factory;
    }

    /**
     * Returns the format selected with which to interpret the table.
     *
     * @return  the selected format name (or <tt>null</tt>)
     */
    public String getFormatName() {
        return (String) formatModel_.getSelectedItem();
    }

    /**
     * This method is invoked by the various table selection components
     * when the user has successfully selected a table. 
     *
     * @param  table  table selected by the user
     */
    private void setSelectedTable( StarTable table ) {
        selectedTable_ = table;
        if ( dialog_ != null ) {
            dialog_.dispose();
        }
    }

    /**
     * Constructs a modal dialogue containing this window which can
     * be presented to the user.  This method is called by {@link #getTable}.
     *
     * @param   parent   parent window
     */
    public JDialog createDialog( Component parent ) {

        /* Locate parent's frame. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame 
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }

        /* Create a new dialogue. */
        final JDialog dialog = new JDialog( frame, "Load Table", true );
        dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        Container pane = dialog.getContentPane();

        /* Place this component. */
        pane.setLayout( new BorderLayout() );
        pane.add( this, BorderLayout.CENTER );

        /* Place a little icon. */
        Box iconBox = Box.createVerticalBox();
        iconBox.add( Box.createVerticalGlue() );
        iconBox.add( new JLabel( getQueryIcon() ) );
        iconBox.add( Box.createVerticalGlue() );
        iconBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane.add( iconBox, BorderLayout.WEST );

        /* Place a cancel button. */
        Action cancelAction = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                dialog.dispose();
            }
        };
        Box cancelBox = Box.createHorizontalBox();
        cancelBox.add( Box.createHorizontalGlue() );
        cancelBox.add( new JButton( cancelAction ) );
        cancelBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane.add( cancelBox, BorderLayout.SOUTH );

        /* Position. */
        dialog.pack();
        dialog.setLocationRelativeTo( parent );
        return dialog;
    }

    /**
     * Returns a list of all the known dialogs which can be used to 
     * load a table.  Each of these will be represented by a button
     * in the chooser window.  Subclasses can override this to 
     * provide their own.
     *
     * @return   options for loading tables
     */
    public TableLoadDialog[] getDialogs() {
        if ( dialogs_ == null ) {
            List dlist = new ArrayList( Arrays.asList( new TableLoadDialog[] {
                new FileChooserLoader(),
                new NodeLoader(),
                new SQLReadDialog(),
            } ) );
            dlist.addAll( Loader.getClassInstances( LOAD_DIALOGS_PROPERTY, 
                                                    TableLoadDialog.class ) );
            dialogs_ = (TableLoadDialog[]) 
                       dlist.toArray( new TableLoadDialog[ 0 ] );
        }
        return dialogs_;
    }

    /**
     * Returns a transfer handler which will accept a table dropped on it
     * as a selected one.  This handler is installed by default on this
     * window.
     *
     * @param  table drop target transfer handler
     */
    public TransferHandler getTableImportTransferHandler() {
        return transferHandler_;
    }

    /**
     * Attempts to make and select a table from a location string.
     *
     * @param  location
     */
    private void submitLocation( String location ) {
        StarTable table =
            attemptMakeTable( this, tableFactory_, getFormatName(), location );
        if ( table != null ) {
            setSelectedTable( table );
        }
    }

    /**
     * Convenience method which attempts to construct a table from 
     * a location string.  In the event of an error, the user will be
     * politely informed.
     *
     * @param  parent  parent component
     * @param  factory  table factory
     * @param  format   name of table format
     * @param  location   location of table
     */
    public static StarTable attemptMakeTable( Component parent,
                                              StarTableFactory factory,
                                              String format,
                                              String location ) {
        try {
            return factory.makeStarTable( location, format );
        }
        catch ( IOException e ) {
            String[] msg = new String[] { "Failed to load table \"" 
                                          + location + "\"",
                                          e.getMessage() };
            JOptionPane.showMessageDialog( parent, msg, "Table Load Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
    }

    /**
     * Convenience method which attempts to construct a table from 
     * a <tt>DataSource</tt>.  In the event of an error, the user will be
     * politely informed.
     *
     * @param  parent  parent component
     * @param  factory  table factory
     * @param  format   name of table format
     * @param  datsrc   location of table
     */
    public static StarTable attemptMakeTable( Component parent,
                                              StarTableFactory factory,
                                              String format,
                                              DataSource datsrc ) {
        try {
            return factory.makeStarTable( datsrc, format );
        }
        catch ( IOException e ) {
            String[] msg = new String[] { "Failed to load table \"" 
                                          + datsrc.getName() + "\"",
                                          e.getMessage() };
            JOptionPane.showMessageDialog( parent, msg, "Table Load Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
    }

    /**
     * Creates and returns a ComboBoxModel suitable for use in a JComboBox
     * which the user can use to choose the format of tables to be loaded.
     * Each element of the returned model is a String.
     *
     * @return   ComboBoxModel with entries for each of the known formats,
     *           as well as an AUTO option
     */
    public static ComboBoxModel makeFormatBoxModel( StarTableFactory factory ) {
        DefaultComboBoxModel fmodel = new DefaultComboBoxModel();
        fmodel.addElement( StarTableFactory.AUTO_HANDLER );
        for ( Iterator it = factory.getKnownBuilders().iterator();
              it.hasNext(); ) {
            TableBuilder handler = (TableBuilder) it.next();
            fmodel.addElement( handler.getFormatName() );
        }
        return fmodel;
    }

    /**
     * Lazily construct an icon by getting it from JOptionPane (there's
     * probably a better way?)
     *
     * @return icon
     */
    private Icon getQueryIcon() {
        if ( queryIcon_ == null ) {
            queryIcon_ = UIManager.getIcon( "OptionPane.questionIcon" );
        }
        return queryIcon_;
    }

    /**
     * Transfer handler for this window, which will treat a drop of
     * a suitable dragged object as equivalent to typing something in
     * the dialog box.
     */
    private class LoadTransferHandler extends TransferHandler {

         public boolean canImport( JComponent comp, DataFlavor[] flavors ) {
             return tableFactory_.canImport( flavors );
         }

         public boolean importData( JComponent comp, Transferable trans ) {

             /* Try to obtain a StarTable from the Transferable. */
             StarTable table;
             Component parent = StarTableChooser.this;
             try {
                 table = tableFactory_.makeStarTable( trans );
                 if ( table == null ) {
                     return false;
                 }
             }
             catch ( IOException e ) {
                 ErrorDialog.showError( e, "Error trying to accept " +
                                        "dropped table", parent );
                 return false;
             }
             catch ( OutOfMemoryError e ) {
                 ErrorDialog.showError( e, "Out of memory trying to accept " +
                                        "dropped table", parent );
                 return false;
             }

             /* Perform any required pre-processing. */
             if ( table == null ) {
                 return false;
             }

             /* Do whatever needs doing with the successfully created
              * StarTable. */
             setSelectedTable( table );
             return true;
        }

         public int getSourceActions( JComponent comp ) {
             return NONE;
         }
    }
}
