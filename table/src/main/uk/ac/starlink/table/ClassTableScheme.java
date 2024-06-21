package uk.ac.starlink.table;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TableScheme implementation that allows to use any TableScheme
 * implementation on the classpath.
 *
 * <p>Usage is for example:
 * <pre>
 *    class:uk.ac.starlink.table.LoopTableScheme:10
 * </pre>
 *
 * @author   Mark Taylor
 * @since    22 Jul 2020
 */
public class ClassTableScheme implements TableScheme, Documented {

    private static final Pattern CLASSNAME_REGEX =
        Pattern.compile( "([\\p{L}[0-9]_$.]+):(.*)" );

    /**
     * Returns "class";
     */
    public String getSchemeName() {
        return "class";
    }

    public String getSchemeUsage() {
        return "<TableScheme-classname>:<scheme-spec>";
    }

    public String getExampleSpecification() {
        return LoopTableScheme.class.getName() + ":" + 5;
    }

    public String getXmlDescription() {
        String prefix = ":" + getSchemeName() + ":";
        String exClassname = LoopTableScheme.class.getName();
        String exArglist = "10";
        return String.join( "\n",
            "<p>Uses an instance of a named class that implements",
            "the <code>uk.ac.starlink.table.TableScheme</code> interface",
            "and that has a no-arg constructor.",
            "Arguments to be passed to an instance of the named class",
            "are appended after a colon following the classname.",
            "</p>",
            "<p>For example, the specification",
            "\"<code>" + prefix + exClassname + ":" + exArglist + "</code>\"",
            "would return a table constructed by the code",
            "<code>new " + exClassname + "()"
               + ".createTable(\"" + exArglist +"\")</code>.",
            "</p>",
            "" );
    }

    public StarTable createTable( String spec ) throws IOException {
        Matcher matcher = CLASSNAME_REGEX.matcher( spec );
        if ( ! matcher.matches() ) {
            throw new TableFormatException();
        }
        String classname = matcher.group( 1 );
        String subspec = matcher.group( 2 );
        Class<?> clazz;
        try {
            clazz = Class.forName( classname );
        }
        catch ( ClassNotFoundException e ) {
            throw new TableFormatException( "Unkown class " + classname );
        }
        if ( ! TableScheme.class.isAssignableFrom( clazz ) ) {
            throw new TableFormatException( "Class " + classname
                                          + " does not implement "
                                          + TableScheme.class.getName() );
        }
        @SuppressWarnings("unchecked")
        Class<? extends TableScheme> schemeClazz =
            (Class<? extends TableScheme>) clazz;
        TableScheme subscheme;
        try {
            subscheme = schemeClazz.getDeclaredConstructor().newInstance();
        }
        catch ( ReflectiveOperationException e ) {
            throw new TableFormatException( "Can't instantiate "
                                          + clazz.getName(), e );
        }
        return subscheme.createTable( subspec );
    }
}
