package uk.ac.starlink.topcat.plot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * ListSelectionListener implementation which can tell you what order 
 * items have been selected in.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public class OrderedSelectionRecorder implements ListSelectionListener {

    private BitSet lastState_;
    private ListSelectionModel model_;
    private List orderedSelection_;

    /**
     * Constructs a new recorder based on a given list selection model.
     *
     * @param  model  the model this listener is listening to
     */
    public OrderedSelectionRecorder( ListSelectionModel model ) {
        model_ = model;
        orderedSelection_ = new ArrayList();
        lastState_ = new BitSet();
        valueChanged( new ListSelectionEvent( this, 
                                              model_.getMinSelectionIndex(),
                                              model_.getMaxSelectionIndex(),
                                              false ) );
    }

    /**
     * Returns a list of the currently-selected indices in the selection
     * model in the order in which they were (most recently) added to the
     * selection.
     *
     * @return  ordered selection model indices
     */
    public int[] getOrderedSelection() {
        int nsel = orderedSelection_.size();
        int[] sel = new int[ nsel ];
        for ( int i = 0; i < nsel; i++ ) {
            sel[ i ] = ((Integer) orderedSelection_.get( i )).intValue();
        }
        return sel;
    }

    public void valueChanged( ListSelectionEvent evt ) {
        BitSet oldState = lastState_;
        BitSet newState = getModelState();
        lastState_ = newState;

        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {
            Integer item = new Integer( i );
            boolean oldFlag = oldState.get( i );
            boolean newFlag = newState.get( i );
            if ( ! oldFlag && newFlag ) {
                assert ! orderedSelection_.contains( item );
                orderedSelection_.add( item );
            }
            else if ( oldFlag && ! newFlag ) {
                assert orderedSelection_.contains( item );
                orderedSelection_.remove( item );
            }
            else {
                assert oldFlag == newFlag;
            }
        }
    }

    /**
     * Returns a BitSet which represents the current state of the
     * selection model.
     *
     * @param  flag vector with a flag set for every selected item
     */
    private BitSet getModelState() {
        BitSet state = new BitSet();
        int lo = model_.getMinSelectionIndex();
        int hi = model_.getMaxSelectionIndex();
        if ( lo < 0 ) {
            assert hi < 0;
            // no selections
        }
        else {
            for ( int i = lo; i <= hi; i++ ) {
                state.set( i, model_.isSelectedIndex( i ) );
            }
        }
        return state;
    }
}
