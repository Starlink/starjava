package uk.ac.starlink.ttools.votlint;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure which keeps track of the elements currently in scope
 * during the SAX parse.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class HandlerStack {

    /** The stack is implemented using a List of Item objects. */
    private final List stack_ = new ArrayList();

    /** The number of items currently in the list. */
    private int nItem = 0;

    /**
     * Pops a handler off the top of the stack.
     *
     * @return   the element which has just been removed
     */
    public ElementHandler pop() {
        if ( nItem > 0 ) {
            return ((Item) stack_.remove( --nItem )).handler_;
        }
        else {
            throw new IllegalStateException( "Empty stack" );
        }
    }

    /**
     * Pushes a new handler onto the stack.
     *
     * @param  handler  handler to push
     */
    public void push( ElementHandler handler ) {
        if ( nItem > 0 ) {
            ((Item) stack_.get( nItem - 1 )).nChild_++;
        }
        stack_.add( new Item( handler ) );
        nItem++;
    }

    /**
     * Returns the handler at the top of the list without removing it.
     *
     * @return   top element
     */
    public ElementHandler top() {
        return nItem > 0 ? ((Item) stack_.get( nItem - 1 )).handler_
                         : null;
    }

    /**
     * Returns an ancestry object for the handler at the top of this stack.
     * This is only valid for as long as that object is still in the stack.
     * An attempt to use it after that point will give garbage results or
     * may result in an unchecked throwable.
     *
     * @return  ancestry for the top element
     */
    public Ancestry getAncestry() {
        final int point = nItem - 1;
        final Item self = (Item) stack_.get( point );
        final int siblingIndex = point > 0
                               ? ((Item) stack_.get( point - 1 )).nChild_ - 1
                               : 0;
        return new Ancestry() {
            public int getSiblingIndex() {
                return siblingIndex;
            }
            public int getChildCount() {
                return self.nChild_;
            }
            public ElementHandler getSelf() {
                check();
                return ((Item) stack_.get( point )).handler_;
            }
            public ElementHandler getParent() {
                check();
                return point > 0 ? ((Item) stack_.get( point - 1 )).handler_
                                 : null;
            }
            public ElementHandler getAncestor( Class clazz ) {
                check();
                if ( ElementHandler.class.isAssignableFrom( clazz ) ) {
                    for ( int i = point - 1; i >= 0; i-- ) {
                        ElementHandler handler = 
                            ((Item) stack_.get( i )).handler_;
                        if ( clazz.isAssignableFrom( handler.getClass() ) ) {
                            return handler;
                        }
                    }
                    return null;
                }
                else {
                    throw new IllegalArgumentException( clazz.getName() + 
                                                        " not ElementHandler" );
                }
            }
            private void check() {
                if ( stack_.get( point ) != self ) {
                    throw new IllegalStateException();
                }
            }
        };
    }

    /**
     * Helper class which is used for storing items on the stack.
     * It encapsulates both the element handler itself and the number
     * of children it so far has.
     */
    private static class Item {

        /** Element handler. */
        final ElementHandler handler_;

        /* Number of children currently owned by handler. */
        int nChild_;

        /**
         * Constructor.
         */
        Item( ElementHandler handler ) {
            handler_ = handler;
        }
    }
}
