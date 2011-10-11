package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.TableRowHeader;

/**
 * A window which displays metadata about each of the columns in a table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfoWindow extends AuxWindow {

    private final TopcatModel tcModel;
    private final PlasticStarTable dataModel;
    private final TableColumnModel columnModel;
    private final ViewerTableModel viewModel;
    private final ColumnList columnList;
    private final Action addcolAct;
    private final Action addskycolAct;
    private final Action replacecolAct;
    private final Action hidecolAct;
    private final Action revealcolAct;
    private final Action hideallAct;
    private final Action revealallAct;
    private final Action explodecolAct;
    private ColumnInfo indexColumnInfo;
    private JTable jtab;
    private AbstractTableModel metaTableModel;

    private static final Logger logger = 
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a new ColumnInfoWindow.
     *
     * @param  tcModel  model containing the data for the table concerned
     * @param  parent   component used for window positioning
     */
    public ColumnInfoWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "Table Columns", parent );
        this.tcModel = tcModel;
        this.dataModel = tcModel.getDataModel();
        this.columnModel = tcModel.getColumnModel();
        this.columnList = tcModel.getColumnList();
        this.viewModel = tcModel.getViewModel();

        /* Make a dummy column to hold index values. */
        indexColumnInfo = dummyIndexColumn();

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column represents an item of metadata in the data table. */
        List metas = new ArrayList();

        /* Add active column. */
        metas.add( new MetaColumn( "Visible", Boolean.class ) {
            public Object getValue( int irow ) {
                if ( irow == 0 ) {
                    return null;
                }
                else {
                    int jrow = getColumnListIndexFromRow( irow );
                    return Boolean.valueOf( columnList.isActive( jrow ) );
                }
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                int jrow = getColumnListIndexFromRow( irow );
                columnList.setActive( jrow, Boolean.TRUE.equals( value ) );
            }
        } );

        /* Add name column. */
        metas.add( new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getName();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                tcModel.renameColumn( getColumnFromRow( irow ), 
                                      (String) value );
            }
        } );

        /* Add $ID column. */
        metas.add( new ValueInfoMetaColumn( TopcatUtils.COLID_INFO, false ) );

        /* Add class column. */
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

        /* Add expression column. */
        metas.add( new ValueInfoMetaColumn( TopcatUtils.EXPR_INFO ) {
            private SyntheticColumn getSyntheticColumn( int irow ) {
                ColumnData coldata = 
                    dataModel.getColumnData( getModelIndexFromRow( irow ) );
                return coldata instanceof SyntheticColumn 
                     ? (SyntheticColumn) coldata
                     : null;
            }
            public boolean isEditable( int irow ) {
                return getSyntheticColumn( irow ) != null;
            }
            public void setValue( int irow, Object value ) {
                try { 
                    getSyntheticColumn( irow )
                   .setExpression( (String) value, null,
                                   tcModel.createJELRowReader() );
                    super.setValue( irow, value );

                    /* Message the table that its data may have changed.
                     * Since every cell in one column is changing, 
                     * potentially any cell in the table could change, since
                     * it may be in a synthetic column depending on the
                     * changed one.  Which is just as well, since no event
                     * is defined to describe all the data in a single
                     * column changing. */
                    viewModel.fireTableDataChanged();
                }
                catch ( CompilationException e ) {
                    String[] msg = new String[] {
                        "Syntax error in synthetic column expression \"" + 
                        value + "\":",
                        e.getMessage(),
                    };
                    JOptionPane.showMessageDialog( ColumnInfoWindow.this, msg,
                                                   "Expression Syntax Error",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        } );
           
        /* Add description column.  Note this actually views/sets the
         * 'base description', which is like the description but for
         * synthetic columns doesn't include the expression text.
         * Access to base description is defined in the TopcatUtils class. */
        metas.add( new MetaColumn( "Description", String.class ) {
            public Object getValue( int irow ) {
                return TopcatUtils.getBaseDescription( getColumnInfo( irow ) );
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                String sval = (String) value;
                TopcatUtils.setBaseDescription( getColumnInfo( irow ), sval );
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
                metaTableModel.fireTableRowsUpdated( irow, irow );
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
                metaTableModel.fireTableRowsUpdated( irow, irow );
            }
        } );

        /* Get the list of aux metadata columns. */
        List auxInfos = new ArrayList( dataModel.getColumnAuxDataInfos() );

        /* Remove any from this list which we have already added explicitly
         * or otherwise don't want to show up. */
        auxInfos.remove( TopcatUtils.EXPR_INFO );
        auxInfos.remove( TopcatUtils.BASE_DESCRIPTION_INFO );
        auxInfos.remove( TopcatUtils.COLID_INFO );
        
        /* Add all the remaining aux columns. */
        for ( Iterator it = auxInfos.iterator(); it.hasNext(); ) {
            metas.add( new ValueInfoMetaColumn( (ValueInfo) it.next(), true ) );
        }

        /* Make a table model from the metadata columns.  This model has
         * an extra row 0 to represent the row index. */
        metaTableModel = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnList.size() + 1;
            }
            public boolean isCellEditable( int irow, int icol ) {
                return irow > 0 && super.isCellEditable( irow, icol );
            }
        };

        /* Construct and place a JTable to contain it. */
        jtab = new JTable( metaTableModel ) {
            public TableCellRenderer getDefaultRenderer( Class clazz ) {
                TableCellRenderer rend = super.getDefaultRenderer( clazz );
                if ( rend == null ) {
                    rend = super.getDefaultRenderer( Object.class );
                }
                return rend;
            }
        };
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

        /* Customise the JTable's column model to provide control over
         * which columns are displayed. */
        MetaColumnModel metaColumnModel = 
            new MetaColumnModel( jtab.getColumnModel(), metaTableModel );
        metaColumnModel.purgeEmptyColumns();
        jtab.setColumnModel( metaColumnModel );

        /* Hide some columns by default. */
        metaColumnModel.removeColumn( sizePos );

        /* Place the table into a scrollpane in this frame. */
        JScrollPane scroller = new SizingScrollPane( jtab );
        getMainArea().add( scroller );

        /* Set up a row header. */
        TableRowHeader rowHead = new TableRowHeader( jtab ) {
            public int rowNumber( int irow ) {
                return irow;
            }
        };
        rowHead.installOnScroller( scroller );

        /* Arrange that dragging on the row header moves columns around
         * in the table column model. */
        rowHead.setSelectionModel( new DefaultListSelectionModel() );
        MouseInputListener mousey = new RowDragMouseListener( rowHead );
        rowHead.addMouseListener( mousey );
        rowHead.addMouseMotionListener( mousey );

        /* Ensure that subsequent changes to the main column model are 
         * reflected in this window.  This listener implemenatation is 
         * lazy, it could be done more efficiently. */
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                metaTableModel.fireTableDataChanged();
            }
            public void columnMoved( TableColumnModelEvent evt ) {
                metaTableModel.fireTableDataChanged();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                metaTableModel.fireTableDataChanged();
            }
        } );

        /* Define actions. */
        addcolAct = new ColumnInfoAction( "New Synthetic Column",
                                          ResourceIcon.ADD,
                                          "Add a new column defined " +
                                          "algebraically from existing ones" );
        addskycolAct = new ColumnInfoAction( "New Sky Coordinate Columns",
                                             ResourceIcon.ADDSKY,
                                             "Add new sky coordinate columns " +
                                             "based on existing ones" );
        replacecolAct = new ColumnInfoAction( "Replace Column With Synthetic",
                                              ResourceIcon.MODIFY,
                                              "Replace the selected column " +
                                              "with a new one based on it" );
        hidecolAct = new ColumnInfoAction( "Hide Selected Column(s)",
                                           ResourceIcon.HIDE,
                                           "Hide all selected columns" );
        revealcolAct = new ColumnInfoAction( "Reveal Selected Column(s)",
                                             ResourceIcon.REVEAL,
                                             "Reveal All Selected columns" );
        hideallAct = new ColumnInfoAction( "Hide All Columns",
                                           ResourceIcon.HIDE_ALL,
                                           "Make all table columns invisible" );
        revealallAct = new ColumnInfoAction( "Reveal All Columns",
                                             ResourceIcon.REVEAL_ALL,
                                             "Make all table columns visible" );
        explodecolAct = new ColumnInfoAction( "Explode Array Column",
                                              ResourceIcon.EXPLODE,
                                              "Replace N-element array column "
                                              + "with N scalar columns" );
        final Action sortupAct = new SortAction( true );
        final Action sortdownAct = new SortAction( false );
        addcolAct.setEnabled( TopcatUtils.canJel() );
        replacecolAct.setEnabled( TopcatUtils.canJel() );

        /* Construct a new menu for column operations. */
        JMenu colMenu = new JMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        colMenu.add( addcolAct );
        colMenu.add( addskycolAct );
        colMenu.add( replacecolAct );
        colMenu.add( hidecolAct );
        colMenu.add( revealcolAct );
        colMenu.add( hideallAct );
        colMenu.add( revealallAct );
        colMenu.add( explodecolAct );
        colMenu.add( sortupAct );
        colMenu.add( sortdownAct );
        getJMenuBar().add( colMenu );

        /* Make a menu for controlling metadata display. */ 
        JMenu displayMenu = metaColumnModel.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Add a selection listener for menu items. */
        ListSelectionListener selList = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                int nsel = jtab.getSelectedRowCount();
                boolean hasSelection = nsel > 0;
                boolean hasUniqueSelection = nsel == 1;
                if ( hasUniqueSelection && jtab.getSelectedRow() == 0 ) {
                    hasUniqueSelection = false;
                    hasSelection = false;
                }
                boolean hasArraySelection;
                if ( hasUniqueSelection ) {
                    StarTableColumn tcol =
                        (StarTableColumn)
                         getColumnFromRow( jtab.getSelectedRow() );
                    int nel = getElementCount( tcol.getColumnInfo() );
                    hasArraySelection = nel > 0;
                    tcModel.fireModelChanged( TopcatEvent.COLUMN, tcol );
                }
                else {
                    hasArraySelection = false;
                }
                hidecolAct.setEnabled( hasSelection );
                revealcolAct.setEnabled( hasSelection );
                explodecolAct.setEnabled( hasArraySelection );
                sortupAct.setEnabled( hasUniqueSelection );
                sortdownAct.setEnabled( hasUniqueSelection );
                replacecolAct.setEnabled( hasUniqueSelection && 
                                          TopcatUtils.canJel() );
            }
        };
        ListSelectionModel selectionModel = jtab.getSelectionModel();
        selectionModel.addListSelectionListener( selList );
        selList.valueChanged( null );

        /* Add actions to the toolbar. */
        getToolBar().add( addcolAct );
        getToolBar().add( addskycolAct );
        getToolBar().add( replacecolAct );
        getToolBar().add( hidecolAct );
        getToolBar().add( revealcolAct );
        getToolBar().add( hideallAct );
        getToolBar().add( revealallAct );
        getToolBar().add( explodecolAct );
        getToolBar().addSeparator();
        getToolBar().add( sortupAct );
        getToolBar().add( sortdownAct );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "ColumnInfoWindow" );
    }

    private int getModelIndexFromRow( int irow ) {
        assert irow != 0;
        return getColumnFromRow( irow ).getModelIndex();
    }

    private int getColumnListIndexFromRow( int irow ) {
        assert irow > 0;
        return irow - 1;
    }

    private int getActiveIndexFromRow( int irow ) {
        int iActive = 0;
        for ( int i = 1; i <= irow; i++ ) {
            if ( columnList.isActive( getColumnListIndexFromRow( i ) ) ) {
                iActive++;
            }
        }
        return iActive;
    }

    private TableColumn getColumnFromRow( int irow ) {
        return columnList.getColumn( getColumnListIndexFromRow( irow ) );
    }

    private ColumnInfo getColumnInfo( int irow ) {
        if ( irow == 0 ) {
            return indexColumnInfo;
        }
        else {
            return dataModel.getColumnInfo( getModelIndexFromRow( irow ) );
        }
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

        private ValueInfo vinfo;
        private Class vclass;
        private boolean isEditable;

        ValueInfoMetaColumn( ValueInfo vinfo, boolean isEditable ) {
            super( vinfo.getName(), vinfo.getContentClass() );
            this.vinfo = vinfo;
            this.vclass = vinfo.getContentClass();
            this.isEditable = isEditable;
        }

        ValueInfoMetaColumn( ValueInfo vinfo ) {
            this( vinfo, false );
        }

        private DescribedValue getAuxDatum( int irow ) {
            return getColumnInfo( irow ).getAuxDatum( vinfo );
        }

        public Object getValue( int irow ) {
            DescribedValue auxDatum = getAuxDatum( irow );
            if ( auxDatum != null ) {
                Object value = auxDatum.getValue();
                if ( value != null && 
                     vclass.isAssignableFrom( value.getClass() ) ) {
                    return value;
                }
            }
            return null;
        }

        public boolean isEditable( int irow ) {
            return isEditable;
        }

        public void setValue( int irow, Object value ) {
            DescribedValue auxDatum = getAuxDatum( irow );
            if ( auxDatum == null ) {
                auxDatum = new DescribedValue( vinfo );
                getColumnInfo( irow ).getAuxData().add( auxDatum );
            }
            if ( value instanceof String ) {
                auxDatum.setValue( vinfo.unformatString( (String) value ) );
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
            if ( this == addcolAct ) {
                int[] selrows = jtab.getSelectedRows();
                int insertPos;
                if ( selrows.length > 0 ) {
                    int iSel = selrows[ selrows.length - 1 ];
                    insertPos = getActiveIndexFromRow( iSel );
                }
                else {
                    insertPos = -1;
                }
                new SyntheticColumnQueryWindow( tcModel, insertPos, parent )
               .setVisible( true );
            }

            /* Add new sky columns: pop up a dialogue window which will
             * result in new sky coordinate columns being added when the
             * user OKs it. */
            else if ( this == addskycolAct ) {
                new SkyColumnQueryWindow( tcModel, parent ).setVisible( true );
            }

            /* Replace a column by another one.  This creates and pops up 
             * a new column-creation dialogue window, and initialises it
             * with the same information that the old one had.
             * When the user OKs it, the new column will be inserted in the
             * column model and the old one will be hidden. */
            else if ( this == replacecolAct ) {
                if ( jtab.getSelectedRowCount() == 1 ) {
                    int selrow = jtab.getSelectedRow();
                    StarTableColumn tcol = 
                        (StarTableColumn) getColumnFromRow( selrow );
                    SyntheticColumnQueryWindow
                        .replaceColumnDialog( tcModel, tcol, parent );
                }
                else {
                    logger.warning( "Replace column enabled erroneously" );
                }
            }

            /* Replace an N-element array column with N scalar columns. */
            else if ( this == explodecolAct ) {
                if ( jtab.getSelectedRowCount() == 1 ) {
                    int selrow = jtab.getSelectedRow();
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
                            tcModel.explodeColumn( tcol );
                        }
                    }
                }
            }

            /* Hide/Reveal a column. */
            else if ( this == hidecolAct || this == revealcolAct ) {
                boolean active = ( this == revealcolAct );
                int[] selected = jtab.getSelectedRows();
                Arrays.sort( selected );
                for ( int i = 0; i < selected.length; i++ ) {
                    if ( selected[ i ] > 0 ) {
                        int jrow = getColumnListIndexFromRow( selected[ i ] );
                        columnList.setActive( jrow, active );
                    }
                }
            }

            /* Hide/Reveal all columns. */
            else if ( this == hideallAct || this == revealallAct ) {
                boolean active = this == revealallAct;
                int ncol = columnList.size();
                for ( int i = 0; i < ncol; i++ ) {
                    columnList.setActive( i, active );
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
        private boolean ascending;

        public SortAction( boolean ascending ) {
            super( "Sort Selected " + ( ascending ? "Up" : "Down" ),
                   ascending ? ResourceIcon.UP : ResourceIcon.DOWN );
            this.ascending = ascending;
            putValue( SHORT_DESCRIPTION, "Sort rows by " + 
                                         ( ascending ? "a" : "de" ) +
                                         "scending value of selected column" );
        }

        public void actionPerformed( ActionEvent evt ) {
            int irow = jtab.getSelectedRow();
            SortOrder order = ( irow > 0 )
                      ? new SortOrder( getColumnFromRow( irow ) )
                      : SortOrder.NONE;
            tcModel.sortBy( order, ascending );
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
            selModel_ = jtab.getSelectionModel();
            kFrom_ = -1;
        }

        @Override
        public void mousePressed( MouseEvent evt ) {
            int i = rowHead_.rowAtPoint( evt.getPoint() );
            if ( i > 0 ) {
                int j = i - 1;
                kFrom_ = columnList.isActive( j ) ? getActiveIndexFromRow( j )
                                                  : -1;
                if ( kFrom_ >= 0 ) {
                    adjustGui( evt, true );
                }
            }
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( kFrom_ >= 0 ) {
                Point p = evt.getPoint();
                p.y = Math.min( Math.max( p.y, 0 ), rowHead_.getHeight() - 1 );
                int i = rowHead_.rowAtPoint( p );
                int j = Math.max( i - 1, 0 );
                int kTo = getActiveIndexFromRow( j );
                if ( kTo != kFrom_ ) {
                    columnModel.moveColumn( kFrom_, kTo );
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
                int i =
                    1 + columnList.indexOf( columnModel .getColumn( kFrom_ ) );
                selModel_.setSelectionInterval( i, i );
            }
            else {
                int i = rowHead_.rowAtPoint( evt.getPoint() );
                headCursor = i > 0 && columnList.isActive( i - 1 )
                           ? hoverCursor_
                           : null;
            }
            rowHead_.setCursor( headCursor );
            ColumnInfoWindow.this.setCursor( isDrag ? dragCursor_ : null );
        }
    }
}
