package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.ProgressBarStarTable;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * A window which displays statistics for a RowSubset in the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StatsWindow extends AuxWindow {

    private TableViewer tv;
    private PlasticStarTable dataModel;
    private TableColumnModel columnModel;
    private OptionsListModel subsets;
    private RowSubset rset = RowSubset.ALL;
    private StatsCalculator activeCalculator;
    private Map calcMap;
    private JTable jtab;
    private JProgressBar progBar;
    private JComboBox subSelector;

    /**
     * Constructs a StatsWindow to report on the statistics of data in a 
     * given TableViewer.  Initially, no results are displayed; call
     * the {@link #setSubset} method to show some statistics.
     *
     * @param  tableviewer  the viewer whose data are to be analysed
     */
    public StatsWindow( TableViewer tableviewer ) {
        super( "Row statistics", tableviewer );
        this.tv = tableviewer;
        this.dataModel = tv.getDataModel();
        this.columnModel = tv.getColumnModel();
        this.subsets = tv.getSubsets();
        JPanel mainArea = getMainArea();

        /* Set up a map to contain statistic sets that have been calculated. */
        calcMap = new HashMap();

        /* Construct and place the JTable which will form the main display
         * area. */
        jtab = new JTable( new StatsTableModel( null ) );
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( false );
        new StatsTableModel( null ).configureJTable( jtab );
        mainArea.add( new SizingScrollPane( jtab ) );

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

        /* Construct and place a widget for requesting a recalculation. */
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

        /* Menus. */
        JMenu statsMenu = new JMenu( "Statistics" );
        statsMenu.add( new JMenuItem( recalcAct ) ).setIcon( null );
        getJMenuBar().add( statsMenu );
  
        /* Add a progress bar for table scanning. */
        progBar = placeProgressBar();

        /* Add standard help actions. */
        addHelp( "StatsWindow" );

        /* Make the component visible. */
        pack();
        setVisible( true );
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
        if ( rset !=  subSelector.getSelectedItem() ) {
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
        StatsTableModel model = new StatsTableModel( stats );
        jtab.setModel( model );
        model.configureJTable( jtab );
        RowSubset rset = stats.rset;
        long nrow = stats.ngoodrow;
        String head = new StringBuffer()
            .append( "Statistics for " )
            .append( rset == RowSubset.ALL 
                          ? "all rows" : ( "row subset: " + rset.getName() ) )
            .append( " (" )
            .append( nrow )
            .append( ' ' )
            .append( nrow == 1L ? "row" : "rows" )
            .append( ')' )
            .toString();
        setMainHeading( head );
    }

    /**
     * Extends the dispose method to interrupt any pending calculations.
     */
    public void dispose() {
        super.dispose();
        if ( activeCalculator != null ) {
            activeCalculator.interrupt();
            activeCalculator = null;
        }
    }

    /**
     * Helper class which performs the calculations in its own thread,
     * and displays the results in the StatsWindow when it's done.
     * One instance of this is maintained by each StatsWindow 
     * its <tt>run</tt> method running in a separate thread.
     */
    private class StatsCalculator extends Thread {

        private final RowSubset rset;

        long ngoodrow;
        Object[] mins;
        Object[] maxs;
        long[] ngoods;
        long[] nbads;
        double[] means;
        double[] sdevs;
        double[] sums;
        double[] sum2s;

        /**
         * Constructs a calculator object which can calculate the statistics
         * of the table owned by this StatsWindow over a given RowSubset.
         *
         * @param  rset the RowSubset to do calculations for
         */
        public StatsCalculator( RowSubset rset ) {
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
            int ncol = dataModel.getColumnCount();

            /* Allocate result objects. */
            mins = new Object[ ncol ];
            maxs = new Object[ ncol ];
            ngoods = new long[ ncol ];
            nbads = new long[ ncol ];
            means = new double[ ncol ];
            sdevs = new double[ ncol ];
            sums = new double[ ncol ];
            sum2s = new double[ ncol ];

            boolean[] badcompars = new boolean[ ncol ];
            boolean[] isNumber = new boolean[ ncol ];
            boolean[] isComparable = new boolean[ ncol ];
            double[] dmins = new double[ ncol ];
            double[] dmaxs = new double[ ncol ];
            Arrays.fill( dmins, Double.MAX_VALUE );
            Arrays.fill( dmaxs, -Double.MAX_VALUE );

            /* See which columns we can sensibly gather statistics on. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                Class clazz = dataModel.getColumnInfo( icol ).getContentClass();
                isNumber[ icol ] = Number.class.isAssignableFrom( clazz );
                isComparable[ icol ] = Comparable.class
                                                 .isAssignableFrom( clazz );
            }

            /* Iterate over the selected rows in the table. */
            RowSequence rseq = new ProgressBarStarTable( dataModel, progBar )
                              .getRowSequence();
            IOException interruption = null;
            long lrow = 0L;
            ngoodrow = 0L;
            for ( ; rseq.hasNext(); lrow++ ) {

                /* A thread interruption may manifest itself here as an 
                 * exception (see ProgressBarStarTable).  If so, save the
                 * exception and break out. */
                try {
                    rseq.next();
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
                                if ( ! ( val instanceof Number ) ) {
                                    System.err.println( 
                                        "Error in table data: not numeric at " +
                                        lrow + "," + icol + "(" + val + ")" );
                                    good = false;
                                }
                                double dval = ((Number) val).doubleValue();
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
                                    }
                                    if ( dval > dmaxs[ icol ] ) {
                                        dmaxs[ icol ] = dval;
                                        maxs[ icol ] = val;
                                    }
                                    sums[ icol ] += dval;
                                    sum2s[ icol ] += dval * dval;
                                }
                            }
                            else if ( isComparable[ icol ] ) {
                                if ( ! ( val instanceof Comparable ) ) {
                                    System.err.println(
                                        "Error in table data: not Comparable " +
                                        " at " + lrow + "," + icol + "(" +
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
                                    }
                                    else {
                                        try {
                                            if ( cval.compareTo( mins[ icol ] ) 
                                                 < 0 ) {
                                                mins[ icol ] = val;
                                            }
                                            else if ( cval
                                                     .compareTo( maxs[ icol ] )
                                                      > 0 ) {
                                                maxs[ icol ] = val;
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
                            else {
                                good = true;
                            }
                            if ( good ) {
                                ngoods[ icol ]++;
                            }
                        }
                    }
                }
            }
            long nrow = lrow;

            /* Calculate the actual statistics based on the accumulated
             * values.  We do this even if the summation was interrupted,
             * since the partially-accumulated values may be of interest. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                long ngood = ngoods[ icol ];
                nbads[ icol ] = ngoodrow - ngood;
                if ( ngood > 0 ) {
                    double mean = sums[ icol ] / ngood;
                    means[ icol ] = mean;
                    double var = sum2s[ icol ] / ngood - mean * mean;
                    sdevs[ icol ] = Math.sqrt( var );
                }
                else {
                    means[ icol ] = Double.NaN;
                    sdevs[ icol ] = Double.NaN;
                }
                if ( badcompars[ icol ] ) {
                    mins[ icol ] = null;
                    maxs[ icol ] = null;
                }
            }

            /* Re-throw any interruption-type exception we picked up. */
            if ( interruption != null ) {
                throw interruption;
            }
        }
    }

    /**
     * Helper class which provides a TableModel view of a StatsCalculator
     * object.
     */
    private class StatsTableModel extends AbstractTableModel
                                  implements TableColumnModelListener {

        private static final int NAME_COL = 0;
        private static final int MEAN_COL = 1;
        private static final int SDEV_COL = 2;
        private static final int MIN_COL = 3;
        private static final int MAX_COL = 4;
        private static final int NGOOD_COL = 5;
        private static final int NBAD_COL = 6;
        private static final int NCOL = 7;
        private StatsCalculator calc;

        /**
         * Constructs a new StatsTableModel from a StatsCalculator
         * <tt>calc</tt>.  The calculator should have finished its
         * calculations (or at least been interrupted during them).
         * A null <tt>calc</tt> may be used to represent no available data.
         *
         * @param  calc   the StatsCalculator
         */
        public StatsTableModel( StatsCalculator calc ) {
            this.calc = calc;

            /* Ensure that changes to the column model are reflected in
             * this table. */
            columnModel.addColumnModelListener( this );
        }

        public int getColumnCount() {
            return NCOL;
        }

        public int getRowCount() {
            return columnModel.getColumnCount();
        }

        public Object getValueAt( int irow, int icol ) {
            int jcol = getModelIndexFromRow( irow );
            if ( icol == NAME_COL ) {
                return dataModel.getColumnInfo( jcol ).getName();
            }
            else if ( calc == null ) {
                return null;
            }
            switch ( icol ) {
                case NGOOD_COL:   return new Long( calc.ngoods[ jcol ] );
                case NBAD_COL:    return new Long( calc.nbads[ jcol ] );
                case MEAN_COL:    return new Float( calc.means[ jcol ] );
                case SDEV_COL:    return new Float( calc.sdevs[ jcol ] );
                case MIN_COL:     return calc.mins[ jcol ];
                case MAX_COL:     return calc.maxs[ jcol ];
                default:          throw new AssertionError();
            }
        }

        public String getColumnName( int icol ) {
            switch ( icol ) {
                case NAME_COL:    return "Name";
                case NGOOD_COL:   return "Good cells";
                case NBAD_COL:    return "Bad cells";
                case MEAN_COL:    return "Mean";
                case SDEV_COL:    return "S.D.";
                case MIN_COL:     return "Minimum";
                case MAX_COL:     return "Maximum";
                default:          throw new AssertionError();
            }
        }

        public Class getColumnClass( int icol ) {
            switch ( icol ) {
                case NAME_COL:    return String.class;
                case NGOOD_COL:   return Long.class;
                case NBAD_COL:    return Long.class;
                case MEAN_COL:    return Float.class;
                case SDEV_COL:    return Float.class;
                case MIN_COL:     return Object.class;
                case MAX_COL:     return Object.class;
                default:          throw new AssertionError();
            }
        }

        /**
         * Performs some cosmetic configuration on a JTable which will
         * be used to view this table model.
         *
         * @param   jtab   the JTable to be configured
         */
        public void configureJTable( JTable jtab ) {
            TableColumnModel tcm = jtab.getColumnModel();
            for ( int icol = 0; icol < NCOL; icol++ ) {
                Class clazz = getColumnClass( icol );
                if ( clazz.equals( Long.class ) ) {
                    clazz = Integer.class;
                }
                else if ( clazz.equals( Object.class ) ) {
                    clazz = Double.class;
                }
                NumericCellRenderer rend = new NumericCellRenderer( clazz );
                TableColumn tcol = tcm.getColumn( icol );
                tcol.setCellRenderer( rend );
                int wid;
                if ( icol == NAME_COL ) {
                    wid = 160;
                }
                else {
                    wid = rend.getCellWidth();
                }
                tcol.setPreferredWidth( wid );
            } 
        }

        private int getModelIndexFromRow( int irow ) {
            return columnModel.getColumn( irow ).getModelIndex();
        }

        /*
         * Implementation of TableColumnModelListener interface.
         * A more efficient implementation would be possible, but this
         * is hardly going to be a performance bottleneck.
         */
        public void columnAdded( TableColumnModelEvent evt ) {
            fireTableDataChanged();
        }
        public void columnRemoved( TableColumnModelEvent evt ) {
            fireTableDataChanged();
        }
        public void columnMoved( TableColumnModelEvent evt ) {
            fireTableDataChanged();
        }
        public void columnMarginChanged( ChangeEvent evt ) {}
        public void columnSelectionChanged( ListSelectionEvent evt ) {}
        
    }

}
