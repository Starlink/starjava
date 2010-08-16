package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JDialog;
import uk.ac.starlink.vo.SiapTableLoadDialog;

/**
 * SiapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class SiapTableLoadDialog2 extends SiapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;
    public SiapTableLoadDialog2() {
        adjuster_ = new RegistryDialogAdjuster( this, "siap" );
    }
    public JDialog createDialog( Component parent ) {
        JDialog dialog = super.createDialog( parent );
        dialog.getJMenuBar().add( adjuster_.createInteropMenu() );
        return dialog;
    }
}
