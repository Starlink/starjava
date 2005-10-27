package uk.ac.starlink.topcat;

import java.util.EventListener;

/**
 * Listener interface for objects that want to notice changes in a 
 * {@link TopcatModel}.  
 * <p>
 * Note that TopcatModel is currently a mixed bag of existing models
 * together with its own data.  I'm not sure whether it makes sense to
 * amalgamate these at some point.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Feb 2004
 */
public interface TopcatListener extends EventListener {

    /**
     * Invoked when the model has changed in some way.
     * 
     * @param  evt  event description
     */
    void modelChanged( TopcatEvent evt );
}
