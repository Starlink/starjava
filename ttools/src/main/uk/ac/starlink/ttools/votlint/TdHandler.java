package uk.ac.starlink.ttools.votlint;

/**
 * Element handler for TD elements.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TdHandler extends ElementHandler {

    private final StringBuffer content_ = new StringBuffer();

    public void characters( char[] ch, int start, int length ) {
        content_.append( ch, start, length );
    }

    public void endElement() {
        Ancestry family = getAncestry();
        DataHandler data = family.getAncestor( DataHandler.class );
        if ( data != null ) {
            FieldHandler field = data.getField( family.getSiblingIndex() );
            if ( field != null ) {
                ValueParser parser = field.getParser();
                if ( parser != null ) {
                    parser.checkString( content_.toString() );
                }
            }
        }
        else {
            error( new VotLintCode( "TDS" ), getName() + " outside DATA" );
        }
    }
}
