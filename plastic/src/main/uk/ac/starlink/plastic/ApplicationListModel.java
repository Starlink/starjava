package uk.ac.starlink.plastic;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

/**
 * ListModel implementation which tracks the applications 
 * currently registered with a Plastic hub.
 * Use the thread-safe {@link #register} and {@link #unregister}
 * methods to add and remove applications to the list.
 *
 * <p>All the elements of this list are {@link ApplicationItem} objects.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2006
 */
class ApplicationListModel extends AbstractListModel {

    private List appList_;
    private Map nameMap_;

    /**
     * Constructs a new model with no entries.
     */
    public ApplicationListModel() {
        this( null );
    }

    /**
     * Constructs a new model and initialises it with an array of entries.
     *
     * @param  items  initial population of registered applications
     */
    public ApplicationListModel( ApplicationItem[] items ) {
        setItems( items );
    }

    /**
     * Sets the contents of this list explicitly.  
     * This notifies listeners directly, so after construction
     * it should only be called from the event dispatch thread.
     *
     * @param  items  new list contents
     */
    public void setItems( ApplicationItem[] items ) {
        nameMap_ = new HashMap();
        appList_ = items == null ? new ArrayList()
                                 : new ArrayList( Arrays.asList( items ) );
        for ( Iterator it = appList_.iterator(); it.hasNext(); ) {
            retag( (ApplicationItem) it.next() );
        }
        fireContentsChanged( this, -1, -1 );
    }

    /**
     * Adds a new application to this list.
     * This method may be called from any thread; it updates the model
     * and fires an event asynchronously.
     *
     * @param  id    registered URI
     * @param  name  application name
     * @param  supportedMessages  list of URIs giving the messages that the
     *         registering application supports
     */
    public void register( URI id, String name, List supportedMessages ) {
        final ApplicationItem item =
            new ApplicationItem( id, name, supportedMessages );
        retag( item );
        if ( ! appList_.contains( item ) ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ! appList_.contains( item ) ) {
                        appList_.add( item );
                        int index = appList_.size() - 1;
                        fireIntervalAdded( this, index, index );
                    }
                }
            } );
        }
    }

    /**
     * Removes an application from this list.
     * This method may be called from any thread; it updates the model
     * and fires an event asynchronously.
     *
     * @param  id  registration URI of the application to be unregistered
     */
    public void unregister( final URI id ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                int nItem = appList_.size();
                for ( int i = 0; i < nItem; i++ ) {
                    ApplicationItem item = (ApplicationItem) appList_.get( i );
                    if ( id.equals( item.getId() ) ) {
                        appList_.remove( item );
                        fireIntervalRemoved( this, i, i );
                        return;
                    }
                }
            }
        } );
    }

    /**
     * Removes all applications from this list.
     * This method may be called from any thread; it updates the model
     * and fires an event asynchronously.
     */
    public void clear() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( ! appList_.isEmpty() ) {
                    int nItem = appList_.size();
                    appList_.clear();
                    fireIntervalRemoved( this, 0, nItem - 1 );
                }
            }
        } );
    }

    /**
     * Returns an element of this list.  All such returned elements will
     * be of type {@link ApplicationItem}.
     *
     * @param  index  list index
     * @return   <code>ApplicationItem</code> object
     */
    public Object getElementAt( int index ) {
        return (ApplicationItem) appList_.get( index );
    }

    public int getSize() {
        return appList_.size();
    }

    /**
     * Gives the item in question a possibly modified tag.  This is intended
     * to be a unique and human-readable name.  Its {@link #setTag} method
     * may be called if this model thinks it should be.
     *
     * @param   item   application item
     */
    private void retag( ApplicationItem item ) {
        String name = item.getName();
        URI id = item.getId();
        if ( ! nameMap_.containsKey( name ) ) {
            nameMap_.put( name, new ArrayList() );
        }
        List nameIdList = (List) nameMap_.get( name );
        int index = nameIdList.indexOf( id );
        if ( index < 0 ) {
            index = nameIdList.size();
            nameIdList.add( id );
        }
        item.setTag( index == 0 ? name : ( name + "-" + index ) );
    }
}
