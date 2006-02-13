package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableConsumer;
import uk.ac.starlink.table.gui.LoadWorker;
import uk.ac.starlink.table.gui.TableLoadChooser;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;

/**
 * Dialogue for user to enter a new table location for loading.
 * This dialogue is not modal; if and when the user specifies a valid
 * table location, that table will be instantiated and the
 * {@link #performLoading} method will be called on it at that time.
 * <tt>performLoading</tt> is abstract, and must be implemented by
 * concrete subclasses to decide what happens to a table which is
 * successfully loaded.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class LoadQueryWindow extends QueryWindow {

    private final TableLoadChooser chooser_;
    private final StarTableFactory tableFactory_;
    private final JProgressBar progBar_;
    private TopcatTableConsumer tableConsumer_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new load window.
     *
     * @param  factory   table factory
     * @param  parent   parent component
     */
    public LoadQueryWindow( StarTableFactory factory, TableLoadChooser chooser,
                            Component parent ) {
        super( "Load New Table", parent, false, true );
        tableFactory_ = factory;
        chooser_ = chooser;

        /* Place a progress bar. */
        progBar_ = placeProgressBar();

        /* Add the main chooser widget. */
        getAuxControlPanel().add( chooser_ );
        JMenu dialogMenu = chooser_.makeKnownDialogsMenu( "DataSources" );
        dialogMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( dialogMenu );

        /* Demo actions. */
        JMenu demoMenu = new JMenu( "Examples" );
        demoMenu.setMnemonic( KeyEvent.VK_X );
        demoMenu.add( new AbstractAction( "Load Example Table" ) {
            public void actionPerformed( ActionEvent evt ) {
                String demoPath = TopcatUtils.DEMO_LOCATION + "/" + 
                                  TopcatUtils.DEMO_TABLE;
                final String loc = getClass().getClassLoader()
                                  .getResource( demoPath ).toString();
                new LoadWorker( tableConsumer_, loc ) {
                    public StarTable attemptLoad() throws IOException {
                        return tableFactory_.makeStarTable( loc, null );
                    }
                }.invoke();
            }
        } );

        try {
            final TableLoadDialog treed = new DemoLoadDialog();
            Action treedAct = new BasicAction( treed.getName(), null,
                                               treed.getDescription() ) {
                public void actionPerformed( ActionEvent evt ) {
                    treed.showLoadDialog( LoadQueryWindow.this,
                                          tableFactory_, null, tableConsumer_ );
                }
            };
            demoMenu.add( treedAct );
        }
        catch ( Throwable e ) {
            logger_.info( "Error instantiating demo load dialog" + e );
        }
        getJMenuBar().add( demoMenu );

        /* Help button. */
        addHelp( "LoadQueryWindow" );
    }

    /**
     * This method is called on a successfully loaded table when it has
     * been obtained.
     *
     * @param  startab  a newly-instanticated StarTable as specified by
     *         the user
     * @param  location some indication of where the table has come from
     */
    protected abstract void performLoading( StarTable startab,
                                            String location );

    /**
     * Display the window.  The default implementation is overridden to 
     * perform some actions required to get ready for loading.
     */
    public void makeVisible() {
        if ( tableConsumer_ == null ) {
            tableConsumer_ = new TopcatTableConsumer();
            chooser_.setTableConsumer( tableConsumer_ );
            chooser_.setEnabled( true );
        }
        super.makeVisible();
    }

    /**
     * Hide the window.  The default implementation is overridden to
     * perform some tidy up.
     */
    public void dispose() {
        tableConsumer_.cancel();
        tableConsumer_ = null;
        chooser_.setTableConsumer( null );
        super.dispose();
    }

    protected boolean perform() {
        chooser_.getSubmitAction()
                .actionPerformed( new ActionEvent( this, 0, "OK" ) );
        return true;
    }

    /**
     * Provides the callbacks for when a table is loaded.
     */
    private class TopcatTableConsumer extends BasicTableConsumer {

        private String id_;

        TopcatTableConsumer() {
            super( LoadQueryWindow.this );
        }

        public void loadStarted( String id ) {
            id_ = id;
            super.loadStarted( id );
        }

        protected void setLoading( boolean loading ) {
            super.setLoading( loading );
            if ( tableConsumer_ == this ) {
                chooser_.setEnabled( ! loading );
                progBar_.setIndeterminate( loading );
            }
        }

        protected void tableLoaded( StarTable table ) {
            assert table != null;
            if ( tableConsumer_ == this ) {

                /* Check we have at least one row. */
                if ( table.getRowCount() > 0 ) {

                    /* Hide the loader window. */
                    LoadQueryWindow.this.dispose();

                    /* Pass the table to the method that actually wants it. */
                    performLoading( table, id_ );
                }
                else {
                    JOptionPane.showMessageDialog( LoadQueryWindow.this,
                                                   "Table contained no rows",
                                                   "Empty Table",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        }
    }

}
