package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * Convenience class extending AbstractAction.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class BasicAction extends AbstractAction {

    public BasicAction( String name, String shortdesc ) {
        this( name, null, shortdesc );
    }

    public BasicAction( String name, Icon icon, String shortdesc ) {
        super( name, icon );
        putValue( SHORT_DESCRIPTION, shortdesc );
    }

    /**
     * Returns the window from which this action was invoked.
     * This is currently the Frame in which the event originated.
     *
     * @param   evt  event to check the window for
     * @return  window in which evt originated if it can be determined -
     *          may be null
     */
    public Component getEventWindow( ActionEvent evt ) {
        if ( evt != null ) {
            for ( Object comp = evt.getSource();
                  comp instanceof Component;
                  comp = ((Component) comp).getParent() ) {
                if ( comp instanceof Frame ) {
                    return (Frame) comp;
                }
            }
        }
        return null;
    }
}
