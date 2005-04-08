package uk.ac.starlink.ttools.lint;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for attribute checkers.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class CheckerFactory {

    private final LintContext context_;
    private final Map checkerMaps_ = new HashMap();

    /**
     * Constructor.
     *
     * @param  context  context within which this factory will work
     */
    public CheckerFactory( LintContext context ) {
        context_ = context;
    }

    /**
     * Returns a map of attname->AttributeChecker objects for a named type
     * of element.  A new map is only constructed if this method hasn't
     * been called with the same argument before.
     * 
     * @param  name  local name of the VOTable element type
     * @return  attribute name -> attribute checker map for checkable attributes
     */
    public Map getAttributeCheckers( String name ) {
        if ( ! checkerMaps_.containsKey( name ) ) {
            checkerMaps_.put( name, createAttributeCheckers( name ) );
        }
        return (Map) checkerMaps_.get( name );
    }

    /**
     * Constructs an attname->AttributeChecker map for objects which can
     * check attribute values in elements of a named type.
     * These checkers only perform the sort of checks which are not
     * done by normal XML validation (against a DTD, or possibly schema).
     *
     * @param  name  local name of the VOTable element type
     * @return  attribute name -> attribute checker map for checkable attributes
     */
    private Map createAttributeCheckers( String name ) {
        Map map = new HashMap();
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
            map.put( "ref", new RefChecker( "COOSYS" ) );
        }
        else if ( "FIELDref".equals( name ) ) {
            map.put( "ref", new FieldRefChecker() );
        }
        else if ( "FITS".equals( name ) ) {
        }
        else if ( "GROUP".equals( name ) ) {
            hasID = true;
            hasName = true;
            map.put( "ref", new RefChecker( new String[] { "GROUP",
                                                           "COOSYS" } ) );
        }
        else if ( "INFO".equals( name ) ) {
            hasID = true;
            hasName = true;
        }
        else if ( "LINK".equals( name ) ) {
            hasID = true;
            if ( ! LintContext.V10.equals( context_.getVersion() ) ) {
                map.put( "gref", new DeprecatedAttChecker( "gref" ) );
            }
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
            map.put( "ref", new RefChecker( "COOSYS" ) );
        }
        else if ( "PARAMref".equals( name ) ) {
            map.put( "ref", new RefChecker( "PARAM" ) );
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
        }
        else if ( "TABLEDATA".equals( name ) ) {
        }
        else if ( "TD".equals( name ) ) {
            map.put( "ref", new RefChecker( new String[ 0 ] ) );
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
