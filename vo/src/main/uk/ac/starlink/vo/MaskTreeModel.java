package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Wrapper tree model that includes only a selection of the nodes
 * in the base model.  The selection is controlled by a supplied Mask object.
 *
 * <p>It's intended for use with a static base model.  It ought to work
 * for a base model which sends TreeModelEvents, but that hasn't been
 * tested, and it doesn't translate the events very cleverly
 * to downstream listeners.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2015
 */
public class MaskTreeModel implements TreeModel {

    private final boolean includeDescendants_;
    private final List<TreeModelListener> listeners_;
    private final TreeModelListener baseModelListener_;
    private TreeModel base_;
    private int baseNodeCount_;
    private Content content_;

    private static final List<Object> EMPTY_CHILDLIST = Collections.emptyList();

    /**
     * Constructor.
     *
     * @param  base   underlying tree model
     * @param  includeDescendants  if true, all descendants of an included node
     *                             are automatically included
     */
    public MaskTreeModel( TreeModel base, boolean includeDescendants ) {
        includeDescendants_ = includeDescendants;
        base_ = base;
        listeners_ = new ArrayList<TreeModelListener>();
        baseModelListener_ = new BaseModelListener();
        base_.addTreeModelListener( baseModelListener_ );
        baseNodeCount_ = -1;
    }

    public Object getRoot() {
        return base_.getRoot();
    }

    public boolean isLeaf( Object node ) {
        return base_.isLeaf( node );
    }

    public int getChildCount( Object parent ) {
        if ( content_ == null ) {
            return base_.getChildCount( parent );
        }
        else {
            List<Object> children = content_.childMap_.get( parent );
            return children == null ? 0 : children.size();
        }
    }

    public Object getChild( Object parent, int index ) {
        if ( content_ == null ) {
            return base_.getChild( parent, index );
        }
        else {
            List<Object> children = content_.childMap_.get( parent );
            return children == null ? null : children.get( index );
        }
    }

    public int getIndexOfChild( Object parent, Object child ) {
        if ( content_ == null ) {
            return base_.getIndexOfChild( parent, child );
        }
        else {
            List<Object> children = content_.childMap_.get( parent );
            return children == null ? -1 : children.indexOf( child );
        }
    }

    public void valueForPathChanged( TreePath path, Object newValue ) {
        base_.valueForPathChanged( path, newValue );
    }

    public void addTreeModelListener( TreeModelListener lnr ) {
        listeners_.add( lnr );
    }

    public void removeTreeModelListener( TreeModelListener lnr ) {
        listeners_.remove( lnr );
    }

    /**
     * Returns the underlying, unmasked, model.
     *
     * @return   base tree model
     */
    public TreeModel getBaseModel() {
        return base_;
    }

    /**
     * Sets the underlying, unmasked, model.
     *
     * @param  base  new base model
     */
    public void setBaseModel( TreeModel base ) {
        base_.removeTreeModelListener( baseModelListener_ );
        base_ = base;
        baseNodeCount_ = -1;
        if ( content_ != null ) {
            content_ = createContent( base_, content_.mask_ );
        }
        base_.addTreeModelListener( baseModelListener_ );
        fireStructureChanged();
    }

    /**
     * Returns the currently active mask.
     *
     * @return  mask, may be null for full inclusion
     */
    public Mask getMask() {
        return content_ == null ? null : content_.mask_;
    }

    /**
     * Sets the mask for defning inclusion of nodes in the base model.
     * A null mask is used for full inclusion (and will be more efficient
     * than one for which <code>isIncluded</code> always returns true).
     *
     * <p>If the supplied <code>mask</code> matches the previously installed
     * one by equality, no action is performed.  So it's not essential
     * that mask implementations implement equals/hashCode, but it may be
     * beneficial.
     *
     * @param  mask  new mask, or null for full inclusion
     */
    public void setMask( Mask mask ) {
        if ( content_ == null ? mask != null
                              : ! content_.mask_.equals( mask ) ) {
            content_ = createContent( base_, mask );
            fireStructureChanged();
        }
    }

