package uk.ac.starlink.vo;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import uk.ac.starlink.vo.TapServiceFinder.Service;
import uk.ac.starlink.vo.TapServiceFinder.Table;

/**
 * TreeModel implementation representing a particular set of tables
 * contained in a list of known TAP services.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public class TapServiceTreeModel implements TreeModel {

    private final String rootLabel_;
    private final Service[] services_;
    private final Map<Service,Table[]> tableMap_;
    private final List<TreeModelListener> listeners_;
    private static final Comparator<Table> BY_TABLE_NAME = byTableName();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  rootLabel  text label for root element (may be null)
     * @param  services  list of services nodes in tree
     * @param  tableMap  array of tables providing child nodes for each service;
     *                   may be null if no table children are required,
     *                   but if not null must contain an entry for each service
     */
    protected TapServiceTreeModel( String rootLabel, Service[] services,
                                   Map<Service,Table[]> tableMap ) {
        rootLabel_ = rootLabel;
        services_ = services;
        tableMap_ = tableMap;
        listeners_ = new ArrayList<TreeModelListener>();
        if ( tableMap != null ) {
            for ( Service service : services ) {
                if ( tableMap.get( service ) == null ) {
                    throw new IllegalArgumentException( "No table list for "
                                                      + service );
                }
            }
        }
    }

    /**
     * Constructs an instance with no entries.
     *
     * @param  rootLabel  text label for root element (may be null)
     */
    public TapServiceTreeModel( String rootLabel ) {
        this( rootLabel, new Service[ 0 ], null );
    }

    public Object getRoot() {
        return services_;
    }

    public boolean isLeaf( Object node ) {
        return asNode( node ).isLeaf();
    }

    public int getChildCount( Object parent ) {
        Object[] children = asNode( parent ).children_;
        return children == null ? 0 : children.length;
    }

    public Object getChild( Object parent, int index ) {
        return asNode( parent ).children_[ index ];
    }

    public int getIndexOfChild( Object parent, Object child ) {
        if ( parent != null && child != null ) {
            Object[] children = asNode( parent ).children_;
            if ( children != null ) {

                /* Obviously, not very efficient.  I'm not sure when this
                 * method is called, but not I think during normal tree
                 * navigation.  So assume it doesn't matter unless it is
                 * demonstrated otherwise. */
                int nc = children.length;
                for ( int ic = 0; ic < nc; ic++ ) {
                    if ( children[ ic ] == child ) {
                        return ic;
                    }
                }
            }
        }
        return -1;
    }

    public void valueForPathChanged( TreePath path, Object newValue ) {
        assert false : "Tree is not editable from GUI";
    }

    public void addTreeModelListener( TreeModelListener lnr ) {
        listeners_.add( lnr );
    }

    public void removeTreeModelListener( TreeModelListener lnr ) {
        listeners_.remove( lnr );
    }

    /**
     * Returns a tree path which correponds to a TAP service, and which
     * is an ancestor of the supplied path.  The supplied path counts
     * as its own ancestor for these purposes.
     *
     * @param  path   path to examine
     * @return   path corresponding to a sub-path of the supplied one,
     *           for which the terminal element is a TapServiceFinder.Service,
     *           or null if no service appears in the ancestry
     */
    public static TreePath getServicePath( TreePath path ) {
        for ( int i = 0; i < path.getPathCount(); i++ ) {
            if ( path.getPathComponent( i )
                 instanceof TapServiceFinder.Service ) {
                Object[] spath = new Object[ i + 1 ];
                System.arraycopy( path.getPath(), 0, spath, 0, i + 1 );
                return new TreePath( spath );
            }
        }
        return null;
    }

    /**
     * Returns a service in the ancestry of a supplied path.
     * The supplied path counts as its own ancestor for these purposes.
     *
     * @param  path   path to examine
     * @return   service owning the path,
     *           or null if no service appears in the ancestry
     */
    public static TapServiceFinder.Service getService( TreePath path ) {
        if ( path != null ) {
            TreePath servicePath = getServicePath( path );
            if ( servicePath != null ) {
                return (TapServiceFinder.Service)
                       servicePath.getLastPathComponent();
            }
        }
        return null;
    }

    /**
     * Returns a table that forms part of a supplied path.
     *
     * @param  path  path to examine
     * @return  table at the end of the path, or null if there isn't one
     */
    public static TapServiceFinder.Table getTable( TreePath path ) {
        if ( path != null ) {
            Object node = path.getLastPathComponent();
            if ( node instanceof TapServiceFinder.Table ) {
                return (TapServiceFinder.Table) node;
            }
        }
        return null;
    }

    /**
     * Constructs a tree model based on some given constraints.
     * May require a read of service data, hence should not be executed
     * on the Event Dispatch Thread.
     *
     * @param   allServices   list of all services that may be relevant
     * @param   finder    object that can search for TAP services
     * @param   constraint  defines the services of interest;
     *                      if null, all are used
     * @return   tree model
     */
    public static TapServiceTreeModel 
            readTreeModel( Service[] allServices, TapServiceFinder finder, 
                           TapServiceFinder.Constraint constraint ) 
            throws IOException {
        if ( constraint == null ) {
            return createModel( allServices );
        }               
        else {          
            TapServiceFinder.Table[] tables =
                finder.readSelectedTables( constraint );
            Service[] extraServices = null;
            for ( TapServiceFinder.Target target : constraint.getTargets() ) {
                if ( target.isServiceMeta() ) {
                    String[] keywords = constraint.getKeywords();
                    boolean isAnd = constraint.isAndKeywords();
                    List<Service> extras = new ArrayList<Service>();
                    for ( Service serv : allServices ) {
                        if ( target.matchesService( serv, keywords, isAnd ) ) {
                            extras.add( serv );
                        }
                    }
                    extraServices = extras.toArray( new Service[ 0 ] );
                }
            }
            return createModel( allServices, tables, extraServices );
        }
    }

    /**
     * Creates a tree model from a list of tables to be displayed,
     * along with a list of services containing at least the owners
     * of the supplied tables.
     *
     * <p>Note that only those services containing at least one of the
     * tables in the supplied list will be displayed.
     * Services without associated tables and tables without associated
     * services will be ignored for the purposes of this model.
     * Tables know which service they belong to.
     *
     * @param   allServices  open-ended list TAP services
     *                       (should contain all parents of given tables,
     *                       but may contain others as well)
     * @param   tables    list of TAP tables for display by this model
     * @return  new tree model
     */
    private static TapServiceTreeModel createModel( Service[] allServices,
                                                    Table[] tables,
                                                    Service[] extraServices ) {
        Map<String,Service> serviceMap = new LinkedHashMap<String,Service>();
        for ( Service serv : allServices ) {
            serviceMap.put( serv.getId(), serv );
        }
        Map<String,List<Table>> tMap = new LinkedHashMap<String,List<Table>>();
        if ( extraServices != null ) {
            for ( Service service : extraServices ) {
                tMap.put( service.getId(), new ArrayList<Table>() );
            }
        }
        for ( Table table : tables ) {
            String ivoid = table.getServiceId();
            if ( ! tMap.containsKey( ivoid ) ) {
                tMap.put( ivoid, new ArrayList<Table>() );
            }
            tMap.get( ivoid ).add( table );
        }
        Map<Service,Table[]> tableMap = new LinkedHashMap<Service,Table[]>();
        for ( Map.Entry<String,List<Table>> entry : tMap.entrySet() ) {
            Service service = serviceMap.get( entry.getKey() );
            if ( service != null ) {
                Table[] ts = entry.getValue().toArray( new Table[ 0 ] );
                Arrays.sort( ts, BY_TABLE_NAME );
                tableMap.put( service, ts );
            }
        }
        Service[] displayServices =
            tableMap.keySet().toArray( new Service[ 0 ] );
        Arrays.sort( displayServices, byTableCount( tableMap ) );
        String label = "Selected TAP services (" + displayServices.length
                     + "/" + allServices.length + ")";
        return new TapServiceTreeModel( label, displayServices, tableMap );
    }

    /**
     * Creates a tree model from a list of services to be displayed.
     * The services are sorted before presentation.
     *
     * @param  services to form nodes of tree
     * @return  new tree model
     */
    private static TapServiceTreeModel createModel( Service[] services ) {
        Service[] displayServices = services.clone();
        Arrays.sort( displayServices, byTableCount( null ) );
        String label = "All TAP services (" + services.length + ")";
        return new TapServiceTreeModel( label, displayServices, null );
    }

    /**
     * Returns a lightweight facade for an object representing a node
     * in this tree.  The result is an adapter supplying basic tree-like
     * behaviour.  The expectation is that this method will be called
     * frequently as required (perhaps many times for the same object)
     * rather than cached, so this method should be cheap.
     *
     * @param   node  raw data object
     * @return  adapter object representing node data
     */
    private Node asNode( final Object item ) {
        if ( item instanceof Service[] ) {
            return new Node( (Service[]) item, null ) {
                public String toString() {
                    return rootLabel_;
                }
            };
        }
        else if ( item instanceof Service ) {
            final Service service = (Service) item;
            return new Node( tableMap_ == null ? null
                                               : tableMap_.get( service ),
                             ResourceIcon.NODE_SERVICE ) {
                public String toString() {
                    int ntTotal = service.getTableCount();
                    int ntPresent = children_ == null ? -1 : children_.length;
                    StringBuffer cbuf = new StringBuffer()
                        .append( " (" );
                    if ( ntPresent >= 0 ) {
                         cbuf.append( ntPresent )
                             .append( "/" );
                    }
                    cbuf.append( ntTotal <= 0 ? "?"
                                              : Integer.toString( ntTotal ) )
                        .append( ")" );
                    return getServiceLabel( service ) + cbuf.toString();
                }
            };
        }
        else if ( item instanceof Table ) {
            final Table table = (Table) item;
            return new Node( null, ResourceIcon.NODE_TABLE ) {
                public String toString() {
                    String descrip = table.getDescription();
                    String txt = table.getName();
                    if ( descrip != null ) {
                        txt += " - " + descrip.replaceAll( "\\s+", " " );
                    }
                    return txt;
                }
            };
        }
        else {
            assert false;
            return new Node( null, null ) {
                public String toString() {
                    return item.toString();
                }
            };
        }
    }

    /**
     * Returns a short text label by which a service will be identified
     * in the GUI.
     *
     * @param  service  service
     * @return   short human-readable text string, not null
     */
    private static String getServiceLabel( Service service ) {
        String name = service.getName();
        if ( name != null && name.trim().length() > 0 ) {
            return name.trim();
        }
        String title = service.getTitle();
        if ( title != null && title.trim().length() > 0 ) {
            return title.trim();
        }
        String ivoid = service.getId();
        if ( ivoid != null && ivoid.trim().length() > 0 ) {
            return ivoid.trim();
        }
        return "<nameless>";
    }

    /**
     * Returns a comparator to sort services by the number of tables.
     * It sorts first by the number of children it has in this tree,
     * and as a tie-breaker by the total number of tables in the service.
     *
     * @return  service comparator
     */
    private static Comparator<Service>
            byTableCount( final Map<Service,Table[]> tableMap ) {
        return new Comparator<Service>() {
            public int compare( Service s1, Service s2 ) {
                if ( tableMap != null ) {
                    int dc = tableMap.get( s2 ).length
                           - tableMap.get( s1 ).length;
                    if ( dc != 0 ) {
                        return dc;
                    }
                }
                int dt = s2.getTableCount() - s1.getTableCount();
                if ( dt != 0 ) {
                    return dt;
                }
                int dn = getServiceLabel( s1 )
                        .compareTo( getServiceLabel( s2 ) );
                if ( dn != 0 ) {
                    return dn;
                }
                int da = s2.hashCode() - s1.hashCode();
                return da;
            }
        };
    }

    /**
     * Returns a comparator that sorts tables alphabetically by name.
     *
     * @return  table comparator
     */
    private static Comparator<Table> byTableName() {
        return new Comparator<Table>() {
            public int compare( Table t1, Table t2 ) {
                String n1 = t1.getName();
                String n2 = t2.getName();
                if ( n1 != null && n2 != null ) {
                    int cmp = n1.compareTo( n2 );
                    if ( cmp != 0 ) {
                        return cmp;
                    }
                }
                return t1.hashCode() - t2.hashCode();
            }
        };
    }

    /**
     * Creates an icon from an image resource.
     *
     * @param  filename  filename in the directory of the current class
     * @return  icon, or null if there's a problem
     */
    private static Icon createIcon( String filename ) {
        try {
            return new ImageIcon( TapServiceTreeModel.class
                                 .getResource( filename ) );
        }
        catch ( Exception e ) {
            logger_.warning( "No icon " + filename );
            return null;
        }
    }

    /**
     * Returns a cell renderer suitable for rendering nodes of a JTree
     * using a model of this class.
     *
     * @return  tree cell renderer
     */
    public static TreeCellRenderer createCellRenderer() {
        final JComponent line = Box.createHorizontalBox();
        final DefaultTreeCellRenderer rend1 = new DefaultTreeCellRenderer();
        final DefaultTreeCellRenderer rend2 = new DefaultTreeCellRenderer();
        final Color fg2 = UIManager.getColor( "Label.disabledForeground" );
        line.add( rend1 );
        line.add( rend2 );
        return new TreeCellRenderer() {
            public Component getTreeCellRendererComponent( JTree tree,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean isExpanded,
                                                           boolean isLeaf,
                                                           int irow,
                                                           boolean hasFocus ) {
                TreeModel model = tree.getModel();

                /* Prepare text for labelling the node. */
                final String text;
                final String text2;
                final Icon icon;
                if ( model instanceof TapServiceTreeModel ) {
                    Node node = ((TapServiceTreeModel) model).asNode( value );
                    text = node.toString();
                    text2 = (value instanceof Service)
                          ? " - " + ((Service) value).getId()
                          : null;
                    icon = node.getIcon();
                }
                else {
                    text = value.toString();
                    text2 = null;
                    icon = null;
                }

                /* Adjust presentation for nodes that are present in the
                 * selection model, but which don't represent services
                 * (these are probably tables).
                 * They should look less prominent, since 'selecting' them
                 * doesn't have any effect on the rest of the GUI.
                 * Currently, make them look focussed but unselected - this
                 * is just a hack to give them some L&F-friendly appearance 
                 * that's (hopefully) visible but not the same as normal
                 * selection.  Could probably be done better, but hard to
                 * get something that's guaranteed to match the L&F. */
                if ( isSelected && ! ( value instanceof Service ) ) {
                    hasFocus = true;
                    isSelected = false;
                }

                /* Configure the renderer and return. */
                Component comp1 =
                    rend1.getTreeCellRendererComponent( tree, value, isSelected,
                                                        isExpanded, isLeaf,
                                                        irow, hasFocus );
                Component comp2 =
                    rend2.getTreeCellRendererComponent( tree, value, false,
                                                        isExpanded, isLeaf,
                                                        irow, false );
                assert comp1 == rend1;
                assert comp2 == rend2;
                rend1.setText( text );
                rend2.setText( text2 );
                rend2.setForeground( fg2 );
                rend2.setIcon( null );
                if ( icon != null ) {
                    rend1.setIcon( icon );
                }
                line.revalidate();
                return line;
            }
        };
    }

    /**
     * Defines the basic behaviour required from a node in this tree.
     */
    private static abstract class Node {
        final Object[] children_;
        final Icon icon_;

        /**
         * Constructor.
         *
         * @param  children   child nodes, null for tree leaves
         * @param  icon       custom tree node icon, or null
         */
        Node( Object[] children, Icon icon ) {
            children_ = children;
            icon_ = icon;
        }

        /**
         * Indicates whether this node is a leaf.
         *
         * @return  true for leaf, false for branch
         */
        boolean isLeaf() {
            return children_ == null || children_.length == 0;
        }

        /**
         * Returns a custom tree node icon.
         *
         * @return   icon, or null
         */
        Icon getIcon() {
            return icon_;
        }

        /**
         * This method is used to provide the text of the node as rendered
         * by this class's custom TreeCellRenderer.
         */
        @Override
        public abstract String toString();
    }
}
