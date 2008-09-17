package uk.ac.starlink.topcat.interop;

import javax.swing.ComboBoxModel;

/**
 * Superinterface for objects which can direct interoperability communications
 * to one or more target applications based on the selection in a 
 * combo box model.
 * 
 * @author   Mark Taylor
 * @since    17 Sep 2008
 */
public interface Activity {

    /**
     * Returns a ComboBoxModel which allows selection of target applications.
     *
     * @return   target application selection model
     */
    ComboBoxModel getTargetSelector();
}
