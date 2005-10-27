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
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarJTable;

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
    private Collection uneditableParams;
    private DescribedValue ncolParam;
    private DescribedValue nrowParam;
    private MetaColumnTableModel metaTableModel;
    private final ListSelectionModel rowSelectionModel;
    private final Action removeAct;
    private int ncolRowIndex;
    private int nrowRowIndex;

    private static final ValueInfo NAME_INFO = 
        new DefaultValueInfo( "Name", String.class, "Table name" );
    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "URL", URL.class, "URL of original table" );
    private static final ValueInfo NCOL_INFO =
        new DefaultValueInfo( "Column Count", Integer.class, 
                              "Number of columns" );
    private static final ValueInfo NROW_INFO =
        new DefaultValueInfo( "Row Count", Long.class, "Number of rows" );

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs a parameter window.
     *
     * @param  tcModel  model representing the table concerned
     * @param  parent   parent component used for window positioning
     */
    public ParameterWindow( TopcatModel tcModel, Component parent ) {
        super( null, parent );
        labelView( tcModel, "Table Parameters" );

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
        uneditableParams = new HashSet();

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
        ncolRowIndex = params.indexOf( ncolParam );
        nrowRowIndex = params.indexOf( nrowParam );

        /* Assemble a list of MetaColumns which hold information about
         * the columns in the JTable this component will display.
         * Each column holds one item of information about a DescribedValue. */
        List metas = new ArrayList();

        /* Add name column. */
        metas.add( new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getName();
            }
        } );

        /* Add value column. */
        metas.add( new MetaColumn( "Value", Object.class ) {
            public Object getValue( int irow ) {
                return getParam( irow ).getValueAsString( 64 );
            }
            public boolean isEditable( int irow ) {
                return isEditableParameter( irow );
            }
            public void setValue( int irow, Object value ) {
                getParam( irow ).setValue( value );
            }
        } );

        /* Add class column. */
        int classPos = metas.size();
        metas.add( new MetaColumn( "Class", String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatClass( getParamInfo( irow ).getContentClass() );
            }
        } );

        /* Add shape column. */
        metas.add( new MetaColumn( "Shape", String.class ) {
            public Object getValue( int irow ) {
                return DefaultValueInfo
                      .formatShape( getParamInfo( irow ).getShape() );
            }
        } );

        /* Add element size column. */
        int sizePos = metas.size();
        metas.add( new MetaColumn( "Element Size", Integer.class ) {
            public Object getValue( int irow ) {
                int size = getParamInfo( irow ).getElementSize();
                return size > 0 ? new Integer( size ) : null;
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
        metas.add( new MetaColumn( "Units", String.class ) {
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
        metas.add( new MetaColumn( "Description", String.class ) {
            public Object getValue( int irow ) {
                return getParamInfo( irow ).getDescription();
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
        metas.add( new MetaColumn( "UCD", String.class ) {
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
        metas.add( new MetaColumn( "UCD description", String.class ) {
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
                                           .SINGLE_SELECTION );
        rowSelectionModel.addListSelectionListener( this );
        StarJTable.configureColumnWidths( jtab, 20000, 100 );

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

        /* Place the JTable into a scrollpane in this frame. */
        getMainArea().add( new SizingScrollPane( jtab ) );

        /* Action for adding a parameter. */
        Action addAct = new BasicAction( "New Parameter", ResourceIcon.ADD,
                                  "Add a new parameter" ) {
            public void actionPerformed( ActionEvent evt ) {
                ParameterWindow paramWindow = ParameterWindow.this;
                new ParameterQueryWindow( paramWindow.tcModel, paramWindow );
            }
        };
        getToolBar().add( addAct );

        /* Action for removing a parameter. */
        removeAct = new BasicAction( "Remove Parameter", ResourceIcon.DELETE,
                                     "Delete the selected parameter" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( jtab.getSelectedRowCount() == 1 ) {
                    int irow = jtab.getSelectedRow();
                    DescribedValue dval = getParam( irow );
                    if ( ParameterWindow.this.tcModel.getDataModel()
                                        .getParameters().contains( dval ) ) {
                        if ( confirm( "Remove Parameter \"" + dval + "\"?",
                                      "Confirm Removal" ) ) {
                            ParameterWindow.this.tcModel
                                           .removeParameter( dval );
                        }
                    }
                    else {
                        logger.warning( "Parameter \"" + dval + 
                                        "\" missing from model??" );
                    }
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

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    private DescribedValue getParam( int irow ) {
        return (DescribedValue) params.get( irow );
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

    private void configureColumnCount() {
        ncolParam.setValue( new Integer( columnModel.getColumnCount() ) );
        metaTableModel.fireTableRowsUpdated( ncolRowIndex, ncolRowIndex );
    }

    private void configureRowCount() {
        nrowParam.setValue( new Long( (long) viewModel.getRowCount() ) );
        metaTableModel.fireTableRowsUpdated( nrowRowIndex, nrowRowIndex );
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
            boolean active = index >= 0 
                          && index == rowSelectionModel.getMaxSelectionIndex()
                          && ! params.isPseudoParam( index );
            removeAct.setEnabled( active );
        }
    }

    /**
     * Helper class that holds two lists together - one list of 'pseudo'
     * parameters and one list of 'normal' ones.
     */
    private static class ParamList extends AbstractList {
        private final List pseudoParams;
        private final List normalParams;

        ParamList( List normalParams ) {
            this.normalParams = normalParams;
            this.pseudoParams = new ArrayList();
        }

        public void addPseudoParameter( DescribedValue dval ) {
            pseudoParams.add( dval );
        }

        public int size() {
            return pseudoParams.size() + normalParams.size();
        }

        public Object get( int index ) {
            return isPseudoParam( index ) 
                 ? pseudoParams.get( index )
                 : normalParams.get( index - pseudoParams.size() );
        }

        public boolean isPseudoParam( int index ) {
            return index < pseudoParams.size();
        }
    }
}
