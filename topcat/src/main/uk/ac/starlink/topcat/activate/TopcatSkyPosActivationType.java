package uk.ac.starlink.topcat.activate;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;

/**
 * Activation type that sends sky coordinates to other windows in the
 * TOPCAT application.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2018
 */
public class TopcatSkyPosActivationType implements ActivationType {

    private final ControlWindow controlWin_;

    /**
     * Constructor.
     */
    public TopcatSkyPosActivationType() {
        controlWin_ = ControlWindow.getInstance();
    }

    public String getName() {
        return "Use Sky Coordinates in TOPCAT";
    }

    public String getDescription() {
        return "Update other TOPCAT windows (e.g. Cone Search) "
             + "with sky coordinates";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasSkyCoords() ? Suitability.ACTIVE
                                         : Suitability.PRESENT;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SkyPosConfigurator( tinfo ) {
            public Activator createActivator( ColumnData raData,
                                              ColumnData decData ) {
                return new SkyPosActivator( raData, decData, true ) {
                    protected Outcome useSkyPos( double raDeg, double decDeg ) {
                        controlWin_.acceptSkyPosition( raDeg, decDeg );
                        return Outcome.success();
                    }
                };
            }
            public String getSkyConfigMessage() {
                return null;
            }
            public Safety getSafety() {
                return Safety.SAFE;
            }
            public ConfigState getState() {
                return getSkyPosState();
            }
            public void setState( ConfigState state ) {
                setSkyPosState( state );
            }
        };
    }
}
