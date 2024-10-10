package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableCone;
import uk.ac.starlink.vo.SsapTableLoadDialog;

/**
 * SsapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class TopcatSsapTableLoadDialog extends SsapTableLoadDialog {

    private final RegistryDialogAdjuster adjuster_;
    private final SkyDalReporter stiltsReporter_;

    @SuppressWarnings("this-escape")
    public TopcatSsapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "ssap", true );
        stiltsReporter_ =
            new SkyDalReporter( this, () -> ConeServiceType.SSA,
                                () -> getSearchDiameter() * 0.5,
                                this::getExtraSettings );
        addToolbarAction( stiltsReporter_.createStiltsAction() );
    }

    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        ActionForwarder forwarder = stiltsReporter_.getActionForwarder();
        getSkyEntry().addActionListener( forwarder );
        getServiceUrlField().addActionListener( forwarder );
        getFormatSelector().addActionListener( forwarder );
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
     * Returns SSA-specific settings for this dialogue.
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
