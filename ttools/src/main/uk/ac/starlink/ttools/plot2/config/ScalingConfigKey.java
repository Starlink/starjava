package uk.ac.starlink.ttools.plot2.config;

import java.util.Collection;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.layer.Scaling;

/**
 * ConfigKey for selecting Scaling objects.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2015
 */
public class ScalingConfigKey extends ChoiceConfigKey<Scaling> {

    /**
     * Constructor.
     *
     * @param  meta  metadata object
     * @param  options   possible values for selection
     */
    public ScalingConfigKey( ConfigMeta meta, Scaling[] options ) {
        super( meta, Scaling.class, options[ 0 ], false );
        for ( Scaling scaling : options ) {
            addOption( scaling );
        }
    }

    public String stringifyValue( Scaling value ) {
        return value.getName();
    }

    public Scaling decodeString( String txt ) {
        boolean flip = false;
        while ( txt.charAt( 0 ) == '-' || txt.charAt( 0 ) == '+' ) {
            if ( txt.charAt( 0 ) == '-' ) {
                flip = ! flip;
                txt = txt.substring( 1 );
            }
        }
        for ( Map.Entry<String,Scaling> entry : getOptionMap().entrySet() ) {
            if ( txt.equalsIgnoreCase( entry.getKey() ) ) {
                return flipScaling( entry.getValue(), flip );
            }
        }
        return null;
    }

    public Specifier<Scaling> createSpecifier() {
        return new ScalingSpecifier( getOptionMap().values() );
    }

    /**
     * Returns a metadata object for describing a ScalingConfigKey.
     *
     * @param  item  word for thing to which scaling applies
     * @param  stretches   basic scaling options
     * @return  metadata object
     */
    public static ConfigMeta createScalingMeta( String item,
                                                Scaling[] stretches ) {
        ConfigMeta meta =
            new ConfigMeta( item + "scaling",
                            ConfigMeta.capitalise( item ) + " Scaling" );
        meta.setShortDescription( "Mapping from " + item
                                + " range to color ramp" );
        StringBuffer ubuf = new StringBuffer();
        StringBuffer dbuf = new StringBuffer();
        boolean isFirst = true;
        for ( Scaling stretch : stretches ) {
            if ( isFirst ) {
                ubuf.append( "|" );
                isFirst = false;
            }
            String name = stretch.getName().toLowerCase();
            ubuf.append( name );
            dbuf.append( "<li><code>" )
                .append( name )
                .append( "</code>: " )
                .append( stretch.getDescription() )
                .append( "</li>" );
        }
        meta.setStringUsage( "[-]" + ubuf );
        meta.setXmlDescription( new String[] {
            "<p>Defines the way that the values in the (possibly clipped)",
            item,
            "range are mapped to the selected colour ramp.",
            "The basic stretch functions are:",
            "<ul>",
            dbuf.toString(),
            "</ul>",
            "The sense of the mapping can be reversed by using the",
            "<label>Flip</label> button in the GUI,",
            "or prepending a minus sign (\"<code>-</code>\")",
            "to the stretch function name.",
            "</p>",
        } );
        return meta;
    }

    /**
     * Optionally reverses a scaling, so that the scaleValue output
     * is one minus its basic value.
     *
     * @param  scaling  input scaling
     * @param  isFlip   if true reverse, if false leave aloune
     * @return  output scaling
     */
    private static Scaling flipScaling( Scaling scaling, boolean isFlip ) {
        return isFlip ? Scaling.flipScaling( scaling ) : scaling;
    }

    /**
     * Specifier panel for scaling object.
     * A flip switch is provided as well as a selector for
     * the basic scaling function form.
     */
    private static class ScalingSpecifier extends SpecifierPanel<Scaling> {

        private final Specifier<Scaling> stretchSpecifier_;
        private final JCheckBox flipButton_;

        /**
         * Constructor.
         *
         * @param   stretches  basic scaling operations
         */
        public ScalingSpecifier( Collection<Scaling> stretches ) {
            super( false );
            stretchSpecifier_ = new ComboBoxSpecifier( stretches ) ;
            flipButton_ = new JCheckBox( "Flip" );
        }

        protected JComponent createComponent() {
            stretchSpecifier_.addActionListener( getActionForwarder() );
            flipButton_.addActionListener( getActionForwarder() );
            JComponent line = Box.createHorizontalBox();
            line.add( stretchSpecifier_.getComponent() );
            line.add( Box.createHorizontalStrut( 10 ) );
            line.add( flipButton_ );
            return line;
        }

        public Scaling getSpecifiedValue() {
            return flipScaling( stretchSpecifier_.getSpecifiedValue(),
                                flipButton_.isSelected() );
        }

        public void setSpecifiedValue( Scaling scaling ) {
            stretchSpecifier_.setSpecifiedValue( scaling );
            flipButton_.setSelected( false );
            fireAction();
        }
    }
}
