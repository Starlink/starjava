package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import uk.ac.starlink.connect.FilestoreChooser;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;

/**
 * Dialog which permits a user to save a {@link StarTable} in a place
 * and format of choice.  It should be able to provide suitable dialogs
 * for all the supported table types; in particular it includes a filestore
 * browser and special JDBC connection dialog.
 *
 * <p>The usual way to use this is to implement {@link #getTable} to
 * provide the table which will be saved, and then call 
 * {@link #showSaveDialog}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class TableSaveChooser extends JPanel {

    private final JComboBox formatSelector_;
    private final JComponent[] activeComponents_;
    private final FilestoreChooser filestoreChooser_;
    private StarTableOutput sto_;
    private JDialog dialog_;
    private JProgressBar progBar_;
    private SaveWorker worker_;

    /**
     * Constructs a saver with a default StarTableOutput.
     */
    public TableSaveChooser() {
        this( new StarTableOutput() );
    }

    /**
     * Constructs a saver with a given StarTableOutput.
     *
     * @param  sto  output marshaller
     */
    public TableSaveChooser( StarTableOutput sto ) {
        super( new BorderLayout() );
        Border emptyBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Box actionBox = Box.createVerticalBox();
        actionBox.setBorder( emptyBorder );
        add( actionBox, BorderLayout.CENTER );

        /* Prepare a list of components which can be enabled/disabled. */
        List activeList = new ArrayList();

        /* Construct and place format selector. */
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Output Format: " ) );
        formatSelector_ = new JComboBox() {
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        formatBox.add( formatSelector_ );
        activeList.add( formatSelector_ );
        formatBox.setAlignmentX( LEFT_ALIGNMENT );
        actionBox.add( formatBox );
        setTableOutput( sto );

        /* Construct and place location text entry field. */
        final JTextField locField = new JTextField( 25 );
        Action locAction = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                submitLocation( locField.getText() );
            }
        };
        JComponent locBox = Box.createHorizontalBox();
        locField.addActionListener( locAction );
        locBox.add( new JLabel( "Location: " ) );
        locBox.add( locField );
        activeList.add( locField );
        Dimension locSize = locBox.getPreferredSize();
        locSize.width = 1024;
        locBox.setMaximumSize( locSize );
        locBox.setAlignmentX( LEFT_ALIGNMENT );
        locBox.add( Box.createHorizontalStrut( 5 ) );
        JButton locButton = new JButton( locAction );
        locBox.add( locButton );
        activeList.add( locButton );
        actionBox.add( Box.createVerticalStrut( 5 ) );
        actionBox.add( locBox );

        /* Define actions for invoking sub-dialogues. */
        FilestoreTableSaveDialog filestoreDialog = 
            new FilestoreTableSaveDialog();
        filestoreChooser_ = filestoreDialog.getChooser();
        TableSaveDialog[] saverDialogs = new TableSaveDialog[] {
              filestoreDialog,
              new SQLWriteDialog(),
        };
        List buttList = new ArrayList();
        for ( int i = 0; i < saverDialogs.length; i++ ) {
            final TableSaveDialog tsd = saverDialogs[ i ];
            Action saverAction = new AbstractAction( tsd.getName() ) {
                public void actionPerformed( ActionEvent evt ) {
                    if ( tsd.showSaveDialog( TableSaveChooser.this,
                                             getTableOutput(),
                                             formatSelector_.getModel(),
                                             getTable() ) ) {
                        done();
                    }
                }
            };
            saverAction.putValue( Action.SHORT_DESCRIPTION,
                                  tsd.getDescription() );
            JButton butt = new JButton( saverAction );
            saverAction.setEnabled( tsd.isAvailable() );
            if ( saverAction.isEnabled() ) {  
                activeList.add( butt );
            }
            buttList.add( butt );
        }
        JButton[] buttons = (JButton[]) buttList.toArray( new JButton[ 0 ] );
        int nopt = buttons.length;

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

        /* Store list of components that can be enabled/disabled. */
        activeComponents_ = (JComponent[])
                            activeList.toArray( new JComponent[ 0 ] );
    }

    /**
     * Sets the configuration of this saver up to match that of a
     * loader widget.  This will typically involve things like making
     * sure they are viewing the same directory.
     * 
     * @param  loader  loader
     */
    public void configureFromLoader( TableLoadChooser loader ) {
        FilestoreChooser saveChooser = getFilestoreChooser();
        FilestoreChooser loadChooser = loader.getFilestoreChooser();
        if ( saveChooser != null && loadChooser != null ) {
            saveChooser.setModel( loadChooser.getModel() );
        }
    }

    /**
     * Returns the FilestoreChooser, if any, used by this save dialogue.
     *
     * @return  chooser
     */
    FilestoreChooser getFilestoreChooser() {
        return filestoreChooser_;
    }

    /**
     * Sets the StarTableOutput object which this saver uses to do the
     * actual writing of the StarTables.
     * 
     * @param  sto  the new StarTableOutput to use
     */
    public void setTableOutput( StarTableOutput sto ) {
        sto_ = sto;
        formatSelector_.setModel( makeFormatBoxModel( sto ) );
    }

    /**
     * Returns the StarTableOutput object which this saver uses to
     * do the actual writing of the StarTables.
     *
     * @return sto  the StarTableOutput object
     */
    public StarTableOutput getTableOutput() {
        return sto_;
    }

    /**
     * Obtains the table to write.   This must be implemented by 
     * concrete subclasses.
     *
     * @return   table to write
     */
    public abstract StarTable getTable();

    /**
     * Called when the table has been written.
     */
    public void done() {
        if ( dialog_ != null ) {
            dialog_.dispose();
        }
        filestoreChooser_.refreshList();
    }

    /**
     * Cancels the current operation - either the user interaction or
     * the saving of a table.
     */
    private void cancel() {
        if ( worker_ != null ) {
            worker_.cancel();
            worker_ = null;
            setEnabled( true );
        }
        else if ( dialog_ != null ) {
            dialog_.dispose();
            dialog_ = null;
        }
    }

    /**
     * Pops up a modal dialogue which interacts with the user to save
     * the table returned by {@link #getTable}.
     *
     * @param  parent   parent component
     */
    public void showSaveDialog( Component parent ) {
        dialog_ = createDialog( parent );
        progBar_ = new JProgressBar();
        dialog_.getContentPane().add( progBar_, BorderLayout.SOUTH );
        setEnabled( true );
        dialog_.pack();
        dialog_.show();
        dialog_ = null;
        progBar_ = null;
    }

    public void setEnabled( boolean isEnabled ) {
        if ( isEnabled != isEnabled() ) {
            for ( int i = 0; i < activeComponents_.length; i++ ) {
                activeComponents_[ i ].setEnabled( isEnabled );
            }
        }
        super.setEnabled( isEnabled );
    }

    /**
     * Creates a dialogue which can be used for user interaction.
     *
     * @param  parent  parent component
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
        final JDialog dialog = new JDialog( frame, "Save Table", true );
        dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        Container pane = new JPanel( new BorderLayout() );
        dialog.getContentPane().setLayout( new BorderLayout() );
        dialog.getContentPane().add( pane, BorderLayout.CENTER );

        /* Place this component in it. */
        pane.setLayout( new BorderLayout() );
        pane.add( this, BorderLayout.CENTER );

        /* Place a little icon. */
        Box iconBox = Box.createVerticalBox();
        iconBox.add( Box.createVerticalGlue() );
        iconBox.add( new JLabel( UIManager
                                .getIcon( "OptionPane.questionIcon" ) ) );
        iconBox.add( Box.createVerticalGlue() );
        iconBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane.add( iconBox, BorderLayout.WEST );

        /* Place a cancel button. */
        Action cancelAction = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancel();
            }
        };
        Box cancelBox = Box.createHorizontalBox();
        cancelBox.add( Box.createHorizontalGlue() );
        cancelBox.add( new JButton( cancelAction ) );
        cancelBox.add( Box.createHorizontalGlue() );
        cancelBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane.add( cancelBox, BorderLayout.SOUTH );

        /* Position. */
        dialog.setLocationRelativeTo( parent );
        return dialog;
    }

    /**
     * Called when the user has entered a filename in the main text box,
     * indicating that this is the location under which to save it.
     *
     * @param   loc  save location
     */
    private void submitLocation( final String loc ) {
        final StarTableOutput sto = getTableOutput();
        final String format = (String) formatSelector_.getSelectedItem();
        worker_ = new SaveWorker( progBar_, getTable(), loc ) {
            protected void attemptSave( StarTable table ) throws IOException {
                sto.writeStarTable( table, loc, format );
            }
            protected void done( boolean success ) {
                if ( success ) {
                    TableSaveChooser.this.done();
                }
                worker_ = null;
                setEnabled( true );
            }
        };
        setEnabled( false );
        worker_.invoke();
    }

    /**
     * Constructs a ComboBoxModel containing an entry for each of the
     * known output formats.
     *
     * @return   format combo box model
     */
    private static ComboBoxModel makeFormatBoxModel( StarTableOutput sto ) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement( StarTableOutput.AUTO_HANDLER );
        for ( Iterator it = sto.getHandlers().iterator(); it.hasNext(); ) {
            StarTableWriter handler = (StarTableWriter) it.next();
            model.addElement( handler.getFormatName() );
        }
        return model;
    }

    /** Testing purposes only. */
    public static void main( String[] args ) {
        final int nrow = Integer.parseInt( args[ 0 ] );
        final uk.ac.starlink.table.ColumnStarTable tab =
            new uk.ac.starlink.table.ColumnStarTable() {
                public long getRowCount() {
                    return (long) nrow;
                }
            };
        tab.addColumn( 
            new uk.ac.starlink.table.ColumnData( 
                new uk.ac.starlink.table.ColumnInfo( "index", Integer.class,
                                                     null ) ) {
                    public Object readValue( long irow ) {
                        return new Integer( (int) ( irow + 1 ) );
                    }
                } );
        new TableSaveChooser() {
            public StarTable getTable() {
                return tab;
            }
         }.showSaveDialog( null );
         System.exit( 0 );
    }

}
