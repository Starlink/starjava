package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.func.Activation;

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
            new SogActivatorFactory(),
            new SplatActivatorFactory(),
            new JELActivatorFactory(),
        };

        /* Set up the window outline. */
        JComponent mainBox = Box.createVerticalBox();
        mainBox.add( Box.createHorizontalStrut( 400 ) );
        getMainArea().add( mainBox );

        /* Add a new row for each activator factory. */
        ButtonGroup buttGroup = new ButtonGroup();
        for ( int i = 0; i < factories.length; i++ ) {
            ActivatorFactory fact = factories[ i ];
            JRadioButton butt = fact.button_;
            butt.setEnabled( fact.isPossible() );
            buttGroup.add( butt );
            Box hbox = Box.createHorizontalBox();
            hbox.add( butt );
            hbox.add( fact.getQueryComponent() );
            hbox.add( Box.createHorizontalGlue() );
            mainBox.add( hbox );
            fact.setEnabled( false );
        }
        factories[ 0 ].button_.setSelected( true );

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this, true ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "ActivationQueryWindow" );

        /* Show the window. */
        pack();
        setVisible( true );
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
     * Helper class defining the appearance and fucntionality of an
     * option for creating new Activator objects on the basis of
     * user selections.
     */
    private abstract class ActivatorFactory implements ChangeListener {
        String description_;
        JRadioButton button_;
        JPanel queryPanel_;

        /**
         * Constructs a new factory.
         *
         * @param  desc  description string
         */
        ActivatorFactory( String desc ) {
            description_ = desc;
            button_ = new JRadioButton( desc );
            button_.addChangeListener( this );
            queryPanel_ = new JPanel();
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
        abstract void setEnabled( boolean enabled );

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
        void setEnabled( boolean enabled ) {
        }
        Activator makeActivator() {
            return Activator.NOP;
        }
    }

    /** 
     * Factory implementation for the user to enter custom JEL code.
     */
    private class JELActivatorFactory extends ActivatorFactory
                                      implements ActionListener {
        JComponent qcomp_;
        JTextField codeField_;

        JELActivatorFactory() {
            super( "Execute Custom Code: " );
            codeField_ = new JTextField( 24 );
            codeField_.addActionListener( this );
            queryPanel_.add( codeField_ );
        }

        void setEnabled( boolean enabled ) {
            codeField_.setEnabled( enabled );
        }

        Activator makeActivator() {
            String expr = codeField_.getText();
            try {
                return new JELActivator( tcModel_, expr );
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

        public void actionPerformed( ActionEvent evt ) {
            invokeOK();
        }
    }

    /**
     * Activator factory superclass for actions that do something to the
     * content of a single column.
     */
    private abstract class ColumnActivatorFactory extends ActivatorFactory {
        JComboBox colSelector_;

        ColumnActivatorFactory( String descrip ) {
            super( descrip );
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
            queryPanel_.add( colSelector_ );
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

        abstract Activator makeActivator( TableColumn tcol );

        void setEnabled( boolean enabled ) {
            colSelector_.setEnabled( enabled );
        }
    }

    /**
     * Activator factory for displaying a column in SOG.
     */
    private class SogActivatorFactory extends ColumnActivatorFactory {
        SogActivatorFactory() {
            super( "Display Image in SoG for Column: " );
        }
        Activator makeActivator( TableColumn tcol ) {
            return new ColumnActivator( "sog", tcol ) {
                String activateValue( Object val ) {
                    return val == null 
                         ? null
                         : Activation.sog( val.toString() );
                }
            };
        }
        boolean isPossible() {
            return TopcatUtils.canSog();
        }
    }

    /**
     * Activator factory for displaying a column in SPLAT.
     */
    private class SplatActivatorFactory extends ColumnActivatorFactory {
        SplatActivatorFactory() {
            super( "Display Spectrum in SPLAT for Column: " );
        }
        Activator makeActivator( TableColumn tcol ) {
            return new ColumnActivator( "splat", tcol ) {
                String activateValue( Object val ) {
                    return val == null
                         ? null
                         : Activation.splat( val.toString() );
                }
            };
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
