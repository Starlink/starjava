package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable; 
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSource;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.formats.AsciiTableWriter;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.ProgressBarStarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.ttools.filter.QuantCalc;

/**
 * A window which displays statistics for a RowSubset in the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StatsWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final StarTable dataModel_;
    private final TableColumnModel columnModel_;
    private final OptionsListModel subsets_;
    private final Map calcMap_;
    private final JTable jtab_;
    private final JProgressBar progBar_;
    private final JComboBox subSelector_;
    private final AbstractTableModel statsTableModel_;
    private final BitSet hideColumns_ = new BitSet();
    private final Action recalcAct_;
    private StatsCalculator activeCalculator_;
    private StatsCalculator lastCalc_;
    private SaveTableQueryWindow saveWindow_;

    private static final ValueInfo NROW_INFO =
        new DefaultValueInfo( "statRows", Long.class,
                              "Number of rows over which statistics " +
                              "were gathered" );
    private static final ValueInfo LOC_INFO =
        new DefaultValueInfo( "dataLocation", String.class,
                              "Location of original table" );
    private static final ValueInfo RSET_INFO =
        new DefaultValueInfo( "subset", String.class,
                              "Name of row subset over which statistics " +
                              "were gathered" );
    private static final Map QUANTILE_NAMES = quantileNames();
    private static final double[] QUANTILES =
        { 0.001, 0.01, 0.1, 0.25, 0.5, 0.75, 0.9, 0.99, 0.999, };

    /**
     * Constructs a StatsWindow to report on the statistics of data in a
     * given TableViewer.  Initially, no results are displayed; call
     * the {@link #setSubset} method to show some statistics.
     *
     * @param  tcModel  model containing the data for the table concerned
     * @param  parent   component used for window positioning
     */
    public StatsWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, "Row Statistics", parent );
        tcModel_ = tcModel;
        dataModel_ = tcModel.getDataModel();
        columnModel_ = tcModel.getColumnModel();
        subsets_ = tcModel.getSubsets();

        /* Set up a map to contain statistic sets that have been calculated. */
        calcMap_ = new HashMap();

        /* Construct a table model which contains the results of the
         * current calculation. */
        statsTableModel_ = makeStatsTableModel();

        /* Construct, configure and place the JTable which will form the 
         * main display area. */
        jtab_ = new JTable( statsTableModel_ );
        configureJTable( jtab_ );
        getMainArea().add( new SizingScrollPane( jtab_ ) );

        /* Customise the JTable's column model to provide control over
         * which columns are displayed. */
        final MetaColumnModel statsColumnModel =
            new MetaColumnModel( jtab_.getColumnModel(), statsTableModel_ );
        jtab_.setColumnModel( statsColumnModel );

        /* By default, hide some of the less useful columns. */
        int nstat = statsColumnModel.getColumnCount();
        for ( int i = 0; i < nstat; i++ ) {
            if ( hideColumns_.get( i ) ) {
                statsColumnModel.removeColumn( i );
            }
        }

        /* Watch the column model to see whether any quantile columns are
         * introduced.  If they are, a recalculation may be required,
         * since (because of the expense) quantiles are not calculated
         * unless explicitly asked for. */
        statsColumnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                if ( getMetaColumn( evt.getToIndex() )
                     instanceof QuantileColumn ) {
                    StatsCalculator calc = activeCalculator_;
                    if ( calc == null ) {
                        calc = lastCalc_;
                    }
                    if ( calc != null && ! calc.hasQuant ) {
                        recalcAct_.actionPerformed( null );
                    }
                }
            }
        } );

        /* Construct and place a widget for selecting which subset to
         * present results for. */
        JPanel controlPanel = getControlPanel();
        subSelector_ = subsets_.makeComboBox();
        subSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    setSubset( (RowSubset) evt.getItem() );
                }
            }
        } );
        controlPanel.add( new JLabel( "Subset for calculations: " ) );
        controlPanel.add( subSelector_ );

        /* Provide actions for saving the calculated statistics as a table
         * or importing them into TOPCAT as a new table. */
        TableSource statsSrc = new TableSource() {
            public StarTable getStarTable() {
                return getStatsTable();
            }
        };
        Action saveAct = createSaveTableAction( "statistics", statsSrc );
        Action importAct =
            createImportTableAction( "statistics", statsSrc,
                                     "stats of " + tcModel_.getID() );

        /* Provide an action for requesting a recalculation. */
        recalcAct_ = new BasicAction( "Recalculate", ResourceIcon.REDO,
                                      "Recalculate the statistics for " +
                                      "the current subset" ) {
            public void actionPerformed( ActionEvent evt ) {
                RowSubset rset = (RowSubset) subSelector_.getSelectedItem();
                calcMap_.remove( rset );
                setSubset( rset );
            }
        };

        /* Add actions to toolbar. */
        getToolBar().add( saveAct );
        getToolBar().add( importAct );
        getToolBar().add( recalcAct_ );
        getToolBar().addSeparator();

        /* Add Export menu. */
        JMenu exportMenu = new JMenu( "Export" );
        exportMenu.add( saveAct );
        exportMenu.add( importAct );
        getJMenuBar().add( exportMenu );

        /* Add a menu for statistics operations. */
        JMenu statsMenu = new JMenu( "Statistics" );
        statsMenu.setMnemonic( KeyEvent.VK_S );
        statsMenu.add( new JMenuItem( recalcAct_ ) );
        getJMenuBar().add( statsMenu );

        /* Add a menu for controlling column display. */
        JMenu displayMenu = statsColumnModel.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Add a progress bar for table scanning. */
        progBar_ = placeProgressBar();

        /* Add standard help actions. */
        addHelp( "StatsWindow" );

        /* Set the initial subset selection to the model's current subset,
         * which triggers the initial statistics calculation.
         * It doesn't continue to reflect this value (for performance reasons)
         * but it needs an initial value from somewhere. */
        subSelector_.setSelectedItem( tcModel.getSelectedSubset() );

        /* Add a trigger to recalculate for a different subset if the
         * global current subset is changed. */
        tcModel.addTopcatListener( new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                if ( evt.getCode() == TopcatEvent.CURRENT_SUBSET ) {
                    subSelector_.setSelectedItem( tcModel_
                                                 .getSelectedSubset() );
                }
                if ( evt.getCode() == TopcatEvent.SHOW_SUBSET ) {
                    subSelector_.setSelectedItem( (RowSubset) evt.getDatum() );
                }
            }
        } );
    }

    /**
     * Sets the RowSubset for which results are to be calculated.
     * This begins calculation of the statistics in a separate thread;
     * the table will be updated when the stats have been calculated.
     * This method will therefore return quickly, and may be called
     * on the event dispatcher thread.  Any pre-existing calculation
     * will be aborted, since its results will not now be required.
     *
     * @param   rset  the RowSubset for which results are to be displayed
     */
    public void setSubset( RowSubset rset ) {

        /* In the below, note that this window's calculator object keeps
         * a record of the active calculator.  Any StatsCalculator which
         * is not the active one should be stopped in its tracks rather
         * than continuing to munch cycles.  Keeping track of them like
         * this helps to ensure that there is never more than one active
         * calculator at a time. */

        /* Stop any calculations that are in train, since we will not now
         * need their results. */
        if ( activeCalculator_ != null ) {
            activeCalculator_.interrupt();
        }

        /* Ensure consistency with the subset selector. */
        if ( rset != subSelector_.getSelectedItem() ) {
            subSelector_.setSelectedItem( rset );
            return;
        }

        /* Work out if the requested statistics include any quantiles. */
        boolean hasQuant = false;
        for ( int icol = 0; icol < jtab_.getColumnCount() && ! hasQuant; 
              icol++ ) {
            hasQuant = hasQuant
                    || getMetaColumn( icol ) instanceof QuantileColumn;
        }

        /* If we have already done this calculation, display the results
         * directly. */
        if ( calcMap_.containsKey( rset ) ) {
            displayCalculations( (StatsCalculator) calcMap_.get( rset ) );
        }

        /* Otherwise, kick off a new thread which will perform the
         * calculations and display the results in due course. */
        else {
            activeCalculator_ = new StatsCalculator( rset, hasQuant );
            activeCalculator_.start();
        }
    }

    /**
     * Writes the results into the display portion of this StatsWindow.
     *
     * @param   stats  a StatsCalculator object which has completed
     *          its calculations
     */
    private void displayCalculations( StatsCalculator stats ) {

        /* Make the new results available to the table model and notify
         * it to update its data. */
        boolean firstTime = lastCalc_ == null;
        lastCalc_ = stats;
        statsTableModel_.fireTableDataChanged();

        /* First time only, configure the column widths according to 
         * contents. */
        if ( firstTime ) {
            StarJTable.configureColumnWidths( jtab_, 200, Integer.MAX_VALUE );
        }
        
        /* We used to take the opportunity here to update the subsets count
         * model, but it causes caching problems (can update it with an
         * out of date value), so no longer do this; the efficiency gains
         * are not great. */
    }

    /**
     * Extends the dispose method to interrupt any pending calculations.
     */
    public void dispose() {
        super.dispose();
        if ( activeCalculator_ != null ) {
            activeCalculator_.interrupt();
            activeCalculator_ = null;
            setBusy( false );
        }
    }

    /**
     * Do cosmetic configuration on a JTable appropriate to the kind
     * of data we want to display.
     *
     * @param   jtab  the table to configure
     */
    private static void configureJTable( JTable jtab ) {
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( false );
        TableColumnModel tcm = jtab.getColumnModel();
        TableModel tmodel = jtab.getModel();
        StarJTable.configureColumnWidth( jtab, 200, Integer.MAX_VALUE, 0 );

        for ( int icol = 0; icol < tcm.getColumnCount(); icol++ ) {
            Class clazz = tmodel.getColumnClass( icol );
            if ( clazz.equals( Long.class ) ) {
                clazz = Integer.class;
            }
            if ( clazz.equals( Object.class ) ) { // render min/max for numbers
                clazz = Double.class;
            }
            NumericCellRenderer rend = new NumericCellRenderer( clazz );
            TableColumn tcol = tcm.getColumn( icol );
            tcol.setCellRenderer( rend );
            tcol.setPreferredWidth( rend.getCellWidth() );
        }
    }

    private int getModelIndexFromRow( int irow ) {
        return columnModel_.getColumn( irow ).getModelIndex();
    }

    /**
     * Provides the largest cardinality which is counted as valid for
     * a given number of rows.  Any cardinality higher than this value
     * will not be reported.  This limit is provided for two reasons:
     * firstly for efficiency to reduce the burden of looking for a 
     * black needle in a dark haystack <em>when it isn't there</em>,
     * and secondly because cardinalities equal or near to the number 
     * of good values are not very useful figures to provide, since
     * they probably only indicate a few values which happen to be
     * the same by chance.
     * <p>
     * A cardinality, by the way, is the number of distinct values 
     * assumed by the rows in a column.
     * <p>
     * The implementation provided here is currently the lower of
     * 50 or <tt>0.75*nvalue</tt>.
     * 
     * @param  nvalue the number of values over which the cardinality 
     *                is to be assessed
     * @return  the  largest number of distinct values which is to 
     *               count as a cardinality
     */
    public int getCardinalityLimit( long nvalue ) {
        return Math.min( 50, (int) Math.min( nvalue * 0.75, 
                                             (double) Integer.MAX_VALUE ) );
    }


    /**
     * Helper class which provides a TableModel view of this window's
     * most recently completed StatsCalculator object.
     */
    private AbstractTableModel makeStatsTableModel() {

        /* Assemble the list of statistical quantities the model knows
         * about.  Some of the following columns are hidden by default. */
        List metas = new ArrayList();

        /* Index. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Index", Integer.class, "Column index" ) {
            public Object getValue( int irow ) {
                return new Integer( irow + 1 );
            }
        } );

        /* $ID. */
        hideColumns_.set( metas.size() );
        final ValueInfo idInfo = TopcatUtils.COLID_INFO;
        metas.add( new MetaColumn( idInfo.getName(), String.class,
                                   "Column unique identifier" ) {
            public Object getValue( int irow ) {
                return ((StarTableColumn) columnModel_.getColumn( irow ))
                      .getColumnInfo()
                      .getAuxDatum( idInfo )
                      .getValue();
            }
        } );

        /* Name. */
        metas.add( new MetaColumn( "Name", String.class, "Column name" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                return dataModel_.getColumnInfo( jcol ).getName();
            }
        } );

        /* Sum. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Sum", Double.class,
                                   "Sum of all values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                if ( lastCalc_.isNumber[ jcol ] ) {
                    return new Double( lastCalc_.sums[ jcol ] );
                }
                else if ( lastCalc_.isBoolean[ jcol ] ) {
                    return new Long( lastCalc_.ntrues[ jcol ] );
                }
                else {
                    return null;
                }
            }
        } );

        /* Mean. */
        metas.add( new MetaColumn( "Mean", Float.class,
                                   "Mean of values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ] || lastCalc_.isBoolean[ jcol ]
                     ? new Float( lastCalc_.means[ jcol ] )
                     : null;
            }
        } );

        /* Population Standard Deviation. */
        metas.add( new MetaColumn( "SD", Float.class,
                                   "Population standard deviation " +
                                   "of values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.popsdevs[ jcol ] ) 
                     : null;
            }
        } );

        /* Population Variance. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Variance", Float.class,
                                   "Population variance of values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.popvars[ jcol ] )
                     : null;
            }
        } );

        /* Sample Standard Deviation. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Sample_SD", Float.class,
                                   "Sample standard deviation of " +
                                   "values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.sampsdevs[ jcol ] ) 
                     : null;
            }
        } );

        /* Sample Variance. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Sample_Variance", Float.class,
                                   "Sample variance of values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.sampvars[ jcol ] )
                     : null;
            }
        } );

        /* Skew. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Skew", Float.class, 
                                   "Gamma 1 measure of skewness " +
                                   "of column value distribution" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.skews[ jcol ] )
                     : null;
            }
        } );

        /* Kurtosis. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Kurtosis", Float.class,
                                   "Gamma 2 measure of peakedness of " +
                                   "column value distribution" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.isNumber[ jcol ]
                     ? new Float( lastCalc_.kurts[ jcol ] )
                     : null;
            }
        } );

        /* Minimum. */
        metas.add( new MetaColumn( "Minimum", Object.class,
                                   "Numerically or other (e.g. alphabetically) "
                                 + "smallest value in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.mins[ jcol ];
            }
        } );

        /* Row for minimum. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Row_of_min", Long.class,
                                   "Row index of the minimum value " +
                                   "from column"  ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if  ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.mins[ jcol ] != null
                     ? new Long( lastCalc_.imins[ jcol ] )
                     : null;
            }
        } );

        /* Maximum. */
        metas.add( new MetaColumn( "Maximum", Object.class,
                                   "Numerically or other (e.g. alphabetically) "
                                 + "largest value in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.maxs[ jcol ];
            }
        } );

        /* Row for maximum. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Row_of_max", Long.class,
                                   "Row index of the maximum value " +
                                   "from column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return lastCalc_.maxs[ jcol ] != null
                     ? new Long( lastCalc_.imaxs[ jcol ] )
                     : null;
            }
        } );

        /* Count of non-null rows. */
        metas.add( new MetaColumn( "nGood", Long.class,
                                   "Number of non-blank values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return new Long( lastCalc_.ngoods[ jcol ] );
            }
        } );

        /* Count of null rows. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "nBad", Long.class,
                                   "Number of blank values in column" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                return new Long( lastCalc_.nbads[ jcol ] );
            }
        } );

        /* Cardinality. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Cardinality", Integer.class,
                                   "Number of distinct non-blank values " +
                                   "in column (blank if too large)" ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc_ == null || jcol >= lastCalc_.ncol ) return null;
                int card = lastCalc_.cards[ jcol ];
                return ( lastCalc_.isCardinal[ jcol ] && card > 0 )
                     ? new Integer( card )
                     : null;
            }
        } );

        /* Quantiles. */
        for ( int iq = 0; iq < QUANTILES.length; iq++ ) {
            hideColumns_.set( metas.size() );
            metas.add( new QuantileColumn( QUANTILES[ iq ] ) );
        }

        /* Construct a new TableModel based on these meta columns. */
        final MetaColumnTableModel tmodel = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnModel_.getColumnCount();
            }
        };

        /* Ensure that it responds to changes in the main column model. */
        columnModel_.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                tmodel.fireTableDataChanged();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                tmodel.fireTableDataChanged();
            }
            public void columnMoved( TableColumnModelEvent evt ) {
                tmodel.fireTableDataChanged();
            }
        } );

        /* Return the new model. */
        return tmodel;
    }

    /**
     * Returns the MetaColumn providing the metadata and data for a given
     * column in the JTable displayed by this window.
     *
     * @param   icol  JTable column index
     * @return  metacolumn object
     */
    private MetaColumn getMetaColumn( int icol ) {
        int jcol = jtab_.getColumnModel().getColumn( icol ).getModelIndex();
        return ((MetaColumnTableModel) jtab_.getModel())
              .getColumnList().get( jcol );
    }

    /**
     * Returns a StarTable giving the results of the most recent statistics
     * calculations.
     *
     * <p>The returned table is self-contained, and does not rely on any
     * of the data models held by this window.  This is not an efficiency 
     * problem, since the amount of data associated with a stats table
     * should always be relatively small (unless you have many thousands
     * of columns??).  And it's a Good Thing if this table is going to
     * be used later as imported into TOPCAT.
     *
     * @return   table of statistics
     */
    private StarTable getStatsTable() {

        /* Get an array of the column infos, one for each displayed item
         * of statistical data. */
        int ncol = jtab_.getColumnCount();
        ColumnInfo[] infos = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            infos[ icol ] =
                new ColumnInfo( getMetaColumn( icol ).getInfo() );
        }

        /* Construct an empty table with these columns. */
        RowListStarTable table = new RowListStarTable( infos );

        /* Add parameter metadata giving a basic description of what
         * this statistics table represents. */
        table.setName( "Statistics for " + tcModel_.getLabel() );
        RowSubset rset = (RowSubset) subSelector_.getSelectedItem();
        String loc = tcModel_.getLocation();
        if ( loc != null && loc.trim().length() > 0 ) {
            table.setParameter( new DescribedValue( LOC_INFO, loc ) );
        }
        table.setParameter( new DescribedValue( NROW_INFO,
                                                new Long( lastCalc_
                                                         .ngoodrow ) ) );
        if ( rset != null && rset != RowSubset.ALL ) {
            table.setParameter( new DescribedValue( RSET_INFO,
                                                    rset.getName() ) );
        }

        /* Populate the table with data as currently displayed in this
         * window's JTable. */
        int nrow = jtab_.getRowCount();
        for ( int irow = 0; irow < nrow; irow++ ) {
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = jtab_.getValueAt( irow, icol );
            }
            table.addRow( row );
        }

        /* Return the table.  In its raw form it may have some columns with 
         * types including Number and Object - it's not wise to let these
         * loose outside of this window, since they typically cannot be 
         * serialized by table output handlers.  So normalise it to contain
         * only sensible types. */
        return new NormaliseTable( table );
    }

    /**
     * Map providing human-readable names for quantiles.
     * There ought to be an entry for every entry in the {@link #QUANTILES}
     * array.
     *
     * @return   Double -&gt; String map
     */
    private static Map quantileNames() {
        Map map = new HashMap();
        map.put( new Double( 0.5 ), "Median" );
        map.put( new Double( 0.25 ), "Quartile1" );
        map.put( new Double( 0.75 ), "Quartile3" );
        map.put( new Double( 0.001 ), "Q001" );
        map.put( new Double( 0.01 ), "Q01" );
        map.put( new Double( 0.1 ), "Q10" );
        map.put( new Double( 0.9 ), "Q90" );
        map.put( new Double( 0.99 ), "Q99" );
        map.put( new Double( 0.999 ), "Q999" );
        return map;
    }

    /**
     * Metacolumn subclass which can display quantile values.
     */
    private class QuantileColumn extends MetaColumn {
        private final Double key_;
        private final double quant_;

        /**
         * Constructor.
         *
         * @param   quant   value at which quantile is calculated (0-1)
         */
        QuantileColumn( double quant ) {
            this( quant, (String) QUANTILE_NAMES.get( new Double( quant ) ) );
        }

        /**
         * Constructor with name.
         *
         * @param   quant   value at which quantile is calculated (0-1)
         * @param  name   column name
         */
        QuantileColumn( double quant, String name ) {
            super( name, Number.class,
                   "Value below which " + quant + " of column contents fall" );
            quant_ = quant;
            key_ = new Double( quant );
        }

        public Object getValue( int irow ) {
            int jcol = getModelIndexFromRow( irow );
            if ( lastCalc_.hasQuant ) {
                Map quantiles = lastCalc_.quantiles[ jcol ];
                if ( quantiles == null || ! quantiles.containsKey( key_ ) ) {
                    return null;
                }
                else {
                    return quantiles.get( key_ );
                }
            }
            else {
                return null;
            }
        }
    }

    /**
     * Helper class which performs the calculations in its own thread,
     * and displays the results in the StatsWindow when it's done.
     * A maximum of one active instance of this is maintained by each 
     * StatsWindow, its <tt>run</tt> method running in a separate thread.
     */
    private class StatsCalculator extends Thread {

        private final RowSubset rset;
        private final boolean hasQuant;

        int ncol;
        long ngoodrow;
        boolean[] isNumber;
        boolean[] isComparable;
        boolean[] isBoolean;
        boolean[] isCardinal;
        Object[] mins;
        Object[] maxs;
        long[] imins;
        long[] imaxs;
        long[] ngoods;
        long[] nbads;
        long[] ntrues;
        double[] means;
        double[] popsdevs;
        double[] popvars;
        double[] sampsdevs;
        double[] sampvars;
        double[] skews;
        double[] kurts;
        double[] sums;
        double[] sum2s;
        double[] sum3s;
        double[] sum4s;
        int[] cards;
        Map[] quantiles;
        QuantCalc[] quantCalcs;

        /**
         * Constructs a calculator object which can calculate the statistics
         * of the table owned by this StatsWindow over a given RowSubset.
         *
         * @param  rset the RowSubset to do calculations for
         * @param  hasQuant  true if quantiles need calculating
         */
        public StatsCalculator( RowSubset rset, boolean hasQuant ) {
            super( "StatsCalculator" );
            this.rset = rset;
            this.hasQuant = hasQuant; 
        }

        /**
         * Initiates calculations of the requested statistics, and
         * if they complete without interruption arranges for the
         * results to be displayed in the StatsWindow.
         * The cursor is also switched between busy and non-busy at the
         * start and end of calculcations as long as this calculator
         * has not been superceded by another in the mean time.
         */
        public void run() {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( StatsCalculator.this == activeCalculator_ ) {
                        StatsWindow.this.setBusy( true );
                    }
                }
            } );
            try {
                calculate();
                calcMap_.put( rset, StatsCalculator.this );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        displayCalculations( StatsCalculator.this );
                    }
                } );
            }
            catch ( OutOfMemoryError e ) {
                if ( hasQuant ) {
                    quantCalcs = null;
                    final Object msg = new String[] {
                        "Out of memory while calculating quantiles.",
                        "",
                        "Quantiles (median, quartiles, percentiles etc)",
                        "are much more expensive to calculate than other",
                        "statistical quantities.",
                        "To calculate statistics for this table you will",
                        "need to undisplay these columns, or start again",
                        "with more memory.",
                    };
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(
                                    StatsWindow.this, msg, "Calculation failed",
                                    JOptionPane.ERROR_MESSAGE );
                        }
                    } );
                }
            }
            catch ( IOException e ) {
                // no other action
            }
            finally {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( StatsCalculator.this == activeCalculator_ ) {
                            activeCalculator_ = null;
                            StatsWindow.this.setBusy( false );
                        }
                    }
                } );
            }
        }

        /**
         * Performs the calculations, storing the results in the member
         * variables of this StatsCalculator object.
         * An IOException may indicate that the thread was interrupted
         * deliberately, or that some other error occurred.  Either way,
         * some sensible results should be returned based on the number of
         * rows which have been got through so far.
         *
         * @throws  IOException if calculation is not complete
         */
        private void calculate() throws IOException {
            ncol = dataModel_.getColumnCount();

            /* Allocate result objects. */
            isNumber = new boolean[ ncol ];
            isComparable = new boolean[ ncol ];
            isBoolean = new boolean[ ncol ];
            isCardinal = new boolean[ ncol ];
            mins = new Object[ ncol ];
            maxs = new Object[ ncol ];
            imins = new long[ ncol ];
            imaxs = new long[ ncol ];
            ngoods = new long[ ncol ];
            nbads = new long[ ncol ];
            ntrues = new long[ ncol ];
            means = new double[ ncol ];
            popsdevs = new double[ ncol ];
            popvars = new double[ ncol ];
            sampsdevs = new double[ ncol ];
            sampvars = new double[ ncol ];
            skews = new double[ ncol ];
            kurts = new double[ ncol ];
            sums = new double[ ncol ];
            sum2s = new double[ ncol ];
            sum3s = new double[ ncol ];
            sum4s = new double[ ncol ];
            cards = new int[ ncol ];
            quantiles = new Map[ ncol ];
            quantCalcs = new QuantCalc[ ncol ];

            boolean[] badcompars = new boolean[ ncol ];
            double[] dmins = new double[ ncol ];
            double[] dmaxs = new double[ ncol ];
            Set[] valuesets = new Set[ ncol ];
            Arrays.fill( dmins, Double.MAX_VALUE );
            Arrays.fill( dmaxs, -Double.MAX_VALUE );

            /* If we are going to calculate quantiles it's useful to work out
             * how many rows there are in the subset up front.  This is a
             * bit risky - the count may be incorrect if subset definition
             * has changed.  Hmm.  It's not essential to use this value,
             * but doing it will make it easier to catch, e.g.,
             * OutOfMemoryErrors. */
            long nr = -1;
            if ( hasQuant ) {
                Object num = tcModel_.getSubsetCounts().get( rset );
                if ( num instanceof Number ) {
                    nr = ((Number) num).longValue();
                }
            }

            /* See which columns we can sensibly gather statistics on. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                Class clazz = dataModel_.getColumnInfo( icol )
                                        .getContentClass();
                isNumber[ icol ] = Number.class.isAssignableFrom( clazz );
                isComparable[ icol ] = Comparable.class
                                                 .isAssignableFrom( clazz );
                isBoolean[ icol ] = clazz.equals( Boolean.class );
                isCardinal[ icol ] = ! clazz.equals( Boolean.class );
                if ( isCardinal[ icol ] ) {
                    valuesets[ icol ] = new HashSet();
                }
                if ( hasQuant && isNumber[ icol ] ) {
                    quantCalcs[ icol ] = QuantCalc.createInstance( clazz, nr );
                }
            }

            /* Iterate over the selected rows in the table. */
            RowSequence rseq = new ProgressBarStarTable( dataModel_, progBar_ )
                              .getRowSequence();
            int cardlimit = getCardinalityLimit( dataModel_.getRowCount() );
            IOException interruption = null;
            long lrow = 0L;
            ngoodrow = 0L;
            for ( ; true; lrow++ ) {
                long lrow1 = lrow;

                /* A thread interruption may manifest itself here as an
                 * exception (see ProgressBarStarTable).  If so, save the
                 * exception and break out. */
                try {
                    if ( ! rseq.next() ) {
                        break;
                    }
                }
                catch ( IOException e ) {
                    interruption = e;
                    break;
                }

                if ( rset.isIncluded( lrow ) ) {
                    ngoodrow++;
                    Object[] row = rseq.getRow();

                    /* Accumulate statistics as appropriate. */
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        Object val = row[ icol ];
                        boolean good;
                        if ( val == null ) {
                            good = false;
                        }
                        else {
                            if ( isNumber[ icol ] ) {
                                double dval = Double.NaN;
                                if ( ! ( val instanceof Number ) ) {
                                    System.err.println(
                                        "Error in table data: not numeric at " +
                                        lrow1 + "," + icol + "(" + val + ")" );
                                    good = false;
                                }
                                else {
                                    dval = ((Number) val).doubleValue();
                                }
                                if ( Double.isNaN( dval ) ) {
                                    good = false;
                                }
                                else {
                                    good = true;
                                }
                                if ( good ) {
                                    if ( dval < dmins[ icol ] ) {
                                        dmins[ icol ] = dval;
                                        mins[ icol ] = val;
                                        imins[ icol ] = lrow1;
                                    }
                                    if ( dval > dmaxs[ icol ] ) {
                                        dmaxs[ icol ] = dval;
                                        maxs[ icol ] = val;
                                        imaxs[ icol ] = lrow1;
                                    }
                                    double s1 = dval;
                                    double s2 = dval * s1;
                                    double s3 = dval * s2;
                                    double s4 = dval * s3;
                                    sums[ icol ] += s1;
                                    sum2s[ icol ] += s2;
                                    sum3s[ icol ] += s3;
                                    sum4s[ icol ] += s4;
                                    if ( hasQuant ) {
                                        quantCalcs[ icol ].acceptDatum( val );
                                    }
                                }
                            }
                            else if ( isComparable[ icol ] ) {
                                if ( ! ( val instanceof Comparable ) ) {
                                    System.err.println(
                                        "Error in table data: not Comparable " +
                                        " at " + lrow1 + "," + icol + "(" +
                                        val + ")" );
                                    good = false;
                                }
                                else {
                                    good = true;
                                }
                                if ( good ) {
                                    Comparable cval = (Comparable) val;
                                    if ( mins[ icol ] == null ) {
                                        assert maxs[ icol ] == null;
                                        mins[ icol ] = val;
                                        maxs[ icol ] = val;
                                        imins[ icol ] = lrow1;
                                        imaxs[ icol ] = lrow1;
                                    }
                                    else {
                                        try {
                                            if ( cval.compareTo( mins[ icol ] )
                                                 < 0 ) {
                                                mins[ icol ] = val;
                                                imins[ icol ] = lrow1;
                                            }
                                            else if ( cval
                                                     .compareTo( maxs[ icol ] )
                                                      > 0 ) {
                                                maxs[ icol ] = val;
                                                imaxs[ icol ] = lrow1;
                                            }
                                        }

                                        /* It is possible for two objects in the
                                         * same column both to be Comparable,
                                         * but not to each other.  In this case,
                                         * there does not exist a well-defined
                                         * min/max for that column. */
                                        catch ( ClassCastException e ) {
                                            badcompars[ icol ] = true;
                                        }
                                    }
                                }
                            }
                            else if ( isBoolean[ icol ] ) {
                                if ( ! ( val instanceof Boolean ) ) {
                                    System.err.println(
                                        "Error in table data: not boolean at " +
                                        lrow1 + "," + icol + "(" + val + ")" );
                                    good = false;
                                }
                                else {
                                    good = true;
                                }
                                if ( good ) {
                                    boolean bval =
                                        ((Boolean) val).booleanValue();
                                    if ( bval ) {
                                        ntrues[ icol ]++;
                                    }
                                }
                            }
                            else {
                                good = true;
                            }
                            if ( good ) {
                                ngoods[ icol ]++;
                            }
                        }

                        /* Maybe calculate the cardinalities. */
                        if ( good && isCardinal[ icol ] ) {
                            valuesets[ icol ].add( val );
                            if ( valuesets[ icol ].size() > cardlimit ) {
                                isCardinal[ icol ] = false;
                                valuesets[ icol ] = null;
                            }
                        }
                    }
                }
            }
            rseq.close();
            long nrow = lrow;

            /* Calculate the actual statistics based on the accumulated
             * values.  We do this even if the summation was interrupted,
             * since the partially-accumulated values may be of interest. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                long ngood = ngoods[ icol ];
                nbads[ icol ] = ngoodrow - ngood;
                if ( ngood > 0 ) {
                    if ( isNumber[ icol ] ) {
                        double dcount = (double) ngood;
                        double sum0 = dcount;
                        double sum1 = sums[ icol ];
                        double sum2 = sum2s[ icol ];
                        double sum3 = sum3s[ icol ];
                        double sum4 = sum4s[ icol ];
                        double mean = sum1 / dcount;
                        double nvar = ( sum2 - sum1 * sum1 / dcount );
                        means[ icol ] = mean;
                        popvars[ icol ] = nvar / dcount;
                        popsdevs[ icol ] = Math.sqrt( popvars[ icol ] );
                        if ( ngood > 1 ) {
                            sampvars[ icol ] = nvar / ( dcount - 1 );
                            sampsdevs[ icol ] = Math.sqrt( sampvars[ icol ] );
                        }
                        else {
                            sampvars[ icol ] = Double.NaN;
                            sampsdevs[ icol ] = Double.NaN;
                        }
                        skews[ icol ] =
                            Math.sqrt( dcount ) / Math.pow( nvar, 1.5 ) *
                            ( + 1 * sum3 
                              - 3 * mean * sum2 
                              + 3 * mean * mean * sum1
                              - 1 * mean * mean * mean * sum0 );
                        kurts[ icol ] =
                            dcount / ( nvar * nvar ) *
                            ( + 1 * sum4
                              - 4 * mean * sum3
                              + 6 * mean * mean * sum2
                              - 4 * mean * mean * mean * sum1
                              + 1 * mean * mean * mean * mean * sum0 ) - 3.0;
                    }
                    else if ( isBoolean[ icol ] ) {
                        means[ icol ] = (double) ntrues[ icol ] / ngood;
                    }
                    if ( isCardinal[ icol ] ) {
                        int card = valuesets[ icol ].size();
                        if ( card <= getCardinalityLimit( ngood ) ) {
                            cards[ icol ] = card;
                        }
                        else {
                            cards[ icol ] = 0;
                            isCardinal[ icol ] = false;
                            valuesets[ icol ] = null;
                        }
                    }
                    QuantCalc quantCalc = quantCalcs[ icol ];
                    if ( quantCalc != null ) {
                        quantCalc.ready();
                        quantiles[ icol ] = new HashMap();
                        for ( int iq = 0; iq < QUANTILES.length; iq++ ) {
                            double q = QUANTILES[ iq ];
                            quantiles[ icol ].put( new Double( q ),
                                                   quantCalc.getQuantile( q ) );
                        }
                        quantCalcs[ icol ] = null;
                    }
                }
                else {
                    means[ icol ] = Double.NaN;
                    popsdevs[ icol ] = Double.NaN;
                    popvars[ icol ] = Double.NaN;
                    sampvars[ icol ] = Double.NaN;
                    sampsdevs[ icol ] = Double.NaN;
                    skews[ icol ] = Double.NaN;
                    kurts[ icol ] = Double.NaN;
                }
                if ( badcompars[ icol ] ) {
                    mins[ icol ] = null;
                    maxs[ icol ] = null;
                    imins[ icol ] = -1L;
                    imaxs[ icol ] = -1L;
                }
            }

            /* Re-throw any interruption-type exception we picked up. */
            if ( interruption != null ) {
                throw interruption;
            }
        }
    }
}
