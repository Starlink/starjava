package uk.ac.starlink.vo;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

/**
 * Model for RegistrySelector.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2014
 */
public class RegistrySelectorModel {

    private final RegistryProtocol proto_;
    private final ComboBoxModel selectionModel_;

    /**
     * Constructor.
     *
     * @param  proto  registry access protocol
     */
    public RegistrySelectorModel( RegistryProtocol proto ) {
        proto_ = proto;
        selectionModel_ =
            new DefaultComboBoxModel( proto.getDefaultRegistryUrls() );
        selectionModel_.setSelectedItem( selectionModel_.getElementAt( 0 ) );
    }

    /**
     * Returns the registry access protocol.
     *
     * @return  protocol
     */
    public RegistryProtocol getProtocol() {
        return proto_;
    }

    /**
     * Returns the model used for selection of the registry endpoint URL.
     *
     * @return  selection model; elements will be strings
     */
    public ComboBoxModel getUrlSelectionModel() {
        return selectionModel_;
    }
}
