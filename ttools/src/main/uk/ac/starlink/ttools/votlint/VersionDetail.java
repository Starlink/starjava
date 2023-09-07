package uk.ac.starlink.ttools.votlint;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * Contains VOTable version-specific validation logic.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2012
 */
public abstract class VersionDetail {

    private final VOTableVersion version_;
    private final Map<String,Map<String,AttributeChecker>> checkersMap_;

    private static final VersionDetail V10;
    private static final VersionDetail V11;
    private static final VersionDetail V12;
    private static final VersionDetail V13;
    private static final VersionDetail V14;
    private static final VersionDetail V15;
    private static final VersionDetail DUMMY = new DummyVersionDetail();
    private static final Map<VOTableVersion,VersionDetail> VERSION_MAP =
            createMap( new VersionDetail[] {
        V10 = new VersionDetail10( VOTableVersion.V10 ),
        V11 = new VersionDetail11( VOTableVersion.V11 ),
        V12 = new VersionDetail12( VOTableVersion.V12 ),
        V13 = new VersionDetail13( VOTableVersion.V13 ),
        V14 = new VersionDetail14( VOTableVersion.V14 ),
        V15 = new VersionDetail15( VOTableVersion.V15 ),
    } );

    /**
     * Constructor.
     *
     * @param   version   VOTable version to which this detail applies
     */
    protected VersionDetail( VOTableVersion version ) {
        version_ = version;
        checkersMap_ = new HashMap<String,Map<String,AttributeChecker>>();
    }

    /**
     * Returns a map of attribute checkers suitable for processing
     * elements of a given name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @return  String-&gt;AttributeChecker map for checking attributes
     */
    public Map<String,AttributeChecker>
            getAttributeCheckers( String voTagname ) {
        if ( ! checkersMap_.containsKey( voTagname ) ) {
            checkersMap_.put( voTagname, createAttributeCheckers( voTagname ) );
        }
        return checkersMap_.get( voTagname );
    }

    /**
     * Constructs a new ElementHandler for a given local element name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @param   context   processing context
     * @return   handler to process an element of type <tt>name</tt>
     */
    public ElementHandler createElementHandler( String voTagname,
                                                VotLintContext context ) {
        if ( voTagname == null ) {
            throw new NullPointerException();
        }
        else {
            ElementHandler handler = createElementHandler( voTagname );
            if ( handler == null ) {
                if ( ! context.isValidating() ) {
                    context.error( new VotLintCode( "VBE" ),
                                   "Element " + voTagname
                                 + " not known at VOTable " + version_ );
                }
                handler = new ElementHandler();
            }
            handler.configure( voTagname, context );
            return handler;
        }
    }

    /**
     * Constructs a new element handler for an element with the given
     * unqualified VOTable tag name.
     *
     * @param  voTagname  unqualified element name
     * @return  element handler, or null if the element is unknown
     */
    protected abstract ElementHandler createElementHandler( String voTagname );

    /**
     * Constructs a map of attribute checkers suitable for processing
     * elements of a given name.
     *
     * @param   voTagname  unqualified element name in VOTable namespace
     * @return  String-&gt;AttributeChecker map for checking attributes
     */
    protected abstract Map<String,AttributeChecker>
            createAttributeCheckers( String voTagname );

    /**
     * Returns a VersionDetail instance suitable for use with the given
     * context.
     *
     * @param   context   validation context
     * @return  instance, not null
     */
    public static VersionDetail getInstance( VotLintContext context ) {
        VOTableVersion version = context.getVersion();
        if ( VERSION_MAP.containsKey( version ) ) {
            return VERSION_MAP.get( version );
        }
        else {
            context.warning( new VotLintCode( "UKV" ),
                             "No checking information available for version "
                           + version );
            return DUMMY;
        }
    }

    /**
     * Constructs a version->detail map from a list of detail instances.
     *
     * @param  vds  array of VersionDetail instances
     * @return  map keyed by the VOTableVersion of each instance
     */
    private static Map<VOTableVersion,VersionDetail>
            createMap( VersionDetail[] vds ) {
        Map<VOTableVersion,VersionDetail> map =
            new LinkedHashMap<VOTableVersion,VersionDetail>();
        for ( int i = 0; i < vds.length; i++ ) {
            map.put( vds[ i ].version_, vds[ i ] );
        }
        return map;
    }

    /**
     * Version implementation for VOTable 1.0.
     */
    private static class VersionDetail10 extends VersionDetail {

