package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatForwarder;
import uk.ac.starlink.topcat.TopcatListener;

/**
 * Component which keeps track of a number of {@link PointSelector} objects.
 * It currently uses a JTabbedPane to present them.
 * 
 * <p>It also keeps track of whether the selected axes are reversed (flipped)
 * and whether they use linear or logarithmic scales.  This is not 
 * logically the job of this component, but the checkboxes really have 
 * to go in the same bit of screen space, so for practical reasons
 * they are here.
 * 
 * @author   Mark Taylor
 * @since    1 Nov 2005
 */
public abstract class PointSelectorSet extends JPanel {

    private final JTabbedPane tabber_;
    private final ActionForwarder actionForwarder_;
    private final TopcatForwarder topcatForwarder_;
    private final OrderRecorder orderRecorder_;
    private StyleSet styles_;
    private BitSet usedMarkers_;
    private int selectorsCreated_;

    /**
     * Constructs a new set.
     */
    public PointSelectorSet( StyleSet styles ) {
        super( new BorderLayout() );
        setStyles( styles );
        tabber_ = new JTabbedPane();
        selectorsCreated_ = 0;
        actionForwarder_ = new ActionForwarder();
        topcatForwarder_ = new TopcatForwarder();
        add( new SizeWrapper( tabber_ ), BorderLayout.CENTER );

        Action newSelectorAction =
            new BasicAction( "Add Dataset", ResourceIcon.ADD,
                             "Add a new data set" ) {
                public void actionPerformed( ActionEvent evt ) {
                    PointSelector psel = createSelector();
                    resetStyles( psel );
                    addNewSelector( psel );
                } 
            };
        final Action removeSelectorAction =
            new BasicAction( "Remove Dataset", ResourceIcon.SUBTRACT,
                             "Remove the current dataset" ) {
                public void actionPerformed( ActionEvent evt ) {
                    removeCurrentSelector();
                }
            };
        removeSelectorAction.setEnabled( false );
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                removeSelectorAction.setEnabled( tabber_
                                                .getSelectedIndex() > 0 );
            }
        } );

        orderRecorder_ = new OrderRecorder();
        addActionListener( orderRecorder_ );

        JToolBar toolBox = new JToolBar( JToolBar.VERTICAL );
        toolBox.setFloatable( false );
        toolBox.add( newSelectorAction );
        toolBox.add( removeSelectorAction );
        add( toolBox, BorderLayout.WEST );
    }

    /**
     * Returns the number of selectors in this set.
     *
     * @return  selector count
     */
    public int getSelectorCount() {
        return tabber_.getTabCount();
    }

    /**
     * Returns the selector at a given index.
     *
     * @param   index  index of the selector to return
     * @return  selector
     */
    public PointSelector getSelector( int index ) {
        return (PointSelector) tabber_.getComponentAt( index );
    }

    /**
     * Factory method to construct new PointSelector objects to go in
     * this PointSelectorSet.
     *
     * @return   new point selector component
     */
    protected abstract PointSelector createSelector();

    /**
     * Returns the data specification reflecting the current state of this
     * component.  This contains all the information about what points
     * are to be plotted.
     *
     * @return  point selection object
     */
    public PointSelection getPointSelection() {
        List activeList = new ArrayList();
        List nameList = new ArrayList();
        for ( int i = 0; i < getSelectorCount(); i++ ) {
            PointSelector psel = getSelector( i );
            if ( psel.isValid() ) {
                activeList.add( psel );
                nameList.add( psel == getMainSelector() 
                            ? null 
                            : tabber_.getTitleAt( i ) );
            }
        }
        PointSelector[] activeSelectors = 
            (PointSelector[]) activeList.toArray( new PointSelector[ 0 ] );
        String[] names = (String[]) nameList.toArray( new String[ 0 ] );
        int[][] subsetPointers =
            orderRecorder_.getSubsetPointers( activeSelectors );
        return new PointSelection( activeSelectors, names, subsetPointers );
    }

    /**
     * Returns the 'main' PointSelector contained in this set.
     *
     * @return  main selector
     */
    public PointSelector getMainSelector() {
        return getSelector( 0 );
    }

    /**
     * Adds a new selector to this set.
     *
     * @param  psel  new selector
     */
    public void addNewSelector( PointSelector psel ) {
        addSelector( psel );
        tabber_.setSelectedComponent( psel );
    }

    /**
     * Removes the currently selected selector from this set.
     */
    public void removeCurrentSelector() {
        if ( tabber_.getSelectedIndex() != 0 ) {
            removeSelector( (PointSelector) tabber_.getSelectedComponent() );
        }
    }

    /**
     * Sets the default style profile for this point selector set.
     *
     * @param  styles  new style set
     */
    public void setStyles( StyleSet styles ) {
        styles_ = styles;
        usedMarkers_ = new BitSet();
        if ( tabber_ != null ) {
            for ( int i = 0; i < tabber_.getTabCount(); i++ ) {
                resetStyles( (PointSelector) tabber_.getComponentAt( i ) );
            }
        }
    }

    /**
     * Configures a given PointSelector to use this set's current
     * plotting style set.
     *
     * @param   psel  point selector to reconfigure
     */
    private void resetStyles( PointSelector psel ) {
        psel.setStyles( new PoolStyleSet( styles_, usedMarkers_ ) );
    }

    /**
     * Adds an action listener.
     * Such listeners will be notified any time PointSelectors are
     * added to or removed from this set, and any time the state of
     * any selector currently a member of this set changes.
     *
     * @param   listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addListener( listener );
    }

    /**
     * Removes an action listener.
     *
     * @param  listener  listener to remove
     * @see    #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeListener( listener );
    }

    /**
     * Adds a TopcatListener which will be notified when changes occur to
     * any TopcatModel associated with this component.
     *
     * @param   listener  listener to add
     */
    public void addTopcatListener( TopcatListener listener ) {
        topcatForwarder_.addListener( listener );
    }

    /**
     * Removes a TopcatListener which was previously added.
     *
     * @param  listener  listener to remove
     * @see    #addTopcatListener
     */
    public void removeTopcatListener( TopcatListener listener ) {
        topcatForwarder_.removeListener( listener );
    }

    /**
     * Adds a given selector to this set.
     *
     * @param  psel  selector
     */
    private void addSelector( final PointSelector psel ) {

        /* Configure the selector to use this set's house style set. */
        resetStyles( psel );

        /* Add the selector to the tabbed frame. */
        tabber_.add( getNextTabName(), psel );

        /* Make sure actions on the selector will be propagated to this
         * component's listeners. */
        psel.addActionListener( actionForwarder_ );
        psel.addTopcatListener( topcatForwarder_ );

        /* Notify listeners that something has happened. */
        if ( psel.getTable() != null ) {
            action();
        }
    }

    /**
     * Removes a given selector from this set.
     *
     * @param  psel  selector
     */
    private void removeSelector( PointSelector psel ) {

        /* Remove the selector. */
        tabber_.remove( psel );
        psel.removeActionListener( actionForwarder_ );

        /* Notify listeners that something has happened. */
        action();
    }

    /**
     * Invoked when the state of this object changes; it forwards 
     * a notification to registered action listeners.
     */
    private void action() {
        actionForwarder_.actionPerformed( new ActionEvent( this, 0, 
                                                           "State change" ) );
        tabber_.revalidate();
    }

    /**
     * Returns the name to use for the index'th tab.
     * 
     * @param  index  tab index
     * @return  tab name
     */
    private String getNextTabName() {
        return selectorsCreated_++ == 0
             ? "Main"
             : new String( new char[] { (char)
                                        ( 'A' + selectorsCreated_ - 2 ) } );
    }

    /**
     * Helper class which keeps track of the order in which the subsets
     * have been selected/deselected in the various point selectors.
     */
    private class OrderRecorder implements ActionListener {

        /**
         * Stores a boolean[] for each PointSelector, indicating which
         * subsets are selected.
         */
        final Map flagMap_ = new WeakHashMap();

        /** List of Item objects giving the order of subsets chosen. */
        final List order_ = new ArrayList();

        /**
         * Returns a structure indicating the order in which selection
         * subsets are stored.  Each element of the return value is
         * a two-element int array <code>(isel,isub)</code>; 
         * the first element is the index
         * of a point selector in the supplied <code>selectors</code> list,
         * and the second element is the index of the subset within
         * that selector.
         *
         * @param   selectors  list of selectors to enquire about
         * @return  array giving selection order
         */
        int[][] getSubsetPointers( PointSelector[] selectors ) {
            List resultList = new ArrayList();
            for ( Iterator it = order_.iterator(); it.hasNext(); ) {
                Item item = (Item) it.next();
                for ( int isel = 0; isel < selectors.length; isel++ ) {
                    if ( item.sel_ == selectors[ isel ] ) {
                        resultList.add( new int[] { isel, item.isub_ } );
                        break;
                    }
                }
            }
            return (int[][]) resultList.toArray( new int[ 0 ][] );
        }

        /**
         * Checks the current state of this selector set and updates its
         * records accordingly.
         */
        public void updateState() {
            for ( int isel = 0; isel < getSelectorCount(); isel++ ) {
                PointSelector sel = getSelector( isel );
                boolean[] oldFlags = flagMap_.containsKey( sel )
                                   ? (boolean[]) flagMap_.get( sel )
                                   : new boolean[ 0 ];
                boolean[] newFlags = sel.getSubsetSelection();
                flagMap_.put( sel, newFlags );
                for ( int isub = 0; isub < Math.max( oldFlags.length,
                                                     newFlags.length );
                      isub++ ) {
                    boolean oldFlag = isub < oldFlags.length ? oldFlags[ isub ]
                                                             : false;
                    boolean newFlag = isub < newFlags.length ? newFlags[ isub ]
                                                             : false;
                    Item item = new Item( sel, isub );
                    if ( ! oldFlag && newFlag ) {
                        assert ! order_.contains( item );
                        order_.add( item );
                    }
                    else if ( oldFlag && ! newFlag ) {
                        assert order_.contains( item );
                        order_.remove( item );
                        assert ! order_.contains( item );
                    }
                    else {
                        assert oldFlag == newFlag;
                    }
                }
            }
        }

        public void actionPerformed( ActionEvent evt ) {
            updateState();
        }

        /**
         * Helper class which defines a selected subset.
         */
        private class Item {
            final PointSelector sel_;
            final int isub_;
            Item( PointSelector sel, int isub ) {
                sel_ = sel;
                isub_ = isub;
            }
            public boolean equals( Object other ) {
                return other instanceof Item 
                    && ((Item) other).sel_ == sel_
                    && ((Item) other).isub_ == isub_;
            }
            public int hashCode() {
                return sel_.hashCode() * 23 + isub_;
            }
            public String toString() {
                return tabber_.getTitleAt( tabber_.indexOfComponent( sel_ ) )
                     + ":" + isub_;
            }
        }
    }
}
