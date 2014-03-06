package uk.ac.starlink.ttools.plot2.config;

import java.awt.Font;
import org.scilab.forge.jlatexmath.TeXFormula;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.LatexCaptioner;

/**
 * ConfigKeySet for specifying a captioner.
 * Font size, type, weight are configurable as well as plain/latex
 * rendering.
 *
 * @author   Mark Taylor
 * @since    6 Mar 2014
 */
public class CaptionerKeySet implements KeySet<Captioner> {

    private final ConfigKey<TextSyntax> textSyntaxKey_;
    private final ConfigKey<Integer> fontSizeKey_;
    private final ConfigKey<FontType> fontTypeKey_;
    private final ConfigKey<FontWeight> fontWeightKey_;

    /**
     * Constructor.
     */
    public CaptionerKeySet() {
        textSyntaxKey_ =
            new OptionConfigKey<TextSyntax>( new ConfigMeta( "syntax",
                                                             "Text Syntax" ),
                                             TextSyntax.class,
                                             TextSyntax.values(),
                                             TextSyntax.values()[ 0 ], true );
        fontSizeKey_ =
            IntegerConfigKey.createSpinnerKey( new ConfigMeta( "fontsize",
                                                               "Font Size" ),
                                               12, 2, 64 );
        fontTypeKey_ =
            new OptionConfigKey<FontType>( new ConfigMeta( "fontstyle",
                                                           "Font Style" ),
                                           FontType.class, FontType.values() );
        fontWeightKey_ =
            new OptionConfigKey<FontWeight>( new ConfigMeta( "fontweight",
                                                             "Font Weight" ),
                                             FontWeight.class,
                                             FontWeight.values() );
    }

    public ConfigKey[] getKeys() {
        return new ConfigKey[] {
            textSyntaxKey_,
            fontSizeKey_,
            fontTypeKey_,
            fontWeightKey_,
        };
    }

    public Captioner createValue( ConfigMap config ) {
        TextSyntax syntax = config.get( textSyntaxKey_ );
        int size = config.get( fontSizeKey_ );
        FontType type = config.get( fontTypeKey_ );
        FontWeight weight = config.get( fontWeightKey_ );
        return syntax.createCaptioner( type, weight, size );
    }

    /**
     * Font type enum for use with captioner configuration.
     */
    private enum FontType {
        SANSSERIF( "Standard",  "Dialog", TeXFormula.SANSSERIF ),
        SERIF( "Serif", "Serif", TeXFormula.ROMAN ),
        MONO( "Mono", "Monospaced", TeXFormula.TYPEWRITER );

        private final String name_;
        private final String awtName_;
        private final int texType_;

        FontType( String name, String awtName, int texType ) {
            name_ = name;
            awtName_ = awtName;
            texType_ = texType;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Font weight enum for use with captioner configuration.
     */
    private enum FontWeight {
        PLAIN( "Plain", Font.PLAIN, 0 ),
        BOLD( "Bold", Font.BOLD, TeXFormula.BOLD ),
        ITALIC( "Italic", Font.ITALIC, TeXFormula.ITALIC ),
        BOLDITALIC( "Bold Italic", Font.BOLD | Font.ITALIC,
                                   TeXFormula.BOLD | TeXFormula.ITALIC );

        private final String name_;
        private final int awtWeight_;
        private final int texWeight_;

        FontWeight( String name, int awtWeight, int texWeight ) {
            name_ = name;
            awtWeight_ = awtWeight;
            texWeight_ = texWeight;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Text interpretation language enum for use with captioner configuration.
     */
    private enum TextSyntax {
        PLAIN( "Plain" ) {
            public Captioner createCaptioner( FontType type, FontWeight weight,
                                              int size ) {
                return new BasicCaptioner( new Font( type.awtName_,
                                                     weight.awtWeight_,
                                                     size ) );
            }
        },
        LATEX( "LaTeX" ) {
            public Captioner createCaptioner( FontType type, FontWeight weight,
                                              int size ) {
                return new LatexCaptioner( size,
                                           type.texType_ | weight.texWeight_ );
            }
        };
        private final String name_;
        TextSyntax( String name ) {
            name_ = name;
        }
        public abstract Captioner createCaptioner( FontType type,
                                                   FontWeight weight,
                                                   int size );
        public String toString() {
            return name_;
        }
    }
}
