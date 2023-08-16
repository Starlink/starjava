package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ListModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.geom.SkyAspect;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SkySys;

/**
 * Layer plot window for sky coordinates.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class SkyPlotWindow
       extends StackPlotWindow<SkySurfaceFactory.Profile,SkyAspect> {
    private static final SkyPlotType PLOT_TYPE = SkyPlotType.getInstance();
    private static final ConfigKey<SkySys> DATASYS_KEY =
        SkySurfaceFactory.DATASYS_KEY;

    /**
     * Constructor.
     *
     * @param  parent   parent component
     * @param  tablesModel  list of available tables
     */
    public SkyPlotWindow( Component parent,
                          ListModel<TopcatModel> tablesModel ) {
        super( "Sky Plot", parent, PLOT_TYPE, new SkyPlotTypeGui(),
               tablesModel );
        getToolBar().addSeparator();
        addHelp( "SkyPlotWindow" );
    }

    /**
     * Defines GUI features specific to sky plot.
     */
    private static class SkyPlotTypeGui
            implements PlotTypeGui<SkySurfaceFactory.Profile,SkyAspect> {
        SkyAxisController axisController_;

        /**
         * Constructor.
         */
        SkyPlotTypeGui() {
            axisController_ = new SkyAxisController();
        }

        public AxisController<SkySurfaceFactory.Profile,SkyAspect>
                createAxisController() {
            return axisController_;
        }

        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return new SkyPositionCoordPanel( npos ) {
                SkySys getViewSystem() {
                    return axisController_.getViewSystem();
                }
            };
        }

        public PositionCoordPanel createAreaCoordPanel() {
            return new AreaCoordPanel( AreaCoord.SKY_COORD, new Coord[ 0 ],
                                       new ConfigKey<?>[] { DATASYS_KEY } ) {
                final Specifier<SkySys> dataSysSpecifier_;
                /* Constructor. */ {
                    dataSysSpecifier_ = 
                        getConfigSpecifier().getSpecifier( DATASYS_KEY );
                }
                public DataGeom getDataGeom() {
                    return SkyDataGeom
                          .createGeom( dataSysSpecifier_.getSpecifiedValue(),
                                       axisController_.getViewSystem() );
                }
            };
        }

        public boolean hasPositions() {
            return true;
        }

        public FigureMode[] getFigureModes() {
            return SkyFigureMode.MODES;
        }

        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        } 

        public CartesianRanger getCartesianRanger() {
            return null;
        }

        public String getNavigatorHelpId() {
            return "skyNavigation";
        }
    }

    /**
     * Position coordinate entry panel for sky plot.
     * This contains a SkySys selector which allows the user to select
     * which sky system the input data are in.  Note this works with,
     * but is not the same as, the sky system into which the coordinate data
     * is projected (set in the axis controller).
     */
    private static abstract class SkyPositionCoordPanel
            extends PositionCoordPanel {

        private final int npos_;
        private final Specifier<SkySys> dataSysSpecifier_;

        private static final String[] LONLAT = new String[] { "lon", "lat" };
        private static final String[] RADEC = new String[] { "ra", "dec" };
        private static final String[] RAJDEJ = new String[] { "raj", "dej" };
        private static final CoordSpotter[] LONLAT_SPOTTERS = {
            CoordSpotter.createUcdSpotter( "pos.eq", RADEC, false ),
            CoordSpotter.createUcdSpotter( "pos.eq", RADEC, true ),
            CoordSpotter.createUcdSpotter( "pos.ecliptic", LONLAT, false ),
            CoordSpotter.createUcdSpotter( "pos.ecliptic", LONLAT, true ),
            CoordSpotter.createUcdSpotter( "pos.galactic", LONLAT, false ),
            CoordSpotter.createUcdSpotter( "pos.galactic", LONLAT, true ),
            CoordSpotter.createUcdSpotter( "pos.bodyrc", LONLAT, false ),
            CoordSpotter.createUcdSpotter( "pos.bodyrc", LONLAT, true ),
            CoordSpotter.createUcdSpotter( "pos.earth", LONLAT, false ),
            CoordSpotter.createUcdSpotter( "pos.earth", LONLAT, true ),
            CoordSpotter.createNamePrefixSpotter( LONLAT, true ),
            CoordSpotter.createNamePrefixSpotter( LONLAT, false ),
            CoordSpotter.createNamePrefixSpotter( RADEC, true ),
            CoordSpotter.createNamePrefixSpotter( RADEC, false ),
            CoordSpotter.createNamePrefixSpotter( RAJDEJ, true ),
        };

        /**
         * Constructor.
         *
         * @param   npos  number of groups of positional coordinates for entry
         */
        SkyPositionCoordPanel( int npos ) {
            super( multiplyCoords( SkyDataGeom.createGeom( null, null )
                                              .getPosCoords(), npos ),
                   new ConfigKey<?>[] { DATASYS_KEY } );
            npos_ = npos;
            dataSysSpecifier_ =
                getConfigSpecifier().getSpecifier( DATASYS_KEY );
        }

        /**
         * Must be implemented by concrete subclass to provide the sky system
         * into which the data will be projected.
         *
         * @return  view sky system
         */
        abstract SkySys getViewSystem();

        /**
         * Returns the sky system in which the input coordinate lon/lat
         * values are supplied.
         *
         * @return  data sky system
         */
        SkySys getDataSystem() {
            return dataSysSpecifier_.getSpecifiedValue();
        }

        public DataGeom getDataGeom() {
            return SkyDataGeom.createGeom( getDataSystem(), getViewSystem() );
        }

        @Override
        public void autoPopulate() {

            /* Override the default autoPopulate behaviour, which won't
             * work well since we have coordinates with multiple components. */

            /* Do some special handling for the (most common)
             * single-position case: try to work out the data sky system
             * as well as picking suitable columns. */
            if ( npos_ == 1 ) {
                ColumnDataComboBoxModel lonModel = getColumnSelector( 0, 0 );
                ColumnDataComboBoxModel latModel = getColumnSelector( 0, 1 );
                SkySys currentSys = dataSysSpecifier_.getSpecifiedValue();
                SkySys sys = new ColPopulator( lonModel, latModel )
                            .attemptPopulate1( currentSys );
                if ( sys != null && sys != currentSys ) {
                    dataSysSpecifier_.setSpecifiedValue( sys );
                }
            }

            /* Otherwise, just look for matched groups of lon/lat coords.
             * This is less effort, but still better than nothing. */
            else {
                ValueInfo[] lonlatInfos =
                    CoordSpotter
                   .findCoordGroups( npos_,
                                     getInfos( getColumnSelector( 0, 0 ) ),
                                     LONLAT_SPOTTERS );
                if ( lonlatInfos != null ) {
                    for ( int ipos = 0; ipos < npos_; ipos++ ) {
                        populate( getColumnSelector( ipos, 0 ),
                                  lonlatInfos[ ipos * 2 + 0 ] );
                        populate( getColumnSelector( ipos, 1 ),
                                  lonlatInfos[ ipos * 2 + 1 ] );
                    }
                }
                else if ( npos_ == 4 ) {
                    attemptPopulateEpn4();
                }
            }
        }

        /**
         * Tries to find 4 lon/lat bounding box coordinate pairs
         * in a way that will work for EPN-TAP tables.
         * The implementation is hacky and looks at EPN-TAP-defined
         * column names. 
         * Really I should be looking for UCDs with ;stat.min/;stat.max
         * modifiers, but I expect that the large majority of cases
         * where information in this form is available will be from
         * EPN-TAP tables with these known column names, so just do it
         * the dumb way for now until it becomes clear that it's
         * worth the effort to be smarter.
         */
        private boolean attemptPopulateEpn4() {
            ValueInfo[] infos = CoordPanel.getInfos( getColumnSelector( 0, 0 ));
            ValueInfo c1minInfo = null;
            ValueInfo c1maxInfo = null;
            ValueInfo c2minInfo = null;
            ValueInfo c2maxInfo = null;
            for ( ValueInfo info : infos ) {
                String name = info.getName();
                if ( "c1min".equals( name ) ) {
                    c1minInfo = info;
                }
                else if ( "c1max".equals( name ) ) {
                    c1maxInfo = info;
                }
                else if ( "c2min".equals( name ) ) {
                    c2minInfo = info;
                }
                else if ( "c2max".equals( name ) ) {
                    c2maxInfo = info;
                }
            }
            if ( c1minInfo != null && c1maxInfo != null &&
                 c2minInfo != null && c2maxInfo != null ) {
                boolean success =
                    populate( getColumnSelector( 0, 0 ), c1minInfo ) &
                    populate( getColumnSelector( 0, 1 ), c2minInfo ) &
                    populate( getColumnSelector( 1, 0 ), c1maxInfo ) &
                    populate( getColumnSelector( 1, 1 ), c2minInfo ) &
                    populate( getColumnSelector( 2, 0 ), c1maxInfo ) &
                    populate( getColumnSelector( 2, 1 ), c2maxInfo ) &
                    populate( getColumnSelector( 3, 0 ), c1minInfo ) &
                    populate( getColumnSelector( 3, 1 ), c2maxInfo );
                assert success;
                return success;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Attempts to fill in lon/lat coordinate values in a position entry
     * panel according to current constraints.
     */
    private static class ColPopulator {
        final ColumnDataComboBoxModel lonModel_;
        final ColumnDataComboBoxModel latModel_;
        final ValueInfo[] infos_;

        /** 
         * Constructor.
         *
         * @param   lonModel   longitude column selection model
         * @param   latModel   latitude column selection model
         */
        ColPopulator( ColumnDataComboBoxModel lonModel,
                      ColumnDataComboBoxModel latModel ) {
            lonModel_ = lonModel;
            latModel_ = latModel;

            /* We expect that the models are selectors on the same list
             * of table columns.  If they are not, some of the implementation
             * assumptions of this class will fail. */
            assert Arrays
                  .equals( getInfoNames( CoordPanel.getInfos( lonModel ) ),
                           getInfoNames( CoordPanel.getInfos( latModel ) ) );
            infos_ = CoordPanel.getInfos( lonModel );
        }

        /**
         * Attempts to configure selections for the lon/lat column selection
         * models owned by this object appropriate to a given sky system.
         * If that fails, try to configure anytyhing that looks like a
         * matching lon/lat pair.
         *
         * @param  preferredSys   sky system
         * @return  sky system for which population was successfully performed,
         *          or null
         */
        public SkySys attemptPopulate1( SkySys preferredSys ) {

            /* Get a list of known sky systems in order of preference;
             * use the order it comes in with the preferred one inserted
             * at the head. */
            List<SkySys> systems =
                new ArrayList<SkySys>( Arrays.asList(
                                           SkySys.getKnownSystems( false ) ) );
            systems.remove( preferredSys );
            systems.add( 0, preferredSys );

            /* Try to populate for each specific system in turn,
             * returning on success. */
            for ( SkySys sys : systems ) {
                int[] pair = sys.getCoordPair( infos_ );
                if ( pair != null ) {
                    if ( CoordPanel.populate( lonModel_,
                                              infos_[ pair[ 0 ] ] ) &&
                         CoordPanel.populate( latModel_,
                                              infos_[ pair[ 1 ] ] ) ) {
                        return sys;
                    }
                    else {
                        assert false;
                    }
                }
            }

            /* If no success, try EPN-TAP convention. */
            ColumnData[] epnCoords = getEpnCoords1();
            if ( epnCoords != null ) {
                lonModel_.setSelectedItem( epnCoords[ 0 ] );
                latModel_.setSelectedItem( epnCoords[ 1 ] );
                return null;
            }
            return null;
        }

        /**
         * Attempts to return a lon/lat coordinate pair corresponding
         * to the sky/planetary coordinates as per the EPN-TAP convention:
         * the c1min, c1max, c2min and c2max columns give the lon/lat
         * bounding box.
         *
         * @return  pair of lon/lat coordinate values corresponding to EPN-TAP
         *          position, or null if they are not available
         */
        public ColumnData[] getEpnCoords1() {

            /* See whether the required columns are present. */
            List<String> epnReqs =
                    new ArrayList<>( Arrays.asList( new String[] {
                "c1min", "c1max", "c2min", "c2max",
            } ) );
            for ( ValueInfo info : infos_ ) {
                epnReqs.remove( info.getName() );
            }

            /* If so, attempt to return columns with expressions that
             * correspond to the midpoint of the lon/lat bounding box. */
            if ( epnReqs.size() == 0 ) {
                try {
                    return new ColumnData[] {
                        lonModel_.stringToColumnData( "midLon(c1min, c1max)" ),
                        latModel_.stringToColumnData( "midLat(c2min, c2max)" ),
                    };
                }
                catch ( CompilationException e ) {
                    assert false;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /**
         * Returns a list of column names for a given list of colum metadata
         * items.
         *
         * @param  infos   valueinfos
         * @return   names of infos
         */
        private static String[] getInfoNames( ValueInfo[] infos ) {
            int ninfo = infos.length;
            String[] names = new String[ ninfo ];
            for ( int i = 0; i < ninfo; i++ ) {
                names[ i ] = infos[ i ] == null ? null : infos[ i ].getName();
            }
            return names;
        }
    }
}
