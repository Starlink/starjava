package uk.ac.starlink.topcat.plot2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.IntegerCoord;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;

/**
 * LayerControl for plotting Healpix tile sets.
 *
 * @author   Mark Taylor
 * @since    20 Apr 2016
 */
public class HealpixLayerControl extends BasicCoordLayerControl {

    /**
     * Constructor.
     *
     * @param   plotter  healpix plotter
     * @param   zsel    zone id specifier, may be null for single-zone case
     * @param   baseConfigger   provides global configuration info
     */
    public HealpixLayerControl( HealpixPlotter plotter, Specifier<ZoneId> zsel,
                                Configger baseConfigger ) {
        super( plotter, zsel, new HealpixCoordPanel(), baseConfigger, true );
        assert plotter.getCoordGroup().getPositionCount() == 0;
    }

    public LegendEntry[] getLegendEntries() {
        String label = getLegendLabel();
        Style style = getLegendStyle();
        return label != null && style != null
             ? new LegendEntry[] { new LegendEntry( label, style ) }
             : new LegendEntry[ 0 ];
    }

    /**
     * Returns the plot style chosen for the currently configured plot,
     * if any.
     *
     * @return  style or null
     */
    private Style getLegendStyle() {
        PlotLayer[] layers = getPlotLayers();
        if ( layers.length == 1 ) {
            return layers[ 0 ].getStyle();
        }
        return null;
    }

    /**
     * Returns a label suitable for the legend of the currently configured
     * plot, if any.
     *
     * @return  data label or null
     */
    private String getLegendLabel() {
        for ( GuiCoordContent content : getCoordPanel().getContents() ) {
            if ( HealpixPlotter.VALUE_COORD.equals( content.getCoord() ) ) {
                String[] labels = content.getDataLabels();
                if ( labels.length == 1 ) {
                    return labels[ 0 ];
                }
            }
        }
        return null;
    }

    /**
     * CoordPanel implementation for HealpixLayerControl.
     */
    private static class HealpixCoordPanel extends SimplePositionCoordPanel {
        private static final IntegerCoord HEALPIX_COORD =
            HealpixPlotter.HEALPIX_COORD;
        private static final FloatingCoord VALUE_COORD =
            HealpixPlotter.VALUE_COORD;
        private static final ConfigKey<SkySys> DATASYS_KEY =
            HealpixPlotter.DATASYS_KEY;
        private static final ConfigKey<Integer> DATALEVEL_KEY =
            HealpixPlotter.DATALEVEL_KEY;
        private static final Pattern HPXNAME_REGEX =
            Pattern.compile( "(healpix|hpx)[-_.]*([0-9]*)",
                             Pattern.CASE_INSENSITIVE );
        private static final Pattern HPX_REGEX =
            Pattern.compile( "(healpix|hpx)", Pattern.CASE_INSENSITIVE );
        private static final Pattern NUM_REGEX =
            Pattern.compile( "[^0-9]*([0-9]+)[^0-9]*" );

        /**
         * Constructor.
         */
        HealpixCoordPanel() {
            super( new Coord[] { HEALPIX_COORD, VALUE_COORD },
                   new ConfigKey[] { DATASYS_KEY, DATALEVEL_KEY }, null );
        }

        public void autoPopulate() {

            /* Assemble GUI components we may be able to fill in. */
            Specifier<Integer> levelSpecifier =
                getConfigSpecifier().getSpecifier( DATALEVEL_KEY );
            ColumnDataComboBoxModel hpxSelector = getColumnSelector( 0, 0 );
            ColumnDataComboBoxModel valueSelector = getColumnSelector( 1, 0 );
            boolean hasSelectors = hpxSelector != null
                                && valueSelector != null
                                && levelSpecifier != null;
            assert hasSelectors;

            /* Try to identify a column containing healpix indices,
             * and if so fill the GUI components in accordingly. */
            HpxCol hcol = hasSelectors ? getHealpixColumn( hpxSelector ) : null;
            if ( hcol != null && hcol.cdata_ != null ) {
                int level = hcol.level_;
                if ( level >= 0 && level <= HealpixPlotter.MAX_LEVEL ) {
                    levelSpecifier.setSpecifiedValue( new Integer( level ) );
                }
                ColumnData hpxData = hcol.cdata_;
                hpxSelector.setSelectedItem( hpxData );
                ColumnData valData = getOtherColumn( valueSelector, hpxData );
                if ( valData != null ) {
                    valueSelector.setSelectedItem( valData );
                }
            }
        }

