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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.ErrorDialog;
import uk.ac.starlink.util.Loader;

/**
 * Window which permits the user to select an existing {@link StarTable}
 * from file browsers or other external sources.
 * The most straightforward way to use this is to invoke the
 * {@link #showTableDialog} method.  
 *
 * <p>As well as a text field in which the user may type the location of a
 * table, a number of buttons are offered which pop up additional dialogues,
 * for instance a normal file browser and a dialogue for posing an 
 * SQL query.  This list is extensible at run time; if you wish to 
 * provide an additional table acquisition dialogue, then you must 
 * provide an implementation of the {@link TableLoadDialog} interface.
 * This can be made known to the chooser either by subclassing 
 * {@link #getDialogs} or by specifying the class name as the value
 * of the system property with the name {@link #LOAD_DIALOGS_PROPERTY}
 * (multiple classnames may be separated by colons).
 * In the latter case the implementing class(es) must have a 
 * no-arg constructor.
 *
 * <p>If you want to make more customised use of this component than is
 * offered by <tt>showTableDialog</tt> it is possible, but these javadocs
 * don't go out of their way to explain how.  Take a look at the 
 * implementation of <tt>showTableDialog</tt>.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    26 Nov 2004
 */
public class StarTableChooser extends JPanel {

    private final JTextField locField_;
    private final Action locAction_;
    private final Component[] activeComponents_;
    private StarTableFactory tableFactory_;
    private ComboBoxModel formatModel_;
    private TransferHandler transferHandler_;
    private Icon queryIcon_;
    private TableLoadDialog[] dialogs_;
    private TableConsumer tableConsumer_;

    /**
     * Name of the system property which can be used to specify the class
     * names of additional {@link TableLoadDialog} implementations.  
     * Each must have a no-arg constructor.  Multiple classnames should be
     * separated by colons.
     */
    public static final String LOAD_DIALOGS_PROPERTY = "startable.load.dialogs";

