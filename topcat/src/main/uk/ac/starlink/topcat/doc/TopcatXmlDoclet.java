package uk.ac.starlink.topcat.doc;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import java.io.IOException;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.ttools.build.FullXmlDoclet;

/**
 * Doclet which documents public static members of clases in XML
 * for insertion into the TOPCAT user document.
 *
 * <p>Optional doclet flags beyond the superclass ones are:
 * <dl>
 * <dt>-act</dt>
 * <dd>Write output only for 'activation' classes - as reported by
 *     {@link uk.ac.starlink.topcat.TopcatJELUtils#getActivationStaticClasses}.
 *     </dd>
 *
 * <dt>-gen</dt>
 * <dd>Write output only for 'general' classes - as reported by
 *     {@link uk.ac.starlink.topcat.TopcatJELUtils.getStaticClasses}.</dd>
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Sep 2004
 */
public class TopcatXmlDoclet extends FullXmlDoclet {

    private Class<?>[] classes_;

    /**
     * Begin processing document.
     * This method is part of the Doclet public interface.
     */
    public static boolean start( RootDoc root ) throws IOException {
        return new TopcatXmlDoclet( root ).process();
    }

    /**
     * Define permitted command-line flags.
     * This method is part of the Doclet public interface.
     */
    public static int optionLength( String option ) {
        if ( option.equals( "-gen" ) ) {
            return 1;
        }
        else if ( option.equals( "-act" ) ) {
            return 1;
        }
        else {
            return FullXmlDoclet.optionLength( option );
        }
    }

    /**
     * Constructor.
     *
     * @param  root  root document
     */
    protected TopcatXmlDoclet( RootDoc root ) throws IOException {
        super( root );
        String[][] options = root.options();
        for ( String[] opts : options ) {
            String opt = opts[ 0 ];
            if ( opt.equals( "-gen" ) ) {
                classes_ = TopcatJELUtils.getStaticClasses()
                                         .toArray( new Class<?>[ 0 ] );
            }
            else if ( opt.equals( "-act" ) ) {
                classes_ = TopcatJELUtils.getActivationStaticClasses()
                                         .toArray( new Class<?>[ 0 ] );
            }
        }
    }

    @Override
    protected boolean useClass( ClassDoc clazz ) {
        if ( classes_ == null ) {
            return true;
        }
        else {
            String cname = clazz.qualifiedName();
            for ( int i = 0; i < classes_.length; i++ ) {
                if ( classes_[ i ].getName().equals( cname ) ) {
                    return true;
                }
            }
            return false;
        }
    }
}
