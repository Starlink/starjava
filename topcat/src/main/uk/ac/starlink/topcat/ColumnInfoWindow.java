package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
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
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A window which displays metadata about each of the columns in a table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfoWindow extends AuxWindow {

    private final TableViewer tv;
    private final PlasticStarTable dataModel;
    private final TableColumnModel columnModel;
    private final ViewerTableModel viewModel;
    private ColumnInfo indexColumnInfo;
    private JTable jtab;
    private AbstractTableModel metaTableModel;

    /**
     * Constructs a new ColumnInfoWindow from a TableViewer.
     *
     * @param  tableviewer  the viewer whose table is to be reflected here
     */
    public ColumnInfoWindow( TableViewer tableviewer ) {
        super( "Table columns", tableviewer );
        this.tv = tableviewer;
        this.dataModel = tv.getDataModel();
        this.columnModel = tv.getColumnModel();
        this.viewModel = tv.getViewModel();
 
        /* Make a dummy column to hold index values. */
        indexColumnInfo = dummyIndexColumn();

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column represents an item of metadata in the data table. */
        List metas = new ArrayList();

        /* Add name column. */
        metas.add( new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getName();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                String name = (String) value;
                getColumnInfo( irow ).setName( name );
                int jcol = irow - 1;
                TableColumn tcol = columnModel.getColumn( jcol );
                tcol.setHeaderValue( name );

                /* This apparent NOP is required to force the TableColumnModel
                 * to notify its listeners (importantly the main data JTable)
                 * that the column name (headerValue) has changed; there 
                 * doesn't appear to be an event specifically for this. */
                columnModel.moveColumn( jcol, jcol );
            }
        } );

        /* Add $ID column. */
        metas.add( new ValueInfoMetaColumn( PlasticStarTable.COLID_INFO,
                                            false ) );

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
        metas.add( new ValueInfoMetaColumn( SyntheticColumn.EXPR_INFO ) {
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
                   .setExpression( (String) value, null );
                    super.setValue( irow, value );

                    /* This is a blunt instrument, but no event is defined to
                     * describe all the data in a single column changing. */
                    viewModel.fireTableDataChanged();
                }
                catch ( CompilationException e ) {
                    ErrorDialog.showError( e, "Bad expression", 
                                           ColumnInfoWindow.this );
                }
            }
        } );
           
        /* Add description column. */
        metas.add( new MetaColumn( "Description", String.class ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getDescription();
            }
            public boolean isEditable( int irow ) {
                return irow > 0;
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setDescription( (String) value );
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
        metas.add( new MetaColumn( "UCD description", String.class ) {
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

        /* Get the list of aux metadata columns. */
        List auxInfos = new ArrayList( dataModel.getColumnAuxDataInfos() );

        /* Remove any from this list which we have already added explicitly. */
        auxInfos.remove( SyntheticColumn.EXPR_INFO );
        auxInfos.remove( PlasticStarTable.COLID_INFO );
        
        /* Add all the remaining aux columns. */
        for ( Iterator it = auxInfos.iterator(); it.hasNext(); ) {
            metas.add( new ValueInfoMetaColumn( (ValueInfo) it.next(), true ) );
        }

        /* Make a table model from the metadata columns.  This model has
         * an extra row 0 to represent the row index. */
        metaTableModel = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnModel.getColumnCount() + 1;
            }
            public boolean isCellEditable( int irow, int icol ) {
                return irow > 0 && super.isCellEditable( irow, icol );
            }
        };

        /* Construct and place a JTable to contain it. */
        jtab = new JTable( metaTableModel );
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
        setMainHeading( "Column Metadata" );

        /* Set up a row header. */
        JTable rowHead = new TableRowHeader( jtab ) {
            public int rowNumber( int irow ) {
                return irow;
            }
        };
        scroller.setRowHeaderView( rowHead );

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

        /* Construct a new menu for column operations. */
        JMenu colMenu = new JMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        Action addcolAct = new BasicAction( "New synthetic column",
                                            ResourceIcon.ADD,
                                       "Add a new column defined " +
                                       "algebraically from existing ones" ) {
            public void actionPerformed( ActionEvent evt ) {
                Component parent = ColumnInfoWindow.this;
                int[] selrows = jtab.getSelectedRows();
                int insertPos = selrows.length > 0 
                              ? selrows[ selrows.length - 1 ]
                              : -1;
                new SyntheticColumnQueryWindow( tv, insertPos, parent );
            }
        };
        colMenu.add( addcolAct ).setIcon( null );
        final Action delcolAct = 
                new BasicAction( "Delete selected column(s)",
                                 ResourceIcon.REMOVE,
                                 "Delete all selected columns " +
                                 "from the table" ) {
            public void actionPerformed( ActionEvent evt ) {
                int[] selected = jtab.getSelectedRows();
                Arrays.sort( selected );
                for ( int i = selected.length - 1; i >= 0; i-- ) {
                    int icol = selected[ i ] - 1;
                    if ( icol >= 0 ) {
                        columnModel
                       .removeColumn( columnModel.getColumn( icol ) );
                    }
                }
            }
        };
        colMenu.add( delcolAct ).setIcon( null );
        final Action sortupAct = new SortAction( true );
        final Action sortdownAct = new SortAction( false );
        colMenu.add( sortupAct ).setIcon( null );
        colMenu.add( sortdownAct ).setIcon( null );
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
                delcolAct.setEnabled( hasSelection );
                sortupAct.setEnabled( hasUniqueSelection );
                sortdownAct.setEnabled( hasUniqueSelection );
            }
        };
        ListSelectionModel selectionModel = jtab.getSelectionModel();
        selectionModel.addListSelectionListener( selList );
        selList.valueChanged( null );

        /* Add actions to the toolbar. */
        getToolBar().add( addcolAct );
        getToolBar().add( delcolAct );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "ColumnInfoWindow" );

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    private int getModelIndexFromRow( int irow ) {
        assert irow != 0;
        return getColumnFromRow( irow ).getModelIndex();
    }

    private TableColumn getColumnFromRow( int irow ) {
        assert irow != 0;
        return columnModel.getColumn( irow - 1 );
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
     * Returns a ColumnInfo object describing a fictitious column zero
     * which contains index information.
     */
    public static ColumnInfo dummyIndexColumn() {
        ValueInfo indexInfo = new DefaultValueInfo( "Index", Long.class,
                                                    "Table row index" );
        ColumnInfo cinfo = new ColumnInfo( indexInfo );
        cinfo.setAuxDatum( new DescribedValue( PlasticStarTable.COLID_INFO,
                                               "$0" ) );
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
     * Class for an action which sorts on a column.
     */
    private class SortAction extends AbstractAction {
        private boolean ascending;

        public SortAction( boolean ascending ) {
            super( "Sort selected " + ( ascending ? "up" : "down" ) );
            this.ascending = ascending;
            putValue( SHORT_DESCRIPTION, "Sort rows by " + 
                                         ( ascending ? "a" : "de" ) +
                                         "scending value of selected column" );
        }

        public void actionPerformed( ActionEvent evt ) {
            int irow = jtab.getSelectedRow();
            if ( irow > 0 ) {
                tv.sortBy( getColumnFromRow( irow ), ascending );
            }
            else {
                tv.sortBy( null, ascending );
            }
        }
    }
}
