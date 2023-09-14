package uk.ac.starlink.topcat;

import java.io.IOException;
import java.awt.event.ActionEvent;
import java.util.BitSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.gui.ComboBoxBumper;

/**
 * SearchWindow subclass that searches for content in the columns
 * of a TableViewerWindow.
 * Since the effective data model (a StarTable) may be large,
 * the searching is done asynchronously, off the EDT.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2023
 */
public class DataColumnSearchWindow extends ColumnSearchWindow {

    private final TableViewerWindow viewWindow_;
    private final TopcatModel tcModel_;
    private final JProgressBar progBar_;
    private final Action searchAct_;
    private final Action cancelAct_;
    private ExecutorService executor_;
    private Future<?> searchJob_;

    /**
     * Constructor.
     *
     * @param  viewWindow  the table view window on behalf of which
     *                     searches will be carried out
     */
    public DataColumnSearchWindow( TableViewerWindow viewWindow ) {
        super( "Search Column", viewWindow, "Column",
               createColumnSelectorModel( viewWindow.getTopcatModel() ) );
        viewWindow_ = viewWindow;
        tcModel_ = viewWindow.getTopcatModel();
        ActionForwarder forwarder = getActionForwarder();

        /* Action to perform search. */
        searchAct_ =
                new BasicAction( "Search", null,
                                 "Search selected column for target string" ) {
            public void actionPerformed( ActionEvent evt ) {
                final Search search = createSearch();
                if ( search != null ) {
                    cancelSearch();
                    searchJob_ = getExecutor().submit( new Runnable() {
                        public void run() {
                            performSearch( search );
                        }
                    } );
                    updateActions();
                };
            }
        };
        forwarder.addActionListener( evt -> updateActions() );
        cancelAct_ = new BasicAction( "Stop", null,
                                      "Interrupt running search" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancelSearch();
            }
        };
        updateActions();

        /* Hitting return in text field starts search. */
        getTextField().addActionListener( searchAct_ );

