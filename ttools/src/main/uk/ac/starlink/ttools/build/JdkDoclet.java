package uk.ac.starlink.ttools.build;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import uk.ac.starlink.ttools.gui.DocNames;

/**
 * Doclet for documenting user-visible JEL library classes,
 * based on the jdk.javadoc.doclet API.
 *
 * <p>Two concrete implementations are provided as inner classes,
 * using different {@link DocletOutput} backends.
 *
 * <p>The <code>jdk.javadoc.doclet</code> API is available only at
 * Java 9 and later, so this class will not compile at earlier
 * JDK versions.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2023
 */
public abstract class JdkDoclet implements Doclet {

    private final String name_;
    private DocletOutput output_;
    private Reporter reporter_;

    /**
     * Constructor.
     *
     * @param  name  doclet name
     */
    protected JdkDoclet( String name ) {
        name_ = name;
    }

    public String getName() {
        return name_;
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public void init( Locale locale, Reporter reporter ) {
        reporter_ = reporter;
    }

    /**
     * Must provide a format-specific output backend.
     * Called during the doclet {@link #run} method,
     * after option configuration.
     *
     * @return  output instance
     */
    protected abstract DocletOutput createOutput() throws IOException;

    public boolean run( DocletEnvironment env ) {
        DocTrees docTrees = env.getDocTrees();
        try {
            output_ = createOutput();
            output_.startOutput();
            env.getIncludedElements()
               .stream()
               .filter( el -> el instanceof TypeElement )
               .map( el -> (TypeElement) el )
               .filter( JdkDoclet::isDocumentable )
               .sorted( Comparator
                       .comparing( el -> el.getSimpleName().toString() ) )
               .anyMatch( clazzEl -> ! processClass( clazzEl, docTrees ) );
            output_.endOutput();
            output_ = null;
            return true;
        }
        catch ( IOException e ) {
            reporter_.print( Diagnostic.Kind.ERROR, e.toString() );
            return false;
        }
    }

    /**
     * Processes a program element representing a class
     * encountered by the javadoc run,
     * handing output to this doclet's DocletOutput instance.
     *
     * @param   clazzEl  element representing a class to be documented
     * @param   docTrees   javadoc context
     * @return   false in case of fatal error
     */
    private boolean processClass( TypeElement clazzEl, DocTrees docTrees ) {
        DocCommentTree clazzTree = docTrees.getDocCommentTree( clazzEl );
        try {
            output_.startClass( clazzEl.getQualifiedName().toString(),
                                toText( clazzTree.getFirstSentence() ),
                                toText( clazzTree.getFullBody() ) );
            clazzEl.getEnclosedElements()
                   .stream()
                   .filter( el -> el.getKind() == ElementKind.METHOD )
                   .map( el -> (ExecutableElement) el )
                   .filter( JdkDoclet::isDocumentable )
                   .anyMatch( methEl -> ! processMethod( methEl, docTrees ) );
            clazzEl.getEnclosedElements()
                   .stream()
                   .filter( el -> el.getKind() == ElementKind.FIELD )
                   .map( el -> (VariableElement) el )
                   .filter( JdkDoclet::isDocumentable )
                   .anyMatch( fieldEl -> ! processField( fieldEl, docTrees ) );
            output_.endClass();
            return true;
        }
        catch ( IOException e ) {
            reporter_.print( Diagnostic.Kind.ERROR, clazzEl, e.toString() );
            return false;
        }
    }

    /**
     * Processes a program element representing a field
     * encountered by the javadoc run,
     * handing output to this doclet's DocletOutput instance.
     *
     * @param  fieldEl  element representing field to be documented
     * @param  docTrees  javadoc context
     * @return   false in case of fatal error
     */
    private boolean processField( VariableElement fieldEl, DocTrees docTrees ) {
        DocCommentTree fieldTree = docTrees.getDocCommentTree( fieldEl );
        String fieldName = fieldEl.getSimpleName().toString();
        String comment = toText( fieldTree.getFullBody() );
        if ( comment == null || comment.trim().length() == 0 ) {
            reporter_.print( Diagnostic.Kind.WARNING, fieldEl,
                             "No description" );
        }
        try {
            output_.startMember( fieldName, "Constant", fieldName, comment );
            output_.outMemberItem( "Type", typeString( fieldEl.asType() ) );
            Object value = fieldEl.getConstantValue();
            if ( value != null ) {
                output_.outMemberItem( "Value", value.toString() );
            }
            output_.endMember();
            return true;
        }
        catch ( IOException e ) {
            reporter_.print( Diagnostic.Kind.ERROR, fieldEl, e.toString() );
            return false;
        }
    }

    /**
     * Processes a program element representing a method
     * encountered by the javadoc run,
     * handing output to this doclet's DocletOutput instance.
     *
     * @param  methodEl  element representing field to be documented
     * @param  docTrees  javadoc context
     * @return   false in case of fatal error
     */
    private boolean processMethod( ExecutableElement methodEl,
                                   DocTrees docTrees ) {
        DocCommentTree methodTree = docTrees.getDocCommentTree( methodEl );
        boolean isVararg = methodEl.isVarArgs();
        List<? extends VariableElement> paramEls = methodEl.getParameters();

        /* Get description. */
        String description = toText( methodTree.getFullBody() );
        if ( description == null || description.trim().length() == 0 ) {
            reporter_.print( Diagnostic.Kind.WARNING, methodEl,
                             "No description" );
        }

        /* Prepare parameter list. */
        String paramList =
            new StringBuffer( "( " )
           .append( paramEls.stream()
                            .map( el -> el.getSimpleName().toString() )
                            .collect( Collectors.joining( ", " ) ) )
           .append( isVararg ? ", ..." : "" )
           .append( " )" )
           .toString();

        /* Prepare parameter tag comments. */
        Map<String,ParamTree> paramTags =
            methodTree
           .getBlockTags()
           .stream()
           .filter( t -> t instanceof ParamTree )
           .map( t -> (ParamTree) t )
           .collect( Collectors.toMap( t -> t.getName().getName().toString(),
                                       t -> t ) );

        List<DocletOutput.DocVariable> varList = new ArrayList<>();
        for ( int i = 0; i < paramEls.size(); i++ ) {
            VariableElement paramEl = paramEls.get( i );
            String pname = paramEl.getSimpleName().toString();
            ParamTree paramTree = paramTags.remove( pname );
            String pcomment = paramTree == null
                            ? null
                            : toText( paramTree.getDescription() );
            if ( pcomment == null || pcomment.trim().length() == 0 ) {
                reporter_.print( Diagnostic.Kind.WARNING, methodEl,
                                 "No @param for " + pname );
            }
            String ptype =
                varargTypeString( paramEl.asType(),
                                  isVararg && i == paramEls.size() - 1 );
            varList.add( new DocletOutput.DocVariable() {
                public String getName() {
                    return pname;
                }
                public String getType() {
                    return ptype;
                }
                public String getCommentText() {
                    return pcomment;
                }
            } );
        }
        if ( ! paramTags.isEmpty() ) {
            reporter_.print( Diagnostic.Kind.WARNING, methodEl,
                             "Unused @params: " + paramTags.keySet() );
        }
        assert varList.size() == paramEls.size();

        /* Create unique identifier. */
        StringBuffer idBuf = new StringBuffer( methodEl.getSimpleName() );
        for ( VariableElement paramEl : paramEls ) {
            idBuf.append( DocNames.TOKEN_SEPARATOR )
                 .append( typeMirrorText( paramEl.asType() )
                         .replaceAll( "\\[\\]", "" + DocNames.ARRAY_SUFFIX ) );
        }
        String methodId = idBuf.toString();

        /* Get return value description. */
        TypeMirror retType = methodEl.getReturnType();
        boolean isVoid = retType.getKind() == TypeKind.VOID;
        List<ReturnTree> retTrees =
            methodTree.getBlockTags()
                      .stream()
                      .filter( el -> el.getKind() == DocTree.Kind.RETURN )
                      .map( el -> (ReturnTree) el )
                      .collect( Collectors.toList() );
        String retDesc = null;
        if ( ! isVoid ) {
            if ( retTrees.size() == 1 ) {
                retDesc = toText( retTrees.get( 0 ).getDescription() );
            }
            else {
                reporter_.print( Diagnostic.Kind.WARNING, methodEl,
                                 retTrees.size() + " @return tags" );
            }
        }

        /* Get signature. */
        StringBuffer sigbuf = new StringBuffer()
              .append( retType.toString().replaceFirst( ".*[.]", "" ) )
              .append( ' ' )
              .append( methodEl.getSimpleName() )
              .append( "(" );
        for ( int i = 0; i < paramEls.size(); i++ ) {
            if ( i > 0 ) {
                sigbuf.append( ", " );
            }
            String typetxt = typeMirrorText( paramEls.get( i ).asType() );
            if ( isVararg && i == paramEls.size() - 1 &&
                 typetxt.endsWith( "[]" ) ) {
                typetxt = typetxt.substring( 0, typetxt.length() - 2 ) + "...";
            }
            sigbuf.append( typetxt );
        }
        sigbuf.append( ")" );
        String signature = sigbuf.toString();

        /* Get examples. */
        String[] examples =
            methodTree.getBlockTags()
                      .stream()
                      .filter( tree -> tree instanceof UnknownBlockTagTree )
                      .map( tree -> (UnknownBlockTagTree) tree )
                      .filter( tree -> "example".equals( tree.getTagName() ) )
                      .map( tree -> toText( tree.getContent() ) )
                      .collect( Collectors.toList() )
                      .toArray( new String[ 0 ] );

        /* Get See Also tags. */
        String[] sees =
            methodTree.getBlockTags()
                      .stream()
                      .filter( tree -> tree instanceof SeeTree )
                      .map( tree -> (SeeTree) tree )
                      .map( tree -> toText( tree.getReference() ) )
                      .collect( Collectors.toList() )
                      .toArray( new String[ 0 ] );

        try {
            output_.startMember( methodEl.getSimpleName() + paramList,
                                 "Function", methodId, description );
            if ( varList.size() > 0 ) {
                output_.outParameters( varList.toArray( new DocletOutput
                                                           .DocVariable[0] ) );
            }
            if ( ! isVoid ) {
                output_.outReturn( typeString( retType ), retDesc );
            }
            if ( examples.length > 1 ) {
                output_.outExamples( "Examples", examples );
            }
            else if ( examples.length == 1 ) {
                output_.outExamples( "Example", examples );
            }
            if ( sees.length > 0 ) {
                output_.outSees( "See Also", sees );
            }
            output_.outMemberItem( "Signature", "<tt>" + signature + "</tt>" );
            output_.endMember();
            return true;
        }
        catch ( IOException e ) {
            reporter_.print( Diagnostic.Kind.ERROR, methodEl, e.toString() );
            return false;
        }
    }

    /**
     * Indicates whether a given item is to be documented or ignored.
     * Currently, members that are public, static, and not marked by
     * the {@link uk.ac.starlink.ttools.build.HideDoc @HideDoc}
     * annotation are considered documentable.
     * Fields must additionally be declared final.
     *
     * @param  el  program element
     * @return   true to process for documentation, false to skip
     */
    private static boolean isDocumentable( Element el ) {
        Set<Modifier> modifiers = el.getModifiers();
        ElementKind kind = el.getKind();
        if ( ! modifiers.contains( Modifier.PUBLIC ) ) {
            return false;
        }
        if ( ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) &&
             ! modifiers.contains( Modifier.STATIC ) ) {
            return false;
        }
        if ( kind == ElementKind.FIELD &&
             ! modifiers.contains( Modifier.FINAL ) ) {
            return false;
        }
        if ( el.getAnnotationsByType( HideDoc.class ).length > 0 ) {
            return false;
        }
        return true;
    }

