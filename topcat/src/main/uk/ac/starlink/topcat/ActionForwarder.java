package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * ActionListener implementation which forwards all ActionEvents to a list
 * of clients.
 * It also implements ChangeListener, and any ChangeEvents it receives
 * will be turned into ActionEvents and forwarded as well.
 *
 * @author   Mark Taylor 
 * @since    28 Oct 2005
 */
public class ActionForwarder implements ActionListener, ChangeListener {

    private final List listeners_ = new ArrayList();

    /**
     * Adds a new listener to the list of forwardees.
     *
     * @param  listener   listener to add
     */
    public void addListener( ActionListener listener ) {
        listeners_.add( listener );
    }

    /**
     * Removes a listener which was previously added.
     *
     * @param   listener   listener to remove
     * @see     #addListener
     */
    public void removeListener( ActionListener listener ) {
        listeners_.remove( listener );
    }

    public void actionPerformed( ActionEvent evt ) {
        for ( Iterator it = listeners_.iterator(); it.hasNext(); ) {
            ((ActionListener) it.next()).actionPerformed( evt );
        }
    }

    public void stateChanged( ChangeEvent evt ) {
        actionPerformed( new ActionEvent( evt.getSource(), 0, "Change" ) );
    }
}
