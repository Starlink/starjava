package uk.ac.starlink.topcat.plot;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
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
    private final Action addSelectorAction_;
    private final Action removeSelectorAction_;
    private BitSet usedMarkers_;
    private int selectorsCreated_;
    private StyleWindow styleWindow_;

    public static final String MAIN_TAB_NAME = "Main";

    /**
     * Constructs a new set.
     */
    @SuppressWarnings("this-escape")
    public PointSelectorSet() {
        super( new BorderLayout() );
        tabber_ = new JTabbedPane();
        selectorsCreated_ = 0;
        actionForwarder_ = new ActionForwarder();
        topcatForwarder_ = new TopcatForwarder();
        add( tabber_, BorderLayout.CENTER );

        addSelectorAction_ =
            new BasicAction( "Add Dataset", ResourceIcon.ADD_TAB,
                             "Add a new data set" ) {
                public void actionPerformed( ActionEvent evt ) {
                    PointSelector psel = createSelector();
                    addNewSelector( psel );
                } 
            };
        removeSelectorAction_ =
            new BasicAction( "Remove Dataset", ResourceIcon.REMOVE_TAB,
                             "Remove the current dataset" ) {
                public void actionPerformed( ActionEvent evt ) {
                    removeCurrentSelector();
                }
            };
        removeSelectorAction_.setEnabled( false );
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                removeSelectorAction_.setEnabled( tabber_
                                                 .getSelectedIndex() > 0 );
            }
        } );

        orderRecorder_ = new OrderRecorder();
        addActionListener( orderRecorder_ );
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
     * Returns an action which adds a new selector to this set.
     *
     * @return  add action
     */
    public Action getAddSelectorAction() {
        return addSelectorAction_;
    }

    /**
     * Returns an action which removes a selector from this set.
     *
     * @return  remove action
     */
    public Action getRemoveSelectorAction() {
        return removeSelectorAction_;
    }

    /**
     * Factory method to construct new PointSelector objects to go in
     * this PointSelectorSet.
     *
     * @return   new point selector component
     */
    protected abstract PointSelector createSelector();

    /**
     * Factory method to construct a StyleEditor component for configuring
     * how different styles appear in the plot.
     *
     * @return  new style editor component
     */
    protected abstract StyleEditor createStyleEditor();

    public StyleWindow getStyleWindow() {
        if ( styleWindow_ == null ) {
            Frame parent =
                (Frame) SwingUtilities.getAncestorOfClass( Frame.class, this );
            StyleEditor ed = createStyleEditor();
            styleWindow_ = new StyleWindow( parent, ed );
            styleWindow_.setTitle( "Plot Style Editor" );
        }
        return styleWindow_;
    }

    /**
     * Returns the data specification reflecting the current state of this
     * component.  This contains all the information about what points
     * are to be plotted.
     *
     * @return  point selection object
     */
    public PointSelection getPointSelection() {

        /* Assemble a list of the point selectors with plottable data. */
        List<PointSelector> activeList = new ArrayList<PointSelector>();
        for ( int i = 0; i < getSelectorCount(); i++ ) {
            PointSelector psel = getSelector( i );
            if ( psel.isReady() ) {
                activeList.add( psel );
            }
        }
        PointSelector[] activeSelectors =
            activeList.toArray( new PointSelector[ 0 ] );

        /* Assemble a flattened list of the subsets to be plotted. */
        int[][] subsetPointers =
            orderRecorder_.getSubsetPointers( activeSelectors );

        /* Assemble a list of the labels to use for these subsets. */
        int nset = subsetPointers.length;
        String[] setLabels = new String[ nset ];
        for ( int iset = 0; iset < nset; iset++ ) {
            int isel = subsetPointers[ iset ][ 0 ];
            int itset = subsetPointers[ iset ][ 1 ];
            PointSelector psel = activeSelectors[ isel ];
            setLabels[ iset ] = psel.getSubsetDisplayLabel( itset );
        }

        /* Construct and return a PointSelection object encapsulating
         * this information. */
        return new PointSelection( activeSelectors, subsetPointers, setLabels );
    }

    /**
     * Returns the 'main' PointSelector contained in this set.
     *
     * @return  main selector, or null if there isn't one yet
     */
    public PointSelector getMainSelector() {
        return tabber_.getTabCount() > 0 ? getSelector( 0 )
                                         : null;
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
     * Adds an action listener.
     * Such listeners will be notified any time PointSelectors are
     * added to or removed from this set, and any time the state of
     * any selector currently a member of this set changes.
     *
     * @param   listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes an action listener.
     *
     * @param  listener  listener to remove
     * @see    #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Adds a TopcatListener which will be notified when changes occur to
     * any TopcatModel associated with this component.
     *
     * @param   listener  listener to add
     */
    public void addTopcatListener( TopcatListener listener ) {
        topcatForwarder_.addTopcatListener( listener );
    }

    /**
     * Removes a TopcatListener which was previously added.
     *
     * @param  listener  listener to remove
     * @see    #addTopcatListener
     */
    public void removeTopcatListener( TopcatListener listener ) {
        topcatForwarder_.removeTopcatListener( listener );
    }

    /**
     * Adds a given selector to this set.
     *
     * @param  psel  selector
     */
    private void addSelector( final PointSelector psel ) {

        /* Configure it to use this set's style editing window. */
        psel.setStyleWindow( getStyleWindow() );

        /* Add the selector to the tabbed frame. */
        String label = getNextTabName();
        if ( ! label.equals( MAIN_TAB_NAME ) ) {
            psel.setLabel( label );
        }
        tabber_.add( label, psel );

        /* Make sure actions on the selector will be propagated to this
         * component's listeners. */
        psel.addActionListener( actionForwarder_ );
        psel.addTopcatListener( topcatForwarder_ );
        if ( tabber_.getTabCount() > 1 ) {
            psel.addActionListener( new AxisDefaulter( psel ) );
        }

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
             ? MAIN_TAB_NAME
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
        final Map<PointSelector,boolean[]> flagMap_ =
            new WeakHashMap<PointSelector,boolean[]>();

        /** List of Item objects giving the order of subsets chosen. */
        final List<Item> order_ = new ArrayList<Item>();

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
            List<int[]> resultList = new ArrayList<int[]>();
            for ( Item item : order_ ) {
                for ( int isel = 0; isel < selectors.length; isel++ ) {
                    if ( item.sel_ == selectors[ isel ] ) {
                        resultList.add( new int[] { isel, item.isub_ } );
                        break;
                    }
                }
            }
            return resultList.toArray( new int[ 0 ][] );
        }

        /**
         * Checks the current state of this selector set and updates its
         * records accordingly.
         */
        public void updateState() {
            for ( int isel = 0; isel < getSelectorCount(); isel++ ) {
                PointSelector sel = getSelector( isel );
                boolean[] oldFlags = flagMap_.containsKey( sel )
                                   ? flagMap_.get( sel )
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

    /**
     * Listener which sets default values for plot axes of secondary point
     * selectors given the current values of the main point selector.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private class AxisDefaulter implements ActionListener {

        private final PointSelector mainPsel_;
        private final PointSelector psel_;

        /**
         * Constructor.
         *
         * @param  psel  point selector whose values may be modified
         */
        AxisDefaulter( PointSelector psel ) {
            psel_ = psel;
            mainPsel_ = getMainSelector();
            if ( mainPsel_ == psel_ ) {
                throw new IllegalArgumentException( "Bad/unnecessary idea" );
            }
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Act only if the main point selector has a table, and it is not
             * the same as for this one. */
            if ( PointSelector.TABLE_CHANGED.equals( evt.getActionCommand() ) &&
                 mainPsel_.getTable() != null &&
                 mainPsel_.getTable() != psel_.getTable() ) {
                JComboBox[] selectors =
                    psel_.getAxesSelector().getColumnSelectors();
                JComboBox[] mainSelectors =
                    mainPsel_.getAxesSelector().getColumnSelectors();
                if ( selectors.length != mainSelectors.length ) {
                    assert false;
                    return;
                }

                /* For each JComboBox in the PointSelector, try to set its
                 * value as the same string used in the corresponding one in
                 * the main PointSelector.  In many cases such a setting will
                 * be invalid, since the tables will have different column
                 * names etc - this is fine, and does not cause an error.
                 * If they are the same however, there is a good chance that
                 * choosing these selections is what the user wants to do. */
                for ( int i = 0; i < selectors.length; i++ ) {
                    Object item = mainSelectors[ i ].getSelectedItem();
                    if ( item != null ) {
                        ComboBoxModel model = selectors[ i ].getModel();
                        if ( model instanceof ColumnDataComboBoxModel ) {
                            ColumnDataComboBoxModel cmodel =
                                (ColumnDataComboBoxModel) model;
                            ColumnData cdata;
                            try {
                                cdata = cmodel
                                       .stringToColumnData( item.toString() );
                            }
                            catch ( CompilationException e ) {
                                cdata = null;
                            }
                            if ( cdata != null ) {
                                cmodel.setSelectedItem( cdata );
                            }
                        }
                    }
                }
            }
        }
    }
}
