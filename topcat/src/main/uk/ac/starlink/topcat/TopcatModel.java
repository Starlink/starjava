package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.plot.PlotWindow;

/**
 * Defines all the state for the representation of a given table as
 * viewed by TOPCAT.  As well as the table itself this contains 
 * information about current row ordering, defined subsets, etc.
 * It also constructs and keeps track of windows and actions associated
 * with the table.
 * <p>
 * This is a big ugly mixed bag of various different models.
 * It has crossed my mind to attempt to amalgamate them into something
 * a bit more rational, but the structure of one model containing a
 * set of other (swing-defined) models seems to work OK.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2004
 */
public class TopcatModel {

    private final PlasticStarTable dataModel;
    private final ViewerTableModel viewModel;
    private final TableColumnModel columnModel;
    private final ColumnList columnList;
    private final OptionsListModel subsets;
    private final Map subsetCounts;
    private final OptionsListModel activators;
    private final ComboBoxModel sortSelectionModel;
    private final ComboBoxModel subsetSelectionModel;
    private final ButtonModel sortSenseModel;
    private final Collection listeners;
    private final Map columnSelectorMap;
    private final int id;
    private String location;
    private String label;
    private Activator activator;

    private TableViewerWindow viewerWindow;
    private ParameterWindow paramWindow;
    private ColumnInfoWindow colinfoWindow;
    private StatsWindow statsWindow;
    private SubsetWindow subsetWindow;
    private PlotWindow plotWindow;
    private ActivationQueryWindow activationWindow;

    private Action newsubsetAct;
    private Action unsortAct;
    private TopcatWindowAction viewerAct;
    private TopcatWindowAction paramAct;
    private TopcatWindowAction colinfoAct;
    private TopcatWindowAction statsAct;
    private TopcatWindowAction subsetAct;
    private TopcatWindowAction plotAct;
    private TopcatWindowAction activationAct;
    private TopcatWindowAction[] windowActions;

    private static int instanceCount = 0;

    /**
     * Constructs a new model from a given StarTable.
     *
     * @param   startab  the StarTable
     */
    public TopcatModel( StarTable startab, String location ) {

        /* Ensure that we have random access. */
        if ( ! startab.isRandom() ) {
            throw new IllegalArgumentException( "Can't use non-random table" );
        }

        /* Initialize the label. */
        this.location = location;
        label = location;
        id = ++instanceCount;
        if ( label == null ) {
            label = startab.getName();
        }
        if ( label == null ) {
            label = "(table)";
        }

        /* Construct a data model based on the StarTable; it is a new
         * StarTable which will also allow some additional functionality 
         * such as column addition. */
        dataModel = new PlasticStarTable( startab );

        /* Set up the model which defines what the table view will be. */
        viewModel = new ViewerTableModel( dataModel );

        /* Set up the column model and column list. */
        columnModel = new DefaultTableColumnModel();
        for ( int icol = 0; icol < dataModel.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = dataModel.getColumnInfo( icol );
            TableColumn tcol = new StarTableColumn( cinfo, icol );
            columnModel.addColumn( tcol );
        }
        columnList = new ColumnList( columnModel );

        /* Set up the current sort selector. */
        sortSelectionModel = new SortSelectionModel();
        sortSenseModel = new SortSenseModel();

        /* Initialise subsets list. */
        subsets = new OptionsListModel();
        subsets.add( RowSubset.ALL );

        /* Set up the current subset selector. */
        subsetSelectionModel = new SubsetSelectionModel();

        /* Initialise count of subsets. */
        subsetCounts = new HashMap();
        subsetCounts.put( RowSubset.NONE, new Long( 0 ) );
        subsetCounts.put( RowSubset.ALL, new Long( startab.getRowCount() ) );

        /* Add subsets for any boolean type columns. */
        int ncol = dataModel.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            final ColumnInfo cinfo = dataModel.getColumnInfo( icol );
            if ( cinfo.getContentClass() == Boolean.class ) {
                final int jcol = icol;
                RowSubset yes =
                    new BooleanColumnRowSubset( dataModel, icol);
                subsets.add( yes );
            }
        }

        /* Set up a dummy row activator. */
        activator = Activator.NOP;

