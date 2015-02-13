package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Displays the metadata from an array of SchemaMeta objects.
 * These can be acquired from a TableSet XML document as exposed
 * by VOSI and TAP interfaces or from interrogating TAP_SCHEMA tables.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2011
 */
public class TableSetPanel extends JPanel {

    private final JTree tTree_;
    private final TreeSelectionModel selectionModel_;
    private final JTable colTable_;
    private final JTable foreignTable_;
    private final ArrayTableModel colTableModel_;
    private final ArrayTableModel foreignTableModel_;
    private final MetaColumnModel colColModel_;
    private final MetaColumnModel foreignColModel_;
    private final JComponent metaPanel_;
    private final JSplitPane metaSplitter_;
    private final JLabel tableLabel_;
    private SchemaMeta[] schemas_;

    /**
     * Constructor.
     */
    public TableSetPanel() {
        super( new BorderLayout() );
        tTree_ = new JTree();
        tTree_.setRootVisible( false );
        tTree_.setShowsRootHandles( true );
        tTree_.setExpandsSelectedPaths( true );
        selectionModel_ = tTree_.getSelectionModel();
        selectionModel_.setSelectionMode( TreeSelectionModel
                                         .SINGLE_TREE_SELECTION );
        selectionModel_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                updateForSelectedTable();
            }
        } );

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

        tableLabel_ = new JLabel();
        JComponent tLine = Box.createHorizontalBox();
        tLine.add( new JLabel( "Table: " ) );
        tLine.add( tableLabel_ );

        JComponent detailPanel = new JPanel( new BorderLayout() );
        detailPanel.add( tLine, BorderLayout.NORTH );
        detailPanel.add( new JScrollPane( colTable_ ), BorderLayout.CENTER );

        metaSplitter_ = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        metaSplitter_.setBorder( BorderFactory.createEmptyBorder() );
        metaSplitter_.setLeftComponent( new JScrollPane( tTree_ ) );
        metaSplitter_.setRightComponent( detailPanel );

        metaPanel_ = new JPanel( new BorderLayout() );
        metaPanel_.add( metaSplitter_, BorderLayout.CENTER );
        add( metaPanel_, BorderLayout.CENTER );
        setSchemas( null );
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
     * The data is in the form of an array of schema metadata objects.
     *
     * @param  schemas  schema metadata objects, null if no metadata available
     */
    public void setSchemas( SchemaMeta[] schemas ) {
        schemas_ = schemas;
        TreeModel treeModel =
            new TapMetaTreeModel( schemas_ == null ? new SchemaMeta[ 0 ]
                                                   : schemas_ );
        tTree_.setModel( treeModel );
        selectionModel_.setSelectionPath( null );

        metaPanel_.removeAll();
        metaPanel_.add( metaSplitter_ );
        metaPanel_.revalidate();
        updateForSelectedTable();
        repaint();
    }

    /**
     * Returns the most recently set table metadata set.
     *
     * @return   current schema metadata array, may be null
     */
    public SchemaMeta[] getSchemas() {
        return schemas_;
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
        TreePath path = selectionModel_.getSelectionPath();
        Object tail = path == null ? null : path.getLastPathComponent();
        return TapMetaTreeModel.getTable( tail );
    }

    /**
     * Invoked when table selection may have changed.
     */
    private void updateForSelectedTable() {
        TableMeta table = getSelectedTable();
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
