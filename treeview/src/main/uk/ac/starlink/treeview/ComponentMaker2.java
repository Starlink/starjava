package uk.ac.starlink.treeview;

import javax.swing.JComponent;

/**
 * An interface implemented by objects which can make a pair of Components
 * for display on demand.
 * The two will be displayed one above the other; the first should be
 * small and will be displayed at the top of a container; the second
 * may be large and will be displayed within a scrolling pane below it.
 * the 
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface ComponentMaker2 {

    /**
     * Return a pair of Components.  It is only expected that this method 
     * will be called once for each instance of this class.
     *
     * @return  a 2-element array giving the Components.  The first element
     *          should be a small one for constant display at the top;
     *          the second element will be displayed within scrollbars
     *          if necessary
     * @throws  Exception  any exception which is thrown will be caught
     *                     something sensible will be done with it - 
     *                     typically a different component displaying the
     *                     exception message will be displayed instead
     */
    public JComponent[] getComponents() throws Exception;

}
