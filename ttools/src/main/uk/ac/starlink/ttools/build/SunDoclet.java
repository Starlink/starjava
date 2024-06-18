package uk.ac.starlink.ttools.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import uk.ac.starlink.ttools.gui.DocNames;

/**
 * Doclet for documenting user-visible JEL classes,
 * based on the com.sun.javadoc API.
 
 * <p>At Java 8 and below, the <code>com.sun.javadoc</code> API is the
 * standard/only way to parse javadocs.
 * At Java 9 it is deprecated in favour of the
 * <code>jdk.javadoc.doclet</code> API, and at Java 17 it is withdrawn
 * altogether.
 *
 * <p>If the target build platform is ever moved to Java 9 or later,
 * this class should be retired in favour of {@link JdkDoclet}.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2023
 * @see   {@link JdkDoclet}
 */
public class SunDoclet {

    private final RootDoc root_;
    private final DocletOutput output_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.build" );

    /**
     * Constructor.
     *
     * @param   root  document tree root
     * @param   output   output destination
     */
    public SunDoclet( RootDoc root, DocletOutput output ) {
        root_ = root;
        output_ = output;
    }

    /**
     * Works through the root document invoking the various protected methods
     * to produce output.
     */
    public boolean process() throws IOException {
        ClassDoc[] classes = root_.classes();
        Arrays.sort( classes );
        output_.startOutput();
        for ( int i = 0; i < classes.length; i++ ) {
            ClassDoc clazz = classes[ i ];
            if ( clazz.isPublic() ) {
                processClass( clazz );
            }
        }
        output_.endOutput();
        return true;
    }

    /**
     * Generates documentation for a given class.
     *
     * @param  clazz  class
     */
    private void processClass( ClassDoc clazz ) throws IOException {
        if ( isDocumentable( clazz ) ) {
            output_.startClass( clazz.qualifiedName(), firstSentence( clazz ),
                                clazz.commentText() );
            MethodDoc[] methods = clazz.methods();
            for ( int i = 0; i < methods.length; i++ ) {
                MethodDoc method = methods[ i ];
                if ( isDocumentable( method ) ) {
                    processMethod( method );
                }
            }
            FieldDoc[] fields = clazz.fields();
            for ( int i = 0; i < fields.length; i++ ) {
                FieldDoc field = fields[ i ];
                if ( isDocumentable( field ) ) {
                    processField( field );
                }
            }
            output_.endClass();
        }
    }

    /**
     * Generates documentation for a given field.
     *
     * @param  field  field
     */
    private void processField( FieldDoc field )
            throws IOException {
        output_.startMember( field.name(), "Constant", field.name(),
                             field.commentText() );
        output_.outMemberItem( "Type", typeString( field.type() ) );
        Object value = field.constantValue();
        if ( value != null ) {
            output_.outMemberItem( "Value", value.toString() );
        }
        output_.endMember();
    }

    /**
     * Generates documentation for a given method.
     *
     * @param  method   method
     */
    private void processMethod( MethodDoc method )
            throws IOException { 
        ClassDoc clazz = method.containingClass();
        boolean isVararg = method.isVarArgs(); 
     
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
        if ( isVararg ) {
            paramList.append( ", ..." );
        }
        paramList.append( " )" );

        /* Prepare parameter tag comments. */
        ParamTag[] ptags = method.paramTags();
        List<DocletOutput.DocVariable> varList = new ArrayList<>();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            String pname = param.name();
            String comment = null;
            for ( int j = 0; j < ptags.length; j++ ) {
                if ( pname.equals( ptags[ j ].parameterName() ) ) {
                    if ( comment == null ) {
                        comment = ptags[ j ].parameterComment();
                    }
                    else {
                        logger_.warning( clazz + "." + method + ": "
                                       + "multiple tags for parameter "
                                       + pname );
                    }
                }
            }
            String comment0 = comment;
            if ( comment0 == null ) {
                logger_.warning( clazz + "." + method + ": " +
                                 "no tag for parameter " + pname );
            }
            String type =
                varargTypeString( param.type(),
                                  isVararg && i == params.length - 1 );
            varList.add( new DocletOutput.DocVariable() {
                public String getName() {
                    return pname;
                }
                public String getType() {
                    return type;
                }
                public String getCommentText() {
                    return comment0;
                }
            } );
        }
        assert varList.size() == params.length;

