package uk.ac.starlink.splat.vo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.ValuesElement;

public class DataLinkQueryFrame extends JFrame implements ActionListener, DocumentListener {

    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.vo.DataLinkQueryFrame" );

    HashMap< String, DataLinkParams > servers; // one entry for each datalink supporting server
    /** The list of all dataLink parameters read from the servers as a hash map */
    private  HashMap< String, String > dataLinkParam=null;  // pairs param - value
    /** List of all possible parameters **/
    private HashMap<String,String> accessURL; // the query access URL 
    private HashMap<String,String> idSource; // the query id source 
    private String currentServer = null; // HashMap for accessURL and idSourcem key=server!!
   // private ArrayList <String> [] accessURL = new String[]();
    

    
    Boolean isActive;
    // Panel for components and options from dataLink parameters
    private JPanel dataLinkPanel;
    private JPanel paramPanel;
   // private JScrollPane  dataLinkScroller; 
    private JButton submitButton;
    private JButton clearButton;
    private ArrayList<Component> queryComponents;
    private JComboBox optbox;
    private JScrollPane scroller; 
   
    
    public DataLinkQueryFrame() {
        isActive = false;
        servers = new HashMap< String, DataLinkParams >();
        dataLinkParam = new HashMap<String, String>();
        accessURL= new HashMap<String, String>();
        idSource= new HashMap<String, String>();
        queryComponents = new ArrayList<Component>();
       
        initUI();        
    }
   
    public void addServer( String server, DataLinkParams dlparams ) {
        DataLinkParams dlp = servers.get(server);
        if (dlp == null) {          
            servers.put(server, dlparams);           
        }
   //     } else {
            // DO WHAT? Compare parameters?
   //     }
    }
    
    private void resetpanels() // is it still needed? 
    {
 // RESET dataLink panels
        
     //   dataLinkPanel = null;
    //    paramPanel = null;
      //  dataLinkScroller  = null; 
        
        dataLinkParam.clear();
        queryComponents.clear();

    }
    
    protected void initUI()
    {
        this.setSize(450, 250);
   
             dataLinkPanel = (JPanel) this.getContentPane();
          //  dataLinkPanel.setPreferredSize(new Dimension(480,180));
            dataLinkPanel.setLayout(new GridBagLayout());
         
            GridBagConstraints gbc = new GridBagConstraints();
          
            
            dataLinkPanel.setAlignmentY(CENTER_ALIGNMENT);
            dataLinkPanel.setBorder(BorderFactory.createTitledBorder("Parameters for Server-Generated data processing"));
            Border empty = BorderFactory.createEmptyBorder(); 
   
            
            paramPanel = new JPanel();
            paramPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
         //   paramPanel.setMinimumSize(new Dimension(350,250));
            paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
         
            paramPanel.setAlignmentY(CENTER_ALIGNMENT);
               
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setBorder(empty);
           
            clearButton = new JButton("Clear parameters");
            clearButton.addActionListener(this);
            clearButton.setName("clearButton");
           // clearButton.setMargin(new Insets(2,10,2,10));  
            buttonsPanel.add(clearButton);
            
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx=0;
            gbc.gridy=0;
            gbc.weighty=0;
            gbc.weightx=0;
            gbc.fill=GridBagConstraints.HORIZONTAL;
            dataLinkPanel.add(paramPanel, gbc); //(paramPanel);
            
           
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx=0;
            gbc.gridy=1;
            gbc.weighty=1;
            gbc.weightx=1;
            gbc.fill=GridBagConstraints.NONE;
        
            dataLinkPanel.add(buttonsPanel, gbc);
     
    }

    private void setMaximumSize(int i, int j) {
        // TODO Auto-generated method stub
        
    }

