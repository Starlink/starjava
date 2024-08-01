package uk.ac.starlink.topcat.interop;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;

/**
 * Implements a ListModel based on an existing ListModel which is taken to
 * contain (PLASTIC) {@link uk.ac.starlink.plastic.ApplicationItem} objects,
 * and optionally the {@link #ALL_LISTENERS} string.
 * The purpose of this class is to give you the subset of the applications
 * which support a given PLASTIC message ID.
 * Applications which support all messages are included.
 *
 * <p>For convenience, this class implements ComboBoxModel as well, but 
 * you don't need to use the selection if you don't want.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2006
 */
public class SelectivePlasticListModel
        extends AbstractListModel<Object>
        implements ListDataListener, ComboBoxModel<Object> {

    private final ListModel<ApplicationItem> base_;
    private final URI messageId_;
    private List<Object> appList_;
    private boolean includeAll_;
    private HubManager excludeApp_;
    private Object selected_;
    public static final String ALL_LISTENERS = "All Listeners";

    /**
     * Constructs a new list model specifying whether certain particular
     * options are included.
     *
     * @param   base  base list model; should contain 
     *          {@link uk.ac.starlink.plastic.ApplicationItem}s
     * @param   messageId  PLASTIC message id to be supported by all the
     *          apps in this list
     * @param   includeAll  true iff the list should include an 
     *          "All Listeners" option
     * @param   excludeApp  plastic listener manager whose ID will be 
     *          excluded from the list (typically represents the 
     *          sender application)
     */
    @SuppressWarnings("this-escape")
    public SelectivePlasticListModel( ListModel<ApplicationItem> base,
                                      URI messageId, boolean includeAll,
                                      HubManager excludeApp ) {
        base_ = base;
        messageId_ = messageId;
        includeAll_ = includeAll;
        excludeApp_ = excludeApp;
        appList_ = new ArrayList<Object>();
        updateItems();
        base_.addListDataListener( this );
        if ( includeAll_ ) {
            selected_ = ALL_LISTENERS;
        }
    }

    /**
     * Updates state to match that of the base model.
     */
    private void updateItems() {
        List<Object> list = new ArrayList<>();

        /* Optionally include an entry representing a full broadcast. */
        if ( includeAll_ ) {
            list.add( ALL_LISTENERS );
        }

        /* Get an application ID to ignore maybe. */
        URI excludeId = excludeApp_ == null ? null
                                            : excludeApp_.getRegisteredId();

        /* Go through the base list assembling a list of acceptable items. */
        for ( int i = 0; i < base_.getSize(); i++ ) {
            Object obj = base_.getElementAt( i );
            if ( obj instanceof ApplicationItem ) {
                ApplicationItem app = (ApplicationItem) obj;
                if ( excludeId == null || ! excludeId.equals( app.getId() ) ) {
                    List<?> msgList = app.getSupportedMessages();
                    if ( msgList == null || msgList.size() == 0 ||
                         msgList.contains( messageId_ ) ) {
                        list.add( app );
                    }
                }
            }
        }

        /* If it's different from what we already have, install it and
         * notify listeners. */
        if ( ! list.equals( appList_ ) ) {
            appList_ = list;
            fireContentsChanged( this, -1, -1 );
        }
    }

    /*
     * ListModel implementation.
     */

    public int getSize() {
        return appList_.size();
    }

    public Object getElementAt( int i ) {
        return appList_.get( i );
    }

    /*
     * ComboBoxModel implementation.
     */

    public void setSelectedItem( Object item ) {
        selected_ = item;
    }

    public Object getSelectedItem() {
        return selected_;
    }

    /*
     * ListDataListener implementation.
     */

    public void contentsChanged( ListDataEvent evt ) {
        updateItems();
    }

    public void intervalAdded( ListDataEvent evt ) {
        updateItems();
    }

    public void intervalRemoved( ListDataEvent evt ) {
        updateItems();
    }
}
