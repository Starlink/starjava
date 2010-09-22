package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.vo.SiapTableLoadDialog;

/**
 * SiapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatSiapTableLoadDialog extends SiapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;
    public TopcatSiapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "siap" );
    }
    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.addInteropMenu();
        return comp;
    }
}
