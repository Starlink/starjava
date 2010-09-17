package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.vo.ConeSearchDialog;

/**
 * ConeSearchDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatConeSearchDialog extends ConeSearchDialog {
    private final RegistryDialogAdjuster adjuster_;
    public TopcatConeSearchDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "cone" );
    }
    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.addInteropMenu();
        return comp;
    }
}
