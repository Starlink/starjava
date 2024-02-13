package uk.ac.starlink.hapi;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Characterises the bins information of a HAPI parameter.
 * One but not both of the {@link #getRanges} and {@link #getCenters}
 * methods is supposed to return null.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2024
 * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#3611-bins-object"
 *          >HAPI 3.1, sec 3.6.11</a>
 */
public interface HapiBins {

    /**
     * Name for the dimension.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns an array giving the centers of each bin.
     * May be null if ranges has a value.
     *
     * @return bin centers
     */
    double[] getCenters();

    /**
     * Returns an array of 2-element arrays giving (low,high) bounds of
     * each bin.
     * May be null if centers has a value.
     * 
     * @return   array of 2-element arrays
     */
    double[][] getRanges();

    /**
     * Returns the units of bin centers or ranges.
     *
     * @return  units string
     */
    String getUnits();

    /**
     * Returns a human-readable label for bins axis.
     *
     * @return  label or null
     */
    String getLabel();

    /**
     * Returns a description of what the bins represent.
     *
     * @return  description or null
     */
    String getDescription();

    /**
     * Reads HapiBins from a HAPI bins json object.
     *
     * @param  json  JSON object
     * @return  bins object, not null
     */
    public static HapiBins fromJson( JSONObject json ) {
        String name = json.optString( "name", null );
        String units = json.optString( "units", null );
        String label = json.optString( "label", null );
        String description = json.optString( "description", null );
        JSONArray centersArray = json.optJSONArray( "centers" );
        final double[] centers;
        if ( centersArray != null ) {
            int nc = centersArray.length();
            centers = new double[ nc ];
            for ( int ic = 0; ic < nc; ic++ ) {
                centers[ ic ] = centersArray.optDouble( ic );
            }
        }
        else {
            centers = null;
        }
        JSONArray rangesArray = json.optJSONArray( "ranges" );
        final double[][] ranges;
        if ( rangesArray != null ) {
            int nr = rangesArray.length();
            ranges = new double[ nr ][];
            for ( int ir = 0; ir < nr; ir++ ) {
                JSONArray rangeArray = rangesArray.optJSONArray( ir );
                if ( rangeArray != null && rangeArray.length() == 2 ) {
                    double lo = rangeArray.optDouble( 0 );
                    double hi = rangeArray.optDouble( 1 );
                    if ( lo < hi ) {
                        ranges[ ir ] = new double[] { lo, hi };
                    }
                }
            }
        }
        else {
            ranges = null;
        }
        return new HapiBins() {
            public String getName() {
                return name;
            }
            public double[] getCenters() {
                return centers;
            }
            public double[][] getRanges() {
                return ranges;
            }
            public String getUnits() {
                return units;
            }
            public String getLabel() {
                return label;
            }
            public String getDescription() {
                return description;
            }
        };
    }
}