        /* Set up a map to contain column selector models. */
        columnSelectorMap = new HashMap();

        /* If there are any activation strings stored in the table, 
         * pull them out and store them. */
        activators = new OptionsListModel();
        DescribedValue actVal = 
            startab.getParameterByName( TopcatUtils.ACTIVATORS_INFO.getName() );
        if ( actVal != null ) {
            Object av = actVal.getValue();
            if ( av instanceof String ) {
                activators.add( av );
            }
            else if ( av instanceof String[] ) {
                for ( int i = 0; i < ((String[]) av).length; i++ ) {
                    activators.add( ((String[]) av)[ i ] );
                }
            }
        }

        /* Create and configure window actions. */
        viewerAct = new TopcatWindowAction( 
                           "Table browser", ResourceIcon.VIEWER,
                           "Display table cell data" );
        paramAct = new TopcatWindowAction( 
                           "Table parameters", ResourceIcon.PARAMS,
                           "Display table metadata" );
        colinfoAct = new TopcatWindowAction(
                           "Column metadata", ResourceIcon.COLUMNS,
                           "Display metadata for each column" );
        statsAct = new TopcatWindowAction(
                           "Column statistics", ResourceIcon.STATS,
                           "Display statistics for each column" );
        subsetAct = new TopcatWindowAction(
                           "Row subsets", ResourceIcon.SUBSETS,
                           "Display row subsets" );
        plotAct = new TopcatWindowAction(
                           "Plot", ResourceIcon.PLOT,
                           "Plot columns from this table" );
        windowActions = new TopcatWindowAction[] {
            viewerAct, paramAct, colinfoAct, statsAct, subsetAct, plotAct,
        };

        /* Create and configure some other actions. */
        newsubsetAct = new ModelAction( "New Subset Expression", null,
                                        "Define a new row subset" );
        unsortAct = new ModelAction( "Unsort", null,
                                     "Use Unsorted Order" );
        activationAct = new TopcatWindowAction( 
                           "Set Activation Action", null,
                           "Set what happens when a row/point is clicked on" );

