package uk.ac.starlink.tfcat;

import org.json.JSONObject;

/**
 * Abstract superclass representing one of the seven typed Geometry
 * objects defined in the TFCat specification.
 * Instances will be one of the seven typed internal subclasses.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public abstract class Geometry<S> extends TfcatObject {

    private final S shape_;

    /**
     * Constructor.
     *
     * @param   json  JSON object on which this is based
     * @param   type  value of type member, defining the TFCat type
     * @param   bbox  bounding box defined by bbox member, may be null
     * @param   shape  content of geometry
     */
    private Geometry( JSONObject json, String type, Bbox bbox, S shape ) {
        super( json, type, bbox );
        shape_ = shape;
    }

    /**
     * Returns the coordinate information giving the content of this geometry.
     *
     * @return  shape
     */
    public S getShape() {
        return shape_;
    }

    @Override
    public void purgeJson() {
        super.purgeJson();
        if ( shape_ instanceof TfcatObject ) {
            ((TfcatObject) shape_).purgeJson();
        }
        else if ( shape_ instanceof TfcatObject[] ) {
            for ( TfcatObject tfc : ((TfcatObject[]) shape_) ) {
                tfc.purgeJson();
            }
        }
    }

    /** Geometry subclass representing a TFCat Point. */
    public static class Point extends Geometry<Position> {
        Point( JSONObject json, Bbox bbox, Position position ) {
            super( json, "Point", bbox, position );
        }
    }

    /** Geometry subclass representing a TFCat MultiPoint. */
    public static class MultiPoint extends Geometry<Position[]> {
        MultiPoint( JSONObject json, Bbox bbox, Position[] positions ) {
            super( json, "MultiPoint", bbox, positions );
        }
    }

    /** Geometry subclass representing a TFCat LineString. */
    public static class LineString extends Geometry<Position[]> {
        LineString( JSONObject json, Bbox bbox, Position[] positions ) {
            super( json, "LineString", bbox, positions );
        }
    }

    /** Geometry subclass representing a TFCat MultiLineString. */
    public static class MultiLineString extends Geometry<Position[][]> {
        MultiLineString( JSONObject json, Bbox bbox, Position[][] lines ) {
            super( json, "MultiLineString", bbox, lines );
        }
    }

    /** Geometry subclass representing a TFCat Polygon. */
    public static class Polygon extends Geometry<LinearRing[]> {
        Polygon( JSONObject json, Bbox bbox, LinearRing[] rings ) {
            super( json, "Polygon", bbox, rings );
        }
    }

    /** Geometry subclass representing a TFCat MultiPolygon. */
    public static class MultiPolygon extends Geometry<LinearRing[][]> {
        MultiPolygon( JSONObject json, Bbox bbox, LinearRing[][] polygons ) {
            super( json, "MultiPolygon", bbox, polygons );
        }
    }

    /** Geometry subclass representing a TFCat GeometryCollection. */
    public static class GeometryCollection extends Geometry<Geometry<?>[]> {
        GeometryCollection( JSONObject json, Bbox bbox, Geometry<?>[] geoms ) {
            super( json, "GeometryCollection", bbox, geoms );
        }
    }
}
