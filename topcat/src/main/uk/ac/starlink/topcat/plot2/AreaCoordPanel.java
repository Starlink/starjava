package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * PositionCoordPanel for entering positional Area values.
 * This implementation class mostly handles autopopulation
 * (default values).
 *
 * @author   Mark Taylor
 * @since    27 Mar 2020
 */
public abstract class AreaCoordPanel extends PositionCoordPanel {

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
        int bestScore = 0;
        ColumnData bestCdata = null;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnData cdata = areaSelector.getColumnDataAt( ic );
            if ( cdata != null ) {
                ColumnInfo info = cdata.getColumnInfo();
                if ( info != null ) {
                    int score = getAreaScore( info );
                    if ( score > bestScore ) {
                        bestScore = score;
                        bestCdata = cdata;
                    }
                }
            }
        }
        if ( bestCdata != null ) {
            areaSelector.setSelectedItem( bestCdata );
        }
    }

    /**
     * Returns some kind of value which indicates how likely a column
     * is to represent an area that can be plotted by this panel.
     *
     * @param   info  column metadata
     * @return   score for area compability:
     *           higher is better, zero means no use
     */
    private static int getAreaScore( ColumnInfo info ) {
        int score = 0;
        Class<?> clazz = info.getContentClass();
        String name = info.getName();
        String ucd = info.getUCD();
        String xtype = Tables.getXtype( info );
        boolean isString = clazz.equals( String.class );
        boolean isNumarray = clazz.equals( float[].class )
                          || clazz.equals( double[].class );
        if ( isString || isNumarray ) {
            if ( "s_region".equals( name ) ) {
                score += 4;
            }
            if ( "pos.outline;obs.field".equals( ucd ) ) {
                score += 3;
            }
            else if ( ucd != null && ucd.startsWith( "pos.outline" ) ) {
                score += 2;
            }
        }
        if ( isNumarray ) {
            if ( "circle".equalsIgnoreCase( xtype ) ||
                 "polygon".equalsIgnoreCase( xtype ) ) {
                score += 5;
            }
            else if ( "point".equalsIgnoreCase( xtype ) ) {
                score += 2;
            }
        }
        return score;
    }
}
