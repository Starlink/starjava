package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatForwarder;
import uk.ac.starlink.topcat.TopcatListener;

/**
 * Component which keeps track of a number of {@link PointSelector} objects.
 * It currently uses a JTabbedPane to present them.
 * 
 * @author   Mark Taylor
 * @since    1 Nov 2005
 */
public class PointSelectorSet extends JPanel {

    private final JTabbedPane tabber_;
    private final String[] axisNames_;
    private final GraphicsWindow gwin_;
    private final ActionForwarder actionForwarder_;
    private final TopcatForwarder topcatForwarder_;
    private int selectorsCreated_;

    /**
     * Constructs a new set, with given names for the axis labels.
     *
     * @param  axisNames  axis names; length defines dimensionality of 
     *         point selectors
     */
    public PointSelectorSet( String[] axisNames, GraphicsWindow gwin ) {
        super( new BorderLayout() );
        tabber_ = new JTabbedPane();
        axisNames_ = axisNames;
        gwin_ = gwin;
        selectorsCreated_ = 0;
        actionForwarder_ = new ActionForwarder();
        topcatForwarder_ = new TopcatForwarder();
        add( new SizeWrapper( tabber_ ), BorderLayout.SOUTH );
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
        for ( int i = 0; i < getSelectorCount(); i++ ) {
            PointSelector psel = getSelector( i );
            if ( psel.isValid() &&
                 psel.getOrderedSubsetSelection().length > 0 ) {
                activeList.add( psel );
            }
        }
        PointSelector[] activeSelectors = 
            (PointSelector[]) activeList.toArray( new PointSelector[ 0 ] );
        return new PointSelection( getNdim(), activeSelectors );
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
            new PointSelector( axisNames_, gwin_.getDefaultStyles( -1 ), null );
        addSelector( psel );
        tabber_.setSelectedComponent( psel );
    }

    /**
     * Removes the currently selected selector from this set.
     */
    public void removeCurrentSelector() {
        removeSelector( (PointSelector) tabber_.getSelectedComponent() );
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
        tabber_.remove( psel );
        psel.removeActionListener( actionForwarder_ );
        action();
    }

    /**
     * Invoked when the state of this object changes; it forwards 
     * a notification to registered action listeners.
     */
    private void action() {
        actionForwarder_.actionPerformed( new ActionEvent( this, 0, 
                                                           "State change" ) );
    }

    /**
     * Returns a default mark style profile for use with a new PointSelector.
     * Ideally this should dispense different styles than those already
     * used by other profiles.
     *
     * @return   mark style profile
     */
    private MarkStyleProfile getNextMarkStyles() {
        return gwin_.getDefaultStyles( -1 );
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
    public String getNextTabName() {
        return selectorsCreated_++ == 0
             ? "Main"
             : new String( new char[] { (char)
                                        ( 'A' + selectorsCreated_ - 2 ) } );
    }
}
