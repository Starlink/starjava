package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.DriverManager;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Dialog which permits a user to save a {@link StarTable} in a place
 * and format of choice.  It should be able to provide suitable dialogs
 * for all the supported table types; in particular it includes a file
 * browser and special JDBC connection dialog.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableSaver extends JOptionPane {

    private JTextField locField;
    private ComboBoxModel formatModel;
    private JFileChooser chooser;
    private StarTableOutput sto;
    private SQLWriteDialog sqlDialog;

    private static final String NO_FORMAT = new String( "(auto)" );
    private static final int JDBC_OPTION = 101;
    private static final int BROWSE_OPTION = 102;

    /**
     * Constructs a <tt>StarTableSaver</tt>.
     */
    public StarTableSaver() {

        /* Field for output table location. */
        locField = new JTextField( 32 );

        /* Field for output table format. */
        JComboBox formatField = new JComboBox();
        formatField.addItem( NO_FORMAT );
        for ( Iterator it = getStarTableOutput().getHandlers().iterator();
              it.hasNext(); ) {
            StarTableWriter handler = (StarTableWriter) it.next();
            formatField.addItem( handler.getFormatName() );
        }
        formatModel = formatField.getModel();

        /* Define actions for invoking other dialogs. */
        Action browseAction = new AbstractAction( "Browse Files" ) {
            public void actionPerformed( ActionEvent evt ) {
                setValue( new Integer( BROWSE_OPTION ) );
            }
        };
        Action jdbcAction = new AbstractAction( "SQL Table" ) {
            public void actionPerformed( ActionEvent evt ) {
                setValue( new Integer( JDBC_OPTION ) );
            }
        };

        /* Deactivate the JDBC action if no JDBC drivers are installed. */
        if ( ! DriverManager.getDrivers().hasMoreElements() ) {
            jdbcAction.setEnabled( false );
            jdbcAction.putValue( Action.SHORT_DESCRIPTION,
                                 "No JDBC drivers installed" );
        }

        /* Set up the panel for specifying name and format. */
        LabelledComponentStack locPanel = new LabelledComponentStack();
        locPanel.addLine( "Table output format", formatField );
        locPanel.addLine( "Output location", locField );

        /* Set up the panel for invoking other dialogs. */
        JPanel actionsPanel = new JPanel();
        actionsPanel.add( new JButton( browseAction ) );
        actionsPanel.add( new JButton( jdbcAction ) );

        /* Set up the input widgets for the dialog. */
        JPanel msg = new JPanel( new BorderLayout() );
        msg.add( locPanel, BorderLayout.CENTER );
        msg.add( actionsPanel, BorderLayout.SOUTH );

        /* JOptionPane configuration. */
        setMessage( msg );
        setOptionType( OK_CANCEL_OPTION );
    }

        
    /**
     * Returns the format selected in which to write the table.
     * <tt>null</tt> will be returned if no format has been selected.
     *
     * @return  the selected format (or <tt>null</tt>)
     */
    public String getFormat() {
        String fmt = (String) formatModel.getSelectedItem();
        return ( fmt == NO_FORMAT ) ? null : fmt;
    }

    /**
     * Sets the StarTableOutput object which this saver uses to do the
     * actual writing of the StarTables.
     *
     * @param  sto  the new StarTableOutput to use
     */
    public void setStarTableOutput( StarTableOutput sto ) {
        this.sto = sto;
    }

    /**
     * Returns the StarTableOutput object which this saver uses to 
     * do the actual writing of the StarTables.
     *
     * @return sto  the StarTableOutput object
     */
    public StarTableOutput getStarTableOutput() {
        if ( sto == null ) {
            sto = new StarTableOutput();
        }
        return sto;
    }

    /**
     * Saves a given StarTable, requesting relevant information from 
     * the user.
     *
     * @param  startab  the table to save
     * @param  parent   the parent component, used for positioning 
     *         dialog boxes
     */
    public void saveTable( StarTable startab, Component parent ) {
        JDialog dialog = createDialog( parent, "Save table" );
        boolean done = false;
        while ( ! done ) {
            dialog.show();
            Object selected = getValue();
            if ( selected instanceof Integer ) {
                switch ( ((Integer) selected).intValue() ) {
                    case JDBC_OPTION:
                        done = saveJDBCTable( startab, parent );
                        break;
                    case BROWSE_OPTION:
                        done = saveTableFromBrowser( startab, parent );
                        break;
                    case OK_OPTION:
                        String loc = locField.getText();
                        if ( loc != null && loc.trim().length() > 0 ) {
                            String format = getFormat();
                            File floc = new File( loc );
                            if ( ! floc.exists() ||
                                 confirmOverwrite( parent, floc ) ) {
                                saveTable( getStarTableOutput(), startab, loc,
                                           format, parent );
                                done = true;
                            }
                        }
                        break;
                    case CANCEL_OPTION:
                    default:
                        return;
                }
            }

            /* Window was closed. */
            else {
                return;
            }
        }
    }

    /**
     * Invokes the <tt>writeStarTable</tt> method of a <tt>StarTableOutput</tt>
     * in a graphical environment to save a table.
     * As well as simply calling the method, it ensures that any interaction
     * with the user (such as JDBC authentication) will be done in a
     * graphical fashion appropriately postiioned relative to a given
     * parent component.  If an error occurs during the save the user 
     * is informed with an error dialog.
     *
     * @param   sto  the object which handles the actual table writing
     * @param   startab  the table to write
     * @param   loc  the location to which the table should be written
     * @param   format  the format type for writing the table
     * @param   parent  the parent window (may be null)
     */
    public static void saveTable( final StarTableOutput sto, StarTable startab,
                                  final String loc, final String format, 
                                  Component parent ) {

        /* Configure any JDBC authentication to be GUI-based, and
         * positioned properly with respect to the parent. */
        JDBCHandler jh = sto.getJDBCHandler();
        SwingAuthenticator auth = new SwingAuthenticator();
        auth.setParentComponent( parent );
        jh.setAuthenticator( auth );

        /* Manipulate the StarTable so that it will message a progress bar. */
        JProgressBar progBar = new JProgressBar();
        final StarTable progtab = new ProgressBarStarTable( startab, progBar );

        /* Set up a progress window. */
        final JFrame progFrame = new JFrame( "Saving table" );
        Box box = new Box( BoxLayout.Y_AXIS );
        box.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        progFrame.getContentPane().add( box, BorderLayout.CENTER );
        box.add( new JLabel( "Writing table to " + loc ) );
        box.add( Box.createVerticalStrut( 5 ) );
        box.add( progBar );
        box.add( Box.createVerticalStrut( 5 ) );

        /* Set up a thread to do the writing. */
        final Thread saveThread = new Thread() {
            public void run() {
                try {
                    sto.writeStarTable( progtab, loc, format );
                    progFrame.dispose();
                }
                catch ( final Exception e ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            ErrorDialog
                           .showError( e, "Can't save table to " + loc,
                                       progFrame );
                            progFrame.dispose();
                        }
                    } );
                }
            }
        };

        /* Provide a means of cancelling the write midway through. */
        Action cancelAct = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                saveThread.interrupt();
            }
        };
        JButton cancelButt = new JButton( cancelAct );
        Box cancelBox = new Box( BoxLayout.X_AXIS );
        cancelBox.add( Box.createHorizontalGlue() );
        cancelBox.add( cancelButt );
        box.add( cancelBox );

        /* Reveal the progress window. */
        progFrame.setLocationRelativeTo( parent );
        progFrame.pack();
        progFrame.setVisible( true );

        /* Initiate the actual table save. */
        saveThread.start();
    }

    /**
     * Saves the StarTable in a position selected by the user by browsing
     * files in a filechooser.
     *
     * @param  startab  the table to save
     * @param  parent   the parent component used for positioning windows
     * @return  true iff the table was saved successfully
     */
    public boolean saveTableFromBrowser( StarTable startab, Component parent ) {
        JFileChooser chooser = getFileChooser();
        boolean done = false;
        while ( ! done && chooser.showSaveDialog( parent ) 
                          == JFileChooser.APPROVE_OPTION ) {
            String loc = chooser.getSelectedFile().toString();
            String format = getFormat();
            File floc = new File( loc );
            if ( ! floc.exists() || confirmOverwrite( parent, floc ) ) {
                saveTable( getStarTableOutput(), startab, loc, format, parent );
                done = true;
            }
        }
        return done;
    }

    /**
     * Returns the file chooser used by this saver for file browsing.
     */
    public JFileChooser getFileChooser() {
        if ( chooser == null ) {

            /* Create a new file chooser. */
            chooser = new JFileChooser();
            chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

            /* Add a format selector as an accessory. */
            JPanel acc = new JPanel();
            JLabel label = new JLabel( "Output format" );
            acc.add( label );
            acc.add( new JComboBox( formatModel ) );
            chooser.setAccessory( acc );

            /* Add file filters based on the available handlers. */
            for ( Iterator it = getStarTableOutput().getHandlers().iterator();
                  it.hasNext(); ) {
                StarTableWriter handler = (StarTableWriter) it.next();
                chooser.addChoosableFileFilter( 
                            new WriterFileFilter( handler ) );
            }
            chooser.setAcceptAllFileFilterUsed( true );
        }
        return chooser;
    }


    /**
     * Saves the StarTable over a JDBC connection following a specialised
     * JDBC dialog.
     *
     * @param  startab  the table to save
     * @param  parent   the parent component used for positioning windows
     * @return  true iff the table was saved successfully
     */
    public boolean saveJDBCTable( StarTable startab, Component parent ) {
        return getSQLWriteDialog().writeTableDialog( startab, parent );
    }

    /**
     * Returns the SQLWriteDialog object used for getting JDBC/SQL table specs
     * from the user.
     *
     * @return an SQLWriteDialog
     */
    public SQLWriteDialog getSQLWriteDialog() {
        if ( sqlDialog == null ) {
            sqlDialog = new SQLWriteDialog();
        }
        return sqlDialog;
    }

    /**
     * Sets the SQLWriteDialog object used for getting JDBC/SQL table specs
     * from the user.
     *
     * @param  sqlDialog  an SQLWriteDialog
     */
    public void setSQLWriteDialog( SQLWriteDialog sqlDialog ) {
        this.sqlDialog = sqlDialog;
    }

    /**
     * Requests confirmation from the user that an existing file can be
     * overwritten.
     *
     * @param  parent   the parent component, used for positioning 
     *         dialog boxes
     * @param  name of the file to overwrite
     * @return  <tt>true</tt> if the user agrees it's OK to overwrite
     */
    private boolean confirmOverwrite( Component parent, File loc ) {
        String[] msg = new String[] {
            "You have selected the output file " + loc + ".",
            "A file with this name already exists, " +
            "do you wish to overwrite it?",
            "",
            "IMPORTANT: in some cases, overwriting a file while it is being",
            "edited in TOPCAT can cause an error, losing the data completely.",
            "If in doubt, you are advised to save using a new name.",
        };
        Object cancelOption = "Cancel";
        Object overwriteOption = "Overwrite";
        Object[] options = new Object[] { cancelOption, overwriteOption };
        int result = JOptionPane
                    .showOptionDialog( parent, msg, "Confirm overwrite", 0,
                                       JOptionPane.WARNING_MESSAGE, null,
                                       options, cancelOption );
        return result == 1;
    }
}
