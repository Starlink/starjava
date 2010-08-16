package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JDialog;
import uk.ac.starlink.vo.ConeSearchDialog;

/**
 * ConeSearchDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class ConeSearchDialog2 extends ConeSearchDialog {
    private final RegistryDialogAdjuster adjuster_;
    public ConeSearchDialog2() {
        adjuster_ = new RegistryDialogAdjuster( this, "cone" );
    }
    public JDialog createDialog( Component parent ) {
        JDialog dialog = super.createDialog( parent );
        dialog.getJMenuBar().add( adjuster_.createInteropMenu() );
        return dialog;
    }
}
