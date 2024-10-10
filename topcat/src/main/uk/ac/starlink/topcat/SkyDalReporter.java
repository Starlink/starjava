package uk.ac.starlink.topcat;

import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.SettingGroup;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableCone;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.vo.SkyDalTableLoadDialog;
import uk.ac.starlink.vo.SkyPositionEntry;

/**
 * StiltsReporter implementation for cone-search-like windows.
 *
 * @author   Mark Taylor
 * @since    22 Oct 2024
 */
public class SkyDalReporter implements StiltsReporter {

    private final SkyDalTableLoadDialog dialog_;
    private final Supplier<ConeServiceType> serviceTypeSupplier_;
    private final DoubleSupplier radiusSupplier_;
    private final Function<TableCone,Setting[]> extraSettingsFunc_;
    private final ActionForwarder forwarder_;

    /**
     * Constructor.
     *
     * @param   dialog  cone-like load dialogue on behalf of which
     *                  this will be reporting
     * @param   servTypeSupplier  provides positional service identifier
     * @param   radiusSupplier  provides search radius
     * @param   extraSettingsFunc   provides service-specific settings
     */
    public SkyDalReporter( SkyDalTableLoadDialog dialog,
                           Supplier<ConeServiceType> servTypeSupplier,
                           DoubleSupplier radiusSupplier,
                           Function<TableCone,Setting[]> extraSettingsFunc ) {
        dialog_ = dialog;
        serviceTypeSupplier_ = servTypeSupplier;
        radiusSupplier_ = radiusSupplier;
        extraSettingsFunc_ = extraSettingsFunc;
        forwarder_ = new ActionForwarder();
    }

    public void addStiltsListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    public void removeStiltsListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public StiltsCommand createStiltsCommand( TopcatTableNamer tnamer ) {
        TableCone task = new TableCone();
        List<SettingGroup> groups = new ArrayList<>();
        String serviceUrlTxt = dialog_.getServiceUrl();
        if ( serviceUrlTxt == null ) {
            return null;
        }
        URL serviceUrl;
        try {
            serviceUrl = URLUtils.newURL( serviceUrlTxt );
        }
        catch ( MalformedURLException e ) {
            return null;
        }
        groups.add( new SettingGroup( 1, new Setting[] {
            pset( task.getServiceUrlParameter(), serviceUrl ),
        } ) );
        groups.add( new SettingGroup( 1, new Setting[] {
            pset( task.getServiceTypeParameter(),
                  serviceTypeSupplier_.get() ),
        } ) );
        SkyPositionEntry skyEntry = dialog_.getSkyEntry();
        final double raDeg;
        final double decDeg;
        try {
            raDeg = skyEntry.getRaDegreesField().getValue();
            decDeg = skyEntry.getDecDegreesField().getValue();
        }
        catch ( RuntimeException e ) {
            return null;
        }
        double radiusDeg = radiusSupplier_.getAsDouble();
        if ( Double.isNaN( raDeg ) || Double.isNaN( decDeg ) ||
             Double.isNaN( radiusDeg ) ) {
            return null;
        }
        groups.add( new SettingGroup( 1, new Setting[] {
            pset( task.getLongitudeParameter(), Double.valueOf( raDeg ) ),
            pset( task.getLatitudeParameter(), Double.valueOf( decDeg ) ),
            pset( task.getRadiusDegParameter(), Double.valueOf( radiusDeg ) ),
            new Setting( task.getSkySystemParameter().getName(),
                         SkySystem.ICRS.toString(), null ),
        } ) );
        Setting[] extraSettings = extraSettingsFunc_.apply( task );
        if ( extraSettings.length > 0 ) {
            groups.add( new SettingGroup( 1, extraSettings ) );
        }
        return StiltsCommand
              .createCommand( task, groups.toArray( new SettingGroup[ 0 ] ) );
    }

    /**
     * Returns a forwarder for listening to dialogue updates when
     * stilts state may have changed.
     *
     * @return  action forwarder
     */
    public ActionForwarder getActionForwarder() {
        return forwarder_;
    }

    /**
     * Creates a StiltsAction associated with this reporter.
     *
     * @return  new action
     */
    public StiltsAction createStiltsAction() {
        return
            new StiltsAction( this,
                              () -> SwingUtilities
                                   .getWindowAncestor( dialog_
                                                      .getQueryComponent() ) );
    }
}
