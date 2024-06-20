package uk.ac.starlink.ttools.example;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * Experimental GeoJSON table input handler.
 * It was written with reference to RFC7946 and a countries.geojson file
 * I found, but it's pretty scrappy.  It looks for a type="FeatureCollection"
 * entry in the top-level object, and then pulls Features out of that,
 * currently taking account of only Polygon and MultiPolygon types.
 * Winding directions and exclusions are currently ignored.
 * Polygons are turned into STC-S POLYGON or UNION of POLYGON entries,
 * and the other metadata items are stored in their own columns.
 * The whole GeoJSON input file is slurped into memory, even in streaming mode.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2020
 */
public class GeojsonTableBuilder implements TableBuilder {

    private String shapeColName_;

    /**
     * Default constructor.
     */
    public GeojsonTableBuilder() {
        shapeColName_ = "shape";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public String getFormatName() {
        return "GeoJSON";
    }

    public boolean looksLikeFile( String location ) {
        String loc = location.toLowerCase();
        return loc.endsWith( ".geojson" )
            || loc.endsWith( ".geo-json" );
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage )
            throws IOException {
        return createStarTable( new BufferedInputStream( datsrc
                                                        .getInputStream() ) );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        StarTable table = createStarTable( in );
        sink.acceptMetadata( table );
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            sink.acceptRow( rseq.getRow() );
        }
        sink.endRows();
    }

    /**
     * Sets the name for the feature shape column.
     *
     * @param  shapeColName  name for STC-S feature shape column
     */
    public void setShapeColName( String shapeColName ) {
        shapeColName_ = shapeColName;
    }

    /**
     * Returns the name of the feature shape column.
     *
     * @return  name for STC-S feature shape column
     */
    public String getShapeColName() {
        return shapeColName_;
    }

    /**
     * Constructs a StarTable based on JSON from a given input stream.
     *
     * @param  in  input stream containing GeoJSON; closed before exit
     */
    private StarTable createStarTable( InputStream in ) throws IOException {
        JSONObject top;
        try {
            top = new JSONObject( new JSONTokener( in ) );
        }
        catch ( JSONException e ) {
            throw new TableFormatException( "Not JSON", e );
        }
        finally {
            in.close();
        }
        if ( "FeatureCollection".equals( top.get( "type" ) ) ) {
            JSONArray features = (JSONArray) top.get( "features" );
            return createStarTable( features );
        }
        else {
            throw new TableFormatException( "No FeatureCollection "
                                          + " in top-level JSON object" );
        }
    }

    /**
     * Creates a StarTable based on a JSONArray assumed to contain
     * GeoJSON type="Feature" items.
     *
     * @param   features   feature array
     * @return  table
     */
    private StarTable createStarTable( final JSONArray features ) {
        int nf = features.length();

        /* Go through all the rows picking up properties items;
         * work out which ones are present so that we can use any of them
         * that occurs at least once as a table heading. */
        Map<String,Object> propMap = new LinkedHashMap<>();
        for ( int i = 0; i < nf; i++ ) {
            JSONObject jobj = features.getJSONObject( i );
            if ( "Feature".equals( jobj.get( "type" ) ) ) {
                JSONObject props = jobj.getJSONObject( "properties" );
                if ( props != null ) {
                    for ( String key : props.keySet() ) {
                        Object value = props.get( key );
                        propMap.put( key, value );
                    }
                }
            }
        }

        /* Adapt the features array as a StarTable. */
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( nf );
        for ( Map.Entry<String,Object> entry : propMap.entrySet() ) {
            ColumnData cdata =
                getColumnData( features, entry.getKey(), entry.getValue() );
            if ( cdata != null ) {
                table.addColumn( cdata );
            }
        }

        /* The feature data will be presented as column with xtype="stc-s".
         * DALI xtype="polygon" would be more efficient, but at time
         * of writing it does not handle multi-polygons. */
        ColumnInfo geomInfo = new ColumnInfo( shapeColName_, String.class, "" );
        geomInfo.setXtype( "stc-s" );
        table.addColumn( new ColumnData( geomInfo ) {
            public Object readValue( long irow ) {
                JSONObject geom = features.getJSONObject( (int) irow )
                                          .getJSONObject( "geometry" );
                String type = geom.getString( "type" );
                JSONArray coords = geom.getJSONArray( "coordinates" );
                return toStcs( geom.getString( "type" ),
                               geom.getJSONArray( "coordinates" ) );
            }
        } );
        return table;
    }

    /**
     * Attempts to construct a column based on a feature property.
     *
     * @param   features  feature array
     * @param   key   key from features properties object
     * @param   exampleValue  example value from feature properties object
     *                        associated with <code>key</code>
     * @return   column data, or null
     */
    private ColumnData getColumnData( final JSONArray features,
                                      final String key, Object exampleValue ) {
        final Class<?> clazz = exampleValue == null
                             ? null
                             : exampleValue.getClass();
        if ( exampleValue instanceof Number ) {
            return new ColumnData( new ColumnInfo( key, Double.class, null ) ) {
                public Object readValue( long irow ) {
                    Object value = features
                                  .getJSONObject( (int) irow )
                                  .getJSONObject( "properties" )
                                  .get( key );
                    if ( value instanceof Double ) {
                        return value;
                    }
                    else if ( value instanceof Number ) {
                        return Double.valueOf( ((Number) value).doubleValue() );
                    }
                    else {
                        return null;
                    }
                }
            };
        }
        else if ( exampleValue instanceof String ||
                  exampleValue instanceof Boolean ) {
            return new ColumnData( new ColumnInfo( key, clazz, null ) ) {
                public Object readValue( long irow ) {
                    Object value = features
                                  .getJSONObject( (int) irow )
                                  .getJSONObject( "properties" )
                                  .get( key );
                    return clazz.isInstance( value ) ? value : null;
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Turns GeoJSON feature information into an STC-S string, if possible.
     *
     * @param  type   GeoJSON feature type;
     *                only Polygon and MultiPolygon currently recognised
     * @param  jsonCoords  array of coordinate tuples;
     *                     the first two (lon,lat) entries from each tuple
     *                     are used, any subsequent values including elevation
     *                     are ignored
     * @return  STC-S string approximating GeoJSON intent if possible,
     *          or null if not
     */
    private String toStcs( String type, JSONArray jsonCoords ) {
        if ( "MultiPolygon".equals( type ) ) {
            int npoly = jsonCoords.length();
            StringBuffer sbuf = new StringBuffer();
            if ( npoly > 1 ) {
                sbuf.append( "UNION (" );
            }
            for ( int ip = 0; ip < npoly; ip++ ) {
                if ( ip > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( toStcs( "Polygon",
                                     jsonCoords.getJSONArray( ip ) ) );
            }
            if ( npoly > 1 ) {
                sbuf.append( ")" );
            }
            return sbuf.toString();
        }
        if ( "Polygon".equals( type ) ) {
            JSONArray linearRing = jsonCoords.getJSONArray( 0 );
            int nc = linearRing.length();
            StringBuffer sbuf = new StringBuffer( "POLYGON" );
            // The first and last elements of the linearRing are supposed
            // to be identical, so skip the last one.
            // But it might be prudent to check they really are the same.
            for ( int ic = 0; ic < nc - 1; ic++ ) {
                JSONArray point = linearRing.getJSONArray( ic );
                sbuf.append( ' ' )
                    .append( point.getDouble( 0 ) )
                    .append( ' ' )
                    .append( point.getDouble( 1 ) );
            }
            return sbuf.toString();
        }
        return null;
    }
}
