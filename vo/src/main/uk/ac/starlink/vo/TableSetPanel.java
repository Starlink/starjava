package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Displays the metadata from an array of TableMeta objects.
 * These can be acquired from a TableSet XML document as exposed
 * by VOSI and TAP interfaces.
 * 
 * @author   Mark Taylor
 * @since    21 Jan 2011
 */
public class TableSetPanel extends JPanel {

    private final JComboBox tSelector_;
    private final JLabel countLabel_;
    private final JTable colTable_;
    private final ArrayTableModel colTableModel_;
    private final MetaColumnModel colModel_;

    /**
     * Constructor.
     */
    public TableSetPanel() {
        super( new BorderLayout() );
        tSelector_ = new JComboBox();
        tSelector_.setRenderer( new DefaultListCellRenderer() {
            public Component getListCellRendererComponent( JList list,
                                                           Object value,
                                                           int index,
                                                           boolean isSelected,
                                                           boolean hasFocus ) {
                super.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
                if ( value instanceof TableMeta ) {
                    setText( ((TableMeta) value).getName() );
                }
                return this;
            }
        } );
        tSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                int code = evt.getStateChange();
                if ( code == ItemEvent.DESELECTED ) {
                    setSelectedTable( null );
                }
                else if ( code == ItemEvent.SELECTED ) {
                    setSelectedTable( (TableMeta) evt.getItem() );
                }
            }
        } );
        countLabel_ = new JLabel();
        JComponent tLine = Box.createHorizontalBox();
        tLine.add( new JLabel( "Table: " ) );
        tLine.add( new ShrinkWrapper( tSelector_ ) );
        tLine.add( countLabel_ );
        tLine.add( Box.createHorizontalGlue() );
        tLine.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
        JComponent chLine = Box.createHorizontalBox();
        chLine.add( new JLabel( "Columns:" ) );
        chLine.add( Box.createHorizontalGlue() );
        JComponent topBox = Box.createVerticalBox();
        topBox.add( tLine );
        topBox.add( chLine );
        add( topBox, BorderLayout.NORTH );
        colTableModel_ = new ArrayTableModel( createColumnMetaColumns(),
                                              new ColumnMeta[ 0 ] );
        colTable_ = new JTable( colTableModel_ );
        colTable_.setColumnSelectionAllowed( false );
        colTable_.setRowSelectionAllowed( false );
        colModel_ =
            new MetaColumnModel( colTable_.getColumnModel(), colTableModel_ );
        colTable_.setColumnModel( colModel_ );
        add( new JScrollPane( colTable_ ), BorderLayout.CENTER );
        setSelectedTable( null );
    }

    /**
     * Returns a new menu for controlling which columns are visible in
     * the column display table.
     *
     * @param  name  menu name
     */
    public JMenu makeColumnDisplayMenu( String name ) {
        return colModel_.makeCheckBoxMenu( name );
    }

    /**
     * Sets the data model for the metadata displayed by this panel.
     * The data is in the form of an array of table metadata objects.
     *
     * @param  tables  table metadata objects
     */
    public void setTables( TableMeta[] tables ) {
        tSelector_.setModel( new DefaultComboBoxModel( tables ) );
        int nTable = tables == null ? 0 : tables.length;
        String txt = tables == null
            ? null
            : " (" + nTable + " table" + ( nTable == 1 ? "" : "s" ) + ")";
        countLabel_.setText( txt );
        if ( tables.length > 0 ) {
            tSelector_.setSelectedIndex( 0 );
            setSelectedTable( tables[ 0 ] );  // should happen automatically?
            StarJTable.configureColumnWidths( colTable_, 360, 9999 );
        }
    }

    /**
     * Invoked to display the metadata corresponding to a given table.
     *
     * @param   table  newly selected table
     */
    private void setSelectedTable( TableMeta table ) {
        colTableModel_.setItems( table == null ? new ColumnMeta[ 0 ]
                                               : table.getColumns() );
    }

    /**
     * Constructs an array of columns which define the table model
     * to use for displaying the column metadata.
     *
     * @return   column descriptions
     */
    private static ArrayTableColumn[] createColumnMetaColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getName();
                }
            },
            new ArrayTableColumn( "DataType", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getDataType();
                }
            },
            new ArrayTableColumn( "Unit", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUnit();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getDescription();
                }
            },
            new ArrayTableColumn( "UCD", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUcd();
                }
            },
            new ArrayTableColumn( "Utype", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUtype();
                }
            },
            new ArrayTableColumn( "Flags", String.class ) {
                public Object getValue( Object item ) {
                    String[] flags = getCol( item ).getFlags();
                    if ( flags != null && flags.length > 0 ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < flags.length; i++ ) {
                            if ( i > 0 ) {
                                sbuf.append( ' ' );
                            }
                            sbuf.append( flags[ i ] );
                        }
                        return sbuf.toString();
                    }
                    else {
                        return null;
                    }
                }
            },
        };
    }

    /**
     * Returns the ColumnMeta object associated with a given item
     * in the column metadata table model.  It's just a cast.
     *
     * @param   item  table cell contents
     * @return   column metadata object associated with <code>item</code>
     */
    private static ColumnMeta getCol( Object item ) {
        return (ColumnMeta) item;
    }
}
