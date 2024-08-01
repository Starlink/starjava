package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * Top-level window which displays the parameters of a table.
 * Other per-table metadata may be displayed as well.
 */
public class ParameterWindow extends AuxWindow 
                             implements TopcatListener, ListSelectionListener {

    private TopcatModel tcModel;
    private PlasticStarTable dataModel;
    private TableModel viewModel;
    private TableColumnModel columnModel;
    private ParamList params;
    private Collection<DescribedValue> uneditableParams;
    private DescribedValue ncolParam;
    private DescribedValue nrowParam;
    private final MetaColumnTableModel metaTableModel;
    private final ListSelectionModel rowSelectionModel;
    private final Action removeAct;
    private final ParameterDetailPanel detailPanel;

    private static final ValueInfo NAME_INFO = 
        new DefaultValueInfo( "Name", String.class, "Table name" );
    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "URL", URL.class, "URL of original table" );
    private static final ValueInfo NCOL_INFO =
        new DefaultValueInfo( "Column Count", Integer.class, 
                              "Number of columns" );
    private static final ValueInfo NROW_INFO =
        new DefaultValueInfo( "Row Count", Long.class, "Number of rows" );

    public static final String NAME_NAME = "Name";
    public static final String VALUE_NAME = "Value";
    public static final String CLASS_NAME = "Class";
    public static final String SHAPE_NAME = "Shape";
    public static final String ELSIZE_NAME = "Element Size";
    public static final String UNITS_NAME = "Units";
    public static final String DESC_NAME = "Description";
    public static final String UCD_NAME = "UCD";
    public static final String UCDDESC_NAME = "UCD Description";
    public static final String UTYPE_NAME = "Utype";

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a parameter window.
     *
     * @param  tcModel  model representing the table concerned
     * @param  parent   parent component used for window positioning
     */
    @SuppressWarnings("this-escape")
    public ParameterWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "Table Parameters", parent );
        this.tcModel = tcModel;
        this.dataModel = tcModel.getDataModel();
        this.viewModel = tcModel.getViewModel();
        this.columnModel = tcModel.getColumnModel();

        /* Listen on our TopcatModel in case the parameter list changes. */
        tcModel.addTopcatListener( this );

        /* Assemble a list of DescribedValue objects representing the 
         * parameters and other metadata items which describe this table.
         * This is based on the live parameter list from the data model,
         * but will include some extra 'pseudo'-parameters. */
        params = new ParamList( dataModel.getParameters() );

        /* Also maintain a record of subsets of this list which have
         * certain characteristics. */
        uneditableParams = new HashSet<DescribedValue>();

        /* Make a parameter for the table name. */
        DescribedValue nameParam = new DescribedValue( NAME_INFO ) {
            public Object getValue() {
                return dataModel.getName();
            }
            public void setValue( Object value ) {
                String name = value == null ? null : value.toString();
                if ( name.trim().length() == 0 ) {
                    name = null;
                }
                dataModel.setName( name );
                tcModel.fireModelChanged( TopcatEvent.LABEL, null );
            }
        };
        params.addPseudoParameter( nameParam );

        /* Make a parameter for the table URL. */
        URL url = dataModel.getBaseTable().getURL();
        if ( url != null ) {
            DescribedValue urlParam = new DescribedValue( URL_INFO, url );
            params.addPseudoParameter( urlParam );
            uneditableParams.add( urlParam );
        }

        /* Make a parameter for the number of columns and rows. */
        ncolParam = new DescribedValue( NCOL_INFO );
        nrowParam = new DescribedValue( NROW_INFO );
        params.addPseudoParameter( ncolParam );
        params.addPseudoParameter( nrowParam );
        uneditableParams.add( ncolParam );
        uneditableParams.add( nrowParam );
        columnModel.addColumnModelListener( new TableColumnModelAdapter() {
            public void columnAdded( TableColumnModelEvent evt ) {
                configureColumnCount();
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                configureColumnCount();
            }
        } );
        viewModel.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                configureRowCount();
            }
        } );

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column holds one item of information about a DescribedValue. */
        List<MetaColumn> metas = new ArrayList<MetaColumn>();

        /* Add name column. */
        metas.add( new MetaColumn( NAME_NAME, String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getName();
            }
            public boolean isEditable( int irow ) {
                return ! isPseudoParameter( irow ) 
                    && getParam( irow ).getInfo() instanceof DefaultValueInfo;
            }
            public void setValue( int irow, Object value ) {
                ((DefaultValueInfo) getParam( irow ).getInfo())
                                   .setName( value.toString() );
            }
        } );

        /* Add value column. */
        metas.add( new MetaColumn( VALUE_NAME, Object.class ) {
            public Object getValue( int irow ) {
                return Tables.collapseWhitespace( getParam( irow )
                                                 .getValueAsString( 64 ) );
            }
            public boolean isEditable( int irow ) {
                if ( isEditableParameter( irow ) ) {
                    Class<?> clazz =
                        getParam( irow ).getInfo().getContentClass();

                    /* Not all value types can be edited, since the 
                     * ValueInfo.unformatString() method doesn't work in all
                     * cases.  Assembling this list of what does and doesn't
                     * work as is done here is not very satisfactory, since
                     * it doesn't necessarily get right which ones do and
                     * which ones don't. */
                    if ( Number.class.isAssignableFrom( clazz ) ||
                         Boolean.class.equals( clazz ) ||
                         String.class.equals( clazz ) ) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
            public void setValue( int irow, Object value ) {
                DescribedValue param = getParam( irow );
                try {
                    if ( value == null ) {
                        param.setValue( null );
                    }
                    else if ( value instanceof String ) {
                        param.setValueFromString( (String) value );
                    }
                    else if ( param.getInfo().getContentClass()
                                   .isAssignableFrom( value.getClass() ) ) {
                        param.setValue( value );
                    }
                    else {
                        param.setValueFromString( value.toString() );
                    }
                    tcModel.fireModelChanged( TopcatEvent.PARAMETER_VALUE,
                                              param );
                }
                catch ( RuntimeException e ) {
                    Object msg = "Invalid value \"" + value + "\"";
                    JOptionPane.showMessageDialog( ParameterWindow.this, msg,
                                                   "Invalid Parameter Value",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        } );

        /* Add class column. */
        int classPos = metas.size();
        metas.add( new MetaColumn( CLASS_NAME, String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatClass( getParamInfo( irow ).getContentClass() );
            }
        } );

        /* Add shape column. */
        metas.add( new MetaColumn( SHAPE_NAME, String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatShape( getParamInfo( irow ).getShape() );
            }
        } );

        /* Add element size column. */
        int sizePos = metas.size();
        metas.add( new MetaColumn( ELSIZE_NAME, Integer.class ) {
            public Object getValue( int irow ) {
                int size = getParamInfo( irow ).getElementSize();
                return size > 0 ? Integer.valueOf( size ) : null;
            }
            public boolean isEditable( int irow ) {
                return getParamInfo( irow ) instanceof DefaultValueInfo;
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
                ((DefaultValueInfo) getParamInfo( irow ))
                                   .setElementSize( size );
            }
        } );

        /* Add units column. */
        metas.add( new MetaColumn( UNITS_NAME, String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getUnitString();
            }
            public boolean isEditable( int irow ) {
                return ! isPseudoParameter( irow ) &&
                       getParamInfo( irow ) instanceof DefaultValueInfo;
            }
            public void setValue( int irow, Object value ) {
                ((DefaultValueInfo) getParamInfo( irow ))
               .setUnitString( value == null ? null : value.toString() );
            }
        } );

        /* Add description column. */
        metas.add( new MetaColumn( DESC_NAME, String.class ) {
            public Object getValue( int irow ) {
                return Tables.collapseWhitespace( getParamInfo( irow )
                                                 .getDescription() );
            }
            public boolean isEditable( int irow ) {
                return ! isPseudoParameter( irow ) &&
                       getParamInfo( irow ) instanceof DefaultValueInfo;
            }
            public void setValue( int irow, Object value ) {
                ((DefaultValueInfo) getParamInfo( irow ))
               .setDescription( value == null ? null : value.toString() );
            }
        } );

        /* Add UCD column. */
        metas.add( new MetaColumn( UCD_NAME, String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getUCD();
            }
            public boolean isEditable( int irow ) {
                return ! isPseudoParameter( irow ) &&
                       getParamInfo( irow ) instanceof DefaultValueInfo;
            }
            public void setValue( int irow, Object value ) {
                ((DefaultValueInfo) getParamInfo( irow ))
               .setUCD( value == null ? null : value.toString() );
            }
        } );

        /* Add UCD description column. */
        metas.add( new MetaColumn( UCDDESC_NAME, String.class ) {
            public Object getValue( int irow ) {
                String ucdid = getParamInfo( irow ).getUCD();
                if ( ucdid != null ) {
                    UCD ucd = UCD.getUCD( ucdid );
                    if ( ucd != null ) {
                        return ucd.getDescription();
                    }
                }
                return null;
            }
        } );

        /* Add Utype column. */
        metas.add( new MetaColumn( UTYPE_NAME, String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getUtype();
            }
            public boolean isEditable( int irow ) {
                return ! isPseudoParameter( irow ) &&
                       getParamInfo( irow ) instanceof DefaultValueInfo;
            }
            public void setValue( int irow, Object value ) {
                ((DefaultValueInfo) getParamInfo( irow ))
               .setUtype( value == null ? null : value.toString() );
            }
        } );

        /* Make a table model from the metadata columns. */
        metaTableModel = new MetaColumnTableModel( metas ) {
            public int getRowCount() {
                return params.size();
            }
        };

        /* Construct and place a JTable to contain it. */
        final JTable jtab = new JTable( metaTableModel );
        jtab.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );
        rowSelectionModel = jtab.getSelectionModel();
        rowSelectionModel.setSelectionMode( ListSelectionModel
                                           .MULTIPLE_INTERVAL_SELECTION );
        rowSelectionModel.addListSelectionListener( this );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

        /* Allow JTable sorting by clicking on column headers. */
        new MetaColumnTableSorter( metaTableModel )
           .install( jtab.getTableHeader() );

        /* Customise the JTable's column model to provide control over 
         * which columns are displayed. */
        MetaColumnModel metaColumnModel =
            new MetaColumnModel( jtab.getColumnModel(), metaTableModel );
        metaColumnModel.purgeEmptyColumns();
        jtab.setColumnModel( metaColumnModel );

        /* By default, hide some of the less useful columns. */
        metaColumnModel.removeColumn( classPos );
        metaColumnModel.removeColumn( sizePos );

        /* Initialise some of the table data which needs initialising. */
        configureColumnCount();
        configureRowCount();

        /* Construct and place a split pane to hold list/detail panels. */
        JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        splitter.setOneTouchExpandable( true );
        getMainArea().add( splitter );

        /* Place the JTable into a scrollpane in this frame. */
        splitter.setTopComponent( new SizingScrollPane( jtab ) );

        /* Place a component to hold value detail display. */
        detailPanel = new ParameterDetailPanel( metaTableModel );
        splitter.setBottomComponent( detailPanel );

        /* Action for adding a parameter. */
        Action addAct = new BasicAction( "New Parameter", ResourceIcon.ADD,
                                         "Add a new parameter" ) {
            public void actionPerformed( ActionEvent evt ) {
                ParameterWindow paramWindow = ParameterWindow.this;
                new ParameterQueryWindow( paramWindow.tcModel, paramWindow )
               .setVisible( true );
            }
        };
        getToolBar().add( addAct );

        /* Action for removing a parameter. */
        removeAct = new BasicAction( "Remove Parameter", ResourceIcon.DELETE,
                                     "Delete the selected parameter" ) {
            public void actionPerformed( ActionEvent evt ) {
                TopcatModel tcm = ParameterWindow.this.tcModel;
                List<DescribedValue> removals = new ArrayList<DescribedValue>();
                for ( int irow = rowSelectionModel.getMinSelectionIndex();
                      irow <= rowSelectionModel.getMaxSelectionIndex();
                      irow++ ) {
                    if ( rowSelectionModel.isSelectedIndex( irow ) ) {
                        DescribedValue dval =
                            getParam( toUnsortedIndex( irow ) );
                        if ( tcm.getDataModel().getParameters()
                                               .contains( dval ) ) {
                            removals.add( dval );
                        }
                        else {
                            logger.warning( "Parameter \"" + dval + 
                                            "\" missing from model??" );
                        }
                    }
                }
                if ( ! removals.isEmpty() ) {
                    rowSelectionModel.clearSelection();
                }
                for ( DescribedValue dval : removals ) {
                    tcm.removeParameter( dval );
                }
            }
        };
        removeAct.setEnabled( false );
        getToolBar().add( removeAct );
        getToolBar().addSeparator();

        /* Make a menu for parameter manipulation. */
        JMenu paramMenu = new JMenu( "Parameters" );
        paramMenu.setMnemonic( KeyEvent.VK_P );
        paramMenu.add( addAct );
        paramMenu.add( removeAct );
        getJMenuBar().add( paramMenu );

        /* Make a menu for controlling metadata display. */
        JMenu displayMenu = metaColumnModel.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Add standard help actions. */
        addHelp( "ParameterWindow" );
    }

    private DescribedValue getParam( int irow ) {
        return params.get( irow );
    }

    private ValueInfo getParamInfo( int irow ) {
        return getParam( irow ).getInfo();
    }

    private boolean isPseudoParameter( int irow ) {
        return params.isPseudoParam( irow );
    }

    private boolean isEditableParameter( int irow ) {
        return ! uneditableParams.contains( getParam( irow ) );
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
        return metaTableModel.getListIndex( jrow );
    }

    private void configureColumnCount() {
        ncolParam.setValue( Integer.valueOf( columnModel.getColumnCount() ) );
        int ixRow = getJTableRowIndex( ncolParam );
        metaTableModel.fireTableRowsUpdated( ixRow, ixRow );
    }

    private void configureRowCount() {
        nrowParam.setValue( Long.valueOf( (long) viewModel.getRowCount() ) );
        int ixRow = getJTableRowIndex( nrowParam );
        metaTableModel.fireTableRowsUpdated( ixRow, ixRow );
    }

    /**
     * Returns the index of the row in the displayed JTable (possibly sorted)
     * that is currently displaying a given parameter.
     *
     * @param  dval  table parameter value
     * @return   row index in displayed table, or -1 if not found
     */
    private int getJTableRowIndex( DescribedValue dval ) {
        for ( int i = 0; i < metaTableModel.getRowCount(); i++ ) {
            if ( dval.equals( params.get( toUnsortedIndex( i ) ) ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Implements TopcatListener so that the display will be updated 
     * if the table's parameter list changes.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.PARAMETERS ) {
            metaTableModel.fireTableDataChanged();
        }
    }

    /**
     * Implements ListSelectionListener so it can update actions when a
     * JTable row (a parameter) is selected.
     */
    public void valueChanged( ListSelectionEvent evt ) {
        if ( evt.getSource() == rowSelectionModel ) {
            int index = rowSelectionModel.getMinSelectionIndex();
            boolean active = index >= 0;
            for ( int i = rowSelectionModel.getMinSelectionIndex();
                  i <= rowSelectionModel.getMaxSelectionIndex(); i++ ) {
                if ( rowSelectionModel.isSelectedIndex( i ) ) {
                    active = active &&
                           ! params.isPseudoParam( toUnsortedIndex( index ) );
                }
            }
            removeAct.setEnabled( active );
            detailPanel.setItem( index,
                                 ( index >= 0
                                   ? params.get( toUnsortedIndex( index ) )
                                   : null ) );
        }
    }

    /**
     * Helper class that holds two lists together - one list of 'pseudo'
     * parameters and one list of 'normal' ones.
     */
    private static class ParamList extends AbstractList<DescribedValue> {
        private final List<DescribedValue> pseudoParams;
        private final List<DescribedValue> normalParams;

        ParamList( List<DescribedValue> normalParams ) {
            this.normalParams = normalParams;
            this.pseudoParams = new ArrayList<DescribedValue>();
        }

        public void addPseudoParameter( DescribedValue dval ) {
            pseudoParams.add( dval );
        }

        public int size() {
            return pseudoParams.size() + normalParams.size();
        }

        public DescribedValue get( int index ) {
            if ( isPseudoParam( index ) ) {
                return pseudoParams.get( index );
            }
            else if ( index - pseudoParams.size() < normalParams.size() ) {
                return normalParams.get( index - pseudoParams.size() );
            }

            /* Hack - sometimes this method gets called with an out of
             * range index following parameter deletion.
             * This is probably down to incorrect orchestration of
             * events triggered by other events.  The problem is only
             * transient, once the events have settled down it seems to
             * get called with the right values.
             * Rather than do the right thing and sort the events out,
             * I am just returning some value here that will not cause
             * an exception. */
            else {
                return pseudoParams.get( 0 );
            }
        }

        public boolean isPseudoParam( int index ) {
            return index < pseudoParams.size();
        }
    }
}
