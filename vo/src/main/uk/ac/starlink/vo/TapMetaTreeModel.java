package uk.ac.starlink.vo;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * TreeModel for representing a TAP table set.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2015
 */
public class TapMetaTreeModel implements TreeModel {

    private final List<TreeModelListener> listeners_;
    private SchemaMeta[] schemas_;

    /**
     * Constructs an empty tree model.
     */
    public TapMetaTreeModel() {
        this( new SchemaMeta[ 0 ] );
    }

    /**
     * Constructs a tree model to display a given table set.
     *
     * @param  schemas  schema array defining the table metadata to be
     *                  represented
     */
    public TapMetaTreeModel( SchemaMeta[] schemas ) {
        listeners_ = new ArrayList<TreeModelListener>();
        setSchemas( schemas );
    }

    /**
     * Sets the content of this tree.
     *
     * @param  schemas  schema array defining the table metadata to be
     *                  represented
     */
    public void setSchemas( SchemaMeta[] schemas ) {
        schemas_ = schemas;
        TreeModelEvent evt =
            new TreeModelEvent( this, new Object[] { schemas } );
        for ( TreeModelListener lnr : listeners_ ) {
            lnr.treeStructureChanged( evt );
        }
    }

    /**
     * Returns the schemas array that forms the root of this tree model.
     *
     * @return  schema array
     */
    public SchemaMeta[] getSchemas() {
        return schemas_;
    }

    public Object getRoot() {
        return schemas_;
    }

    public boolean isLeaf( Object node ) {
        return asTapNode( node ).isLeaf();
    }

    public int getChildCount( Object parent ) {
        Object[] children = asTapNode( parent ).children_;
        return children == null ? 0 : children.length;
    }

    public Object getChild( Object parent, int index ) {
        return asTapNode( parent ).children_[ index ];
    }

    public int getIndexOfChild( Object parent, Object child ) {
        if ( parent != null && child != null ) {
            Object[] children = asTapNode( parent ).children_;
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
     * Acquires the table metadata object, if any, associated with
     * a given tree path.
     *
     * @param  path  tree path associated with an instance of this class
     * @return   associated TableMeta object, or null
     */
    public static TableMeta getTable( TreePath path ) {
        if ( path != null ) {
            for ( Object element : path.getPath() ) {
                if ( element instanceof TableMeta ) {
                    return (TableMeta) element;
                }
            }
        }
        return null;
    }

    /**
     * Acquires the schema metadata object, if any, associated with
     * a given tree path.
     *
     * @param  path  tree path associated with an instance of this class
     * @return   associated SchemaMeta object, or null
     */
    public static SchemaMeta getSchema( TreePath path ) {
        if ( path != null ) {
            for ( Object element : path.getPath() ) {
                if ( element instanceof SchemaMeta ) {
                    return (SchemaMeta) element;
                }
            }
        }
        return null;
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
    private static TapNode asTapNode( Object node ) {
        if ( node instanceof SchemaMeta[] ) {
            final SchemaMeta[] schemas = (SchemaMeta[]) node;
            return new TapNode( schemas );
        }
        else if ( node instanceof SchemaMeta ) {
            final SchemaMeta schema = (SchemaMeta) node;
            return new TapNode( schema.getTables() );
        }
        else {
            assert node instanceof TableMeta;
            return new TapNode( null );
        }
    }

    /**
     * Defines the basic behaviour required from a node in this tree.
     */
    private static class TapNode {
        final Object[] children_;

        /**
         * Constructor.
         *
         * @param  children  child node objects; if null, it's a leaf
         */
        TapNode( Object[] children ) {
            children_ = children;
        }

        /**
         * Indicates whether this object is a leaf.
         *
         * @return  true iff this is not the sort of node
         *          that can have children
         */
        boolean isLeaf() {
            return children_ == null;
        }
    }
}
