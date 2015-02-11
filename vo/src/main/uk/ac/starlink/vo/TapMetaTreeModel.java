package uk.ac.starlink.vo;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * TreeModel for representing a TAP table set.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2015
 */
public class TapMetaTreeModel implements TreeModel {

    private final SchemaMeta[] schemas_;

    /**
     * Constructor.
     *
     * @param   schemas  schema array defining the table metadata
     *                   to be represented
     */
    public TapMetaTreeModel( SchemaMeta[] schemas ) {
        schemas_ = schemas;
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
        // tree structure is immutable
    }

    public void removeTreeModelListener( TreeModelListener lnr ) {
        // tree structure is immutable
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
