package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
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
import uk.ac.starlink.topcat.RadioButtonGroup;
import uk.ac.starlink.util.gui.ShrinkWrapper;

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
    private final RadioButtonGroup lineSelector_;
    private final JLabel corrLabel_;

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

        /* Marker box. */
        markFlagger_ = new JCheckBox( "Plot Marker" );
        markFlagger_.setSelected( true );
        markFlagger_.addActionListener( this );

        /* Shape selector. */
        shapeSelector_ =
            new JComboBox( new DefaultComboBoxModel( SHAPES ) );
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
        colorSelector_ = new JComboBox( new DefaultComboBoxModel( COLORS ) );
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

        /* Place components. */
        JComponent markBox = Box.createHorizontalBox();
        if ( withLines ) {
            markBox.add( markFlagger_ );
            markBox.add( Box.createHorizontalStrut( 10 ) );
        }
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
        markBox.setBorder( AuxWindow.makeTitledBorder( "Marker" ) );
        add( markBox );

        if ( withLines ) {
            thickSelector_ = new JComboBox( createNumberedModel( MAX_THICK ) );
            thickSelector_.setRenderer( new LineRenderer() {
                public int getLineThickness( int index ) {
                    return index + 1;
                }
            } );
            thickSelector_.addActionListener( this );

            Box lineStyleBox = Box.createHorizontalBox();
            lineStyleBox.add( new JLabel( "Thickness: " ) );
            lineStyleBox.add( new ShrinkWrapper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 5 ) );
            lineStyleBox.add( new ComboBoxBumper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalGlue() );

            /* Set up radio buttons for selecting line type. */
            lineSelector_ = new RadioButtonGroup();
            Box noneLineBox = Box.createHorizontalBox();
            noneLineBox.add( lineSelector_.addButton( "None", null ) );
            noneLineBox.add( Box.createHorizontalGlue() );
            Box dotsLineBox = Box.createHorizontalBox();
            dotsLineBox.add( lineSelector_.addButton( "Dot to Dot",
                                                      MarkStyle.DOT_TO_DOT ) );
            dotsLineBox.add( Box.createHorizontalGlue() );
            Box corrLineBox = Box.createHorizontalBox();
            corrLineBox.add( lineSelector_.addButton( "Linear Correlation",
                                                      MarkStyle.LINEAR ) );
            corrLabel_ = new JLabel();
            corrLineBox.add( corrLabel_ );
            corrLineBox.add( Box.createHorizontalGlue() );
            lineSelector_.addActionListener( this );

            Box lineBox = Box.createVerticalBox();
            lineBox.add( lineStyleBox );
            lineBox.add( noneLineBox );
            lineBox.add( dotsLineBox );
            lineBox.add( corrLineBox );
            lineBox.setBorder( AuxWindow.makeTitledBorder( "Line" ) );
            add( lineBox );
        }
        else {
            thickSelector_ = null;
            lineSelector_ = null;
            corrLabel_ = null;
        }
    }

    public void setStyle( Style style ) {
        MarkStyle mstyle = (MarkStyle) style;
        shapeSelector_.setSelectedItem( mstyle.getShapeId() );
        sizeSelector_.setSelectedIndex( mstyle.getSize() );
        colorSelector_.setSelectedItem( mstyle.getColor() );
        if ( thickSelector_ != null ) {
            Stroke stroke = mstyle.getStroke();
            int thick = stroke instanceof BasicStroke 
                      ? (int) ((BasicStroke) stroke).getLineWidth()
                      : 1;
            thickSelector_.setSelectedIndex( thick - 1 );
        }
        if ( lineSelector_ != null ) {
            lineSelector_.setValue( mstyle.getLine() );
        }
    }

    public Style getStyle() {
        return getStyle( (MarkShape) shapeSelector_.getSelectedItem(),
                         sizeSelector_.getSelectedIndex(),
                         (Color) colorSelector_.getSelectedItem(),
                         lineSelector_ == null
                             ? null
                             : (MarkStyle.Line) lineSelector_.getValue(),
                         thickSelector_ == null 
                             ? 1
                             : thickSelector_.getSelectedIndex() + 1 );
    }

    /**
     * Constructs a new ComboBoxModel which contains Integers numbered from
     * 0 to <code>count-1</code>.
     *
     * @param   count  number of entries in the model
     * @return  new ComboBoxModel filled with Integers
     */
    private static ComboBoxModel createNumberedModel( int count ) {
        Object[] items = new Object[ count ];
        for ( int i = 0; i < count; i++ ) {
            items[ i ] = new Integer( i );
        }
        return new DefaultComboBoxModel( items );
    }

    /**
     * Returns a MarkStyle described by its attributes.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  color  marker colour
     * @param  line   line type
     * @param  thick  line thickness
     * @return  marker
     */
    private static MarkStyle getStyle( MarkShape shape, int size, Color color,
                                       MarkStyle.Line line, int thick ) {
        MarkStyle style = size == 0 ? MarkShape.POINT.getStyle( color, 0 )
                                    : shape.getStyle( color, size );
        style.setLine( line );
        style.setStroke( new BasicStroke( (float) thick, BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND, 10f, null,
                                          0f ) );
        return style;
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
                                                         null, 1 )
                                             : getStyle( getMarkShape(),
                                                         getMarkSize(),
                                                         getMarkColor(),
                                                         null, 1 );
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
