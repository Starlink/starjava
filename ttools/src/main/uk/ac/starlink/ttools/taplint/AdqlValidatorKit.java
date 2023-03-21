package uk.ac.starlink.ttools.taplint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.vo.AdqlValidator;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapLanguage;

/**
 * Supplies ADQL validator instances for given requested languages.
 *
 * @author   Mark Taylor
 * @since    20 May 2016
 */
abstract class AdqlValidatorKit {

    private final AdqlValidator.ValidatorTable[] vtables_;
    private static final String ADQL_ID = "ivo://ivoa.net/std/ADQL";

    /**
     * Constructor.
     *
     * @param   vtables  list of validator tables known;
     *                   null means no restrictions
     */
    protected AdqlValidatorKit( AdqlValidator.ValidatorTable[] vtables ) {
        vtables_ = vtables;
    }

    /**
     * Returns a validator instance for validating against an explicitly
     * declared language identifier.
     *
     * @param  langId   language identifier for which validation is to be done;
     *                  may be null to get some kind of default
     */
    public abstract AdqlValidator getValidator( String langId );

    /**
     * Indicates whether a named table is allowed by this validator.
     * If the validator has a null list of tables, true is always returned.
     *
     * @param  tableName  table name symbol
     * @return  true if this validator will pass use of the named table
     *               int validation attempts without an error
     */
    public boolean hasTable( String tableName ) {
        if ( vtables_ == null ) {
            return true;
        }
        else {
            if ( tableName != null ) {
                for ( AdqlValidator.ValidatorTable vtable : vtables_ ) {
                    if ( tableName.equalsIgnoreCase( vtable.getTableName() ) ) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Returns a validator that just validates standard ADQL syntax.
     * No language-specific or (meta)data-specific validation is performed.
     *
     * @return  vanilla syntax-only ADQL validator
     */
    public AdqlValidator getSyntaxValidator() {
        return AdqlValidator.createValidator();
    }

    /**
     * Constructs a validator kit based on a given set of table metadata
     * and service capabilities.
     *
     * @param  reporter   reporter
     * @param  smetas   metadata for available tables
     * @param  tcap   service capability information
     * @return   best effort validator kit, not null
     */
    public static AdqlValidatorKit createInstance( final Reporter reporter,
                                                   SchemaMeta[] smetas,
                                                   TapCapability tcap ) {

        /* Prepare available table metadata in a form suitable
         * for feeding to an AdqlValidator. */
        final AdqlValidator.ValidatorTable[] vtables;
        if ( smetas == null ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata "
                           + "(earlier stages failed/skipped?) "
                           + "example parsing will not check "
                           + "table/column ids" );
            vtables = null;
        }
        else {
            List<AdqlValidator.ValidatorTable> vtlist =
                new ArrayList<AdqlValidator.ValidatorTable>();
            for ( SchemaMeta smeta : smetas ) {
                final String sname = smeta.getName();
                for ( TableMeta tmeta : smeta.getTables() ) {
                    final String tname = tmeta.getName();
                    final List<String> cnames = new ArrayList<String>();
                    for ( ColumnMeta cmeta : tmeta.getColumns() ) {
                        cnames.add( cmeta.getName() );
                    }
                    vtlist.add( new AdqlValidator.ValidatorTable() {
                        public String getSchemaName() {
                            return sname;
                        }
                        public String getTableName() {
                            return tname;
                        }
                        public Collection<String> getColumnNames() {
                            return Collections.unmodifiableList( cnames );
                        }
                    } );
                }
            }
            vtables = vtlist.toArray( new AdqlValidator.ValidatorTable[ 0 ] );
        }

        /* Determine available TapLanguages; these contain information about
         * available geometry functions, UDFs etc. */
        final TapLanguage[] langs;
        if ( tcap == null ) {
            reporter.report( FixedCode.I_EXA2,
                             "No table capabilities available, "
                           + "assuming vanilla ADQL-2.0 for examples" );
            langs = null;
        }
        else {
            langs = tcap.getLanguages();
            if ( langs == null || langs.length == 0 ) {
                reporter.report( FixedCode.I_EXA2,
                                 "No TAP languages declared, "
                               + "assuming vanilla ADQL-2.0 for examples" );
            }
        }

        /* Construct and return a validator kit that can examine a
         * supplied language identifier and return a suitable validator
         * instance. */
        if ( langs == null || langs.length == 0 ) {
            return new AdqlValidatorKit( vtables ) {
                public AdqlValidator getValidator( String langId ) {
                    return AdqlValidator.createValidator( vtables );
                }
            };
        }
        else if ( langs.length == 1 ) {
            final TapLanguage lang0 = langs[ 0 ];
            return new AdqlValidatorKit( vtables ) {
                public AdqlValidator getValidator( String langId ) {
                    return AdqlValidator.createValidator( vtables, lang0 );
                }
            };
        }
        else {
            final TapLanguage lang0 = getDefaultLanguage( langs );
            return new AdqlValidatorKit( vtables ) {
                public AdqlValidator getValidator( String langId ) {
                    final TapLanguage lang;
                    if ( langId == null ) {
                        reporter.report( FixedCode.I_EXA2,
                                         "Example language not specified, "
                                       + "using " + langToString( lang0 ) );
                        lang = lang0;
                    }
                    else {
                        TapLanguage nlang = getNamedLanguage( langs, langId );
                        if ( nlang != null ) {
                            lang = nlang;
                        }
                        else {
                            reporter.report( FixedCode.W_EXUL,
                                             "Specified language \"" + langId
                                           + "\" undeclared in capabilities, "
                                           + "using " + langToString( lang0 ) );
                            lang = lang0;
                        }
                    }
                    return AdqlValidator.createValidator( vtables, lang );
                }
            };
        }
    }

    /**
     * Returns the most obvious TapLanguage to use from a list of available
     * ones.
     * Currently returns the highest reported version of ADQL if applicable,
     * otherwise just returns the first one in the list.
     *
     * @param   langs  non-empty list of known languages
     * @return   default
     */
    private static TapLanguage getDefaultLanguage( TapLanguage[] langs ) {
        TapLanguage latestAdql = null;
        double latestAdqlVersion = 0;
        for ( TapLanguage lang : langs ) {
            double avers = getAdqlVersion( lang );
            if ( avers > latestAdqlVersion ) {
                latestAdqlVersion = avers;
                latestAdql = lang;
            }
        }
        return latestAdql == null ? langs[ 0 ] : latestAdql;
    }

    /**
     * Extracts a TapLanguage element from a list, by using a
     * string specifier.  The string is assumed to have the form used
     * by the TAP LANG parameter, that is language[-version].
     * Or it can be an ivo-id for the language.
     *
     * @param  langId   name or identifier for language, not null
     * @return   named language, or null of none matches
     */
    private static TapLanguage getNamedLanguage( TapLanguage[] langs,
                                                 String langId ) {
        String langName;
        String langVers;
        int dashPos = langId.indexOf( '-' );
        if ( dashPos >= 0 ) {
            langName = langId.substring( 0, dashPos );
            langVers = langId.substring( dashPos + 1 );
        }
        else {
            langName = langId;
            langVers = null;
        }
        for ( TapLanguage lang : langs ) {

            /* Look for ivo-id exact match. */
            for ( String vid : lang.getVersionIds() ) {
                if ( langId.equals( vid ) ) {
                    return lang;
                }
            }

            /* Otherwise look for an exact match with the language name. */
            if ( langId.equals( lang.getName() ) ) {
                return lang;
            }

            /* Otherwise look for an exact match with language name-version. */
            if ( langName.equals( lang.getName() ) ) {
                for ( String vers : lang.getVersions() ) {
                    if ( langVers.equals( vers ) ) {
                        return lang;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of a given TapLanguage.
     * That includes all known versions for clarity.
     *
     * @param  lang  language object
     * @return  string representation containing version information
     */
    private static String langToString( TapLanguage lang ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( lang.getName() );
        String[] versions = lang.getVersions();
        if ( versions == null || versions.length == 0 ) {
        }
        else if ( versions.length == 1 ) {
            sbuf.append( "-" )
                .append( versions[ 0 ] );
        }
        else {
            sbuf.append( "-{" );
            for ( int i = 0; i < versions.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( "," );
                }
                sbuf.append( versions[ i ] );
            }
            sbuf.append( "}" );
        }
        return sbuf.toString();
    }

    /**
     * Returns the ADQL version number of a TapLanguage.
     * If no ADQL version number can definitely be determined,
     * NaN is returned.
     *
     * @param   lang  language object
     * @return   ADQL version number as a double ("2.0" = 2.0),
     *           or NaN if not sure
     */
    private static double getAdqlVersion( TapLanguage lang ) {
        List<String> vstrs = new ArrayList<String>();
        if ( "ADQL".equalsIgnoreCase( lang.getName() ) ) {
            vstrs.addAll( Arrays.asList( lang.getVersions() ) );
        }
        for ( String vid : lang.getVersionIds() ) {
            if ( vid.startsWith( ADQL_ID + "#" ) ) {
                vstrs.add( vid.substring( ADQL_ID.length() + 1 ) );
            }
        }
        double maxVers = 0;
        for ( String vstr : vstrs ) {
            double dv;
            try {
                dv = Double.parseDouble( vstr );
            }
            catch ( NumberFormatException e ) {
                dv = 0;
            }
            maxVers = Math.max( maxVers, dv );
        }
        return maxVers > 0 ? maxVers : Double.NaN;
    }
}
