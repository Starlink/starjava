package uk.ac.starlink.ttools.build;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * Abstract superclass for doclets which document the static public members
 * of classes which are to be made available at runtime in TOPCAT 
 * using JEL.
 * This class deals with going through the root document as presented
 * by the (generic) Doclet application and presenting the useful bits
 * (mainly: static public methods and fields) for output.
 * Concrete subclasses must implement methods to do the actual output.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Sep 2004
 */
public abstract class MemberDoclet {

    private final RootDoc root_;
    private final Set packages_ = new HashSet();

    private static final Pattern P_PATTERN = 
        Pattern.compile( "\\s*(</*[Pp]>)?\\s+(<[Pp]>)\\s*" );
    private static final Map TYPE_NAMES;
    static {
        TYPE_NAMES = new HashMap();
        TYPE_NAMES.put( byte.class.getName(),
                        "byte" );
        TYPE_NAMES.put( short.class.getName(),
                        "short integer" );
        TYPE_NAMES.put( int.class.getName(),
                        "integer" );
        TYPE_NAMES.put( long.class.getName(),
                        "long integer" );
        TYPE_NAMES.put( float.class.getName(),
                        "floating point" );
        TYPE_NAMES.put( double.class.getName(),
                        "floating point" );
        TYPE_NAMES.put( Byte.class.getName(),
                        "byte" );
        TYPE_NAMES.put( Short.class.getName(),
                        "short integer" );
        TYPE_NAMES.put( Integer.class.getName(),
                        "integer" );
        TYPE_NAMES.put( Long.class.getName(),
                        "long integer" );
        TYPE_NAMES.put( Float.class.getName(),
                        "floating point" );
        TYPE_NAMES.put( Double.class.getName(),
                        "floating point" );
        TYPE_NAMES.put( String.class.getName(),
                        "String" );
    }

    /**
     * Begin output of documentation for a given class.
     * Subsequent calls to <tt>outItem</tt> refer to this.
     *
     * @param  clazz  class to document
     */
    protected abstract void startClass( ClassDoc clazz ) throws IOException;

    /**
     * End output of documentation for the most recently started class.
     */
    protected abstract void endClass() throws IOException;

    /**
     * Begin output of documentation for a given clas member (field or method).
     * Subsequent calls to <tt>outItem</tt> etc refer to this.
     *
     * @param  mem  class member
     * @param  memType  some user-viewable (that is not necessarily using
     *         technical terms) description of what kind of member it is 
     * @param  memName  some user-viewable label for the member
     */
    protected abstract void startMember( MemberDoc mem, String memType,
                                         String memName ) throws IOException;

    /**
     * End output of the most recently started member.
     */
    protected abstract void endMember() throws IOException;

    /**
     * Output an item to the current documentandum (class/member).
     *
     * @param  name  item title
     * @param  val   item content (HTML text)
     */
    protected abstract void outItem( String name, String val )
            throws IOException;

    /**
     * Output parameters to the current documentandum (presumably a method).
     *
     * @param   params  array of Parameter objects
     * @param   comments  array of comment strings matching <tt>params</tt>;
     *          if there's no comment, the element may be null
     */
    protected abstract void outParameters( Parameter[] params,
                                           String[] comments )
            throws IOException;

    /**
     * Output return value for the current method.
     *
     * @param   rtype  type of return value
     * @param   rdesc  text of return value description (may be null)
     */
    protected abstract void outReturn( Type rtype, String rdesc )
            throws IOException;

    /**
     * Output examples of the current documentandum (presumably a method).
     *
     * @param  examples  array of strings each representing an example
     */
    protected abstract void outExamples( String[] examples ) throws IOException;

    /**
     * Output a description item.
     *
     * @param  descrip  description string
     */
    protected abstract void outDescription( String descrip ) throws IOException;

    /**
     * Constructor.
     */
    protected MemberDoclet( RootDoc root ) {
        root_ = root;
    }

    /**
     * Works through the root document invoking the various protected methods
     * to produce output.
     */
    protected boolean process() throws IOException {
        ClassDoc[] classes = root_.classes();
        for ( int i = 0; i < classes.length; i++ ) {
            ClassDoc clazz = classes[ i ];
            if ( clazz.isPublic() ) {
                processClass( clazz );
                if ( ! packages_.contains( clazz ) ) {
                    processPackage( clazz.containingPackage() );
                }
            }
        }
        return true;
    }

    /**
     * Generates documentation for a given package.
     *
     * @param  pack  package 
     */
    protected void processPackage( PackageDoc pack ) throws IOException {
        // no action
    }

