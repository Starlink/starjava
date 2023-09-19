package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;

/**
 * CoordPanel for 2-d vector-valued coordinates.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2021
 */
public class XYArrayCoordPanel extends PositionCoordPanel {

    private static final int MIN_USEFUL_SIZE = 10;
    private static final FloatingArrayCoord[] XYARRAY_COORDS = {
        FloatingArrayCoord.X, FloatingArrayCoord.Y,
    };

    /**
     * Constructor.
     */
    public XYArrayCoordPanel() {
        super( XYARRAY_COORDS.clone(), new ConfigKey<?>[ 0 ] );
    }

    /**
     * Returns null.
     */
    public DataGeom getDataGeom() {
        return null;
    }

    @Override
    public boolean isPreferredCoord( Coord coord ) {

        /* Neither X nor Y is actually required for a viable plot,
         * but at least one or the other should be filled in. */
        return coord.isRequired()
            || Arrays.asList( XYARRAY_COORDS ).contains( coord );
    }

    @Override
    public void autoPopulate() {
        TopcatModel tcModel = getTable();
        StarTable table = tcModel == null
                        ? null
                        : tcModel.getApparentStarTable();
        String[] pair = table == null
                      ? null
                      : getPossiblePair( getColumnArrays( table ),
                                         getParamArrays( table ) );
        if ( pair != null ) {
            ColumnDataComboBoxModel xModel = getColumnSelector( 0, 0 );
            ColumnDataComboBoxModel yModel = getColumnSelector( 1, 0 );
            final ColumnData xdata;
            final ColumnData ydata;
            try {
                xdata = xModel.stringToColumnData( pair[ 0 ] );
                ydata = yModel.stringToColumnData( pair[ 1 ] );
            }
            catch ( CompilationException e ) {
                assert false;
                return;
            }
            xModel.setSelectedItem( xdata );
            yModel.setSelectedItem( ydata );
        }
    }

    /**
     * Returns a pair of strings that could be used as matched values
     * for the X and Y coordinate selectors.  Used for autopopulation.
     * Guesswork is involved.
     *
     * @param   colArrays  map of lists of array string representations
     *                     keyed by array size
     * @param   paramArrays  map of lists of parameter string representations
     *                       keyed by array size
     */
    private static String[]
            getPossiblePair( Map<Integer,List<String>> colArrays,
                             Map<Integer,List<String>> paramArrays ) {

        /* Prefer a parameter for the X value and a column for the Y value.
         * Possibly this choice is prejudiced by the test data I happen
         * to be looking at during development.
         * Array sizes must visibly match. */
        for ( Map.Entry<Integer,List<String>> paramEnt :
              paramArrays.entrySet() ) {
            Integer pSize = paramEnt.getKey();
            if ( pSize.intValue() >= MIN_USEFUL_SIZE ) {
                List<String> cols = colArrays.get( pSize );
                if ( cols != null ) {
                    String pTxt = paramEnt.getValue().get( 0 );
                    String cTxt = cols.get( 0 );
                    return new String[] { pTxt, cTxt };
                }
            }
        }

        /* Otherwise, look for matched column values.
         * Array sizes must visibly match. */
        for ( Map.Entry<Integer,List<String>> colEnt : colArrays.entrySet() ) {
            Integer cSize = colEnt.getKey();
            List<String> cTxts = colEnt.getValue();
            if ( cSize.intValue() >= MIN_USEFUL_SIZE && cTxts.size() > 1 ) {
                return new String[] { cTxts.get( 0 ), cTxts.get( 1 ) };
            }
        }
        return null;
    }

    /**
     * For a given table, returns array column representations grouped
     * by declared array size.
     *
     * @param  table  input table
     * @return  map of lists of array string representations
     *          keyed by array size
     */
    private static SortedMap<Integer,List<String>>
            getColumnArrays( StarTable table ) {
        SortedMap<Integer,List<String>> map = new TreeMap<>();
        int nc = table.getColumnCount();
        for ( int ic = 0; ic < nc; ic++ ) {
            ColumnInfo cinfo = table.getColumnInfo( ic );
            int[] shape = getNumericArrayShape( cinfo );
            if ( shape != null && shape.length == 1 ) {
                int vsize = Math.max( shape[ 0 ], -1 );
                if ( vsize > 1 || vsize == -1 ) {
                    map.computeIfAbsent( vsize, vs -> new ArrayList<String>() )
                       .add( cinfo.getName() );
                }
            }
        }
        return map;
    }

    /**
     * For a given table, returns array parameter value representations grouped
     * by declared array size.
     *
     * @param  table  input table
     * @return  map of lists of array string representations
     *          keyed by array size
     */
    private static SortedMap<Integer,List<String>>
            getParamArrays( StarTable table ) {
        SortedMap<Integer,List<String>> map = new TreeMap<>();
        for ( DescribedValue param : table.getParameters() ) {
            int[] shape = getNumericArrayShape( param.getInfo() );
            if ( shape != null && shape.length == 1 ) {
                Object value = param.getValue();
                if ( value != null && value.getClass().isArray() ) {
                    int vsize = Array.getLength( value );
                    if ( vsize > 1 ) {
                        map.computeIfAbsent( vsize,
                                             vs -> new ArrayList<String>() )
                           .add( "param$" + param.getInfo().getName() );
                    }
                }
            }
        }
        return map;
    }

    /**
     * Returns the array shape corresponding to a ValueInfo for numeric
     * array data.
     *
     * @param  info  metadata item
     * @return   array shape if numeric array, null otherwise
     */
    private static int[] getNumericArrayShape( ValueInfo info ) {
        Class<?> clazz = info.getContentClass();
        if ( clazz.equals( byte[].class ) ||
             clazz.equals( short[].class ) ||
             clazz.equals( int[].class ) ||
             clazz.equals( long[].class ) ||
             clazz.equals( float[].class ) ||
             clazz.equals( double[].class ) ) {
            return info.getShape();
        }
        else {
            return null;
        }
    }
}
