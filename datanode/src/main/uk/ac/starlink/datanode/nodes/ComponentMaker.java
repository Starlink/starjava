package uk.ac.starlink.datanode.nodes;

import javax.swing.JComponent;

/**
 * An interface implemented by objects which can make a Component for
 * display on demand.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface ComponentMaker {

    /**
     * Return a Component.  It is only expected that this method will be
     * called once for each instance of this class.
     *
     * @return  the Component
     * @throws  Exception  any exception which is thrown will be caught
     *                     something sensible will be done with it - 
     *                     typically a different component displaying the
     *                     exception message will be displayed instead
     */
    public JComponent getComponent() throws Exception;
}
