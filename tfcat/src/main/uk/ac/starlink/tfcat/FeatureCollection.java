package uk.ac.starlink.tfcat;

import java.util.Map;
import org.json.JSONObject;

/**
 * Represents a TFCat FeatureCollection object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class FeatureCollection extends TfcatObject {

    private final Feature[] features_;
    private final Map<String,Field> fieldMap_;

    /**
     * Constructor.
     *
     * @param   json  JSON object on which this is based
     * @param   crs   coordinate reference system, may be null
     * @param   bbox  bounding box defined by bbox member, may be null
     * @param   fieldMap  map of fields associated with this collection;
     *                    may be empty but not null
     * @param   features   features in this collection;
     *                     may be empty but not null
     */
    public FeatureCollection( JSONObject json, Crs crs, Bbox bbox,
                              Map<String,Field> fieldMap, Feature[] features ) {
        super( json, "FeatureCollection", crs, bbox );
        for ( Feature f : features ) {
            f.setParent( this );
        }
        features_ = features;
        fieldMap_ = fieldMap;
    }

    /**
     * Returns the features in this collection.
     *
     * @return  feature array
     */
    public Feature[] getFeatures() {
        return features_;
    }

    /**
     * Returns a map of field names to fields for this collection.
     *
     * @return  field map
     */
    public Map<String,Field> getFields() {
        return fieldMap_;
    }
}
