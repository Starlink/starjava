package uk.ac.starlink.startask;

import java.io.*;
import java.util.*;
import javax.swing.*;          //This is the final package name.
//import com.sun.java.swing.*; //Used by JDK 1.2 Beta 4 and all
                               //Swing releases before Swing 1.1 Beta 3.
import java.awt.*;
import java.awt.event.*;
import org.xml.sax.*;

import uk.ac.starlink.jpcs.ArrayParameterValue;
import uk.ac.starlink.jpcs.IfxHandler;
import uk.ac.starlink.jpcs.Msg;
import uk.ac.starlink.jpcs.Parameter;
import uk.ac.starlink.jpcs.ParameterList;
import uk.ac.starlink.jpcs.ParameterValueList;
import uk.ac.starlink.jpcs.TaskReply;

/** A class that provides a general purpose GUI to drive a web service Starlink
 *  task based on the JPCS Parameter and Control Subsystem. 
*/
public class WSGUI {
   String serviceName;
   String taskName;
   StarlinkService request;
   StarlinkService runner;
   ParameterList plist;
   GUIMsg info;
   int nReadParams;
   String[] parNames;
   JTextField[] valueFields;
   JTextArea textArea;

/**
 *  Displays a Graphical User Interface to run 
 *  The sub-Components are created using information in the task's interface
 *  file and current value list. An attempt is made to find an interface file
 *  <code>taskName.ifx</code> in the current working directory - if this fails
 *  the interface file is requested from the server using the Service.getIfx()
 *  method.
 *  An attempt is made to find a current value list in the user's working
 *  directory. If this fails, no current values will be available as suggested
 *  values. On completion a list of current values is return and will be saved
 *  in the current working directory for future use.
 */
     public static void main(String[] args) throws Exception {
        WSGUI app = new WSGUI();
        app.run( args );
     }
     

/** Implements the WSGUI
 */
     private void run( String[] args ) throws Exception {   
        TaskReply taskReply;
        if ( args.length != 2 ) {
           throw new Exception( 
            "ERROR - Usage java uk.ac.starlink.startask.WSGUI Service task" );

        }
        serviceName = args[0];
        taskName = args[1];
        
        try{
           UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName() );
//           UIManager.setLookAndFeel(
//                UIManager.getCrossPlatformLookAndFeelClassName());
           JFrame.setDefaultLookAndFeelDecorated( true );
        } catch ( Exception e ) { }      


//        try{
           // Make a connection with the service
           request = new StarlinkService( serviceName );
           runner = new StarlinkService( "ShellRunner" );
//        } catch ( Exception e ) {
//           System.out.println(
//            "WSGUI failed to connect with service " + serviceName );
//           System.out.println( e.getMessage() );
//           System.exit(1);           
//        }

           // Create the top-level container and add contents to it.
           JFrame frame = new JFrame( "WSGUI" );
        
           // Get the ParameterList
           try{
              plist = ParameterList.readIfx(
                               new FileInputStream( taskName + ".ifx" ) );
              plist.setTaskName( taskName );
              
           } catch( FileNotFoundException e ) {
           // There isn't a local Interface File - request it from the server
System.out.println("Requesting .ifx from server");
              String[] reply = request.runTask( "getIfx", taskName );
//for( int i=0;i<reply.length;i++)System.out.println(reply[i]);

/* Get the constituents of the reply Msg */
              IfxHandler handler = new IfxHandler();

              try{
//System.out.println("Handling reply");
                 plist = handler.readIfx( reply );
              } catch ( Exception ifxerr ) {
//System.out.println("Exception handling reply");
                 Msg ifxMsg = new Msg( reply );
                 ifxMsg.head( "Failed to get Interface File" );
                 ifxerr.printStackTrace();
                 plist = null;
              }
//System.out.println("List Msg");
//Msg tmp = new Msg( ifx );
//tmp.end();
//tmp.list();
           }
           
           
           if( plist != null ) {
           // Set the current value list for the ParameterList
              
              plist.setCurrentList();
              
//              String[] cl = request.getStringArray( "getCurrent", taskName );
//              taskReply = TaskReply.readReply( cl );
//              plist.setCurrentList( taskReply.getPVList() );
           
              Component contents = createComponents();
              frame.getContentPane().add(contents, BorderLayout.CENTER);

              // Finish setting up the frame, and show it.
              frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
//System.out.println("window closing");
                   System.exit(1);
                }
              });
              frame.pack();
              frame.setVisible(true);
           }
    }


