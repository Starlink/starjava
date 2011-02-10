package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
    private final JTable colTable_;
    private final ArrayTableModel colTableModel_;
    private final MetaColumnModel colModel_;
    private final JScrollPane colScroller_;

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
        JComponent tLine = Box.createHorizontalBox();
        tLine.add( new JLabel( "Table: " ) );
        tLine.add( new ShrinkWrapper( tSelector_ ) );
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
        colScroller_ = new JScrollPane();
        add( colScroller_, BorderLayout.CENTER );
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
     * @param  tables  table metadata objects, null if no metadata available
     */
    public void setTables( TableMeta[] tables ) {
        tSelector_.setModel( tables == null
                                    ? new DefaultComboBoxModel()
                                    : new DefaultComboBoxModel( tables ) );
        colScroller_.setViewportView( colTable_ );
        if ( tables != null && tables.length > 0 ) {
            tSelector_.setSelectedIndex( 0 );
            setSelectedTable( tables[ 0 ] );  // should happen automatically?
            StarJTable.configureColumnWidths( colTable_, 360, 9999 );
        }
    }

    /**
     * Displays a progress bar to indicate that metadata fetching is going on.
     *
     * @param  message  message to display
     * @return  new progress bar
     */
    public JProgressBar showFetchProgressBar( String message ) {
        JProgressBar progBar = new JProgressBar();
        progBar.setIndeterminate( true );
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.add( Box.createHorizontalGlue() );
        msgLine.add( new JLabel( message ) );
        msgLine.add( Box.createHorizontalGlue() );
        JComponent progLine = Box.createHorizontalBox();
        progLine.add( Box.createHorizontalGlue() );
        progLine.add( progBar );
        progLine.add( Box.createHorizontalGlue() );
        JComponent workBox = Box.createVerticalBox();
        workBox.add( Box.createVerticalGlue() );
        workBox.add( msgLine );
        workBox.add( Box.createVerticalStrut( 5 ) );
        workBox.add( progLine );
        workBox.add( Box.createVerticalGlue() );
        JComponent workPanel = new JPanel( new BorderLayout() );
        workPanel.add( workBox, BorderLayout.CENTER );
        colScroller_.setViewportView( workPanel );
        return progBar;
    }

    /**
     * Displays an indication that metadata fetching failed.
     *
     * @param  metaUrl  the tableset metadata acquisition attempted URL
     * @param  error   error that caused the failure
     */
    public void showFetchFailure( URL metaUrl, Throwable error ) {
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.setAlignmentX( 0 );
        msgLine.add( new JLabel( "No table metadata available" ) );
        JComponent urlLine = Box.createHorizontalBox();
        urlLine.setAlignmentX( 0 );
        urlLine.add( new JLabel( "Metadata URL: " ) );
        JTextField urlField = new JTextField( metaUrl.toString() );
        urlField.setEditable( false );
        urlField.setBorder( BorderFactory.createEmptyBorder() );
        urlLine.add( new ShrinkWrapper( urlField ) );
        JComponent errLine = Box.createHorizontalBox();
        errLine.setAlignmentX( 0 );
        errLine.add( new JLabel( "Error: " ) );
        String errtxt = error.getMessage();
        if ( errtxt == null || errtxt.trim().length() == 0 ) {
            errtxt = error.toString();
        }
        JTextField errField = new JTextField( errtxt );
        errField.setEditable( false );
        errField.setBorder( BorderFactory.createEmptyBorder() );
        errLine.add( new ShrinkWrapper( errField ) );
        JComponent linesVBox = Box.createVerticalBox();
        linesVBox.add( Box.createVerticalGlue() );
        linesVBox.add( msgLine );
        linesVBox.add( Box.createVerticalStrut( 15 ) );
        linesVBox.add( urlLine );
        linesVBox.add( errLine );
        linesVBox.add( Box.createVerticalGlue() );
        JComponent linesHBox = Box.createHorizontalBox();
        linesHBox.add( Box.createHorizontalGlue() );
        linesHBox.add( linesVBox );
        linesHBox.add( Box.createHorizontalGlue() );
        JComponent panel = new JPanel( new BorderLayout() );
        panel.add( linesHBox, BorderLayout.CENTER );
        colScroller_.setViewportView( panel );
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
