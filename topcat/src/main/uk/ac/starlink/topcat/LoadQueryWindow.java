package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.SQLReadDialog;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Dialogue for user to enter a new table location for loading.
 * The dialogue can be invoked using the {@link #loadAnyStarTable}
 * or {@link #loadRandomStarTable} methods.
 * This dialogue is not modal; if and when the user specifies a valid
 * table location, that table will be instantiated and the 
 * {@link #performLoading} method will be called on it at that time.
 * <p>
 * If one of the <tt>get*StarTable</tt> methods is called, before another
 * one is completed (before the user has specified a table), the other
 * one will be cancelled.  This is reasonable behaviour - a user who is
 * trying to load two tables at once in two separate dialogues has 
 * probably forgotten all about the first one.
 *
 * @author   Mark Taylor (Starlink)
 */
public class LoadQueryWindow extends QueryWindow {

    private boolean requireRandom;
    private JTextField locField;
    private StarTableFactory tableFactory;
    private JFileChooser fileChooser;
    private SQLReadDialog sqlDialog;

    /**
     * Creates a new LoadQueryWindow.  No window is displayed at 
     * construction time.
     *
     * @param  tableFactory  the factory to be used for constructing tables
     */
    public LoadQueryWindow( StarTableFactory tableFactory ) {
        super( "Load new table", null );
        this.tableFactory = tableFactory;

        /* Place the field for entering the location. */
        locField = new JTextField( 24 );
        getStack().addLine( "Location", locField );

        /* Define the actions for starting other dialogues. */
        Action browseAction = new AbstractAction( "Browse Files" ) {
            public void actionPerformed( ActionEvent evt ) {
                fileDialog();
            }
        };
        Action jdbcAction = new AbstractAction( "SQL Database" ) {
            public void actionPerformed( ActionEvent evt ) {
                jdbcDialog();
            }
        };

        /* Deactivate the JDBC action if no JDBC drivers are installed. */
        if ( ! DriverManager.getDrivers().hasMoreElements() ) {
            jdbcAction.setEnabled( false );
            jdbcAction.putValue( Action.SHORT_DESCRIPTION, 
                                 "No JDBC drivers installed" );
        }

        /* Place the buttons for the other dialogues. */
        JPanel controls = getAuxControlPanel();
        controls.add( new JButton( browseAction ) );
        controls.add( new JButton( jdbcAction ) );

        /* Configure drag'n'drop operation. */
        TransferHandler th = new LoadWindowTransferHandler();
        ((JComponent) getContentPane()).setTransferHandler( th );

        /* Add a help button. */
        addHelp( "Read" );
        pack();
    }

    /**
     * Loads a StarTable as directed by the user.
     * Pops up a dialogue which invites the user to enter a table location.
     * When a valid location is entered, {@link #performLoading} is 
     * called on it.
     *
     * @param  parent  parent component, used for positioning
     */
    public synchronized void loadAnyStarTable( Component parent ) {
        loadStarTable( false, parent );
    }

    /**
     * Loads a random-access StarTable as directed by the user.
     * Pops up a dialogue which invites the user to enter a table location.
     * When a valid location is entered a random-access table based on 
     * the given location is constructed, and {@link #performLoading} is
     * called on it.
     *
     * @param  parent  parent component, used for positioning
     */
    public synchronized void loadRandomStarTable( Component parent ) {
        loadStarTable( true, parent );
    }

    /**
     * This method is called on a successfully loaded table when it has
     * been obtained.  The default behaviour is to instantiate a new 
     * TableViewer instance to view it.  The method may be overridden
     * by subclasses to change the behaviour.
     *
     * @param  startab  a newly-instanticated StarTable as specified by
     *         the user
     */
    protected void performLoading( StarTable startab ) {
        new TableViewer( startab, this );
    }

    /**
     * Loads a StarTable as directed by the user, optionally a random-access
     * one.
     * Pops up a dialogue which invites the user to enter a table.
     * When a valid location is entered, a table is constructed based on
     * the location specified by the user, if <tt>requireRandom</tt> is
     * set it will be turned into a random-access table, and 
     * {@link #performLoading} is called on it.
     *
     * @param   requireRandom  whether it must be made random prior to loading
     * @param  parent  parent component, used for positioning
     */
    private synchronized void loadStarTable( boolean requireRandom,
                                             Component parent ) {
        setAuthenticatorParent( this );
        this.requireRandom = requireRandom;
        positionAfter( parent, this );
        makeVisible();
    }

    protected boolean perform() {
        return submitLocation( locField.getText() );
    }

    /**
     * Called when a location string has been obtained from the user.
     * An attempt is made to turn it into a StarTable and to call 
     * the {@link #performLoading} method on it.
     * If the loading is successful, this window will dispose of itself.
     *
     * @param  loc  the location of the table to attempt to open
     * @return  true iff the table was successfully loaded
     */
    private boolean submitLocation( String loc ) {

        /* If it's blank, refuse to accept it. */
        if ( loc.trim().length() == 0 ) {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }

        /* Turn the location string into a StarTable. */
        StarTable st;
        try {
            st = tableFactory.makeStarTable( loc );
        }
        catch ( Exception e ) {
            ErrorDialog.showError( e, "Can't make table " + loc, this );
            return false;
        }

        /* Perform any required pre-processing. */
        st = doctorTable( st );
        if ( st == null ) {
            return false;
        }
        
        /* Do whatever needs doing with the successfully created StarTable. */
        performLoading( st );

        /* We're finished. */
        dispose();
        return true;
    }

    /**
     * Performs any necesary pre-processing on a StarTable prior to the
     * performLoading step.
     *
     * @param  startab  input StarTable
     * @return  StarTable with required pre-processing done, or <tt>null</tt>
     *          if it couldn't be done
     */
    private StarTable doctorTable( StarTable startab ) {

        /* Turn it into a random-access table if necessary. */
        if ( requireRandom ) {
            try {
                return Tables.randomTable( startab );
            }
            catch ( IOException e ) {
                ErrorDialog.showError( e, "Can't randomise table", this );
                return null;
            }
        }
        else {
            return startab;
        }
    }

    /**
     * This method is invoked when the user hits the 'Browse files' button
     * in the loader dialogue.
     */
    private void fileDialog() {
        JFileChooser fc = getFileChooser();
        int result = fc.showDialog( this, "Open Table" );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fc.getSelectedFile();
            if ( file != null ) {
                submitLocation( file.toString() );
            }
        }
    }

    /**
     * This method is invoked when the user hits the 'SQL' button on the
     * loader dialogue.
     */
    private void jdbcDialog() {
        SQLReadDialog sqld = getSQLDialog();
        setAuthenticatorParent( sqld );
        StarTable startab = sqld.readTableDialog( this );
        if ( startab != null ) {
            startab = doctorTable( startab );
        }
        if ( startab != null ) {
            performLoading( startab );
            dispose();
        }
    }

    /**
     * Returns the JFileChooser object used for file browsing.
     *
     * @return   a JFileChooser
     */
    public JFileChooser getFileChooser() {
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
            fileChooser.setCurrentDirectory( new File( "." ) );
        }
        return fileChooser;
    }

    /**
     * Returns the SQLReadDialog object used for loading JDBC tables.
     *
     * @return   a JDBC dialogue window
     */
    public SQLReadDialog getSQLDialog() {
        if ( sqlDialog == null ) {
            sqlDialog = new SQLReadDialog();
        }
        return sqlDialog;
    }

    /**
     * Ensure that any SQL authentication done by the StarTableFactory
     * is done graphically, and positioned relative to a given component.
     *
     * @param   parent component, which may be used for positioning
     *          authentication dialogues
     */
    public void setAuthenticatorParent( Component parent ) {
        JDBCHandler jh = tableFactory.getJDBCHandler();
        JDBCAuthenticator auth = jh.getAuthenticator();
        SwingAuthenticator guiAuth;
        if ( auth instanceof SwingAuthenticator ) {
            guiAuth = (SwingAuthenticator) auth;
        }
        else {
            guiAuth = new SwingAuthenticator();
            jh.setAuthenticator( guiAuth );
        }
        guiAuth.setParentComponent( parent );
    }

    /**
     * Transfer handler for this window, which will treat a drop of 
     * a suitable dragged object as equivalent to typing something in
     * the dialog box.
     */
    private class LoadWindowTransferHandler extends TransferHandler {

         public boolean canImport( JComponent comp, DataFlavor[] flavors ) {
             return tableFactory.canImport( flavors );
         }

         public boolean importData( JComponent comp, Transferable trans ) {
             StarTable table = tableFactory.makeStarTable( trans );
             if ( table == null ) {
                 return false;
             }
             else {

                 /* Perform any required pre-processing. */
                 table = doctorTable( table );
                 if ( table == null ) {
                     return false;
                 }

                 /* Do whatever needs doing with the successfully created
                  * StarTable. */
                 performLoading( table );

                 /* We're finished. */
                 dispose();
                 return true;
             }
         }

         public int getSourceActions( JComponent comp ) {
             return NONE;
         }
    }

}
