package uk.ac.starlink.topcat;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * ListModel implementation that represents the concatenation of
 * two supplied constituent ListModels.
 *
 * @author   Mark Taylor
 * @since    10 May 2018
 */
public class ListModel2 extends AbstractListModel {

    private final ListModel model1_;
    private final ListModel model2_;

    /**
     * Constructor.
     *
     * @param  model1  first constituent model
     * @param  model2  second constituend model
     */
    public ListModel2( ListModel model1, ListModel model2 ) {
        model1_ = model1;
        model2_ = model2;
        model1.addListDataListener( new OffsetListDataListener() {
            protected int getOffset() {
                return 0;
            }
        } );
        model2_.addListDataListener( new OffsetListDataListener() {
            protected int getOffset() {
                return model1_.getSize();
            }
        } );
    }

    /**
     * Returns the model providing the first run of entries.
     *
     * @return   model 1
     */
    public ListModel getModel1() {
        return model1_;
    }

    /**
     * Returns the model providing the second run of entries.
     *
     * @return  model 2
     */
    public ListModel getModel2() {
        return model2_;
    }

    public Object getElementAt( int ix ) {
        int ix2 = ix - model1_.getSize();
        return ix2 >= 0 ? model2_.getElementAt( ix2 )
                        : model1_.getElementAt( ix );
    }

    public int getSize() {
        return model1_.getSize() + model2_.getSize();
    }

    /**
     * Partial ListDataListener implementation that forwards events to
     * this ListModel2's listeners, adjusting the event element indices
     * by an offset supplied by the concrete implementation.
     */
    private abstract class OffsetListDataListener implements ListDataListener {
        public void contentsChanged( ListDataEvent evt ) {
            fireContentsChanged( evt.getSource(),
                                 adjustIndex( evt.getIndex0() ),
                                 adjustIndex( evt.getIndex1() ) );
        }
        public void intervalAdded( ListDataEvent evt ) {
            fireIntervalAdded( evt.getSource(),
                               adjustIndex( evt.getIndex0() ),
                               adjustIndex( evt.getIndex1() ) );
        }
        public void intervalRemoved( ListDataEvent evt ) {
            fireIntervalRemoved( evt.getSource(),
                                 adjustIndex( evt.getIndex0() ),
                                 adjustIndex( evt.getIndex1() ) );
        }
        /**
         * Adjusts the received event index appropriately for listeners
         * to this ListModel2.
         *
         * @param  ix  constituent model element index
         * @return   combined model element index
         */
        private int adjustIndex( int ix ) {
            return ix >= 0 ? ix + getOffset() : ix;
        }

        /**
         * Returns the offset required to convert element indices from
         * consituent model to combined model values.
         *
         * @return  constituent model base index
         */
        protected abstract int getOffset();
    }
}
