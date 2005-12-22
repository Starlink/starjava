package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.func.Browsers;
import uk.ac.starlink.topcat.func.Image;
import uk.ac.starlink.topcat.func.Spectrum;
import uk.ac.starlink.ttools.JELRowReader;

/**
 * A dialogue window which queries the user for a new activation action
 * and installs it in the TopcatModel.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Aug 2004
 */
public class ActivationQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private ActivatorFactory activeFactory_;

    /**
     * Constructs a new window.
     *
     * @param  tcModel  topcat model
     * @param  parent  parent window, may be used for positioning
     */
    public ActivationQueryWindow( TopcatModel tcModel, Component parent ) {
        super( "Set Activation Action", parent );
        tcModel_ = tcModel;

        /* Set up the different types of activator. */
        ActivatorFactory[] factories = new ActivatorFactory[] {
            new NopActivatorFactory(),
            new CutoutActivatorFactory(),
            new ImageActivatorFactory(),
            new SpectrumActivatorFactory(),
            new BrowserActivatorFactory(),
            new JELActivatorFactory(),
        };

        /* Set up the window outline. */
        GridBagLayout layer = new GridBagLayout();
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 0;
        bc.gridx = 0;
        bc.ipadx = 4;
        bc.ipady = 4;
        bc.insets = new Insets( 2, 2, 2, 2 );
        bc.anchor = GridBagConstraints.WEST;
        GridBagConstraints qc = (GridBagConstraints) bc.clone();
        qc.gridx++;
        qc.fill = GridBagConstraints.BOTH;
        qc.weightx = 1.0;
        JComponent mainBox = new JPanel( layer );
        getMainArea().add( mainBox );

        /* Add a new row for each activator factory. */
        ButtonGroup buttGroup = new ButtonGroup();
        for ( int i = 0; i < factories.length; i++ ) {
            ActivatorFactory fact = factories[ i ];
            fact.setEnabled( false );

            /* Place radio button. */
            JRadioButton butt = fact.button_;
            butt.setEnabled( fact.isPossible() );
            buttGroup.add( butt );
            layer.setConstraints( butt, bc );
            mainBox.add( butt );

            /* Place query component. */
            JComponent query = fact.getQueryComponent();
            Border border = BorderFactory
                           .createBevelBorder( BevelBorder.RAISED );
            border = BorderFactory
                    .createCompoundBorder( border,
                         BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
            query.setBorder( border );
            layer.setConstraints( query, qc );
            mainBox.add( query );
           
            /* Update constraints. */
            bc.gridy++;
            qc.gridy++;
        }
        factories[ 0 ].button_.setSelected( true );

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this, true ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "ActivationQueryWindow" );
    }

    /**
     * Invoked if the user hits OK.
     */
    public boolean perform() {
        Activator actor = activeFactory_.makeActivator();
        if ( actor != null ) {
            tcModel_.setActivator( actor );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns a label which identifies a particular column in this window's
     * table.  Used for labelling display windows.
     *
     * @param   tcol  column
     * @return   label
     */
    private String getWindowLabel( TableColumn tcol ) {
        String tname = tcol instanceof StarTableColumn 
                     ? ((StarTableColumn) tcol).getColumnInfo().getName()
                     : tcol.getHeaderValue().toString();
        return tname + "(" + tcModel_.getID() + ")";
    }

    /**
     * Helper class defining the appearance and fucntionality of an
     * option for creating new Activator objects on the basis of
     * user selections.
     */
    private abstract class ActivatorFactory implements ChangeListener {
        String description_;
        JRadioButton button_;
        JPanel queryPanel_;
        Component[] enablables_;

        /**
         * Constructs a new factory.
         *
         * @param  desc  description string
         */
        ActivatorFactory( String desc ) {
            description_ = desc;
            button_ = new JRadioButton( desc );
            button_.addChangeListener( this );
            queryPanel_ = new JPanel( new BorderLayout() );
        }

        /**
         * Return a graphical component containing all the widgets required
         * for the user to specify the details of created activators.
         *
         * @return  graphical component
         */
        JComponent getQueryComponent() {
            return queryPanel_;
        }

        /**
         * Whether this factory can possibly return an activator.
         * This implementation returns true, but subclasses may override it.
         *
         * @return  whether this factory is any good
         */
        boolean isPossible() {
            return true;
        }

        /**
         * Instructs this component to make its query component 
         * available/unavailable for user interaction.
         *
         * @param  enabled  true iff the user is permitted to interact
         */
        public void setEnabled( boolean enabled ) {
            if ( enablables_ != null ) {
                for ( int i = 0; i < enablables_.length; i++ ) {
                    enablables_[ i ].setEnabled( enabled );
                }
            }
        }

        /**
         * Returns a new Activator object in accordance with the current
         * state of this factory.
         * 
         * @return  new activator
         */
        abstract Activator makeActivator();

        /**
         * Implements ChangeListener to maintain the window's active factory
         * by listening for button selection events.
         */
        public void stateChanged( ChangeEvent evt ) {
            if ( evt.getSource() == button_ ) {
                boolean active = button_.isSelected();
                setEnabled( active );
                if ( active ) {
                    activeFactory_ = this;
                }
            }
        }
    }

    /**
     * Factory for providing dummy activators.
     */
    private class NopActivatorFactory extends ActivatorFactory {
        JComponent qcomp_ = new JPanel();
        NopActivatorFactory() {
            super( "No Action" );
        }
        Activator makeActivator() {
            return Activator.NOP;
        }
    }

    /** 
     * Factory implementation for the user to enter custom JEL code.
     */
    private class JELActivatorFactory extends ActivatorFactory {
        JComboBox codeField_;

        JELActivatorFactory() {
            super( "Execute Custom Code" );
            codeField_ = tcModel_.getActivatorList().makeComboBox();
            codeField_.setEditable( true );
            codeField_.validate();
            JLabel codeLabel = new JLabel( "Executable Expression: " );
            enablables_ = new Component[] { codeField_, codeLabel, };
            Box box = Box.createHorizontalBox();
            box.add( codeLabel );
            box.add( codeField_ );
            queryPanel_.add( box );
        }

        Activator makeActivator() {
            Object sel = codeField_.getSelectedItem();
            String expr = sel == null ? null : sel.toString();
            try {
                Activator activ = tcModel_.makeActivator( expr );
                OptionsListModel actlist = tcModel_.getActivatorList();
                if ( ! actlist.contains( expr ) ) {
                    actlist.add( expr );
                }
                return activ;
            }
            catch ( CompilationException e ) {
                Object msg = new String[] {
                    "Syntax error in activation function \"" + expr + "\":",
                    e.getMessage(),
                };
                JOptionPane.showMessageDialog( ActivationQueryWindow.this, msg,
                                               "Syntax Error",
                                               JOptionPane.ERROR_MESSAGE );
                return null;
            }
        }
    }

    /**
     * Factory implementation for activators that display a URL in a 
     * general purpose web browser.
     */
    private class BrowserActivatorFactory extends ColumnActivatorFactory {
        final JComboBox browserChooser_;
        final String MOZILLA = "mozilla";
        final String NETSCAPE = "netscape";
        final String FIREFOX = "firefox";
        final String BASIC_BROWSER = "basic browser";
        final String SYSTEM_BROWSER = "system browser";

        BrowserActivatorFactory() {
            super( "Web Page" );
            browserChooser_ = new JComboBox();
            browserChooser_.addItem( BASIC_BROWSER );
            browserChooser_.addItem( SYSTEM_BROWSER );
            browserChooser_.addItem( MOZILLA );
            browserChooser_.addItem( NETSCAPE );
            browserChooser_.addItem( FIREFOX );
            JLabel browserLabel = new JLabel( "Browser Type: " );
            List eList = new ArrayList( Arrays.asList( enablables_ ) );
            eList.add( browserLabel );
            eList.add( browserChooser_ );
            enablables_ = (Component[]) eList.toArray( new Component[ 0 ] );
            Box browserBox = Box.createHorizontalBox();
            browserBox.add( browserLabel );
            browserBox.add( browserChooser_ );
            queryPanel_.add( Box.createVerticalStrut( 5 ) );
            queryPanel_.add( browserBox );
        }

        Activator makeActivator( TableColumn tcol ) {
            Object browser = browserChooser_.getSelectedItem();
            if ( BASIC_BROWSER.equals( browser ) ) {
                return new ColumnActivator( "basicBrowser", tcol ) {
                    String activateValue( Object val ) {
                        return val == null 
                             ? null
                             : Browsers.basicBrowser( val.toString() );
                    }
                };
            }
            else if ( SYSTEM_BROWSER.equals( browser ) ) {
                return new ColumnActivator( "systemBrowser", tcol ) {
                    String activateValue( Object val ) {
                        return val == null
                             ? null
                             : Browsers.systemBrowser( val.toString() );
                    }
                };
            }
            else if ( MOZILLA.equals( browser ) ) {
                return createMozalikeActivator( "mozilla", tcol );
            }
            else if ( NETSCAPE.equals( browser ) ) {
                return createMozalikeActivator( "netscape", tcol );
            }
            else if ( FIREFOX.equals( browser ) ) {
                return createMozalikeActivator( "firefox", tcol );
            }
            return null;
        }

        private Activator createMozalikeActivator( final String cmdname, 
                                                   TableColumn tcol ) {
            return new ColumnActivator( "document", tcol ) {
                String activateValue( Object val ) {
                    return val == null
                         ? null
                         : Browsers.mozalike( cmdname, val.toString() );
                }
                public String toString() {
                    String colName = tcModel_.getDataModel()
                                             .getColumnInfo( icol_ ).getName();
                    return cmdname + "( " + colName + " )";
                }
            };
        }
    }

    /**
     * Factory implementation for selecting a cutout service.
     */
    private class CutoutActivatorFactory extends ActivatorFactory {
        CutoutSelector cutter_;

        CutoutActivatorFactory() {
            super( "Display Cutout Image" );
            cutter_ = new CutoutSelector( tcModel_ );
            Box box = Box.createHorizontalBox();
            box.add( cutter_ );
            box.add( Box.createHorizontalGlue() );
            queryPanel_.add( box );
            enablables_ = new Component[] { cutter_, };
        }

        Activator makeActivator() {
            return cutter_.makeActivator();
        }
    }

    /**
     * Activator factory superclass for actions that do something to the
     * content of a single column.
     */
    private abstract class ColumnActivatorFactory extends ActivatorFactory {
        JComboBox colSelector_;

        ColumnActivatorFactory( String descrip ) {
            super( "View URL as " + descrip );
            colSelector_ = new RestrictedColumnComboBoxModel( tcModel_
                                                             .getColumnModel(),
                                                              true ) {
                public boolean acceptColumn( ColumnInfo cinfo ) {
                    Class clazz = cinfo.getContentClass();
                    return clazz == String.class
                        || clazz == URL.class
                        || clazz == URI.class
                        || clazz == File.class;
                }
            }.makeComboBox();
            colSelector_.setSelectedIndex( 0 );
            JLabel colLabel = new JLabel( descrip + " Location column: " );
            enablables_ = new Component[] { colLabel, colSelector_, };
            Box colBox = Box.createHorizontalBox();
            colBox.add( colLabel );
            colBox.add( colSelector_ );
            queryPanel_.setLayout( new BoxLayout( queryPanel_,
                                                  BoxLayout.Y_AXIS ) );
            queryPanel_.add( colBox );
        }

        boolean isPossible() {
            return colSelector_.getItemCount() > 1;
        }

        Activator makeActivator() {
            TableColumn tcol = (TableColumn) colSelector_.getSelectedItem();
            if ( tcol == ColumnComboBoxModel.NO_COLUMN ) {
                tcol = null;
            }
            if ( tcol != null ) {
                return makeActivator( tcol );
            }
            else {
                return null;
            }
        }

        /**
         * If a column exists with the named UCD, select it in the selector.
         *
         * @param  ucd  UCD to match
         */
        protected void selectColumnByUCD( String ucd ) {
            for ( int i = 0; i < colSelector_.getItemCount(); i++ ) {
                TableColumn tcol = (TableColumn) colSelector_.getItemAt( i );
                if ( tcol instanceof StarTableColumn ) {
                    ColumnInfo cinfo = ((StarTableColumn) tcol).getColumnInfo();
                    if ( ucd.equals( cinfo.getUCD() ) ) {
                        colSelector_.setSelectedIndex( i );
                        break;
                    }
                }
            }
        }

        abstract Activator makeActivator( TableColumn tcol );

    }

    /**
     * Activator factory for displaying a column in an ImageWindow.
     */
    private class ImageActivatorFactory extends ColumnActivatorFactory {
        ImageActivatorFactory() {
            super( "Image" );

            /* If this is the result of a SIAP query, select the acref 
             * field for display by default. */
            selectColumnByUCD( "VOX:Image_AccessReference" );
        }
        Activator makeActivator( final TableColumn tcol ) {
            return new ColumnActivator( "image", tcol ) {
                String activateValue( Object val ) {
                    return val == null
                         ? null
                         : Image.displayImage( getWindowLabel( tcol ),
                                               val.toString() );
                }
            };
        }
    }

    /**
     * Activator factory for displaying a column in a spectrum viewer.
     */
    private class SpectrumActivatorFactory extends ColumnActivatorFactory {
        SpectrumActivatorFactory() {
            super( "Spectrum" );
        }
        Activator makeActivator( final TableColumn tcol ) {
            return new ColumnActivator( "spectrum", tcol ) {
                String activateValue( Object val ) {
                    return val == null
                         ? null
                         : Spectrum.displaySpectrum( getWindowLabel( tcol ),
                                                     val.toString() );
                }
            };
        }
        boolean isPossible() {
            return super.isPossible() && TopcatUtils.canSplat();
        }
    }

    /**
     * Activator superclass for activators that do something with the
     * content of a given column.
     */
    private abstract class ColumnActivator implements Activator {
        final int icol_;
        final String funcName_;

        /**
         * Constructs a new ColumnActivator.
         *
         * @param  funcName  name of the function (for reporting to user)
         * @param  tcol   the table column on which this will operate
         */
        ColumnActivator( String funcName, TableColumn tcol ) {
            icol_ = tcol.getModelIndex();
            funcName_ = funcName;
        }

        /**
         * Concrete subclasses must implement this to do something or
         * other with the value from a table cell.
         * 
         * @param  value  value to operate on
         * @return  report string for possible presentation to the user
         */
        abstract String activateValue( Object value ); 

        public String activateRow( long lrow ) {
            Object value;
            try {
                value = tcModel_.getDataModel().getCell( lrow, icol_ );
            }
            catch ( IOException e ) {
                value = null;
            }
            return value == null ? null
                                 : activateValue( value );
        }

        public String toString() {
            String colName = tcModel_.getDataModel().getColumnInfo( icol_ )
                                     .getName();
            return funcName_ + "( " + colName + " )";
        }
    }

}
