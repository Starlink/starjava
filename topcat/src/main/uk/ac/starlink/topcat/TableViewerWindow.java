package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.BitSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.table.gui.ViewHugeSelectionModel;
import uk.ac.starlink.table.gui.ViewHugeTableModel;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * Browser window for viewing the data in a table.
 * This provides a JTable view on a TopcatModel.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Feb 2004
 */
public class TableViewerWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final JTable jtable_;
    private final JScrollPane scroller_;
    private final JScrollBar vbar_;
    private final ListSelectionListener rowSelListener_;
    private final TableColumnModel colModel_;
    private final ViewerTableModel viewModel_;
    private final ListSelectionModel rowSelectionModel_;
    private final TableColumnModel dummyColModel_;
    private final PropertyChangeListener viewbaseListener_;
    private final TableRowHeader rowHeader_;
    private RowManager rowManager_;
    private int lastViewRowCount_;
    private boolean selfHighlighting_;

    private static int MAX_COLUMN_WIDTH = 300;
    private static int MAX_SAMPLE_ROWS = 800;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new TableViewer to view a given table.
     *
     * @param  tcModel  topcat model
     * @param  parent  parent component for positioning; may be null
     */
    public TableViewerWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, "Table Browser", parent );
        tcModel_ = tcModel;
        colModel_ = tcModel.getColumnModel();
        viewModel_ = tcModel.getViewModel();
        rowSelectionModel_ = new DefaultListSelectionModel();
        rowSelectionModel_.setSelectionMode( ListSelectionModel
                                            .MULTIPLE_INTERVAL_SELECTION );

        /* Listen for topcat events. */
        tcModel.addTopcatListener( new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                topcatModelChanged( evt );
            }
        } );

        /* Construct and place the JTable and containing scroll panel. 
         * Address here a rather obscure issue which causes the wrong cell to
         * be edited if a sort happens during an edit. */
        jtable_ = new JTable() {
            @Override
            public void tableChanged( TableModelEvent evt ) {
                editingCanceled( new ChangeEvent( this ) );
                super.tableChanged( evt );
            }
        };
        dummyColModel_ = jtable_.getColumnModel();
        jtable_.setCellSelectionEnabled( false );
        jtable_.setColumnSelectionAllowed( false );
        jtable_.setRowSelectionAllowed( true );
        scroller_ = new SizingScrollPane( jtable_ );
        vbar_ = scroller_.getVerticalScrollBar();
        getMainArea().add( scroller_, BorderLayout.CENTER );

        /* Set up row header panel. */
        rowHeader_ = new TableRowHeader( jtable_ ) {
            public long rowNumber( int jRow ) {
                int irow = rowManager_.getViewTableRow( jRow );
                return viewModel_.getBaseRow( irow ) + 1;
            }
        };
        rowHeader_.setLongestNumber( (int) Math.min( tcModel.getDataModel()
                                                            .getRowCount(),
                                                     Integer.MAX_VALUE ) );
        rowHeader_.installOnScroller( scroller_ );
        viewbaseListener_ = new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( ViewHugeTableModel.VIEWBASE_PROPERTY
                                       .equals( evt.getPropertyName() ) ) {
                    rowHeader_.repaint();
                }
            }
        };

        /* Configure for the current state of the apparent table and arrange
         * for reconfiguration if the row count changes. */
        lastViewRowCount_ = viewModel_.getRowCount();
        viewModel_.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                int nrow = viewModel_.getRowCount();
                if ( nrow != lastViewRowCount_ ) {
                    lastViewRowCount_ = nrow;
                    configureTable();
                }
            }
        } );
        configureTable();

        /* Print action - this is just a dummy, but people have asked
         * why there is no print action here. */
        Action printAct = new BasicAction( "Print", ResourceIcon.PRINT,
                                           "Table printing information" ) {
            Object msg = new String[] {
                "There is no option to print the table out directly.",
                "However, you can save it in a format suitable for printing",
                "(ascii, text, HTML or LaTeX)",
                "using the Save option in the Control Window",
                "and print out the resulting file separately.",
            };
            public void actionPerformed( ActionEvent evt ) {
                JOptionPane.showMessageDialog(
                     TableViewerWindow.this, msg, "Printing Tables",
                     JOptionPane.INFORMATION_MESSAGE );
            }
        };

        /* Actions for new subsets from the row selection. */
        final Action includeAct = new SelectionSubsetAction( true );
        final Action excludeAct = new SelectionSubsetAction( false );

        /* Configure a listener for row selection events. */
        rowSelListener_ = new ListSelectionListener() {
            long lastActive = -1;
            public void valueChanged( ListSelectionEvent evt ) {
                boolean hasSelection = ! rowSelectionModel_.isSelectionEmpty();

                /* Configure event availability. */
                includeAct.setEnabled( hasSelection );
                excludeAct.setEnabled( hasSelection );

                /* If the selection consists of a single row, construe this
                 * as a row activation request. */
                if ( evt != null && ! evt.getValueIsAdjusting() ) {
                    if ( hasSelection ) {
                        int first = rowSelectionModel_.getMinSelectionIndex();
                        if ( rowSelectionModel_.getMaxSelectionIndex()
                             == first ) {
                            long active = viewModel_.getBaseRow( first );
                            if ( active != lastActive ) {
                                lastActive = active;

                                /* Unless this call was initiated by a call to
                                 * rowHighlight, message the topcat model that
                                 * row has been highlighted. */
                                if ( ! selfHighlighting_ ) {
                                    selfHighlighting_ = true;
                                    tcModel_.highlightRow( active );
                                    selfHighlighting_ = false;
                                }
                            }
                        }
                    }
                    else {
                        lastActive = -1;
                    }
                }
            }
        };
        rowSelListener_.valueChanged( null );
        rowSelectionModel_.addListSelectionListener( rowSelListener_ );

        /* Configure a listener for column popup menus. */
        MouseListener mousey = new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            public void mouseReleased( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            private void maybeShowPopup( MouseEvent evt ) {
                if ( evt.isPopupTrigger() ) {
                    Component comp = evt.getComponent();
                    int jcol = jtable_.columnAtPoint( evt.getPoint() );
                    if ( comp == rowHeader_ ) {
                        jcol = -1;
                    }
                    if ( jcol >= -1 ) {
                        JPopupMenu popper = columnPopup( jcol );
                        if ( popper != null ) {
                            popper.show( comp, evt.getX(), evt.getY() );
                        }
                    }
                }
            }
        };
        jtable_.addMouseListener( mousey );
        jtable_.getTableHeader().addMouseListener( mousey );
        rowHeader_.addMouseListener( mousey );

        /* Arrange for column widths to be set from the data. */
        StarJTable.configureColumnWidths( jtable_, MAX_COLUMN_WIDTH,
                                          MAX_SAMPLE_ROWS );
        colModel_.addColumnModelListener( new TableColumnModelListener() {
            public void columnAdded( TableColumnModelEvent evt ) {
                int icol = evt.getToIndex();
                StarJTable.configureColumnWidth( jtable_, MAX_COLUMN_WIDTH,
                                                 MAX_SAMPLE_ROWS, icol );
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
            }
            public void columnMarginChanged( ChangeEvent evt ) {
            }
            public void columnMoved( TableColumnModelEvent evt ) {
            }
            public void columnSelectionChanged( ListSelectionEvent evt ) {
            }
        } );

        /* Add actions to the toolbar. */
        getToolBar().add( includeAct );
        getToolBar().add( excludeAct );
        getToolBar().addSeparator();

        /* Add print action to the File menu. */
        getWindowMenu().insert( printAct, 1 );

        /* Add a subsets menu. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        getJMenuBar().add( subsetMenu );
        subsetMenu.add( includeAct );
        subsetMenu.add( excludeAct );
        final OptionsListModel subsets = tcModel.getSubsets();
        Action applysubsetAct = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                int index = evt.getID();
                tcModel_.applySubset( (RowSubset) subsets.get( index ) );
            }
        };
        Action highlightsubsetAct = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                int index = evt.getID();
                setSelection( (RowSubset) subsets.get( index ) );
            }
        };
        JMenu applysubsetMenu =
            subsets.makeJMenu( "Select Current Subset", applysubsetAct );
        applysubsetMenu.setIcon( ResourceIcon.APPLY_SUBSET );
        applysubsetMenu.setToolTipText( "Sets the current subset for this "
                                      + "table; most subsequent views and "
                                      + "operations are restricted to the "
                                      + " corresponding rows" );
        JMenu highlightsubsetMenu =
            subsets.makeJMenu( "Highlight Subset", highlightsubsetAct );
        highlightsubsetMenu.setIcon( ResourceIcon.HIGHLIGHT );
        highlightsubsetMenu.setToolTipText( "Marks the rows of a selected "
                                          + "subset" );
        subsetMenu.add( highlightsubsetMenu );

        /* This action, formerly known as "Apply Subset", is a bit dangerous,
         * since it changes the globally selected subset in a way that might
         * not be obvious.  Comment it out for now. */
        if ( false ) {
            subsetMenu.add( applysubsetMenu );
        }

        /* Add help information. */
        addHelp( "TableViewerWindow" );
    }

    /**
     * Sets the JTable up for the current state of the view model.
     * Should be called if the view model changes in significant ways,
     * in particular if its row count changes.
     */
    private void configureTable() {

        /* Configure the JTable with a dummy column model;
         * if you don't do that, then resetting its TableModel scribbles
         * over the installed ColumnModel, and we don't want that to happen. */
        jtable_.setColumnModel( dummyColModel_ );

        /* Configure the JTable for the current state.
         * Mostly this is done by an appropriately constructed RowManager,
         * but here we also manage registration of listeners specific to
         * ViewHugeTableModels. */
        rowManager_ = createRowManager();
        TableModel tm0 = jtable_.getModel();
        if ( tm0 instanceof ViewHugeTableModel ) {
            ViewHugeTableModel vhtm = (ViewHugeTableModel) tm0;
            vhtm.removePropertyChangeListener( viewbaseListener_ );
        }
        rowManager_.configureJTable();
        TableModel tm1 = jtable_.getModel();
        if ( tm1 instanceof ViewHugeTableModel ) {
            ViewHugeTableModel vhtm = (ViewHugeTableModel) tm1;
            vhtm.addPropertyChangeListener( viewbaseListener_ );
        }

        /* Restore the column model we want. */
        jtable_.setColumnModel( tcModel_.getColumnModel() );

        /* Make sure the row header is up to date. */
        if ( tm0 != tm1 ) {
            rowHeader_.modelChanged();
        }

        /* Sensible default position. */
        scroller_.getViewport().setViewPosition( new Point( 0, 0 ) );
    }

    /**
     * Displays a given row in a highlighted fashion.
     * 
     * <p>This may disturb the state of the viewer somewhat - the current
     * implementation sets the current table selection to the single row
     * indicated.
     * 
     * @param  lrow  index in the data model (not the view model) of the
     *               row to be highlighted
     */
    private void highlightRow( long lrow ) {
        
        /* Maintain a flag to ensure that this doesn't cause a call to
         * the topcat model's highlightRow method, which would in turn
         * call this one, causing infinite recursion. */
        if ( selfHighlighting_ ) {
            return;
        }
        selfHighlighting_ = true;
    
        /* Get ready. */    
        rowSelectionModel_.clearSelection();
     
        /* Check if the view currently on display contains the requested row. */
        if ( viewModel_.getSubset().isIncluded( lrow ) ) {
        
            /* Get the view row corresponding to the requested table row. */
            int viewRow = viewModel_.getViewRow( lrow );
            
            /* It can't be -1 since we've just checked it's in the current
             * subset. */
            assert viewRow >= 0;
                
            /* Set the JTable's selection to contain just this row. */
            rowSelectionModel_.addSelectionInterval( viewRow, viewRow );
        
            /* Arrange for the row to be visible in the middle of the
             * scrollpane's viewport. */
            scrollToRow( viewRow );
        }   
        selfHighlighting_ = false;
    }

    /**
     * Returns a popup menu for a given column.  If the dummy column
     * index -1 is used, a popup suitable for the row header column
     * will be returned.
     *
     * @param  jcol the data model column to which the menu applies
     */
    private JPopupMenu columnPopup( final int jcol ) {
        final StarTableColumn tcol;
        ColumnInfo colInfo;
        String colName;
        boolean rowHead = jcol < 0;
        if ( ! rowHead ) {
            tcol = (StarTableColumn) colModel_.getColumn( jcol );
            colInfo = tcol.getColumnInfo();
            colName = colInfo.getName();
        }
        else {
            tcol = null;
            colInfo = null;
            colName = null;
        }

        JPopupMenu popper = new JPopupMenu();
        final Component parent = this;

        /* Action to replace current column. */
        if ( ! rowHead ) {
            Action replacecolAct =
                new BasicAction( "Replace Column", ResourceIcon.MODIFY,
                                 "Replace " + colName +
                                 " with new synthetic column" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        SyntheticColumnQueryWindow
                           .replaceColumnDialog( tcModel_, tcol, parent );
                    }
                };
            replacecolAct.setEnabled( TopcatUtils.canJel() );
            popper.add( replacecolAct );
        }

        /* Action to append a new column here. */
        Action addcolAct =
            new BasicAction( "New Synthetic Column", ResourceIcon.ADD,
                             "Add new synthetic column after " + colName ) {
                public void actionPerformed( ActionEvent evt ) {
                    new SyntheticColumnQueryWindow( tcModel_, jcol + 1, parent )
                   .setVisible( true );
                }
            };
        addcolAct.setEnabled( TopcatUtils.canJel() );
        popper.add( addcolAct );

        /* Actions to sort on current column. */
        if ( rowHead ) {
            popper.add( tcModel_.getUnsortAction() );
        }
        else {
            if ( Comparable.class
                           .isAssignableFrom( colInfo.getContentClass() ) ) {
                popper.add( tcModel_
                           .getSortAction( new SortOrder( tcol ), true ) );
                popper.add( tcModel_
                           .getSortAction( new SortOrder( tcol ), false ) );
            }
        }

        /* Action to hide the current column. */
        if ( ! rowHead ) {
            Action hidecolAct =
                new BasicAction( "Hide Column", ResourceIcon.HIDE,
                                 "Hide column " + colName + " from view" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        colModel_.removeColumn( tcol );
                    }
                };
            popper.add( hidecolAct );
        }

        /* Action to search for a string. */
        if ( ! rowHead && colInfo.getContentClass() == String.class ) {
            Action searchAct =
                new BasicAction( "Search Column", ResourceIcon.SEARCH,
                                 "Search for regular expression in cell" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        findRegex( tcol, jcol );
                    }
                };
            popper.add( searchAct );
        }

        /* Action to explode column. */
        if ( ! rowHead &&
             colInfo.isArray() &&
             ColumnInfoWindow.getElementCount( colInfo ) > 0 ) {
            Action explodeAct =
                new BasicAction( "Explode Column", ResourceIcon.EXPLODE,
                                 "Replace N-element array column " +
                                 "with N scalar columns" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        tcModel_.explodeColumn( tcol );
                    }
                };
             popper.add( explodeAct );
        }

        return popper;
    }

    /**
     * Returns a BitSet in which bit <i>i</i> is set if a table view row
     * corresponding to row <i>i</i> of this viewer's data model has
     * (or has not) been selected in the GUI.  This BitSet has the
     * same number of bits as the data model has rows.
     *
     * @param   isInclude  true for an inclusion mask,
     *                     false for exclusion
     * @return  new bit vector
     */
    private BitSet getSelectionMask( boolean isInclude ) {
        int nrow = (int) tcModel_.getDataModel().getRowCount();
        BitSet bits = new BitSet( nrow );
        int imin = rowSelectionModel_.getMinSelectionIndex();
        int imax = rowSelectionModel_.getMaxSelectionIndex();
        int[] rowMap = viewModel_.getRowMap();
        for ( int i = imin; i <= imax; i++ ) {
            if ( rowSelectionModel_.isSelectedIndex( i ) ) {
                bits.set( rowMap == null ? i : rowMap[ i ] );
            }
        }
        if ( ! isInclude ) {
            bits.flip( 0, nrow );
        }
        return bits;
    }

    /**
     * Pops up a window asking for a regular expression and selects the
     * rows which match it in a given column.
     *
     * @param   table column for matching
     * @param   index of the column index in the view model
     */
    private void findRegex( StarTableColumn tcol, int jcol ) {
        Object[] msg = {
             "Enter a regular expression (e.g. \".*XYZ.*\")",
             "to select rows whose " + tcol.getColumnInfo().getName() +
             " value match it",
        };
        String regex = JOptionPane
                      .showInputDialog( this, msg, "Search Column",
                                        JOptionPane.QUESTION_MESSAGE );
        if ( regex != null && regex.trim().length() > 0 ) {
            Pattern pat = Pattern.compile( regex );
            int nfound = 0;
            int first = -1;
            int nrow = viewModel_.getRowCount();
            for ( int irow = 0; irow < nrow; irow++ ) {
                Object cell = viewModel_.getValueAt( irow, jcol );
                if ( cell instanceof String ) {
                    if ( pat.matcher( (String) cell ).matches() ) {
                        if ( nfound == 0 ) {
                            first = irow;
                            rowSelectionModel_.clearSelection();
                        }
                        rowSelectionModel_.addSelectionInterval( irow, irow );
                        nfound++;
                    }
                }
            }
            if ( nfound == 1 ) {
                tcModel_.highlightRow( viewModel_.getBaseRow( first ) );
            }
            else if ( nfound > 1 ) {
                scrollToRow( first );
            }
        }
    }

    /**
     * Scrolls the JTable so that the given row is visible in the centre
     * of the window.
     *
     * @param   viewRow  row index in the view model
     */
    private void scrollToRow( int viewRow ) {
        rowManager_.scrollToRow( viewRow );
    }

    /**
     * Scrolls the JTable so that the given column is visible at the
     * left of the window.
     *
     * @param  viewCol  column index in the view model
     */
    private void scrollToColumn( int viewCol ) {
        Rectangle viewRect = jtable_.getCellRect( 0, viewCol, false );
        int xMid = viewRect.x + viewRect.width / 2;
        JScrollBar xBar = scroller_.getHorizontalScrollBar();
        xBar.setValue( xMid - xBar.getVisibleAmount() / 2 );
    }

    /**
     * Invoked when a TopcatEvent is received.
     *
     * @param  evt  event
     */
    private void topcatModelChanged( TopcatEvent evt ) {
        int code = evt.getCode();
        if ( code == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                highlightRow( ((Long) datum).longValue() );
            }
            else {
                assert false;
            }
        }
        else if ( code == TopcatEvent.COLUMN ) {
            Object datum = evt.getDatum();
            assert datum instanceof StarTableColumn;
            int viewCol = -1;
            int ncol = colModel_.getColumnCount();
            for ( int icol = 0; icol < ncol && viewCol < 0; icol++ ) {
                if ( colModel_.getColumn( icol ) == datum ) {
                    viewCol = icol;
                }
            }
            if ( viewCol >= 0 ) {
                scrollToColumn( viewCol );
            }
        }
        else if ( code == TopcatEvent.SHOW_SUBSET ) {
            setSelection( (RowSubset) evt.getDatum() );
        }
    }

    /**
     * Sets the row selection for this window's JTable to correspond to a
     * given row subset.
     *
     * @param   rset  row subset
     */
    private void setSelection( RowSubset rset ) {
        rowSelectionModel_.setValueIsAdjusting( true );
        rowSelectionModel_.clearSelection();
        int nrow = (int) viewModel_.getRowCount();
        int[] rowMap = viewModel_.getRowMap();
        for ( int irow = 0; irow < nrow; irow++ ) {
            long jrow = rowMap == null ? (long) irow
                                       : (long) rowMap[ irow ];
            if ( rset.isIncluded( jrow ) ) {
                rowSelectionModel_.addSelectionInterval( irow, irow );
            }
        }
        rowSelectionModel_.setValueIsAdjusting( false );
    }

    /**
     * Action which creates a new subset based on the rows which are
     * (or are not) selected in the current view.
     */
    private class SelectionSubsetAction extends BasicAction {

        private final boolean isInclude_;

        /**
         * Constructor.
         *
         * @param   isInclude   true for inclusion subset, false for exclusion
         */
        public SelectionSubsetAction( boolean isInclude ) {
            super( "Subset From " + ( isInclude ? "Selected" : "Unselected" )
                                  + "Rows",
                   isInclude ? ResourceIcon.INCLUDE_ROWS
                             : ResourceIcon.EXCLUDE_ROWS,
                   "Define a new row subset containing all "
                 + ( isInclude ? "selected" : "visible unselected" ) + "rows" );
            isInclude_ = isInclude;
        }

        public void actionPerformed( ActionEvent evt ) {
            SubsetConsumer consumer =
                tcModel_.enquireNewSubsetConsumer( TableViewerWindow.this );
            if ( consumer != null ) {
                BitSet bits = getSelectionMask( isInclude_ );
                consumer.consumeSubset( tcModel_, bits );
            }
        }
    }

    /**
     * Returns a RowManager for the current view table.
     *
     * @return  row manager
     */
    private RowManager createRowManager() {
        long nrow = viewModel_.getRowCount();
        if ( nrow <= ViewHugeTableModel.VIEWSIZE ) {
            return new NormalRowManager();
        }
        else {
            logger_.info( "Large table (" + nrow + " rows)"
                        + ": using ViewHugeTableModel" );
            return new EnormoRowManager();
        }
    }

    /**
     * Abstraction for handling operations related to the view table rows
     * as represented in this window's JTable.
     */
    private interface RowManager {

        /**
         * Sets up this window's JTable to view its view TableModel
         * and its selection model.
         */
        void configureJTable();

        /**
         * Returns the row index in this window's view table corresponding
         * to a given row index in the JTable.
         *
         * @param   jtableRow  JTable row index
         * @return  view table row index
         */
        int getViewTableRow( int jtableRow );

        /**
         * Ensures that the given row index in the view model is visible
         * in the current JTable viewport.
         *
         * @param  viewRow  row index in view model
         */
        void scrollToRow( int viewRow );
    }

    /**
     * RowManager implementation for a standard table.
     */
    private class NormalRowManager implements RowManager {
        public void configureJTable() {
            jtable_.setModel( viewModel_ );
            jtable_.setSelectionModel( rowSelectionModel_ );
        }
        public int getViewTableRow( int jtableRow ) {
            return jtableRow;
        }
        public void scrollToRow( int viewRow ) {
            Rectangle viewRect = jtable_.getCellRect( viewRow, 0, false );
            int yMid = viewRect.y + viewRect.height / 2;
            vbar_.setValue( yMid - vbar_.getVisibleAmount() / 2 );
        }
    }

    /**
     * RowManager for very large tables.   The relevant constraint is that
     * attempting to display a very large TableModel in a JTable in the
     * usual way may end up with a JTable component whose height in pixels
     * cannot be stored in an int, which means Swing can't cope with it.
     * See ViewHugeTableModel documentation for additional discussion.
     */
    private class EnormoRowManager implements RowManager {
        private final ViewHugeTableModel vhModel_;
        private final ViewHugeSelectionModel vhSelModel_;
        EnormoRowManager() {
            vhModel_ = new ViewHugeTableModel( viewModel_, vbar_ );
            vhSelModel_ =
                new ViewHugeSelectionModel( rowSelectionModel_, vhModel_ );
        }
        public void configureJTable() {
            jtable_.setModel( vhModel_ );
            jtable_.setSelectionModel( vhSelModel_ );
        }
        public int getViewTableRow( int jtableRow ) {
            return vhModel_.getHugeRow( jtableRow );
        }
        public void scrollToRow( int hugeRow ) {

            /* First set the scrollbar to approximately the right position.
             * It's not possible to configure the viewport position
             * exactly in this way, because of the way the ViewHugeTableModel
             * works - view position is underdetermined by scrollbar state. */
            int vmin = vbar_.getMinimum();
            int vmax = vbar_.getMaximum();
            int vext = vbar_.getVisibleAmount();
            long nrow = viewModel_.getRowCount();
            long vrange = vmax - vmin;
            long vval0 = vmin + ( vrange * hugeRow / nrow ) - vext / 2;
            long vval1 = Math.min( Math.max( vmin, vval0 ), vmax - vext );
            int vval2 = Tables.assertLongToInt( vval1 );
            vbar_.setValue( vval2 );

            /* Now the viewBase is set appropriately, we can adjust the
             * scrollbar state to exactly the required position. */
            int jtableRow = hugeRow - vhModel_.getViewBase();
            Rectangle viewRect = jtable_.getCellRect( jtableRow, 0, false );
            int yMid = viewRect.y + viewRect.height / 2;
            vbar_.setValue( yMid - vbar_.getVisibleAmount() / 2 );
        }
    }
}