/** Creates a swing Component to be displayed in the GUI. Sub-Compnents are
 *  constructed using information from the given {@link ParameterList}.
 *
 *  The Component contains:
 *     <ul>
 *     <li>A Label giving the name of the webservice (e.g. Kappa) and the task
 *     (e.g. stats).
 *     <li>A series of TextBoxes, one for each possible input parameter.
 *         (the initial entries in the TextBoxes will be the suggested value
 *         for the {@link Parameter} but note that 'global' and 'dynamic'
 *         on the <code>ppath</code> are ignored.
 *     <li>A RUN button which will cause the task to be run with a command
 *         line constructed from the parameter values currently in the
 *         TextBoxes.
 *     <li>A TextArea in which the output from the task will be displayed.
 *     </ul>
 *  @param the {@link ParameterList}
 *  @return the Component
 */
   public Component createComponents( ) throws Exception {
      int nparams;
               
// Set up the text area
        textArea = new JTextArea(20,80);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea,
                                       JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      
// GUIMsg for output of diagnostic information
        info = new GUIMsg( textArea );
        info.setBuffered( false );

      /* Create the task name label */
      JLabel taskLabel = new JLabel(
       "Parameters for " + serviceName + ":" + taskName,
       SwingConstants.CENTER );          
            
      /* Create the Parameter name/prompt Pane */
      JPanel namePane = new JPanel();
      namePane.setLayout( new GridLayout( 0, 1 ) );        

      /* Create the Text fields pane */
      JPanel valuePane = new JPanel();
      valuePane.setLayout( new GridLayout( 0, 1 ) );
        
      /* Add each Parameter to each pane */
      /* First create arrays to hold the panes for each parameter */
      /* We create arrays the maximum possible required size */
      /* but only Read parameters are entered */
      nparams = plist.size() - 1;
//System.out.println("nparams " + nparams );
      parNames = new String[ nparams ];
      JLabel[] nameLabels = new JLabel[ nparams ];
      valueFields = new JTextField[ nparams ];
         
      Iterator it = plist.iterator();
      int id = 0;
      // discard the first (dummy) Parameter
      Parameter p = (Parameter)it.next();

      while( it.hasNext() ) {
//System.out.println( "id " + id );
         p = (Parameter)it.next();
         if( p.isRead() ) {
            parNames[ id ] = p.getKeyword();
            nameLabels[ id ] = new JLabel( parNames[ id ] + ":  " +
                                           p.getPromptString() + "  " );
            Object suggested = p.getSuggestedValue();
            String sugStr;
            if ( suggested != null ) {
               sugStr = suggested.toString();
            } else {
               sugStr = "";
            }
            valueFields[ id ] = new JTextField( sugStr, 20 );
            // Tell accessibility tools about label/textfield pairs
            nameLabels[ id ].setLabelFor( valueFields[ id ] );
//System.out.println( valueFields[id].getText() );
            namePane.add( nameLabels[id] );
            valuePane.add( valueFields[ id ] );
            id++;
         }   
      } 

// Set the number of Read parameters to be displayed */
      nReadParams = id;
      if( nReadParams == 0 ) {
         taskLabel.setText( "No Input Parameters for " + serviceName + ":"
          + taskName );
      }
                    
// Set up the RUN button
      JButton runButton = new JButton("RUN");
      runButton.setMnemonic(KeyEvent.VK_I);
      RunListener runListener = new RunListener();
      runButton.addActionListener( runListener );

// Put the button in a container
      JPanel runContainer = new JPanel();
      runContainer.add( runButton );
      
         //Put the panels in another panel, labels on left,
         //text fields on right.
         JPanel contentPane = new JPanel();
         contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
         contentPane.setLayout(new BorderLayout());
         contentPane.add(namePane, BorderLayout.CENTER);
         contentPane.add(valuePane, BorderLayout.EAST);
// and put in yet another container
         JPanel contentPane2 = new JPanel();
         contentPane2.add( contentPane );
        
         /*
         * An easy way to put space between a top-level container
         * and its contents is to put the contents in a JPanel
         * that has an "empty" border.
         */
         JPanel pane = new JPanel();
         pane.setBorder(BorderFactory.createEmptyBorder(
                                        30, //top
                                        30, //left
                                        10, //bottom
                                        30) //right
                                        );
//         pane.setLayout( new BoxLayout(pane, BoxLayout.Y_AXIS) );
//         pane.setLayout( new GridLayout(0,1) );
         GridBagConstraints guiConstraints = new GridBagConstraints();
         guiConstraints.fill = GridBagConstraints.HORIZONTAL;
         guiConstraints.gridwidth = GridBagConstraints.REMAINDER;

         GridBagLayout guiLayout = new GridBagLayout();
         guiLayout.addLayoutComponent( taskLabel, guiConstraints );
         guiLayout.addLayoutComponent( contentPane2, guiConstraints );
         guiLayout.addLayoutComponent( runContainer, guiConstraints );
         guiLayout.addLayoutComponent( scrollPane, guiConstraints );
         pane.setLayout( guiLayout );

         pane.add( taskLabel );
         pane.add( contentPane2 );
         pane.add( runContainer );
         pane.add( scrollPane );

         return pane;
     }

