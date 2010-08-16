package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JDialog;
import uk.ac.starlink.vo.SsapTableLoadDialog;

/**
 * SsapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class SsapTableLoadDialog2 extends SsapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;
    public SsapTableLoadDialog2() {
        adjuster_ = new RegistryDialogAdjuster( this, "ssap" );
    }
    public JDialog createDialog( Component parent ) {
        JDialog dialog = super.createDialog( parent );
        dialog.getJMenuBar().add( adjuster_.createInteropMenu() );
        return dialog;
    }
}
