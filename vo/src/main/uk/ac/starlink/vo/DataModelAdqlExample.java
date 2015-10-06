package uk.ac.starlink.vo;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * AdqlExample implementation that provides fixed-text examples
 * specific to a given standard declared data model.
 * The getText method returns a non-null value only if the supplied
 * TapCapability declares a data model that the example recognises.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2015
 */
public abstract class DataModelAdqlExample implements AdqlExample {

    private final String name_;
    private final String description_;
    private final URL infoUrl_;
    private final String[] textLines_;

    /**
     * Constructor.
     *
     * @param   name  example name
     * @param   description   example short description
     * @param   infoUrl  URL for explanation
     * @param   textLines  lines of ADQL text
     */
    protected DataModelAdqlExample( String name, String description,
                                    String infoUrl, String[] textLines ) {
        name_ = name;
        description_ = description;
        try {
            infoUrl_ = new URL( infoUrl );
        }
        catch ( MalformedURLException e ) {
            throw new RuntimeException( "bad url: " + infoUrl, e );
        }
        textLines_ = textLines;
    }

    /**
     * Indicates whether a given data model string is the one to which
     * this example corresponds.
     * The supplied string is the ivo-id attribute of the TAPRegExt
     * <code>dataModel</code> attribute, available from
     * {@link TapCapability#getDataModels}.
     *
     * @param  dm  data model identifier
     */
    public abstract boolean isDataModel( String dm );

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public URL getInfoUrl() {
        return infoUrl_;
    }

    public String getText( boolean lineBreaks, String lang, TapCapability tcap,
                           TableMeta[] tables, TableMeta table ) {
        if ( hasDataModel( tcap ) ) {
            if ( lineBreaks ) {
                StringBuffer sbuf = new StringBuffer();
                for ( String line : textLines_ ) {
                    sbuf.append( line )
                        .append( '\n' );
                }  
                return sbuf.toString();
            }
            else {
                StringBuffer sbuf = new StringBuffer();
                for ( String line : textLines_ ) {
                    if ( sbuf.length() != 0 ) {
                        sbuf.append( ' ' );
                    }
                    sbuf.append( line.trim().replaceAll( "--.*", "" ) );
                }
                return sbuf.toString();
            }
        }
        else {
            return null;
        }
    }

