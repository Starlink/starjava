package uk.ac.starlink.ttools.plot2.config;

import java.awt.Font;
import org.scilab.forge.jlatexmath.TeXFormula;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.LatexCaptioner;
import uk.ac.starlink.ttools.plot2.PlotUtil;

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
        ConfigMeta syntaxMeta = new ConfigMeta( "texttype", "Text Type" );
        syntaxMeta.setShortDescription( "Text interpretation" );
        syntaxMeta.setXmlDescription( new String[] {
            "<p>Determines how to turn label text into characters",
            "on the plot.",
            "<code>" + TextSyntax.PLAIN + "</code> and",
            "<code>" + TextSyntax.AAPLAIN + "</code>",
            "both take the text at face value,",
            "but <code>" + TextSyntax.AAPLAIN + "</code>",
            "smooths the characters.",
            "<code>" + TextSyntax.LATEX + "</code>",
            "interprets the text as LaTeX source code",
            "and typesets it accordingly.",
            "</p>",
            "<p>When not using LaTeX, antialiased text usually looks nicer,",
            "but can be perceptibly slower to plot.",
            "At time of writing, on MacOS antialiased text",
            "seems to be required to stop the writing coming out",
            "upside-down for non-horizontal text (MacOS java bug).",
            "</p>",
        } );
        TextSyntax syntaxDflt = PlotUtil.getDefaultTextAntialiasing()
                              ? TextSyntax.AAPLAIN
                              : TextSyntax.PLAIN;
        textSyntaxKey_ =
            new OptionConfigKey<TextSyntax>( syntaxMeta, TextSyntax.class,
                                             TextSyntax.values(),
                                             syntaxDflt, true ) {
                public String getXmlDescription( TextSyntax syntax ) {
                    return null;
                }
            }
           .setOptionUsage();

        ConfigMeta sizeMeta = new ConfigMeta( "fontsize", "Font Size" );
        sizeMeta.setShortDescription( "Font size in points" );
        sizeMeta.setXmlDescription( new String[] {
            "<p>Size of the text font in points.",
            "</p>",
        } );
        fontSizeKey_ =
            IntegerConfigKey.createSpinnerKey( sizeMeta, 12, 2, 64 );

        ConfigMeta typeMeta = new ConfigMeta( "fontstyle", "Font Style" );
        typeMeta.setShortDescription( "Font style" );
        typeMeta.setXmlDescription( new String[] {
            "<p>Font style for text.",
            "</p>",
        } );
        fontTypeKey_ =
            new OptionConfigKey<FontType>( typeMeta, FontType.class,
                                           FontType.values() ) {
                public String getXmlDescription( FontType ft ) {
                    return null;
                }
            }
           .setOptionUsage()
           .addOptionsXml();

        ConfigMeta weightMeta = new ConfigMeta( "fontweight", "Font Weight" );
        weightMeta.setShortDescription( "Font weight" );
        weightMeta.setXmlDescription( new String[] {
            "<p>Font weight for text.",
            "</p>",
        } );
        fontWeightKey_ =
            new OptionConfigKey<FontWeight>( weightMeta, FontWeight.class,
                                             FontWeight.values() ) {
                public String getXmlDescription( FontWeight fw ) {
                    return null;
                }
            }
           .setOptionUsage()
           .addOptionsXml();
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
                                                     size ), false );
            }
        },
        AAPLAIN( "Antialias" ) {
            public Captioner createCaptioner( FontType type, FontWeight weight,
                                              int size ) {
                return new BasicCaptioner( new Font( type.awtName_,
                                                     weight.awtWeight_,
                                                     size ), true );
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

        /**
         * Creates a captioner for this syntax.
         *
         * @param   type  font type
         * @param   weight  font weight
         * @param   size   font size
         */
        public abstract Captioner createCaptioner( FontType type,
                                                   FontWeight weight,
                                                   int size );
        public String toString() {
            return name_;
        }
    }
}