    /**
     * Constructs a new chooser window with a default table factory.
     */
    public StarTableChooser() {
        this( new StarTableFactory() );
        SwingAuthenticator auth = new SwingAuthenticator();
        auth.setParentComponent( this );
        tableFactory_.getJDBCHandler().setAuthenticator( auth );
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

        /* Prepare a list of components which can be enabled/disabled. */
        List activeList = new ArrayList();
        
        /* Construct and place format selector. */
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Format: " ) );
        formatBox.add( Box.createHorizontalStrut( 5 ) );
        JComboBox fcombo = new JComboBox( formatModel_ );
        fcombo.setMaximumSize( fcombo.getPreferredSize() );
        formatBox.add( fcombo );
        activeList.add( fcombo );
        formatBox.setAlignmentX( LEFT_ALIGNMENT );
        actionBox.add( formatBox );

        /* Construct and place location text entry field. */
        locAction_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                submitLocation( locField_.getText() );
            }
        };
        JComponent locBox = Box.createHorizontalBox();
        locField_ = new JTextField( 25 );
        locField_.addActionListener( locAction_ );
        locBox.add( new JLabel( "Location: " ) );
        locBox.add( Box.createHorizontalStrut( 5 ) );
        locBox.add( locField_ );
        activeList.add( locField_ );
        Dimension locSize = locBox.getPreferredSize();
        locSize.width = 1024;
        locBox.setMaximumSize( locSize );
        locBox.setAlignmentX( LEFT_ALIGNMENT );
        locBox.add( Box.createHorizontalStrut( 5 ) );
        locBox.add( new JButton( locAction_ ) );
        actionBox.add( Box.createVerticalStrut( 5 ) );
        actionBox.add( locBox );
        
        /* Create buttons for each of the pluggable dialog options. */
        TableLoadDialog[] options = getDialogs();
        int nopt = options.length;
        JButton[] buttons = new JButton[ options.length ];
        for ( int i = 0; i < nopt; i++ ) {
            buttons[ i ] = new JButton( makeAction( options[ i ] ) );
            if ( buttons[ i ].isEnabled() ) {
                activeList.add( buttons[ i ] );
            }
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

        /* Store list of disablable components. */
        activeComponents_ = (Component[]) 
                            activeList.toArray( new Component[ 0 ] );
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
    public StarTable showTableDialog( Component parent ) {

        /* Create the dialogue. */
        final JProgressBar progBar = new JProgressBar();
        final JDialog dialog = createDialog( parent, progBar );

        /* Create and install a table consumer which can return 
         * the loaded table. */
        final StarTable[] result = new StarTable[ 1 ];
        final BasicTableConsumer tc = new BasicTableConsumer( dialog ) {
            protected void setLoading( boolean isLoading ) {
                super.setLoading( isLoading );
                setEnabled( ! isLoading );
                progBar.setIndeterminate( isLoading );
            }
            protected void tableLoaded( StarTable table ) {
                assert table != null;
                result[ 0 ] = table;
                dialog.dispose();
            }
        };
        setTableConsumer( tc );

        /* Ensure that if the dialogue is closed for any reason (this may
         * happen as the result of the user hitting the Cancel button)
         * then the table consumer stops listening. */
        dialog.addWindowListener( new WindowAdapter() {
            public void windowClosed( WindowEvent evt ) {
                tc.cancel();
            }
        } );

        /* Pop up the dialogue.  Since it is modal, this will block until
         * (a) tableLoaded is called on the TableConsumer above because
         * a successful table load has completed or (b) the dialog is
         * disposed with a cancel or close action of some kind. */
        setEnabled( true );
        dialog.show();
        return result[ 0 ];
    }

    /**
     * Sets the object which does something with tables that the user
     * selects to load.
     *
     * @param   eater  table consumer
     */
    public void setTableConsumer( TableConsumer eater ) {
        tableConsumer_ = eater;
    }

    /**
     * Returns the object which does something with tables that the user
     * selects to load.
     *
     * @return  table consumer
     */
    public TableConsumer getTableConsumer() {
        return tableConsumer_;
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

    public void setEnabled( boolean isEnabled ) {
        if ( isEnabled != isEnabled() ) {
            for ( int i = 0; i < activeComponents_.length; i++ ) {
                activeComponents_[ i ].setEnabled( isEnabled );
            }
            locAction_.setEnabled( isEnabled );
        }
        super.setEnabled( isEnabled );
    }

    /**
     * Constructs a modal dialogue containing this window which can
     * be presented to the user.
     *
     * @param   parent   parent window
     * @param   progBar  progress bar used to indicate load progress
     */
    public JDialog createDialog( Component parent, JProgressBar progBar ) {

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
        Container pane = new JPanel( new BorderLayout() );
        dialog.getContentPane().setLayout( new BorderLayout() );
        dialog.getContentPane().add( pane, BorderLayout.CENTER );
        dialog.getContentPane().add( progBar, BorderLayout.SOUTH );

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
     * Constructs and returns a new action suitable for invoking a
     * TableLoadDialog within this chooser.  This is called when constructing
     * the buttons for display.
     *
     * @param  tld   loader dialogue supplier
     * @return   action which calls the <tt>showLoadDialog</tt> method of
     *           <tt>tld</tt>
     */
    public Action makeAction( final TableLoadDialog tld ) {
        Action act = new AbstractAction( tld.getName() ) {
            public void actionPerformed( ActionEvent evt ) {
                boolean status =
                    tld.showLoadDialog( StarTableChooser.this, tableFactory_,
                                        formatModel_, getTableConsumer() );
            }
        };
        act.putValue( Action.SHORT_DESCRIPTION, tld.getDescription() );
        act.setEnabled( tld.isEnabled() );
        return act;   
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
     * Returns the file chooser widget used by this chooser.
     * This could possibly be null if you've mucked around with the default
     * list of <tt>TableLoadDialog</tt>s (subclassed {@link #getDialogs}).
     *
     * @return  file chooser
     */
    public JFileChooser getFileChooser() {
        TableLoadDialog[] dialogs = getDialogs();
        for ( int i = 0; i < dialogs.length; i++ ) {
            if ( dialogs[ i ] instanceof FileChooserLoader ) {
                return (JFileChooser) dialogs[ i ];
            }
        }
        return null;
    }

    /**
     * Attempts to make and select a table from a location string.
     *
     * @param  location
     */
    public void submitLocation( final String location ) {
        final StarTableFactory factory = tableFactory_;
        final String format = getFormatName();
        new LoadWorker( getTableConsumer(), location ) {
            public StarTable attemptLoad() throws IOException {
                return factory.makeStarTable( location, format );
            }
        }.invoke();
    }

    /**
     * Returns the action used when the location text is submitted.
     *
     * @return  action
     */
    public Action getSubmitAction() {
        return locAction_;
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
     * Return an icon used to indicate a query dialogue.
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

         public boolean importData( JComponent comp, 
                                    final Transferable trans ) {
             final StarTableFactory factory = tableFactory_;
             TableConsumer eater = getTableConsumer();

             /* The table has to be loaded in line here, i.e. on the event
              * dispatch thread, since otherwise the weird IPC magic
              * which provides the inputstream from the Transferable
              * will go away.  This is unfortunate, since it might be
              * slow, but I don't *think* there's any alternative. */
             eater.loadStarted( "Dropped Table" );
             try {
                 eater.loadSucceeded( factory.makeStarTable( trans ) );
                 return true;
             }
             catch ( Throwable th ) {
                 eater.loadFailed( th );
                 return false;
             }
        }

        public int getSourceActions( JComponent comp ) {
            return NONE;
        }
    }
}
