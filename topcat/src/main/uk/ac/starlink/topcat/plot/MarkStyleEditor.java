package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * StyleEditor implementation for editing {@link MarkStyle} objects.
 *
 * @author   Mark Taylor
 * @since    10 Jan 2005
 */
public class MarkStyleEditor extends StyleEditor {

    private final JCheckBox markFlagger_;
    private final JComboBox shapeSelector_;
    private final JComboBox sizeSelector_;
    private final JComboBox colorSelector_;
    private final JComboBox thickSelector_;
    private final ValueButtonGroup lineSelector_;
    private final JLabel corrLabel_;
    private final Map statMap_;

    private static final int MAX_SIZE = 5;
    private static final int MAX_THICK = 10;
    private static final Color[] COLORS = Styles.COLORS;
    private static final MarkShape[] SHAPES = new MarkShape[] {
        MarkShape.FILLED_CIRCLE,
        MarkShape.OPEN_CIRCLE,
        MarkShape.CROSS,
        MarkShape.CROXX,
        MarkShape.OPEN_SQUARE,
        MarkShape.OPEN_DIAMOND,
        MarkShape.OPEN_TRIANGLE_UP,
        MarkShape.OPEN_TRIANGLE_DOWN,
        MarkShape.FILLED_SQUARE,
        MarkShape.FILLED_DIAMOND,
        MarkShape.FILLED_TRIANGLE_UP,
        MarkShape.FILLED_TRIANGLE_DOWN,
    };