/** Listen for the RUN button to be pressed.
 *  When it is, construct a command line with keyword=value for each of the
 *  input parameters using the value currently in the parameter TextBoxes.
 *  Send the command request to the server and display the reply in the
 *  TextArea.
 */
    public class RunListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
/* Create the commad line array - allow for the ShellRunner method (runPack),
 * the package, the package method, the input parameters and 'NOPROMPT'.
 */ 
       String[] cmdline = new String[ nReadParams + 3];
       String params = "";
       boolean err;
       cmdline[0] = serviceName;
       cmdline[1] = taskName;            

       err = false;
       try{
          for ( int i=0; i<nReadParams; i++ ) {
             String val = valueFields[i].getText();
//System.out.println( i + ": " + val );
             String val1 = valueCheck( val );
             cmdline[i+2] = parNames[i] + "=" + val1;
             params = params + " " + cmdline[i+2];
          }
/* Add the NOPROMPT keyword to prevent prompting on the server */
          cmdline[nReadParams+2] = "NOPROMPT";

       } catch( Exception exc ) {
//System.out.println("Exception reported");
          info.out( exc + "\n" );
          err = true;
       }

       if( !err ) {
          textArea.append(
           "Run ShellRunner runPack " + serviceName + " " + taskName + params +
           " NOPROMPT\n" );
          String[] results = new String[0];                
          try{ 
//                   GUIMsg output = new GUIMsg();
//                   output.out("Run task\n");
             results = runner.runTask( "runPack", cmdline );
//for ( int i=0; i<results.length; i++ ) System.out.println( results[i] );

/* Construct a TaskReply from the returned array of String */
             TaskReply taskReply = TaskReply.readReply( results );                 
//System.out.println("PVList from parsed reply");

/* Handle the returned messages */
             GUIMsg output =
               new GUIMsg( textArea,
                (String[])( taskReply.getMsg().toArray(new String[0]) ));
//System.out.println("Number of messages in reply " + output.size() );
                  output.flush();
                  
/* Now handle the returned ParameterValueList */
             ParameterValueList pvList = taskReply.getPVList();
             if ( pvList != null ) {
                plist.setCurrentList( pvList );
                plist.setToCurrentValue();
                plist.deActivate( true );

             } else {
                System.out.println( "No ParameterValueList returned" );
             }

          } catch ( Exception exc ) {
             System.out.println( "Exception raised: " + exc );
//              Display the results, if any
             for ( int i=0; i<results.length; i++ ) {
                System.out.println( results[i] );
             }
          }
          
       }
   }
         

/** Checks the given String as a parameter value, enclosing open array values in
  * '[]'.
  * @param the String to be checked.
  * @return the given String or the given String enclosed in '[]'.
  * @throws {@link Exception} if the given String is blank.
  */
         String valueCheck( String value ) throws Exception {
            if( value.trim().length() == 0 ) {
               throw new Exception(
                "You must provide a value for all Parameters" );
            } else if ( ArrayParameterValue.isOpenArray( value ) ) {
                return "[" + value + "]";
            } else {
               return value;
            }
         }
            
          
     }

/** A class to handle messages destined for a TextArea.
 */      
     static class GUIMsg extends Msg {
        JTextArea textArea;     

/** Creates a buffered GUIMsg containing the messages in the given String array.
 *  Messages are saved until flushed.
 *  @param The JTextArea in which to display the messages
 *  @param The initial contents of the GUIMsg
 */ 
        GUIMsg( JTextArea textarea, String[] array ) {
           super( array );
           this.setBuffered( true ); 
           textArea = textarea;
        }

/** Creates an unbuffered GUIMsg. Messages will be output immediately.
 */
        GUIMsg( JTextArea textarea ) {
           super();
           textArea = textarea;
        }
     
/** Displays the given message in the TextArea associated with this GUIMsg.
 *  @param the message to be displayed.
 */
        protected void write( String message ) {
           textArea.append( message + "\n");
        }
     
     }
        
}
