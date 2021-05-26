package uk.ac.starlink.ttools.votlint;

/** 
 * ElementHandler for FIELD elements.
 * It inherits from ParamHandler since the parsing functionality can be
 * reused.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class FieldHandler extends ParamHandler {

    ElementRef tableRef_;

    public void startElement() {

        /* Store in the TABLE handler information about how to parse data
         * from this column. */
        TableHandler table = getAncestry().getAncestor( TableHandler.class );
        if ( table != null ) {
            tableRef_ = table.getRef();
            table.registerField( this );
        }
        else {
            error( new VotLintCode( "DDF" ), this + " outside of TABLE" );
        }
    }

    /** 
     * Returns the table to which this field belongs.
     * 
     * @return   table ref
     */
    public ElementRef getTableRef() {
        return tableRef_;
    }
}
