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
    @SuppressWarnings("this-escape")
    public TopcatSiapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "siap", true );
    }
    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.adjustComponent();
        return comp;
    }
    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        return adjuster_.acceptResourceIdLists()
            && super.acceptResourceIdList( ivoids, msg );
    }
    public boolean acceptSkyPosition( double raDegrees, double decDegrees ) {
        return adjuster_.acceptSkyPositions()
            && super.acceptSkyPosition( raDegrees, decDegrees );
    }
}
