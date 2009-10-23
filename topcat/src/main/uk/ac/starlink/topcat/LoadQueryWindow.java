package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import uk.ac.starlink.table.gui.FileChooserLoader;
import uk.ac.starlink.table.gui.LoadWorker;
import uk.ac.starlink.table.gui.TableLoadChooser;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.vo.RegistryTableLoadDialog;

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
    private final WindowListener closeCanceler_;
    private BasicTableConsumer tableConsumer_;

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

        /* Toolbar buttons. */
        TableLoadDialog[] tlds = chooser.getKnownDialogs();
        Set excludeTldSet = new HashSet( Arrays.asList( new Class[] {
            FileChooserLoader.class,
            RegistryTableLoadDialog.class,
        } ) );
        for ( int i = 0; i < tlds.length; i++ ) {
            TableLoadDialog tld = tlds[ i ];
            if ( tld.getIcon() != null &&
                 ! excludeTldSet.contains( tld.getClass() ) ) {
                getToolBar().add( chooser.makeAction( tld ) );
            }
        }
        getToolBar().addSeparator();

        /* Listener to arrange that if the window is disposed, any pending
         * load action will be cancelled. */
        closeCanceler_ = new WindowAdapter() {
            public void windowClosed( WindowEvent evt ) {
                if ( tableConsumer_ != null ) {
                    tableConsumer_.cancel();
                }
                tableConsumer_ = null;
                chooser_.setTableConsumer( null );
            }
        };
        addWindowListener( closeCanceler_ );

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
            tableConsumer_ = new LoadWindowTableConsumer();
            chooser_.setTableConsumer( tableConsumer_ );
            chooser_.setEnabled( true );
        }
        super.makeVisible();
    }

    protected boolean perform() {
        chooser_.getSubmitAction()
                .actionPerformed( new ActionEvent( this, 0, "OK" ) );
        return true;
    }

    /**
     * Provides the callbacks for when a table is loaded.
     */
    private class LoadWindowTableConsumer extends TopcatTableConsumer {

        LoadWindowTableConsumer() {
            super( LoadQueryWindow.this, ControlWindow.getInstance() );
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

                /* Since at least one table has been successfully loaded,
                 * dispose the loader window.  This is a bit fiddly; 
                 * dispose it in such a way that the current consumer is
                 * not cancelled, since it may receive more tables.
                 * Reinstate the canceler for next time it's made visible. */
                final LoadQueryWindow window = LoadQueryWindow.this;
                window.removeWindowListener( closeCanceler_ );
                window.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        window.removeWindowListener( this );
                        window.addWindowListener( closeCanceler_ );
                    }
                } );
                LoadQueryWindow.this.dispose();

                /* Pass the table to the method that actually wants it. */
                performLoading( table, getLoadingId() );
            }
        }
    }
}