    /**
     * Does a crude job of turning a list of DocTrees into a text string.
     * Text nodes, entity nodes and element nodes are handled,
     * but for instance embedded tags are just ignored.
     *
     * @param  docTreeList  list of trees
     * @return  text content
     */
    private static String toText( List<? extends DocTree> docTreeList ) {
        if ( docTreeList == null ) {
            return null;
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for ( DocTree t : docTreeList ) {
                if ( t instanceof TextTree ) {
                    TextTree text = (TextTree) t;
                    sbuf.append( text.getBody() );
                }
                else if ( t instanceof EntityTree ) {
                    EntityTree entity = (EntityTree) t;
                    sbuf.append( '&' )
                        .append( entity.getName() )
                        .append( ';' );
                }
                else if ( t instanceof StartElementTree ) {
                    StartElementTree startEl = (StartElementTree) t;
                    sbuf.append( "<" )
                        .append( startEl.getName() );
                    for ( DocTree atree : startEl.getAttributes() ) {
                        if ( atree instanceof AttributeTree ) {
                            AttributeTree att = (AttributeTree) atree;
                            char quote = ( att.getValueKind() ==
                                           AttributeTree.ValueKind.SINGLE )
                                       ? '\''
                                       : '"';
                            sbuf.append( ' ' )
                                .append( att.getName() )
                                .append( '=' )
                                .append( quote )
                                .append( toText( att.getValue() ) )
                                .append( quote );
                        }
                    }
                    if ( startEl.isSelfClosing() ) {
                        sbuf.append( '/' );
                    }
                    sbuf.append( '>' );
                }
                else if ( t instanceof EndElementTree ) {
                    EndElementTree endEl = (EndElementTree) t;
                    sbuf.append( "</" )
                        .append( endEl.getName() )
                        .append( ">" );
                }
            }
            return sbuf.toString();
        }
    }