        /* Place components. */
        JComponent controlLine = getControlBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( cancelAct_ ) );
        controlLine.add( Box.createHorizontalStrut( 10 ) );
        controlLine.add( new JButton( searchAct_ ) );
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        progBar_ = placeProgressBar();
        pack();
    }

    /**
     * Stringifies a cell value for pattern matching purposes.
     *
     * @param  cell value, assumed to be from a column for which
     *         <code>canSearchColumn</code> returns true
     * @return  stringified value
     */
    public String cellToString( Object cell ) {
        return cell == null ? "" : cell.toString();
    }

    @Override
    public void dispose() {
        cancelSearch();
        super.dispose();
    }

    /**
     * Cancels a running search operation.
     */
    private void cancelSearch() {
        if ( searchJob_ != null ) {
            searchJob_.cancel( true );
            searchJob_ = null;
        }
        progBar_.setModel( new DefaultBoundedRangeModel() );
        updateActions();
    }

    /**
     * Updates enabled state of the Cancel and Search actions
     * based on the current state of this window.
     */
    private void updateActions() {
        boolean isRunning = searchJob_ != null && !searchJob_.isDone();
        cancelAct_.setEnabled( isRunning );
        searchAct_.setEnabled( !isRunning && createSearch() != null );
    }

    /**
     * Returns an executor to which jobs can be submitted.
     *
     * @return  lazily-created single-threaded executor
     */
    private ExecutorService getExecutor() {
        if ( executor_ == null ) {
            executor_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
                public Thread newThread( Runnable r ) {
                    Thread thread = new Thread( r, "Searcher" );
                    thread.setDaemon( true );
                    return thread;
                }
            } );
        }
        return executor_;
    }

    /**
     * Does the work of searching according to a given specification,
     * updating the GUI asynchronously as it goes.
     *
     * <p>This method must be invoked from a non-EDT thread, and it
     * checks for thread interruption status as it goes.
     *
     * @param  search  search specification
     */
    private void performSearch( Search search ) {
        int jcol = search.getColumnIndex();
        Pattern pattern = search.getPattern();
        SearchScope scope = search.getScope();
        StarTable dataModel = tcModel_.getDataModel();
        final ViewerTableModel viewModel = tcModel_.getViewModel();
        long nfind = 0;
        long irow0 = -1;

        /* Clear selection. */
        SwingUtilities
            .invokeLater( () -> viewWindow_.setSelection( RowSubset.NONE ) );

        /* Set up a progress bar.  Since we install a new model, this
         * will have the effect of throwing out any model that is still
         * being updated by previous invocations of this method. */
        int nrow = viewModel.getRowCount();
        final BoundedRangeModel progModel =
            new DefaultBoundedRangeModel(0, 0, 0, nrow );
        SwingUtilities.invokeLater( () -> progBar_.setModel( progModel ) );

        /* Iterate over rows in the view model (not the whole table). */
        long nextUpdate = System.currentTimeMillis();
        int krow = 0;
        final IndexSet foundSet = new IndexSet( dataModel.getRowCount() );
        for ( Iterator<Long> irowIt = viewModel.getRowIndexIterator();
              irowIt.hasNext() && ! Thread.currentThread().isInterrupted(); ) {

            /* Look for and record matching rows. */
            long irow = irowIt.next().longValue();
            Object cell;
            try {
                cell = dataModel.getCell( irow, jcol );
            }
            catch ( IOException e ) {
                cell = null;
            }
            if ( cell != null ) {
                String txt = cellToString( cell );
                if ( scope.matches( pattern.matcher( txt ) ) ) {
                    foundSet.addIndex( irow );
                    final boolean isFirst = nfind++ == 0;
                    if ( isFirst ) {
                        irow0 = irow;
                    }
                }
            }

            /* Update the progress bar if we haven't done it recently. */
            long time = System.currentTimeMillis();
            if ( time >= nextUpdate || krow == nrow - 1 ) {
                nextUpdate = time + 100;
                final int krow0 = krow;
                SwingUtilities.invokeLater( () -> progModel.setValue( krow0 ) );
            }
            krow++;
        }

        /* If we complete successfully, update the selection. */
        if ( !Thread.currentThread().isInterrupted() ) {
            final long nfind0 = nfind;
            final long irow00 = irow0;
            SwingUtilities.invokeLater( () -> {
                updateActions();
                progModel.setValue( 0 );
                if ( nfind0 == 1 ) {
                    tcModel_.highlightRow( irow00 );
                }
                else if ( nfind0 > 0 ) {
                    viewWindow_.setSelection( foundSet.createRowSubset() );
                    viewWindow_.scrollToRow( viewModel.getViewRow( irow00 ) );
                }
                searchCompleted( nfind0 > 0 );
            } );
        }
    }

    /**
     * Indicates whether a given column can be searched by this window.
     *
     * @param  info   column metadata
     * @return   true iff this window is prepared to search contents of
     *                the described column
     */
    public static boolean canSearchColumn( ColumnInfo info ) {
        Class<?> clazz = info.getContentClass();
        return String.class.isAssignableFrom( clazz )
            || Number.class.isAssignableFrom( clazz )
            || Boolean.class.isAssignableFrom( clazz );
    }

    /**
     * Returns a column selector model for use with this window
     * based on a given topcat model.
     *
     * @param  tcModel  topcat model
     * @return   column selector
     */
    private static ComboBoxModel<TableColumn>
            createColumnSelectorModel( TopcatModel tcModel ) {
        return new RestrictedColumnComboBoxModel( tcModel.getColumnModel(),
                                                  false ) {
            public boolean acceptColumn( ColumnInfo info ) {
                return canSearchColumn( info );
            }
        };
    }

    /**
     * Utility class to keep a set of row index values (long integers).
     * The implementation could get cleverer, more efficient, more robust,
     * (may be wasteful for finding few rows in large tables)
     * but it's probably OK up to 2^31 rows.  Avoid premature optimisation
     * for now.
     */
    private static class IndexSet {
        final BitSet bitset_;

        /**
         * Constructor.
         *
         * @param   maximum index value that might be stored
         */
        IndexSet( long nrow ) {
            bitset_ = new BitSet( (int) nrow );
        }

        /**
         * Add an entry.
         *
         * @param  ix  row index
         */
        void addIndex( long ix ) {
            bitset_.set( Tables.checkedLongToInt( ix ) );
        }

        /**
         * Return a RowSubset corresponding to the values added so far.
         *
         * @return  row subset
         */
        RowSubset createRowSubset() {
            return new BitsRowSubset( "Found", bitset_ );
        }
    }
}
