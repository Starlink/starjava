package uk.ac.starlink.tfcat;

import org.json.JSONObject;

/**
 * Superclass for TFCat objects.
 * This corresponds to objects of all of the nine "TFCat types"
 * defined in the TFCat specification.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public abstract class TfcatObject {

    private JSONObject json_;
    private final String type_;
    private final Bbox bbox_;

    /**
     * Constructor.
     *
     * @param   json  JSON object on which this is based
     * @param   type  value of type member, defining the TFCat type
     * @param   bbox  bounding box defined by bbox member, may be null
     */
    protected TfcatObject( JSONObject json, String type, Bbox bbox ) {
        json_ = json;
        type_ = type;
        bbox_ = bbox;
    }

    /**
     * Returns the JSON object on which this is based.
     * On construction this will be non-null, but if {@link #purgeJson}
     * has been called it will be null.
     *
     * @return  JSON object or null
     */
    public JSONObject getJson() {
        return json_;
    }

    /**
     * Returns the value of the type member, defining the TFCat type.
     *
     * @return  type string
     */
    public String getType() {
        return type_;
    }

    /**
     * Returns this object's bounding box, if any.
     *
     * @return   bbox, may be null
     */
    public Bbox getBbox() {
        return bbox_;
    }

    /**
     * Removes any reference to the original parsed JSON from this object.
     * This may be useful for purposes of efficiency following a parse
     * if the TFCat objects are to be long-lived.
     */
    public void purgeJson() {
        json_ = null;
    }
}
