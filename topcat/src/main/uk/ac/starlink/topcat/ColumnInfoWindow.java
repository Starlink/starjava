package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
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
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * A window which displays metadata about each of the columns in a table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfoWindow extends AuxWindow {

    private final TableViewer tv;
    private final PlasticStarTable dataModel;
    private final TableColumnModel columnModel;
    private ColumnInfo indexColumnInfo;
    private JTable jtab;

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

        /* Construct a new menu for column operations. */
        JMenu colMenu = new JMenu( "Columns" );
        Action addcolAct = new BasicAction( "New synthetic column",
                                       "Add a new column defined " +
                                       "algebraically from existing ones" ) {
            public void actionPerformed( ActionEvent evt ) {
                Component parent = ColumnInfoWindow.this;
                ColumnData coldata = tv.obtainColumn( parent );
                if ( coldata != null ) {
                    tv.appendColumn( coldata );
                }
            }
        };
        colMenu.add( addcolAct );
        final Action delcolAct = 
                new BasicAction( "Delete selected column(s)",
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
        colMenu.add( delcolAct );
        final Action sortupAct = new SortAction( true );
        final Action sortdownAct = new SortAction( false );
        colMenu.add( sortupAct );
        colMenu.add( sortdownAct );
        getJMenuBar().add( colMenu );
 
        /* Make a dummy column to hold index values. */
        indexColumnInfo = dummyIndexColumn();

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column represents an item of metadata in the data table. */
        List metas = new ArrayList();

        /* Add index column. */
        metas.add( new MetaColumn( "Index", Long.class, false ) {
            public Object getValue( int irow ) {
                return new Long( irow );
            }
        } );

        /* Add name column. */
        metas.add( new MetaColumn( "Name", String.class, true ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getName();
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

        /* Add class column. */
        metas.add( new MetaColumn( "Class", String.class, false ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatClass( getColumnInfo( irow ).getContentClass() );
            }
        } );

        /* Add shape column. */
        metas.add( new MetaColumn( "Shape", String.class, false ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatShape( getColumnInfo( irow ).getShape() );
            }
        } );

        /* Add units column. */
        metas.add( new MetaColumn( "Units", String.class, true ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getUnitString();
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setUnitString( (String) value );
            }
        } );
           
        /* Add description column. */
        metas.add( new MetaColumn( "Description", String.class, true ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getDescription();
            }
            public void setValue( int irow, Object value ) {
                getColumnInfo( irow ).setDescription( (String) value );
            }
        } );

        /* Add UCD. */
        metas.add( new MetaColumn( "UCD", String.class, false ) {
            public Object getValue( int irow ) {
                return getColumnInfo( irow ).getUCD();
            }
        } );

        /* Add aux metadata columns. */
        List auxInfos = new ArrayList( dataModel.getColumnAuxDataInfos() );

        /* Mess with the ordering of some columns. */
        auxInfos.remove( SyntheticColumn.EXPR_INFO );
        auxInfos.add( 0, SyntheticColumn.EXPR_INFO );
        // auxInfos.add( 0, PlasticStarTable.COLID_INFO );

        /* Add all the aux columns. */
        for ( Iterator it = auxInfos.iterator(); it.hasNext(); ) {
            ValueInfo vinfo = (ValueInfo) it.next();
            final String vname = vinfo.getName();
            final Class vclass = vinfo.getContentClass();
            metas.add( new MetaColumn( vname, vinfo.getContentClass(), false ) {
                public Object getValue( int irow ) {
                    DescribedValue auxDatum = getColumnInfo( irow )
                                             .getAuxDatumByName( vname );
                    if ( auxDatum != null ) {
                         Object value = auxDatum.getValue();
                         if ( value != null &&
                              vclass.isAssignableFrom( value.getClass() ) ) {
                             return value;
                         }
                    }
                    return null;
                }
            } );
        }

        /* Make a table model from the metadata columns.  This model has
         * an extra row 0 to represent the row index. */
        final AbstractTableModel model = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return columnModel.getColumnCount() + 1;
            }
            public boolean isCellEditable( int irow, int icol ) {
                return irow > 0 && super.isCellEditable( irow, icol );
            }
        };

        /* Construct and place a JTable to contain it. */
        jtab = new JTable( model );
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

        /* Place the table into a scrollpane in this frame. */
        getMainArea().add( new SizingScrollPane( jtab ) );
        setMainHeading( "Column metadata" );

        /* Ensure that subsequent changes to the column model are reflected
         * in this window.  This listener implemenatation is lazy, it
         * could be done more efficiently. */
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                model.fireTableDataChanged();
            }
            public void columnMoved( TableColumnModelEvent evt ) {
                model.fireTableDataChanged();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                model.fireTableDataChanged();
            }
        } );

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
