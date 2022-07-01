package uk.ac.starlink.tfcat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Contains decoder implementations for all the TFCat types.
 * This class contains most of the logic for converting a parsed JSON
 * object representing a TFCat text into the classes provided by this package,
 * while performing attendant validation.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public abstract class Decoders {

    /* Careful if you try to rearrange the sequence of the assignments in
     * the source code.  Some of the later ones require the earlier ones,
     * so surprising runtime errors can result if you're not careful. */

    /**
     * Private constructor prevents instantiation.
     */
    private Decoders() {
    }

    /**
     * If set true, the "properties" member is permitted on FeatureCollection
     * and Geometry objects, in contravention of Section 7.1 of
     * the GeoJSON spec RFC7946.
     * Currently, the TFCat does allow properties here, so this value
     * is set true.
     *
     * @see <a href="https://gitlab.obspm.fr/maser/catalogues/catalogue-format/-/issues/3"
     *         >TFCat spec issue #3</a>
     */
    public static final boolean ALLOW_FCG_PROPERTIES = true;

    /** Decoder for Position object. */
    public static final Decoder<Position> POSITION =
            ( reporter, json, parent ) -> {
        double[] pair = new JsonTool( reporter ).asNumericArray( json, 2 );
        return pair == null ? null : new Position( pair[ 0 ], pair[ 1 ] );
    };

    /** Decoder for array of Position objects. */
    public static final Decoder<Position[]> POSITIONS =
        createArrayDecoder( POSITION, Position.class );

    /** Decoder for array of Position objects representing TFCat LineString. */
    public static final Decoder<Position[]> LINE_STRING =
            ( reporter, json, parent ) -> {
        Position[] positions = POSITIONS.decode( reporter, json, null );
        if ( positions == null ) {
            return null;
        }
        else {
            int np = positions.length;
            if ( np < 2 ) {
                reporter.report( "Too few positions (" + np + "<2)" );
                return null;
            }
            else {
                return positions;
            }
        }
    };

    /** Decoder for an array of LineStrings. */
    public static final Decoder<Position[][]> LINE_STRINGS =
        createArrayDecoder( LINE_STRING, Position[].class );

    /** Decoder for a LinearRing. */
    public static final Decoder<LinearRing> LINEAR_RING =
            ( reporter, json, parent ) -> {
        Position[] allPositions = POSITIONS.decode( reporter, json, null );
        if ( allPositions == null ) {
            return null;
        }
        else {
            int np1 = allPositions.length;
            if ( np1 < 4 ) {
                reporter.report( "too few positions for linear ring"
                               + " (" + np1 + "<4)" );
                return null;
            }
            else if ( !allPositions[ 0 ].equals( allPositions[ np1 - 1 ] ) ) {
                reporter.report( "first and last positions not identical"
                               + " for linear ring" );
                return null;
            }
            else {
                Position[] distinctPositions = new Position[ np1 - 1 ];
                System.arraycopy( allPositions, 0,
                                  distinctPositions, 0, np1 - 1 );
                return new LinearRing( distinctPositions );
            }
        }
    };

    /** Decoder for array of LinearRings. */
    public static final Decoder<LinearRing[]> LINEAR_RINGS =
        createArrayDecoder( LINEAR_RING, LinearRing.class );

    /** Decoder for array of LinearRings representing a TFCat Polygon. */
    public static final Decoder<LinearRing[]> POLYGON =
            ( reporter, json, parent ) -> {
        LinearRing[] rings = LINEAR_RINGS.decode( reporter, json, null );
        if ( rings != null && rings.length > 0 ) {
            if ( rings[ 0 ].isClockwise() ) {
                reporter.report( "first (exterior) linear ring"
                               + " is not anticlockwise" );
            }
            for ( int ir = 1; ir < rings.length; ir++ ) {
                if ( ! rings[ ir ].isClockwise() ) {
                    reporter.report( "interior linear ring #" + ir
                                   + " is not clockwise" );
                }
            }
        }
        return rings;
    };

    /** Decoder for an array of TFCat polygons. */
    public static final Decoder<LinearRing[][]> POLYGONS =
        createArrayDecoder( POLYGON, LinearRing[].class );

    /** Decoder for a Bbox object. */
    public static final Decoder<Bbox> BBOX =
            ( reporter, json, parent ) -> {
        double[] bounds = new JsonTool( reporter ).asNumericArray( json, 4 );
        if ( bounds == null ) {
            return null;
        }
        else if ( bounds[ 0 ] <= bounds[ 2 ] &&
                  bounds[ 1 ] <= bounds[ 3 ] ) {
            return new Bbox( bounds[ 0 ], bounds[ 1 ],
                             bounds[ 2 ], bounds[ 3 ] );
        }
        else {
            reporter.report( "bbox bounds out of sequence: "
                           + Arrays.toString( bounds ) );
            return null;
        }
    };

    /** Decoder for a DataType object. */
    public static final Decoder<Datatype<?>> DATATYPE =
            ( reporter, json, parent ) -> {
        String txt = new JsonTool( reporter ).asString( json, false );
        if ( txt == null ) {
            reporter.report( "no declared datatype, treat as string" );
            return Datatype.STRING;
        }
        Datatype<?> datatype = Datatype.forName( txt );
        if ( datatype == null ) {
            reporter.report( "Unknown datatype \"" + txt + "\", "
                           + "treat as string" );
            return Datatype.STRING;
        }
        else {
            return datatype;
        }
    };

    /** Decoder for an array of Field objects. */
    public static final Decoder<Field[]> FIELDS =
            ( reporter, json, parent ) -> {
        List<Field> fieldList = new ArrayList<>();
        JSONObject jobj = new JsonTool( reporter ).asJSONObject( json );
        if ( jobj != null ) {
            for ( String key : jobj.keySet() ) {
                Reporter fieldReporter = reporter.createReporter( key );
                JSONObject fieldObj = new JsonTool( fieldReporter ) 
                                     .asJSONObject( jobj.get( key ) );
                if ( fieldObj != null ) {
                    String info =
                        new JsonTool( fieldReporter.createReporter( "info" ) )
                       .asString( fieldObj.opt( "info" ), true );
                    String ucd =
                        new JsonTool( fieldReporter.createReporter( "ucd" ) )
                       .asString( fieldObj.opt( "ucd" ), true );
                    String unit =
                        new JsonTool( fieldReporter.createReporter( "unit" ) )
                       .asString( fieldObj.opt( "unit" ), false );
                    reporter.checkUcd( ucd );
                    reporter.checkUnit( unit );
                    Datatype<?> datatype =
                        DATATYPE.decode( fieldReporter
                                        .createReporter( "datatype" ),
                                         fieldObj.opt( "datatype" ), null );
                    fieldList.add( new Field() {
                        public String getName() {
                            return key;
                        }
                        public String getInfo() {
                            return info;
                        }
                        public String getUcd() {
                            return ucd;
                        }
                        public String getUnit() {
                            return unit;
                        }
                        public Datatype<?> getDatatype() {
                            return datatype; 
                        }
                    } );
                }
            }
        }
        return fieldList.toArray( new Field[ 0 ] );
    };

    /** Decoder for a Geometry object. */
    public static final Decoder<Geometry<?>> GEOMETRY =
            ( reporter, json, parent ) -> {
        JsonTool jtool = new JsonTool( reporter );
        JSONObject jobj = jtool.asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        String type = new JsonTool( reporter.createReporter( "type" ) )
                     .asString( jobj.opt( "type" ), true );
        Object crsJson = jobj.opt( "crs" );
        Crs crs = crsJson == null
                ? null
                : Decoders.CRS
                 .decode( reporter.createReporter( "crs" ), crsJson, null );
        Object bboxJson = jobj.opt( "bbox" );
        Bbox bbox = bboxJson == null
                  ? null
                  : Decoders.BBOX
                   .decode( reporter.createReporter( "bbox" ), bboxJson, null );
        if ( type == null ) {
            return null;
        }
        Map<String,ShapeType<?>> shapeTypes = getShapeTypes();
        TfcatUtil.checkOption( reporter.createReporter( "type" ), type,
                               shapeTypes.keySet() );
        ShapeType<?> shapeType = shapeTypes.get( type );
        if ( shapeType == null ) {
            return null;
        }
        else {
            jtool.requireAbsent( jobj, "geometry" );   // RFC7946 sec 7.1
            if ( ! ALLOW_FCG_PROPERTIES ) {
                jtool.requireAbsent( jobj, "properties" );
            }
            jtool.requireAbsent( jobj, "features" );
            Geometry<?> geom =
                shapeType.createGeometry( reporter.createReporter( type ),
                                          jobj, crs, bbox );
            if ( geom != null ) {
                geom.setParent( parent );
            }
            return geom;
        }
    };

    /** Decoder for a GeometryCollection. */
    public static final Decoder<Geometry<?>[]> GEOMETRIES =
            ( reporter, json, parent ) -> {
        List<Geometry<?>> geomList = new ArrayList<>();
        JSONObject jobj = new JsonTool( reporter ).asJSONObject( json );
        if ( jobj != null ) {
            Reporter geomsReporter = reporter.createReporter( "geometries" );
            JSONArray jarray = new JsonTool( geomsReporter )
                              .asJSONArray( jobj.opt( "geometries" ) );
            if ( jarray != null ) {
                for ( int ig = 0; ig < jarray.length(); ig++ ) { 
                    Geometry<?> geom =
                        GEOMETRY.decode( geomsReporter.createReporter( ig ),
                                         jarray.get( ig ), parent );
                    if ( geom != null ) {
                        geomList.add( geom );
                    }
                }
            }
        }
        return geomList.toArray( new Geometry<?>[ 0 ] );
    };

    /** Decoder for a TimeCoords object. */
    public static final Decoder<TimeCoords> TIME_COORDS =
            ( reporter, json, parent ) -> {
        if ( json == null ) {
            return null;
        }
        JSONObject jobj = new JsonTool( reporter ).asJSONObject( json );
        String name = new JsonTool( reporter.createReporter( "name" ) )
                     .asString( jobj.opt( "name" ), false );
        String unit = new JsonTool( reporter.createReporter( "unit" ) )
                     .asString( jobj.opt( "unit" ), true );
        reporter.checkUnit( unit );
        Reporter toReporter = reporter.createReporter( "time_origin" );
        String timeOrigin = new JsonTool( toReporter )
                           .asString( jobj.opt( "time_origin" ), true );
        if ( timeOrigin != null &&
             ! TimeCoords.TIME_ORIGIN_REGEX.matcher( timeOrigin ).matches() ) {
            toReporter.report( "not ISO-8601: \"" + timeOrigin + "\"" );
        }
        Reporter tsReporter = reporter.createReporter( "time_scale" );
        String timeScale = new JsonTool( tsReporter )
                          .asString( jobj.opt( "time_scale" ), true );
        if ( timeScale != null ) {
             TfcatUtil.checkOption( tsReporter, timeScale,
                                    TimeCoords.TIME_SCALES );
        }
        return new TimeCoords() {
            public String getName() {
                return name;
            }
            public String getUnit() {
                return unit;
            }
            public String getTimeOrigin() {
                return timeOrigin;
            }
            public String getTimeScale() {
                return timeScale;
            }
        };
    };

    /** Decoder for a SpectralCoords object. */
    public static final Decoder<SpectralCoords> SPECTRAL_COORDS =
            ( reporter, json, parent ) -> {
        JSONObject jobj = new JsonTool( reporter ).asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        Reporter typeReporter = reporter.createReporter( "type" );
        String type = new JsonTool( typeReporter )
                     .asString( jobj.opt( "type" ), true );
        if ( type != null ) {
            TfcatUtil.checkOption( typeReporter, type,
                                   SpectralCoords.TYPE_VALUES );
        }
        Reporter unitReporter = reporter.createReporter( "unit" );
        String unit = new JsonTool( unitReporter )
                     .asString( jobj.opt( "unit" ), true );
        unitReporter.checkUnit( unit );
        Reporter scaleReporter = reporter.createReporter( "scale" );
        String scale = new JsonTool( scaleReporter )
                      .asString( jobj.opt( "scale" ), false );
        if ( scale != null ) {
            TfcatUtil.checkOption( scaleReporter, scale,
                                   SpectralCoords.SCALE_VALUES );
        }
        return new SpectralCoords() {
            public String getType() {
                return type;
            }
            public String getUnit() {
                return unit;
            }
            public String getScale() {
                return scale;
            }
        };
    };

    /** Decoder for a CRS object. */
    public static final Decoder<Crs> CRS = ( reporter, json, parent ) -> {
        JSONObject jobj = new JsonTool( reporter ).asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        Reporter typeReporter = reporter.createReporter( "type" );
        String crsType = new JsonTool( typeReporter )
                        .asString( jobj.opt( "type" ), true );
        if ( crsType == null ) {
            return null;
        }
        if ( ! "local".equals( crsType ) ) {
            typeReporter.report( "Unsupported CRS type \"" + crsType + "\"" );
            return new Crs() {
                public String getCrsType() {
                    return crsType;
                }
            };
        }
        Reporter propsReporter = reporter.createReporter( "properties" );
        JSONObject crsProps = new JsonTool( propsReporter )
                             .asJSONObject( jobj.opt( "properties" ) );
        if ( crsProps == null ) {
            return null;
        }

        final String timeCoordsId =
            new JsonTool( propsReporter.createReporter( "time_coords_id" ) )
           .asString( crsProps.opt( "time_coords_id" ), false );
        Object timeCoordsObj = crsProps.opt( "time_coords" );
        TimeCoords timeCoordsDef =
            TIME_COORDS.decode( propsReporter.createReporter( "time_coords" ),
                                crsProps.opt( "time_coords" ), null );
        final TimeCoords timeCoords;
        if ( timeCoordsDef != null ) {
            timeCoords = timeCoordsDef;
            if ( timeCoordsId != null ) {
                propsReporter.report( "time_coords_id unused"
                                    + " since time_coords supplied" );
            }
        }
        else if ( timeCoordsId != null ) {
            TfcatUtil.checkOption( reporter.createReporter( "time_coords_id" ),
                                   timeCoordsId,
                                   TimeCoords.PREDEF_MAP.keySet() );
            timeCoords = TimeCoords.PREDEF_MAP.get( timeCoordsId );
        }
        else {
            propsReporter.report( "Neither time_coords nor time_coords_id"
                                + " supplied" );
            timeCoords = null;
        }

        final SpectralCoords spectralCoords =
            SPECTRAL_COORDS
           .decode( propsReporter.createReporter( "spectral_coords" ),
                    crsProps.opt( "spectral_coords" ), null );

        final String refPositionId =
            new JsonTool( propsReporter.createReporter( "ref_position_id" ) )
           .asString( crsProps.opt( "ref_position_id" ), true );

        return new LocalCrs() {
            public String getCrsType() {
                return "local";
            }
            public String getTimeCoordsId() {
                return timeCoordsId;
            }
            public TimeCoords getTimeCoords() {
                return timeCoords;
            }
            public SpectralCoords getSpectralCoords() {
                return spectralCoords;
            }
            public String getRefPositionId() {
                return refPositionId;
            }
        };
    };

    /** Decoder for a Feature object. */
    public static final Decoder<Feature> FEATURE =
            ( reporter, json, parent ) -> {
        JsonTool jtool = new JsonTool( reporter );
        JSONObject jobj = jtool.asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        String type = new JsonTool( reporter.createReporter( "type" ) )
                     .asString( jobj.opt( "type" ), true );
        if ( type == null ) {
            return null;
        }
        else if ( type.equals( "Feature" ) ) {
            Geometry<?> geometry =
                GEOMETRY.decode( reporter.createReporter( "geometry" ),
                                 jobj.opt( "geometry" ), null );
            if ( geometry == null ) {
                return null;
            }
            Object crsJson = jobj.opt( "crs" );
            Crs crs = crsJson == null
                    ? null
                    : Decoders.CRS
                     .decode( reporter.createReporter( "crs" ), crsJson, null );
            Object bboxJson = jobj.opt( "bbox" );
            Bbox bbox = bboxJson == null
                  ? null
                  : Decoders.BBOX
                   .decode( reporter.createReporter( "bbox" ), bboxJson, null );
            String id = new JsonTool( reporter.createReporter( "id" ) )
                       .asStringOrNumber( jobj.opt( "id" ), true );
            Object propsJson = jobj.opt( "properties" );
            JSONObject properties =
                  propsJson == null
                ? null
                : new JsonTool( reporter.createReporter( "properties" ) )
                 .asJSONObject( propsJson );
            jtool.requireAbsent( jobj, "features" );   // RFC7946 sec 7.1
            Feature feat =
                new Feature( jobj, crs, bbox, geometry, id, properties );
            feat.setParent( parent );
            assert feat.getGeometry().getParent() == feat;
            return feat;
        }
        else {
            reporter.report( "type is \"" + type + "\" not \"Feature\"" );
            return null;
        }
    };

    /** Decoder for a FeatureCollection object. */
    public static final Decoder<FeatureCollection> FEATURE_COLLECTION =
            ( reporter, json, parent ) -> {
        JsonTool jtool = new JsonTool( reporter );
        JSONObject jobj = jtool.asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        String type = new JsonTool( reporter.createReporter( "type" ) )
                     .asString( jobj.opt( "type" ), true );
        if ( type == null ) {
            return null;
        }
        else if ( type.equals( "FeatureCollection" ) ) {
            jtool.requireAbsent( jobj, "geometry" );   // RFC7946 sec 7.1
            if ( ! ALLOW_FCG_PROPERTIES ) {
                jtool.requireAbsent( jobj, "properties" );
            }
            Field[] fields = FIELDS.decode( reporter.createReporter( "fields" ),
                                            jobj.opt( "fields" ), null );
            Map<String,Field> fieldMap = new HashMap<>();
            for ( Field field : fields ) {
                fieldMap.put( field.getName(), field );
            }
            Reporter featsReporter = reporter.createReporter( "features" );
            Feature[] features =
                createArrayDecoder( FEATURE, Feature.class )
               .decode( featsReporter, jobj.opt( "features" ), null );
            if ( features == null ) {
                return null;
            }
            Set<String> idSet = new HashSet<>();
            for ( int ifeat = 0; ifeat < features.length; ifeat++ ) {
                Feature feat = features[ ifeat ];
                Reporter featReporter = featsReporter.createReporter( ifeat );
                String id = feat.getId();
                if ( id != null ) {
                    boolean isNewId = idSet.add( id );
                    if ( ! isNewId ) {
                        featReporter.report( "id attribute not unique: "
                                           + "\"" + id + "\"" );
                    }
                }
                JSONObject props = feat.getProperties();
                if ( props != null ) {
                    Reporter propsReporter =
                        featReporter.createReporter( "properties" );
                    checkProperties( propsReporter, props, fieldMap );
                }
            }
            Object bboxJson = jobj.opt( "bbox" );
            Bbox bbox = bboxJson == null
                      ? null
                      : Decoders.BBOX
                       .decode( reporter.createReporter( "bbox" ), bboxJson,
                                null );
            Crs crs = CRS.decode( reporter.createReporter( "crs" ),
                                  jobj.opt( "crs" ), null );
            FeatureCollection fc =
                new FeatureCollection( jobj, crs, bbox, fieldMap, features );
            fc.setParent( parent );
            for ( Feature f : fc.getFeatures() ) {
                assert f.getParent() == fc;
            }
            return fc;
        }
        else {
            reporter.report( "type is \"" + type
                           + "\" not \"FeatureCollection\"" );
            return null;
        }
    };

    private static final Map<String,ShapeType<?>> shapeTypes_ =
        createShapeTypes();

    private static final Map<String,Decoder<? extends TfcatObject>>
                         tfcatDecoders_ = createTfcatDecoders();

    /** Decoder for a TFCat object. */
    public static final Decoder<TfcatObject> TFCAT =
            ( reporter, json, parent ) -> {
        JsonTool jtool = new JsonTool( reporter );
        JSONObject jobj = jtool.asJSONObject( json );
        if ( jobj == null ) {
            return null;
        }
        Reporter typeReporter = reporter.createReporter( "type" );
        String type = new JsonTool( typeReporter )
                     .asString( jobj.opt( "type" ), true );
        if ( type == null ) {
            return null;
        }
        TfcatUtil.checkOption( typeReporter, type, tfcatDecoders_.keySet() );
        Decoder<? extends TfcatObject> decoder = tfcatDecoders_.get( type );
        return decoder == null
             ? null
             : decoder.decode( reporter.createReporter( type ), jobj, parent );
    };

    /**
     * Checks that a JSON properties object has values consistent with
     * a given set of Fields.  Any discrepancies are reported through
     * the suplied reporter.
     *
     * @param  reporter  validity message destination
     * @param  properties   JSON object holding property members
     * @param  fields   field definitions
     */
    private static void checkProperties( Reporter reporter,
                                         JSONObject properties,
                                         Map<String,Field> fields ) {
        for ( String key : properties.keySet() ) {
            Field field = fields.get( key );
            Reporter propReporter = reporter.createReporter( key );
            if ( field == null ) {
                propReporter.report( "no corresponding field for property" );
            }
            else {
                Object value = properties.get( key );
                if ( !JsonTool.isNull( value ) &&
                     ( value instanceof Number || value instanceof String ) ) {
                    Datatype<?> datatype = field.getDatatype();
                    if ( ! datatype.isType( value.toString() ) ) {
                        propReporter.report( "bad " + datatype + " syntax"
                                           + " \"" + value + "\"" );
                    }
                }
            }
        }
    }

    /**
     * Prepares a map from geometry type names to coordinate object decoders.
     *
     * @return  shape decoder map
     */
    private static Map<String,ShapeType<?>> createShapeTypes() {
        Map<String,ShapeType<?>> map = new LinkedHashMap<>();
        map.put( "Point",
                 new ShapeType<Position>( POSITION,
                                          Geometry.Point::new ) );
        map.put( "MultiPoint",
                 new ShapeType<Position[]>( POSITIONS,
                                            Geometry.MultiPoint::new ) );
        map.put( "LineString",
                 new ShapeType<Position[]>( LINE_STRING,
                                            Geometry.LineString::new ) );
        map.put( "MultiLineString",
                 new ShapeType<Position[][]>( LINE_STRINGS,
                                              Geometry.MultiLineString::new ) );
        map.put( "Polygon",
                 new ShapeType<LinearRing[]>( POLYGON,
                                              Geometry.Polygon::new ) );
        map.put( "MultiPolygon",
                 new ShapeType<LinearRing[][]>( POLYGONS,
                                                Geometry.MultiPolygon::new ) );
        map.put( "GeometryCollection",
                 new ShapeType<Geometry<?>[]>( GEOMETRIES,
                                               Geometry
                                              .GeometryCollection::new ));
        return map;
    }

    /**
     * Prepares a map from TFCat object type names to TFCat object decoders.
     *
     * @return   TFCat object decoder map
     */
    private static Map<String,Decoder<? extends TfcatObject>>
            createTfcatDecoders() {
        Map<String,Decoder<? extends TfcatObject>> map = new LinkedHashMap<>();
        map.put( "Feature", FEATURE );
        map.put( "FeatureCollection", FEATURE_COLLECTION );
        for ( String geomType : shapeTypes_.keySet() ) {
            map.put( geomType, GEOMETRY );
        }
        return map;
    }

    /**
     * Returns a decoder for an array of typed objects given the decoder
     * for a scalar.
     *
     * @param   scalarDecoder  decoder for scalar
     * @param   scalarClazz    parameterised type of scalar decoder
     * @return  array decoder
     */
    private static <T> Decoder<T[]>
            createArrayDecoder( Decoder<T> scalarDecoder,
                                Class<T> scalarClazz ) {
        return ( reporter, json, parent ) -> {
            JSONArray jarray = new JsonTool( reporter ).asJSONArray( json );
            if ( jarray == null ) {
                return null;
            }
            int n = jarray.length();
            @SuppressWarnings("unchecked")
            T[] array = (T[]) Array.newInstance( scalarClazz, n );
            for ( int i = 0; i < n; i++ ) {
                T item = scalarDecoder.decode( reporter.createReporter( i ),
                                               jarray.get( i ), parent );
                if ( item == null ) {
                    return null;
                }
                array[ i ] = item;
            }
            return array;
        };
    }

    /**
     * Returns the mapping of geometry type values to ShapeTypes.
     * <p>Even though shapeTypes_ is static final, it has to be referenced
     * via a static method rather than directly in some cases
     * because of ordering constraints during static initialization.
     *
     * @return  shape type map
     */
    private static Map<String,ShapeType<?>> getShapeTypes() {
        return shapeTypes_;
    }

    /**
     * Functional interface to construct a typed geometry.
     */
    @FunctionalInterface
    private static interface GeomConstructor<S> {

        /**
         * Returns a geometry.
         *
         * @param  json  JSON object on which the geometry is based
         * @param  crs   coordinate reference system, or null
         * @param  bbox  bounding box, or null
         * @param  shape   geometric content
         * @return  new typed geometry
         */
        Geometry<S> toGeom( JSONObject json, Crs crs, Bbox bbox, S shape );
    }

    /**
     * Defines usage of one of the typed Geometry subclasses.
     */
    private static class ShapeType<S> {

        private final Decoder<S> shapeDecoder_;
        private final GeomConstructor<S> geomConstructor_;

        /**
         * Constructor.
         *
         * @param  shapeDecoder   decodes coordinates to geometry shape
         * @param  geomConstructor  packages shape into typed Geometry subclass
         */
        ShapeType( Decoder<S> shapeDecoder,
                   GeomConstructor<S> geomConstructor ) {
            shapeDecoder_ = shapeDecoder;
            geomConstructor_ = geomConstructor;
        }

        /**
         * Creates a typed geometry from a JSON object.
         *
         * @param  reporter  destination for validity messages
         * @param  json    JSON object containing coordinates member
         * @param  crs     coordinate reference system, or null
         * @param  bbox    bounding box, or null
         * @return   geometry, or null if it can't be constructed
         */
        Geometry<S> createGeometry( Reporter reporter, JSONObject json,
                                    Crs crs, Bbox bbox ) {
            final Object content;
            final Reporter contentReporter;

            // GeometryCollection is a special case, see RFC7946 sec 3.1.
            if ( shapeDecoder_ == GEOMETRIES ) {
                content = json;
                contentReporter = reporter;
            }
            else {
                content = json.opt( "coordinates" );
                if ( content == null ) {
                    reporter.report( "no coordinates" );
                    return null;
                }
                contentReporter = reporter.createReporter( "coordinates" );
            }
            S shape = shapeDecoder_.decode( contentReporter, content, null );
            return shape == null
                 ? null
                 : geomConstructor_.toGeom( json, crs, bbox, shape );
        }
    }
}
