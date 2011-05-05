package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.Arrays;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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
    private final JTable foreignTable_;
    private final ArrayTableModel colTableModel_;
    private final ArrayTableModel foreignTableModel_;
    private final MetaColumnModel colColModel_;
    private final MetaColumnModel foreignColModel_;
    private final JComponent metaPanel_;
    private final JSplitPane metaSplitter_;
    private final JLabel tableLabel_;
    private TableMeta[] tables_;

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
        tableLabel_ = new JLabel();
        tLine.add( new JLabel( "Table: " ) );
        tLine.add( new ShrinkWrapper( tSelector_ ) );
        tLine.add( Box.createHorizontalStrut( 5 ) );
        tLine.add( tableLabel_ );
        tLine.add( Box.createHorizontalGlue() );
        tLine.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
        add( tLine, BorderLayout.NORTH );

        colTableModel_ = new ArrayTableModel( createColumnMetaColumns(),
                                              new ColumnMeta[ 0 ] );
        colTable_ = new JTable( colTableModel_ );
        colTable_.setColumnSelectionAllowed( false );
        colTable_.setRowSelectionAllowed( false );
        colColModel_ =
            new MetaColumnModel( colTable_.getColumnModel(), colTableModel_ );
        colTable_.setColumnModel( colColModel_ );

        foreignTableModel_ = new ArrayTableModel( createForeignMetaColumns(),
                                                  new ColumnMeta[ 0 ] );
        foreignTable_ = new JTable( foreignTableModel_ );
        foreignTable_.setColumnSelectionAllowed( false );
        foreignTable_.setRowSelectionAllowed( false );
        foreignColModel_ =
            new MetaColumnModel( foreignTable_.getColumnModel(),
                                 foreignTableModel_ );
        foreignTable_.setColumnModel( foreignColModel_ );

        metaSplitter_ = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        metaSplitter_.setResizeWeight( 0.8 );
        metaSplitter_.setBorder( BorderFactory.createEmptyBorder() );
        JComponent colPanel = new JPanel( new BorderLayout() );
        JComponent chBox = Box.createHorizontalBox();
        chBox.add( new JLabel( "Columns:" ) );
        chBox.add( Box.createHorizontalGlue() );
        colPanel.add( chBox, BorderLayout.NORTH );
        colPanel.add( new JScrollPane( colTable_ ), BorderLayout.CENTER );
        metaSplitter_.setTopComponent( colPanel );
        JComponent foreignPanel = new JPanel( new BorderLayout() );
        JComponent fhBox = Box.createHorizontalBox();
        fhBox.add( new JLabel( "Foreign Keys:" ) );
        fhBox.add( Box.createHorizontalGlue() );
        foreignPanel.add( fhBox, BorderLayout.NORTH );
        foreignPanel.add( new JScrollPane( foreignTable_ ),
                          BorderLayout.CENTER );
        metaSplitter_.setBottomComponent( foreignPanel );

        metaPanel_ = new JPanel( new BorderLayout() );
        metaPanel_.add( metaSplitter_, BorderLayout.CENTER );
        add( metaPanel_, BorderLayout.CENTER );
        setSelectedTable( null );
    }

    /**
     * Returns a new menu for controlling which columns are visible in
     * the column display table.
     *
     * @param  name  menu name
     */
    public JMenu makeColumnDisplayMenu( String name ) {
        return colColModel_.makeCheckBoxMenu( name );
    }

    /**
     * Sets the data model for the metadata displayed by this panel.
     * The data is in the form of an array of table metadata objects.
     *
     * @param  tables  table metadata objects, null if no metadata available
     */
    public void setTables( TableMeta[] tables ) {
        tables_ = tables;
        tSelector_.setModel( tables == null
                                    ? new DefaultComboBoxModel()
                                    : new DefaultComboBoxModel( tables ) );
        metaPanel_.removeAll();
        metaPanel_.add( metaSplitter_ );
        metaPanel_.revalidate();
        if ( tables != null && tables.length > 0 ) {
            tSelector_.setSelectedIndex( 0 );
            setSelectedTable( tables[ 0 ] );  // should happen automatically?
        }
        else {
            setSelectedTable( null );
        }
        repaint();
    }

    /**
     * Returns the most recently set table metadata set.
     *
     * @return   current table metadata array
     */
    public TableMeta[] getTables() {
        return tables_;
    }

    /**
     * Returns the combo box used to select different tables for metadata
     * display.
     *
     * @return   table selector, whose items are {@link TableMeta} objects
     */
    public JComboBox getTableSelector() {
        return tSelector_;
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
        metaPanel_.removeAll();
        metaPanel_.add( workPanel, BorderLayout.CENTER );
        metaPanel_.revalidate();
        return progBar;
    }

    /**
     * Displays an indication that metadata fetching failed.
     *
     * @param  metaUrl  the tableset metadata acquisition attempted URL
     * @param  error   error that caused the failure
     */
    public void showFetchFailure( String metaUrl, Throwable error ) {
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.setAlignmentX( 0 );
        msgLine.add( new JLabel( "No table metadata available" ) );
        JComponent urlLine = Box.createHorizontalBox();
        urlLine.setAlignmentX( 0 );
        urlLine.add( new JLabel( "Metadata URL: " ) );
        JTextField urlField = new JTextField( metaUrl );
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
        metaPanel_.removeAll();
        metaPanel_.add( panel, BorderLayout.CENTER );
        metaPanel_.revalidate();
    }

    /**
     * Returns the table which is currently selected for metadata display.
     *
     * @return   selected table, may be null
     */
    public TableMeta getSelectedTable() {
        return (TableMeta) tSelector_.getSelectedItem();
    }

    /**
     * Invoked to display the metadata corresponding to a given table.
     *
     * @param   table  newly selected table
     */
    private void setSelectedTable( TableMeta table ) {
        if ( table == null ) {
            colTableModel_.setItems( new ColumnMeta[ 0 ] );
            foreignTableModel_.setItems( new ForeignMeta[ 0 ] );
            tableLabel_.setText( "" );
        }
        else {
            configureTableLabel( table.getTitle(), table.getDescription() );
            colTableModel_.setItems( table.getColumns() );
            foreignTableModel_.setItems( table.getForeignKeys() );
            final JTable ct = colTable_;
            final JTable ft = foreignTable_;
            Runnable configer = new Runnable() {
                public void run() {
                    StarJTable.configureColumnWidths( ct, 360, 9999 );
                    StarJTable.configureColumnWidths( ft, 360, 9999 );
                }
            };
            if ( metaSplitter_.getSize().width > 0 ) {
                configer.run();
            }
            else {
                SwingUtilities.invokeLater( configer );
            }
        }
    }

    /**
     * Configures the table label given a table title and description.
     *
     * @param  title  table title (should be short)
     * @param  desc   table description (may be long)
     */
    private void configureTableLabel( String title, String desc ) {
        boolean hasTitle = title != null && title.trim().length() > 0;
        boolean hasDesc = desc != null && desc.trim().length() > 0;
        final String heading;
        if ( hasTitle ) {
            heading = title.trim().replaceAll( "\\s+", " " );
        }
        else if ( hasDesc ) {
            heading = desc.trim().replaceFirst( "(?s)[.,;]\\s.*", " ..." )
                                 .replaceAll( "\\s+", " " );
        }
        else {
            heading = null;
        }
        String note = null;
        if ( hasDesc ) {
            note = desc.trim().matches( "(?s).*[\n\r]+.*" ) 
                 ? "<html>" + desc.replaceAll( "[\r\n]+", "<br>" ) + "</html>"
                 : desc;
        }
        else {
            note = null;
        }
        tableLabel_.setText( heading );
        tableLabel_.setToolTipText( note );
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

    /**
     * Returns the ForeignMeta object associated with a given item
     * in the foreign key table model.  It's just a cast.
     *
     * @param  item   table cell contents
     * @return   foreign key object associated with <code>item</code>
     */
    private static ForeignMeta getForeign( Object item ) {
        return (ForeignMeta) item;
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
            new ArrayTableColumn( "Indexed", Boolean.class ) {
                public Object getValue( Object item ) {
                    return Boolean
                          .valueOf( Arrays.asList( getCol( item ).getFlags() )
                                          .indexOf( "indexed" ) >= 0 );
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
     * Constructs an array of columns which define the table model
     * to use for displaying foreign key information.
     *
     * @return  column descriptions
     */
    private static ArrayTableColumn[] createForeignMetaColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Target Table", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getTargetTable();
                }
            },
            new ArrayTableColumn( "Links", String.class ) {
                public Object getValue( Object item ) {
                    ForeignMeta.Link[] links = getForeign( item ).getLinks();
                    StringBuffer sbuf = new StringBuffer();
                    for ( int i = 0; i < links.length; i++ ) {
                        ForeignMeta.Link link = links[ i ];
                        if ( i > 0 ) {
                            sbuf.append( "; " );
                        }
                        sbuf.append( link.getFrom() )
                            .append( "->" )
                            .append( link.getTarget() );
                    }
                    return sbuf.toString();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getDescription();
                }
            },
            new ArrayTableColumn( "Utype", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getUtype();
                }
            },
        };
    }
}
