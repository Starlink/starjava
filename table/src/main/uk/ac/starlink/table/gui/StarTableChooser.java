package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Dialog which permits selection of an existing {@link StarTable}.
 * It should be able to provide suitable dialogs for all the supported
 * table types; in particular it includes a file browser and 
 * special JDBC connection dialog.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableChooser extends JOptionPane {

    private StarTableFactory tabfact;
    private JFileChooser chooser;
    private SQLReadDialog sqlDialog;
    private JTextField locField;
    private JPanel actionsPanel;
    private ComboBoxModel formatModel;

    private static final int JDBC_OPTION = 101;
    private static final int BROWSE_OPTION = 102;

    /**
     * Constructs a <tt>StarTableChooser</tt>.
     */
    public StarTableChooser( StarTableFactory tabfact ) {
        this.tabfact = tabfact;

        /* Field for input table location. */
        locField = new JTextField( 32 );

        /* Field for input table format. */
        formatModel = makeFormatBoxModel( tabfact );
        JComboBox formatField = new JComboBox( formatModel );

        /* Set up the field for entering a table location. */
        LabelledComponentStack locPanel = new LabelledComponentStack();
        locPanel.addLine( "Table Format", formatField );
        locPanel.addLine( "Table Location", locField );

        /* Set up actions for invoking other dialogs. */
        Action browseAction = new AbstractAction( "Browse files" ) {
            public void actionPerformed( ActionEvent evt ) {
                setValue( new Integer( BROWSE_OPTION ) );
            }
        };
        Action jdbcAction = new AbstractAction( "JDBC table" ) {
            public void actionPerformed( ActionEvent evt ) {
                setValue( new Integer( JDBC_OPTION ) );
            }
        };

        /* Deactivate the JDBC action if there are no drivers installed. */
        if ( DriverManager.getDrivers().hasMoreElements() ) {
            jdbcAction.setEnabled( false );
            jdbcAction.putValue( Action.SHORT_DESCRIPTION,
                                 "No JDBC drivers installed" );
        }

        /* Set up the panel for invoking other dialogs. */
        actionsPanel = new JPanel();
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
     * Returns the factory object which this chooser
     * uses to resolve files into <tt>StarTable</tt>s.
     *
     * @return  the factory
     */
    public StarTableFactory getStarTableFactory() {
        return tabfact;
    }

    /**
     * Sets the factory object which this chooser
     * uses to resove files into <tt>StarTable</tt>s.
     *
     * @param  the factory
     */
    public void setStarTableFactory( StarTableFactory tabfact ) {
        this.tabfact = tabfact;
    }

    /**
     * Returns the format selected with which to interpret the table.
     * <tt>null</tt> will be returned if no format has been selected
     * (auto-detect mode).
     *
     * @return  the selected format name (or <tt>null</tt>)
     */
    public String getFormatName() {
        return (String) formatModel.getSelectedItem();
    }

    /**
     * Returns an existing <tt>StarTable</tt> object which has been
     * selected by the user.  In the event that the user declines to
     * select a valid <tt>StarTable</tt> then <tt>null</tt> is returned.
     * The user will be informed of any errors and asked to reconsider
     * (so it should not normally be invoked in a loop).
     *
     * @param  parent  the parent component, used for window positioning etc
     * @return  a new <tt>StarTable</tt> object as selected by the user,
     *          or <tt>null</tt>
     */
    public StarTable getTable( Component parent ) {
        JDialog dialog = createDialog( parent, "Open table" );
        StarTable startab = null;
        while ( startab == null ) {
            dialog.show();
            Object selected = getValue();
            if ( selected instanceof Integer ) {
                switch ( ((Integer) selected).intValue() ) {
                    case JDBC_OPTION:
                        startab = getJDBCTable( parent );
                        break;
                    case BROWSE_OPTION:
                        dialog.hide();
                        startab = getTableFromBrowser( parent );
                        break;
                    case OK_OPTION:
                        String loc = locField.getText();
                        if ( loc != null && loc.trim().length() > 0 ) {
                            startab = getTable( getStarTableFactory(),
                                                loc, getFormatName(), dialog );
                        }
                        break;
                    case CANCEL_OPTION:
                    default:
                        return null;
                }
            }

            /* Window was closed. */
            else {
                return null;
            }
        }
        return startab;
    }

    /**
     * Invokes {@link #getTable} but ensures that the returned table
     * provides random access.
     * 
     * @param  parent  the parent component, used for window positioning etc
     * @return  a new <tt>StarTable</tt> object as selected by the user
     *          for which {@link StarTable#isRandom} is true,
     *          or <tt>null</tt>.  
     * @see #getTable
     */
    public StarTable getRandomTable( Component parent ) {
        while ( true ) {
            StarTable st = getTable( parent );
            if ( st == null ) {
                return null;
            }
            else {
                try {
                    st = Tables.randomTable( st );
                    return st;
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( e, "Can't randomise table", parent );
                }
            }
        } 
    }

    /**
     * Invokes the <tt>makeStarTable</tt> method of a <tt>StarTableFactory</tt>
     * in a graphical environment to make a table.
     * As well as simply calling the method, it ensures that any interaction
     * with the user (such as JDBC authentication) will be done in a
     * graphical fashion appropriately positioned relative to a given
     * parent component.  If any error occurs in constructing the table
     * the user is informed with an error dialog, and <tt>null</tt> is
     * returned.
     *
     * @param  tabfact  the StarTableFactory to use
     * @param  loc      the location of the new table
     * @param  format   name of the table format, or null for auto-detect
     * @param  parent   the parent window (may be null)
     * @return  the new StarTable as selected by the user,
     *          or <tt>null</tt> if it couldn't be constructed
     */
    public static StarTable getTable( StarTableFactory tabfact, String loc,
                                      String format, Component parent ) {

        /* Configure any JDBC authentication to be GUI-based, and
         * positioned properly with respect to the parent. */
        JDBCHandler jh = tabfact.getJDBCHandler();
        JDBCAuthenticator oldAuth = jh.getAuthenticator();
        SwingAuthenticator auth = new SwingAuthenticator();
        auth.setParentComponent( parent );
        jh.setAuthenticator( auth );

        /* Get a table from the factory. */
        try {
            return tabfact.makeStarTable( loc, format );
        }

        /* In case of error, inform the user and return null. */
        catch ( Exception e ) {
            JOptionPane.showMessageDialog( parent, e.toString(),
                                           "Can't open table " + loc,
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }

        /* Restore the factory to its original state. */
        finally {
            jh.setAuthenticator( oldAuth );
        }
    }

    /**
     * Returns a StarTable from this chooser's file browser widget.
     * If the user declines to choose one, <tt>null</tt> is returned.
     *
     * @param  parent   the parent component, used for positioning
     * @return  a new StarTable selected by the user, or <tt>null</tt>
     */
    public StarTable getTableFromBrowser( Component parent ) {
        StarTable st = null;
        JFileChooser chooser = getFileChooser();
        while ( st == null && chooser.showOpenDialog( parent ) 
                              == JFileChooser.APPROVE_OPTION ) {
            String loc = chooser.getSelectedFile().toString();
            st = getTable( getStarTableFactory(), loc, getFormatName(),
                           parent );
        }
        return st; 
    }

    /**
     * Returns a container which can be used to hold custom controls.
     * this container already holds some control buttons
     * (the ones which cause auxiliary windows to be popped up).
     *
     * @return  container for buttons etc
     */
    public JPanel getActionPanel() {
        return actionsPanel;
    }

    /**
     * Returns the JFileChooser object used for file browsing.
     *
     * @return   a JFileChooser
     */
    public JFileChooser getFileChooser() {
        if ( chooser == null ) {

            /* Create a new file chooser. */
            chooser = new JFileChooser();
            chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

            /* Add a format selector as an accessory. */
            JComponent line = new JPanel();
            line.add( new JLabel( "Table Format" ) );
            line.add( new JComboBox( formatModel ) );
            JComponent acc = Box.createVerticalBox();
            acc.add( Box.createVerticalGlue() );
            acc.add( line );
            chooser.setAccessory( acc );
        }
        return chooser;
    }

    /**
     * Returns a StarTable got by asking the user for an SQL query on
     * a JDBC connection.
     * If the user declines to supply enough data, <tt>null</tt> is returned
     *
     * @param  parent   the parent component, used for positioning
     * @return  a new StarTable, or <tt>null</tt>
     */
    public StarTable getJDBCTable( Component parent ) {
        return getSQLReadDialog().readTableDialog( parent );
    }

    /**
     * Returns the SQLReadDialog object used for getting JDBC/SQL queries 
     * from the user.
     *
     * @return an SQLReadDialog
     */
    public SQLReadDialog getSQLReadDialog() {
        if ( sqlDialog == null ) {
            sqlDialog = new SQLReadDialog();
        }
        return sqlDialog;
    }

    /**
     * Sets the SQLReadDialog object used for getting JDBC/SQL queries
     * from the user.
     *
     * @param  sqlDialog  an SQLReadDialog
     */
    public void setSQLReadDialog( SQLReadDialog sqlDialog ) {
        this.sqlDialog = sqlDialog;
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

}
