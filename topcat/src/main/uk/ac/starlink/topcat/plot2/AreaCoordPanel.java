package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.AreaDomain;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * PositionCoordPanel for entering positional Area values.
 * This implementation class mostly handles autopopulation
 * (default values).
 *
 * @author   Mark Taylor
 * @since    27 Mar 2020
 */
public abstract class AreaCoordPanel extends BasicCoordPanel
                                     implements PositionCoordPanel {

    /**
     * Constructor.
     *
     * @param  coord  coordinate for Area objects
     * @param  otherCoords  additional coordinates required by panel
     * @param  configKeys  config value keys (often empty)
     */
    protected AreaCoordPanel( AreaCoord<?> coord, Coord[] otherCoords,
                              ConfigKey<?>[] configKeys ) {
        super( PlotUtil.arrayConcat( new Coord[] { coord }, otherCoords ),
               configKeys );
    }

    @Override
    public void autoPopulate() {
        ColumnDataComboBoxModel areaSelector = getColumnSelector( 0, 0 );
        if ( areaSelector == null ) {
            assert false;
            return;
        }
        int ncol = areaSelector.getSize();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnData cdata = areaSelector.getColumnDataAt( ic );
            if ( cdata != null ) {
                ColumnInfo info = cdata.getColumnInfo();
                if ( info != null &&
                     AreaDomain.INSTANCE.getProbableMapper( info ) != null ) {
                    areaSelector.setSelectedItem( cdata );
                    return;
                }
            }
        }
    }
}
