package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.gui.ColorComboBox;
import uk.ac.starlink.ttools.gui.DashComboBox;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.BarStyles;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * Style editor for histogram bars.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2006
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BarStyleEditor extends StyleEditor {

    private final ColorComboBox colorSelector_;
    private final JComboBox formSelector_;
    private final ValueButtonGroup<BarStyle.Placement> placeSelector_;
    private final ThicknessComboBox thickSelector_;
    private final DashComboBox dashSelector_;

    private static final int MAX_THICK = 8;
    private static final BarStyle.Form[] FORMS = new BarStyle.Form[] {
        BarStyle.FORM_FILLED,
        BarStyle.FORM_OPEN,
        // BarStyle.FORM_FILLED3D,
        BarStyle.FORM_TOP,
        BarStyle.FORM_SPIKE,
    };

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public BarStyleEditor() {
        super();

        colorSelector_ = new ColorComboBox();
        colorSelector_.addActionListener( this );
        thickSelector_ = new ThicknessComboBox( MAX_THICK );
        thickSelector_.addActionListener( this );
        dashSelector_ = new DashComboBox();
        dashSelector_.addActionListener( this );

        JRadioButton overButton = new JRadioButton( "Over", true );
        JRadioButton adjButton = new JRadioButton( "Adjacent" );
        placeSelector_ = new ValueButtonGroup<BarStyle.Placement>();
        placeSelector_.add( overButton, BarStyle.PLACE_OVER );
        placeSelector_.add( adjButton, BarStyle.PLACE_ADJACENT );
        placeSelector_.addChangeListener( this );

        formSelector_ = new RenderingComboBox( FORMS ) {
            protected Icon getRendererIcon( Object form ) {
                return BarStyles.getIcon( (BarStyle.Form) form );
            }
        };
        formSelector_.addActionListener( this );

        JComponent colorBox = Box.createHorizontalBox();
        colorBox.add( new JLabel( "Colour: " ) );
        colorBox.add( new ShrinkWrapper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( new ComboBoxBumper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalGlue() );

        JComponent formBox = Box.createHorizontalBox();
        formBox.add( new JLabel( "Bar Form: " ) );
        formBox.add( new ShrinkWrapper( formSelector_ ) );
        formBox.add( Box.createHorizontalStrut( 5 ) );
        formBox.add( new ComboBoxBumper( formSelector_ ) );
        formBox.add( Box.createHorizontalGlue() );
 
        JComponent placeBox = Box.createHorizontalBox();
        placeBox.add( new JLabel( "Bar Placement: " ) );
        placeBox.add( overButton );
        placeBox.add( Box.createHorizontalStrut( 10 ) );
        placeBox.add( adjButton );
        placeBox.add( Box.createHorizontalStrut( 5 ) );
        placeBox.add( Box.createHorizontalGlue() );

        JComponent lineBox = Box.createHorizontalBox();
        lineBox.add( new JLabel( "Line" ) );
        lineBox.add( Box.createHorizontalStrut( 10 ) );
        lineBox.add( new JLabel( "Thickness: " ) );
        lineBox.add( new ShrinkWrapper( thickSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( new ComboBoxBumper( thickSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 10 ) );
        lineBox.add( new JLabel( "Dash: " ) );
        lineBox.add( new ShrinkWrapper( dashSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( new ComboBoxBumper( dashSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( Box.createHorizontalGlue() );

        JComponent barBox = Box.createVerticalBox();
        barBox.add( colorBox );
        barBox.add( Box.createVerticalStrut( 5 ) );
        barBox.add( formBox );
        barBox.add( Box.createVerticalStrut( 5 ) );
        barBox.add( placeBox );
        barBox.add( Box.createVerticalStrut( 5 ) );
        barBox.add( lineBox );
        barBox.add( Box.createVerticalStrut( 5 ) );
        barBox.setBorder( AuxWindow.makeTitledBorder( "Bars" ) );
        add( barBox );
    }

    public void setStyle( Style st ) {
        BarStyle style = (BarStyle) st;
        colorSelector_.setSelectedColor( style.getColor() );
        formSelector_.setSelectedItem( style.getForm() );
        placeSelector_.setValue( style.getPlacement() );
        thickSelector_.setSelectedThickness( style.getLineWidth() );
        dashSelector_.setSelectedDash( style.getDash() );
    }

    public Style getStyle() {
        return getStyle( colorSelector_.getSelectedColor(),
                         (BarStyle.Form) formSelector_.getSelectedItem(),
                         placeSelector_.getValue(),
                         thickSelector_.getSelectedThickness(),
                         dashSelector_.getSelectedDash() );
    }

    public String getHelpID() {
        return "BarStyleEditor";
    }

    private static Style getStyle( Color color, BarStyle.Form form,
                                   BarStyle.Placement placement, int thick,
                                   float[] dash ) {
        if ( form == BarStyle.FORM_TOP ) {
            placement = BarStyle.PLACE_OVER;
        }
        BarStyle style = new BarStyle( color, form, placement );
        style.setLineWidth( thick );
        style.setDash( dash );
        return style;
    }
}