        /**
         * Returns a column from a given selector that is distinct from
         * a supplied column.
         *
         * @param  selector  selector model
         * @param  cmpCol   comparison column data
         * @return   column data with different content to <code>cmpCol</code>
         */
        private static ColumnData
                getOtherColumn( ColumnDataComboBoxModel selector,
                                ColumnData cmpCol ) {
            int ncol = selector.getSize();
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnData cdata = selector.getColumnDataAt( ic );
                if ( cdata != null ) {
                    if ( isDifferent( cdata, cmpCol ) ) {
                        return cdata;
                    }
                }
            }
            return null;
        }

        /**
         * Indicates whether two column data objects appear to represent
         * different data.
         *
         * @param  cdata1  first item
         * @param  cdata2  second item
         * @return  true if different
         */
        private static boolean isDifferent( ColumnData cdata1,
                                            ColumnData cdata2 ) {
            String cname1 = cdata1.getColumnInfo().getName();
            String cname2 = cdata2.getColumnInfo().getName();
            return cname1 != null && cname2 != null
                && ! cname1.equals( cname2 );
        }

        /**
         * Tries to identify a column from a selector that represents
         * healpix index values.  If it finds one, it returns a structure
         * constaining the selector item along with a value for the
         * healpix level it represents.
         *
         * <p>At time of writing there is no respectable metadata
         * indicating healpixness, so we have to grub around looking
         * at column names and descriptions.
         *
         * @param  selector  selector containing columns
         * @return   item indicating healpix column, or null if none found
         */
        private static HpxCol
                getHealpixColumn( ColumnDataComboBoxModel selector ) {
            int ncol = selector.getSize();
            HpxCol byName = null;
            HpxCol byDescrip = null;
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnData cdata = selector.getColumnDataAt( ic );
                if ( cdata != null ) {
                    ColumnInfo info = cdata.getColumnInfo();
                    if ( byName == null ) {
                        String name = info.getName();
                        if ( name != null ) {
                            Matcher matcher = HPXNAME_REGEX.matcher( name );
                            if ( matcher.find() ) {
                                byName =
                                    new HpxCol( cdata, matcher.group( 2 ) );
                            }
                        }
                    }
                    if ( byDescrip == null ) {
                        String descrip = info.getDescription();
                        if ( descrip != null &&
                             HPX_REGEX.matcher( descrip ).find() ) {
                            Matcher numMatcher = NUM_REGEX.matcher( descrip );
                            if ( numMatcher.matches() ) {
                                byDescrip =
                                    new HpxCol( cdata, numMatcher.group( 1 ) );
                            }
                        }
                    }
                }
            }
            if ( byName != null ) {
                return byName;
            }
            if ( byDescrip != null ) {
                return byDescrip;
            }
            return null;
        }
    }

    /**
     * Aggregates a ColumnData object and the Healpix level represented
     * by its contents.
     */
    private static class HpxCol {
        final ColumnData cdata_;
        final int level_;

        /**
         * Constructs a HpxCol from data and level.
         *
         * @param  cdata  column data, not null
         * @param  level   healpix level, -1 if not known
         */
        HpxCol( ColumnData cdata, int level ) {
            cdata_ = cdata;
            level_ = level;
        }

        /**
         * Contructs a HpxCol from data and level string.
         *
         * @param  cdata  column data, not null
         * @param  level  string that may represent healpix level, may be null
         */
        HpxCol( ColumnData cdata, String levelStr ) {
            this( cdata, toNumber( levelStr ) );
        }

        /**
         * Tries to turn a string into a number.
         *
         * @param  txt  string
         * @return  numberic value, or -1 if conversion failed
         */
        private static int toNumber( String txt ) {
            if ( txt == null || txt.trim().length() == 0 ) {
                return -1;
            }
            else {
                try {
                    return Integer.parseInt( txt );
                }
                catch ( NumberFormatException e ) {
                    return -1;
                }
            }
        }
    }
}
