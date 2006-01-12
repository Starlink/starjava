package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Style editor for histogram bars.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2006
 */
public class BarStyleEditor extends StyleEditor {

    private final ColorComboBox colorSelector_;
    private final JComboBox formSelector_;
    private final JComboBox placeSelector_;
    private final ThicknessComboBox thickSelector_;
    private final DashComboBox dashSelector_;

    private static final int MAX_THICK = 8;
    private static final BarStyle.Form[] FORMS = new BarStyle.Form[] {
        BarStyle.FORM_OPEN,
        BarStyle.FORM_FILLED,
        BarStyle.FORM_FILLED3D,
        BarStyle.FORM_TOP,
        BarStyle.FORM_SPIKE,
    };
    private static final BarStyle.Placement[] PLACEMENTS =
            new BarStyle.Placement[] {
        BarStyle.PLACE_OVER,
        BarStyle.PLACE_ADJACENT,
    };

    /**
     * Constructor.
     */
    public BarStyleEditor() {
        super();

        colorSelector_ = new ColorComboBox();
        colorSelector_.addActionListener( this );
        formSelector_ = new JComboBox( FORMS );
        formSelector_.addActionListener( this );
        placeSelector_ = new JComboBox( PLACEMENTS );
        placeSelector_.addActionListener( this );
        thickSelector_ = new ThicknessComboBox( MAX_THICK );
        thickSelector_.addActionListener( this );
        dashSelector_ = new DashComboBox();
        dashSelector_.addActionListener( this );

        JComponent colorBox = Box.createHorizontalBox();
        colorBox.add( new JLabel( "Color: " ) );
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
        placeBox.add( new ShrinkWrapper( placeSelector_ ) );
        placeBox.add( Box.createHorizontalGlue() );

        JComponent thickBox = Box.createHorizontalBox();
        thickBox.add( new JLabel( "Line Thickness: " ) );
        thickBox.add( new ShrinkWrapper( thickSelector_ ) );
        thickBox.add( Box.createHorizontalStrut( 5 ) );
        thickBox.add( new ComboBoxBumper( thickSelector_ ) );
        thickBox.add( Box.createHorizontalGlue() );

        JComponent dashBox = Box.createHorizontalBox();
        dashBox.add( new JLabel( "Line Dash: " ) );
        dashBox.add( new ShrinkWrapper( dashSelector_ ) );
        dashBox.add( Box.createHorizontalStrut( 5 ) );
        dashBox.add( new ComboBoxBumper( dashSelector_ ) );
        dashBox.add( Box.createHorizontalGlue() );

        JComponent barBox = Box.createVerticalBox();
        barBox.add( colorBox );
        barBox.add( formBox );
        barBox.add( placeBox );
        barBox.add( thickBox );
        barBox.add( dashBox );
        barBox.setBorder( AuxWindow.makeTitledBorder( "Bars" ) );
        add( barBox );
    }

    public void setStyle( Style st ) {
        BarStyle style = (BarStyle) st;
        colorSelector_.setSelectedColor( style.getColor() );
        formSelector_.setSelectedItem( style.getForm() );
        placeSelector_.setSelectedItem( style.getPlacement() );
        thickSelector_.setSelectedThickness( style.getLineWidth() );
        dashSelector_.setSelectedDash( style.getDash() );
    }

    public Style getStyle() {
        return getStyle( colorSelector_.getSelectedColor(),
                         (BarStyle.Form) formSelector_.getSelectedItem(),
                         (BarStyle.Placement) placeSelector_.getSelectedItem(),
                         thickSelector_.getSelectedThickness(),
                         dashSelector_.getSelectedDash() );
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
