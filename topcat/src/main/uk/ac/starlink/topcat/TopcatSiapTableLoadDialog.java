package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableCone;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.vo.SiapTableLoadDialog;

/**
 * SiapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatSiapTableLoadDialog extends SiapTableLoadDialog {

    private final RegistryDialogAdjuster adjuster_;
    private final SkyDalReporter stiltsReporter_;

    @SuppressWarnings("this-escape")
    public TopcatSiapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "siap", true );
        stiltsReporter_ =
            new SkyDalReporter( this, this::getSiaServiceType,
                                () -> getSizeDeg() * 0.5,
                                this::getExtraSettings );
        addToolbarAction( stiltsReporter_.createStiltsAction() );
    }

    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        ActionForwarder forwarder = stiltsReporter_.getActionForwarder();
        getSkyEntry().addActionListener( forwarder );
        getServiceUrlField().addActionListener( forwarder );
        getFormatSelector().addActionListener( forwarder );
        getVersionSelector().addActionListener( forwarder );
        adjuster_.adjustComponent();
        return comp;
    }

    @Override
    public TableLoader createTableLoader() {
        stiltsReporter_.getActionForwarder()
                       .actionPerformed( new ActionEvent( this, 0, "load" ) );
        return super.createTableLoader();
    }

    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        return adjuster_.acceptResourceIdLists()
            && super.acceptResourceIdList( ivoids, msg );
    }

    public boolean acceptSkyPosition( double raDegrees, double decDegrees ) {
        return adjuster_.acceptSkyPositions()
            && super.acceptSkyPosition( raDegrees, decDegrees );
    }

    /**
     * Returns the version-specific service ConeServiceType specified
     * by the current state of this dialogue.
     *
     * @return  service type instance
     */
    private ConeServiceType getSiaServiceType() {
        switch ( getSiaVersion() ) {
            case V10:
                return ConeServiceType.SIA1;
            case V20:
                return ConeServiceType.SIA2;
            default:
                assert false;
                return ConeServiceType.SIA1;
        }
    }

    /**
     * Returns SIA-specific settings for this dialogue.
     *
     * @param  task  stilts invocation task
     * @return  array of miscellaneous settings
     */
    private Setting[] getExtraSettings( TableCone task ) {
        return new Setting[] {
            stiltsReporter_.pset( task.getFormatParameter(),
                                  getFormat().toString() ),
        };
    }
}