    /**
     * Returns the total number of nodes in this model.
     *
     * @return   node count
     */
    public int getNodeCount() {
        if ( content_ != null ) {
            return content_.inclusionSet_.size();
        }
        else {
            if ( baseNodeCount_ < 0 ) {
                baseNodeCount_ = countDescendants( base_.getRoot(), base_ );
            }
            return baseNodeCount_;
        }
    }

    /**
     * Messages listeners that the tree structure has changed completely.
     */
    private void fireStructureChanged() {
        TreeModelEvent evt =
            new TreeModelEvent( this, new TreePath( getRoot() ) );
        for ( TreeModelListener lnr : listeners_ ) {
            lnr.treeStructureChanged( evt );
        }
    }

    /**
     * Constructs a Content object that calculates and stores static
     * information about nodes included from a given tree model by
     * a given mask.
     *
     * @param   model   base tree model
     * @param   mask    mask to apply
     * @return    node inclusion cache, or null for full inclusion
     */
    private Content createContent( TreeModel model, Mask mask ) {
        if ( mask == null ) {
            return null;
        }
        Set<Object> inclusionSet = new HashSet<Object>();
        Stack<Object> path = new Stack<Object>();
        Object root = model.getRoot();
        path.push( root );
        boolean allIncluded =
            addIncludedNodes( inclusionSet, path, model, mask, false,
                              includeDescendants_ ); 
        inclusionSet.add( root );
        if ( allIncluded ) {
            return null;
        }
        Map<Object,List<Object>> childMap = new HashMap<Object,List<Object>>();
        addChildLists( childMap, root, inclusionSet, model );
        return new Content( mask, inclusionSet, childMap );
    }

    /**
     * Recursively counts the descendents of a tree node.
     *
     * @param  node   node
     * @param  model  tree model
     * @return   number of descendents of node, including itself
     */
    private static int countDescendants( Object node, TreeModel model ) {
        int n = 1;
        int nc = model.getChildCount( node );
        for ( int ic = 0; ic < nc; ic++ ) {
            n += countDescendants( model.getChild( node, ic ), model );
        }
        return n;
    }

    /**
     * Recursively populates a supplied Set of nodes from a given tree path
     * as included by a given mask.  Not only those nodes included
     * in the mask, but all of their ancestor nodes, are added to the Set.
     * The result is therefore the set of all nodes that must appear in
     * the tree to make the mask inclusions visible, which is in general
     * larger than the set of nodes actually permitted by the mask.
     * All descendants of included nodes may also be unconditionally
     * included, according to the <code>includeDescendants</code> parameter.
     *
     * <p>The return value is true only if all nodes under the given path
     * are included in the mask and hence added to the inclusion Set.
     *
     * @param  inclusionSet  collection to which nodes are added
     * @param  path    tree path at which to start recursing
     *                 (works like a TreePath; element 0 is tree root)
     * @param  model   tree model
     * @param  mask    inclusion mask
     * @param  ancestorsIncluded  true iff the ancestor elements of the
     *                            submitted path have already been included
     * @param  includeDescendants  if true, all descendants of an included node
     *                             are automatically included
     * @return   true iff path and all its descendants are included
     */
    private static boolean addIncludedNodes( Set<Object> inclusionSet,
                                             Stack<Object> path,
                                             TreeModel model, Mask mask,
                                             boolean ancestorsIncluded,
                                             boolean includeDescendants ) {
        Object node = path.peek();
        final boolean nodeIncluded;
        if ( ancestorsIncluded ) {
            nodeIncluded = includeDescendants || mask.isIncluded( node );
            if ( nodeIncluded ) {
                inclusionSet.add( node );
            }
        }
        else {
            nodeIncluded = mask.isIncluded( node );
            if ( nodeIncluded ) {
                for ( Object ancestor : path ) {
                    inclusionSet.add( ancestor );
                }
            }
        }
        boolean allIncluded = nodeIncluded;
        int nchild = model.getChildCount( node );
        for ( int ic = 0; ic < nchild; ic++ ) {
            path.push( model.getChild( node, ic ) );
            allIncluded = addIncludedNodes( inclusionSet, path, model, mask,
                                            nodeIncluded, includeDescendants )
                       && allIncluded;
            path.pop();
        }
        return allIncluded;
    }

