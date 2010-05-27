/** RVPanel.java
 * Provides a Swing GUI to run RV
 *
 * @author Roy Platon
 * @version 1.0 26 Mar 2002

 * <h1>Java GUI for RV - Calculation of Relative Velocities</h1>
 * <P>
 **/

package uk.ac.starlink.rv;

import java.io.*;
import java.util.*;
import java.text.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*  Positional Asronomy Library */
import uk.ac.starlink.pal.*;
/*  Needed for WebServices - temporily disabled */
// import uk.ac.starlink.util.*;

/**
 * Creates a dialogue box to enter Details.
 */
public class RVPanel extends JPanel implements ActionListener {
    private Pal pal;
    
/*
 * Constants
 */
    private final boolean TRACE = false;
    private final boolean DEBUG = false;

    private final String HELP = "Help";
    private final String RUN = "Run";
    private final String EXIT = "Exit";
//    private final String WSON = "WS on";
//    private final String WSOFF = "WS off";
    private final String SELECT = "Select File";
    private final Color FOREGROUND = Color.blue;
    private final Color BACKGROUND = Color.lightGray;
    private final Color WHITE = Color.white;

/*
 * Default settings
 */
    private String task = null;
    private String text = null;
    private File output = null;
    private boolean applet = false;
//    private boolean webservice = false;
    private boolean fileoutput = true;

    private static String Equinox[] = {
        "J2000", "B1975", "B1950", "B1900", "B1875", "B1855"
    };
    private static String Days[] = {
        "1", "2", "3", "4", "5", "6", "7", "14"
    };
    
    private String observatory;
    private String observatoryName;
    private String day = "1";
    private static String equinox_def= Equinox[2];
    private String equinox = equinox_def;
    private String outputname = "rv.lis";

    private final int XSIZE = 660;
    private final int YSIZE = 580;
    private final int TXSIZE = 600;
    private final int TYSIZE = 300;
/*
 * Text fields in dialogue box
 */
    private Container Pane;
    private GridBagConstraints dateConstraint;
    private GridBagConstraints dayConstraint;
    private GridBagConstraints raConstraint;
    private GridBagConstraints decConstraint;
    private JComboBox observatoryBox;
    private JFileChooser outputFileBox;
    private JTextField observatoryField;
    private JTextField outputFile;
    private JTextField dateField;
    private JTextField raField;
    private JTextField dayField;
    private JTextField decField;
    private JTextArea textarea;
    private JScrollPane scrollpane;
    private JComboBox equinoxBox;
    private JComboBox dayBox;
    private JButton button;
//    private JTextField namefield;
    private GridBagConstraints result_constraints;

    RVPanel ( Container pane ) {
        pal = new Pal ();
        Pane = pane;
    }

    public void paintComponent( Graphics gc ) {
        super.paintComponent( gc );
        gc.setColor( BACKGROUND );
        gc.fillRect( 0, 0, 800, 600 );
    }
/**
 * This displays the dialogue box and begins processing a command
 * @param task The task name
 */
    public void setup( String task ) {

        if ( task == null ) {
            applet = true;
            fileoutput = false;
            task = "Applet";
        }

        trace( getToday() );
        trace( "Setup: " + task );

        Pane.setBackground( BACKGROUND );
        Pane.setForeground( FOREGROUND );
        initPanel( Pane );
        Pane.setVisible(true);

        return;        
    }

