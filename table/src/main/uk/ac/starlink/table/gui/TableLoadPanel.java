package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;

/**
 * Component which aggregates a TableLoadDialog and buttons (OK and Cancel)
 * to control it.
 * The utility method ({@link #loadTables loadTables} is a convenient
 * way to do synchronous table loading.
 *
 * <p>Concrete implementations of this abstract class must implement the
 * {@link #getLoadClient} method to determine how loaded tables will be
 * consumed.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public abstract class TableLoadPanel extends JPanel {

    private final TableLoadDialog tld_;
    private final Action okAct_;
    private final Action cancelAct_;
    private JProgressBar progBar_;
    private volatile TableLoadWorker worker_;

    /**
     * Constructor.
     *
     * @param  tld  load dialogue
     * @param  tfact  representative table factory
     *         (not necessarily the one used to create tables)
     */
    @SuppressWarnings("this-escape")
    public TableLoadPanel( TableLoadDialog tld, StarTableFactory tfact ) {
        super( new BorderLayout() );
        tld_ = tld;
        progBar_ = new JProgressBar();
        progBar_.setString( "" );
        progBar_.setStringPainted( true );
        JComponent main = new JPanel( new BorderLayout() );
        JComponent controlBox = Box.createHorizontalBox();
        controlBox.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );
        main.add( controlBox, BorderLayout.SOUTH );
        add( main, BorderLayout.CENTER );
        okAct_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                assert worker_ == null;
                worker_ = new TableLoadWorker( tld_.createTableLoader(),
                                               getLoadClient(),
                                               getProgressBar() ) {
                    protected void finish( boolean cancelled ) {
                        super.finish( cancelled );
                        assert worker_ == this;
                        worker_ = null;
                        updateStatus();
                    }
                };
                worker_.start();
                updateStatus();
            }
        };
        cancelAct_ = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                assert worker_ != null;
                if ( worker_ != null ) {
                    worker_.cancel();
                }
            }
        };
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( cancelAct_ ) );
        controlBox.add( Box.createHorizontalStrut( 5 ) );
        controlBox.add( new JButton( okAct_ ) );
        okAct_.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( "enabled".equals( evt.getPropertyName() ) ) {
                    updateStatus();
                }
            }
        } );
        tld_.configure( tfact, okAct_ );
        main.add( tld.getQueryComponent(), BorderLayout.CENTER );
        updateStatus();
    }

    /**
     * Returns a GUI consumer for any tables loaded by this panel.
     * It will be called once for each load sequence; the returned object
     * may or may not be the same one each time.
     *
     * @return  load client ready to accept tables
     */
    protected abstract TableLoadClient getLoadClient();

    /**
     * Returns the action which starts to load tables.
     *
     * @return   OK action
     */
    public Action getOkAction() {
        return okAct_;
    }

    /**
     * Returns the action which cancels a load in progress.
     *
     * @return  Cancel action
     */
    public Action getCancelAction() {
        return cancelAct_;
    }

    /**
     * Indicates whether a load is currently in progress.
     *
     * @return   true  iff loading is taking place
     */
    public boolean isLoading() {
        return worker_ != null;
    }

    /**
     * Returns the progress bar used by this panel.
     *
     * @return  progress bar
     */
    public JProgressBar getProgressBar() {
        return progBar_;
    }

    /**
     * Sets the progress bar used by this panel.
     *
     * @param  progBar  progress bar
     */
    public void setProgressBar( JProgressBar progBar ) {
        progBar_ = progBar;
    }

    /**
     * Updates enabledness of actions based on current state.
     */
    private void updateStatus() {
        okAct_.setEnabled( worker_ == null );
        cancelAct_.setEnabled( worker_ != null );
    }

    /**
     * Modal dialogue which contains a TableLoadPanel.
     */
    private static class ModalDialog extends JDialog {
        private final List<StarTable> tableList_;
        private Throwable error_;

        /**
         * Constructor.
         *
         * @param  parent  parent component
         * @param  tld   load dialogue
         * @param  tfact  table factory
         */
        ModalDialog( Component parent, TableLoadDialog tld,
                     final StarTableFactory tfact, final boolean multi ) {
            super( parent instanceof Frame 
                       ? (Frame) parent
                       : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                                    parent ),
                   tld.getName(),
                   true );
            setDefaultCloseOperation( DISPOSE_ON_CLOSE );
            tableList_ = new ArrayList<StarTable>();
            final TableLoadClient client = new TableLoadClient() {
                public StarTableFactory getTableFactory() {
                    return tfact;
                }
                public void startSequence() {
                    tableList_.clear();
                }
                public void setLabel( String label ) {
                }
                public boolean loadSuccess( StarTable table ) {
                    tableList_.add( table );
                    return multi;
                }
                public boolean loadFailure( Throwable error ) {
                    error_ = error;
                    return false;
                }
                public void endSequence( boolean cancelled ) {
                    if ( ! cancelled ) {
                        dispose();
                    }
                }
            };
            final TableLoadPanel tlp = new TableLoadPanel( tld, tfact ) {
                protected TableLoadClient getLoadClient() {
                    return client;
                }
            };
            tlp.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
            getContentPane().setLayout( new BorderLayout() );
            getContentPane().add( tlp, BorderLayout.CENTER );
            getContentPane().add( tlp.getProgressBar(), BorderLayout.SOUTH );
            JMenu[] menus = tld.getMenus();
            if ( menus != null ) {
                JMenuBar mbar = new JMenuBar();
                setJMenuBar( mbar );
                for ( int im = 0; im < menus.length; im++ ) {
                    mbar.add( menus[ im ] );
                }
            }
            Action[] tacts = tld.getToolbarActions();
            if ( tacts != null && tacts.length > 0 ) {
                JToolBar tbar = new JToolBar();
                getContentPane().add( tbar, BorderLayout.NORTH );
                for ( int ia = 0; ia < tacts.length; ia++ ) {
                    tbar.add( tacts[ ia ] );
                }
            }
            addWindowListener( new WindowAdapter() {
                public void windowClosed( WindowEvent evt ) {
                    Action cancelAct = tlp.getCancelAction();
                    if ( cancelAct.isEnabled() ) {
                        cancelAct
                       .actionPerformed( new ActionEvent( evt.getSource(),
                                                          evt.getID(),
                                                          "Close" ) );
                    }
                }
            } );
        }

        /**
         * Returns the tables loaded by this dialogue, or throws an error
         * if one resulted from the loading action.
         *
         * @return   loaded tables
         */
        public StarTable[] getTables() throws IOException {
            if ( error_ == null ) {
                return tableList_.toArray( new StarTable[ 0 ] );
            }
            else {
                throw (IOException)
                      new IOException( error_.getMessage() )
                     .initCause( error_ );
            }
        }
    }

    /**
     * Displays a modal load dialogue to load a single table,
     * and returns the tables it has loaded when finished.
     *
     * @param  parent  parent component
     * @param  tld   load dialogue
     * @param  tfact  table factory
     */
    public static StarTable loadTable( Component parent,
                                       TableLoadDialog tld,
                                       StarTableFactory tfact )
            throws IOException {
        ModalDialog dia = new ModalDialog( parent, tld, tfact, false );
        dia.pack();
        dia.setLocationRelativeTo( parent );
        dia.setVisible( true );
        StarTable[] tables = dia.getTables();
        if ( tables.length == 0 ) {
            return null;
        }
        else {
            assert tables.length == 1;
            return tables[ 0 ];
        }
    }

    /**
     * Displays a modal load dialogue to load (possibly) multiple tables,
     * and returns the tables it has loaded when finished.
     *
     * @param  parent  parent component
     * @param  tld   load dialogue
     * @param  tfact  table factory
     */
    public static StarTable[] loadTables( Component parent,
                                          TableLoadDialog tld,
                                          StarTableFactory tfact )
            throws IOException {
        ModalDialog dia = new ModalDialog( parent, tld, tfact, true );
        dia.pack();
        dia.setLocationRelativeTo( parent );
        dia.setVisible( true );
        return dia.getTables();
    }

    /**
     * Test method.  Posts a file load dialogue and summarises any tables
     * that were loaded.
     */
    public static void main( String[] args ) throws IOException {
        TableLoadDialog tld = new FilestoreTableLoadDialog();
        StarTableFactory tfact = new StarTableFactory( true );
        boolean multi = false;
        if ( multi ) {
            StarTable[] tables = loadTables( null, tld, tfact );
            for ( int i = 0; i < tables.length; i++ ) {
                StarTable table = tables[ i ];
                System.out.println( i + ":\t" + table.getName() + " \t"
                                  + table.getColumnCount() + " x "
                                  + table.getRowCount() );
            }
        }
        else {
            StarTable table = loadTable( null, tld, tfact );
            System.out.println( table.getName() + " \t" 
                              + table.getColumnCount() + " x "
                              + table.getRowCount() );
        }
    }
}
