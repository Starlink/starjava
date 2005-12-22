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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable; 
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.ProgressBarStarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * A window which displays statistics for a RowSubset in the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StatsWindow extends AuxWindow {

    private TopcatModel tcModel;
    private StarTable dataModel;
    private TableColumnModel columnModel;
    private OptionsListModel subsets;
    private RowSubset rset = RowSubset.ALL;
    private StatsCalculator activeCalculator;
    private StatsCalculator lastCalc;
    private Map calcMap;
    private JTable jtab;
    private JProgressBar progBar;
    private JComboBox subSelector;
    private AbstractTableModel statsTableModel;
    private BitSet hideColumns = new BitSet();

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
        this.tcModel = tcModel;
        this.dataModel = tcModel.getDataModel();
        this.columnModel = tcModel.getColumnModel();
        this.subsets = tcModel.getSubsets();

        /* Set up a map to contain statistic sets that have been calculated. */
        calcMap = new HashMap();

        /* Construct a table model which contains the results of the
         * current calculation. */
        statsTableModel = makeStatsTableModel();

        /* Construct, configure and place the JTable which will form the 
         * main display area. */
        jtab = new JTable( statsTableModel );
        configureJTable( jtab );
        getMainArea().add( new SizingScrollPane( jtab ) );

        /* Customise the JTable's column model to provide control over
         * which columns are displayed. */
        MetaColumnModel statsColumnModel =
            new MetaColumnModel( jtab.getColumnModel(), statsTableModel );
        jtab.setColumnModel( statsColumnModel );

        /* By default, hide some of the less useful columns. */
        int nstat = statsColumnModel.getColumnCount();
        for ( int i = 0; i < nstat; i++ ) {
            if ( hideColumns.get( i ) ) {
                statsColumnModel.removeColumn( i );
            }
        }

        /* Construct and place a widget for selecting which subset to
         * present results for. */
        JPanel controlPanel = getControlPanel();
        subSelector = subsets.makeComboBox();
        subSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    setSubset( (RowSubset) evt.getItem() );
                }
            }
        } );
        controlPanel.add( new JLabel( "Subset for calculations: " ) );
        controlPanel.add( subSelector );

        /* Provide a button for requesting a recalculation. */
        Action recalcAct = new BasicAction( "Recalculate", ResourceIcon.REDO,
                                            "Recalculate the statistics for " +
                                            "the current subset" ) {
            public void actionPerformed( ActionEvent evt ) {
                RowSubset rset = (RowSubset) subSelector.getSelectedItem();
                calcMap.remove( rset );
                setSubset( rset );
            }
        };
        getToolBar().add( recalcAct );
        getToolBar().addSeparator();

        /* Add a menu for statistics operations. */
        JMenu statsMenu = new JMenu( "Statistics" );
        statsMenu.setMnemonic( KeyEvent.VK_S );
        statsMenu.add( new JMenuItem( recalcAct ) );
        getJMenuBar().add( statsMenu );

        /* Add a menu for controlling column display. */
        JMenu displayMenu = statsColumnModel.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Add a progress bar for table scanning. */
        progBar = placeProgressBar();

        /* Add standard help actions. */
        addHelp( "StatsWindow" );

        /* Set the initial subset selection to the model's current subset,
         * which triggers the initial statistics calculation.
         * It doesn't continue to reflect this value (for performance reasons)
         * but it needs an initial value from somewhere. */
        subSelector.setSelectedItem( tcModel.getSelectedSubset() );

        /* Add a trigger to recalculate for a different subset if the
         * global current subset is changed. */
        tcModel.addTopcatListener( new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                if ( evt.getCode() == TopcatEvent.SUBSET ) {
                    subSelector.setSelectedItem( StatsWindow.this.tcModel
                                                .getSelectedSubset() );
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
        this.rset = rset;

        /* In the below, note that this window's calculator object keeps
         * a record of the active calculator.  Any StatsCalculator which
         * is not the active one should be stopped in its tracks rather
         * than continuing to munch cycles.  Keeping track of them like
         * this helps to ensure that there is never more than one active
         * calculator at a time. */

        /* Stop any calculations that are in train, since we will not now
         * need their results. */
        if ( activeCalculator != null ) {
            activeCalculator.interrupt();
        }

        /* Ensure consistency with the subset selector. */
        if ( rset != subSelector.getSelectedItem() ) {
            subSelector.setSelectedItem( rset );
            return;
        }

        /* If we have already done this calculation, display the results
         * directly. */
        if ( calcMap.containsKey( rset ) ) {
            displayCalculations( (StatsCalculator) calcMap.get( rset ) );
        }

        /* Otherwise, kick off a new thread which will perform the
         * calculations and display the results in due course. */
        else {
            activeCalculator = new StatsCalculator( rset );
            activeCalculator.start();
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
        boolean firstTime = lastCalc == null;
        lastCalc = stats;
        statsTableModel.fireTableDataChanged();

        /* First time only, configure the column widths according to 
         * contents. */
        if ( firstTime ) {
            StarJTable.configureColumnWidths( jtab, 200, Integer.MAX_VALUE );
        }
        
        /* Write a heading appropriate to the subset we have the results for. */
        // configureTitle();

        /* Since we've worked out the subset count, update the count model. */
        RowSubset rset = stats.rset;
        long nrow = stats.ngoodrow;
        int irset = subsets.indexOf( rset );
        if ( irset >= 0 ) {
            tcModel.getSubsetCounts().put( rset, new Long( nrow ) );
            subsets.fireContentsChanged( irset, irset );
        }
    }

    /**
     * Extends the dispose method to interrupt any pending calculations.
     */
    public void dispose() {
        super.dispose();
        if ( activeCalculator != null ) {
            activeCalculator.interrupt();
            activeCalculator = null;
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
        return columnModel.getColumn( irow ).getModelIndex();
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
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Index", Integer.class ) {
            public Object getValue( int irow ) {
                return new Integer( irow + 1 );
            }
        } );

        /* $ID. */
        hideColumns.set( metas.size() );
        final ValueInfo idInfo = TopcatUtils.COLID_INFO;
        metas.add( new MetaColumn( idInfo.getName(), String.class ) {
            public Object getValue( int irow ) {
                return ((StarTableColumn) columnModel .getColumn( irow ))
                      .getColumnInfo()
                      .getAuxDatum( idInfo )
                      .getValue();
            }
        } );

        /* Name. */
        metas.add( new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                return dataModel.getColumnInfo( jcol ).getName();
            }
        } );

        /* Sum. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Sum", Double.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                if ( lastCalc.isNumber[ jcol ] ) {
                    return new Double( lastCalc.sums[ jcol ] );
                }
                else if ( lastCalc.isBoolean[ jcol ] ) {
                    return new Long( lastCalc.ntrues[ jcol ] );
                }
                else {
                    return null;
                }
            }
        } );

        /* Mean. */
        metas.add( new MetaColumn( "Mean", Float.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.isNumber[ jcol ] || lastCalc.isBoolean[ jcol ]
                     ? new Float( lastCalc.means[ jcol ] )
                     : null;
            }
        } );

        /* Standard Deviation. */
        metas.add( new MetaColumn( "S.D.", Float.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.isNumber[ jcol ]
                     ? new Float( lastCalc.sdevs[ jcol ] ) 
                     : null;
            }
        } );

        /* Variance. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Variance", Float.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.isNumber[ jcol ]
                     ? new Float( lastCalc.vars[ jcol ] )
                     : null;
            }
        } );

        /* Minimum. */
        metas.add( new MetaColumn( "Minimum", Object.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.mins[ jcol ];
            }
        } );

        /* Row for minimum. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Row of min.", Long.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if  ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.mins[ jcol ] != null
                     ? new Long( lastCalc.imins[ jcol ] )
                     : null;
            }
        } );

        /* Maximum. */
        metas.add( new MetaColumn( "Maximum", Object.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.maxs[ jcol ];
            }
        } );

        /* Row for maximum. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Row of max.", Long.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return lastCalc.maxs[ jcol ] != null
                     ? new Long( lastCalc.imaxs[ jcol ] )
                     : null;
            }
        } );

        /* Count of non-null rows. */
        metas.add( new MetaColumn( "Good cells", Long.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return new Long( lastCalc.ngoods[ jcol ] );
            }
        } );

        /* Count of null rows. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Bad cells", Long.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                return new Long( lastCalc.nbads[ jcol ] );
            }
        } );

        /* Cardinality. */
        hideColumns.set( metas.size() );
        metas.add( new MetaColumn( "Cardinality", Integer.class ) {
            public Object getValue( int irow ) {
                int jcol = getModelIndexFromRow( irow );
                if ( lastCalc == null || jcol >= lastCalc.ncol ) return null;
                int card = lastCalc.cards[ jcol ];
                return ( lastCalc.isCardinal[ jcol ] && card > 0 )
                     ? new Integer( card )
                     : null;
            }
        } );

        /* Construct a new TableModel based on these meta columns. */
        final MetaColumnTableModel tmodel = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnModel.getColumnCount();
            }
        };

        /* Ensure that it responds to changes in the main column model. */
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
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
     * Helper class which performs the calculations in its own thread,
     * and displays the results in the StatsWindow when it's done.
     * A maximum of one active instance of this is maintained by each 
     * StatsWindow, its <tt>run</tt> method running in a separate thread.
     */
    private class StatsCalculator extends Thread {

        private final RowSubset rset;

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
        double[] sdevs;
        double[] vars;
        double[] sums;
        double[] sum2s;
        int[] cards;

        /**
         * Constructs a calculator object which can calculate the statistics
         * of the table owned by this StatsWindow over a given RowSubset.
         *
         * @param  rset the RowSubset to do calculations for
         */
        public StatsCalculator( RowSubset rset ) {
            super( "StatsCalculator" );
            this.rset = rset;
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
                    if ( StatsCalculator.this == activeCalculator ) {
                        StatsWindow.this.setBusy( true );
                    }
                }
            } );
            try {
                calculate();
                calcMap.put( rset, StatsCalculator.this );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        displayCalculations( StatsCalculator.this );
                    }
                } );
            }
            catch ( IOException e ) {
                // no other action
            }
            finally {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( StatsCalculator.this == activeCalculator ) {
                            activeCalculator = null;
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
            ncol = dataModel.getColumnCount();

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
            sdevs = new double[ ncol ];
            vars = new double[ ncol ];
            sums = new double[ ncol ];
            sum2s = new double[ ncol ];
            cards = new int[ ncol ];

            boolean[] badcompars = new boolean[ ncol ];
            double[] dmins = new double[ ncol ];
            double[] dmaxs = new double[ ncol ];
            Set[] valuesets = new Set[ ncol ];
            Arrays.fill( dmins, Double.MAX_VALUE );
            Arrays.fill( dmaxs, -Double.MAX_VALUE );

            /* See which columns we can sensibly gather statistics on. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                Class clazz = dataModel.getColumnInfo( icol ).getContentClass();
                isNumber[ icol ] = Number.class.isAssignableFrom( clazz );
                isComparable[ icol ] = Comparable.class
                                                 .isAssignableFrom( clazz );
                isBoolean[ icol ] = clazz.equals( Boolean.class );
                isCardinal[ icol ] = ! clazz.equals( Boolean.class );
                if ( isCardinal[ icol ] ) {
                    valuesets[ icol ] = new HashSet();
                }
            }

            /* Iterate over the selected rows in the table. */
            RowSequence rseq = new ProgressBarStarTable( dataModel, progBar )
                              .getRowSequence();
            int cardlimit = getCardinalityLimit( dataModel.getRowCount() );
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
                                    sums[ icol ] += dval;
                                    sum2s[ icol ] += dval * dval;
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
                        double mean = sums[ icol ] / ngood;
                        means[ icol ] = mean;
                        double var = sum2s[ icol ] / ngood - mean * mean;
                        vars[ icol ] = var;
                        sdevs[ icol ] = Math.sqrt( var );
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
                }
                else {
                    means[ icol ] = Double.NaN;
                    sdevs[ icol ] = Double.NaN;
                    vars[ icol ] = Double.NaN;
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
