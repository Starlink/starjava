package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
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
        SkySys.createConfigKey( new ConfigMeta( "datasys", "Data Sky System" ),
                                false );

    /**
     * Constructor.
     *
     * @param  parent   parent component
     */
    public SkyPlotWindow( Component parent ) {
        super( "Sky2", parent, PLOT_TYPE, new SkyPlotTypeGui() );
    }

    /**
     * Defines GUI features specific to sky plot.
     */
    private static class SkyPlotTypeGui
            implements PlotTypeGui<SkySurfaceFactory.Profile,SkyAspect> {
        SkyAxisControl axisControl_;

        /**
         * Constructor.
         */
        SkyPlotTypeGui() {
            axisControl_ = new SkyAxisControl();
        }

        public AxisControl<SkySurfaceFactory.Profile,SkyAspect>
                createAxisControl( ControlStack stack ) {
            return axisControl_;
        }

        public PositionCoordPanel createPositionCoordPanel() {
            return new SkyPositionCoordPanel( true ) {
                SkySys getViewSystem() {
                    return axisControl_.getViewSystem();
                }
            };
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
            implements PositionCoordPanel {

        private final boolean autoPopulate_;
        private final JComponent panel_;
        private final CoordPanel basePanel_;
        private final Specifier<SkySys> dataSysSpecifier_;

        /**
         * Constructor.
         *
         * @param   autoPopulate  true if it should be filled in with
         *          coordinates from an available table when possible
         */
        SkyPositionCoordPanel( boolean autoPopulate ) {
            autoPopulate_ = autoPopulate;

            /* The basic functionality is just a sky panel for the sky
             * coordinates. */
            basePanel_ = new CoordPanel( SkyDataGeom.createGeom( null, null )
                                        .getPosCoords(), false );

            /* But add a data sky system selector. */
            ConfigSpecifier cspec =
                new ConfigSpecifier( new ConfigKey[] { DATASYS_KEY } );
            dataSysSpecifier_ = cspec.getSpecifier( DATASYS_KEY );
            dataSysSpecifier_.addActionListener( basePanel_
                                                .getActionForwarder() );
            panel_ = Box.createVerticalBox();
            panel_.add( cspec.getComponent() );
            panel_.add( Box.createVerticalStrut( 5 ) );
            panel_.add( new LineBox( basePanel_ ) );
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

        public JComponent getComponent() {
            return panel_;
        }

        public void setTable( TopcatModel table ) {
            basePanel_.setTable( table );
            if ( autoPopulate_ ) {
                ColumnDataComboBoxModel lonModel =
                    basePanel_.getColumnSelector( 0, 0 );
                ColumnDataComboBoxModel latModel =
                    basePanel_.getColumnSelector( 0, 1 );
                ColPopulator cp = new ColPopulator( lonModel, latModel );
                SkySys currentSys = dataSysSpecifier_.getSpecifiedValue();
                SkySys sys = new ColPopulator( lonModel, latModel )
                            .attemptPopulate( currentSys );
                if ( sys != null && sys != currentSys ) {
                    dataSysSpecifier_.setSpecifiedValue( sys );
                }
            }
        }

        public GuiCoordContent[] getContents() {
            return basePanel_.getContents();
        }

        public void addActionListener( ActionListener listener ) {
            basePanel_.addActionListener( listener );
        }

        public void removeActionListener( ActionListener listener ) {
            basePanel_.removeActionListener( listener );
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
            assert Arrays.equals( getInfoNames( getInfos( lonModel ) ),
                                  getInfoNames( getInfos( latModel ) ) );
            infos_ = getInfos( lonModel );
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
        public SkySys attemptPopulate( SkySys preferredSys ) {

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
                    if ( populate( lonModel_, infos_[ pair[ 0 ] ] ) &&
                         populate( latModel_, infos_[ pair[ 1 ] ] ) ) {
                        return sys;
                    }
                    else {
                        assert false;
                    }
                }
            }
            return null;
        }

        /**
         * Tries to find an item of a given combo box model matching a given
         * metadata item.  If it finds it, it will set the selection and
         * return true.
         *
         * @param   model   list model
         * @param  info   template for selection value
         * @return  true if selection was successfully performed
         */
        private static boolean populate( ColumnDataComboBoxModel model,
                                         ValueInfo info ) {
            for ( int i = 0; i < model.getSize(); i++ ) {
                Object item = model.getElementAt( i );
                if ( item instanceof ColumnData &&
                     infoMatches( ((ColumnData) item).getColumnInfo(),
                                  info ) ) {
                    model.setSelectedItem( item );
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns a list of column metadata items for the items in a
         * list model of columns.
         *
         * @param  model  column list model
         * @return  list of valueinfos
         */
        private static ValueInfo[] getInfos( ColumnDataComboBoxModel model ) {
            List<ValueInfo> list = new ArrayList<ValueInfo>();
            for ( int i = 0; i < model.getSize(); i++ ) {
                Object item = model.getElementAt( i );
                if ( item instanceof ColumnData ) {
                    ValueInfo info = ((ColumnData) item).getColumnInfo();
                    if ( info != null ) {
                        list.add( info );
                    }
                }
            }
            return list.toArray( new ValueInfo[ 0 ] );
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

        /**
         * Indicates whether two infos match.
         * The criterion is that both name and UCD are the same.
         *
         * @param  info1  first item
         * @param  info2  second item
         * @return  true iff match
         */
        private static boolean infoMatches( ValueInfo info1, ValueInfo info2 ) {
            return PlotUtil.equals( info1.getName(), info2.getName() )
                && PlotUtil.equals( info1.getUCD(), info2.getUCD() );
        }
    }
}
