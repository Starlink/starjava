package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * Convenience class extending AbstractAction.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class BasicAction extends AbstractAction {

    /**
     * Constructor without icon.
     *
     * @param  name  action name (NAME property)
     * @param  descrip   action description (SHORT_DESCRIPTION property),
     *                   used as tooltip text
     */
    public BasicAction( String name, String descrip ) {
        this( name, null, descrip );
    }

    /**
     * Constructor with icon.
     *
     * @param  name  action name (NAME property)
     * @param  icon  action icon (SMALL_ICON property)
     * @param  descrip   action description (SHORT_DESCRIPTION property),
     *                   used as tooltip text
     */
    public BasicAction( String name, Icon icon, String descrip ) {
        super( name, icon );
        putValue( SHORT_DESCRIPTION, descrip );
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

    /**
     * Convenience method to create an Action with a lambda to define the
     * actionPerformed behaviour.
     *
     * @param  name  action name (NAME property)
     * @param  icon  action icon (SMALL_ICON property)
     * @param  descrip   action description (SHORT_DESCRIPTION property),
     *                   used as tooltip text
     * @param  perform   provides <code>actionPerformed</code> behaviour
     * @return   new action
     */
    public static BasicAction create( String name, Icon icon, String descrip,
                                      Consumer<ActionEvent> perform ) {
        return new BasicAction( name, icon, descrip ) {
            public void actionPerformed( ActionEvent evt ) {
                perform.accept( evt );
            }
        };
    }
}
