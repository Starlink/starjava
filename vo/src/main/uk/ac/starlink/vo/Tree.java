package uk.ac.starlink.vo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hierarchical data structure suitable for general use.
 * All instances of this class are either of class {@link Tree.Leaf} or
 * {@link Tree.Branch}.
 *
 * <p>Node might be a better name than Tree, but since it's used with
 * DOM parsing I want to avoid a name clash with
 * {@link org.w3c.dom.Node org.w3c.dom.Node}.
 *
 * @author   Mark Taylor
 * @since    19 Jan 2023
 */
public abstract class Tree<T> {

    /**
     * Private constructor ensures that the only concrete implementations
     * of this class are as defined here: Leaf and Branch.
     */
    private Tree() {
    }

    /**
     * Returns true if this instance is a Leaf, false if it's a Branch.
     *
     * @return  true iff this is a leaf
     */
    public abstract boolean isLeaf();

    /**
     * Returns this instance as a Leaf if it's a leaf,
     * or null if it's a branch.
     *
     * @return  this cast to Leaf, or null
     */
    public abstract Leaf<T> asLeaf();

    /**
     * Returns this instance as a Branch if it's a branch,
     * or null if it's a leaf.
     *
     * @return  this cast to Branch, or null
     */
    public abstract Branch<T> asBranch();

    /**
     * Recursively converts this Tree to one with the same structure,
     * but with the leaf items mapped from their existing values to
     * new values determined by a supplied mapping function.
     *
     * @param  mapping  mapping function
     * @return   tree with mapped values
     */
    public abstract <R> Tree<R> map( Function<T,R> mapping );

    /**
     * Tree instance that contains a referenced item and no children.
     */
    public static class Leaf<T> extends Tree<T> {

        private final T item_;

        /**
         * Constructor.
         *
         * @param  item  item held by this leaf
         */
        public Leaf( T item ) {
            item_ = item;
        }

        /**
         * Returns the item referenced by this leaf.
         *
         * @return  item
         */
        public T getItem() {
            return item_;
        }

        public boolean isLeaf() {
            return true;
        }

        public Leaf<T> asLeaf() {
            return this;
        }

        public Branch<T> asBranch() {
            return null;
        }

        public <R> Leaf<R> map( Function<T,R> mapping ) {
            return new Leaf<R>( mapping.apply( item_ ) );
        }
    }

    /**
     * Tree instance that contains a list of children and no referenced item.
     */
    public static class Branch<T> extends Tree<T> {

        private final String label_;
        private final List<Tree<T>> children_;

        /**
         * Constructor.
         *
         * @param  children  list of child trees
         * @param  label     label for this branch, may be null
         */
        public Branch( List<Tree<T>> children, String label ) {
            label_ = label;
            children_ = children;
        }

        /**
         * Returns this branch's children.
         *
         * @return  list of child trees
         */
        public List<Tree<T>> getChildren() {
            return children_;
        }

        /**
         * Returns this branch's label.
         *
         * @return  label, may be null
         */
        public String getLabel() {
            return label_;
        }

        public boolean isLeaf() {
            return false;
        }

        public Leaf<T> asLeaf() {
            return null;
        }

        public Branch<T> asBranch() {
            return this;
        }

        public <R> Branch<R> map( Function<T,R> mapping ) {
            return new Branch<R>( children_.stream()
                                           .map( t -> t.map( mapping ) )
                                           .collect( Collectors.toList() ),
                                  label_ );
        }
    }
}