    /**
     * Determines whether the given capability object declares support for
     * this example's data model.
     *
     * @param   tcap  capability speicfication object
     * @return   true iff tcap declares support for this example's model
     */
    private boolean hasDataModel( TapCapability tcap ) {
        if ( tcap != null ) {
            String[] dms = tcap.getDataModels();
            if ( dms != null ) {
                for ( String dm : dms ) {
                    if ( isDataModel( dm ) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a list of examples applicable for the RegTAP data model.
     * This list is taken from section 10 of RegTAP 1.0.
     *
     * @return   example list
     * @see   <a href="http://www.ivoa.net/documents/RegTAP/>RegTAP 1.0</a>
     */
    public static DataModelAdqlExample[] createRegTapExamples() {
        return new DataModelAdqlExample[] {

            createRegTapExample(
                "TAP accessURLs",
                "Find all TAP services, return their accessURL strings",
                "tth_sEc10.1",
                new String[] {
                    "SELECT ivoid, access_url",
                    "FROM rr.capability",
                    "NATURAL JOIN rr.interface",
                    "WHERE standard_id like 'ivo://ivoa.net/std/tap%'",
                    "  AND intf_type='vs:paramhttp'",
                }
            ),

            createRegTapExample(
                "SIA Services with Spirals",
                "Find all Simple Image Access services "
                + "that might have spiral galaxies",
                "tth_sEc10.2",
                new String[] {
                    "SELECT ivoid, access_url",
                    "FROM rr.capability",
                    "  NATURAL JOIN rr.resource",
                    "  NATURAL JOIN rr.interface",
                    "  NATURAL JOIN rr.res_subject",
                    "WHERE standard_id='ivo://ivoa.net/std/sia'",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND (",
                    "    1=ivo_nocasematch(res_subject, '%spiral%')",
                    "    OR 1=ivo_hasword(res_description, 'spiral')",
                    "    OR 1=ivo_hasword(res_title, 'spiral'))",
                }
            ),

            createRegTapExample(
                "Infrared SIA Services",
                "Find all Simple Image Access services "
                + "that provide infrared images",
                "tth_sEc10.3",
                new String[] {
                    "SELECT ivoid, access_url",
                    "FROM rr.capability",
                    "  NATURAL JOIN rr.resource",
                    "  NATURAL JOIN rr.interface",
                    "WHERE standard_id='ivo://ivoa.net/std/sia'",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND 1=ivo_hashlist_has('infrared', waveband)",
                }
            ),

            createRegTapExample(
                "Cone Searches with Redshifts",
                "Find all cone search services "
                + " that provide a column containing redshifts",
                "tth_sEc10.4",
                new String[] {
                    "SELECT ivoid, access_url",
                    "FROM rr.capability",
                    "  NATURAL JOIN rr.table_column",
                    "  NATURAL JOIN rr.interface ",
                    "WHERE standard_id='ivo://ivoa.net/std/conesearch'",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND ucd='src.redshift'",
                }
            ),

            createRegTapExample(
                "Names from an Authority",
                "Find all the resources published by a certain authority",
                "tth_sEc10.5",
                new String[] {
                    "SELECT ivoid",
                    "FROM rr.resource",
                    "WHERE ivoid LIKE 'ivo://org.gavo.dc%'",
                }
            ),

            createRegTapExample(
                "Records Published by X",
                "What registry records are there from a given publisher?",
                "tth_sEc10.6",
                new String[] {
                    "SELECT ivoid",
                    "FROM rr.res_role",
                    "WHERE 1=ivo_nocasematch(role_name, '%gavo%')",
                    "  AND base_role='publisher'",
                }
            ),

            createRegTapExample(
                "Records from Registry",
                "What registry records are there originating from registry X?",
                "tth_sEc10.7",
                new String[] {
                    "SELECT ivoid FROM rr.resource",
                    "RIGHT OUTER JOIN (",
                    "  SELECT 'ivo://' || detail_value || '%' AS pat",
                    "    FROM rr.res_detail",
                    "  WHERE detail_xpath='/managedAuthority'",
                    "    AND ivoid='ivo://cds.vizier/registry')",
                    "  AS authpatterns",
                    "ON (resource.ivoid LIKE authpatterns.pat)",
                }
            ),

            createRegTapExample(
                "Locate RegTAP Services",
                "Find all TAP endpoints offering the relational registry",
                "tth_sEc10.8",
                new String[] {
                    "SELECT access_url",
                    "FROM rr.interface",
                    "NATURAL JOIN rr.capability",
                    "NATURAL JOIN rr.res_detail",
                    "WHERE standard_id='ivo://ivoa.net/std/tap'",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND detail_xpath='/capability/dataModel/@ivo-id'",
                    "  AND 1=ivo_nocasematch(detail_value, ",
                    "    'ivo://ivoa.net/std/regtap#1.%')",
                }
            ),

            createRegTapExample(
                "TAP with Physics",
                "Find all TAP services exposing a table with certain features",
                "tth_sEc10.9",
                new String[] {
                    "SELECT ivoid, access_url, name, ucd, column_description",
                    "FROM rr.capability",
                    "  NATURAL JOIN rr.interface",
                    "  NATURAL JOIN rr.table_column",
                    "  NATURAL JOIN rr.res_table",
                    "WHERE standard_id='ivo://ivoa.net/std/tap'",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND 1=ivo_hasword(table_description, 'quasar')",
                    "  AND ucd='phot.mag;em.opt.v'",
                }
            ),

            createRegTapExample(
                "Theoretical SSA",
                "Find all SSAP services that provide theoretical spectra",
                "tth_sEc10.10",
                new String[] {
                    "SELECT access_url",
                    "FROM rr.res_detail",
                    "  NATURAL JOIN rr.capability",
                    "  NATURAL JOIN rr.interface ",
                    "WHERE detail_xpath='/capability/dataSource' ",
                    "  AND intf_type='vs:paramhttp'",
                    "  AND standard_id='ivo://ivoa.net/std/ssa'",
                    "  AND detail_value='theory'",
                }
            ),

            createRegTapExample(
                "Find Contact Persons",
                "The service at a given access URL is down, who can fix it?",
                "tth_sEc10.11",
                new String[] {
                    "SELECT DISTINCT base_role, role_name, email",
                    "FROM rr.res_role",
                    "  NATURAL JOIN rr.interface",
                    "WHERE access_url='http://dc.zah.uni-heidelberg.de/tap'",
                }
            ),

            createRegTapExample(
                "Related Capabilities",
                "Get the capabilities of all services serving a specific "
                + "resource (typically, a data collection)",
                "tth_sEc10.12",
                new String[] {
                    "SELECT *",
                    "FROM rr.relationship AS a",
                    "  JOIN rr.capability AS b",
                    "    ON (a.related_id=b.ivoid)",
                    "WHERE relationship_type='served-by'",
                }
            ),
        };
    }

    /**
     * Returns a list of examples applicable for the ObsCore data model.
     * These examples are written with reference to Appendix A of ObsCore 1.0,
     * with a few alterations and corrections.
     *
     * @return   example list
     * @see   <a href="http://www.ivoa.net/documents/ObsCore/">ObsCore 1.0</a>
     */
    public static DataModelAdqlExample[] createObsTapExamples() {
        return new DataModelAdqlExample[] {

            createObsTapExample(
                "By Position",
                "Find all observations that contain a given sky position",
                new String[] {
                    "SELECT * FROM ivoa.Obscore",
                    "WHERE CONTAINS(POINT('ICRS', 16.0, 40.0),s_region)=1",
                }
            ),

            // this one fixed - ObsTAP omits required parentheses here
            createObsTapExample(
                "By Spatial and Spectral",
                "Find image observations with constraints on position, "
                + "spatial resolution and filter",
                new String[] {
                    "SELECT * FROM ivoa.Obscore",
                    "WHERE dataproduct_type='image'",
                    "  AND s_resolution < 0.3",
                    "  AND s_ra BETWEEN 240 AND 255",
                    "  AND s_dec BETWEEN 10 AND 11",
                    "  AND ((em_min >= 2.1e-06 AND em_max <= 2.4e-06) OR",
                    "       (em_min >= 1.6e-06 AND em_max <= 1.8e-06) OR",
                    "       (em_min >= 1.2e-06 AND em_max <= 1.4e-06))",
                }
            ),

            createObsTapExample(
                "By Spatial, Spectral, Exposure",
                "Find observatrinos with constraints on position, "
                + "band and exposure time",
                new String[] {
                    "SELECT * FROM ivoa.Obscore",
                    "WHERE em_min < 2.48E-10 AND em_max > 2.48E-10",
                    " AND CONTAINS(POINT('ICRS',16.0,10.0),s_region) = 1",
                    " AND t_exptime > 10000",
                }
            ),

            createObsTapExample(
                "By Spectral, Resolution, Exposure",
                "Find spectral observations with constraints on waveband, "
                + "resolution and exposure time",
                new String[] {
                    "SELECT * from ivoa.Obscore",
                    "WHERE dataproduct_type='spectrum'",
                    "  AND em_min < 650E-9",
                    "  AND em_max > 650E-9",
                    "  AND em_res_power > 6500/15.",
                    "  AND s_resolution < 2",
                    "  AND t_exptime > 3600",
                }
            ),
        };
    }

    /**
     * Constructs an example applicable to the RegTAP data model.
     *
     * @param   name  example name
     * @param   description   example short description
     * @param   textLines  lines of ADQL text
     * @return   example
     */
    private static DataModelAdqlExample
            createRegTapExample( String name, String description,
                                 String regtapFrag, String[] textLines ) {
        String regtapUrl =
            "http://www.ivoa.net/documents/RegTAP/20141208/REC-RegTAP-1.0.html";
        String url = regtapFrag != null ? ( regtapUrl + "#" + regtapFrag )
                                        : null;
        return new DataModelAdqlExample( name, description, url, textLines ) {
            public boolean isDataModel( String dm ) {
                return dm.toLowerCase()
                         .startsWith( "ivo://ivoa.net/std/regtap" );
            }
        };
    }

    /**
     * Constructs an example applicable to the ObsCore data model.
     *
     * @param   name  example name
     * @param   description   example short description
     * @param   textLines  lines of ADQL text
     * @return   example
     */
    private static DataModelAdqlExample
            createObsTapExample( String name, String description,
                                 String[] textLines ) {
        String obstapUrl = "http://www.ivoa.net/documents/ObsCore/index.html";
        return new DataModelAdqlExample( name, description, obstapUrl,
                                         textLines ) {
            public boolean isDataModel( String dm ) {
                return dm.toLowerCase()
                         .startsWith( "ivo://ivoa.net/std/obscore" );
            }
        };
    }
}
