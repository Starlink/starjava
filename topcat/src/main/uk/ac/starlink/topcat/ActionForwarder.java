package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * ActionListener implementation which forwards all ActionEvents to a list
 * of clients.
 * It also implements some other listener interfaces,
 * currently <code>ChangeListener</code> and <code>ListDataListener</code>.
 * Any events received throught those interfaces will be adapted into
 * ActionEvents and forwarded as well.
 *
 * @author   Mark Taylor 
 * @since    28 Oct 2005
 */
public class ActionForwarder
             implements ActionListener, ChangeListener, ListDataListener {

    private final List listeners_ = new ArrayList();

    /**
     * Adds a new listener to the list of forwardees.
     *
     * @param  listener   listener to add
     */
    public void addActionListener( ActionListener listener ) {
        listeners_.add( listener );
    }

    /**
     * Removes a listener which was previously added.
     *
     * @param   listener   listener to remove
     * @see     #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
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

    public void contentsChanged( ListDataEvent evt ) {
        actionPerformed( new ActionEvent( evt.getSource(), 0, "List" ) );
    }

    public void intervalAdded( ListDataEvent evt ) {
        actionPerformed( new ActionEvent( evt.getSource(), 1, "List" ) );
    }

    public void intervalRemoved( ListDataEvent evt ) {
        actionPerformed( new ActionEvent( evt.getSource(), 2, "List" ) );
    }
}
