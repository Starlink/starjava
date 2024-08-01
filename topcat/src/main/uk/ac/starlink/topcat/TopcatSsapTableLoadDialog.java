package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.vo.SsapTableLoadDialog;

/**
 * SsapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatSsapTableLoadDialog extends SsapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;
    @SuppressWarnings("this-escape")
    public TopcatSsapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "ssap", true );
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
