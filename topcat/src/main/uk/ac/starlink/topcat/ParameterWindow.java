package uk.ac.starlink.topcat;

import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JTable;
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
public class ParameterWindow extends AuxWindow {

    private TableViewer tv;
    private PlasticStarTable dataModel;
    private TableModel viewModel;
    private TableColumnModel columnModel;
    private List params;
    private Collection pseudoParams;
    private Collection uneditableParams;
    private DescribedValue ncolParam;
    private DescribedValue nrowParam;
    private MetaColumnTableModel metaTableModel;
    private int ncolRowIndex;
    private int nrowRowIndex;

    private static final ValueInfo NAME_INFO = 
        new DefaultValueInfo( "Name", String.class, "Table name" );
    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "URL", URL.class, "URL of original table" );
    private static final ValueInfo NCOL_INFO =
        new DefaultValueInfo( "Columns", Integer.class, "Number of columns" );
    private static final ValueInfo NROW_INFO =
        new DefaultValueInfo( "Rows", Long.class, "Number of rows" );

    /**
     * Constructs a parameter window for the given table viewer.
     *
     * @param  tv  the viewer
     */
    public ParameterWindow( TableViewer tv ) {
        super( "Table Parameters", tv );
        this.tv = tv;
        this.dataModel = tv.getDataModel();
        this.viewModel = tv.getViewModel();
        this.columnModel = tv.getColumnModel();

        /* Assemble a list of DescribedValue objects representing the 
         * parameters and other metadata items which describe this table.
         * Also maintain a record of subsets of this list which have
         * certain characteristics. */
        params = new ArrayList();
        pseudoParams = new HashSet();
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
        params.add( nameParam );
        pseudoParams.add( nameParam );

        /* Make a parameter for the table URL. */
        URL url = dataModel.getBaseTable().getURL();
        if ( url != null ) {
            DescribedValue urlParam = new DescribedValue( URL_INFO, url );
            params.add( urlParam );
            pseudoParams.add( urlParam );
            uneditableParams.add( urlParam );
        }

        /* Make a parameter for the number of columns and rows. */
        ncolParam = new DescribedValue( NCOL_INFO );
        nrowParam = new DescribedValue( NROW_INFO );
        params.add( ncolParam );
        params.add( nrowParam );
        pseudoParams.add( ncolParam );
        pseudoParams.add( nrowParam );
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

        /* Add the actual table parameters as such. */
        for ( Iterator it = dataModel.getParameters().iterator();
              it.hasNext(); ) {
            DescribedValue param = (DescribedValue) it.next();
            params.add( param );
        }

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
                return getParam( irow ).getValue();
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
        JTable jtab = new JTable( metaTableModel );
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

        /* By default, hide some of the less useful columns. */
        metaColumnModel.removeColumn( classPos );

        /* Initialise some of the table data which needs initialising. */
        configureColumnCount();
        configureRowCount();

        /* Place the JTable into a scrollpane in this frame. */
        getMainArea().add( new SizingScrollPane( jtab ) );
        setMainHeading( "Table Metadata" );

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
        return pseudoParams.contains( getParam( irow ) );
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
}
