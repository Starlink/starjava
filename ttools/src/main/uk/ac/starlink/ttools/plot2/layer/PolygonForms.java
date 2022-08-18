package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ComboBoxSpecifier;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;

/**
 * ShapeForm implementations for plotting filled or outline polygons.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2019
 */
public class PolygonForms {

    /** Shape form for a quadrilateral defined by 4 positional coordinates. */
    public static final ShapeForm QUAD = new FixedPolygonForm( 4 );

    /** Shape form for a polygon defined by an array coordinate. */
    public static final ShapeForm ARRAY = new ArrayPolygonForm();

    /** Polygon mode configuration key. */
    public static final ConfigKey<PolygonShape> POLYSHAPE_KEY =
        createPolygonShapeKey();

    /** Polygon thickness configuration key. */
    public static final ConfigKey<Integer> POLYTHICK_KEY =
        createPolygonThicknessKey();

    /** Reference position inclusion toggle key. */
    public static final ConfigKey<Boolean> INCLUDEPOS_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "usepos", "Use Position" )
           .setShortDescription( "Include reference position in vertices?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the basic positional coordinates",
                "are included as one of the polygon vertices or not.",
                "The polygon has N+1 vertices if true,",
                "or N vertices if false,",
                "where N is the number of vertices supplied by the",
                "array coordinate.",
                "If false, the basic position is ignored for the purposes",
                "of drawing the polygon.",
                "</p>",
            } )
        , true );

    /** Coordinate for array coordinate, used with ARRAY. */
    public static final FloatingArrayCoord ARRAY_COORD =
        createArrayCoord();

    /**
     * Private constructor prevents instantiation.
     */
    private PolygonForms() {
    }

    /**
     * Creates the array-valued coordinate object for use with the
     * ArrayPolygonForm.
     *
     * @return   array coord
     */
    private static FloatingArrayCoord createArrayCoord() {
        InputMeta meta = new InputMeta( "otherpoints", "Other Points" );
        meta.setShortDescription( "array of positions" );
        meta.setValueUsage( "array" );
        meta.setXmlDescription( new String[] {
            "<p>Array of coordinates giving the points of the vertices",
            "defining the polygon to be drawn.",
            "These coordinates are given as an interleaved array",
            "by this parameter, e.g. (x1,y1, x2,y2, y3,y3).",
            "The basic position for the row being plotted",
            "either is or is not included as the first vertex,",
            "according to the setting of the",
            "<code>" + INCLUDEPOS_KEY.getMeta().getShortName() + "</code>",
            "parameter.",
            "</p>",
            "<p>Some expression language functions that can be useful",
            "when specifying this parameter are",
            "<code>array()</code> and <code>parseDoubles()</code>.",
            "</p>",
        } );
        return FloatingArrayCoord.createCoord( meta, true );
    }

    /**
     * Creates the configuration key for polygon drawing mode.
     *
     * @return  mode key
     */
    private static ConfigKey<PolygonShape> createPolygonShapeKey() {
        ConfigMeta meta = new ConfigMeta( "polymode", "Polygon Mode" );
        meta.setXmlDescription( new String[] {
            "<p>Polygon drawing mode.",
            "Different options are available, including drawing an outline",
            "round the edge and filling the interior with colour.",
            "</p>",
        } );
        final PolygonShape[] shapes = PolygonShape.POLYSHAPES;
        OptionConfigKey<PolygonShape> key =
                new OptionConfigKey<PolygonShape>( meta, PolygonShape.class,
                                                   shapes ) {
            public String getXmlDescription( PolygonShape polyShape ) {
                return polyShape.getDescription();
            }
            @Override
            public Specifier<PolygonShape> createSpecifier() {
                JComboBox<PolygonShape> comboBox = new JComboBox<>( shapes );
                comboBox.setRenderer( new PolygonShapeRenderer() );
                return new ComboBoxSpecifier<>( PolygonShape.class, comboBox );
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Returns a config key for line drawing thickness.
     *
     * @return  new config key
     */
    private static final ConfigKey<Integer> createPolygonThicknessKey() {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setShortDescription( "Line thickness for open shapes" );
        meta.setXmlDescription( new String[] {
            "<p>Controls the line thickness used when drawing polygons.",
            "Zero, the default value, means a 1-pixel-wide line is used.",
            "Larger values make drawn lines thicker,",
            "but note changing this value will not affect all shapes,",
            "for instance filled polygons contain no line drawings.",
            "</p>",
        } );
        return StyleKeys.createPaintThicknessKey( meta, 4 );
    }

    /**
     * ComboBox renderer for PolygonShape.  It draws a representation icon
     * as well as displaying the shape name.
     */
    private static class PolygonShapeRenderer extends DefaultListCellRenderer {

        private int iconHeight_;
        private PolygonShape polyShape_;
        private final Icon icon_;

        /**
         * Constructor.
         */
        PolygonShapeRenderer() {
            iconHeight_ = 0;
            icon_ = new Icon() {
                public int getIconWidth() {
                    return iconHeight_ * 2;
                }
                public int getIconHeight() {
                    return iconHeight_;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    if ( polyShape_ != null ) {
                        int w = getIconWidth();
                        int h = getIconHeight();
                        int[] xs = new int[] { x+0, x+w, x+w, x+0 };
                        int[] ys = new int[] { y+0, y+0, y+h, y+h };
                        int x0 = x + w / 2;
                        int y0 = y + h / 2;
                        Color color0 = g.getColor();
                        g.setColor( c.getForeground() );
                        polyShape_.createPolygonGlyph( x0, y0, xs, ys, 4 )
                                  .paintGlyph( g );
                        g.setColor( color0 );
                    }
                }
            };
        }

        @Override
        public Component getListCellRendererComponent( JList<?> list,
                                                       Object value,
                                                       int index, boolean isSel,
                                                       boolean hasFocus ) {
            Component c =
                super.getListCellRendererComponent( list, value, index,
                                                    isSel, hasFocus );
            if ( iconHeight_ <= 0 ) {
                iconHeight_ = c.getFontMetrics( c.getFont() ).getAscent();
            }
            if ( c instanceof JLabel && value instanceof PolygonShape ) {
                JLabel label = (JLabel) c;
                polyShape_ = (PolygonShape) value;
                label.setText( polyShape_.toString() );
                label.setIcon( icon_ );
            }
            return c;
        }
    }

    /**
     * ShapeForm implementation for a polygon defined by a fixed number
     * of positional coordinates.
     */
    private static class FixedPolygonForm implements ShapeForm {

        private final int np_;

        /**
         * Constructor.
         *
         * @param   np   number of vertices in polygon
         */
        public FixedPolygonForm( int np ) {
            np_ = np;
        }

        public String getFormName() {
            return "Poly" + np_;
        }

        public Icon getFormIcon() {
            return ResourceIcon.FORM_POLYLINE;
        }

        public String getFormDescription() {
            final String figname;
            if ( np_ == 3 ) {
                figname = "triangle";
            }
            else if ( np_ == 4 ) {
                figname = "quadrilateral";
            }
            else {
                figname = Integer.toString( np_ ) + "-sided polygon";
            }
            return PlotUtil.concatLines( new String[] {
                "<p>Draws a closed " + figname,
                "given the coordinates of its vertices",
                "supplied as " + np_ + " separate positions.",
                "The way that the polygon is drawn (outline, fill etc)",
                "is determined using the",
                "<code>" + POLYSHAPE_KEY.getMeta().getShortName() + "</code>",
                "option.",
                "</p>",
                "<p>Polygons smaller than a configurable threshold size",
                "in pixels are by default represented by a replacement marker,",
                "so the position of even a very small polygon",
                "is still visible on the screen.",
                "</p>",
            } );
        }

        public int getBasicPositionCount() {
            return np_;
        }

        public Coord[] getExtraCoords() {
            return new Coord[ 0 ];
        }

        public int getExtraPositionCount() {
            return 0;
        }

        public DataGeom adjustGeom( DataGeom geom, DataSpec dataSpec,
                                    ShapeStyle style ) {
            return geom;
        }

        public ConfigKey<?>[] getConfigKeys() {
            return new ConfigKey<?>[] {
                POLYSHAPE_KEY,
                POLYTHICK_KEY,
                PolygonOutliner.MINSIZE_KEY,
                PolygonOutliner.MINSHAPE_KEY,
            };
        }

        public Outliner createOutliner( ConfigMap config ) {
            PolygonShape basicShape = config.get( POLYSHAPE_KEY );
            int nthick = config.get( POLYTHICK_KEY ).intValue();
            PolygonShape polyShape =
                nthick == 0 ? basicShape : basicShape.toThicker( nthick );
            int minSize = config.get( PolygonOutliner.MINSIZE_KEY );
            MarkerShape minShape = config.get( PolygonOutliner.MINSHAPE_KEY );
            return PolygonOutliner
                  .createFixedOutliner( np_, polyShape, minSize, minShape );
        }
    }

    /**
     * ShapeForm implementation for a polygon defined by one positional
     * coordinate defining the first vertex and an array-valued coordinate
     * for the other vertices.
     * This looks a bit clunky from a UI point of view,
     * but the rest of the plotting system really wants at least
     * one positional coordinate for any plot type that's plotting
     * objects at positions, so trying to do it using (for instance)
     * an array-valued coordinate supplying all of the vertices
     * means a lot of things don't work well.
     */
    private static class ArrayPolygonForm implements ShapeForm {

        public String getFormName() {
            return "Polygon";
        }

        public Icon getFormIcon() {
            return ResourceIcon.FORM_POLYLINE;
        }

        public String getFormDescription() {
            String arrayCoordName =
                ARRAY_COORD.getInputs()[ 0 ].getMeta().getShortName();
            return PlotUtil.concatLines( new String[] {
                "<p>Draws a closed polygon given an array of coordinates",
                "that define its vertices.",
                "In fact this plot requires the position of the first vertex",
                "supplied as a positional value in the usual way",
                "(e.g. <code>X</code> and <code>Y</code> coordinates)",
                "and the second, third etc vertices supplied as an array",
                "using the <code>" + arrayCoordName + "</code> parameter.",
                "</p>",
                "<p>Invocation might therefore look like",
                "\"<code>xN=x1 yN=y1 " + arrayCoordName + "N="
                                       + "array(x2,y2, x3,y3, x4,y4)</code>\".",
                "</p>",
            } );
        }

        public int getBasicPositionCount() {
            return 1;
        }

        public Coord[] getExtraCoords() {
            return new Coord[] { ARRAY_COORD, };
        }

        public int getExtraPositionCount() {
            return 0;
        }

        public DataGeom adjustGeom( DataGeom geom, DataSpec dataSpec,
                                    ShapeStyle style ) {
            return geom;
        }

        public ConfigKey<?>[] getConfigKeys() {
            return new ConfigKey<?>[] {
                INCLUDEPOS_KEY,
                POLYSHAPE_KEY,
                POLYTHICK_KEY,
            };
        }

        public Outliner createOutliner( ConfigMap config ) {
            boolean includePos = config.get( INCLUDEPOS_KEY );
            PolygonShape basicShape = config.get( POLYSHAPE_KEY );
            int nthick = config.get( POLYTHICK_KEY ).intValue();
            PolygonShape polyShape =
                nthick == 0 ? basicShape : basicShape.toThicker( nthick );
            return PolygonOutliner
                  .createArrayOutliner( ARRAY_COORD, includePos, polyShape );
        }
    }
}