    /**
     * Constructor.
     */
    public MarkStyleEditor( boolean withLines ) {
        super();
        statMap_ = new HashMap();

        /* Shape selector. */
        shapeSelector_ = new JComboBox( SHAPES );
        shapeSelector_.setRenderer( new MarkRenderer() {
            public MarkShape getMarkShape( int index ) {
                return (MarkShape) shapeSelector_.getItemAt( index );
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public int getMarkSize() {
                return 5;
            }
        } );
        shapeSelector_.addActionListener( this );

        /* Size selector. */
        sizeSelector_ = new JComboBox( createNumberedModel( MAX_SIZE + 1 ) );
        sizeSelector_.setRenderer( new MarkRenderer( true ) {
            public int getMarkSize( int index ) {
                return index;
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public MarkShape getMarkShape() {
                return MarkShape.OPEN_SQUARE;
            }
        } );
        sizeSelector_.addActionListener( this );

        /* Colour selector. */
        colorSelector_ = new JComboBox( COLORS );
        colorSelector_.setRenderer( new MarkRenderer() {
            public Color getMarkColor( int index ) {
                return (Color) colorSelector_.getItemAt( index );
            }
            public int getMarkSize() {
                return 5;
            }
            public MarkShape getMarkShape() {
                return MarkShape.FILLED_SQUARE;
            }
        } );
        colorSelector_.addActionListener( this );

        /* Marker hiding selector. */
        markFlagger_ = new JCheckBox( "Hide Markers" );
        markFlagger_.setSelected( false );
        markFlagger_.addActionListener( this );

        /* Line thickness selector. */
        thickSelector_ = new JComboBox( createNumberedModel( MAX_THICK ) );
        thickSelector_.setRenderer( new LineRenderer() {
            public int getLineThickness( int index ) {
                return index + 1;
            }
        } );
        thickSelector_.addActionListener( this );

        /* Line type selector. */
        JRadioButton noneButton = new JRadioButton( "None", true );
        JRadioButton dotsButton = new JRadioButton( "Dot to Dot" );
        JRadioButton corrButton = new JRadioButton( "Linear Correlation" );
        lineSelector_ = new ValueButtonGroup();
        lineSelector_.add( noneButton, null );
        lineSelector_.add( dotsButton, MarkStyle.DOT_TO_DOT );
        lineSelector_.add( corrButton, MarkStyle.LINEAR );
        lineSelector_.addChangeListener( this );
        corrLabel_ = new JLabel();

        /* Place marker selection components. */
        JComponent markBox = Box.createHorizontalBox();
        markBox.add( new JLabel( "Shape: " ) );
        markBox.add( shapeSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( shapeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Size: " ) );
        markBox.add( sizeSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( sizeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Colour: " ) );
        markBox.add( colorSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( colorSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.setBorder( AuxWindow.makeTitledBorder( "Marker" ) );
        add( markBox );

        /* Place line selection components if required. */
        if ( withLines ) {
            Box lineStyleBox = Box.createHorizontalBox();
            lineStyleBox.add( new JLabel( "Thickness: " ) );
            lineStyleBox.add( new ShrinkWrapper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 5 ) );
            lineStyleBox.add( new ComboBoxBumper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 10 ) );
            lineStyleBox.add( markFlagger_ );
            lineStyleBox.add( Box.createHorizontalGlue() );

            Box noneLineBox = Box.createHorizontalBox();
            noneLineBox.add( noneButton );
            noneLineBox.add( Box.createHorizontalGlue() );
            Box dotsLineBox = Box.createHorizontalBox();
            dotsLineBox.add( dotsButton );
            dotsLineBox.add( Box.createHorizontalGlue() );
            Box corrLineBox = Box.createHorizontalBox();
            corrLineBox.add( corrButton );
            corrLineBox.add( corrLabel_ );
            corrLineBox.add( Box.createHorizontalGlue() );

            Box lineBox = Box.createVerticalBox();
            lineBox.add( lineStyleBox );
            lineBox.add( noneLineBox );
            lineBox.add( dotsLineBox );
            lineBox.add( corrLineBox );
            lineBox.setBorder( AuxWindow.makeTitledBorder( "Line" ) );
            add( lineBox );
        }
    }

    public void setStyle( Style style ) {
        MarkStyle mstyle = (MarkStyle) style;
        shapeSelector_.setSelectedItem( mstyle.getShapeId() );
        sizeSelector_.setSelectedIndex( mstyle.getSize() );
        colorSelector_.setSelectedItem( mstyle.getColor() );
        Stroke stroke = mstyle.getStroke();
        int thick = stroke instanceof BasicStroke 
                  ? (int) ((BasicStroke) stroke).getLineWidth()
                  : 1;
        thickSelector_.setSelectedIndex( thick - 1 );
        lineSelector_.setValue( mstyle.getLine() );
        markFlagger_.setSelected( mstyle.getHidePoints() );
    }

    public Style getStyle() {
        return getStyle( (MarkShape) shapeSelector_.getSelectedItem(),
                         sizeSelector_.getSelectedIndex(),
                         (Color) colorSelector_.getSelectedItem(),
                         ! markFlagger_.isSelected(),
                         (MarkStyle.Line) lineSelector_.getValue(),
                         thickSelector_.getSelectedIndex() + 1 );
    }

    /**
     * Returns a MarkStyle described by its attributes.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  color  marker colour
     * @param  hidePoints  whether markers are invisible
     * @param  line   line type
     * @param  thick  line thickness
     * @return  marker
     */
    private static MarkStyle getStyle( MarkShape shape, int size, Color color,
                                       boolean hidePoints, MarkStyle.Line line,
                                       int thick ) {
        MarkStyle style = size == 0 ? MarkShape.POINT.getStyle( color, 0 )
                                    : shape.getStyle( color, size );
        style.setLine( line );
        style.setHidePoints( ! hidePoints );
        style.setStroke( new BasicStroke( (float) thick, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND, 10f, null,
                                          0f ) );
        return style;
    }

    /**
     * Sets the known statistical information about a list of plottable sets.
     * This represents information about calculated linear regression
     * coefficients.
     * 
     * @param  setIds  set identifiers for the statistics objects provided
     * @param  stats   statistics objects themselves, one per element of 
     *                 <code>setIds</code>
     */
    public void setStats( SetId[] setIds, XYStats[] stats ) {
        statMap_.clear();
        if ( setIds.length != stats.length ) {
            throw new IllegalArgumentException();
        }
        for ( int i = 0; i < setIds.length; i++ ) {
            statMap_.put( setIds[ i ], stats[ i ] );
        }
        refreshState();
    }

    protected void refreshState() {
        super.refreshState();

        /* Ensure that information about linear correlations is up to date. */
        String statText;
        XYStats stats = (XYStats) statMap_.get( getSetId() );
        if ( stats != null ) {
            statText = new StringBuffer()
                .append( " - " )
                .append( "m=" )
                .append( (float) stats.getLinearCoefficients()[ 1 ] )
                .append( "; c=" )
                .append( (float) stats.getLinearCoefficients()[ 0 ] )
                .append( "; r=" )
                .append( (float) stats.getCorrelation() )
                .toString();
        }
        else {
            statText = "";
        }
        corrLabel_.setText( statText );

        /* Make sure that marker presence control is only enabled if 
         * a line is being plotted. */
        markFlagger_.setEnabled( lineSelector_.getValue() != null );
    }

    /**
     * ComboBoxRenderer class suitable for rendering MarkStyles.
     */
    private class MarkRenderer extends BasicComboBoxRenderer {
        private boolean useText_;
        MarkRenderer() {
            this( false );
        }
        MarkRenderer( boolean useText ) {
            useText_ = useText;
        }
        MarkShape getMarkShape( int itemIndex ) {
            return getMarkShape();
        }
        MarkShape getMarkShape() {
            return (MarkShape) shapeSelector_.getSelectedItem();
        }
        int getMarkSize( int itemIndex ) {
            return getMarkSize();
        }
        int getMarkSize() {
            return sizeSelector_.getSelectedIndex();
        }
        Color getMarkColor( int itemIndex ) {
            return getMarkColor();
        }
        Color getMarkColor() {
            return (Color) colorSelector_.getSelectedItem();
        }
        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c =
                super.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                if ( ! useText_ ) {
                    setText( null );
                }
                MarkStyle style = index >= 0 ? getStyle( getMarkShape( index ),
                                                         getMarkSize( index ),
                                                         getMarkColor( index ),
                                                         false, null, 1 )
                                             : getStyle( getMarkShape(),
                                                         getMarkSize(),
                                                         getMarkColor(),
                                                         false, null, 1 );
                label.setIcon( Styles.getLegendIcon( style, 15, 15 ) );
            }
            return c;
        }
    }

    /**
     * ComboBoxRenderer class suitable for rendering lines.
     */
    private class LineRenderer extends BasicComboBoxRenderer {
        final int LINE_ICON_WIDTH = 48;
        final int LINE_ICON_HEIGHT = MAX_THICK / 2 * 2 + 1;

        Color getLineColor( int index ) {
            return getLineColor();
        }
        Color getLineColor() {
            return Color.BLACK;
        }
        int getLineThickness( int index ) {
            return getLineThickness();
        }
        int getLineThickness() {
            return thickSelector_.getSelectedIndex() + 1;
        }
        float[] getLineDash( int index ) {
            return getLineDash();
        }
        float[] getLineDash() {
            return null;
        }

        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c =
                super.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                setText( null );
                final Color color = index < 0 ? getLineColor()
                                              : getLineColor( index );
                int thick = index < 0 ? getLineThickness()
                                      : getLineThickness( index );
                float[] dash = index < 0 ? getLineDash()
                                         : getLineDash( index );
                final Stroke stroke =
                    new BasicStroke( (float) thick, BasicStroke.CAP_SQUARE,
                                     BasicStroke.JOIN_MITER, 10f, dash, 0f );
                setIcon( new Icon() {
                    public int getIconHeight() {
                        return LINE_ICON_HEIGHT;
                    }
                    public int getIconWidth() {
                        return LINE_ICON_WIDTH + 5;
                    }
                    public void paintIcon( Component comp, Graphics g,
                                           int x, int y ) {
                        int yoff = y + LINE_ICON_HEIGHT / 2;
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor( color );
                        g2.setStroke( stroke );
                        g2.drawLine( 5, yoff, LINE_ICON_WIDTH, yoff );
                    }
                } );
            }
            return c;
        }
    }
}
