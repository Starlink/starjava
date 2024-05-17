package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * A window which displays metadata about each of the columns in a table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfoWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final PlasticStarTable dataModel_;
    private final TableColumnModel columnModel_;
    private final ViewerTableModel viewModel_;
    private final ColumnList columnList_;
    private final Action addcolAct_;
    private final Action addskycolAct_;
    private final Action replacecolAct_;
    private final Action editcolAct_;
    private final Action hidecolAct_;
    private final Action revealcolAct_;
    private final Action hideallAct_;
    private final Action revealallAct_;
    private final Action sortupAct_;
    private final Action sortdownAct_;
    private final Action explodecolAct_;
    private final Action collapsecolsAct_;
    private final ColumnInfo indexColumnInfo_;
    private final JTable jtab_;
    private final MetaColumnTableModel metaTableModel_;
    private final MetaColumnModel metaColumnModel_;
    private final int icolColsetIndex_;
    private final int icolColsetClass_;
    private final MetaColumnTableSorter sorter_;
    private final SmallColumnSearchWindow searchWindow_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new ColumnInfoWindow.
     *
     * @param  tcModel  model containing the data for the table concerned
     * @param  parent   component used for window positioning
     */
    public ColumnInfoWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "Table Columns", parent );
        tcModel_ = tcModel;
        dataModel_ = tcModel.getDataModel();
        columnModel_ = tcModel.getColumnModel();
        columnList_ = tcModel.getColumnList();
        viewModel_ = tcModel.getViewModel();

        /* Make a dummy column to hold index values. */
        indexColumnInfo_ = dummyIndexColumn();

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column represents an item of metadata in the data table. */
        List<MetaColumn> metas = new ArrayList<MetaColumn>();

        /* Add index column. */
        icolColsetIndex_ = metas.size();
        metas.add( new MetaColumn( "Index", Integer.class ) {
            public Object getValue( int irow ) {
                if ( irow == 0 ) {
                    return null;
                }
                else {
                    TableColumn tcol = getColumnFromRow( irow );
                    int ncol = columnModel_.getColumnCount();
                    for ( int ic = 0; ic < ncol; ic++ ) {
                        if ( columnModel_.getColumn( ic ).equals( tcol ) ) {
                            return new Integer( ic + 1 );
                        }
                    }
                    return null;
                }
            }
        } );

        /* Add active column. */
        metas.add( new MetaColumn( "Visible", Boolean.class ) {
            public Object getValue( int irow ) {
                if ( irow == 0 ) {
                    return null;
                }
                else {
                    int jrow = getColumnListIndexFromRow( irow );
                    return Boolean.valueOf( columnList_.isActive( jrow ) );
                }
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                int jrow = getColumnListIndexFromRow( irow );
                columnList_.setActive( jrow, Boolean.TRUE.equals( value ) );
            }
        } );

        /* Add name column. */
        int icolName = metas.size();
        metas.add( new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getName();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                tcModel_.renameColumn( getColumnFromRow( irow ), 
                                       (String) value );
            }
        } );

        /* Add $ID column. */
        metas.add( new ValueInfoMetaColumn( TopcatUtils.COLID_INFO, false ) );

        /* Add class column. */
        icolColsetClass_ = metas.size();
        metas.add( new MetaColumn( "Class", String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatClass( getColumnInfo( irow ).getContentClass() );
            }
        } );

        /* Add shape column. */
        metas.add( new MetaColumn( "Shape", String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatShape( getColumnInfo( irow ).getShape() );
            }
            public boolean isEditable( int irow ) {
                return irow > 0 && getColumnInfo( irow ).isArray();
            }
            public void setValue( int irow, Object value ) {
                ColumnInfo cinfo = getColumnInfo( irow );
                int[] shape = null;
                if ( cinfo.isArray() ) {
                    shape = new int[] { -1 };
                    String sval = value == null ? null : value.toString();
                    if ( sval != null && sval.trim().length() > 0 ) {
                        try {
                            shape = DefaultValueInfo.unformatShape( sval );
                        }
                        catch ( RuntimeException e ) {
                        }
                    }
                }
                else {
                    shape = null;
                }
                getColumnInfo( irow ).setShape( shape );
                metaTableModel_.fireTableRowsUpdated( irow, irow );
                viewModel_.fireTableDataChanged();
                selectionUpdated();
            }
        } );

        /* Add element size column. */
        int sizePos = metas.size();
        metas.add( new MetaColumn( "Element Size", Integer.class ) {
            public Object getValue( int irow ) {
                int size = getColumnInfo( irow ).getElementSize();
                return size > 0 ? new Integer( size ) : null;
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                int size;
                if ( value instanceof Number ) {
                    size = ((Number) value).intValue();
                }
                else if ( value instanceof String ) {
                    try {
                        size = Integer.parseInt( (String) value );
                    }
                    catch ( NumberFormatException e ) {
                        size = -1;
                    }
                }
                else {
                    size = -1;
                }
                if ( size <= 0 ) {
                    size = -1;
                }
                getColumnInfo( irow ).setElementSize( size );
            }
        } );

        /* Add units column. */
        metas.add( new MetaColumn( "Units", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getUnitString();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setUnitString( (String) value );
            }
        } );

        /* Add domain mapper column. */
        metas.add( new MetaColumn( "Domain", String.class ) {
            public Object getValue( int irow ) {
                DomainMapper[] mappers =
                    getColumnInfo( irow ).getDomainMappers();
                if ( mappers.length == 0 ) {
                    return null;
                }
                else {
                    StringBuffer sbuf = new StringBuffer();
                    for ( int i = 0; i < mappers.length; i++ ) {
                        if ( i > 0 ) {
                            sbuf.append( ", " );
                        }
                        DomainMapper mapper = mappers[ i ];
                        sbuf.append( mapper.getSourceName() )
                            .append( "->" )
                            .append( mapper.getTargetDomain().getDomainName() );
                    }
                    return sbuf.toString();
                }
            }
            public boolean isEditable( int irow ) {
                return false;
            }
        } );

        /* Add expression column. */
        metas.add( new ValueInfoMetaColumn( TopcatUtils.EXPR_INFO ) {
            public boolean isEditable( int irow ) {
                return dataModel_.getColumnData( getModelIndexFromRow( irow ) )
                       instanceof SyntheticColumn;
            }
            public void setValue( int irow, Object value ) {
                int icol = getModelIndexFromRow( irow );
                String expr = (String) value;
                SyntheticColumnQueryWindow qwin =
                    SyntheticColumnQueryWindow
                   .editColumnDialog( tcModel_, icol, ColumnInfoWindow.this,
                                      exCh -> columnChanged( icol, exCh ) );
                qwin.setExpression( expr );
                if ( qwin.perform() ) {
                    qwin.dispose();
                }
                else {
                    qwin.setVisible( true );
                }
            }
        } );
           
        /* Add description column. */
        metas.add( new MetaColumn( "Description", String.class ) {
            public Object getValue( int irow ) {
                return Tables.collapseWhitespace( getColumnInfo( irow )
                                                 .getDescription() );
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                String sval = value instanceof String &&
                              ((String) value).trim().length() > 0
                            ? (String) value
                            : null;
                getColumnInfo( irow ).setDescription( sval );
            }
        } );

        /* Add UCD. */
        metas.add( new MetaColumn( "UCD", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getUCD();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setUCD( (String) value );
                metaTableModel_.fireTableRowsUpdated( irow, irow );
            }
        } );

        /* Add UCD description. */
        metas.add( new MetaColumn( "UCD Description", String.class ) {
            public Object getValue( int irow ) {
                String ucdid = getColumnInfo( irow ).getUCD();
                if ( ucdid != null ) {
                    UCD ucd = UCD.getUCD( getColumnInfo( irow ).getUCD() );
                    if ( ucd != null ) {
                        return ucd.getDescription();
                    }
                }
                return null;
            }
        } );
 
        /* Add Xtype. */
        metas.add( new MetaColumn( "XType", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getXtype();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setXtype( (String) value );
                metaTableModel_.fireTableRowsUpdated( irow, irow );
            }
        } );

        /* Add Utype. */
        metas.add( new MetaColumn( "Utype", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getUtype();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setUtype( (String) value );
                metaTableModel_.fireTableRowsUpdated( irow, irow );
            }
        } );

        /* Get the list of aux metadata columns. */
        List<ValueInfo> auxInfos =
            new ArrayList<ValueInfo>( dataModel_.getColumnAuxDataInfos() );

        /* Remove any from this list which we have already added explicitly
         * or otherwise don't want to show up. */
        auxInfos.remove( TopcatUtils.EXPR_INFO );
        auxInfos.remove( TopcatUtils.COLID_INFO );
        
        /* Add all the remaining aux columns. */
        for ( ValueInfo auxInfo : auxInfos ) {
            metas.add( new ValueInfoMetaColumn( auxInfo, true ) );
        }

        /* Make a table model from the metadata columns.  This model has
         * an extra row 0 to represent the row index. */
        metaTableModel_ = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnList_.size() + 1;
            }
            public boolean isCellEditable( int irow, int icol ) {
                return toUnsortedIndex( irow ) > 0
                    && super.isCellEditable( irow, icol );
            }
        };

        /* Construct and place a JTable to contain it. */
        jtab_ = new JTable( metaTableModel_ ) {
            public TableCellRenderer getDefaultRenderer( Class<?> clazz ) {
                if ( Boolean.class.equals( clazz ) ) {
                    return super.getDefaultRenderer( clazz );
                }
                else {
                    return new NumericCellRenderer( clazz );
                }
            }
        };
        jtab_.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab_.setColumnSelectionAllowed( false );
        jtab_.setRowSelectionAllowed( true );
        StarJTable.configureColumnWidths( jtab_, 20000, 100 );

        /* Allow JTable sorting by clicking on column headers. */
        sorter_ = new MetaColumnTableSorter( metaTableModel_ );
        sorter_.install( jtab_.getTableHeader() );

        /* Customise the JTable's column model to provide control over
         * which columns are displayed. */
        metaColumnModel_ = 
            new MetaColumnModel( jtab_.getColumnModel(), metaTableModel_ );
        metaColumnModel_.purgeEmptyColumns();
        jtab_.setColumnModel( metaColumnModel_ );

        /* Hide some columns by default. */
        metaColumnModel_.removeColumn( sizePos );

        /* Place the table into a scrollpane in this frame. */
        JScrollPane scroller = new SizingScrollPane( jtab_ );
        getMainArea().add( scroller );

        /* Set up a row header. */
        TableRowHeader rowHead = new TableRowHeader( jtab_ ) {
            public long rowNumber( int irow ) {
                return metaTableModel_.getListIndex( irow );
            }
        };
        rowHead.installOnScroller( scroller );
        scroller.setCorner( ScrollPaneConstants.UPPER_LEFT_CORNER,
                            sorter_.getUnsortLabel() );

        /* Prepare a search action. */
        searchWindow_ =
            new SmallColumnSearchWindow( "Search by Metadata", this, jtab_,
                                         scroller, "Field" );
        searchWindow_.getSearchAction()
                     .putValue( Action.SHORT_DESCRIPTION,
                                "Search for column with text in "
                              + "selected metadata category" );
        searchWindow_.getColumnSelector().setSelectedIndex( icolName );
        assert ((TableColumn)
                searchWindow_.getColumnSelector().getSelectedItem())
              .getHeaderValue().equals( "Name" );
        Action searchAct =
                new BasicAction( "Search by Metadata", ResourceIcon.SEARCH,
                                 "Search for text in metadata column" ) {
            public void actionPerformed( ActionEvent evt ) {
                searchWindow_.setVisible( true );
            }
        };

        /* Configure a listener for column popup menus. */
        MouseListener mousepop = new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            public void mouseReleased( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            private void maybeShowPopup( MouseEvent evt ) {
                if ( evt.isPopupTrigger() ) {
                    Component comp = evt.getComponent();
                    int icol = comp == rowHead
                             ? -1
                             : jtab_.columnAtPoint( evt.getPoint() );
                    if ( icol >= -1 ) {
                        JPopupMenu popper = columnPopup( icol );
                        if ( popper != null ) {
                            popper.show( comp, evt.getX(), evt.getY() );
                        }
                    }
                }
            }
        };
        jtab_.addMouseListener( mousepop );
        jtab_.getTableHeader().addMouseListener( mousepop );

        /* Arrange that dragging on the row header moves columns around
         * in the table column model. */
        rowHead.setSelectionModel( new DefaultListSelectionModel() );
        MouseInputListener mousey = new RowDragMouseListener( rowHead );
        rowHead.addMouseListener( mousey );
        rowHead.addMouseMotionListener( mousey );

        /* Ensure that subsequent changes to the main column model are 
         * reflected in this window.  This listener implementation is 
         * sloppy, it could be done more efficiently. */
        columnModel_.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                changed();

                /* If a column is added to the model, scroll the table so
                 * that it's visible and message the TopcatModel so that the
                 * table view is scrolled sideways to make it visible there.
                 * We schedule this for later execution as a hack;
                 * if the user requests adding a column at some position
                 * other than the end, it gets added then immediately moved.
                 * Doing it like this allows the final resting place of the
                 * column rather than its initial one to be made visible. */
                TableColumn tc = columnModel_.getColumn( evt.getToIndex() );
                SwingUtilities.invokeLater( () -> {
                    int ir = getRowIndexFromColumn( tc );
                    if ( ir >= 0 ) {
                        TopcatUtils.ensureRowIndexIsVisible( jtab_, ir );
                        tcModel_.fireModelChanged( TopcatEvent.COLUMN, tc );
                    }
                } );
            }
            public void columnMoved( TableColumnModelEvent evt ) {
                changed();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                changed();
            }
            private void changed() {

                /* Ensure that the metaTableModel is messaged after its data
                 * has actually changed.  There no doubt exists a nicer,
                 * though not necessarily more robust, way to do this. */
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        metaTableModel_.fireTableDataChanged();
                    }
                } );
            }
        } );

        /* Define actions. */
        addcolAct_ = new ColumnInfoAction( "New Synthetic Column",
                                           ResourceIcon.ADD,
                                           "Add a new column defined " +
                                           "algebraically from existing ones" );
        addskycolAct_ = new ColumnInfoAction( "New Sky Coordinate Columns",
                                              ResourceIcon.ADDSKY,
                                              "Add new sky coordinate columns "
                                            + "based on existing ones" );
        replacecolAct_ = new ColumnInfoAction( "Replace Column With Synthetic",
                                               ResourceIcon.MODIFY,
                                               "Replace the selected column " +
                                               "with a new one based on it" );
        editcolAct_ = new ColumnInfoAction( "Edit Column Definition",
                                            ResourceIcon.EDIT,
                                            "View and edit all metadata " +
                                            "for the selected column" );
        hidecolAct_ = new ColumnInfoAction( "Hide Selected Column(s)",
                                            ResourceIcon.HIDE,
                                            "Hide all selected columns" );
        revealcolAct_ = new ColumnInfoAction( "Reveal Selected Column(s)",
                                              ResourceIcon.REVEAL,
                                              "Reveal All Selected columns" );
        hideallAct_ = new ColumnInfoAction( "Hide All Columns",
                                            ResourceIcon.HIDE_ALL,
                                            "Make all table columns invisible");
        revealallAct_ = new ColumnInfoAction( "Reveal All Columns",
                                              ResourceIcon.REVEAL_ALL,
                                              "Make all table columns visible");
        explodecolAct_ = new ColumnInfoAction( "Explode Array Column",
                                               ResourceIcon.EXPLODE,
                                               "Replace N-element array column "
                                             + "with N scalar columns" );
        collapsecolsAct_ = new ColumnInfoAction( "Collapse Columns to Array",
                                                 ResourceIcon.COLLAPSE,
                                                 "Add new array column "
                                               + "made from selected "
                                               + "numeric scalar columns" );
        sortupAct_ = new SortAction( true );
        sortdownAct_ = new SortAction( false );
        Action importAct =
            createImportTableAction( "column metadata", this::getColumnsTable,
                                     "Columns of " + tcModel_.getID() );
        Action saveAct =
            createSaveTableAction( "column metadata", this::getColumnsTable );
        addcolAct_.setEnabled( TopcatUtils.canJel() );
        replacecolAct_.setEnabled( TopcatUtils.canJel() );

        /* Construct a new menu for column operations. */
        JMenu colMenu = new JMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        colMenu.add( addcolAct_ );
        colMenu.add( addskycolAct_ );
        colMenu.add( replacecolAct_ );
        colMenu.add( editcolAct_ );
        colMenu.add( hidecolAct_ );
        colMenu.add( revealcolAct_ );
        colMenu.add( hideallAct_ );
        colMenu.add( revealallAct_ );
        colMenu.add( explodecolAct_ );
        colMenu.add( collapsecolsAct_ );
        colMenu.add( searchAct );
        colMenu.add( sortupAct_ );
        colMenu.add( sortdownAct_ );
        getJMenuBar().add( colMenu );

        /* Make a menu for controlling metadata display. */ 
        JMenu displayMenu = metaColumnModel_.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Menu for export actions. */
        JMenu exportMenu = new JMenu( "Export" );
        exportMenu.add( importAct );
        exportMenu.add( saveAct );
        getJMenuBar().add( exportMenu );

        /* Add a selection listener for menu items. */
        jtab_.getSelectionModel()
            .addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                selectionUpdated();
            }
        } );
        selectionUpdated();

        /* Add actions to the toolbar. */
        getToolBar().add( addcolAct_ );
        getToolBar().add( addskycolAct_ );
        getToolBar().add( replacecolAct_ );
        getToolBar().add( editcolAct_ );
        getToolBar().add( hidecolAct_ );
        getToolBar().add( revealcolAct_ );
        getToolBar().add( hideallAct_ );
        getToolBar().add( revealallAct_ );
        getToolBar().add( explodecolAct_ );
        getToolBar().add( collapsecolsAct_ );
        getToolBar().add( searchAct );
        getToolBar().add( importAct );
        getToolBar().addSeparator();
        getToolBar().add( sortupAct_ );
        getToolBar().add( sortdownAct_ );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "ColumnInfoWindow" );
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
        assert irow != 0;
        return getColumnFromRow( irow ).getModelIndex();
    }

    /**
     * Determines the index in the topcat model's column list
     * for a given row in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window.
     *
     * @param  irow   row index in unsorted table model
     * @return  ColumnList index
     */
    private int getColumnListIndexFromRow( int irow ) {
        assert irow > 0;
        return irow - 1;
    }

    /**
     * Determines the index of the closest active (visible) column
     * for a given row in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window.
     *
     * @param  irow   row index in unsorted table model
     * @return   rank in list of visible columns for irow if visible,
     *           else for the most recent visible column
     */
    private int getActiveIndexFromRow( int irow ) {
        int iActive = 0;
        for ( int i = 1; i <= irow; i++ ) {
            if ( columnList_.isActive( getColumnListIndexFromRow( i ) ) ) {
                iActive++;
            }
        }
        return iActive;
    }

    /**
     * Returns the TableColumn object
     * for a given row in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window.
     *
     * @param  irow   row index in unsorted table model
     * @return  table column
     */
    private TableColumn getColumnFromRow( int irow ) {
        return columnList_.getColumn( getColumnListIndexFromRow( irow ) );
    }

    /**
     * Returns the ColumnInfo object
     * for a given row in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window.
     *
     * @param  irow   row index in unsorted table model
     * @return  column info
     */
    private ColumnInfo getColumnInfo( int irow ) {
        if ( irow == 0 ) {
            return indexColumnInfo_;
        }
        else {
            return dataModel_.getColumnInfo( getModelIndexFromRow( irow ) );
        }
    }

    /**
     * Determines the row index in the naturally ordered (unsorted)
     * MetaColumnTableModel displayed in this window corresponding to
     * a given row in the JTable.  Some disentangling may be required
     * if the JTable is currently sorted by one of the columns.
     *
     * @param   jrow   row index in displayed JTable
     * @return  row index in unsorted table model
     */
    private int toUnsortedIndex( int jrow ) {
        return metaTableModel_.getListIndex( jrow );
    }

    /**
     * Determines the row index in the (sorted) MetaColumnTableModel
     * displayed in this window corresponding to a row in the unsorted
     * table model (equivalently, the column model for the topcat table).
     *
     * @param  irow  row index in unsorted table model
     * @return  row index in displayed JTable
     */
    private int toSortedIndex( int irow ) {

        /* Lucky guess?  Will get the right answer if no sort order is
         * in operation. */
        if ( metaTableModel_.getListIndex( irow ) == irow ) {
            return irow;
        }

        /* Do it the hard way. */
        else {
            int n = metaTableModel_.getRowCount();
            for ( int i = 0; i < n; i++ ) {
                if ( metaTableModel_.getListIndex( i ) == irow ) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Returns the row index in the sorted table model at which a given
     * TableColumn is found.
     *
     * @param   tc  table column
     * @return  index of JTable row, or -1 if not found
     */
    private int getRowIndexFromColumn( TableColumn tc ) {
        for ( int ir = 1; ir < jtab_.getRowCount(); ir++ ) {
            if ( getColumnFromRow( toUnsortedIndex( ir ) ) == tc ) {
                return ir;
            }
        }
        return -1;
    }

    /**
     * Ensures that the GUI is in the correct state given that the
     * currently selected JTable row (i.e. data model column) may
     * have changed. 
     */
    private void selectionUpdated() {
        int nsel = jtab_.getSelectedRowCount();
        boolean hasSelection = nsel > 0;
        boolean hasUniqueSelection = nsel == 1;
        boolean hasMultiSelection = nsel > 1;
        if ( hasUniqueSelection &&
             toUnsortedIndex( jtab_.getSelectedRow() ) == 0 ) {
            hasUniqueSelection = false;
            hasSelection = false;
        }
        boolean hasArraySelection;
        if ( hasUniqueSelection ) {
            int iSel = toUnsortedIndex( jtab_.getSelectedRow() );
            StarTableColumn tcol =
                (StarTableColumn) getColumnFromRow( iSel );
            int nel = getElementCount( tcol.getColumnInfo() );
            hasArraySelection = nel > 0;
            tcModel_.fireModelChanged( TopcatEvent.COLUMN, tcol );
        }
        else {
            hasArraySelection = false;
        }
        hidecolAct_.setEnabled( hasSelection );
        revealcolAct_.setEnabled( hasSelection );
        explodecolAct_.setEnabled( hasArraySelection );
        collapsecolsAct_.setEnabled( hasMultiSelection );
        sortupAct_.setEnabled( hasUniqueSelection );
        sortdownAct_.setEnabled( hasUniqueSelection );
        replacecolAct_.setEnabled( hasUniqueSelection && TopcatUtils.canJel() );
        editcolAct_.setEnabled( hasUniqueSelection );
    }

    /**
     * Perform required additional updates following possible change of
     * column metadata.
     *
     * @param  icol  model index of column
     * @param  expressionChanged  true if the column is synthetic and its
     *                            defining expression may have changed
     */
    private void columnChanged( int icol, boolean expressionChanged ) {

        /* Update the metadata that may have changed.
         * This really only applies to one row, but it's
         * possible that the row index we have is no longer
         * correct, so hit it with a sledgehammer and update
         * all the table cells to be sure. */
        metaTableModel_.fireTableDataChanged();

        /* The other steps are only required if the expression changed. */
        if ( expressionChanged ) {

            /* Recompile any other expressions that may depend
             * on this one, since the way that JEL expression
             * evaluation works, the compiled expressions won't work
             * any longer if the data type has changed. */
            recompileDependencies( icol );

            /* Message the data table that its data may have changed.
             * Since every cell in one column is changing, 
             * potentially any cell in the table could change, since
             * it may be in a synthetic column depending on the
             * changed one.  Which is just as well, since no event
             * is defined to describe all the data in a single
             * column changing. */
            viewModel_.fireTableDataChanged();
        }
    }

    /**
     * Called if the evaluation expression of a synthetic column has changed,
     * to trigger recompilation of any expressions dependent on it.
     * This is required for the dependent expressions to continue working
     * if the datatype of the upstream expression has changed;
     * if the datatype hasn't changed it's not necessary, but it should be
     * harmless.
     *
     * @param  icol  index of column whose expression has changed
     */
    private void recompileDependencies( int icol ) {

        /* Recompile expressions for any dependent synthetic columns. */
        for ( int ic = 0; ic < dataModel_.getColumnCount(); ic++ ) {
            ColumnData coldata = dataModel_.getColumnData( ic );
            if ( ic != icol && coldata instanceof SyntheticColumn ) {
                SyntheticColumn scol = (SyntheticColumn) coldata;
                String expr = scol.getExpression();
                if ( TopcatJELUtils
                    .isColumnReferenced( tcModel_, icol, expr ) ) {
                    try {
                        scol.setExpression( expr, null );
                    }
                    catch ( CompilationException e ) {
                        logger_.warning( "Uh oh: " + e );
                    }
                }
            }
        }

        /* Recompile expressions for any dependent synthetic subsets. */
        for ( RowSubset rset : tcModel_.getSubsets() ) {
            if ( rset instanceof SyntheticRowSubset ) {
                SyntheticRowSubset srset = (SyntheticRowSubset) rset;
                String expr = srset.getExpression();
                if ( TopcatJELUtils
                    .isColumnReferenced( tcModel_, icol, expr ) ) {
                    try {
                        srset.setExpression( expr );
                    }
                    catch ( CompilationException e ) {
                        logger_.warning( "Uh oh: " + e );
                    }
                }
            }
        }
    }

    /**
     * Returns a new array column formed from scalar columns
     * specified by index.
     * The output column has a dummy name, so should be renamed before use.
     *
     * <p>Currently, the output column is of type <code>double[]</code> and
     * any non-numeric input values, including values from non-numeric columns,
     * are represented as NaN.
     * Because of the problems associated with representing null values,
     * for now no attempt is made to return array values of different types
     * based on the input columns (for instance an <code>int[]</code> array).
     *
     * @param  icols  array of column model indices
     * @return   array-valued column, or null if something went wrong
     */
    private ColumnData createArrayColumn( int[] icols ) {

        /* Check columns are suitable. */
        final int nc = icols.length;
        if ( nc == 0 ) {
            return null;
        }
        List<String> colNames = new ArrayList<>();
        for ( int ic = 0; ic < nc; ic++ ) {
            ColumnInfo sinfo = dataModel_.getColumnInfo( icols[ ic ] );
            if ( ! Number.class.isAssignableFrom( sinfo.getContentClass() ) ) {
                String msg = "Column " + sinfo + " is not numeric";
                JOptionPane.showMessageDialog( ColumnInfoWindow.this, msg,
                                               "Collapse Error",
                                               JOptionPane.ERROR_MESSAGE );
                return null;
            }
            colNames.add( sinfo.getName() );
        }

        /* Check if we have some metadata items that are exactly the same
         * for all input columns. */
        ColumnInfo cinfo0 = dataModel_.getColumnInfo( icols[ 0 ] );
        String unit0 = cinfo0.getUnitString();
        String ucd0 = cinfo0.getUCD();
        for ( int ic = 1; ic < nc; ic++ ) {
            ColumnInfo cinfo1 = dataModel_.getColumnInfo( icols[ ic ] );
            String unit1 = cinfo1.getUnitString();
            String ucd1 = cinfo1.getUCD();
            if ( unit0 != null && ! unit0.equals( unit1 ) ) {
                unit0 = null;
            }
            if ( ucd0 != null && ! ucd0.equals( ucd1 ) ) {
                ucd0 = null;
            }
        }

        /* Prepare and return array column. */
        ColumnInfo info = new ColumnInfo( "dummy", double[].class,
                                          "array of scalar columns" );
        info.setShape( new int[] { nc } );
        StringBuffer dbuf = new StringBuffer()
                           .append( "Array column from input scalars: " );
        int maxNames = 20;
        if ( colNames.size() > maxNames ) {
            dbuf.append( colNames.stream()
                                 .limit( maxNames / 2 )
                                 .collect( Collectors.joining( ", " ) ) )
                .append( ", ... , " )
                .append( colNames.get( colNames.size() -1 ) );
        }
        else {
            dbuf.append( colNames.stream()
                                 .collect( Collectors.joining( ", " ) ) );
        }
        info.setDescription( dbuf.toString() );
        if ( unit0 != null ) {
            info.setUnitString( unit0 );
        }
        if ( ucd0 != null ) {
            info.setUCD( ucd0 );
        }
        return new ColumnData( info ) {
            public double[] readValue( long irow ) throws IOException {
                double[] value = new double[ nc ];
                for ( int ic = 0; ic < nc; ic++ ) {
                    Object scalar = dataModel_.getCell( irow, icols[ ic ] );
                    value[ ic ] = scalar instanceof Number
                                ? ((Number) scalar).doubleValue()
                                : Double.NaN;
                }
                return value;
            }
        };
    }

    /**
     * Converts the displayed content of this window to a StarTable
     * suitable for export.
     *
     * @return   StarTable of column metadata
     */
    private StarTable getColumnsTable() {
        int ncol = jtab_.getColumnCount();
        ColumnInfo[] infos = new ColumnInfo[ ncol ];
        List<MetaColumn> colList = metaTableModel_.getColumnList();
        for ( int icol = 0; icol < ncol; icol++ ) {
            int jcol = metaColumnModel_.getColumn( icol ).getModelIndex();
            infos[ icol ] = new ColumnInfo( colList.get( jcol ).getInfo() );
        }
        RowListStarTable table = new RowListStarTable( infos );
        table.setName( "Column metadata for " + tcModel_.getLabel() );
        int nrow = jtab_.getRowCount();
        for ( int irow = 0; irow < nrow; irow++ ) {
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = jtab_.getValueAt( irow, icol );
            }
            table.addRow( row );
        }
        return new NormaliseTable( table );
    }

    /**
     * Returns a popup menu for the given column.
     *
     * @param  jcol  column index
     * @return  popup menu to post for requested column
     */
    private JPopupMenu columnPopup( int icol ) {
        TableColumn tcol = metaColumnModel_.getColumn( icol );
        if ( tcol == null ) {
            return null;
        }
        int jcol = tcol.getModelIndex();
        MetaColumn metaCol = metaTableModel_.getColumnList().get( jcol );
        Object chead = tcol.getHeaderValue();
        String cname = chead instanceof String ? (String) chead : "metadata";
        JPopupMenu popper = new JPopupMenu();
        popper.add( new BasicAction( "Search " + cname + " Metadata",
                                     ResourceIcon.SEARCH,
                                     "Find columns with " + cname + 
                                     " matching text" ) {
            public void actionPerformed( ActionEvent evt ) {
                searchWindow_.setColumn( tcol );
                searchWindow_.setVisible( true );
            }
        } );
        if ( sorter_.getSortIndex() == jcol ) {
            popper.add( new BasicAction( "Unsort", ResourceIcon.UNSORT,
                                         "Restore metadata display " +
                                         "to natural order" ) {
                public void actionPerformed( ActionEvent evt ) {
                    sorter_.setSorting( -1, false );
                }
            } );
        }
        if ( metaTableModel_.canSort( metaCol ) ) {
            popper.add( new BasicAction( "Sort Up by " + cname,
                                         ResourceIcon.UP,
                                         "Sort metadata display by " + cname +
                                         ", ascending" ) {
                public void actionPerformed( ActionEvent evt ) {
                    sorter_.setSorting( jcol, false );
                }
            } );
            popper.add( new BasicAction( "Sort Down by " + cname,
                                         ResourceIcon.DOWN,
                                         "Sort metadata display by " + cname +
                                         ", descending" ) {
                public void actionPerformed( ActionEvent evt ) {
                    sorter_.setSorting( jcol, true );
                }
            } );
        }
        popper.add( new BasicAction( "Hide " + cname + " metadata",
                                     ResourceIcon.HIDE,
                                     "Remove " + cname + " column from " +
                                     "metadata display" ) {
            public void actionPerformed( ActionEvent evt ) {
                metaColumnModel_.removeColumn( tcol );
            }
        } );
        return popper;
    }

    /**
     * Returns the number of elements per cell in a ValueInfo.
     * The result is a positive integer only if the info represents an
     * array value with a fixed length.
     *
     * @param   info  description of a value
     * @return   fixed number of scalar elements described by <tt>info</tt>,
     *           or a non-positive number if it's not suitable
     */
    public static int getElementCount( ValueInfo info ) {
        if ( info.isArray() ) {
            int[] shape = info.getShape();
            int nel = 1;
            for ( int i = 0; i < shape.length; i++ ) {
                int dim = shape[ i ];
                if ( dim >= 0 ) {
                    nel *= dim;
                }
                else {
                    return -1;
                }
            }
            return nel;
        }
        return 0; 
    }

    /**
     * Returns a ColumnInfo object describing a fictitious column zero
     * which contains index information.
     */
    public static ColumnInfo dummyIndexColumn() {
        ValueInfo indexInfo = new DefaultValueInfo( "Index", Long.class,
                                                    "Table row index" );
        ColumnInfo cinfo = new ColumnInfo( indexInfo );
        cinfo.setAuxDatum( 
            new DescribedValue( TopcatUtils.COLID_INFO, 
                                TopcatJELRowReader.COLUMN_ID_CHAR + "0" ) );
        return cinfo;
    }

    /**
     * Class which adapts a ValueInfo into a MetaColumn.
     */
    private class ValueInfoMetaColumn extends MetaColumn {

        private ValueInfo vinfo_;
        private boolean isEditable_;
        private Class<?> vclass_;
        private boolean isFormattable_;

        ValueInfoMetaColumn( ValueInfo vinfo, boolean isEditable ) {
            super( vinfo.getName(), canFormat( vinfo ) ? vinfo.getContentClass()
                                                       : Object.class );
            vinfo_ = vinfo;
            isEditable_ = isEditable;
            vclass_ = vinfo.getContentClass();
            isFormattable_ = canFormat( vinfo );
        }

        ValueInfoMetaColumn( ValueInfo vinfo ) {
            this( vinfo, false );
        }

        private DescribedValue getAuxDatum( int irow ) {
            return getColumnInfo( irow ).getAuxDatum( vinfo_ );
        }

        public Object getValue( int irow ) {
            DescribedValue auxDatum = getAuxDatum( irow );
            if ( auxDatum != null ) {
                Object value = auxDatum.getValue();
                if ( value != null && 
                     vclass_.isAssignableFrom( value.getClass() ) ) {
                    return value;
                }
            }
            return null;
        }

        public boolean isEditable( int irow ) {
            return isEditable_ && isFormattable_;
        }

        public void setValue( int irow, Object value ) {
            DescribedValue auxDatum = getAuxDatum( irow );
            if ( auxDatum == null ) {
                auxDatum = new DescribedValue( vinfo_ );
                getColumnInfo( irow ).getAuxData().add( auxDatum );
            }
            if ( value instanceof String ) {
                auxDatum.setValue( vinfo_.unformatString( (String) value ) );
            }
            else if ( value == null ) {
                auxDatum.setValue( null );
            }
            else {
                // ??
            }
        }
    }

    /**
     * Indicates whether sensible formatting and unformatting
     * is possible for a given ValueInfo.
     *
     * @param  info  metadata item to test
     * @return   true if String format/unformat is implemented
     */
    private static boolean canFormat( ValueInfo info ) {
        Class<?> clazz = info.getContentClass();
        return Number.class.isAssignableFrom( clazz )
            || String.class.isAssignableFrom( clazz )
            || Boolean.class.isAssignableFrom( clazz );
    }

    /**
     * Implementation of actions for this window.
     */
    private class ColumnInfoAction extends BasicAction {
        ColumnInfoAction( String name, Icon icon, String description ) {
            super( name, icon, description );
        }

        public void actionPerformed( ActionEvent evt ) {
            Component parent = ColumnInfoWindow.this;

            /* Add a new column: pop up a dialogue window which will 
             * result in a new column being added when the user OKs it. */
            if ( this == addcolAct_ ) {
                int[] selrows = jtab_.getSelectedRows();
                int insertPos;
                if ( selrows.length > 0 ) {
                    int iSel = selrows[ selrows.length - 1 ];
                    int mSel = toUnsortedIndex( iSel );
                    insertPos = getActiveIndexFromRow( mSel );
                }
                else {
                    insertPos = -1;
                }
                SyntheticColumnQueryWindow
                   .newColumnDialog( tcModel_, insertPos, parent )
                   .setVisible( true );
            }

            /* Add new sky columns: pop up a dialogue window which will
             * result in new sky coordinate columns being added when the
             * user OKs it. */
            else if ( this == addskycolAct_ ) {
                new SkyColumnQueryWindow( tcModel_, parent ).setVisible( true );
            }

            /* Replace a column by another one.  This creates and pops up 
             * a new column-creation dialogue window, and initialises it
             * with the same information that the old one had.
             * When the user OKs it, the new column will be inserted in the
             * column model and the old one will be hidden. */
            else if ( this == replacecolAct_ ) {
                if ( jtab_.getSelectedRowCount() == 1 ) {
                    int selrow = toUnsortedIndex( jtab_.getSelectedRow() );
                    StarTableColumn tcol = 
                        (StarTableColumn) getColumnFromRow( selrow );
                    SyntheticColumnQueryWindow
                        .replaceColumnDialog( tcModel_, tcol, parent )
                        .setVisible( true );
                }
                else {
                    logger_.warning( "Replace column enabled erroneously" );
                }
            }

            /* Edit an existing synthetic or non-synthetic column. */
            else if ( this == editcolAct_ ) {
                if ( jtab_.getSelectedRowCount() == 1 ) {
                    int irow = toUnsortedIndex( jtab_.getSelectedRow() );
                    int icol = getModelIndexFromRow( irow );
                    SyntheticColumnQueryWindow
                   .editColumnDialog( tcModel_, icol, ColumnInfoWindow.this,
                                      exCh -> columnChanged( icol, exCh ) )
                   .setVisible( true );
                }
            }

            /* Replace an N-element array column with N scalar columns. */
            else if ( this == explodecolAct_ ) {
                if ( jtab_.getSelectedRowCount() == 1 ) {
                    int selrow = toUnsortedIndex( jtab_.getSelectedRow() );
                    StarTableColumn tcol =
                        (StarTableColumn) getColumnFromRow( selrow );
                    ColumnInfo cinfo = tcol.getColumnInfo();
                    int nel = getElementCount( cinfo );
                    if ( nel > 0 ) {
                        String yesOpt = "OK";
                        String noOpt = "Cancel";
                        String msg = "Replace array column " + cinfo.getName()
                                   + " with " + nel + " scalar columns?";
                        if ( JOptionPane
                            .showOptionDialog( parent, msg, "Explode Column",
                                               JOptionPane.YES_NO_OPTION,
                                               JOptionPane.QUESTION_MESSAGE,
                                               null, 
                                               new String[] { yesOpt, noOpt },
                                               yesOpt ) == 0 ) {
                            tcModel_.explodeColumn( tcol );
                        }
                    }
                }
            }

            /* New array column from selected scalar columns. */
            else if ( this == collapsecolsAct_ ) {
                int nsel = jtab_.getSelectedRowCount();
                if ( nsel > 1 ) {
                    int[] isels = jtab_.getSelectedRows();
                    int nscalar = isels.length;
                    int[] icScalars = new int[ nscalar ];
                    for ( int is = 0; is < nscalar; is++ ) {
                        icScalars[ is ] =
                            getModelIndexFromRow(
                                toUnsortedIndex( isels[ is ] ) );
                    }
                    ColumnData arrayCol = createArrayColumn( icScalars );
                    if ( arrayCol != null ) {
                        String msg = "Column name for collapse of " + nscalar
                                   + " columns";
                        String cname =
                            JOptionPane
                           .showInputDialog( parent, msg,
                                             "Create Collapsed Column",
                                             JOptionPane.QUESTION_MESSAGE );
                        arrayCol.getColumnInfo().setName( cname );
                        tcModel_.appendColumn( arrayCol );
                    }
                }
            }

            /* Hide/Reveal a column. */
            else if ( this == hidecolAct_ || this == revealcolAct_ ) {
                boolean active = ( this == revealcolAct_ );
                int[] selected = jtab_.getSelectedRows().clone();
                for ( int i = 0; i < selected.length; i++ ) {
                    selected[ i ] = toUnsortedIndex( selected[ i ] );
                }
                Arrays.sort( selected );
                for ( int i = 0; i < selected.length; i++ ) {
                    if ( selected[ i ] > 0 ) {
                        int jrow = getColumnListIndexFromRow( selected[ i ] );
                        columnList_.setActive( jrow, active );
                    }
                }
            }

            /* Hide/Reveal all columns. */
            else if ( this == hideallAct_ || this == revealallAct_ ) {
                boolean active = this == revealallAct_;
                int ncol = columnList_.size();
                for ( int i = 0; i < ncol; i++ ) {
                    columnList_.setActive( i, active );
                }
            }

            /* Unknown action?? */
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * Class for an action which sorts on a column.
     */
    private class SortAction extends AbstractAction {
        private final boolean ascending_;

        public SortAction( boolean ascending ) {
            super( "Sort Selected " + ( ascending ? "Up" : "Down" ),
                   ascending ? ResourceIcon.UP : ResourceIcon.DOWN );
            ascending_ = ascending;
            putValue( SHORT_DESCRIPTION, "Sort table rows by " + 
                                         ( ascending_ ? "a" : "de" ) +
                                         "scending value of selected column" );
        }

        public void actionPerformed( ActionEvent evt ) {
            int irow = toUnsortedIndex( jtab_.getSelectedRow() );
            SortOrder order = ( irow > 0 )
                      ? new SortOrder( getColumnFromRow( irow ) )
                      : SortOrder.NONE;
            tcModel_.sortBy( order, ascending_ );
        }
    }

    /**
     * Mouse Listener for use with the table row header which allows
     * dragging column rows up and down.  The effect of these drags is
     * to change the TopcatModel's TableColumnModel.
     * The cursor is manipulated to indicate the possibilities and behaviour.
     */
    private class RowDragMouseListener extends MouseInputAdapter {
        private final JTable rowHead_;
        private final Cursor hoverCursor_;
        private final Cursor dragCursor_;
        private final ListSelectionModel selModel_;
        private int kFrom_;

        /**
         * Constructor.
         * 
         * @param  rowHead  table whose rows can be dragged
         */
        RowDragMouseListener( JTable rowHead ) {
            rowHead_ = rowHead;
            hoverCursor_ = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );
            dragCursor_ = Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR );
            selModel_ = jtab_.getSelectionModel();
            kFrom_ = -1;
        }

        /**
         * Indicates whether row dragging is permitted.
         * It's permitted if either the natural order or ordering
         * by the column set index is in effect.
         * For other sort orders, dragging the rows around would have
         * no effect (except maybe cause the GUI to flicker).
         */
        private boolean canDrag() {
            int icolSort = sorter_.getSortIndex();
            return icolSort < 0 || icolSort == icolColsetIndex_;
        }

        @Override
        public void mousePressed( MouseEvent evt ) {
            if ( canDrag() ) {
                int i = toUnsortedIndex( rowHead_
                                        .rowAtPoint( evt.getPoint() ) );
                if ( i > 0 ) {
                    int j = i - 1;
                    kFrom_ = columnList_.isActive( j )
                           ? getActiveIndexFromRow( j )
                           : -1;
                    if ( kFrom_ >= 0 ) {
                        adjustGui( evt, true );
                    }
                }
            }
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( kFrom_ >= 0 ) {
                Point p = evt.getPoint();
                p.y = Math.min( Math.max( p.y, 0 ), rowHead_.getHeight() - 1 );
                int i = toUnsortedIndex( rowHead_.rowAtPoint( p ) );
                int j = Math.max( i - 1, 0 );
                int kTo = getActiveIndexFromRow( j );
                if ( kTo != kFrom_ ) {
                    columnModel_.moveColumn( kFrom_, kTo );
                    kFrom_ = kTo;
                    adjustGui( evt, true );
                }
            }
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            kFrom_ = -1;
            adjustGui( evt, false );
        }

        @Override
        public void mouseMoved( MouseEvent evt ) {
            adjustGui( evt, false );
        }

        /**
         * Adjusts the appearance of the components this listener serves,
         * to reflect the current state.
         * 
         * @param  evt  mouse event which caused the adjustment
         * @param  isDrag  true iff a drag is in progress
         */
        private void adjustGui( MouseEvent evt, boolean isDrag ) {
            final Cursor headCursor;
            if ( isDrag ) {
                headCursor = null;
                int k = columnList_.indexOf( columnModel_.getColumn( kFrom_ ) );
                final int isel = toSortedIndex( 1 + k );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        selModel_.setSelectionInterval( isel, isel );
                    }
                } );
            }
            else {
                int i =
                    toUnsortedIndex( rowHead_.rowAtPoint( evt.getPoint() ) );
                headCursor = i > 0 && columnList_.isActive( i - 1 ) && canDrag()
                           ? hoverCursor_
                           : null;
            }
            rowHead_.setCursor( headCursor );
            ColumnInfoWindow.this.setCursor( isDrag ? dragCursor_ : null );
        }
    }
}
