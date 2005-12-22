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
public class PointSelectorSet extends JPanel {

    private final JTabbedPane tabber_;
    private final String[] axisNames_;
    private final StyleSet styles_;
    private final BitSet usedMarkers_;
    private final ActionForwarder actionForwarder_;
    private final TopcatForwarder topcatForwarder_;
    private final OrderRecorder orderRecorder_;
    private final List toggleSetList_;
    private int selectorsCreated_;

    /**
     * Constructs a new set, with given names for the axis labels.
     *
     * @param  axisNames  axis names; length defines dimensionality of 
     *         point selectors
     * @param  styles    style set
     */
    public PointSelectorSet( String[] axisNames, StyleSet styles ) {
        super( new BorderLayout() );
        tabber_ = new JTabbedPane();
        axisNames_ = axisNames;
        styles_ = styles;
        usedMarkers_ = new BitSet();
        selectorsCreated_ = 0;
        actionForwarder_ = new ActionForwarder();
        topcatForwarder_ = new TopcatForwarder();
        toggleSetList_ = new ArrayList();
        add( new SizeWrapper( tabber_ ), BorderLayout.CENTER );

        Action newSelectorAction =
            new BasicAction( "Add Dataset", ResourceIcon.ADD,
                             "Add a new data set" ) {
                public void actionPerformed( ActionEvent evt ) {
                    addNewSelector();
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
     * Add a named array of toggle buttons to be displayed with this 
     * selector set.  These describe some boolean attribute which may 
     * be applied under user control to each axis.
     * Toggle button sets should be added using this method only 
     * <em>before</em> {@link #addNewSelector} has been called for the
     * first time.
     *
     * @param  name  short label for the control
     * @param  models  array of button models, one for each axis
     */
    public void addAxisToggles( String name, ToggleButtonModel[] models ) {
        if ( models.length != axisNames_.length ) {
            throw new IllegalArgumentException();
        }
        toggleSetList_.add( new ToggleSet( name, models ) );
    }

    /**
     * Returns an array of the ToggleSets which have been added by 
     * {@link #addAxisToggles}.
     *
     * @return   array of per-axis toggle sets
     */
    public ToggleSet[] getAxisToggleSets() {
        return (ToggleSet[]) toggleSetList_.toArray( new ToggleSet[ 0 ] );
    }

    /**
     * Returns the number of axes this component will deal with.
     *
     * @return  dimensionality
     */
    public int getNdim() {
        return axisNames_.length;
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
        return new PointSelection( getNdim(), activeSelectors, names,
                                   subsetPointers );
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
     */
    public void addNewSelector() {
        PointSelector psel = 
            new PointSelector( axisNames_, getAxisToggleSets(),
                               new PoolStyleSet( styles_, usedMarkers_ ),
                               null );
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

        /* Return styles used by the selector which is no longer required
         * to the pool. */
        StyleSet styles = psel.getStyles();
        if ( styles instanceof PoolStyleSet ) {
            ((PoolStyleSet) styles).reset();
        }

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
     * Ensures that the tabbed pane never has any blank PointSelectors
     * except maybe one at the end.  This method may be called
     * from an event handler, so ensure that it doesn't change the model
     * directly.
     */
    private void tidyModel() {
        if ( getSelectorCount() > 1 ) {
            for ( int i = 0; i < getSelectorCount(); i++ ) {
                final PointSelector psel = getSelector( i );
                if ( psel.getTable() == null ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            removeSelector( psel );
                        }
                    } );

                    /* Don't attempt more than one at a time.  The change
                     * will trigger events that cause this method to be
                     * called again.  When the model is tidy, this method
                     * won't cause itself to be recalled. */
                    return;
                }
            }
        }
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
     * Encapsulates an array of toggle button models with an associated name.
     */
    static class ToggleSet {
        final String name_;
        final ToggleButtonModel[] models_;
        ToggleSet( String name, ToggleButtonModel[] models ) {
            name_ = name;
            models_ = (ToggleButtonModel[]) models.clone();
        }
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
