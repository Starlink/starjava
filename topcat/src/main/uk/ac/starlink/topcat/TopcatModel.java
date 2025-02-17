package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
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
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ShapeIterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.activate.ActivationMeta;
import uk.ac.starlink.topcat.activate.ActivationWindow;
import uk.ac.starlink.ttools.convert.Conversions;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.util.gui.ErrorDialog;

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

    private final PlasticStarTable dataModel_;
    private final ViewerTableModel viewModel_;
    private final TableColumnModel columnModel_;
    private final ColumnList columnList_;
    private final OptionsListModel<RowSubset> subsets_;
    private final Map<RowSubset,Long> subsetCounts_;
    private final SingleRowSubset activatedSubset_;
    private final SortOrderSelector.Model sortSelectionModel_;
    private final ComboBoxModel<RowSubset> subsetSelectionModel_;
    private final JToggleButton.ToggleButtonModel sortSenseModel_;
    private final Collection<TopcatListener> listeners_;
    private final Map<ValueInfo,ColumnSelectorModel> columnSelectorMap_;
    private final TableBuilder tableBuilder_;
    private SortOrder sortOrder_;
    private boolean sortSense_;
    private ActivationWindow activationWindow_;
    private SubsetConsumerDialog subsetConsumerDialog_;
    private final int id_;
    private final ControlWindow controlWindow_;
    private String location_;
    private String label_;
    private long lastHighlight_ = -1L;

    private Action newsubsetAct_;
    private Action unsortAct_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static volatile int instanceCount_ = 0;
    private static StarTableColumn DUMMY_COLUMN;

    /**
     * Constructs a new model from a given table.
     * The only row subset available is ALL.
     *
     * @param   startab  random-access table providing the data
     * @param   id       id number; should be unique for loaded tables
     * @param   location  location string
     * @param   controlWindow  control window instance
     */
    @SuppressWarnings("this-escape")
    protected TopcatModel( StarTable startab, int id, String location,
                           ControlWindow controlWindow ) {
        controlWindow_ = controlWindow;

        /* Pull out the information about the input table format that
         * should have been stashed in the table's parametr list
         * by the TopcatTableFactory.  Remove the relevant parameter from
         * the list so that the rest of the application doesn't see it. */
        tableBuilder_ = TopcatPreparation.removeFormatParameter( startab );

        /* Ensure that we have random access. */
        if ( ! startab.isRandom() && startab.getRowCount() != 0 ) {
            throw new IllegalArgumentException( "Can't use non-random table" );
        }

        /* Initialize the label. */
        location_ = location;
        label_ = location_;
        id_ = id;
        if ( label_ == null ) {
            label_ = startab.getName();
        }
        if ( label_ == null ) {
            label_ = "(table)";
        }

        /* Construct a data model based on the StarTable; it is a new
         * StarTable which will also allow some additional functionality 
         * such as column addition. */
        dataModel_ = new PlasticStarTable( startab );

        /* Set up the model which defines what the table view will be. */
        viewModel_ = new ViewerTableModel( dataModel_ );

        /* Set up the column model and column list. */
        columnModel_ = new DefaultTableColumnModel() {

            /* The column model is a normal DefaultTableColumnModel, however
             * we override one method here to work around a bug which appears
             * in (at least) Mac OSX J2SE1.5.0_07 - when columns are deleted 
             * getColumn() is called with a column index of -1 which causes 
             * stack traces to standard error and disrupts the display in
             * various ugly ways.  Here we just intercept such calls and 
             * return a dummy column.  The column doesn't appear to be used
             * for display, which is good, but this in turn causes trouble 
             * in the corresponding TableModel - there is further workaround
             * code in ViewerTableModel. */
            public TableColumn getColumn( int icol ) {
                if ( icol >= 0 ) {
                    return super.getColumn( icol );
                }
                else {

                    /* Don't issue this warning more than once per run. */
                    if ( DUMMY_COLUMN == null ) {
                        logger_.warning( "Attempt to work around "
                                       + "Mac OSX JTable bug" );
                        DUMMY_COLUMN =
                            new StarTableColumn( new ColumnInfo( "DUMMY" ) );
                    }
                    return DUMMY_COLUMN;
                }
            }
        };
        for ( int icol = 0; icol < dataModel_.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = dataModel_.getColumnInfo( icol );
            TableColumn tcol = new StarTableColumn( cinfo, icol );
            columnModel_.addColumn( tcol );
        }
        columnList_ = new ColumnList( columnModel_ );

        /* Set up the current sort selector. */
        sortSenseModel_ = new JToggleButton.ToggleButtonModel();
        sortSelectionModel_ = new SortOrderSelector.Model( this );
        sortSenseModel_.setSelected( true );
        sortSense_ = sortSenseModel_.isSelected();
        sortOrder_ = sortSelectionModel_.getSelectedSortOrder();

        /* Initialise subsets list. */
        activatedSubset_ = new SingleRowSubset( "Activated" );
        subsets_ = new OptionsListModel<RowSubset>();
        subsets_.add( RowSubset.ALL );
        subsets_.add( activatedSubset_ );

        /* Set up the current subset selector. */
        subsetSelectionModel_ = new SubsetSelectionModel();

        /* Initialise count of subsets. */
        subsetCounts_ = new HashMap<RowSubset,Long>();
        subsetCounts_.put( RowSubset.NONE, Long.valueOf( 0 ) );
        subsetCounts_.put( RowSubset.ALL,
                           Long.valueOf( startab.getRowCount() ) );
        subsetCounts_.put( activatedSubset_, Long.valueOf( 0 ) );

        /* Set up a map to contain column selector models. */
        columnSelectorMap_ = new HashMap<ValueInfo,ColumnSelectorModel>();

        /* Install numeric converters as appropriate. */
        for ( int icol = 0; icol < dataModel_.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = dataModel_.getColumnInfo( icol );
            Class<?> clazz = cinfo.getContentClass();
            if ( ! Number.class.isAssignableFrom( clazz ) ) {
                ValueConverter conv = Conversions.getNumericConverter( cinfo );
                if ( conv != null ) {
                    DescribedValue dval =
                        new DescribedValue( TopcatUtils.NUMERIC_CONVERTER_INFO,
                                            conv );
                    cinfo.setAuxDatum( dval );
                }
            }
        }

        /* Create and configure some other actions. */
        newsubsetAct_ = new ModelAction( "New Subset Expression", null,
                                         "Define a new row subset" );
        unsortAct_ = new ModelAction( "Unsort", ResourceIcon.UNSORT,
                                      "Use natural row order" );

        /* Set up the listeners. */
        listeners_ = new ArrayList<TopcatListener>();
    }

    /**
     * Returns the location of the table described by this model.  
     * This is some indication of its provenance, and will not normally
     * change over its lifetime.
     *
     * @return   location
     */
    public String getLocation() {
        return location_;
    }

    /**
     * Returns this model's label.  This is a (short?) string which can
     * be changed by the user, used for human identification of the model.
     *
     * @return   label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the model's ID number.  This is a small sequence number, 
     * typically starting
     * at one and increasing for every model created in this topcat instance.
     * It's used for identifying the model to the user.
     *
     * @return  numeric ID
     */
    public int getID() {
        return id_;
    }

    /**
     * Returns the table builder object that originally loaded
     * this model's table.
     *
     * @return   table origin input handler if known, or null
     */
    public TableBuilder getTableFormat() {
        return tableBuilder_;
    }

    public String toString() {
        return id_ + ": " + getLabel();
    }

    /**
     * Sets the label for model identification.
     *
     * @param  label  new label value
     */
    public void setLabel( String label ) {
        if ( ! equalObject( label, label_ ) ) {
            label_ = label;

            /* Notify listeners. */
            fireModelChanged( TopcatEvent.LABEL, null );
        }
    }

    /**
     * Returns the container for the data held by this viewer.
     * This model, which is a <code>StarTable</code> object, is not
     * affected by changes to the data view such as the order of the results
     * presented in the viewer.  It can have columns added to it but
     * not removed.
     *
     * @return  the data model
     */
    public PlasticStarTable getDataModel() {
        return dataModel_;
    }

    /**
     * Returns the table model which should be used by a <code>JTable</code>
     * for table display.
     * This is based on the <code>dataModel</code>,
     * but can be reordered and configured
     * to display only a subset of the rows and so on.
     *
     * @return  the table model
     */
    public ViewerTableModel getViewModel() {
        return viewModel_;
    }

    /**
     * Returns the table column model which should be used by this a
     * <code>JTable</code> for table display.
     * This can be manipulated either programmatically or as a consequence
     * of user interaction with the JTable (dragging columns around)
     * to modify the mapping of columns visible in this viewer to
     * columns in the dataModel.
     *
     * @return  the column model
     */
    public TableColumnModel getColumnModel() {
        return columnModel_;
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
        return columnList_;
    }

    /**
     * Returns the <code>ListModel</code> which keeps track of which
     * <code>RowSubset</code> objects are available.
     *
     * @return   the RowSubset list model
     */
    public OptionsListModel<RowSubset> getSubsets() {
        return subsets_;
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
    public Map<RowSubset,Long> getSubsetCounts() {
        return subsetCounts_;
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
    public SortOrderSelector.Model getSortSelectionModel() {
        return sortSelectionModel_;
    }

    /**
     * Returns the model indicating whether sorts are up or down.
     * This can be used as the basis for a tickbox or something.
     *
     * @return  sort direction model
     */
    public JToggleButton.ToggleButtonModel getSortSenseModel() {
        return sortSenseModel_;
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
    public ComboBoxModel<RowSubset> getSubsetSelectionModel() {
        return subsetSelectionModel_;
    }

    /**
     * Returns the most recently selected row subset.
     * This is the one which defines the apparent table.
     *
     * @return  current row subset
     */
    public RowSubset getSelectedSubset() {
        return (RowSubset) subsetSelectionModel_.getSelectedItem();
    }

    /**
     * Returns the subset in this model's subset list identified by key.
     *
     * @param  key  subset identifier
     * @return  subset with given key, or null
     */
    public RowSubset getSubsetByKey( RowSubset.Key key ) {
        if ( key != null ) {
            for ( RowSubset subset : subsets_ ) {
                if ( key.equals( subset.getKey() ) ) {
                    return subset;
                }
            }
        }
        return null;
    }

    /**
     * Returns the most recently selected sort order.
     * This is the one which defines the apparent table.
     *
     * @return  current sort order
     */
    public SortOrder getSelectedSort() {
        return sortSelectionModel_.getSelectedSortOrder();
    }

    /**
     * Returns a ColumnSelectorModel which represents the current choice
     * for a given ValueInfo for this table.  A map is maintained,
     * so the same <code>info</code> will always result in getting the
     * same selector model.  If it hasn't been seen before though, a
     * new one will be created.
     *
     * @param  info  description of the column which is wanted
     * @return  model which can be used for selection of a column in this
     *          table with the characteristics of <code>info</code>
     */
    public ColumnSelectorModel getColumnSelectorModel( ValueInfo info ) {
        if ( ! columnSelectorMap_.containsKey( info ) ) {
            columnSelectorMap_.put( info, 
                                    new ColumnSelectorModel( this, info ) );
        }
        return columnSelectorMap_.get( info );
    }

    /**
     * Returns a subset representing the currently activated row.
     *
     * @return  subset
     */
    public SingleRowSubset getActivatedSubset() {
        return activatedSubset_;
    }

    /**
     * Returns the window that manages this model's activation actions.
     *
     * @return  activation window, created lazily
     */
    public ActivationWindow getActivationWindow() {
        if ( activationWindow_ == null ) {
            activationWindow_ =
                new ActivationWindow( this, ControlWindow.getInstance() );
        }
        return activationWindow_;
    }

    /**
     * Indicates whether this model currently has an activation window.
     * If this method returns false, then calling {@link #getActivationWindow}
     * will result in one being created.
     *
     * @return  true iff an activation window has already been created
     *          for this model
     */
    public boolean hasActivationWindow() {
        return activationWindow_ != null;
    }

    /**
     * Adds a listener to be notified of changes in this model.
     *
     * @param  listener  listener to add
     */
    public void addTopcatListener( TopcatListener listener ) {
        listeners_.add( listener );
    }

    /**
     * Removes a listener from notification of changes in this model.
     *
     * @param  listener  listener to remove
     */
    public void removeTopcatListener( TopcatListener listener ) {
        listeners_.remove( listener );
    }

    /**
     * Notifies all registered listeners that this model has changed.
     *
     * @param  code  item code indicating the type of change that has
     *               occurred (one of the static final constants in 
     *               {@link TopcatEvent})
     * @param  datum additional information about the event
     */
    public void fireModelChanged( int code, Object datum ) {
        TopcatEvent evt = new TopcatEvent( this, code, datum );
        for ( TopcatListener l : listeners_ ) {
            l.modelChanged( evt );
        }
    }

    /**
     * Performs all actions required to highlight a row,
     * including notifying external applications via SAMP/PLASTIC
     * if this model is currently so configured.
     *
     * @param  lrow  row index
     */
    public void highlightRow( long lrow ) {
        highlightRow( lrow, true );
    }

    /**
     * Performs actions required to highlight a row,
     * optionally including notifying external applications via SAMP/PLASTIC.
     *
     * @param  lrow  index of the row to activate, or -1 to clear activation
     * @param  sendOut  if true, will notify external applications via
     *         SAMP/PLASTIC when this model is so configured;
     *         if false, no such external notifications will be sent
     */
    public void highlightRow( long lrow, boolean sendOut ) {
        if ( lrow != lastHighlight_ ) {
            lastHighlight_ = lrow;
            activatedSubset_.setRowIndex( lrow );
            updateSubsetCount( activatedSubset_, lrow >= 0 ? 1 : 0 );
            fireModelChanged( TopcatEvent.ROW,
                              lrow >= 0 ? Long.valueOf( lrow ) : null );
            if ( lrow >= 0 ) {
                ActivationMeta meta = sendOut ? ActivationMeta.NORMAL
                                              : ActivationMeta.INHIBIT_SEND;
                getActivationWindow().activateRow( lrow, meta );
            }
        }
    }

    /**
     * Gets an action which will pop up a window for defining a new 
     * algebraic subset for this model.
     * 
     * @return  subset definition action
     */
    public Action getNewSubsetAction() {
        return newsubsetAct_;
    }

    /**
     * Gets an action which will return the view model for this model 
     * to its unsorted state.
     *
     * @return   unsort action
     */
    public Action getUnsortAction() {
        return unsortAct_;
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
        return new BasicAction( "Sort " + ( ascending ? "up" : "down" ),
                                ascending ? ResourceIcon.UP : ResourceIcon.DOWN,
                                "Sort rows by " + ( ascending ? "a" : "de" ) +
                                "scending order of " + order ) {
            public void actionPerformed( ActionEvent evt ) {
                sortBy( order, ascending );
            }
        };
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
        if ( colIndex > dataModel_.getColumnCount() ) {
            throw new IllegalArgumentException();
        }

        /* Add the column to the table model itself. */
        dataModel_.addColumn( col );

        /* Add the new column to the column model. */
        int modelIndex = dataModel_.getColumnCount() - 1;
        TableColumn tc = new StarTableColumn( col.getColumnInfo(), modelIndex );
        columnModel_.addColumn( tc );

        /* Move the new column to the requested position. */
        if ( colIndex >= 0 && colIndex < columnModel_.getColumnCount() - 1 ) {
            columnModel_.moveColumn( columnModel_.getColumnCount() - 1,
                                     colIndex );
        }
        else {
            colIndex = columnModel_.getColumnCount() - 1;
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
        for ( int i = 0; i < columnModel_.getColumnCount(); i++ ) {
            if ( columnModel_.getColumn( i ) == tcol ) { 
                columnModel_.moveColumn( i, i );
            }
        }
    }

    /**
     * Replaces an N-element array-valued column in the table with
     * N scalar-valued columns.  More precisely, it adds N new columns
     * after the original and then hides the original.
     *
     * @param  tcol  vector-valued column
     */
    public void explodeColumn( StarTableColumn tcol ) {
        ColumnInfo baseInfo = tcol.getColumnInfo();
        int insertPos =
             columnList_.getModelIndex( columnList_.indexOf( tcol ) );
        String baseName = baseInfo.getName();
        String baseDesc = baseInfo.getDescription();
        String baseExpr = baseInfo.getAuxDatum( TopcatUtils.COLID_INFO )
                                  .getValue().toString();
        ColumnInfo elInfo = new ColumnInfo( baseInfo );
        elInfo.setShape( null );
        int ipos = 0;
        for ( Iterator<int[]> it = new ShapeIterator( baseInfo.getShape() );
              it.hasNext(); ipos++ ) {
            int[] pos = it.next();
            StringBuffer postxt = new StringBuffer();
            for ( int coord : pos ) {
                postxt.append( '_' );
                postxt.append( Integer.toString( coord + 1 ) );
            }
            ColumnInfo colInfo = new ColumnInfo( elInfo );
            colInfo.setName( baseName + postxt.toString() );
            colInfo.setDescription( "Element " + ( ipos + 1 ) + " of " +
                                    baseName );
            String colExpr = baseExpr + '[' + ipos + ']';
            try {
                SyntheticColumn elcol =
                    new SyntheticColumn( this, colInfo, colExpr, null );
                appendColumn( elcol, ++insertPos );
            }
            catch ( CompilationException e ) {
                throw (AssertionError) new AssertionError( e.getMessage() )
                                      .initCause( e );
            }
        }
        columnModel_.removeColumn( tcol );
    }

    /**
     * Pops up a dialogue to ask the user what to do with a newly created
     * RowSubset.  The user may supply a new or old name which adds it to
     * this model, or may elect to send it to another application via
     * SAMP/PLASTIC.
     *
     * @param   parent  parent component
     * @return   subset consumer, or null if the user doesn't want to play
     */
    public SubsetConsumer enquireNewSubsetConsumer( Component parent ) {
        if ( subsetConsumerDialog_ == null ) {
            subsetConsumerDialog_ =
                new SubsetConsumerDialog( this,
                                          controlWindow_.getCommunicator() );
        }
        return subsetConsumerDialog_.enquireConsumer( parent );
    }

    /**
     * Returns a new editable JComboBox which can be used to select the
     * name of a new RowSubset.  The selectable items in the combo box
     * are the existing RowSubsets for this model, with the exception of
     * RowSubset.ALL, which ought not to be redefined.  The user may either
     * select one of these existing subsets, or may type in a new name.
     * Thus the selectedItem for the returned combo box may be either a
     * String, or an existing RowSubset (which is generally to be used by
     * using its name as the name of a new RowSubset to be added to 
     * this model).
     *
     * @return  a selector to determine the name of a new RowSubset
     */
    public JComboBox<String> createNewSubsetNameSelector() {

        /* Get a selector containing the names of all existing subsets,
         * and doctor its model so that it excludes the special subsets
         * All and Activated. */
        final ComboBoxModel<RowSubset> rsetModel = subsets_.makeComboBoxModel();
        final int nskip = 2;
        assert rsetModel.getElementAt( 0 ) == RowSubset.ALL;
        assert rsetModel.getElementAt( 1 ) == activatedSubset_;
        ComboBoxModel<String> nameModel = new ComboBoxModel<String>() {
            private Object selected;
            public int getSize() {
                return rsetModel.getSize() - nskip;
            }
            public String getElementAt( int index ) {
                return rsetModel.getElementAt( index + nskip ).getName();
            }
            public Object getSelectedItem() {
                return selected;
            }
            public void setSelectedItem( Object item ) {
                selected = item;
            }
            public void addListDataListener( ListDataListener lr ) {
                rsetModel.addListDataListener( lr );
            }
            public void removeListDataListener( ListDataListener lr ) {
                rsetModel.removeListDataListener( lr );
            }
        };
        JComboBox<String> selector = new JComboBox<String>( nameModel );

        /* Set it editable. */
        selector.setEditable( true );

        /* Set no initial default. */
        selector.setSelectedItem( null );

        /* Return. */
        return selector;
    }

    /**
     * Returns a JEL expression evaluation context appropriate for the
     * current state of this table.
     * It may not be updated by future updates to this model,
     * so it should only be used for preparation of evaluation of expressions
     * at call time, not saved for use in compiling expressions acquired later.
     *
     * <p>Note this currently returns a one-size-fits-all implementation,
     * safe for multi-thread use.  But it may not be efficient for
     * multi-thread use (synchronization, contention).
     * This should be fixed so that efficient single-threaded access
     * is possible.
     *
     * @return  row reader
     */
    public TopcatJELRowReader createJELRowReader() {
        return TopcatJELRowReader.createConcurrentReader( this );
    }

    /**
     * Adds a new row subset to the list which this model knows about.
     * If the supplied subset has a name which is the same as an existing
     * one in the list, the new one replaces the old one.  Otherwise,
     * it is appended to the end.
     *
     * @param  rset  the new row subset
     */
    public void addSubset( RowSubset rset ) {

        /* Look for one with a matching name in the list. */
        boolean done = false;
        int nset = subsets_.size();
        for ( int is = 0; is < nset && ! done; is++ ) {
            RowSubset rs = subsets_.get( is );
            if ( rset != RowSubset.ALL &&
                 rset != activatedSubset_ &&
                 rset.getName().equalsIgnoreCase( rs.getName() ) ) {
                rset.setKey( rs.getKey() );
                subsets_.set( is, rset );
                recompileSubsets();
                done = true;
            }
        }

        /* If we didn't find one, append it to the end. */
        if ( ! done ) {
            subsets_.add( rset );
            done = true;
        }
        assert done;

        /* Encourage listeners to flag the new addition/change. */
        showSubset( rset );
    }

    /**
     * Recompiles all synthetic subsets from their expressions.
     * Where this can't be done, a warning is issued.
     */
    public void recompileSubsets() {
        int nset = subsets_.size();
        for ( int is = 0; is < nset; is++ ) {
            RowSubset rs = subsets_.get( is );
            if ( rs instanceof SyntheticRowSubset ) {
                SyntheticRowSubset ss = (SyntheticRowSubset) rs;
                try {
                    ss.setExpression( ss.getExpression() );
                }
                catch ( CompilationException e ) {
                    logger_.warning( "Can't recompile expression "
                                   + ss.getExpression() + " for "
                                   + ss.getName() );
                }
            }
        }
    }

    /**
     * Stores the known row count value for a given subset.
     *
     * @param  rset  subset
     * @param  nrow  row count known for the subset
     */
    public void updateSubsetCount( RowSubset rset, long nrow ) {
        subsetCounts_.put( rset, nrow >= 0 ? Long.valueOf( nrow ) : null );
        int irset = subsets_.indexOf( rset );
        if ( irset >= 0 ) {
            subsets_.fireContentsChanged( irset, irset );
        }
    }

    /**
     * Adds a new table parameter to the table.
     *
     * @param   param new parameter to add to the table
     */
    public void addParameter( DescribedValue param ) {
        dataModel_.getParameters().add( param );
        fireModelChanged( TopcatEvent.PARAMETERS, null );
    }

    /**
     * Removes a table parameter from the table.
     *
     * @param   param  parameter object to remove
     * @return  true if <code>param</code> was removed, false if it wasn't
     *          one of the table parameters in the first place
     */
    public boolean removeParameter( DescribedValue param ) {
        if ( dataModel_.getParameters().remove( param ) ) {
            fireModelChanged( TopcatEvent.PARAMETERS, null );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Trigger a sort of the rows in the viewModel.
     * In case of a material change this causes a
     * {@link TopcatEvent#CURRENT_ORDER} event to be sent to listeners.
     *
     * @param  order  sort order
     * @param  ascending  sort sense (true for up, false for down)
     */
    public void sortBy( SortOrder order, boolean ascending ) {

        /* Transient? */
        if ( order == null ) {
            assert false;
            return;
        }

        /* No change. */
        else if ( order.equals( sortOrder_ ) && ascending == sortSense_ ) {
            return;
        }

        /* There is a material change.  Recalculate the rowMap array. */
        int[] rowMap;
        if ( order.equals( SortOrder.NONE ) ) {
            rowMap = null;
        }
        else if ( order.equals( sortOrder_ ) ) {

            /* If only the sense has changed, reverse the row map
             * in place. */
            assert ascending != sortSense_;
                rowMap = viewModel_.getRowMap();
                for ( int i = 0, j = rowMap.length - 1; i < j; i++, j-- ) {
                    int c = rowMap[ i ];
                    rowMap[ i ] = rowMap[ j ];
                    rowMap[ j ] = c;
                }
        }
        else {
            try {
                rowMap = getSortedRowMap( order, ascending );
            }
            catch ( CompilationException | IOException e ) {
                ErrorDialog.showError( ControlWindow.getInstance(),
                                       "Sort Error", e );
                return;
            }
        }

        /* Store the changed state. */
        sortSense_ = ascending;
        sortOrder_ = order;
        viewModel_.setOrder( rowMap );
        sortSenseModel_.setSelected( ascending );
        sortSelectionModel_.setSelectedSortOrder( order );

        /* Inform listeners. */
        fireModelChanged( TopcatEvent.CURRENT_ORDER, null );
    }

    /**
     * Sets a given row subset to the Current one.  Amongst other things
     * this causes a {@link TopcatEvent#CURRENT_SUBSET} event to be sent
     * to listeners and changes the selection of rows visible in the
     * viewModel.
     *
     * @param  rset  the row subset to use (must be one from the known list)
     */
    public void applySubset( RowSubset rset ) {
        subsetSelectionModel_.setSelectedItem( rset );
    }

    /**
     * Causes a given row subset to be be highlighted in some way.
     * This does not set the current subset, but does cause a 
     * {@link TopcatEvent#SHOW_SUBSET} event to be sent to listeners.
     *
     * @param  rset  the row subset to use (must be one from the known list)
     */
    public void showSubset( RowSubset rset ) {
        fireModelChanged( TopcatEvent.SHOW_SUBSET, rset );
    }

    /**
     * Returns a row mapping array which gives the sort order corresponding
     * to a sort on values in a given column.
     * 
     * @param  order  sort order for the table
     * @param  isAscending  true for ascending sort, false for descending
     */
    private int[] getSortedRowMap( SortOrder order, boolean isAscending )
            throws IOException, CompilationException {
        final int sense = isAscending ? 1 : -1;

        /* Construct an array of all the sort keys. */
        int nv = order.getExpressions().length;
        int nrow =
            AbstractStarTable.checkedLongToInt( dataModel_.getRowCount() );
        TopcatJELRowReader rdr = TopcatJELRowReader.createAccessReader( this );
        CompiledExpression[] compExs =
            JELUtils.compileExpressions( rdr, order.getExpressions() );
        RankedItem[] items = new RankedItem[ nrow ];
        for ( int irow = 0; irow < nrow; irow++ ) {
            Comparable<?>[] values = new Comparable<?>[ nv ];
            for ( int iv = 0; iv < nv; iv++ ) {
                Object value;
                try {
                    value = rdr.evaluateAtRow( compExs[ iv ], irow );
                }
                catch ( Throwable e ) {
                    value = null;
                }
                assert value == null || value instanceof Comparable;
                if ( value instanceof Comparable ) {
                    values[ iv ] = (Comparable<?>) value;
                }
            }
            items[ irow ] = new RankedItem( values, irow );
        }

        /* Sort the list on the ordering of the items. */
        Comparator<RankedItem> comparator = ( item1, item2 ) -> {
            Comparable<?>[] vals1 = item1.values_;
            Comparable<?>[] vals2 = item2.values_;
            for ( int iv = 0; iv < nv; iv++ ) {
                Comparable<?> v1 = vals1[ iv ];
                Comparable<?> v2 = vals2[ iv ];
                final int cmp;
                if ( v1 != null && v2 != null ) {
                    @SuppressWarnings("unchecked")
                    int cmp0 = ((Comparable<Object>) v1).compareTo( v2 );
                    cmp = cmp0;
                }
                else if ( v1 == null && v2 == null ) {
                    cmp = 0;
                }
                else {
                    cmp = v1 == null ? 1 : -1;
                }
                if ( cmp != 0 ) {
                    return sense * cmp;
                }
            }
            return 0;
        };
        Arrays.parallelSort( items, comparator );

        /* Construct and return a list of reordered ranks from the
         * sorted array. */
        int[] rowMap = new int[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = items[ i ].rank_;
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
        int ncol = columnModel_.getColumnCount();
        int[] colMap = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colMap[ icol ] = columnModel_.getColumn( icol ).getModelIndex();
        }
        return new ColumnPermutedStarTable( viewModel_.getSnapshot(), colMap );
    }

    /**
     * Utility method to check equality of two objects without choking
     * on nulls.
     */
    private static boolean equalObject( Object o1, Object o2 ) {
        return o1 == null ? o2 == null : o1.equals( o2 );
    }

    /**
     * Returns a new TopcatModel suitable for a table that has just been
     * loaded in the usual way.  RowSubsets are generated for each boolean
     * column.
     *
     * @param   table    random-access table providing the data
     * @param   location  location string
     * @param   controlWindow  control window instance
     */
    public static TopcatModel
                  createDefaultTopcatModel( StarTable table, String location,
                                            ControlWindow controlWindow ) {

        /* Construct model. */
        TopcatModel tcModel =
            createRawTopcatModel( table, location, controlWindow );

        /* Add subsets for any boolean type columns. */
        StarTable dataModel = tcModel.getDataModel();
        int ncol = dataModel.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            final ColumnInfo cinfo = dataModel.getColumnInfo( icol );
            if ( cinfo.getContentClass() == Boolean.class ) {
                tcModel.subsets_
                       .add( new BooleanColumnRowSubset( dataModel, icol ) );
            }
        }

        /* Return model. */
        return tcModel;
    }

    /**
     * Returns a new TopcatModel based on a table but without some of the
     * additional decorations.
     * In particular no column-based subsets are added.
     *
     * @param   table    random-access table providing the data
     * @param   location  location string
     * @param   controlWindow  control window instance
     */
    public static TopcatModel
                  createRawTopcatModel( StarTable table, String location,
                                        ControlWindow controlWindow ) {
        return new TopcatModel( table, ++instanceCount_,
                                location, controlWindow );
    }

    /**
     * Returns a new TopcatModel that is supposed to be used independently
     * rather than loaded into the main topcat application list.
     *
     * @param  table  random-access table providing the data
     * @param  name   name by which the table will be known
     */
    public static TopcatModel createUnloadedTopcatModel( StarTable table,
                                                         final String name ) {
        return new TopcatModel( table, 0, name, (ControlWindow) null );
    }

    /**
     * Creates and returns a new TopcatModel with no data.
     * This does not increment the count of existing models - it's intended
     * for things like initialising data models which must stop referring
     * to a live TopcatModel.
     *
     * @return  new empty TopcatModel
     */
    public static TopcatModel createDummyModel() {
        return createUnloadedTopcatModel(
            new RowListStarTable( new ColumnInfo[ 0 ] ), "dummy" );
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
            if ( this == newsubsetAct_ ) {
                SubsetQueryWindow.newSubsetDialog( model, parent )
               .setVisible( true );
            }
            else if ( this == unsortAct_ ) {
                sortBy( SortOrder.NONE, true );
            }
            else {
                assert false;
            }
        }
    }

    /**
     * ComboBoxModel used for storing the last-invoked subset selection.
     */
    private class SubsetSelectionModel extends AbstractListModel<RowSubset>
                                       implements ComboBoxModel<RowSubset>, 
                                                  ListDataListener {
        private RowSubset lastSubset_ = RowSubset.ALL;

        SubsetSelectionModel() {
            subsets_.addListDataListener( this );
        }
 
        public Object getSelectedItem() {
            return lastSubset_;
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
            if ( rset.equals( lastSubset_ ) ) {
                return;
            }

            /* OK, we are going to apply the subset. */
            viewModel_.setSubset( rset );

            /* As a side-effect we have calculated the number of rows in 
             * the subset, so update the count model. */
            updateSubsetCount( rset, viewModel_.getRowCount() );

            /* Store the selected value. */
            lastSubset_ = rset;

            /* Make any component displaying this model is updated. */
            fireContentsChanged( this, -1, -1 );

            /* Notify registered listeners to the TopcatModel. */
            fireModelChanged( TopcatEvent.CURRENT_SUBSET, null );
        }

        public RowSubset getElementAt( int index ) {
            return subsets_.getElementAt( index );
        }

        public int getSize() {
            return subsets_.getSize();
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
            if ( ! subsets_.contains( getSelectedItem() ) ) {
                setSelectedItem( subsets_.getElementAt( 0 ) );
            }
        }
    }

    /**
     * Utility class that aggregates a rank and an array, used for sorting.
     */
    private static class RankedItem {
        final Comparable<?>[] values_;
        final int rank_;
        RankedItem( Comparable<?>[] values, int rank ) {
            values_ = values;
            rank_ = rank; 
        }
    }
}