    public void addToUI(String shortname, DataLinkParams dlp ) {
   
          
              
           paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
           Border empty = BorderFactory.createEmptyBorder(); 
           paramPanel.setBorder(empty);
           paramPanel.setAlignmentY(CENTER_ALIGNMENT);
       
           // for each service offered 
           for (int service=0; service< dlp.getServiceCount(); service++ ) 
           {
               // get query parameters (defined in the GROUP element)
               ParamElement[] params = dlp.getQueryParams(service);
              
               JPanel servicePanel = new JPanel();
               servicePanel.setBorder(BorderFactory.createLineBorder(Color.gray));
               servicePanel.setLayout(new BoxLayout(servicePanel, BoxLayout.Y_AXIS));
               int i=0;  
               while ( i < params.length ) {

                   JPanel inputPanel = new JPanel();
                   inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
                   inputPanel.setBorder(empty);
                   String paramName = params[i].getName();
                   JLabel paramLabel=new JLabel(paramName+" : ");
                   inputPanel.add(paramLabel);
                   inputPanel.setAlignmentX(LEFT_ALIGNMENT);
                   //     inputPanel.setAlignmentY(CENTER_ALIGNMENT);

                   String description = params[i].getDescription();                  
                   String datatype = params[i].getAttribute("datatype");
                   String value = params[i].getValue();
                   ValuesElement values = (ValuesElement) params[i].getChildByName("VALUES");
                   String [] options = null;
                   if (values != null )
                       options = values.getOptions();

                   // TO DO: add MIN/MAX values to description!!!!!!!!!!!!

                   if ( options != null && options.length > 0 ) {
                       optbox = new JComboBox(options);
                       optbox.addItem("");
                       value="";
                      /* if (paramName.equals("FORMAT")) { // choose best format for SPLAT as default
                           for (i=0;i<options.length; i++) {
                               if ( options[i].contains("application/x-votable")) {
                                   value = options[i];
                               } else if ( value == null && options[i].contains("application/fits")) {
                                   value = options[i];
                               }
                           }
                       } */
                       optbox.setSelectedItem(value);
                       optbox.setMaximumSize( optbox.getPreferredSize() );

                       //   optbox.setName("gd:"+shortname+":"+paramName);
                       optbox.setName(paramName);
                       optbox.addActionListener(this);
                       if (description.length() > 0)
                           optbox.setToolTipText(description);
                       inputPanel.add(optbox);
                       queryComponents.add(optbox);

                   } else {
                       JTextField paramField = new JTextField(8);

                       if (description != null && description.length() > 0) {
                           paramField.setToolTipText(description);
                       }
                       paramField.setName(paramName);
                       paramField.addActionListener(this);
                       paramField.getDocument().putProperty("owner", paramField); //set the owner
                       paramField.getDocument().addDocumentListener(this);
                       Dimension size=new Dimension(paramField.getPreferredSize().width, paramField.getPreferredSize().height+5);
                       paramField.setMinimumSize(size);
                       paramField.setMaximumSize(size);
                     
                       VOElement constraint = values.getChildByName("MIN"); 
                       String min=constraint.getAttribute("value");
                       constraint = values.getChildByName("MAX");
                       String max=constraint.getAttribute("value"); 
                       if (min != null || max != null)
                           description = description + "  values: ["+min+".."+max+"]";
                       paramField.setToolTipText(description);
                       
                       inputPanel.add(paramField);
                       queryComponents.add(paramField);
                       
                       /* JTextField minField = new JTextField(8);
                        JTextField maxField = new JTextField(8);

                        minField.setMaximumSize( minField.getPreferredSize() );
                        inputPanel.add(minField);
                        inputPanel.add(new JLabel(" / "));
                        inputPanel.add(maxField);                        
                        minField.setName(paramName+":Min");
                        maxField.setName(paramName+":Max");
                        if (paramName.contains("BAND")) {
                            String bandText=dataLinkParam.get(paramName);
                            if (bandText != null) {
                              int divisor=bandText.indexOf('/');
                              minField.setText(bandText.substring(0,divisor));
                              maxField.setText(bandText.substring(divisor+1));
                            }
                        }
                        if (description.length() > 0) {
                            minField.setToolTipText(description);
                            maxField.setToolTipText(description);
                        }
                        minField.addActionListener(this);
                        maxField.addActionListener(this);
                     //   inputPanel.add(new JLabel("["+min+".."+max+"]"));

                        queryComponents.add(minField);
                        queryComponents.add(maxField);
                        */

                   } // else
                   servicePanel.add(inputPanel);                       
                   i++;
               } //while
               if ( servicePanel.getComponentCount() > 0 ) { // !!!!! for the moment, add only parameters for visible query components!
                  accessURL.put(currentServer, dlp.getQueryAccessURL(service));
                  idSource.put(currentServer, dlp.getQueryIdSource(service));    
                  paramPanel.add(servicePanel);
               }
           }// for
           
    } // addToUI

    
    public String  getAccessURL() {
        return accessURL.get(currentServer);
    }
    public String  getIDSource() {
        return idSource.get(currentServer);
    }

    public void actionPerformed(ActionEvent e) {
       
        //
        // dynamically generated dataLink parameters
        //
        
        Component source = (Component) e.getSource();   
        
        if (source.equals(clearButton))
            dataLinkParam.clear();
        
        for (int i = 0; i < queryComponents.size(); i++) 
        {      
               Component c = queryComponents.get(i);
               if (c instanceof JComboBox) {
                   JComboBox cb = (JComboBox) c;
                   String name = cb.getName();
                   if (source.equals(clearButton)) {
                       cb.setSelectedItem("");
                   } else /*if (source.equals(submitButton))*/ {
                      // if ( cb.getSelectedItem().toString().length() > 0)
                           dataLinkParam.put(name, cb.getSelectedItem().toString());
                   } 
               }
               if (c instanceof JTextField) {
                   JTextField tf = (JTextField) c;
                   String name = tf.getName();
                   if (source.equals(clearButton)) {
                       tf.setText("");
                   } else if (source.equals(submitButton)) {
                       dataLinkParam.put(name, tf.getText());                 
                   }
                   
               }
        } // for
     
       
    }
    
    
    public String setServer(String server) {
        
        paramPanel.removeAll();
        if (servers.get(server) == null)
            return null;
        currentServer=server;
        dataLinkParam.clear();
        queryComponents.clear();
        addToUI(server, servers.get(server));
        paramPanel.updateUI();
        dataLinkPanel.revalidate();
        dataLinkPanel.repaint();
        return ("OK");
        
    }
    
    public HashMap<String,String> getParams() {
        if (dataLinkParam != null) {
            return dataLinkParam;
        }
     //   if (servers == null )
            return null;
      //  return servers.get(currentServer);
    }

    public void changedUpdate(DocumentEvent arg0) {
     // Plain text components don't fire these events.
        
    }

    public void insertUpdate(DocumentEvent de) {
        
        //get the owner of this document
        Object owner = de.getDocument().getProperty("owner");
        JTextField param=(JTextField) owner;
        
        dataLinkParam.put(param.getName(), param.getText());
        
        /*
        String inputText = (String) ((JTextField) owner).getText();
       
        
        double radius = 0.0;
        if ( inputText != null && inputText.length() > 0 ) {
            try {
                double dvalue = Double.parseDouble( inputText );
            }
            catch (NumberFormatException e1) {
                ErrorDialog.showError( this, "Value should be a numeric", e1);                         
                return;
            }
        }
        */
        
    }

    public void removeUpdate(DocumentEvent arg0) {
        // TODO Auto-generated method stub
        
    }
    


}