        VersionDetail10( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            if ( "TABLE".equals( name ) ) {
                return new TableHandler();
            }
            else if ( "PARAM".equals( name ) ) {
                return new ParamHandler();
            }
            else if ( "FIELD".equals( name ) ) {
                return new FieldHandler();
            }
            else if ( "DATA".equals( name ) ) {
                return new DataHandler();
            }
            else if ( "TR".equals( name ) ) {
                return new TrHandler();
            }
            else if ( "TD".equals( name ) ) {
                return new TdHandler();
            }
            else if ( "STREAM".equals( name ) ) {
                return new StreamHandler();
            }
            else if ( "BINARY".equals( name ) ) {
                return new BinaryHandler( false );
            }
            else if ( "FITS".equals( name ) ) {
                return new FitsHandler();
            }
            else if ( "VOTABLE".equals( name ) ||
                      "RESOURCE".equals( name ) ||
                      "DESCRIPTION".equals( name ) ||
                      "DEFINITIONS".equals( name ) ||
                      "INFO".equals( name ) ||
                      "VALUES".equals( name ) ||
                      "MIN".equals( name ) ||
                      "MAX".equals( name ) ||
                      "OPTION".equals( name ) ||
                      "LINK".equals( name ) ||
                      "TABLEDATA".equals( name ) ||
                      "COOSYS".equals( name ) ) {
                return new ElementHandler();
            }
            else {
                return null;
            }
        }

        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            Map<String,AttributeChecker> map =
                new HashMap<String,AttributeChecker>();
            boolean hasID = false;
            boolean hasName = false;
            if ( name == null ) {
                throw new NullPointerException();
            }
            else if ( "BINARY".equals( name ) ) {
            }
            else if ( "COOSYS".equals( name ) ) {
                hasID = true;
            }
            else if ( "DATA".equals( name ) ) {
            }
            else if ( "DEFINITIONS".equals( name ) ) {
            }
            else if ( "DESCRIPTION".equals( name ) ) {
            }
            else if ( "FIELD".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "ref",
                         new RefChecker( new String[] { "COOSYS", "TIMESYS",
                                                        "GROUP" } ) );
                map.put( "ucd", UcdChecker.INSTANCE );
                map.put( "unit", UnitChecker.INSTANCE );
            }
            else if ( "FITS".equals( name ) ) {
            }
            else if ( "INFO".equals( name ) ) {
                hasID = true;
                map.put( "ucd", UcdChecker.INSTANCE );
                map.put( "unit", UnitChecker.INSTANCE );

                /* INFO has a name attribute.  However, we don't set hasName
                 * here, since multiple INFOs with the same name in the same
                 * scope is probably reasonable, so we don't want to emit
                 * warnings in that case. */
            }
            else if ( "LINK".equals( name ) ) {
                hasID = true;
            }
            else if ( "MAX".equals( name ) ) {
            }
            else if ( "MIN".equals( name ) ) {
            }
            else if ( "OPTION".equals( name ) ) {
                hasName = true;
            }
            else if ( "PARAM".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "value", new ParamHandler.ValueChecker() );
                map.put( "ref",
                         new RefChecker( new String[] { "COOSYS", "TIMESYS",
                                                        "FIELD", "GROUP" } ) );
                map.put( "ucd", UcdChecker.INSTANCE );
                map.put( "unit", UnitChecker.INSTANCE );
            }
            else if ( "RESOURCE".equals( name ) ) {
                hasID = true;
                hasName = true;
            }
            else if ( "STREAM".equals( name ) ) {
            }
            else if ( "TABLE".equals( name ) ) {
                hasID = true;
                hasName = true;
                map.put( "ref", new RefChecker( "TABLE" ) );
                map.put( "nrows", new TableHandler.NrowsChecker() );
                map.put( "ucd", UcdChecker.INSTANCE );
            }
            else if ( "TABLEDATA".equals( name ) ) {
            }
            else if ( "TD".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[ 0 ] ) );
            }
            else if ( "TIMESYS".equals( name ) ) {
                hasID = true;
                map.put( "timescale",
                         new VocabAttributeChecker( VocabChecker.TIMESCALE ) );
                map.put( "refposition",
                         new VocabAttributeChecker( VocabChecker.REFPOSITION ));
            }
            else if ( "TR".equals( name ) ) {
            }
            else if ( "VALUES".equals( name ) ) {
                hasID = true;
            }
            else if ( "VOTABLE".equals( name ) ) {
                hasID = true;
                map.put( "version", new VersionChecker() );
            }

            if ( hasID ) {
                map.put( "ID", new IDChecker() );
            }
            if ( hasName ) {
                map.put( "name", new NameChecker() );
            }
            return map;
        }
    }

    /**
     * VersionDetail implementation for VOTable 1.1.
     */
    private static class VersionDetail11 extends VersionDetail {

        VersionDetail11( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            ElementHandler handler = V10.createElementHandler( name );
            if ( handler != null ) {
                return handler;
            }
            else if ( "GROUP".equals( name ) ||
                      "FIELDref".equals( name ) ||
                      "PARAMref".equals( name ) ) {
                return new ElementHandler();
            }
            else {
                return null;
            }
        }

        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            Map<String,AttributeChecker> map =
                V10.createAttributeCheckers( name );
            if ( "LINK".equals( name ) ) {
                map.put( "gref", new DeprecatedAttChecker( "gref" ) );
            }
            else if ( "FIELDref".equals( name ) ) {
                map.put( "ref", new RefChecker( "FIELD" ) );
                map.put( "ucd", UcdChecker.INSTANCE );
            }
            else if ( "GROUP".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[] { "GROUP",
                                                               "COOSYS", } ) );
                map.put( "ID", new IDChecker() );
                map.put( "name", new NameChecker() );
            }
            else if ( "PARAMref".equals( name ) ) {
                map.put( "ref", new RefChecker( "PARAM" ) );
                map.put( "ucd", UcdChecker.INSTANCE );
            }
            return map;
        }
    }

    /**
     * VersionDetail implementation for VOTable 1.2.
     */
    private static class VersionDetail12 extends VersionDetail {

        VersionDetail12( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            if ( "COOSYS".equals( name ) ) {
                return new ElementHandler() {
                    public void startElement() {
                        super.startElement();
                        info( new VotLintCode( "CD2" ),
                              "COOSYS is deprecated at VOTable 1.2"
                            + " (though reprieved at 1.3)" );
                    }
                };
            }
            else {
                return V11.createElementHandler( name );
            }
        }

        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            Map<String,AttributeChecker> map =
                V11.createAttributeCheckers( name );
            if ( "GROUP".equals( name ) ) {
                map.put( "ref", new RefChecker( new String[] { "GROUP",
                                                               "COOSYS",
                                                               "TABLE", } ) );
                map.put( "ucd", UcdChecker.INSTANCE );
            }
            return map;
        }
    }

    /**
     * VersionDetail implementation for VOTable 1.3.
     */
    private static class VersionDetail13 extends VersionDetail {

        VersionDetail13( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            ElementHandler handler = V11.createElementHandler( name );
            if ( handler != null ) {
                return handler;
            }
            else if ( "BINARY2".equals( name ) ) {
                return new BinaryHandler( true );
            }
            else {
                return null;
            }
        }

        protected Map<String,AttributeChecker>
               createAttributeCheckers( String name ) {
            Map<String,AttributeChecker> map =
                V12.createAttributeCheckers( name );
            if ( "FIELD".equals( name ) ||
                 "PARAM".equals( name ) ) {
                map.put( "arraysize", new ArraysizeChecker() );
            }
            return map;
        }
    }

    /**
     * VersionDetail implementation for VOTable 1.4.
     */
    private static class VersionDetail14 extends VersionDetail {

        VersionDetail14( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            return V13.createElementHandler( name );
        }

        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            return V13.createAttributeCheckers( name );
        }
    }

    /**
     * VersionDetail implementation for VOTable 1.5.
     */
    private static class VersionDetail15 extends VersionDetail {

        VersionDetail15( VOTableVersion version ) {
            super( version );
        }

        protected ElementHandler createElementHandler( String name ) {
            return V14.createElementHandler( name );
        }

        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            Map<String,AttributeChecker> map =
                V14.createAttributeCheckers( name );
            if ( "COOSYS".equals( name ) ) {
                map.put( "system",
                         new VocabAttributeChecker( VocabChecker.REFFRAME ) );
                map.put( "refposition",
                         new VocabAttributeChecker( VocabChecker.REFPOSITION ));
            }
            return map;
        }
    }

    private static class DummyVersionDetail extends VersionDetail {
        DummyVersionDetail() {
            super( null );
        }
        protected ElementHandler createElementHandler( String voTagName ) {
            return new ElementHandler();
        }
        protected Map<String,AttributeChecker>
                createAttributeCheckers( String name ) {
            return new HashMap<String,AttributeChecker>();
        }
    }
}
