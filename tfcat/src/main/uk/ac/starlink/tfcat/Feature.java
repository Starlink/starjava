package uk.ac.starlink.tfcat;

import org.json.JSONObject;

/**
 * Represents a TFCat Feature object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class Feature extends TfcatObject {

    private final Geometry<?> geometry_;
    private final String id_;
    private JSONObject properties_;

    /**
     * Constructor.
     *
     * @param  json  JSON object with "type":"Feature" on which this is based
     * @param  crs   coordinate reference system, may be null
     * @param  bbox  bounding box, may be null
     * @param  geometry  geometry content, may be null
     * @param  id    identifier string, may be null
     * @param  properties   properties object, may be null
     */
    public Feature( JSONObject json, Crs crs, Bbox bbox, Geometry<?> geometry,
                    String id, JSONObject properties ) {
        super( json, "Feature", crs, bbox );
        if ( geometry != null ) {
            geometry.setParent( this );
        }
        geometry_ = geometry;
        id_ = id;
        properties_ = properties;
    }

    /**
     * Returns this feature's geometry.
     *
     * @return   geometry, may be null
     */
    public Geometry<?> getGeometry() {
        return geometry_;
    }

    /**
     * Returns this feature's identifier.
     *
     * @return  id string, may be null
     */
    public String getId() {
        return id_;
    }

    /**
     * Returns this featurs's properties object.
     *
     * @return  properties object, may be null
     */
    public JSONObject getProperties() {
        return properties_;
    }

    @Override
    public void purgeJson() {
        super.purgeJson();
        properties_ = null;
        geometry_.purgeJson();
    }
}