    private void initPanel( Container pane ) {
        Font label_font = new Font( "SansSerif", Font.BOLD, 12 );

        trace( "initPanel: " + task );
        pane.setLayout( new GridBagLayout() );

        GridBagConstraints constraint = new GridBagConstraints();
        constraint.weightx = 0;
        constraint.weighty = 0;
        constraint.gridwidth = 1;
        constraint.gridheight = 2;
        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.anchor = GridBagConstraints.WEST;
        constraint.fill = GridBagConstraints.HORIZONTAL;

// Observatory Selection
        JLabel label = new JLabel("Observatory: ");
        label.setFont( label_font );
        pane.add( label, constraint );
        observatoryBox = new JComboBox();
        observatoryBox.setBackground( WHITE );
        observatoryBox.setEditable(false);
        for ( int i = 1; ; i++ ) {
            Observatory obs = pal.Obs( i );
            if ( i == 1 ) {
                observatoryName = obs.getName();
                observatory = obs.getId();
            }
            if ( obs.getName() == null ) break;
            String id = obs.getId();
            observatoryBox.addItem( id );
        }
        observatoryBox.addActionListener(this);
        constraint.gridx += 1;
        constraint.gridwidth = 2;
        pane.add( observatoryBox, constraint );
        constraint.gridx += 2;
        constraint.gridwidth = 1;
        pane.add( new JLabel("    "), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 2;
        observatoryField = new JTextField( observatoryName );
        observatoryField.setEditable(false);
        observatoryField.setBackground( WHITE );
        pane.add( observatoryField, constraint );

// Row Spacer & Column Spacer
        constraint.gridy += 2;
        constraint.gridx = 1;
        constraint.gridwidth = 1;
        pane.add( new JLabel("            "), constraint );

// Output File Selection
        if ( fileoutput ) {
            constraint.gridy += 2;
            constraint.gridx = 0;
            pane.add( new JLabel("Output File:"), constraint );
            constraint.gridx += 1;
            outputFile = new JTextField( outputname );
            constraint.gridwidth = 2;
            pane.add( outputFile, constraint );
            constraint.gridx += 3;
            constraint.gridwidth = 1;
            button = new JButton( SELECT );
            button.addActionListener(this);
            pane.add( button, constraint );

// Row Spacer
            constraint.gridy += 2;
            constraint.gridx = 0;
            constraint.gridwidth = 1;
            pane.add( new JLabel(" "), constraint );
        }

// Date and Day
        constraint.gridy += 2;
        constraint.gridx = 0;
        pane.add( new JLabel("Date:"), constraint );
        constraint.gridx += 1;
        dateField = new JTextField( getToday() );
        constraint.gridwidth = 2;
        dateConstraint = (GridBagConstraints) constraint.clone();
        pane.add( dateField, constraint );
        constraint.gridx += 3;
        constraint.gridwidth = 1;
        pane.add( new JLabel("Day (n): "), constraint );
        constraint.gridx += 1;
        dayBox = new JComboBox();
        dayBox.setEditable(true);
        for ( int i = 0; i < Days.length; i++ ) {
            dayBox.addItem( Days[i] );
        }
        dayBox.addActionListener(this);
        dayConstraint = (GridBagConstraints) constraint.clone();
        pane.add( dayBox, constraint );

// Row Spacer
        constraint.gridy += 2;
        constraint.gridx = 0;
        pane.add( new JLabel(" "), constraint );

// Coordinate Data
        constraint.gridy += 2;
        pane.add( new JLabel("R.A. (h m s): "), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 2;
        raField = new JTextField("");
        raConstraint = (GridBagConstraints)constraint.clone();
        pane.add( raField, constraint );
        constraint.gridx += 3;
        constraint.gridwidth = 1;
        pane.add( new JLabel("Dec. (d m s): "), constraint );
        constraint.gridx += 1;
        decField = new JTextField("");
        decConstraint = (GridBagConstraints)constraint.clone();
        pane.add( decField, constraint );

        constraint.gridy += 1;
        constraint.gridx = 0;
        pane.add( new JLabel(" "), constraint );
 
        constraint.gridy += 1;
        constraint.gridx = 4;
        pane.add( new JLabel("Equinox:"), constraint );
        equinoxBox = new JComboBox();
        equinoxBox.setEditable(true);
        for ( int i = 0; i < Equinox.length; i++) {
            equinoxBox.addItem( Equinox[i] );
        }
        equinoxBox.setSelectedItem( equinox_def );
        equinoxBox.addActionListener(this);
        constraint.gridx += 1;
        pane.add( equinoxBox, constraint );

// Add Webservice Option
//        if ( ! applet ) {
//            constraint.gridy += 2;
//            constraint.gridx = 5;
//            JCheckBox wserver = new JCheckBox( "Use Web Service" );
//            wserver.addActionListener(this);
//            wserver.setBackground( Pane.getBackground() );
//            pane.add( wserver, constraint );
//        }

// Add spacer
        constraint.gridy += 2;
        constraint.gridx = 0;
        pane.add( new JLabel(" "), constraint );

// Add Function Buttons
        ButtonPanel buttons = new ButtonPanel();
        buttons.setBackground( Pane.getBackground() );
        JButton button = new JButton( HELP );
        button.addActionListener(this);
        buttons.add( button, BorderLayout.WEST );

        button = new JButton( RUN );
        button.addActionListener(this);
        buttons.add( button, BorderLayout.CENTER );

        if ( ! applet ) {
            button.addActionListener(this);
            button = new JButton( EXIT );
            button.addActionListener(this);
            buttons.add( button, BorderLayout.EAST );
       }

        constraint.gridy += 2;
        constraint.gridx = 0;
        constraint.gridwidth = constraint.REMAINDER;
        pane.add( buttons, constraint );

/*
 *  Add Text Area for Results
 */
        constraint.gridy += 2;
        pane.add( new JLabel("Radial Velocity Results") );

        textarea = new JTextArea( 80, 40 );
        scrollpane = new JScrollPane( textarea );
        textarea.setBackground( Pane.getBackground() );
        scrollpane.setPreferredSize( new Dimension( TXSIZE, TYSIZE ) );
        constraint.gridy += 2;
        constraint.gridx = 0;
        constraint.gridwidth = constraint.REMAINDER;
        result_constraints = (GridBagConstraints) constraint.clone();
        pane.add( scrollpane, constraint );

        return;
    }

    class ButtonPanel extends JPanel {
       JPanel buttonPanel = new JPanel();
       JSeparator separator = new JSeparator();
       public ButtonPanel () {
//          buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
          buttonPanel.setLayout( new BorderLayout( 50, 5 ) );
          buttonPanel.setBackground( Pane.getBackground() );
          add( separator, "North" );
          add( buttonPanel, "Center");
       }
       public void add( JButton button, String pos ) {
           buttonPanel.add( button, pos );
       }
       public void add( JButton button ) {
           buttonPanel.add( button );
       }
    }

/**
 * The main event processing method
 * @param evt The Event
 */
     public void actionPerformed ( ActionEvent evt ) {
        Object source = evt.getSource();
        String arg = evt.getActionCommand();

        if ( source instanceof JComboBox ) {
            int index = 0;
            trace( "Combo Box Event: " + arg );
            JComboBox box = (JComboBox)source;
            String selection = (String)box.getSelectedItem();
 
            trace( selection + " selected" );
            if ( box == observatoryBox ) {
                Observatory obs = pal.Obs( selection );
                if ( obs != null ) {
                    observatory = selection;
                    observatoryName = obs.getName();
                    observatoryField.setText( observatoryName );
                }
                trace( "Telescope: " + observatoryName );
            } else if ( box == equinoxBox ) {
                equinox = selection;
            } else if ( box == dayBox ) {
                day = selection;
            }
            Pane.validate();
        }
/*  Checkbox for Web Services option - Disabled */
//        if ( source instanceof JCheckBox ) {
//            int index = 0;
//            trace( "Check Box Event: " + arg );
//            JCheckBox box = (JCheckBox) source;
//            webservice = box.isSelected();
// 
//            if ( webservice ) trace( "web service selected" );
//            else              trace( "web service not selected" );
//            Pane.validate();
//        }

        if ( source instanceof JButton ) {
            trace( "Button Event: " + arg );
            if ( arg.equals( SELECT ) ) {
               trace( "Get Output File");
               JFileChooser outputFileBox = new JFileChooser( new File(".") );
               outputFileBox.showOpenDialog( null );
               output = outputFileBox.getSelectedFile();
               outputname = output.getName();
               outputFile.setText(outputname);
               trace( "File " + outputname + " selected");
            } else if ( arg.equals( RUN ) ) {
                trace( "Run");
                if ( output == null ) {
                   output = new File( outputname );
                }
                try {
                   calculate();
                }
                catch ( palError e ) {
                    trace ( "ERROR: " + e );
                    JOptionPane errPane =
                        new JOptionPane( e, JOptionPane.ERROR_MESSAGE );
                    JDialog dialog = errPane.createDialog ( button, "Error" );
                    dialog.setVisible( true );
                }
            } else if ( arg.equals( HELP ) ) {
                trace( "Help");
                System.out.println("No Help Available yet");
                JOptionPane helpPane = 
                   new JOptionPane( "No Help Available yet",
                         JOptionPane.INFORMATION_MESSAGE );
                JDialog dialog = helpPane.createDialog (
                   button, "Help" );
                dialog.setVisible( true );
            } else if ( arg.equals( EXIT ) ) {
                trace( task + " killed");
                if ( ! applet ) System.exit(0);
            }
            Pane.validate();
        }
    }

    private void calculate( ) throws palError {
        Vector out = null;
        if ( TRACE ) {
           trace( "Run RV Program here" );
           trace( "Observatory: " + observatoryName );
           trace( "Output File: " + outputname );
           trace( "Date: " + dateField.getText() );
           trace( "Days: " + day );
           trace( "RA: " + raField.getText() );
           trace( "Dec: " + decField.getText() );
           trace( "Equinox: " + equinox );
        }
        if ( observatoryName == null ) throw 
                    new palError( "No Observatory specified" );

        try {
/* Web Service code - temporarily disabled */
//            if ( webservice ) {
//                trace ( "Run Web Service here" );
//                SOAPmessage msg = new SOAPmessage( "RV" );
//                trace ( "Observatory " + observatory );
//                msg.send ( "start", observatory );
//                trace ( "Date " + dateField.getText() );
//                msg.send ( "setDate", dateField.getText() );
//                trace ( "Equinox " + equinox );
//                msg.send ( "setEquinox", equinox );
//                trace ( "Days " + day );
//                msg.send ( "setDays", day );
//                trace ( "Calculate " + raField.getText() +" " + decField.getText() );
//                String[] mess = msg.getList ( "calculate",
//                                    raField.getText(), decField.getText() );
//                out = new Vector();
//                for ( int i = 0; i < mess.length; i++ ) out.add( mess[i] );
//            } else {
                RadialVelocity rv = new RadialVelocity( );
                rv.setObservatory( observatory );
                rv.setDate( dateField.getText() );
                rv.setDays( day );
                rv.setEquinox ( equinox );
                out = rv.calculateRV( raField.getText(), decField.getText() );
//            }
        }
        catch ( palError e ) {
            throw e;
        }
        catch ( Exception e ) {
            throw new palError( e.toString() );
        }
        trace( "Text Area Size: " + out.size() );
        textarea = new JTextArea( 120, out.size() );
        textarea.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
        Iterator i = out.iterator();
        FileWriter ofile = null;
        if ( fileoutput ) {
            try {
                ofile = new FileWriter( outputname );
                while ( i.hasNext() ) {
                    String str = (String) i.next() + "\n";
                    textarea.append( str );
                    if ( fileoutput ) ofile.write( str );
                }   
                if ( fileoutput ) ofile.close( );
            }
            catch ( IOException e ) { }
        } else {
            while ( i.hasNext() ) {
                String str = (String) i.next() + "\n";
                textarea.append( str );
            }
        }

        Pane.remove( scrollpane );
        scrollpane = new JScrollPane( textarea );
        scrollpane.setPreferredSize( new Dimension( TXSIZE, TYSIZE ) );

        Pane.add ( scrollpane, result_constraints );
        Pane.validate();
    }

    private void trace( String message )
    {
        if ( TRACE ) System.err.println( "[rv Panel] " + message );
        return;
    }

    private String getToday( ) {
        DateFormat currentDateFormat =
                DateFormat.getDateInstance( DateFormat.SHORT, Locale.UK );
//        Date currentDate = new Date();
        Calendar today = Calendar.getInstance();
        int day = today.get(today.DATE);
        int month = today.get(today.MONTH) + 1;
        int year = today.get(today.YEAR);
        int days = today.get(today.DAY_OF_YEAR);

        return year + " " + month + " " + day;
//        return currentDateFormat.format(currentDate);
    }
}