    /**
     * Generates documentation for a given class.
     *
     * @param  clazz  class
     */
    protected void processClass( ClassDoc clazz ) throws IOException {
        startClass( clazz );
        FieldDoc[] fields = clazz.fields();
        for ( int i = 0; i < fields.length; i++ ) {
            FieldDoc field = fields[ i ];
            if ( field.isPublic() && field.isStatic() && field.isFinal() ) {
                processField( field );
            }
        }
        MethodDoc[] methods = clazz.methods();
        for ( int i = 0; i < methods.length; i++ ) {
            MethodDoc method = methods[ i ];
            if ( method.isPublic() && method.isStatic() ) {
                processMethod( method );
            }
        }
        endClass();
    }

    /**
     * Generates documentation for a given field.
     *
     * @param  field  field
     */
    private void processField( FieldDoc field )
            throws IOException {
        ClassDoc clazz = field.containingClass();
        startMember( field, "Constant", field.name() );
        outDescription( field.commentText() );
        outItem( "Type", typeString( field.type() ) );
        Object value = field.constantValue();
        if ( value != null ) {
            outItem( "Value", value.toString() );
        }
        endMember();
    }

    /**
     * Generates documentation for a given method.
     *
     * @param  method   method
     */
    private void processMethod( MethodDoc method )
            throws IOException {
        ClassDoc clazz = method.containingClass();

        /* Prepare parameter list. */
        Parameter[] params = method.parameters();
        StringBuffer paramList = new StringBuffer( "( " );
        for ( int i = 0; i < params.length; i++ ) {
            String pname = params[ i ].name();
            if ( i > 0 ) {
                paramList.append( ", " );
            }
            paramList.append( pname );
        }
        paramList.append( " )" );

        /* Prepare parameter tag comments. */
        ParamTag[] ptags = method.paramTags();
        String[] comments = new String[ params.length ];
        for ( int i = 0; i < params.length; i++ ) {
            String pname = params[ i ].name();
            for ( int j = 0; j < ptags.length; j++ ) {
                if ( pname.equals( ptags[ j ].parameterName() ) ) {
                    if ( comments[ i ] == null ) {
                        comments[ i ] = ptags[ j ].parameterComment();
                    }
                    else {
                        warning( clazz + "." + method + ": " +
                                 "multiple tags for parameter " + pname );
                    }
                }
            }
            if ( comments[ i ] == null ) {
                warning( clazz + "." + method + ": " +
                         "no tag for parameter " + pname );
            }
        }

        /* Get return value description. */
        String retdesc = null;
        boolean isVoid = 
            "void".equals( method.returnType().qualifiedTypeName() );
        Tag[] retags = method.tags( "return" );
        if ( ! isVoid ) {
            if ( retags.length != 1 ) {
                warning( clazz + "." + method + ": " +
                         retags.length + " @return tags" );
            }
            else {
                retdesc = retags[ 0 ].text();
            }
        }

        /* Get examples. */
        Tag[] extags = method.tags( "example" );
        String[] examples = new String[ extags.length ];
        for ( int i = 0; i < extags.length; i++ ) {
            examples[ i ] = extags[ i ].text();
        }

        /* Output information. */
        Type rtype = method.returnType();
        startMember( method, "Function", method.name() + paramList );
        outDescription( method.commentText() );
        outParameters( params, comments );
        if ( ! isVoid ) {
            outReturn( rtype, retdesc );
        }
        if ( examples.length > 0 ) {
            outExamples( examples );
        }
        outItem( "Signature", 
                 "<tt>" + rtype.toString().replaceAll( "^.*\\.", "" )
                        + " " 
                        + method.name()
                        + method.signature()
                                .replaceAll( "([ \\(])[a-zA-Z0-9\\.]*\\.",
                                             "$1" )
                        + "</tt>" );
        endMember();
    }

    /**
     * Returns a string suitable for user consumption which describes a Type.
     *
     * @param  type  type
     * @return  string representation of type (non-technical?)
     */
    public static String typeString( Type type ) {
        String pre = type.dimension().replaceAll( "\\[\\]", "array of " );
        String fqname = type.qualifiedTypeName();
        if ( TYPE_NAMES.containsKey( fqname ) ) {
            return pre + (String) TYPE_NAMES.get( fqname );
        }
        else {
            return pre + type.typeName();
        }
    }

    /** 
     * Ensures that a string is a sequence of &lt;p&gt; elements
     * (though it's not foolproof).
     *
     * @param  text  basic text
     * @return  same as <tt>text</tt> but a sequence of HTML P elements
     */
    public static String pWrap( String text ) {
        String[] params = P_PATTERN.split( text );
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < params.length; i++ ) {
            sbuf.append( "<p>" + params[ i ] + "</p>\n" );
        }
        return sbuf.toString();
    }

    /**
     * Log a warning.
     *
     * @param msg  message
     */
    public static void warning( String msg ) {
        System.err.println( msg );
    }

}
