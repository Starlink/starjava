package uk.ac.starlink.ttools.votlint;

/**
 * Element handler for TD elements.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TdHandler extends ElementHandler {

    private final boolean emptyMeansNull_;
    private final StringBuffer content_ = new StringBuffer();

    /**
     * Constructor.
     *
     * @param  emptyMeansNull  if true, zero-length TD content unconditionally
     *                         indicates a null value;
     *                         if false, zero-length TD content will be
     *                         assessed without special consideration
     */
    public TdHandler( boolean emptyMeansNull ) {
        emptyMeansNull_ = emptyMeansNull;
    }

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
                    if ( emptyMeansNull_ && content_.length() == 0 ) {
                        // no action, null is intended
                    }
                    else {
                        TableHandler table =
                            family.getAncestor( TableHandler.class );
                        long irow = table == null ? -1
                                                  : table.getCurrentRowIndex();
                        parser.checkString( content_.toString(), irow );
                    }
                }
            }
        }
        else {
            error( new VotLintCode( "TDS" ), getName() + " outside DATA" );
        }
    }
}