        /* Create unique identifier. */
        StringBuffer idBuf = new StringBuffer( method.name() );
        for ( Parameter param : params ) {
            idBuf.append( DocNames.TOKEN_SEPARATOR );
            Type type = param.type();
            idBuf.append( DocNames.typeNameToWord( type.qualifiedTypeName() ) );
            String dim = type.dimension();
            if ( dim != null ) {
                idBuf.append( dim.replaceAll( "\\[\\]",
                                              "" + DocNames.ARRAY_SUFFIX ) );
            }
        }
        String methodId = idBuf.toString();

        /* Get return value description. */
        String retdesc = null;
        boolean isVoid =
            "void".equals( method.returnType().qualifiedTypeName() );
        Tag[] retags = method.tags( "return" );
        if ( ! isVoid ) {
            if ( retags.length != 1 ) {
                logger_.warning( clazz + "." + method + ": " +
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

        /* Get see tags. */
        List<String> sees = new ArrayList<>();
        for ( Tag seeTag : method.tags( "see" ) ) {
            if ( seeTag instanceof SeeTag ) {
                sees.add( seeTag.text() );
            }
        }

        /* Output to destination. */
        Type rtype = method.returnType();
        output_.startMember( method.name() + paramList, "Function", methodId,
                             method.commentText() );
        if ( varList.size() > 0 ) {
            output_.outParameters( varList.toArray( new DocletOutput
                                                       .DocVariable[ 0 ] ) );
        }
        if ( ! isVoid ) {
            output_.outReturn( typeString( rtype ), retdesc );
        }
        if ( examples.length > 1 ) {
            output_.outExamples( "Examples", examples );
        }
        else if ( examples.length > 0 ) {
            output_.outExamples( "Example", examples );
        }
        if ( sees.size() > 0 ) {
            output_.outSees( "See Also", sees.toArray( new String[ 0 ] ) );
        }
        String signature = new StringBuffer()
            .append( "<code>" )
            .append( rtype.toString().replaceAll( "^.*\\.", "" ) )
            .append( " " )
            .append( method.name() )
            .append( method.signature()
                           .replaceAll( "\\w[\\w\\.]*\\.(\\w+)", "$1" ) )
            .append( "</code>" )
            .toString();
        output_.outMemberItem( "Signature", signature );
        output_.endMember();
    }

    /**
     * This magic static method appears to be required on Doclet classes
     * to make the <code>isVarArgs()</code> method on
     * <code>com.sun.javadoc.ExecutableMemberDoc</code> report variable
     * argument status.  I don't know whether or where that's documented,
     * but I found out from
     * <a href="http://stackoverflow.com/questions/13030271/javadoc-api-how-far-are-varargs-supported">StackOverflow</a>.
     *
     * @return   LanguageVersion.JAVA_1_5
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    /**
     * Indicates whether a given item is to be documented or ignored.
     * Currently, members that are public, static, and not marked by
     * the {@link uk.ac.starlink.ttools.build.HideDoc @HideDoc}
     * annotation are considered documentable.
     * Fields must additionally be declared final.
     *
     * @param  pel  program element
     * @return   true to process for documentation, false to skip
     */
    private static boolean isDocumentable( ProgramElementDoc pel ) {
        if ( ! pel.isPublic() ) {
            return false;
        }
        if ( ( pel instanceof FieldDoc || pel instanceof MethodDoc ) &&
             ! pel.isStatic() ) {
            return false;
        }
        if ( pel instanceof FieldDoc &&
             ! pel.isFinal() ) {
            return false;
        }
        for ( AnnotationDesc adesc : pel.annotations() ) {
            if ( HideDoc.class.getName()
                .equals( adesc.annotationType().qualifiedName() ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string suitable for user consumption which describes a
     * non-varargs Type.
     *
     * @param  type  type
     * @return  string representation of type (non-technical?)
     */
    private static String typeString( Type type ) {
        return varargTypeString( type, false );
    }

    /**
     * Returns a string suitable for user consumption which describes a
     * type that may or may not represent a variable-argument parameter.
     *
     * @param  type  type
     * @param  isVararg   true if type is known to describe a variable-argument
     *                    parameter
     * @return  string representation of type (non-technical?)
     */
    private static String varargTypeString( Type type, boolean isVararg ) {
        String tdim = type.dimension();
        if ( isVararg ) {
            if ( tdim.startsWith( "[]" ) ) {
                tdim = tdim.substring( 2 );
            }
            else {
                isVararg = false;
            }
        }
        String pre = tdim.replaceAll( "\\[\\]", "array of " );
        String post = isVararg ? ", one or more" : "";
        String typetxt =
            DocletUtil.getScalarTypeName( type.qualifiedTypeName() );
        if ( typetxt == null ) {
            typetxt = type.qualifiedTypeName().replaceFirst( ".*[.$]", "" );
        }
        return pre + typetxt + post;
    }

    /**
     * Returns the first sentence of documentation for a Doc element
     * as plain text.  Tags are ignored.
     *
     * @param  doc  documented item
     * @return  first sentence of text
     */
    private static String firstSentence( Doc doc ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Tag tag : doc.firstSentenceTags() ) {
            if ( "Text".equals( tag.kind() ) ) {
                sbuf.append( tag.text() );
            }
        }
        return sbuf.toString();
    }

    /**
     * Class for use with <code>javadoc</code> tool that writes
     * SUN-friendly XML output.
     * 
     * <p>Doclet flags:
     * <ul>
     * <li><code>-o &lt;file-name&;gt</code></li>
     * <li><code>-headonly</code></li>
     * </ul>
     */
    public static class Xml {

        /**
         * Define permitted command-line flags.
         * This method is part of the Doclet public interface.
         */
        public static int optionLength( String option ) {
            if ( option.equals( "-o" ) ) {
                return 2;
            }
            else if ( option.equals( "-headonly" ) ) {
                return 1;
            }
            else {
                return 0;
            }
        }

        /**
         * Begin processing document.
         * This method is part of the Doclet public interface.
         */
        public static boolean start( RootDoc root ) throws IOException {
            String[][] options = root.options();
            String outloc = null;
            boolean headOnly = false;
            for ( String[] opts : options ) {
                String opt = opts[ 0 ];
                if ( "-o".equals( opt ) ) {
                    outloc = opts[ 1 ];
                }
                if ( "-headonly".equals( opt ) ) {
                    headOnly = true;
                }
            }
            OutputStream outStream = ( outloc == null || "-".equals( outloc ) )
                                   ? System.out
                                   : new FileOutputStream( outloc );
            DocletOutput output =
                new XmlDocletOutput( outStream, headOnly,
                                     s -> s.replaceFirst( ".*[.]", "" ) );
            return new SunDoclet( root, output ).process();
        }

        /**
         * Required to make varargs work.
         */
        public static LanguageVersion languageVersion() {
            return SunDoclet.languageVersion();
        }
    }

    /**
     * Class for use with <code>javadoc</code> tool that writes
     * MethodBrowser-friendly HTML output.
     *
     * <p>Doclet flags:
     * <ul>
     * <li><code>-d &lt;base-dir&gt;</code></li>
     * <li><code>-headings</code></li>
     * </ul>
     */
    public static class Html {

        /**
         * Define permitted command-line flags.
         * This method is part of the Doclet public interface.
         */
        public static int optionLength( String option ) {
            if ( option.equals( "-d" ) ) {
                return 2;
            }
            else if ( option.equals( "-headings" ) ) {
                return 1;
            }
            else {
                return 0;
            }
        }

        /**
         * Begin processing document.
         * This method is part of the Doclet public interface.
         */
        public static boolean start( RootDoc root ) throws IOException {
            String[][] options = root.options();
            File baseDir = new File( "." );
            boolean isHeadings = false;
            for ( String[] opts : options ) {
                String opt = opts[ 0 ];
                if ( "-d".equals( opt ) ) {
                    baseDir = new File( opts[ 1 ] );
                }
                if ( "-headings".equals( opt ) ) {
                    isHeadings = true;
                }
            }
            DocletOutput output = new HtmlDocletOutput( baseDir, isHeadings );
            return new SunDoclet( root, output ).process();
        }

        /**
         * Required to make varargs work.
         */
        public static LanguageVersion languageVersion() {
            return SunDoclet.languageVersion();
        }
    }
}