    /**
     * Returns a user-friendly representation of a type.
     *
     * @param  typeMirror  type object
     * @return   text representing type
     */
    private static String typeString( TypeMirror typeMirror ) {
        return varargTypeString( typeMirror, false );
    }

    /**
     * Returns a user-friendly representation of a type which may or may not
     * be in a varargs parameter list.
     *
     * @param  type  type object
     * @param  isVararg   true if this is the last argument in a varargs
     *                    parameter list
     */
    private static String varargTypeString( TypeMirror type,
                                            boolean isVararg ) {
        String post = "";
        if ( isVararg ) {
            if ( type instanceof ArrayType ) {
                type = ((ArrayType) type).getComponentType();
                post = ", one or more";
            }
            else {
                isVararg = false;
            }
        }
        String pre = "";
        if ( type instanceof ArrayType ) {
            pre = "array of ";
            type = ((ArrayType) type).getComponentType();
        }
        String typetxt = DocletUtil.getScalarTypeName( type.toString() );
        if ( typetxt == null ) {
            typetxt = typeMirrorText( type );
        }
        return pre + typetxt + post;
    }

    /**
     * Returns a text representation of a TypeMirror object.
     * The output is an unqualified type name, followed by zero or more
     * "<code>[]</code>" strings to represent and N-dimensional array type.
     *
     * @param  type  type
     * @return   type representation
     */
    private static String typeMirrorText( TypeMirror type ) {
        StringBuffer suffix = new StringBuffer();
        while ( type instanceof ArrayType ) {
            suffix.append( "[]" );
            type = ((ArrayType) type).getComponentType();
        }
        String typeTxt = type.toString();
        return typeTxt.replaceFirst( ".*[.$]", "" ) + suffix;
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
    public static class Xml extends JdkDoclet {

        private String outloc_;
        private boolean headonly_;

        /**
         * No-arg constructor required by doclet API.
         */
        public Xml() {
            super( "XML" );
            headonly_ = false;
        }

        protected DocletOutput createOutput() throws IOException {
            OutputStream out = ( outloc_ == null || "-".equals( outloc_ ) )
                             ? System.out
                             : new FileOutputStream( outloc_ );
            return new XmlDocletOutput( out, headonly_,
                                        s -> s.replaceFirst( ".*[.]", "" ) );
        }

        public Set<Doclet.Option> getSupportedOptions() {
            return new HashSet<Option>( Arrays.asList( new Option[] {
                new Option() {
                    public List<String> getNames() {
                        return Arrays.asList( new String[] {
                            "-o", "--output",
                        } );
                    }
                    public String getDescription() {
                        return "output file location, or \"-\"";
                    }
                    public int getArgumentCount() {
                        return 1;
                    }
                    public String getParameters() {
                        return "<out-file>";
                    }
                    public Kind getKind() {
                        return Kind.STANDARD;
                    }
                    public boolean process( String opt, List<String> args ) {
                        outloc_ = args.get( 0 );
                        return true;
                    }
                },
                new Option() {
                    public List<String> getNames() {
                        return Arrays.asList( new String[] {
                            "-headonly", "--head-only",
                        } );
                    }
                    public String getDescription() {
                        return "if set, only a short summary of each class"
                             + " is written";
                    }
                    public String getParameters() {
                        return "";
                    }
                    public int getArgumentCount() {
                        return 0;
                    }
                    public Kind getKind() {
                        return Kind.STANDARD;
                    }
                    public boolean process( String opt, List<String> args ) {
                        headonly_ = true;
                        return true;
                    }
                },
            } ) );
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
    public static class Html extends JdkDoclet {

        private String baseDir_;
        private boolean isHeadings_;

        /**
         * No-arg constructor required by doclet API.
         */
        public Html() {
            super( "HTML" );
            isHeadings_ = false;
        }

        protected DocletOutput createOutput() throws IOException {
            File baseDir = new File( baseDir_ == null ? "." : baseDir_ );
            return new HtmlDocletOutput( baseDir, isHeadings_ );
        }

        public Set<Doclet.Option> getSupportedOptions() {
            return new HashSet<Option>( Arrays.asList( new Option[] {
                new Option() {
                    public List<String> getNames() {
                        return Arrays.asList( new String[] {
                            "-d", "--base-dir",
                        } );
                    }
                    public String getDescription() {
                        return "base directory for output";
                    }
                    public String getParameters() {
                        return "<dir-name>";
                    }
                    public int getArgumentCount() {
                        return 1;
                    }
                    public Kind getKind() {
                        return Kind.STANDARD;
                    }
                    public boolean process( String opt, List<String> args ) {
                        baseDir_ = args.get( 0 );
                        return true;
                    }
                },
                new Option() {
                    public List<String> getNames() {
                        return Arrays.asList( new String[] {
                            "-headings", "--headings",
                        } );
                    }
                    public String getDescription() {
                        return "if set, writes a couple of extra files";
                    }
                    public String getParameters() {
                        return "";
                    }
                    public int getArgumentCount() {
                        return 0;
                    }
                    public Kind getKind() {
                        return Kind.STANDARD;
                    }
                    public boolean process( String opt, List<String> args ) {
                        isHeadings_ = true;
                        return true;
                    }
                },
            } ) );
        }
    }
}