        /* Set up the listeners. */
        listeners = new ArrayList();
    }

    /**
     * Returns the location of the table described by this model.  
     * This is some indication of its provenance, and will not normally
     * change over its lifetime.
     *
     * @return   location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns this model's label.  This is a (short?) string which can
     * be changed by the user, used for human identification of the model.
     *
     * @return   label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Retursn the model's ID number.  This is a small sequence number, 
     * typically starting
     * at one and increasing for every model created in this topcat instance.
     * It's used for identifying the model to the user.
     *
     * @param  numeric ID
     */
    public int getID() {
        return id;
    }

    public String toString() {
        return id + ": " + getLabel();
    }

    /**
     * Sets the label for model identification.
     *
     * @param  label  new label value
     */
    public void setLabel( String label ) {
        if ( ! equalObject( label, this.label ) ) {
            this.label = label;

            /* Notify listeners. */
            fireModelChanged( TopcatListener.LABEL );
        }
    }

    /**
     * Sets the row activator object.
     *
     * @param  activator  activator to use
     */
    public void setActivator( Activator activator ) {
        if ( ! equalObject( activator, this.activator ) ) {
            this.activator = activator;

            /* Reflect the change in the bound table parameter (so it can
             * be serialized). */
            if ( activator != null && activator != Activator.NOP ) {
                List params = dataModel.getParameters();
                params.remove( dataModel
                              .getParameterByName( TopcatUtils.ACTIVATORS_INFO
                                                  .getName() ) );
                params.add( new DescribedValue( TopcatUtils.ACTIVATORS_INFO,
                                                activator.toString() ) );
            }

            /* Notify listeners. */
            fireModelChanged( TopcatListener.ACTIVATOR );
        }
    }

    /**
     * Returns the row activator object.
     *
     * @return   activator
     */
    public Activator getActivator() {
        return activator;
    }

    /**
     * Returns the list which contains a list of the known activators for
     * this table.  The elements of the list are strings which can be 
     * turned into Activator objects using the {@link #makeActivator} method.
     *
     * @return  list of activator strings which may come in useful for 
     *          this table
     */
    public OptionsListModel getActivatorList() {
        return activators;
    }

    /**
     * Gets an activator object from a string representation of one.
     * This usually involves compiling it using JEL, so it can result in a
     * CompilationException.
     *
     * @param  actstr  string representation of the activator
     * @return  activator object corresponding to <tt>actstr</tt>
     */
    public Activator makeActivator( String actstr )
            throws CompilationException {
        if ( actstr == null || 
             actstr.trim().length() == 0 ||
             actstr.equals( Activator.NOP.toString() ) ) {
            return Activator.NOP;
        }
        else {
            return new JELActivator( this, actstr );
        }
    }

    /**
     * Returns the container for the data held by this viewer.
     * This model, which is a <tt>StarTable</tt> object, is not
     * affected by changes to the data view such as the order of the results
     * presented in the viewer.  It can have columns added to it but
     * not removed.
     *
     * @return  the data model
     */
    public PlasticStarTable getDataModel() {
        return dataModel;
    }

    /**
     * Returns the table model which should be used by a <tt>JTable</tt>
     * for table display.
     * This is based on the <tt>dataModel</tt>,
     * but can be reordered and configured
     * to display only a subset of the rows and so on.
     *
     * @return  the table model
     */
    public ViewerTableModel getViewModel() {
        return viewModel;
    }

    /**
     * Returns the table column model which should be used by this a
     * <tt>JTable</tt> for table display.
     * This can be manipulated either programmatically or as a consequence
     * of user interaction with the JTable (dragging columns around)
     * to modify the mapping of columns visible in this viewer to
     * columns in the dataModel.
     *
     * @return  the column model
     */
    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * Returns the list of columns available from this table.  Unlike a
     * {@link javax.swing.table.TableColumnModel}, this keeps track of
     * all the columns which have ever been in the column model, and 
     * is able to say whether they are currently hidden or not.
     *
     * @return  the column list
     */
    public ColumnList getColumnList() {
        return columnList;
    }

    /**
     * Returns the <tt>ListModel</tt> which keeps track of which
     * <tt>RowSubset</tt> objects are available.
     *
     * @return   the RowSubset list model
     */
    public OptionsListModel getSubsets() {
        return subsets;
    }

    /**
     * Returns the Map which contains the number of rows believed to be
     * in each subset.  The keys of this map are the subsets themselves,
     * and the values are Long objects giving the row counts.
     * If the subset has not been counted, it will not appear in the map.
     * The count in the map may not be accurate, if the table data or
     * subset definitions have changed since the count was last done.
     *
     * @return  subset count map
     */
    public Map getSubsetCounts() {
        return subsetCounts;
    }

    /**
     * Returns the selection model which controls sorts on the table rows.
     * This can be used as the basis for a JComboBox which allows the
     * user to specify a sort.  This model is the primary guardian of
     * the most recent sort, it does not reflect the state of some other
     * holder of that information.
     *
     * @return sort selection model
     */
    public ComboBoxModel getSortSelectionModel() {
        return sortSelectionModel;
    }

    /**
     * Returns the model indicating whether sorts are up or down.
     * This can be used as the basis for a tickbox or something.
     *
     * @return  sort direction model
     */
    public ButtonModel getSortSenseModel() {
        return sortSenseModel;
    }

    /**
     * Returns the selection model which controls the active subset 
     * for the viewed table.  This can be used as the basis of a 
     * JComboBox which allows the user to specify a subset to be applied.
     * This model is the primary guardian of the active subset, it
     * does not reflect the state of some other holder of that information.
     *
     * @return  active row selection model
     */
    public ComboBoxModel getSubsetSelectionModel() {
        return subsetSelectionModel;
    }

    /**
     * Returns the most recently selected row subset.
     * This is the one which defines the apparent table.
     *
     * @param  current row subset
     */
    public RowSubset getSelectedSubset() {
        return (RowSubset) subsetSelectionModel.getSelectedItem();
    }

    /**
     * Returns the most recently selected sort order.
     * This is the one which defines the apparent table.
     *
     * @param  current sort order
     */
    public SortOrder getSelectedSort() {
        return (SortOrder) sortSelectionModel.getSelectedItem();
    }

    /**
     * Returns a ColumnSelectorModel which represents the current choice
     * for a given ValueInfo for this table.  A map is maintained,
     * so the same <tt>info</tt> will always result in getting the
     * same selector model.  If it hasn't been seen before though, a
     * new one will be created.
     *
     * @param  info  description of the column which is wanted
     * @return  model which can be used for selection of a column in this
     *          table with the characteristics of <tt>info</tt>
     */
    public ColumnSelectorModel getColumnSelectorModel( ValueInfo info ) {
        if ( ! columnSelectorMap.containsKey( info ) ) {
            columnSelectorMap.put( info, 
                                   new ColumnSelectorModel( this, info ) );
        }
        return (ColumnSelectorModel) columnSelectorMap.get( info );
    }

    /**
     * Adds a listener to be notified of changes in this model.
     *
     * @param  listener  listener to add
     */
    public void addTopcatListener( TopcatListener listener ) {
        listeners.add( listener );
    }

    /**
     * Removes a listener from notification of changes in this model.
     *
     * @param  listener  listener to remove
     */
    public void removeTopcatListener( TopcatListener listener ) {
        listeners.remove( listener );
    }

    /**
     * Notifies all registered listeners that this model has changed.
     *
     * @param  code  item code indicating the type of change that has
     *               occurred (one of the static final constants in 
     *               {@link TopcatListener})
     */
    public void fireModelChanged( int code ) {
        for ( Iterator it = listeners.iterator(); it.hasNext(); ) {
            ((TopcatListener) it.next()).modelChanged( this, code );
        }
    }

    /**
     * Highlights a row in appropriate view windows.
     *
     * @param  lrow  index of the row to activate
     */
    public void highlightRow( long lrow ) {
        if ( plotWindow != null ) {
            plotWindow.highlightRow( lrow );
        }
        if ( viewerWindow != null ) {
            viewerWindow.highlightRow( lrow );
        }
        if ( activator != null ) {
            String msg = activator.activateRow( lrow );
            if ( msg != null && msg.trim().length() > 0 ) {
                System.out.println( msg );
            }
        }
    }

    /**
     * Gets an action which will pop up a TableViewerWindow associated with
     * this model.
     * 
     * @return   window action
     */
    public WindowAction getViewerAction() {
        return viewerAct;
    }

    /**
     * Gets an action which will pop up a ParameterWindow associated with
     * this model.
     *
     * @return  window action
     */
    public WindowAction getParameterAction() {
        return paramAct;
    }

    /**
     * Gets an actions which will pop up a ColumnInfoWindow associated with
     * this model.
     *
     * @return   window action
     */
    public WindowAction getColumnInfoAction() {
        return colinfoAct;
    }

    /**
     * Gets an action which will pop up a StatsWindow associated with
     * this model.
     *
     * @return  window action
     */
    public WindowAction getStatsAction() {
        return statsAct;
    }

    /**
     * Gets an action which will pop up a SubsetWindow associated with
     * this model.
     *
     * @return   window action
     */
    public WindowAction getSubsetAction() {
        return subsetAct;
    }

    /**
     * Gets an action which will pop up a PlotWindow associated with
     * this model.
     *
     * @return   window action
     */
    public WindowAction getPlotAction() {
        return plotAct;
    }

    /**
     * Gets an action which will pop up a window for defining a new 
     * algebraic subset for this model.
     * 
     * @return  subset definition action
     */
    public Action getNewSubsetAction() {
        return newsubsetAct;
    }

    /**
     * Gets an action which will return the view model for this model 
     * to its unsorted state.
     *
     * @return   unsort action
     */
    public Action getUnsortAction() {
        return unsortAct;
    }

    /**
     * Gets an action which will allow the user to choose what happens
     * if a row is activated.  This probably results in a dialog box
     * or something.
     *
     * @return  activation configuration action
     */
    public Action getActivationAction() {
        return activationAct;
    }

    /**
     * Returns an action which sorts the table on the contents of a given
     * column.  The sort is effected by creating a mapping between model
     * rows and (sorted) view rows, and installing this into this 
     * viewer's data model. 
     *
     * @param  order  sort order
     * @param  ascending  sense of sort (true for up, false for down)
     */
    public Action getSortAction( final SortOrder order, 
                                 final boolean ascending ) {
        TableColumn tcol = order.getColumn();
        return new BasicAction( "Sort " + ( ascending ? "up" : "down" ),
                                ascending ? ResourceIcon.UP : ResourceIcon.DOWN,
                                "Sort rows by " + ( ascending ? "a" : "de" ) +
                                "scending order of " + tcol.getIdentifier() ) {
            public void actionPerformed( ActionEvent evt ) {
                sortBy( order, ascending );
            }
        };
    }

    /**
     * Pops up a modal dialog to ask the user the name for a new RowSubset.
     *
     * @param  parent component, used for positioning
     * @return  a new subset name entered by the user, or <tt>null</tt> if
     *          he bailed out
     */
    public String enquireSubsetName( Component parent ) {
        String name = JOptionPane
                     .showInputDialog( parent, "New subset name",
                                      "Subset Name Input",
                                      JOptionPane.QUESTION_MESSAGE );
        if ( name == null || name.trim().length() == 0 ) {
            return null;
        }
        else {
            return name;
        }
    }

    /**
     * Hides any currently visible view windows associated with this model.
     * The windows are disposed.
     */
    public void hideWindows() {
        for ( int i = 0; i < windowActions.length; i++ ) {
            WindowAction act = windowActions[ i ];
            act.putValue( "VISIBLE", Boolean.FALSE );
        }
    }

    /**
     * Reveals any view windows associated with this window which have
     * been instantiated but may not currently be visible.
     */
    public void revealWindows() {
        for ( int i = 0; i < windowActions.length; i++ ) {
            TopcatWindowAction act = windowActions[ i ];
            if ( act.hasWindow() ) {
                act.putValue( WindowAction.VISIBLE, Boolean.TRUE );
            }
        }
    }

    /**
     * Returns a unique ID string for the given table column, which
     * should be one of the columns in this object's dataModel
     * (though not necessarily its columnModel).  The id will consist
     * of a '$' sign followed by an integer.
     *
     * @param   cinfo column metadata
     * @return  ID string
     */
    public String getColumnID( ColumnInfo cinfo ) {
        return cinfo.getAuxDatum( TopcatUtils.COLID_INFO )
                    .getValue()
                    .toString();
    }

    /**
     * Appends a new column to the existing table at a given column index.
     * This method appends a column to the dataModel, fixes the
     * TableColumnModel to put it in at the right place, and
     * ensures that everybody is notified about what has gone on.
     *  
     * @param  col  the new column
     * @param  colIndex  the column index at which the new column is
     *         to be appended, or -1 for at the end
     */ 
    public void appendColumn( ColumnData col, int colIndex ) {

        /* Check that we are not trying to add the column beyond the end of
         * the table. */
        if ( colIndex > dataModel.getColumnCount() ) {
            throw new IllegalArgumentException();
        }

        /* Add the column to the table model itself. */
        dataModel.addColumn( col );

        /* Add the new column to the column model. */
        int modelIndex = dataModel.getColumnCount() - 1;
        TableColumn tc = new StarTableColumn( col.getColumnInfo(), modelIndex );
        columnModel.addColumn( tc );

        /* Move the new column to the requested position. */
        if ( colIndex >= 0 ) {
            columnModel.moveColumn( columnModel.getColumnCount() - 1,
                                    colIndex );
        }
        else {
            colIndex = columnModel.getColumnCount() - 1;
        }
    }

    /**
     * Appends a new column to the existing table as the last column.
     *          
     * @param  col  the new column
     */ 
    public void appendColumn( ColumnData col ) {
        appendColumn( col, -1 );
    }

    /**
     * Appends all the columns in a given table as new columns in this one.
     *
     * @param  colTable  table containing columns to be grafted onto this
     *         table
     */
    public void appendColumns( final StarTable colTable ) {
        for ( int i = 0; i < colTable.getColumnCount(); i++ ) {
            final int icol = i;
            ColumnData cdata = new ColumnData( colTable.getColumnInfo( i ) ) {
                public Object readValue( long lrow ) throws IOException {
                    return colTable.getCell( lrow, icol );
                }
            };
            appendColumn( cdata, -1 );
        }
    }

    /**
     * Changes the name of a TableColumn in this model.
     * Renaming should be done using this call rather than directly to
     * ensure that all the data is updated properly.
     *
     * @param   tcol  column in this topcat model whose name is to be updated
     * @param   name  new name
     */
    public void renameColumn( TableColumn tcol, String name ) {

        /* Rename the TableColumn and its associated ColumnInfo. */
        tcol.setHeaderValue( name );
        if ( tcol instanceof StarTableColumn ) {
            ((StarTableColumn) tcol).getColumnInfo().setName( name );
        }

        /* This apparent NOP is required to force the TableColumnModel
         * to notify its listeners (importantly the main data JTable)
         * that the column name (headerValue) has changed; there
         * doesn't appear to be an event specifically for this.
         * (Or should I be using bound property changes??) */
        for ( int i = 0; i < columnModel.getColumnCount(); i++ ) {
            if ( columnModel.getColumn( i ) == tcol ) { 
                columnModel.moveColumn( i, i );
            }
        }
    }

    /**
     * Adds a new row subset to the list which this model knows about.
     *
     * @param  rset  the new row subset
     */
    public void addSubset( RowSubset rset ) {
        subsets.add( rset );
    }

    /**
     * Adds a new table parameter to the table.
     *
     * @param   param new parameter to add to the table
     */
    public void addParameter( DescribedValue param ) {
        dataModel.getParameters().add( param );
        fireModelChanged( TopcatListener.PARAMETERS );
    }

    /**
     * Removes a table parameter from the table.
     *
     * @param   param  parameter object to remove
     * @return  true if <tt>param</tt> was removed, false if it wasn't
     *          one of the table parameters in the first place
     */
    public boolean removeParameter( DescribedValue param ) {
        if ( dataModel.getParameters().remove( param ) ) {
            fireModelChanged( TopcatListener.PARAMETERS );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Trigger a sort of the rows in the viewModel.
     *
     * @param  order  sort order
     * @param  ascending  sort sense (true for up, false for down)
     */
    public void sortBy( SortOrder order, boolean ascending ) {
        if ( order != SortOrder.NONE ) {
            sortSenseModel.setSelected( ascending );
        }
        sortSelectionModel.setSelectedItem( order );
    }

    /**
     * Trigger a selection of rows in a given RowSubset for the viewModel.
     *
     * @param  rset  the row subset to use
     */
    public void applySubset( RowSubset rset ) {
        subsetSelectionModel.setSelectedItem( rset );
    }

    /**
     * Returns a row mapping array which gives the sort order corresponding
     * to a sort on values in a given column.
     * 
     * @param  icol  the index of the column to be sorted on in
     *               this viewer's model  
     * @param  ascending  true for ascending sort, false for descending
     */
    private int[] getSortOrder( int icol, final boolean ascending )
            throws IOException { 
        final int sense = ascending ? 1 : -1;
         
        /* Define a little class for objects being sorted. */
        class Item implements Comparable { 
            int rank;
            Comparable value;
            public int compareTo( Object o ) {
                Comparable oval = ((Item) o).value;
                if ( value != null && oval != null ) {
                    return sense * value.compareTo( oval );
                } 
                else if ( value == null && oval == null ) {
                    return 0;
                }
                else {
                    return sense * ( ( value == null ) ? 1 : -1 );
                }
            }
        }

        /* Construct a list of all the elements in the given column. */
        int nrow = AbstractStarTable
                  .checkedLongToInt( dataModel.getRowCount() );
        ColumnData coldata = dataModel.getColumnData( icol );
        Item[] items = new Item[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            Item item = new Item();
            item.rank = i;
            item.value = (Comparable) coldata.readValue( (long) i );
            items[ i ] = item;
        }

        /* Sort the list on the ordering of the items. */
        Arrays.sort( items );

        /* Construct and return a list of reordered ranks from the
         * sorted array. */
        int[] rowMap = new int[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = items[ i ].rank;
        }
        return rowMap;
    }

    /**
     * Returns a StarTable representing the table data as displayed by
     * a JTable looking at this model.  
     * This may differ from the original StarTable object
     * held by it in a number of ways; it may have a different row order,
     * different column orderings, and added or removed columns.
     *
     * @return  a StarTable object representing what this viewer appears
     *          to be showing
     */
    public StarTable getApparentStarTable() {
        int ncol = columnModel.getColumnCount();
        int[] colMap = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colMap[ icol ] = columnModel.getColumn( icol ).getModelIndex();
        }
        return new ColumnPermutedStarTable( viewModel.getSnapshot(), colMap );
    }

    /**
     * Utility method to check equality of two objects without choking
     * on nulls.
     */
    private static boolean equalObject( Object o1, Object o2 ) {
        return o1 == null ? o2 == null : o1.equals( o2 );
    }

    /**
     * Implementations of Actions provided for a TopcatModel.
     */
    private class ModelAction extends BasicAction {

        ModelAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            Component parent = getEventWindow( evt );
            TopcatModel model = TopcatModel.this;
            if ( this == newsubsetAct ) {
                new SyntheticSubsetQueryWindow( model, parent );
            }
            else if ( this == unsortAct ) {
                sortBy( SortOrder.NONE, false );
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Implementations of Actions associated with show/hide of windows.
     */
    private class TopcatWindowAction extends WindowAction {

        TopcatWindowAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public boolean hasWindow() {
            if ( this == viewerAct ) {
                return viewerWindow != null;
            }
            else if ( this == paramAct ) {
                return paramWindow != null;
            }
            else if ( this == colinfoAct ) {
                return colinfoWindow != null;
            }
            else if ( this == statsAct ) {
                return statsWindow != null;
            }
            else if ( this == subsetAct ) {
                return subsetWindow != null;
            }
            else if ( this == plotAct ) {
                return plotWindow != null;
            }
            else if ( this == activationAct ) {
                return activationWindow != null;
            }
            else {
                throw new AssertionError();
            }
        }

        public Window getWindow( Component parent ) {
            TopcatModel tcModel = TopcatModel.this;
            if ( this == viewerAct ) {
                if ( ! hasWindow() ) {
                    viewerWindow = new TableViewerWindow( tcModel, parent );
                }
                return viewerWindow;
            }
            else if ( this == paramAct ) {
                if ( ! hasWindow() ) {
                    paramWindow = new ParameterWindow( tcModel, parent );
                }
                return paramWindow;
            }
            else if ( this == colinfoAct ) {
                if ( ! hasWindow() ) {
                    colinfoWindow = new ColumnInfoWindow( tcModel, parent );
                }
                return colinfoWindow;
            }
            else if ( this == statsAct ) {
                if ( ! hasWindow() ) {
                    statsWindow = new StatsWindow( tcModel, parent );
                }
                return statsWindow;
            }
            else if ( this == subsetAct ) {
                if ( ! hasWindow() ) {
                    subsetWindow = new SubsetWindow( tcModel, parent );
                }
                return subsetWindow;
            }
            else if ( this == plotAct ) {
                if ( ! hasWindow() ) {
                    plotWindow = new PlotWindow( tcModel, parent );
                }
                return plotWindow;
            }
            else if ( this == activationAct ) {
                if ( ! hasWindow() ) {
                    activationWindow = 
                        new ActivationQueryWindow( tcModel, parent );
                }
                return activationWindow;
            }
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * ButtonModel used for storing whether sorts should go up or down.
     */
    private class SortSenseModel extends JToggleButton.ToggleButtonModel {
        boolean lastAscending = true;
        public void setSelected( boolean ascending ) {
            if ( ascending != lastAscending ) {

                /* If the table view has a current (non-null) sort order, 
                 * reverse it in place. */
                int[] rowMap = viewModel.getRowMap();
                if ( rowMap != null ) {
                    for ( int i = 0, j = rowMap.length - 1; i < j; i++, j-- ) {
                        int c = rowMap[ i ];
                        rowMap[ i ] = rowMap[ j ];
                        rowMap[ j ] = c;
                    }
                    viewModel.setRowMap( rowMap );
                }

                /* Store the changed state. */
                lastAscending = ascending;
                fireStateChanged();
            }
        }
        public boolean isSelected() {
            return lastAscending;
        }
    }

    /**
     * ComboBoxModel used for storing the available and last-invoked
     * sort orders.
     */
    private class SortSelectionModel extends RestrictedColumnComboBoxModel {

        private SortOrder lastSort = SortOrder.NONE;

        SortSelectionModel() {
            super( columnModel, true );
        }

        /**
         * Turns a column identifier into a sort order definition.
         */
        public Object getElementAt( int index ) {
            return new SortOrder( (TableColumn) super.getElementAt( index ) );
        }

        /**
         * Defines which columns can be sorted on - only the comparable ones.
         */
        public boolean acceptColumn( ColumnInfo cinfo ) {
            Class clazz = cinfo.getContentClass();
            return Comparable.class.isAssignableFrom( clazz );
        }

        /**
         * Returns the most recent selected sort. 
         */
        public Object getSelectedItem() {
            return lastSort;
        }

        /**
         * Selecting an item in this model triggers the actual sort.
         * All sorts pass through here.
         */
        public void setSelectedItem( Object item ) {
            SortOrder order = (SortOrder) item;

            /* Do nothing if the selected item is being set null - this
             * corresponds to a JComboBox deselection, which happens 
             * immediately prior to a selection when the control is
             * activated. */
            if ( order == null ) {
                return;
            }

            /* Do nothing if the order is the same one we've just had. */
            if ( order.equals( lastSort ) ) {
                return;
            }

            /* OK do the sort, and install it in the viewModel. */
            int[] rowMap;
            if ( order.equals( SortOrder.NONE ) ) {
                rowMap = null;
            }
            else {
                TableColumn tcol = order.getColumn();
                try {
                    rowMap = getSortOrder( tcol.getModelIndex(), 
                                           sortSenseModel.isSelected() );
                }
                catch ( IOException e ) {
                    Toolkit.getDefaultToolkit().beep();
                    e.printStackTrace( System.err );
                    setSelectedItem( SortOrder.NONE );
                    return;
                }
            }

            /* Install the new sorted order in the table view model. */
            viewModel.setOrder( rowMap );

            /* Store the selected value. */
            lastSort = order;

            /* Make sure any component displaying this model is
             * updated (this call copied from Swing source). */
            fireContentsChanged( this, -1, -1 );
        }
    }


    /**
     * ComboBoxModel used for storing the last-invoked subset selection.
     */
    private class SubsetSelectionModel extends AbstractListModel
                                       implements ComboBoxModel, 
                                                  ListDataListener {
        private RowSubset lastSubset = RowSubset.ALL;

        SubsetSelectionModel() {
            subsets.addListDataListener( this );
        }
 
        public Object getSelectedItem() {
            return lastSubset;
        }

        /**
         * Selecting an item in this model triggers the actual selection.
         * All current subset selections pass through here.
         */
        public void setSelectedItem( Object item ) {
            RowSubset rset = (RowSubset) item;

            /* Do nothing if the selected item is being set null - this
             * corresponds to a JComboBox deselection, which happens
             * immediately prior to a selection when the control is
             * activated. */
            if ( rset == null ) {
                return;
            }

            /* Do nothing if the subset is the same as the currently active 
             * one. */
            if ( rset.equals( lastSubset ) ) {
                return;
            }

            /* OK, we are going to apply the subset. */
            viewModel.setSubset( rset );

            /* As a side-effect we have calculated the number of rows in 
             * the subset, so update the count model. */
            subsetCounts.put( rset, new Long( viewModel.getRowCount() ) );
            int irset = subsets.indexOf( rset );
            if ( irset >= 0 ) {
                subsets.fireContentsChanged( irset, irset );
            }

            /* Store the selected value. */
            lastSubset = rset;

            /* Make any component displaying this model is updated. */
            fireContentsChanged( this, -1, -1 );
        }

        public Object getElementAt( int index ) {
            return subsets.getElementAt( index );
        }

        public int getSize() {
            return subsets.getSize();
        }

        /*
         * Propagate listener events from the subsets list to our listeners.
         */
        public void contentsChanged( ListDataEvent evt ) {
            fireContentsChanged( this, evt.getIndex0(), evt.getIndex1() );
        }
        public void intervalAdded( ListDataEvent evt ) {
            fireIntervalAdded( this, evt.getIndex0(), evt.getIndex1() );
        }
        public void intervalRemoved( ListDataEvent evt ) {
            fireIntervalRemoved( this, evt.getIndex0(), evt.getIndex1() );
        }
    }
}