    /**
     * Recursively populates a map representing the masked tree.
     * It maps nodes to Lists of child nodes, including all those
     * descendants of the given node (including itself)
     * which appear in the supplied TreeModel
     * and which also appear in the supplied inclusion set.
     *
     * @param  childMap   map to append node-&gt;childlist entries
     * @param  node   node at which to begin recursive descent
     * @param  inclusionSet   set of all included nodes
     * @param  model   base tree model
     */
    private static void addChildLists( Map<Object,List<Object>> childMap,
                                       Object node, Set<Object> inclusionSet,
                                       TreeModel model ) {
        int nc = model.getChildCount( node );
        List<Object> childList = new ArrayList<Object>();
        for ( int ic = 0; ic < nc; ic++ ) {
            Object child = model.getChild( node, ic );
            if ( inclusionSet.contains( child ) ) {
                childList.add( child );
                addChildLists( childMap, child, inclusionSet, model );
            }
        }
        childMap.put( node, childList.isEmpty() ? EMPTY_CHILDLIST : childList );
    }

    /**
     * Defines node inclusion in a masked tree.
     */
    public interface Mask {

        /**
         * Determines whether a given node from a base tree model
         * is to appear in the masked tree.
         *
         * @param  node  base model node
         * @return   true iff mask allows <code>node<code>
         */
        boolean isIncluded( Object node );
    }

    /**
     * Cache for information about nodes included according to a
     * particular mask.  It is specific to a given base model.
     */
    private static class Content {
        final Mask mask_;
        final Set<Object> inclusionSet_;
        final Map<Object,List<Object>> childMap_;

        /**
         * Constructor.
         *
         * @param  mask   inclusion policy used to populate this object
         * @param  inclusionSet   collection of nodes from the base model
         *                        that appear in the masked tree
         * @param  childMap  map of node-&gt;list-of-children that defines
         *                   the content of the masked tree
         */
        Content( Mask mask, Set<Object> inclusionSet,
                 Map<Object,List<Object>> childMap ) {
            mask_ = mask;
            inclusionSet_ = inclusionSet;
            childMap_ = childMap;
            assert mask_ != null && inclusionSet_ != null && childMap_ != null;
        }
    }

    /**
     * TreeModelListener implementation installed on the base model
     * to propagate base model events forward to listeners to this masked model.
     */
    private class BaseModelListener implements TreeModelListener {
        public void treeNodesChanged( TreeModelEvent evt ) {
            if ( ! recontent() ) {
                for ( TreeModelListener lnr : listeners_ ) {
                    lnr.treeNodesChanged( evt );
                }
            }
        }
        public void treeNodesInserted( TreeModelEvent evt ) {
            if ( ! recontent() ) {
                for ( TreeModelListener lnr : listeners_ ) {
                    lnr.treeNodesInserted( evt );
                }
            }
        }
        public void treeNodesRemoved( TreeModelEvent evt ) {
            if ( ! recontent() ) {
                for ( TreeModelListener lnr : listeners_ ) {
                    lnr.treeNodesRemoved( evt );
                }
            }
        }
        public void treeStructureChanged( TreeModelEvent evt ) {
            if ( ! recontent() ) {
                for ( TreeModelListener lnr : listeners_ ) {
                    lnr.treeStructureChanged( evt );
                }
            }
        }

        /**
         * Recalculates the content object defining the state of this tree
         * if required.  If changes are made, all downstream listeners
         * are messaged that the tree structure has changed
         * (this is overkill, but it's hard work to try to construct
         * less drastic events for forwarding), and returns true.
         *
         * <p>If no mask is in place, the content doesn't need to be adjusted,
         * and false is returned, indicating that no events have been
         * sent to listeners since the original base model events
         * can be forwarded unchanged.
         *
         * @return   false if no message has been sent
         */
        private boolean recontent() {
            if ( content_ != null ) {
                content_ = createContent( base_, content_.mask_ );
                fireStructureChanged();
                return true;
            }
            else {
                return false;
            }
        }
    }
}
