package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable; 
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSource;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.ProgressBarStarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.ttools.filter.GKQuantiler;
import uk.ac.starlink.ttools.filter.Quantiler;
import uk.ac.starlink.ttools.filter.SortQuantiler;
import uk.ac.starlink.ttools.filter.TableStats;
import uk.ac.starlink.ttools.filter.UnivariateStats;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * A window which displays statistics for a RowSubset in the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StatsWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final StarTable dataModel_;
    private final TableColumnModel columnModel_;
    private final OptionsListModel<RowSubset> subsets_;
    private final Map<RowSubset,StatsCalculator> calcMap_;
    private final JTable jtab_;
    private final JProgressBar progBar_;
    private final JComboBox<RowSubset> subSelector_;
    private final MetaColumnTableModel statsTableModel_;
    private final BitSet hideColumns_ = new BitSet();
    private final Action recalcAct_;
    private final ToggleButtonModel qapproxModel_;
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
    private static final Map<Double,String> NAMED_QUANTILES =
        createNamedQuantiles();

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
        calcMap_ = new HashMap<RowSubset,StatsCalculator>();

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
            @Override
            public void columnAdded( TableColumnModelEvent evt ) {
                MetaColumn col = getMetaColumn( evt.getToIndex() );
                boolean isQuant = col instanceof QuantileColumn;
                boolean isMad = col instanceof MadColumn;
                if ( isQuant || isMad ) {
                    StatsCalculator calc = activeCalculator_;
                    if ( calc == null ) {
                        calc = lastCalc_;
                    }
                    if ( calc != null && ( ( isQuant && ! calc.hasQuant_ ) ||
                                           ( isMad && ! calc.hasMad_ ) ) ) {
                        recalcAct_.actionPerformed( null );
                    }
                }
            }
        } );

        /* Allow JTable sorting by clicking on column headers. */
        MetaColumnTableSorter sorter =
            new MetaColumnTableSorter( statsTableModel_ );
        sorter.install( jtab_.getTableHeader() );

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

        /* Configure a toggle for quantile calculation implementation. */
        qapproxModel_ =
            new ToggleButtonModel( "Approximate quantile algorithm",
                                   ResourceIcon.QAPPROX,
                                   "If selected, quantiles are calculated "
                                 + "slower, approximately, "
                                 + "but in fixed memory");

        /* Add actions to toolbar. */
        getToolBar().add( saveAct );
        getToolBar().add( importAct );
        getToolBar().add( recalcAct_ );
        getToolBar().add( qapproxModel_.createToolbarButton() );
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
        statsMenu.add( qapproxModel_.createMenuItem() );
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
            activeCalculator_.cancel();
        }

        /* Ensure consistency with the subset selector. */
        if ( rset != subSelector_.getSelectedItem() ) {
            subSelector_.setSelectedItem( rset );
            return;
        }

        /* Work out if the requested statistics include any quantiles
         * or Median Absolute Deviation. */
        boolean hasQuant = false;
        boolean hasMad = false;
        for ( int icol = 0; icol < jtab_.getColumnCount() && ! hasQuant; 
              icol++ ) {
            MetaColumn metacol = getMetaColumn( icol );
            hasQuant = hasQuant || metacol instanceof QuantileColumn;
            hasMad = hasMad || metacol instanceof MadColumn;
        }

        /* If we have already done this calculation, display the results
         * directly. */
        if ( calcMap_.containsKey( rset ) ) {
            displayCalculations( calcMap_.get( rset ) );
        }

        /* Otherwise, kick off a new thread which will perform the
         * calculations and display the results in due course. */
        else {
            activeCalculator_ = new StatsCalculator( rset, hasQuant, hasMad );
            Thread calcThread =
                new Thread( activeCalculator_, "StatsCalculator" );
            calcThread.setDaemon( true );
            calcThread.start();
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
            activeCalculator_.cancel();
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
            Class<?> clazz = tmodel.getColumnClass( icol );
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

    /**
     * Determines table column model index
     * for a given row in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window.
     *
     * @param  irow   row index in unsorted table model
     * @return  TableColumnModel index
     */
    private int getModelIndexFromRow( int irow ) {
        return columnModel_.getColumn( irow ).getModelIndex();
    }

    /**
     * Helper class which provides a TableModel view of this window's
     * most recently completed StatsCalculator object.
     */
    private MetaColumnTableModel makeStatsTableModel() {

        /* Assemble the list of statistical quantities the model knows
         * about.  Some of the following columns are hidden by default. */
        List<MetaColumn> metas = new ArrayList<MetaColumn>();

        /* Index. */
        hideColumns_.set( metas.size() );
        metas.add( new MetaColumn( "Index", Integer.class, "Column index" ) {
            public Integer getValue( int irow ) {
                return Integer.valueOf( irow + 1 );
            }
        } );

        /* $ID. */
        hideColumns_.set( metas.size() );
        final ValueInfo idInfo = TopcatUtils.COLID_INFO;
        metas.add( new MetaColumn( idInfo.getName(), String.class,
                                   "Column unique identifier" ) {
            public String getValue( int irow ) {
                Object idObj =
                    ((StarTableColumn) columnModel_.getColumn( irow ))
                   .getColumnInfo()
                   .getAuxDatum( idInfo )
                   .getValue();
                return idObj instanceof String ? (String) idObj : null;
            }
        } );

        /* Name. */
        metas.add( new MetaColumn( "Name", String.class, "Column name" ) {
            public String getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                return dataModel_.getColumnInfo( jcol ).getName();
            }
        } );

        /* Sum. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Sum", Number.class,
                                       "Sum of all values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Double.valueOf( cstat.sum_ );
            }
        } );

        /* Mean. */
        metas.add( new StatMetaColumn( "Mean", Number.class,
                                       "Mean of values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Double.valueOf( cstat.mean_ );
            }
        } );

        /* Population Standard Deviation. */
        metas.add( new StatMetaColumn( "SD", Number.class,
                                       "Population standard deviation " +
                                       "of values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) Math.sqrt( cstat.popvar_ ) );
            }
        } );

        /* Population Variance. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Variance", Number.class,
                                       "Population variance "
                                     + "of values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) cstat.popvar_ );
            }
        } );

        /* Sample Standard Deviation. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Sample_SD", Number.class,
                                   "Sample standard deviation of " +
                                   "values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) Math.sqrt( cstat.sampvar_ ) );
            }
        } );

        /* Sample Variance. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Sample_Variance", Number.class,
                                       "Sample variance of values in column" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) cstat.sampvar_ );
            }
        } );

        /* Median Absolute Deviation. */
        hideColumns_.set( metas.size() );
        metas.add( new MadColumn( "Median_Absolute_Deviation",
                                   "Median absolute deviation"
                                 + " of values in column", 1f ) );

        /* Scaled Median Absolute Deviation. */
        hideColumns_.set( metas.size() );
        double madScale = TableStats.MAD_SCALE;
        metas.add( new MadColumn( "Scaled_Median_Absolute_Deviation",
                                   "Median absolute deviation multiplied by "
                                 + madScale
                                 + " (estimator of normal"
                                 + " standard deviation)",
                                   madScale ) );
 
        /* Skew. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Skew", Number.class, 
                                       "Gamma 1 measure of skewness " +
                                       "of column value distribution" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) cstat.skew_ );
            }
        } );

        /* Kurtosis. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Kurtosis", Number.class,
                                       "Gamma 2 measure of peakedness of " +
                                       "column value distribution" ) {
            public Number getValue( ColStat cstat ) {
                return Float.valueOf( (float) cstat.kurt_ );
            }
        } );

        /* Minimum. */
        metas.add( new StatMetaColumn( "Minimum", Comparable.class,
                                       "Numerically or other "
                                     + "(e.g. alphabetically) "
                                     + "smallest value in column" ) {
            public Comparable<?> getValue( ColStat cstat ) {
                return cstat.min_;
            }
        } );

        /* Row for minimum. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Row_of_min", Long.class,
                                       "Row index of the minimum value " +
                                       "from column"  ) {
            public Long getValue( ColStat cstat ) {
                long imin = cstat.imin_; 
                return imin >= 0 ? Long.valueOf( imin + 1 ) : null;
            }
        } );

        /* Maximum. */
        metas.add( new StatMetaColumn( "Maximum", Comparable.class,
                                       "Numerically or other "
                                     + "(e.g. alphabetically) "
                                     + "largest value in column" ) {
            public Comparable<?> getValue( ColStat cstat ) {
                return cstat.max_;
            }
        } );

        /* Row for maximum. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Row_of_max", Long.class,
                                       "Row index of the maximum value " +
                                       "from column" ) {
            public Long getValue( ColStat cstat ) {
                long imax = cstat.imax_;
                return imax >= 0 ? Long.valueOf( imax + 1 ) : null;
            }
        } );

        /* Count of non-null rows. */
        metas.add( new StatMetaColumn( "nGood", Long.class,
                                       "Number of non-blank values in column" ){
            public Long getValue( ColStat cstat ) {
                return Long.valueOf( cstat.ngood_ );
            }
        } );

        /* Count of null rows. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "nBad", Long.class,
                                       "Number of blank values in column" ) {
            public Long getValue( ColStat cstat ) {
                return Long.valueOf( cstat.nbad_ );
            }
        } );

        /* Cardinality. */
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Cardinality", Short.class,
                                       "Number of distinct non-blank values " +
                                       "in column (blank if too large)" ) {
            public Short getValue( ColStat cstat ) {
                int ncard = cstat.ncard_;
                return ncard > 0 ? Short.valueOf( (short) ncard ) : null;
            }
        } );

        /* Quantiles. */
        for ( Map.Entry<Double,String> entry : NAMED_QUANTILES.entrySet() ) {
            hideColumns_.set( metas.size() );
            metas.add( new QuantileColumn( entry.getKey(), entry.getValue() ) );
        }

        /* Quantities for fixed-length arrays. */
        String forArray = " for fixed-length array-valued column";
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Array_nGoods", long[].class,
                                       "Per-element counts of non-blank values"
                                     + forArray ) {
            public long[] getValue( ColStat cstat ) {
                return cstat.arrayCounts_;
            }
        } );
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Array_Sums", double[].class,
                                       "Per-element sums" + forArray ) {
            public double[] getValue( ColStat cstat ) {
                return cstat.arraySums_;
            }
        } );
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Array_Means", double[].class,
                                       "Per-element means" + forArray ) {
            public double[] getValue( ColStat cstat ) {
                return cstat.arrayMeans_;
            }
        } );
        hideColumns_.set( metas.size() );
        metas.add( new StatMetaColumn( "Array_SDs", double[].class,
                                       "Per-element population "
                                     + "standard deviations" + forArray ) {
            public double[] getValue( ColStat cstat ) {
                return cstat.arrayPopstdevs_;
            }
        } );

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
                                                Long.valueOf( lastCalc_
                                                             .ngoodrow_ ) ) );
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
     * Map providing quantile options provided along with human-readable names.
     *
     * @return   value -&gt; name map;
     *           value is between 0 (0th percentile) and 1 (100th percentile)
     */
    private static Map<Double,String> createNamedQuantiles() {
        Map<Double,String> map = new LinkedHashMap<Double,String>();
        map.put( Double.valueOf( 0.001 ), "Q001" );
        map.put( Double.valueOf( 0.01 ), "Q01" );
        map.put( Double.valueOf( 0.1 ), "Q10" );
        map.put( Double.valueOf( 0.25 ), "Quartile1" );
        map.put( Double.valueOf( 0.5 ), "Median" );
        map.put( Double.valueOf( 0.75 ), "Quartile3" );
        map.put( Double.valueOf( 0.9 ), "Q90" );
        map.put( Double.valueOf( 0.99 ), "Q99" );
        map.put( Double.valueOf( 0.999 ), "Q999" );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Metacolumn subclass which displays a scaled version of the
     * Median Absolute Deviation.
     */
    private class MadColumn extends StatMetaColumn {
        private final double scale_;

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  description  column description
         * @param  scale  scale factor for result
         */
        MadColumn( String name, String description, double scale ) {
            super( name, Number.class, description );
            scale_ = scale;
        }

        public Number getValue( ColStat cstat ) {
            return Float.valueOf( (float) ( cstat.mad_ * scale_ ) );
        }
    }

    /**
     * Metacolumn subclass which can display quantile values.
     */
    private class QuantileColumn extends StatMetaColumn {
        private final Double key_;
        private final double quant_;

        /**
         * Constructor with name.
         *
         * @param   quant   value at which quantile is calculated (0-1)
         * @param  name   column name
         */
        QuantileColumn( double quant, String name ) {
            super( name, Double.class,
                   "Value below which " + quant + " of column contents fall" );
            quant_ = quant;
            key_ = Double.valueOf( quant );
        }

        public Double getValue( ColStat cstat ) {
            Map<Double,Double> quantiles = cstat.quantiles_;
            return quantiles == null ? null : quantiles.get( key_ );
        }
    }

    /**
     * Partial MetaColumn implementation that represents some quantity
     * derived from a ColStat object.
     */
    private abstract class StatMetaColumn extends MetaColumn {

        /**
         * Constructor.
         *
         * @param  name  statistic name
         * @param  clazz  value class
         * @param  descrip  statistic description
         */
        StatMetaColumn( String name, Class<?> clazz, String descrip ) {
            super( name, clazz, descrip );
        }

        /**
         * Obtains this column's value from a ColStat.
         *
         * @param  cstat  column statistics object
         * @return  result
         */
        abstract Object getValue( ColStat cstat );

        final public Object getValue( int irow ) {
            if ( lastCalc_ == null ) {
                return null;
            }
            else {
                int jcol = getModelIndexFromRow( irow );
                return jcol < lastCalc_.colStats_.length
                     ? getValue( lastCalc_.colStats_[ jcol ] )
                     : null;
            }
        }
    }

    /**
     * Stores calculated statistical information relating to one column
     * of the data table.  The memory footprint of this object is small.
     */
    private static class ColStat {

        final long ngood_;
        final long nbad_;
        final double sum_;
        final Comparable<?> min_;
        final Comparable<?> max_;
        final long imin_;
        final long imax_;
        final int ncard_;
        final double mean_;
        final double popvar_;
        final double sampvar_;
        final double skew_;
        final double kurt_;
        final double median_;
        final Map<Double,Double> quantiles_;
        final long[] arrayCounts_;
        final double[] arraySums_;
        final double[] arrayMeans_;
        final double[] arrayPopstdevs_;
        double mad_;

        /**
         * Constructs a ColStat from a UnivariateStat.
         * This copies the information from the (potentially resource-heavy)
         * ustat object, but does not retain a reference to it or its members.
         * This constructor does set the median (if quantiles are available),
         * but does not set the MAD, which requires another pass.
         *
         * @param  ustat  univariate statistics object
         * @param  nrow   total number of rows surveyed to acquire statistics
         */
        ColStat( UnivariateStats ustat, long nrow ) {
            ngood_ = ustat.getCount();
            nbad_ = nrow - ngood_;
            sum_ = ustat.getSum();
            min_ = ustat.getMinimum();
            max_ = ustat.getMaximum();
            imin_ = ustat.getMinPos();
            imax_ = ustat.getMaxPos();
            ncard_ = ustat.getCardinality();
            double sum0 = ngood_;
            double sum1 = sum_;
            double sum2 = ustat.getSum2();
            double sum3 = ustat.getSum3();
            double sum4 = ustat.getSum4();
            double dcount = ngood_;
            double rcount = dcount > 0 ? 1. / dcount : Double.NaN;
            double nvar = sum2 - sum1 * sum1 * rcount;
            mean_ = sum1 * rcount;
            popvar_ = nvar * rcount;
            sampvar_ = dcount > 1 ? nvar / ( dcount - 1 ) : Double.NaN;
            skew_ = Math.sqrt( dcount ) / Math.pow( nvar, 1.5 )
                  * ( + 1 * sum3 
                      - 3 * mean_ * sum2 
                      + 3 * mean_ * mean_ * sum1
                      - 1 * mean_ * mean_ * mean_ * sum0 );
            kurt_ = dcount / ( nvar * nvar )
                  * ( + 1 * sum4
                      - 4 * mean_ * sum3
                      + 6 * mean_ * mean_ * sum2
                      - 4 * mean_ * mean_ * mean_ * sum1
                      + 1 * mean_ * mean_ * mean_ * mean_ * sum0 ) - 3.0;
            Quantiler quantiler = ustat.getQuantiler();
            if ( quantiler != null ) {
                median_ = quantiler.getValueAtQuantile( 0.5 );
                quantiles_ = new HashMap<Double,Double>();
                for ( Double qp : NAMED_QUANTILES.keySet() ) {
                    double qv = quantiler.getValueAtQuantile( qp.doubleValue());
                    quantiles_.put( qp, qv );
                }
            }
            else {
                median_ = Double.NaN;
                quantiles_ = null;
            }
            UnivariateStats.ArrayStats arrayStats = ustat.getArrayStats();
            if ( arrayStats == null ) {
                arrayCounts_ = null;
                arraySums_ = null;
                arrayMeans_ = null;
                arrayPopstdevs_ = null;
            }
            else {
                long[] sum0s = arrayStats.getCounts();
                double[] sum1s = arrayStats.getSum1s();
                double[] sum2s = arrayStats.getSum2s();
                int leng = arrayStats.getLength();
                double[] means = new double[ leng ];
                double[] popsds = new double[ leng ];
                for ( int i = 0; i < leng; i++ ) {
                    double acount = sum0s[ i ];
                    double asum1 = sum1s[ i ];
                    double asum2 = sum2s[ i ];
                    double amean = asum1 / acount;
                    double anvar = ( asum2 - asum1 * asum1 / acount );
                    double apopvar = anvar / acount;
                    means[ i ] = amean;
                    popsds[ i ] = Math.sqrt( apopvar );
                }
                arrayCounts_ = sum0s;
                arraySums_ = sum1s;
                arrayMeans_ = means;
                arrayPopstdevs_ = popsds;
            }
            mad_ = Double.NaN;
        }
    }

    /**
     * Helper class whose run method performs the calculations
     * and displays the results in the StatsWindow when it's done.
     * A maximum of one active instance of this is maintained by each 
     * StatsWindow, its <code>run</code> method running in a separate thread.
     */
    private class StatsCalculator implements Runnable {

        private final RowSubset rset_;
        private final boolean hasQuant_;
        private final boolean hasMad_;
        private boolean cancelled_;
        long ngoodrow_;
        ColStat[] colStats_;

        /**
         * Constructs a calculator object which can calculate the statistics
         * of the table owned by this StatsWindow over a given RowSubset.
         *
         * @param  rset the RowSubset to do calculations for
         * @param  hasQuant  true if quantiles need calculating
         * @param  hasMad  true if median absolute deviations need calculating
         */
        public StatsCalculator( RowSubset rset, boolean hasQuant,
                                boolean hasMad ) {
            rset_ = rset;
            hasQuant_ = hasQuant || hasMad; 
            hasMad_ = hasMad;
        }

        /**
         * Messages this calculator to halt pending calculations.
         */
        public void cancel() {
            cancelled_ = true;
        }

        /**
         * Initiates calculations of the requested statistics, and
         * if they complete without interruption arranges for the
         * results to be displayed in the StatsWindow.
         * The cursor is also switched between busy and non-busy at the
         * start and end of calculations as long as this calculator
         * has not been superceded by another in the mean time.
         */
        public void run() {
            if ( cancelled_ ) {
                return;
            }
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( StatsCalculator.this == activeCalculator_ ) {
                        StatsWindow.this.setBusy( true );
                    }
                }
            } );
            try {
                calculate();
                calcMap_.put( rset_, StatsCalculator.this );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        displayCalculations( StatsCalculator.this );
                    }
                } );
            }
            catch ( OutOfMemoryError e ) {
                cancel();
                if ( hasQuant_ && !qapproxModel_.isSelected() ) {
                    final Object msg = new String[] {
                        "Out of memory while calculating quantiles:",
                        "try setting Approximate Quantile Algorithm option."
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
                cancel();
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
            RowRunner runner = ControlWindow.getInstance().getRowRunner();
            StarTable table = new ProgressBarStarTable( dataModel_, progBar_,
                                                        () -> cancelled_ );
            table = SubsetStarTable.createTable( table, rset_ );
            boolean doCard = true;
            final Supplier<Quantiler> qSupplier;
            if ( hasQuant_ ) {
                qSupplier = qapproxModel_.isSelected() ? GKQuantiler::new
                                                       : SortQuantiler::new;
            }
            else {
                qSupplier = null;
            }
            TableStats tstats =
                TableStats.calculateStats( table, runner, qSupplier, doCard );
            long nrow = tstats.getRowCount();
            ngoodrow_ = nrow;
            UnivariateStats[] ustats = tstats.getColumnStats();
            int ncol = ustats.length;
            colStats_ = new ColStat[ ncol ];
            double[] medians = new double[ ncol ];
            int nmed = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                UnivariateStats ustat = ustats[ icol ];
                colStats_[ icol ] = new ColStat( ustat, nrow );
                medians[ icol ] = colStats_[ icol ].median_;
                if ( !Double.isNaN( medians[ icol ] ) ) {
                    nmed++;
                }
            }
            if ( hasMad_ && nmed > 0 ) {
                double[] mads =
                    TableStats.calculateMads( table, runner, qSupplier,
                                              medians );
                for ( int icol = 0; icol < ncol; icol++ ) {
                    colStats_[ icol ].mad_ = mads[ icol ];
                }
            }
        }
    }
}
