package uk.ac.starlink.ttools.lint;

/**
 * Factory for creating ElementHandlers.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class HandlerFactory {

    private final LintContext context_;

    /**
     * Constructor.
     *
     * @param   context   lint context
     */
    public HandlerFactory( LintContext context ) {
        context_ = context;
    }

    /**
     * Constructs an ElementHandler for an element with a given local name.
     *
     * @param  name  local name for the element
     * @return   new handler for an element of type <tt>name</tt>
     */
    public ElementHandler createHandler( String name ) {
        ElementHandler handler;

        /* Check this element is known in this version of VOTable. */
        if ( "GROUP".equals( name ) ||
             "FIELDref".equals( name ) ||
             "PARAMref".equals( name ) ) {
            String version = context_.getVersion();
            if ( LintContext.V10.equals( version ) ) {
                context_.error( "Element " + name + " not known at " +
                                "VOTable " + version );
            }
        }

        if ( name == null ) {
            throw new NullPointerException();
        }
        else if ( "TABLE".equals( name ) ) {
            handler = new TableHandler();
        }
        else if ( "PARAM".equals( name ) ) {
            handler = new ParamHandler();
        }
        else if ( "FIELD".equals( name ) ) {
            handler = new FieldHandler();
        }
        else if ( "DATA".equals( name ) ) {
            handler = new DataHandler();
        }
        else if ( "TR".equals( name ) ) {
            handler = new TrHandler();
        }
        else if ( "TD".equals( name ) ) {
            handler = new TdHandler();
        }
        else if ( "STREAM".equals( name ) ) {
            handler = new StreamHandler();
        }
        else if ( "BINARY".equals( name ) ) {
            handler = new BinaryHandler();
        }
        else {
            handler = new ElementHandler();
        }
        handler.configure( name, context_ );
        return handler;
    }

    private void checkVersion( String version, String elName ) {
        if ( ! version.equals( context_.getVersion() ) ) {
            context_.error( "Element " + elName + " not known at VOTable " +
                            version );
        }
    }

}
