package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ArrayTableSorter;
import uk.ac.starlink.util.gui.ErrorDialog;
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

    private final TapTableLoadDialog tld_;
    private final JTree tTree_;
    private final CountTableTreeCellRenderer renderer_;
    private final JTextField keywordField_;
    private final AndButton keyAndButt_;
    private final JCheckBox useNameButt_;
    private final JCheckBox useDescripButt_;
    private final JRadioButton sortAlphaButt_;
    private final JRadioButton sortServiceButt_;
    private final TreeSelectionModel selectionModel_;
    private final JTree featureTree_;
    private final FeatureTreeModel featureTreeModel_;
    private final JTable colTable_;
    private final JTable foreignTable_;
    private final ArrayTableModel<ColumnMeta> colTableModel_;
    private final ArrayTableModel<ForeignMeta> foreignTableModel_;
    private final ResourceMetaPanel servicePanel_;
    private final SchemaMetaPanel schemaPanel_;
    private final TableMetaPanel tablePanel_;
    private final HintPanel hintPanel_;
    private final JTabbedPane detailTabber_;
    private final JScrollPane treeScroller_;
    private final int itabService_;
    private final int itabFeature_;
    private final int itabSchema_;
    private final int itabTable_;
    private final int itabCol_;
    private final int itabForeign_;
    private final int itabHint_;
    private final JComponent treeContainer_;
    private TapServiceKit serviceKit_;
    private SchemaMeta[] schemas_;
    private ColumnMeta[] selectedColumns_;
    private TapMetaTreeModel treeModel_;
    private static final String featureTitle_ = "ADQL";
    private static final List<ColMetaColumn<?>> colMetaColumns_ =
        createColumnMetaColumns();

    /**
     * Name of bound property for table selection.
     * Property value is the return value of {@link #getSelectedTable}.
     */
    public static final String TABLE_SELECTION_PROPERTY = "selectedTable";

    /**
     * Name of bound property for column list selection.
     * Property value is the return value of {@link #getSelectedColumns}.
     */
    public static final String COLUMNS_SELECTION_PROPERTY = "selectedColumns";

    /**
     * Name of bound property for schema array giving table metadata.
     * Property value is the return value of {@link #getSchemas}.
     */
    public static final String SCHEMAS_PROPERTY = "schemas";

    /** Number of nodes below which tree nodes are expanded. */
    private static final int TREE_EXPAND_THRESHOLD = 100;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   tld  table load dialog configuring this panel
     */
    public TableSetPanel( TapTableLoadDialog tld ) {
        super( new BorderLayout() );
        tld_ = tld;

        /* Set up the schema/table metadata display tree. */
        renderer_ = new CountTableTreeCellRenderer();
        tTree_ = new JTree();
        tTree_.setRootVisible( true );
        tTree_.setShowsRootHandles( false );
        tTree_.setExpandsSelectedPaths( true );
        tTree_.setCellRenderer( renderer_ );
        selectionModel_ = tTree_.getSelectionModel();
        selectionModel_.setSelectionMode( TreeSelectionModel
                                         .SINGLE_TREE_SELECTION );
        selectionModel_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                updateForTableSelection();
                TableMeta oldSel =
                    TapMetaTreeModel.getTable( evt.getOldLeadSelectionPath() );
                TableMeta newSel =
                    TapMetaTreeModel.getTable( evt.getNewLeadSelectionPath() );
                assert newSel == getSelectedTable();
                TableSetPanel.this
                             .firePropertyChange( TABLE_SELECTION_PROPERTY,
                                                  oldSel, newSel );
            }
        } );

        /* Construct components for filtering tree by keyword. */
        keywordField_ = new JTextField();
        keywordField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateTree( false );
            }
        } );
        keywordField_.setToolTipText( "Enter one or more search terms "
                                    + "to restrict the content of "
                                    + "the metadata display tree" );
        keyAndButt_ = new AndButton( false );
        keyAndButt_.setMargin( new Insets( 0, 0, 0, 0 ) );
        keyAndButt_.setToolTipText( "Choose to match either "
                                  + "all (And) or any (Or) "
                                  + "of the entered search terms "
                                  + "against table metadata" );
        useNameButt_ = new JCheckBox( "Name", true );
        useNameButt_.setToolTipText( "Select to match search terms against "
                                   + "table/schema names" );
        useDescripButt_ = new JCheckBox( "Descrip", false );
        useDescripButt_.setToolTipText( "Select to match search terms against "
                                      + "table/schema descriptions" );
        ActionListener findParamListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( ! useNameButt_.isSelected() &&
                     ! useDescripButt_.isSelected() ) {
                    ( evt.getSource() == useNameButt_ ? useDescripButt_
                                                      : useNameButt_ )
                   .setSelected( true );
                }
                updateTree( false );
            }
        };
        keyAndButt_.addAndListener( findParamListener );
        useNameButt_.addActionListener( findParamListener );
        useDescripButt_.addActionListener( findParamListener );

        /* Construct components for sorting the tree. */
        sortAlphaButt_ = new JRadioButton( "Alphabetic" );
        sortServiceButt_ = new JRadioButton( "Service" );
        ButtonGroup sortGrp = new ButtonGroup();
        sortGrp.add( sortAlphaButt_ );
        sortGrp.add( sortServiceButt_ );
        sortAlphaButt_.setToolTipText( "Select for alphabetic ordering" );
        sortServiceButt_.setToolTipText( "Select for service-defined ordering");
        sortAlphaButt_.addActionListener( evt -> updateTreeOrder() );
        sortServiceButt_.addActionListener( evt -> updateTreeOrder() );
        sortServiceButt_.setSelected( true );

        /* Create table for column metadata display. */
        colTableModel_ = new ArrayTableModel<ColumnMeta>( new ColumnMeta[ 0 ] );
        colTableModel_.setColumns( colMetaColumns_ );
        colTable_ = new JTable( colTableModel_ );
        StarJTable.configureDefaultRenderers( colTable_ );
        colTable_.setColumnSelectionAllowed( false );
        new ArrayTableSorter<ColumnMeta>( colTableModel_ )
           .install( colTable_.getTableHeader() );
        ListSelectionModel colSelModel = colTable_.getSelectionModel();
        colSelModel.setSelectionMode( ListSelectionModel
                                     .MULTIPLE_INTERVAL_SELECTION );
        colSelModel.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                columnSelectionChanged();
            }
        } );
        selectedColumns_ = new ColumnMeta[ 0 ];

        /* Create table for foreign key display. */
        foreignTableModel_ =
            new ArrayTableModel<ForeignMeta>( new ForeignMeta[ 0 ] );
        foreignTableModel_.setColumns( createForeignMetaColumns() );
        foreignTable_ = new JTable( foreignTableModel_ );
        StarJTable.configureDefaultRenderers( foreignTable_ );
        foreignTable_.setColumnSelectionAllowed( false );
        foreignTable_.setRowSelectionAllowed( false );
        new ArrayTableSorter<ForeignMeta>( foreignTableModel_ )
           .install( foreignTable_.getTableHeader() );

        /* Create tree for ADQL language feature display. */
        featureTree_ = new JTree();
        JScrollPane featureScroller = metaScroller( featureTree_ );
        featureTreeModel_ = new FeatureTreeModel( featureScroller );
        featureTree_.setModel( featureTreeModel_ );
        featureTree_.setRootVisible( false );
        featureTree_.setShowsRootHandles( true );
        featureTree_.setSelectionModel( null );
        featureTree_.setLargeModel( false );
        featureTree_.putClientProperty( "JTree.lineStyle", "None" );
        featureTree_.setCellRenderer( FeatureTreeModel.createRenderer() );
        featureTreeModel_.addTreeModelListener( new TreeModelListener() {
            public void treeNodesChanged( TreeModelEvent evt ) {
            }
            public void treeNodesInserted( TreeModelEvent evt ) {
            }
            public void treeNodesRemoved( TreeModelEvent evt ) {
            }
            public void treeStructureChanged( TreeModelEvent evt ) {
                TreePath path = evt.getTreePath();
                if ( path.getPathCount() == 1 ) {
                    Object root = path.getLastPathComponent();
                    int nc = featureTreeModel_.getChildCount( root );
                    for ( int i = 0; i < nc; i++ ) {
                        Object child = featureTreeModel_.getChild( root, i );
                        featureTree_.expandPath( path
                                                .pathByAddingChild( child ) );
                    }
                }
            }
        } );

        /* Construct and place tabs to display individual metadata items. */
        Consumer<URL> urlHandler = url -> tld.getUrlHandler().accept( url );
        tablePanel_ = new TableMetaPanel();
        schemaPanel_ = new SchemaMetaPanel();
        servicePanel_ = new ResourceMetaPanel( urlHandler );
        hintPanel_ = new HintPanel( urlHandler );
        detailTabber_ = new JTabbedPane();
        int itab = 0;
        detailTabber_.addTab( "Service", metaScroller( servicePanel_ ) );
        itabService_ = itab++;
        detailTabber_.addTab( featureTitle_, featureScroller );
        itabFeature_ = itab++;
        detailTabber_.addTab( "Schema", metaScroller( schemaPanel_ ) );
        itabSchema_ = itab++;
        detailTabber_.addTab( "Table", metaScroller( tablePanel_ ) );
        itabTable_ = itab++;
        detailTabber_.addTab( "Columns", new JScrollPane( colTable_ ) );
        itabCol_ = itab++;
        detailTabber_.addTab( "FKeys", new JScrollPane( foreignTable_ ) );
        itabForeign_ = itab++;
        detailTabber_.addTab( "Hints", new JScrollPane( hintPanel_ ) );
        itabHint_ = itab++;
        detailTabber_.setSelectedIndex( itabCol_ );

        /* Prepare container for tree search filter query components. */
        JComponent findWordBox = Box.createHorizontalBox();
        findWordBox.add( keywordField_ );
        findWordBox.add( Box.createHorizontalStrut( 5 ) );
        findWordBox.add( keyAndButt_ );
        JComponent findFieldBox = Box.createHorizontalBox();
        useNameButt_.setMargin( new Insets( 0, 0, 0, 0 ) );
        useDescripButt_.setMargin( new Insets( 0, 0, 0, 0 ) );
        findFieldBox.add( useNameButt_ );
        findFieldBox.add( useDescripButt_ );
        GridBagLayout gridLayer = new GridBagLayout();
        JComponent findBox = new JPanel( gridLayer );
        GridBagConstraints gcons = new GridBagConstraints();
        gcons.gridx = 0;
        gcons.gridy = 0;
        gcons.anchor = GridBagConstraints.WEST;
        JLabel findLabel = new JLabel( "Find: " );
        gridLayer.setConstraints( findLabel, gcons );
        findBox.add( findLabel );
        gcons.gridx++;
        gcons.weightx = 1.0;
        gcons.fill = GridBagConstraints.HORIZONTAL;
        gridLayer.setConstraints( findWordBox, gcons );
        findBox.add( findWordBox );
        gcons.gridy++;
        gcons.fill = GridBagConstraints.NONE;
        gridLayer.setConstraints( findFieldBox, gcons );
        findBox.add( findFieldBox );

        /* Prepare container for tree sort option components. */
        JComponent sortBox = Box.createHorizontalBox();
        sortBox.add( new JLabel( "Sort: " ) );
        sortBox.add( sortServiceButt_ );
        sortBox.add( sortAlphaButt_ );
        sortBox.add( Box.createHorizontalGlue() );

        /* Position search and sort components near the tree. */
        JComponent treePanel = new JPanel( new BorderLayout() );
        treeContainer_ = new JPanel( new BorderLayout() );
        treeScroller_ = new JScrollPane( tTree_ );
        treeContainer_.add( treeScroller_, BorderLayout.CENTER );
        treePanel.add( treeContainer_, BorderLayout.CENTER );
        treePanel.add( sortBox, BorderLayout.SOUTH );
        JComponent treeOptBox = Box.createVerticalBox();
        treeOptBox.add( findBox );
        treeOptBox.add( sortBox );
        treeOptBox.setBorder( BorderFactory.createEmptyBorder( 0, 2, 0, 2 ) );
        treePanel.add( treeOptBox, BorderLayout.NORTH );

        /* Place tree and tabber in this panel. */
        JSplitPane metaSplitter = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        metaSplitter.setBorder( BorderFactory.createEmptyBorder() );
        JComponent detailPanel = new JPanel( new BorderLayout() );
        detailPanel.add( detailTabber_, BorderLayout.CENTER );
        detailPanel.add( createHorizontalStrut( 500 ), BorderLayout.SOUTH );
        metaSplitter.setLeftComponent( treePanel );
        metaSplitter.setRightComponent( detailPanel );
        add( metaSplitter, BorderLayout.CENTER );

        /* Set initial state. */
        setServiceKit( null );
        setCapability( null );
    }

    /**
     * Installs an object that knows how to acquire TAP service metadata.
     * If the supplied kit is non-null, calling this method
     * initiates asynchronous reads of metadata, which will be displayed
     * in this panel when it arrives.
     *
     * @param  serviceKit  TAP service metadata access kit
     */
    public void setServiceKit( final TapServiceKit serviceKit ) {
        if ( serviceKit_ != null ) {
            serviceKit_.shutdown();
        }
        serviceKit_ = serviceKit;
        setSchemas( null );
        setResourceInfo( null );
        if ( serviceKit == null ) {
            servicePanel_.setId( null, null );
        }
        else {
            final String ivoid = serviceKit.getIvoid();
            servicePanel_.setId( serviceKit.getTapService().getIdentity(),
                                 ivoid );
            serviceKit.acquireResource(
                           new ResultHandler<Map<String,String>>() {
                public boolean isActive() {
                    return serviceKit == serviceKit_;
                }
                public void showWaiting() {
                    logger_.info( "Reading resource record for " + ivoid );
                }
                public void showResult( Map<String,String> resourceMap ) {
                    setResourceInfo( resourceMap );
                }
                public void showError( IOException error ) {
                    setResourceInfo( null );
                }
            } );
            serviceKit.acquireRoles( new ResultHandler<RegRole[]>() {
                public boolean isActive() {
                    return serviceKit == serviceKit_;
                }
                public void showWaiting() {
                    logger_.info( "Reading res_role records for " + ivoid );
                }
                public void showResult( RegRole[] roles ) {
                    setResourceRoles( roles );
                }
                public void showError( IOException error ) {
                    setResourceRoles( null );
                }
            } );
            serviceKit.acquireSchemas( new ResultHandler<SchemaMeta[]>() {
                private JProgressBar progBar;
                public boolean isActive() {
                    return serviceKit == serviceKit_;
                }
                public void showWaiting() {
                    logger_.info( "Reading up-front table metadata" );
                    progBar = showFetchProgressBar();
                }
                public void showResult( SchemaMeta[] result ) {
                    stopProgress();
                    logger_.info( "Read " + getMetaDescrip() );
                    setSchemas( result );
                }
                public void showError( IOException error ) {
                    stopProgress();
                    logger_.log( Level.WARNING,
                                 "Error reading " + getMetaDescrip(), error );
                    showFetchFailure( error, serviceKit.getMetaReader() );
                }
                private void stopProgress() {
                    if ( progBar != null ) {
                        progBar.setIndeterminate( false );
                        progBar.setValue( 0 );
                        progBar = null;
                    }
                }
                private String getMetaDescrip() {
                    TapMetaReader metaRdr = serviceKit.getMetaReader();
                    StringBuffer sbuf = new StringBuffer()
                        .append( "up-front table metadata" );
                    if ( metaRdr != null ) {
                        sbuf.append( " from " )
                            .append( metaRdr.getSource() )
                            .append( " using " )
                            .append( metaRdr.getMeans() );
                    }
                    return sbuf.toString();
                }
            } );
        }
    }

    /**
     * Returns the object currently responsible for acquiring table metadata.
     *
     * @return  metadata access kit, may be null
     */
    public TapServiceKit getServiceKit() {
        return serviceKit_;
    }

    /**
     * Returns the current table metadata set.
     * May be null if the read is still in progress.
     *
     * @return   current schema metadata array, may be null
     */
    public SchemaMeta[] getSchemas() {
        return schemas_;
    }

    /**
     * Sets the data model for the metadata displayed by this panel,
     * and updates the display.
     * The data is in the form of an array of schema metadata objects.
     *
     * @param  schemas  schema metadata objects, null if no metadata available
     */
    private void setSchemas( SchemaMeta[] schemas ) {
        if ( schemas != null ) {
            checkSchemasPopulated( schemas );
        }
        SchemaMeta[] oldSchemas = schemas_;
        schemas_ = schemas;
        treeModel_ =
            new TapMetaTreeModel( schemas_ == null ? new SchemaMeta[ 0 ]
                                                   : schemas_,
                                  getTreeOrder() );
        tTree_.setModel( new MaskTreeModel( treeModel_, true ) );
        updateTree( true );
        SwingUtilities.invokeLater( () -> highlightFinderSelection() );
        final String countTxt;
        if ( schemas == null ) {
            countTxt = "no metadata";
        }
        else {
            int nTable = 0;
            for ( SchemaMeta schema : schemas ) {
                nTable += schema.getTables().length;
            }
            countTxt = schemas.length + " schemas, " + nTable + " tables";
        }
        servicePanel_.setSize( countTxt );
        replaceTreeComponent( null );
        updateForTableSelection();
        firePropertyChange( SCHEMAS_PROPERTY, oldSchemas, schemas );
        repaint();
    }

    /**
     * Sets the TapCapability information to be displayed in this panel.
     *
     * @param   capability   current capability object, may be null
     */
    public void setCapability( TapCapability capability ) {
        servicePanel_.setCapability( capability );
        featureTreeModel_.setCapability( capability );
        detailTabber_.setIconAt( itabFeature_,
                                 activeIcon( capability != null ) );
    }

    /**
     * Sets whether an examples document is known to be available
     * at the examples endpoint.
     *
     * @param   hasExamples  true iff examples are known to exist
     */
    public void setHasExamples( boolean hasExamples ) {
        URL exampleUrl = hasExamples && serviceKit_ != null
                       ? serviceKit_.getTapService().getExamplesEndpoint()
                       : null;
        String exurl = exampleUrl == null ? null : exampleUrl.toString();
        servicePanel_.setExamplesUrl( exurl );
        hintPanel_.setExamplesUrl( exurl );
    }

    /**
     * Sets the version of the ADQL language for which this panel
     * is showing metadata.
     *
     * @param  version  ADQL version
     */
    public void setAdqlVersion( AdqlVersion version ) {
        hintPanel_.setAdqlVersion( version == null ? AdqlVersion.V20
                                                   : version );
        featureTreeModel_.setAdqlVersion( version );
    }

    /**
     * Displays information about the registry resource corresponding to
     * the TAP service represented by this panel.
     * The argument is a map of standard RegTAP resource column names
     * to their values.
     *
     * @param  map  map of service resource metadata items,
     *              or null for no info
     */
    private void setResourceInfo( Map<String,String> map ) {
        String rootName = null;
        if ( map != null ) {
            if ( rootName == null || rootName.trim().length() == 0 ) {
                rootName = map.get( "short_name" );
            }
            if ( rootName == null || rootName.trim().length() == 0 ) {
                rootName = map.get( "res_title" );
            }
        }
        renderer_.rootName_ = rootName;
        tTree_.repaint();
        servicePanel_.setResourceInfo( map == null
                                     ? new HashMap<String,String>()
                                     : map );
        detailTabber_.setIconAt( itabService_, activeIcon( map != null ) );
    }

    /**
     * Displays information about registry resource roles corresponding
     * to the TAP services represented by this panel.
     *
     * @param  roles  list of known roles, or null for no info
     */
    private void setResourceRoles( RegRole[] roles ) {
        servicePanel_.setResourceRoles( roles == null ? new RegRole[ 0 ]
                                                      : roles );
    }

    /**
     * Displays authenticated user identifer in this panel.
     *
     * @param  authId  string representing user-readable authentication status
     */
    public void setAuthId( String authId ) {
        servicePanel_.setAuthId( authId );
    }

    /**
     * Configure this panel so that it makes prominent the table
     * currently selected in the table finder panel, if any.
     */
    public void highlightFinderSelection() {
        TapServiceFinderPanel finderPanel = tld_ == null
                                          ? null
                                          : tld_.getFinderPanel();
        TapServiceFinder.Table selTable = finderPanel == null
                                        ? null
                                        : finderPanel.getSelectedTable();
        String tname = selTable == null ? null : selTable.getName();
        TreePath selPath = treeModel_.getPathForTableName( tname );
        selectionModel_.setSelectionPath( selPath );
        scrollTreeToPath( selPath );
    }

    /**
     * Returns the tree ordering option currently selected in the GUI.
     *
     * @return   metadata tree order
     */
    private TapMetaOrder getTreeOrder() {
        return sortServiceButt_.isSelected() ? TapMetaOrder.INDEXED
                                             : TapMetaOrder.ALPHABETIC;
    }

    /**
     * Called if the metadata ordering sequence might have changed.
     * Updates the tree state, retaining as much of the GUI state
     * (current node selection, path expansion states) as is possible.
     */
    private void updateTreeOrder() {
        TapMetaOrder order = getTreeOrder();
        if ( treeModel_ != null && treeModel_.getOrder() != order ) {

            /* Gather GUI state information that we would like to preserve
             * over the ordering reset. */
            TreePath selPath = tTree_.getSelectionPath();
            List<TreePath> expandeds = new ArrayList<>();
            TreePath rootPath = new TreePath( treeModel_.getRoot() );
            for ( Enumeration<TreePath> exEn =
                      tTree_.getExpandedDescendants( rootPath );
                  exEn.hasMoreElements(); ) {
                expandeds.add( exEn.nextElement() );
            }

            /* Actually reset the order. */
            treeModel_.setOrder( order );

            /* Restore GUI state information. */
            for ( TreePath exp : expandeds ) {
                tTree_.expandPath( exp );
            }
            if ( selPath != null ) {
                tTree_.setSelectionPath( selPath );
                scrollTreeToPath( selPath );
            }
        }
    }

    /**
     * Make sure that the tree is scrolled appropriately so that
     * a given path is visible to the user.
     * If the supplied path is null, the scroller position is reset
     * to view the top.
     *
     * @param  path  path in tree to scroll to, or null
     */
    private void scrollTreeToPath( TreePath path ) {
        JScrollBar hbar = treeScroller_.getHorizontalScrollBar();
        JScrollBar vbar = treeScroller_.getVerticalScrollBar();
        if ( path == null ) {
            hbar.setValue( hbar.getMinimum() );
            vbar.setValue( vbar.getMinimum() );
        }
        else {
            int hscroll = hbar == null ? 0 : hbar.getValue();

            /* Get the position of the node in the tree.
             * If it doesn't seem to be in the tree, assume that it's
             * been filtered out by a keyword search condition and reset
             * the filter to include all paths.  After that we should
             * be able to get a position for the indicated path.
             * Unless the path isn't in the tree at all; that shouldn't
             * happen, but nothing terrible will happen in that case. */
            Rectangle pathBounds = tTree_.getPathBounds( path );
            if ( pathBounds == null ) {
                keywordField_.setText( "" );
                pathBounds = tTree_.getPathBounds( path );
            }
            assert pathBounds != null;

            /* If possible, try to get both the path and its parent in view. */
            TreePath parentPath = path.getParentPath();
            if ( parentPath != null ) {
                tTree_.scrollPathToVisible( parentPath );
            }
            if ( pathBounds != null ) {
                Rectangle viewBounds =
                    treeScroller_.getViewport().getViewRect();
                if ( ! ( viewBounds.y <= pathBounds.y &&
                         viewBounds.y + viewBounds.height >=
                         pathBounds.y + pathBounds.height ) ) {
                    tTree_.scrollPathToVisible( path );
                }
            }

            /* We only want to change the scroll position vertically,
             * not horizontally, so reset the horizontal scroll to its
             * former position. */
            if ( hbar != null ) {
                hbar.setValue( hscroll );
            }
        }
    }

    /**
     * Displays a progress bar to indicate that metadata fetching is going on.
     *
     * @return  the progress bar component
     */
    private JProgressBar showFetchProgressBar() {
        JProgressBar progBar = new JProgressBar();
        progBar.setIndeterminate( true );
        JComponent progLine = Box.createHorizontalBox();
        progLine.add( Box.createHorizontalGlue() );
        progLine.add( progBar );
        progLine.add( Box.createHorizontalGlue() );
        JComponent workBox = Box.createVerticalBox();
        workBox.add( Box.createVerticalGlue() );
        workBox.add( createLabelLine( "Fetching table metadata" ) );
        workBox.add( Box.createVerticalStrut( 5 ) );
        workBox.add( progLine );
        workBox.add( Box.createVerticalGlue() );
        JComponent workPanel = new JPanel( new BorderLayout() );
        workPanel.setBackground( UIManager.getColor( "Tree.background" ) );
        workPanel.setBorder( BorderFactory.createEtchedBorder() );
        workPanel.add( workBox, BorderLayout.CENTER );
        replaceTreeComponent( workPanel );
        return progBar;
    }

    /**
     * Displays an indication that metadata fetching failed.
     *
     * @param  error   error that caused the failure
     * @param  metaReader   metadata reader
     */
    private void showFetchFailure( Throwable error, TapMetaReader metaReader ) {

        /* Pop up an error dialog. */
        List<String> msgList = new ArrayList<String>();
        msgList.add( "Error reading TAP service table metadata" );
        if ( metaReader != null ) {
            msgList.add( "Method: " + metaReader.getMeans() );
            msgList.add( "Source: " + metaReader.getSource() );
        }
        String[] msgLines = msgList.toArray( new String[ 0 ] );
        ErrorDialog.showError( this, "Table Metadata Error", error, msgLines );

        /* Prepare a component describing what went wrong. */
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
        JComponent linesBox = Box.createVerticalBox();
        linesBox.add( Box.createVerticalGlue() );
        linesBox.add( createLabelLine( "No table metadata" ) );
        linesBox.add( Box.createVerticalStrut( 15 ) );
        for ( String line : msgLines ) {
            linesBox.add( createLabelLine( line ) );
        }
        linesBox.add( Box.createVerticalStrut( 15 ) );
        linesBox.add( errLine );
        linesBox.add( Box.createVerticalGlue() );
        JComponent panel = new JPanel( new BorderLayout() );
        panel.add( linesBox, BorderLayout.CENTER );
        JScrollPane scroller = new JScrollPane( panel );

        /* Post it in place of the metadata jtree display. */
        replaceTreeComponent( scroller );
    }

    /**
     * Returns a component containing some text, suitable for adding to
     * a list of text lines.
     *
     * @param  text  content
     * @return  jlabel
     */
    private JComponent createLabelLine( String text ) {
        JLabel label = new JLabel( text );
        label.setAlignmentX( 0 );
        return label;
    }

    /**
     * Places a component where the schema metadata JTree normally goes.
     * If the supplied component is null, the tree is put back.
     *
     * @param  content  component to replace tree, or null
     */
    public void replaceTreeComponent( JComponent content ) {
        treeContainer_.removeAll();
        treeContainer_.add( content != null ? content : treeScroller_,
                            BorderLayout.CENTER );
        treeContainer_.revalidate();
        treeContainer_.repaint();
    }

    /**
     * Checks that all the schemas are populated with lists of their tables.
     * The SchemaMeta and TapMetaReader interface permit unpopulated schemas,
     * but this GUI relies in some places on the assumption that schemas
     * are always populated, so things will probably go wrong if it's not
     * the case.  Log a warning in case of unpopulated schemas
     * to give a clue what's wrong.
     *
     * @param  schemas  schemas to test
     */
    private void checkSchemasPopulated( SchemaMeta[] schemas ) {
        for ( SchemaMeta smeta : schemas ) {
            if ( smeta.getTables() == null ) {
                logger_.warning( "Schema metadata object(s) not populated"
                               + " with tables, probably will cause trouble"
                               + "; use a different TapMetaReader?" );
                return;
            }
        }
    }

    /**
     * Returns the table which is currently selected for metadata display.
     *
     * @return   selected table, may be null
     */
    public TableMeta getSelectedTable() {
        return TapMetaTreeModel.getTable( selectionModel_.getSelectionPath() );
    }

    /**
     * Returns an array of the columns which are currently selected in
     * the column metadata display table.
     *
     * @return   array of selected columns, may be empty but not null
     */
    public ColumnMeta[] getSelectedColumns() {
        return selectedColumns_;
    }

    /**
     * Invoked when table selection may have changed.
     */
    private void updateForTableSelection() {
        TreePath path = selectionModel_.getSelectionPath();
        final TableMeta table = TapMetaTreeModel.getTable( path );
        SchemaMeta schema = TapMetaTreeModel.getSchema( path );
        if ( table == null ||
             ! serviceKit_.onColumns( table, new Runnable() {
            public void run() {
                if ( table == getSelectedTable() ) {
                    ColumnMeta[] cols = table.getColumns();
                    displayColumns( table, cols );
                    tablePanel_.setColumns( cols );
                }
            }
        } ) ) {
            displayColumns( table, new ColumnMeta[ 0 ] );
        }
        if ( table == null ||
             ! serviceKit_.onForeignKeys( table, new Runnable() {
            public void run() {
                if ( table == getSelectedTable() ) {
                    ForeignMeta[] fkeys = table.getForeignKeys();
                    displayForeignKeys( table, fkeys );
                    tablePanel_.setForeignKeys( fkeys );
                }
            }
        } ) ) {
            displayForeignKeys( table, new ForeignMeta[ 0 ] );
        }
        schemaPanel_.setSchema( schema );
        detailTabber_.setIconAt( itabSchema_, activeIcon( schema != null ) );
        tablePanel_.setTable( table );
        detailTabber_.setIconAt( itabTable_, activeIcon( table != null ) );
    }

    /**
     * Invoked when the column selection may have changed.
     */
    private void columnSelectionChanged() {
        ColumnMeta[] oldCols = selectedColumns_;

        /* Get a list of all the columns in the current selection. */
        Collection<ColumnMeta> curSet = new HashSet<ColumnMeta>();
        ListSelectionModel selModel = colTable_.getSelectionModel();
        if ( ! selModel.isSelectionEmpty() ) {
            ColumnMeta[] colmetas = colTableModel_.getItems();
            int imin = selModel.getMinSelectionIndex();
            int imax = selModel.getMaxSelectionIndex();
            for ( int i = imin; i <= imax; i++ ) {
                if ( selModel.isSelectedIndex( i ) ) {
                    curSet.add( colmetas[ i ] );
                }
            }
        }

        /* Prepare a list with the same content as this selection list,
         * but following the sequence of the elements in the previous
         * version of the list as much as possible.
         * This could be done a bit more elegantly with collections
         * if ColumnMeta implemented object equality properly,
         * but it doesn't. */
        List<ColumnMeta> newList = new ArrayList<ColumnMeta>();
        for ( ColumnMeta col : oldCols ) {
            ColumnMeta oldCol = getNamedEntry( curSet, col.getName() );
            if ( oldCol != null ) {
                newList.add( oldCol );
            }
        }
        for ( ColumnMeta col : curSet ) {
            if ( getNamedEntry( newList, col.getName() ) == null ) {
                newList.add( col );
            }
        }
        assert new HashSet<ColumnMeta>( newList ).equals( curSet );
        selectedColumns_ = newList.toArray( new ColumnMeta[ 0 ] );

        /* Notify listeners if required. */
        if ( ! Arrays.equals( selectedColumns_, oldCols ) ) {
            firePropertyChange( COLUMNS_SELECTION_PROPERTY,
                                oldCols, selectedColumns_ );
        }
    }

    /**
     * Updates the display if required for the columns of a table.
     *
     * @param  table  table
     * @param  cols  columns
     */
    private void displayColumns( TableMeta table, ColumnMeta[] cols ) {
        assert table == getSelectedTable();
        final String[] extras;
        if ( cols != null && cols.length > 0 ) {

            /* Determine what non-standard columns should appear in the
             * column display JTable; this depends on the content of the
             * "extras" maps in the relevant ColumnMeta objects. */
            Map<String,List<Object>> extrasMap =
                new LinkedHashMap<String,List<Object>>();
            for ( ColumnMeta col : cols ) {
                for ( Map.Entry<String,Object> entry :
                      col.getExtras().entrySet() ) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if ( ! extrasMap.containsKey( key ) ) {
                        extrasMap.put( key, new ArrayList<Object>() );
                    }
                    extrasMap.get( key ).add( value );
                }
            }
            extras = extrasMap.keySet().toArray( new String[ 0 ] );
            List<ColMetaColumn<?>> colList = new ArrayList<ColMetaColumn<?>>();
            colList.addAll( colMetaColumns_ );
            for ( Map.Entry<String,List<Object>> entry :
                  extrasMap.entrySet() ) {
                colList.add( ExtraColumn.createInstance( entry.getKey(),
                                                         entry.getValue() ) );
            }

            /* If the set of columns is different than that currently
             * displayed, update the JTable's ColumnModel.
             * But don't do it if it's not necessary, since it will wipe
             * out things like display column ordering, which it's nice
             * to keep if possible. */
            if ( ! new HashSet<Object>( colTableModel_.getColumns() )
                  .equals( new HashSet<Object>( colList ) ) ) {
                colTableModel_.setColumns( colList );
            }
        }
        else {
            extras = new String[ 0 ];
        }
        tablePanel_.setColumnExtras( extras );

        /* Update the data for the columns table. */
        colTableModel_.setItems( cols );
        detailTabber_.setIconAt( itabCol_, activeIcon( cols != null &&
                                                       cols.length > 0 ) );
        if ( table != null ) {
            configureColumnWidths( colTable_ );
        }
        columnSelectionChanged();
    }

    /**
     * Updates the display if required for the foreign keys of a table.
     *
     * @param  table  table
     * @param  fkeys  foreign keys
     */
    private void displayForeignKeys( TableMeta table, ForeignMeta[] fkeys ) {
        assert table == getSelectedTable();
        foreignTableModel_.setItems( fkeys );
        detailTabber_.setIconAt( itabForeign_,
                                 activeIcon( fkeys != null &&
                                             fkeys.length > 0 ) );
        if ( table != null ) {
            configureColumnWidths( foreignTable_ );
        }
    }

    /**
     * Configures the columns widths of a JTable in the tabbed pane
     * to correspond to its current contents.
     *
     * @param  jtable   table to update
     */
    private void configureColumnWidths( final JTable jtable ) {
        Runnable configer = new Runnable() {
            public void run() {
                StarJTable.configureColumnWidths( jtable, 360, 9999 );
            }
        };
        if ( detailTabber_.getSize().width > 0 ) {
            configer.run();
        }
        else {
            SwingUtilities.invokeLater( configer );
        }
    }

    /**
     * Called if the schema information in the JTree or its presentation
     * rules may have changed.
     *
     * @param  dataChanged  true iff this update includes a change of
     *         the schema array underlying the tree model
     */
    private void updateTree( boolean dataChanged ) {

        /* We should have a MaskTreeModel, unless maybe there's no data. */
        TreeModel treeModel = tTree_.getModel();
        if ( ! ( treeModel instanceof MaskTreeModel ) ) {
            return;
        }
        MaskTreeModel mModel = (MaskTreeModel) treeModel;

        /* Get a node mask object from the GUI components. */
        final MaskTreeModel.Mask mask;
        String text = keywordField_.getText();
        if ( text == null || text.trim().length() == 0 ) {
            mask = null;
        }
        else {
            String[] searchTerms = text.split( "\\s+" );
            assert searchTerms.length > 0;
            boolean isAnd = keyAndButt_.isAnd();
            boolean useName = useNameButt_.isSelected();
            boolean useDescrip = useDescripButt_.isSelected();
            NodeStringer stringer =
                NodeStringer.createInstance( useName, useDescrip );
            mask = new WordMatchMask( searchTerms, stringer, isAnd );
        }

        /* We will be changing the mask, which will cause a
         * treeStructureChanged TreeModelEvent to be sent to listeners,
         * more or less wiping out any state of the JTree view.
         * So store the view state we want to preserve (information about
         * selections and node expansion) here, so we can restore it after
         * the model has changed. */
        Object root = mModel.getRoot();
        TreePath[] selections = tTree_.getSelectionPaths();
        List<TreePath> expandedList = new ArrayList<TreePath>();
        for ( Enumeration<TreePath> tpEn =
                  tTree_.getExpandedDescendants( new TreePath( root ) );
              tpEn.hasMoreElements(); ) {
            expandedList.add( tpEn.nextElement() );
        }
        TreePath[] oldExpanded = expandedList.toArray( new TreePath[ 0 ] );
        int oldCount = mModel.getNodeCount();

        /* Update the model. */
        mModel.setMask( mask );

        /* Apply node expansions in the JTree view.  This is a bit ad hoc.
         * If we've just cut the tree down from huge to manageable,
         * expand all the top-level (schema) nodes.  Conversely,
         * if we've just grown it from manageable to huge, collapse
         * all the top-level nodes.  Otherwise, try to retain (restore)
         * the previous expansion state.  */
        int newCount = mModel.getNodeCount();
        int ne = TREE_EXPAND_THRESHOLD;
        final TreePath[] newExpanded;
        if ( ( dataChanged || oldCount > ne ) && newCount < ne ) {
            int nc = mModel.getChildCount( root );
            newExpanded = new TreePath[ nc ];
            for ( int ic = 0; ic < nc; ic++ ) {
                Object child = mModel.getChild( root, ic );
                newExpanded[ ic ] =
                    new TreePath( new Object[] { root, child } );
            }
        }
        else if ( dataChanged || ( oldCount < ne && newCount > ne ) ) {
            newExpanded = new TreePath[ 0 ];
        }
        else {
            newExpanded = oldExpanded;
        }
        for ( TreePath expTp : newExpanded ) {
            tTree_.expandPath( expTp );
        }

        /* Try to restore previous selections (only one, probably).
         * If the old selections are no longer in the tree, chuck them out. */
        if ( mask != null && selections != null ) {
            selections = sanitiseSelections( selections, mModel );
        }

        /* If for whatever reason we have no selections, select something.
         * This logic will probably pick the first leaf node (table metadata,
         * rather than schema metadata).  But if it can't find it where it
         * expects, it will just pick the very first node in the tree. */
        if ( selections == null || selections.length == 0 ) {
            TreePath tp0 = tTree_.getPathForRow( 0 );
            TreePath tp1 = tTree_.getPathForRow( 1 );
            TreePath tp = tp1 != null && tp0 != null
                       && mModel.isLeaf( tp1.getLastPathComponent() )
                       && ! mModel.isLeaf( tp0.getLastPathComponent() )
                     ? tp1
                     : tp0;
            selections = tp != null ? new TreePath[] { tp }
                                    : new TreePath[ 0 ];
        }
        tTree_.setSelectionPaths( selections );
    }

    /**
     * Returns the first column in a given list with a given name,
     * or null if there is no such entry.
     *
     * @param  list  column list
     * @param  name  required name
     * @return   list entry with given name, or null
     */
    private static ColumnMeta getNamedEntry( Collection<ColumnMeta> list,
                                             String name ) {
        if ( name != null ) {
            for ( ColumnMeta c : list ) {
                if ( name.equals( c.getName() ) ) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Get a list of tree paths based on a given list, but making sure
     * they exist in a given tree model.
     *
     * @param   selections  initial selection paths
     * @param   model   tree model within which result selections must exist
     * @return  list of selections that are in the model (may be empty)
     */
    private static TreePath[] sanitiseSelections( TreePath[] selections,
                                                  TreeModel model ) {
        List<TreePath> okPaths = new ArrayList<TreePath>();
        Object root = model.getRoot();

        /* Tackle each input path one at a time. */
        for ( TreePath path : selections ) {
            if ( path.getPathComponent( 0 ) != root ) {
                assert false;
            }
            else {

                /* Try to add each element of the input path to the result path.
                 * If any of the nodes in the chain is missing, just stop
                 * there, leaving an ancestor of the input path. */
                int nel = path.getPathCount();
                List<Object> els = new ArrayList<Object>( nel );
                els.add( root );
                for ( int i = 1; i < nel; i++ ) {
                    Object pathEl = path.getPathComponent( i );
                    if ( model.getIndexOfChild( els.get( i - 1 ), pathEl )
                         >= 0 ) {
                        els.add( pathEl );
                    }
                    else {
                        break;
                    }
                }

                /* Add it to the result, but only if it's not just the
                 * root node (and if we don't already have it). */
                TreePath okPath =
                    new TreePath( els.toArray( new Object[ 0 ] ) );
                if ( okPath.getPathCount() > 1 &&
                     ! okPaths.contains( okPath ) ) {
                    okPaths.add( okPath );
                }
            }
        }
        return okPaths.toArray( new TreePath[ 0 ] );
    }

    /**
     * Returns a small icon indicating whether a given tab is currently
     * active or not.
     *
     * @param   isActive  true iff tab has content
     * @return  little icon
     */
    private Icon activeIcon( boolean isActive ) {
        return HasContentIcon.getIcon( isActive );
    }

    /**
     * Utility method to return a string representing the length of an array.
     *
     * @param  array  array object, or null
     * @return  string giving length of array, or null for null input
     */
    private static String arrayLength( Object[] array ) {
        return array == null ? null : Integer.toString( array.length );
    }

    /**
     * Constructs an array of columns which define the table model
     * to use for displaying the column metadata.
     *
     * @return   column descriptions
     */
    private static List<ColMetaColumn<?>> createColumnMetaColumns() {
        List<ColMetaColumn<?>> list = new ArrayList<ColMetaColumn<?>>();
        list.add( new ColMetaColumn<String>( "Name", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getName();
            }
        } );
        list.add( new ColMetaColumn<String>( "Type", String.class ) {
            public String getValue( ColumnMeta col ) {
                String datatype = col.getDataType();
                String arraysize = col.getArraysize();
                StringBuffer sbuf = new StringBuffer();
                if ( datatype != null ) {
                    sbuf.append( datatype );
                }
                if ( arraysize != null ) {
                    String asize = arraysize.trim();
                    if ( asize.length() > 0 && ! asize.equals( "1" ) ) {
                        sbuf.append( '(' )
                            .append( asize )
                            .append( ')' );
                     }
                }
                return sbuf.toString();
            }
        } );
        list.add( new ColMetaColumn<String>( "Unit", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getUnit();
            }
        } );
        list.add( new ColMetaColumn<Boolean>( "Indexed", Boolean.class ) {
            public Boolean getValue( ColumnMeta col ) {
                return Boolean.valueOf( Arrays.asList( col.getFlags() )
                                              .indexOf( "indexed" ) >= 0 );
            }
        } );
        list.add( new ColMetaColumn<String>( "Description", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getDescription();
            }
        } );
        list.add( new ColMetaColumn<String>( "UCD", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getUcd();
            }
        } );
        list.add( new ColMetaColumn<String>( "Utype", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getUtype();
            }
        } );
        list.add( new ColMetaColumn<String>( "Xtype", String.class ) {
            public String getValue( ColumnMeta col ) {
                return col.getXtype();
            }
        } );
        list.add( new ColMetaColumn<String>( "Flags", String.class ) {
            public String getValue( ColumnMeta col ) {
                String[] flags = col.getFlags();
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
        } );
        return list;
    }

    /**
     * Constructs an array of columns which define the table model
     * to use for displaying foreign key information.
     *
     * @return  column descriptions
     */
    private static List<ForeignMetaColumn> createForeignMetaColumns() {
        List<ForeignMetaColumn> list = new ArrayList<ForeignMetaColumn>();
        list.add( new ForeignMetaColumn( "Target Table" ) {
            public String getValue( ForeignMeta fm ) {
                return fm.getTargetTable();
            }
        } );
        list.add( new ForeignMetaColumn( "Links" ) {
            public String getValue( ForeignMeta fm ) {
                ForeignMeta.Link[] links = fm.getLinks();
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
        } );
        list.add( new ForeignMetaColumn( "Description" ) {
            public String getValue( ForeignMeta fm ) {
                return fm.getDescription();
            }
        } );
        list.add( new ForeignMetaColumn( "Utype" ) {
            public String getValue( ForeignMeta fm ) {
                return fm.getUtype();
            }
        } );
        return list;
    }

    /**
     * Wraps a MetaPanel in a suitable JScrollPane.
     *
     * @param  panel  panel to wrap
     * @return   wrapped panel
     */
    private static JScrollPane metaScroller( JComponent panel ) {
        return new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
    }

    /**
     * Returns a component with a given preferred width, and zero preferred
     * height.
     *
     * @param  width  preferred width
     * @return  new component
     */
    private static JComponent createHorizontalStrut( int width ) {
        JComponent c = new JPanel();
        c.setPreferredSize( new Dimension( width, 0 ) );
        return c;
    }

    /**
     * TreeCellRenderer that appends a count of table children to the
     * text label for each schema entry in the tree.
     */
    private static class CountTableTreeCellRenderer
            extends DefaultTreeCellRenderer {
        String rootName_;
        @Override
        public Component getTreeCellRendererComponent( JTree tree, Object value,
                                                       boolean isSelected,
                                                       boolean isExpanded,
                                                       boolean isLeaf, int irow,
                                                       boolean hasFocus ) {
            Component comp =
                super.getTreeCellRendererComponent( tree, value, isSelected,
                                                    isExpanded, isLeaf, irow,
                                                    hasFocus );
            if ( value instanceof SchemaMeta[] ) {
                SchemaMeta[] schemas = (SchemaMeta[]) value;
                setIcon( ResourceIcon.NODE_SERVICE );
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( rootName_ == null ? "TAP Service" : rootName_ );
                int ntTotal = 0;
                for ( SchemaMeta schema : schemas ) {
                    TableMeta[] tables = schema.getTables();
                    if ( tables != null ) {
                        ntTotal += tables.length;
                    }
                }
                sbuf.append( " (" );
                TreeModel model = tree.getModel();
                boolean hasMask = model instanceof MaskTreeModel
                               && ((MaskTreeModel) model).getMask() != null;
                if ( hasMask ) {
                    int ntPresent = 0;
                    for ( SchemaMeta schema : schemas ) {
                        ntPresent += model.getChildCount( schema );
                    }
                    sbuf.append( ntPresent )
                        .append( "/" );
                }
                sbuf.append( ntTotal )
                    .append( ")" );
                setText( sbuf.toString() );
            }
            if ( value instanceof SchemaMeta ) {
                TableMeta[] tables = ((SchemaMeta) value).getTables();
                if ( tables != null ) {
                    int ntTotal = tables.length;
                    TreeModel model = tree.getModel();
                    int ntPresent =
                        model.isLeaf( value ) ? -1
                                              : model.getChildCount( value );
                    boolean hasMask = model instanceof MaskTreeModel
                                   && ((MaskTreeModel) model).getMask() != null;
                    StringBuffer sbuf = new StringBuffer();
                    sbuf.append( getText() )
                        .append( " (" );
                    if ( hasMask ) {
                        sbuf.append( ntPresent )
                            .append( "/" );
                    }
                    sbuf.append( ntTotal )
                        .append( ")" );
                    setText( sbuf.toString() );
                }
            }
            else if ( value instanceof TableMeta ) {
                setIcon( ResourceIcon.NODE_TABLE );
            }
            return comp;
        }
    }

    /**
     * Extracts text elements from tree nodes for comparison with search terms.
     */
    private static abstract class NodeStringer {
        private final boolean useName_;
        private final boolean useDescription_;

        /**
         * Constructor.
         *
         * @param  useName  true to use the node name as one of the strings
         * @param  useDescription  true to use the node description as one
         *                         of the strings
         */
        private NodeStringer( boolean useName, boolean useDescription ) {
            useName_ = useName;
            useDescription_ = useDescription;
        }

        /**
         * Supplies a list of strings that characterise a given tree node.
         *
         * @param  node  tree node
         * @return   list of strings associated with the node
         */
        public abstract List<String> getStrings( Object node );

        @Override
        public int hashCode() {
            int code = 5523;
            code = 23 * code + ( useName_ ? 11 : 13 );
            code = 23 * code + ( useDescription_ ? 23 : 29 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof NodeStringer ) {
                NodeStringer other = (NodeStringer) o;
                return this.useName_ == other.useName_
                    && this.useDescription_ == other.useDescription_;
            }
            else {
                return false;
            }
        }

        /**
         * Constructs an instance of this class.
         *
         * @param  useName  true to use the node name as one of the strings
         * @param  useDescrip  true to use the node description as
         *                     one of the strings
         */
        public static NodeStringer createInstance( final boolean useName,
                                                   final boolean useDescrip ) {

            /* Treat name only as a special case for efficiency. */
            if ( useName && ! useDescrip ) {
                final List<String> emptyList = Arrays.asList( new String[ 0 ] );
                return new NodeStringer( useName, useDescrip ) {
                    public List<String> getStrings( Object node ) {
                        return node == null
                             ? emptyList
                             : Collections.singletonList( node.toString() );
                    }
                };
            }

            /* Otherwise treat it more generally. */
            else {
                return new NodeStringer( useName, useDescrip ) {
                    public List<String> getStrings( Object node ) {
                        List<String> list = new ArrayList<String>();
                        if ( node != null ) {
                            if ( useName ) {
                                list.add( node.toString() );
                            }
                            if ( useDescrip ) {
                                String descrip = null;
                                if ( node instanceof TableMeta ) {
                                    descrip =
                                        ((TableMeta) node).getDescription();
                                }
                                else if ( node instanceof SchemaMeta ) {
                                    descrip =
                                        ((SchemaMeta) node).getDescription();
                                }
                                if ( descrip != null ) {
                                    list.add( descrip );
                                }
                            }
                        }
                        return list;
                    }
                };
            }
        }
    }

    /**
     * Tree node mask that selects on simple matches of node name strings
     * to one or more space-separated words entered in the search field.
     *
     * <p>Implements equals/hashCode for equality, which isn't essential,
     * but somewhat beneficial for efficiency.
     */
    private static class WordMatchMask implements MaskTreeModel.Mask {
        private final Set<String> lwords_;
        private final NodeStringer stringer_;
        private final boolean isAnd_;

        /**
         * Constructor.
         *
         * @param  words   search terms
         * @param  stringer  converts node to text strings for matching
         * @param  isAnd  true to require matching of a node string against
         *                all search terms, false to match against any
         */
        WordMatchMask( String[] words, NodeStringer stringer, boolean isAnd ) {
            lwords_ = new HashSet<String>( words.length );
            for ( String word : words ) {
                lwords_.add( word.toLowerCase() );
            }
            stringer_ = stringer;
            isAnd_ = isAnd;
        }

        public boolean isIncluded( Object node ) {
            for ( String nodeTxt : stringer_.getStrings( node ) ) {
                if ( nodeTxt != null ) {
                    String nodetxt = nodeTxt.toLowerCase();
                    if ( isAnd_ ? matchesAllWords( nodetxt )
                                : matchesAnyWord( nodetxt ) ) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Tests whether a given string matches any of this mask's search terms.
         *
         * @param  txt  test string
         * @return  true iff txt matches any search term
         */
        private boolean matchesAnyWord( String txt ) {
            for ( String lword : lwords_ ) {
                if ( txt.indexOf( lword ) >= 0 ) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Tests whether a given string matches all of this mask's search terms.
         *
         * @param  txt  test string
         * @return   true iff txt matches all search terms
         */
        private boolean matchesAllWords( String txt ) {
            for ( String lword : lwords_ ) {
                if ( txt.indexOf( lword ) < 0 ) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int code = 324223;
            code = 23 * code + lwords_.hashCode();
            code = 23 * code + stringer_.hashCode();
            code = 23 * code + ( isAnd_ ? 11 : 17 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof WordMatchMask ) {
                WordMatchMask other = (WordMatchMask) o;
                return this.lwords_.equals( other.lwords_ )
                    && this.stringer_.equals( other.stringer_ )
                    && this.isAnd_ == other.isAnd_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Convenience ArrayTableColumn subclass for use with ColumnMeta objects.
     */
    private static abstract class ColMetaColumn<C>
            extends ArrayTableColumn<ColumnMeta,C> {

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  clazz  column content class
         */
        ColMetaColumn( String name, Class<C> clazz ) {
            super( name, clazz );
        }
    }

    /**
     * Convenience ArrayTableColumn subclass for ForeignMeta objects.
     */
    private static abstract class ForeignMetaColumn
            extends ArrayTableColumn<ForeignMeta,String> {

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  clazz  column content class
         */
        ForeignMetaColumn( String name ) {
            super( name, String.class );
        }
    }

    /**
     * ArrayTableColumn for extracting "extra" non-standard column metadata.
     * Implements equals/hashCode.
     */
    private static class ExtraColumn<C> extends ColMetaColumn<C> {
        private final String key_;
        private final Class<C> clazz_;

        /**
         * Constructor.
         *
         * @param  key   name of metadata key in ColumnMeta extras map
         * @param  clazz   class of object returned by getValue method
         */
        ExtraColumn( String key, Class<C> clazz ) {
            super( key, clazz );
            key_ = key;
            clazz_ = clazz;
        }

        public C getValue( ColumnMeta col ) {
            Object value = col.getExtras().get( key_ );
            return clazz_.isInstance( value ) ? clazz_.cast( value ) : null;
        }

        @Override
        public int hashCode() {
            int code = 772261;
            code = 23 * code + key_.hashCode();
            code = 23 * code + clazz_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ExtraColumn ) {
                ExtraColumn<?> other = (ExtraColumn<?>) o;
                return this.key_.equals( other.key_ )
                    && this.clazz_.equals( other.clazz_ );
            }
            else {
                return false;
            }
        }

        /**
         * Creates a type-specific instance of this class.
         * This is a simple wrapper around the constructor require for
         * wrangling generics.
         *
         * @param  key   name of metadata key in ColumnMeta extras map
         * @param  clazz   class of object returned by getValue method
         */
        static <C> ExtraColumn<C> createInstance( String key, Class<C> clazz ) {
            return new ExtraColumn<C>( key, clazz );
        }

        /**
         * Creates an instance of this class that can display a given
         * list of objects.  The content type is determined by the
         * actual data types of the known values.  Don't try too hard;
         * if they don't all have identical classes it just gets String
         * values for them all.  But this is good enough for most cases,
         * and getting the type right is beneficial because it allows
         * sorting by value to work properly.
         *
         * @param    name   column name
         * @param    values  values that will be used
         */
        static ExtraColumn<?> createInstance( String name,
                                              List<Object> values ) {
            Class<?> clazz = null;
            for ( Object obj : values ) {
                if ( obj != null ) {
                    Class<?> c = obj.getClass();
                    if ( clazz == null ) {
                        clazz = c;
                    }
                    else if ( ! clazz.equals( c ) ) {
                        return new ExtraColumn<String>( name, String.class );
                    }
                }
            }
            if ( clazz == null ) {
                clazz = Object.class;
            }
            return createInstance( name, clazz );
        }
    }

    /**
     * MetaPanel subclass for displaying table metadata.
     */
    private static class TableMetaPanel extends MetaPanel {
        private final JTextComponent nameField_;
        private final JTextComponent ncolField_;
        private final JTextComponent nrowField_;
        private final JTextComponent nfkField_;
        private final JTextComponent descripField_;
        private final JTextComponent tableExtrasField_;
        private final JTextComponent colExtrasField_;
        private TableMeta table_;

        /**
         * Constructor.
         */
        TableMetaPanel() {
            nameField_ = addLineField( "Name" );
            ncolField_ = addLineField( "Columns" );
            nrowField_ = addLineField( "Rows (approx)" );
            nfkField_ = addLineField( "Foreign Keys" );
            descripField_ = addMultiLineField( "Description" );
            tableExtrasField_ = addHtmlField( "Non-Standard Table Metadata" );
            colExtrasField_ = addHtmlField( "Non-Standard Column Metadata" );
        }

        /**
         * Configures this component to display metadata for a given table.
         *
         * @param  table  table metadata to display
         */
        public void setTable( TableMeta table ) {
            if ( table != table_ ) {
                table_ = table;
                setFieldText( nameField_,
                              table == null ? null : table.getName() );
                setFieldText( descripField_,
                              table == null ? null : table.getDescription() );
                setFieldText( nrowField_,
                              table == null ? null : table.getNrows() );
                setFieldText( tableExtrasField_,
                              table == null ? null
                                            : mapToHtml( table.getExtras() ) );
                setColumns( table == null ? null : table.getColumns() );
                setForeignKeys( table == null ? null : table.getForeignKeys() );
            }
        }

        /**
         * Informs this panel of the column list for the currently displayed
         * table.  Only the array size is used.
         *
         * @param  cols  column array, or null
         */
        public void setColumns( ColumnMeta[] cols ) {
            setFieldText( ncolField_, arrayLength( cols ) );
        }

        /**
         * Informs this panel of non-standard items of column metadata
         * available for the currently displayed table.
         *
         * @param  names of non-standard per-column metadata items
         */
        public void setColumnExtras( String[] extras ) {
            StringBuffer sbuf = new StringBuffer();
            if ( extras != null ) {
                for ( String extra : extras ) {
                    sbuf.append( extra )
                        .append( "<br />\n" );
                }
            }
            setFieldText( colExtrasField_, sbuf.toString() );
        }

        /**
         * Informs this panel of the foreign key list for the currently
         * displayed table.  Only the array size is used.
         *
         * @param   fkeys  foreign key array, or null
         */
        public void setForeignKeys( ForeignMeta[] fkeys ) {
            setFieldText( nfkField_, arrayLength( fkeys ) );
        }
    }

    /**
     * MetaPanel subclass for displaying schema metadata.
     */
    private static class SchemaMetaPanel extends MetaPanel {
        private final JTextComponent nameField_;
        private final JTextComponent ntableField_;
        private final JTextComponent descripField_;
        private SchemaMeta schema_;

        /**
         * Constructor.
         */
        SchemaMetaPanel() {
            nameField_ = addLineField( "Name" );
            ntableField_ = addLineField( "Tables" );
            descripField_ = addMultiLineField( "Description" );
        }

        /**
         * Configures this component to display metadata for a given schema.
         *
         * @param  schema  schema metadata to display
         */
        public void setSchema( SchemaMeta schema ) {
            if ( schema != schema_ ) {
                schema_ = schema;
                setFieldText( nameField_,
                              schema == null ? null : schema.getName() );
                setFieldText( descripField_,
                              schema == null ? null : schema.getDescription() );
                TableMeta[] tables = schema == null ? null : schema.getTables();
                setFieldText( ntableField_, arrayLength( tables ) );
            }
        }
    }

    /**
     * MetaPanel subclass for displaying service metadata.
     */
    private static class ResourceMetaPanel extends MetaPanel {

        private final JTextComponent ivoidField_;
        private final JTextComponent servurlField_;
        private final JTextComponent authField_;
        private final JTextComponent nameField_;
        private final JTextComponent titleField_;
        private final JTextComponent refurlField_;
        private final JTextComponent examplesurlField_;
        private final JTextComponent sizeField_;
        private final JTextComponent publisherField_;
        private final JTextComponent creatorField_;
        private final JTextComponent contactField_;
        private final JTextComponent descripField_;
        private final JTextComponent dmField_;
        private final JTextComponent geoField_;
        private final JTextComponent adql21Field_;
        private final JTextComponent nonstdField_;

        /**
         * Constructor.
         *
         * @param  urlHandler  handles URLs that the user clicks on; may be null
         */
        ResourceMetaPanel( Consumer<URL> urlHandler ) {
            nameField_ = addLineField( "Short Name" );
            titleField_ = addLineField( "Title" );
            ivoidField_ = addLineField( "IVO ID" );
            servurlField_ = addLineField( "Service URL" );
            authField_ = addLineField( "Authentication" );
            refurlField_ = addUrlField( "Reference URL", urlHandler );
            examplesurlField_ = addUrlField( "Examples URL", urlHandler );
            sizeField_ = addLineField( "Size" );
            publisherField_ = addMultiLineField( "Publisher" );
            creatorField_ = addMultiLineField( "Creator" );
            contactField_ = addMultiLineField( "Contact" );
            descripField_ = addMultiLineField( "Description" );
            dmField_ = addMultiLineField( "Data Models" );
            String star = " [*]";
            geoField_ =
                addMultiLineField( "Geometry Functions" + star );
            adql21Field_ =
                addMultiLineField( "ADQL 2.1 Optional Features" + star );
            nonstdField_ =
                addMultiLineField( "Non-Standard Language Features" + star );
            addTextLine( "<html></html>" );
            addTextLine( "<html>" + star
                       + " <em>see " + featureTitle_ + " tab for details</em>"
                       + "</html>" );
        }

        /**
         * Sets basic identity information for this service.
         *
         * @param  serviceLabel   TAP service label, may be null
         * @param  ivoid  ivorn for TAP service registry resource, may be null
         */
        public void setId( String serviceLabel, String ivoid ) {
            setFieldText( servurlField_, serviceLabel );
            setFieldText( ivoidField_, ivoid );
        }

        /**
         * Supplies a string indicating the size of the service.
         *
         * @param   sizeTxt  text, for instance count of schemas and tables
         */
        public void setSize( String sizeTxt ) {
            setFieldText( sizeField_, sizeTxt );
        }

        /**
         * Sets resource information.
         * The argument is a map of standard RegTAP resource column names
         * to their values.
         *
         * @param  map  map of service resource metadata items,
         *              may be empty but not null
         */
        public void setResourceInfo( Map<String,String> info ) {
            setFieldText( nameField_, info.remove( "short_name" ) );
            setFieldText( titleField_, info.remove( "res_title" ) );
            setFieldText( refurlField_, info.remove( "reference_url" ) );
            setFieldText( descripField_, info.remove( "res_description" ) );
        }

        /**
         * Sets role information.
         *
         * @param  roles  list of known roles, may be empty but not null
         */
        public void setResourceRoles( RegRole[] roles ) {
            setFieldText( publisherField_, getRoleText( roles, "publisher" ) );
            setFieldText( creatorField_, getRoleText( roles, "creator" ) );
            setFieldText( contactField_, getRoleText( roles, "contact" ) );
            setLogoUrl( getLogoUrl( roles ) );
        }

        /**
         * Sets the examples URL to display.
         *
         * @param  examples URL, or null
         */
        public void setExamplesUrl( String examplesUrl ) {
            setFieldText( examplesurlField_, examplesUrl );
        }

        /**
         * Sets capability information to display.
         *
         * @param   tcap  capability object, may be null
         */
        public void setCapability( TapCapability tcap ) {
            setFieldText( dmField_, getDataModelText( tcap ) );
            setFieldText( geoField_,
                          getFeatureFormsText( tcap,
                                               AdqlFeature.ADQLGEO_FILTER ) );
            setFieldText( adql21Field_,
                          getFeatureFormsText( tcap,
                                               AdqlFeature.ADQL21MISC_FILTER ));
            setFieldText( nonstdField_,
                          getFeatureFormsText( tcap,
                                               AdqlFeature.NONSTD_FILTER ) );
        }

        /**
         * Displays authenticated user identifer in this panel.
         *
         * @param  authId  string representing authentication status
         */
        public void setAuthId( String authId ) {
            setFieldText( authField_, authId );
        }

        /**
         * Returns a text string displaying data model information for
         * the given capability.
         *
         * @param   tcap  capability object, may be null
         * @return   text summarising data model information, or null
         */
        private static String getDataModelText( TapCapability tcap ) {
            if ( tcap == null ) {
                return null;
            }
            Ivoid[] dms = tcap.getDataModels();
            if ( dms == null || dms.length == 0 ) {
                return null;
            }
            StringBuffer sbuf = new StringBuffer();
            if ( dms != null ) {
                for ( Ivoid dm : dms ) {
                    if ( sbuf.length() != 0 ) {
                        sbuf.append( '\n' );
                    }
                    sbuf.append( dm );
                }
            }
            return sbuf.toString();
        }

        /**
         * Returns a comma-separated list giving the form values for
         * language features in the given capability that fall within
         * a given list of feature types.
         *
         * @param  tcap   capability object, may be null
         * @param  featTypeFilter   predicate indicating which feature type
         *                          identifier strings should be included
         * @return  forms list string
         */
        private static String
                getFeatureFormsText( TapCapability tcap,
                                     Predicate<Ivoid> featTypeFilter ) {
            return Arrays.stream( tcap == null ? new TapLanguage[ 0 ]
                                               : tcap.getLanguages() )
                  .flatMap( lang -> lang.getFeaturesMap().entrySet().stream() )
                  .filter( entry -> featTypeFilter.test( entry.getKey() ) )
                  .flatMap( entry -> Arrays.stream( entry.getValue() ) )
                  .map( feature -> feature.getForm() )
                  .collect( Collectors.joining( ", " ) );
        }

        /**
         * Returns a text string displaying information about a RegRole
         * category.
         *
         * @param  roles  list of all known role entities
         * @param  baseRole   role category
         * @return  text, may be multi-line
         */
        private static String getRoleText( RegRole[] roles, String baseRole ) {
            StringBuffer sbuf = new StringBuffer();
            for ( RegRole role : roles ) {
                String name = role.getName();
                String email = role.getEmail();
                boolean hasName = name != null && name.trim().length() > 0;
                boolean hasEmail = email != null && email.trim().length() > 0;
                if ( baseRole.equalsIgnoreCase( role.getBaseRole() )
                     && ( hasName || hasEmail ) ) {
                    if ( sbuf.length() > 0 ) {
                        sbuf.append( '\n' );
                    }
                    if ( hasName ) {
                        sbuf.append( name.trim() );
                    }
                    if ( hasName && hasEmail ) {
                        sbuf.append( ' ' );
                    }
                    if ( hasEmail ) {
                        sbuf.append( '<' )
                            .append( email.trim() )
                            .append( '>' );
                    }
                }
            }
            return sbuf.toString();
        }

        /**
         * Returns the URL of a logo icon associated with a set of roles.
         *
         * @param  roles  registry roles
         * @return   logo image URL, or null
         */
        private static URL getLogoUrl( RegRole[] roles ) {
            for ( RegRole role : roles ) {
                String logo = role.getLogo();
                if ( logo != null && logo.trim().length() > 0 ) {
                    try {
                        return new URL( logo );
                    }
                    catch ( MalformedURLException e ) {
                    }
                }
            }
            return null;
        }
    }
}
