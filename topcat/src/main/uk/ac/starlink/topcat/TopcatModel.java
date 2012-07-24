package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import uk.ac.starlink.table.ShapeIterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.convert.Conversions;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.topcat.interop.RowActivity;

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
    private final OptionsListModel subsets_;
    private final Map subsetCounts_;
    private final OptionsListModel activators_;
    private final ComboBoxModel sortSelectionModel_;
    private final ComboBoxModel subsetSelectionModel_;
    private final SortSenseModel sortSenseModel_;
    private final ToggleButtonModel rowSendModel_;
    private final Collection listeners_;
    private final Map columnSelectorMap_;
    private SubsetConsumerDialog subsetConsumerDialog_;
    private final int id_;
    private final ControlWindow controlWindow_;
    private String location_;
    private String label_;
    private Activator activator_;
    private long lastHighlight_ = -1L;

    private ActivationQueryWindow activationWindow_;

    private Action newsubsetAct_;
    private Action unsortAct_;
    private TopcatWindowAction activationAct_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static volatile int instanceCount = 0;
    private static StarTableColumn DUMMY_COLUMN;
    private static RowActivity rowActivity_;

    /**
     * Constructs a new model from a given table.
     * The only row subset available is ALL.
     *
     * @param   startab  random-access table providing the data
     * @param   location  location string
     * @param   controlWindow  control window instance
     */
    protected TopcatModel( StarTable startab, String location,
                           ControlWindow controlWindow ) {
        controlWindow_ = controlWindow;

        /* Ensure that we have random access. */
        if ( ! startab.isRandom() && startab.getRowCount() != 0 ) {
            throw new IllegalArgumentException( "Can't use non-random table" );
        }

        /* Initialize the label. */
        location_ = location;
        label_ = location_;
        id_ = ++instanceCount;
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
        sortSelectionModel_ = new SortSelectionModel();
        sortSenseModel_ = new SortSenseModel();

        /* Set up model for whether activated rows are broadcast. */
        rowSendModel_ =
            new ToggleButtonModel( "Broadcast Row", null,
                                   "Whether to broadcast row index to other "
                                 + "clients at at row activation" );

        /* Initialise subsets list. */
        subsets_ = new OptionsListModel();
        subsets_.add( RowSubset.ALL );

        /* Set up the current subset selector. */
        subsetSelectionModel_ = new SubsetSelectionModel();

        /* Initialise count of subsets. */
        subsetCounts_ = new HashMap();
        subsetCounts_.put( RowSubset.NONE, new Long( 0 ) );
        subsetCounts_.put( RowSubset.ALL, new Long( startab.getRowCount() ) );

        /* Set up a dummy row activator. */
        activator_ = Activator.NOP;

        /* Set up a map to contain column selector models. */
        columnSelectorMap_ = new HashMap();

        /* If there are any activation strings stored in the table, 
         * pull them out and store them. */
        activators_ = new OptionsListModel();

        // Don't do this for now - the string version doesn't isn't always 
        // in a suitable format.
        // DescribedValue actVal = 
        //     startab.getParameterByName( TopcatUtils.ACTIVATORS_INFO
        //                                            .getName() );
        // if ( actVal != null ) {
        //     Object av = actVal.getValue();
        //     if ( av instanceof String ) {
        //         activators_.add( av );
        //     }
        //     else if ( av instanceof String[] ) {
        //         for ( int i = 0; i < ((String[]) av).length; i++ ) {
        //             activators_.add( ((String[]) av)[ i ] );
        //         }
        //     }
        // }

        /* Install numeric converters as appropriate. */
        for ( int icol = 0; icol < dataModel_.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = dataModel_.getColumnInfo( icol );
            Class clazz = cinfo.getContentClass();
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
        activationAct_ = new TopcatWindowAction( 
                           "Set Activation Action", null,
                           "Set what happens when a row/point is clicked on" );

        /* Set up the listeners. */
        listeners_ = new ArrayList();
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
     * Retursn the model's ID number.  This is a small sequence number, 
     * typically starting
     * at one and increasing for every model created in this topcat instance.
     * It's used for identifying the model to the user.
     *
     * @return  numeric ID
     */
    public int getID() {
        return id_;
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
     * Sets the row activator object.
     *
     * @param  activator  activator to use
     */
    public void setActivator( Activator activator ) {
        if ( ! equalObject( activator, activator_ ) ) {
            activator_ = activator;

            /* Reflect the change in the bound table parameter (so it can
             * be serialized). */
            if ( activator_ != null && activator_ != Activator.NOP ) {
                List params = dataModel_.getParameters();
                params.remove( dataModel_
                              .getParameterByName( TopcatUtils.ACTIVATORS_INFO
                                                  .getName() ) );
                params.add( new DescribedValue( TopcatUtils.ACTIVATORS_INFO,
                                                activator_.toString() ) );
            }

            /* Notify listeners. */
            fireModelChanged( TopcatEvent.ACTIVATOR, null );
        }
    }

    /**
     * Returns the row activator object.
     *
     * @return   activator
     */
    public Activator getActivator() {
        return activator_;
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
        return activators_;
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
        return dataModel_;
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
        return viewModel_;
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
     * Returns the <tt>ListModel</tt> which keeps track of which
     * <tt>RowSubset</tt> objects are available.
     *
     * @return   the RowSubset list model
     */
    public OptionsListModel getSubsets() {
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
    public Map getSubsetCounts() {
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
    public ComboBoxModel getSortSelectionModel() {
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
    public ComboBoxModel getSubsetSelectionModel() {
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
     * Returns the most recently selected sort order.
     * This is the one which defines the apparent table.
     *
     * @return  current sort order
     */
    public SortOrder getSelectedSort() {
        return (SortOrder) sortSelectionModel_.getSelectedItem();
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
        if ( ! columnSelectorMap_.containsKey( info ) ) {
            columnSelectorMap_.put( info, 
                                    new ColumnSelectorModel( this, info ) );
        }
        return (ColumnSelectorModel) columnSelectorMap_.get( info );
    }

    /**
     * Returns a toggle button model which determines whether a row 
     * activation will automatically generate an interop row highlight message.
     *
     * @return   row send model
     */
    public ToggleButtonModel getRowSendModel() {
        return rowSendModel_;
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
        for ( Iterator it = listeners_.iterator(); it.hasNext(); ) {
            ((TopcatListener) it.next()).modelChanged( evt );
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
     * @param  lrow  index of the row to activate
     * @param  sendOut  if true, will notify external applications via
     *         SAMP/PLASTIC when this model is so configured;
     *         if false, no such external notifications will be sent
     */
    public void highlightRow( long lrow, boolean sendOut ) {
        if ( lrow != lastHighlight_ ) {
            fireModelChanged( TopcatEvent.ROW, new Long( lrow ) );
            if ( activator_ != null ) {
                String msg = activator_.activateRow( lrow );
                if ( msg != null && msg.trim().length() > 0 ) {
                    System.out.println( msg );
                }
            }
            if ( sendOut && 
                 rowSendModel_.isSelected() &&
                 controlWindow_.getCommunicator() != null &&
                 controlWindow_.getCommunicator().isConnected() ) {
                try {
                    getRowActivity().highlightRow( this, lrow );
                }
                catch ( IOException e ) {
                    logger_.info( "Row send fail: " + e );
                }
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
     * Gets an action which will allow the user to choose what happens
     * if a row is activated.  This probably results in a dialog box
     * or something.
     *
     * @return  activation configuration action
     */
    public Action getActivationAction() {
        return activationAct_;
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
        if ( colIndex >= 0 ) {
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
        RandomJELRowReader rowReader =
            new TopcatJELRowReader( dataModel_,
                                    new RowSubset[ 0 ], new int[ 0 ] );
        elInfo.setShape( null );
        int ipos = 0;
        for ( Iterator it = new ShapeIterator( baseInfo.getShape() );
              it.hasNext(); ipos++ ) {
            int[] pos = (int[]) it.next();
            StringBuffer postxt = new StringBuffer();
            for ( int i = 0; i < pos.length; i++ ) {
                postxt.append( '_' );
                postxt.append( Integer.toString( pos[ i ] + 1 ) );
            }
            ColumnInfo colInfo = new ColumnInfo( elInfo );
            colInfo.setName( baseName + postxt.toString() );
            colInfo.setDescription( "Element " + ( ipos + 1 ) + " of " +
                                    baseName );
            String colExpr = baseExpr + '[' + ipos + ']';
            try {
                SyntheticColumn elcol =
                    new SyntheticColumn( colInfo, colExpr, null, rowReader );
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
    public JComboBox createNewSubsetNameSelector() {

        /* Get a selector containing the names of all existing subsets. */
        JComboBox selector = subsets_.makeComboBox();

        /* Doctor its model so that it excludes RowSubset.ALL. */
        final ComboBoxModel baseModel = selector.getModel();
        Object item0 = baseModel.getElementAt( 0 );
        if ( item0 == RowSubset.ALL ) {
            selector.setModel( new ComboBoxModel() {
                public int getSize() {
                    return baseModel.getSize() - 1;
                }
                public Object getElementAt( int index ) {
                    return baseModel.getElementAt( index >= 0 ? index + 1
                                                              : index );
                }
                public Object getSelectedItem() {
                    return baseModel.getSelectedItem();
                }
                public void setSelectedItem( Object item ) {
                    baseModel.setSelectedItem( item );
                }
                public void addListDataListener( ListDataListener lr ) {
                    baseModel.addListDataListener( lr );
                }
                public void removeListDataListener( ListDataListener lr ) {
                    baseModel.removeListDataListener( lr );
                }
            } );
        }
        else {
            assert false;
        }

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
     * @return  row reader
     */
    public RandomJELRowReader createJELRowReader() {
        int nset = subsets_.size();
        RowSubset[] subsetArray = new RowSubset[ nset ];
        int[] subsetIds = new int[ nset ];
        for ( int isub = 0; isub < nset; isub++ ) {
            subsetArray[ isub ] = (RowSubset) subsets_.get( isub );
            subsetIds[ isub ] = subsets_.indexToId( isub );
        }
        return new TopcatJELRowReader( dataModel_, subsetArray, subsetIds );
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
            RowSubset rs = (RowSubset) subsets_.get( is );
            if ( rset != RowSubset.ALL &&
                 rset.getName().equals( rs.getName() ) ) {
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
        RandomJELRowReader jeller = createJELRowReader();
        int nset = subsets_.size();
        for ( int is = 0; is < nset; is++ ) {
            RowSubset rs = (RowSubset) subsets_.get( is );
            if ( rs instanceof SyntheticRowSubset ) {
                SyntheticRowSubset ss = (SyntheticRowSubset) rs;
                try {
                    ss.setExpression( ss.getExpression(), jeller );
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
     * @return  true if <tt>param</tt> was removed, false if it wasn't
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
     * This causes a {@link TopcatEvent#CURRENT_ORDER} event to be sent
     * to listeners.
     *
     * @param  order  sort order
     * @param  ascending  sort sense (true for up, false for down)
     */
    public void sortBy( SortOrder order, boolean ascending ) {
        if ( order != SortOrder.NONE ) {
            sortSenseModel_.setSelected( ascending );
        }
        sortSelectionModel_.setSelectedItem( order );
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
                  .checkedLongToInt( dataModel_.getRowCount() );
        ColumnData coldata = dataModel_.getColumnData( icol );
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
        int ncol = columnModel_.getColumnCount();
        int[] colMap = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            colMap[ icol ] = columnModel_.getColumn( icol ).getModelIndex();
        }
        return new ColumnPermutedStarTable( viewModel_.getSnapshot(), colMap );
    }

    /**
     * Returns a RowActivity object which can be used from this model.
     *
     * @return  lazily constructed row activity
     */
    private RowActivity getRowActivity() {
        if ( rowActivity_ == null ) {
            rowActivity_ = controlWindow_.getCommunicator().createRowActivity();
        }
        return rowActivity_;
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
        TopcatModel tcModel = new TopcatModel( table, location, controlWindow );

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
        return new TopcatModel( table, location, controlWindow );
    }

    /**
     * Creates and returns a new TopcatModel with no data.
     * This does not increment the count of existing models - it's intended
     * for things like initialising data models which must stop referring
     * to a live TopcatModel.
     *
     * @return  new empty TopcatModel
     */
    public static synchronized TopcatModel createDummyModel() {
        int ic = instanceCount;
        instanceCount = -1;
        TopcatModel dummy = 
            new TopcatModel( new RowListStarTable( new ColumnInfo[ 0 ] ),
                             "dummy", null );
        instanceCount = ic;
        return dummy;
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
                new SyntheticSubsetQueryWindow( model, parent )
               .setVisible( true );
            }
            else if ( this == unsortAct_ ) {
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
            if ( this == activationAct_ ) {
                return activationWindow_ != null;
            }
            else {
                throw new AssertionError();
            }
        }

        public Window getWindow( Component parent ) {
            TopcatModel tcModel = TopcatModel.this;
            if ( this == activationAct_ ) {
                if ( ! hasWindow() ) {
                    activationWindow_ = 
                        new ActivationQueryWindow( tcModel, parent );
                    activationWindow_.setVisible( true );
                }
                return activationWindow_;
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
        boolean lastAscending_ = true;
        public void setSelected( boolean ascending ) {
            if ( ascending != lastAscending_ ) {

                /* If the table view has a current (non-null) sort order, 
                 * reverse it in place. */
                int[] rowMap = viewModel_.getRowMap();
                if ( rowMap != null ) {
                    for ( int i = 0, j = rowMap.length - 1; i < j; i++, j-- ) {
                        int c = rowMap[ i ];
                        rowMap[ i ] = rowMap[ j ];
                        rowMap[ j ] = c;
                    }
                    viewModel_.setRowMap( rowMap );
                }

                /* Store the changed state. */
                lastAscending_ = ascending;
                fireStateChanged();
                fireModelChanged( TopcatEvent.CURRENT_ORDER, null );
            }
        }
        public boolean isSelected() {
            return lastAscending_;
        }
    }

    /**
     * ComboBoxModel used for storing the available and last-invoked
     * sort orders.
     */
    private class SortSelectionModel extends RestrictedColumnComboBoxModel {

        private SortOrder lastSort_ = SortOrder.NONE;

        SortSelectionModel() {
            super( columnModel_, true );
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
            return lastSort_;
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
            if ( order.equals( lastSort_ ) ) {
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
                                           sortSenseModel_.isSelected() );
                }
                catch ( IOException e ) {
                    Toolkit.getDefaultToolkit().beep();
                    e.printStackTrace( System.err );
                    setSelectedItem( SortOrder.NONE );
                    return;
                }
            }

            /* Install the new sorted order in the table view model. */
            viewModel_.setOrder( rowMap );

            /* Store the selected value. */
            lastSort_ = order;

            /* Make sure any component displaying this model is
             * updated (this call copied from Swing source). */
            fireContentsChanged( this, -1, -1 );

            /* Inform TopcatListeners. */
            fireModelChanged( TopcatEvent.CURRENT_ORDER, null );
        }
    }


    /**
     * ComboBoxModel used for storing the last-invoked subset selection.
     */
    private class SubsetSelectionModel extends AbstractListModel
                                       implements ComboBoxModel, 
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
            subsetCounts_.put( rset, new Long( viewModel_.getRowCount() ) );
            int irset = subsets_.indexOf( rset );
            if ( irset >= 0 ) {
                subsets_.fireContentsChanged( irset, irset );
            }

            /* Store the selected value. */
            lastSubset_ = rset;

            /* Make any component displaying this model is updated. */
            fireContentsChanged( this, -1, -1 );

            /* Notify registered listeners to the TopcatModel. */
            fireModelChanged( TopcatEvent.CURRENT_SUBSET, null );
        }

        public Object getElementAt( int index ) {
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
        }
    }
}
