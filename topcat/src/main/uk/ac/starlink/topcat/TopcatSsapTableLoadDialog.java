package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.vo.SsapTableLoadDialog2;

/**
 * SsapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatSsapTableLoadDialog extends SsapTableLoadDialog2 {
    private final RegistryDialogAdjuster adjuster_;
    public TopcatSsapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "ssap" );
    }
    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.addInteropMenu();
        return comp;
    }
}
