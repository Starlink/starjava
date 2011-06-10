package uk.ac.starlink.ttools.votlint;

import java.util.Map;

/**
 * Element handler for elements which describe values - this is FIELD
 * and PARAM type ones.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class ParamHandler extends ElementHandler {

    private ValueParser parser_;

    public void setAttributes( Map atts ) {
        super.setAttributes( atts );

        /* Construct and store a parser which known how to check values
         * associated with this element. */
        parser_ = ValueParser.makeParser( this,
                                          getAttribute( "datatype" ),
                                          getAttribute( "arraysize" ) );
        if ( parser_ != null ) {
            parser_.setContext( getContext() );
        }
    }

    /**
     * Returns a parser which knows how to check values associated with
     * this element.
     *
     * @return   parser
     */
    public ValueParser getParser() {
        return parser_;
    }

    /**
     * Attribute checker which can check attributes which contain strings
     * of the type described by this element.
     */
    public static class ValueChecker implements AttributeChecker {
        public void check( String attValue, ElementHandler handler ) {
            ValueParser parser = ((ParamHandler) handler).getParser();
            if ( parser != null ) {
                parser.checkString( attValue );
            }
        }
    }
}
