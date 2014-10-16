package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.ValuesElement;

public class GetDataQueryFrame extends JFrame implements ActionListener {
    
    
    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.vo.GetDataQueryFrame" );

    HashMap< String, GetDataTable > services;
    /** The list of all getData parameters read from the servers as a hash map */
    private  HashMap< String, String > getDataParam=null; 
    /** List of all possible parameters **/
   // private ArrayList<String> paramList;
    
    Boolean isActive;
    // Panel for components and options from GetData parameters
    private JPanel getDataPanel;
    private JPanel paramPanel;
   // private JScrollPane  getDataScroller; 
    private JButton submitButton;
    private JButton clearButton;
    private ArrayList<Component> queryComponents;
    private JComboBox optbox;
    private JScrollPane scroller; 
    
      public GetDataQueryFrame() {
        isActive = false;
        services = new HashMap< String, GetDataTable >();
        getDataParam = new HashMap<String, String>();
        queryComponents = new ArrayList<Component>();
       
        initUI();        
    }
   
    public void addService( String service, GetDataTable gdtable ) {
        GetDataTable gdt = services.get(service);
        if (gdt == null) {          
            services.put(service, gdtable);           
        }
   //     } else {
            // DO WHAT? Compare parameters?
   //     }
    }
    
    private void resetpanels() // is it still needed? 
    {
 // RESET getData panels
        
        getDataPanel = null;
        paramPanel = null;
      //  getDataScroller  = null; 
        
        getDataParam.clear();
        queryComponents.clear();

    }
    
    protected void initUI()
    {
        this.setSize(400, 200);
            getDataPanel = (JPanel) this.getContentPane();
            getDataPanel.setLayout((new BoxLayout(getDataPanel, BoxLayout.Y_AXIS)));
           // getDataPanel.setAlignmentY(CENTER_ALIGNMENT);
            getDataPanel.setBorder(BorderFactory.createTitledBorder("Parameters for Server-Generated data processing"));
            Border empty = BorderFactory.createEmptyBorder(); 
   
            paramPanel = new JPanel();
            paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
            paramPanel.setBorder(empty);
            paramPanel.setAlignmentY(CENTER_ALIGNMENT);
               
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setBorder(empty);
            buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
           // buttonsPanel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
         
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
            
           
            clearButton = new JButton("clear Parameters");
            clearButton.addActionListener(this);
            clearButton.setName("clearButton");
           // clearButton.setMargin(new Insets(2,10,2,10));  
            buttonsPanel.add(clearButton);
            
            buttonsPanel.add(Box.createRigidArea(new Dimension(5,0)));
            submitButton = new JButton("set Parameters");
            submitButton.addActionListener(this);
            submitButton.setName("setParams");
          //  submitButton.setMargin(new Insets(10,10,10,10)); 
            buttonsPanel.add(submitButton);
            
            scroller  = new JScrollPane(paramPanel); 
           
            getDataPanel.add(scroller); //(paramPanel);
            getDataPanel.add(buttonsPanel);
      
    }

    public void addToUI(String shortname, GetDataTable gdt ) {
   
           int i=0;
              
                ParamElement[] params = gdt.getParams();
            //    JPanel paramPanel = new JPanel();
                  paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
                  Border empty = BorderFactory.createEmptyBorder(); 
                  paramPanel.setBorder(empty);
                  paramPanel.setAlignmentY(CENTER_ALIGNMENT);
              
                
              while ( i < params.length ) {
                 
                  JPanel inputPanel = new JPanel();
                  inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
                  inputPanel.setBorder(empty);
                  String paramName = params[i].getName();
                  inputPanel.add(new JLabel(paramName+" : "));
                  inputPanel.setAlignmentX(LEFT_ALIGNMENT);
               //     inputPanel.setAlignmentY(CENTER_ALIGNMENT);
                  String description = params[i].getAttribute("Description");                  
                  String datatype = params[i].getAttribute("datatype");
                  String value = params[i].getValue();
                  ValuesElement values = (ValuesElement) params[i].getChildByName("VALUES");
                  String [] options = values.getOptions();
                 
                  if ( options.length > 0 ) {
                        optbox = new JComboBox(options);
                        optbox.addItem("");
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
                        JTextField minField = new JTextField(8);
                        JTextField maxField = new JTextField(8);
                        maxField.setMaximumSize( maxField.getPreferredSize() );
                        minField.setMaximumSize( minField.getPreferredSize() );
                        inputPanel.add(minField);
                        inputPanel.add(new JLabel(" / "));
                        inputPanel.add(maxField);                        
                        minField.setName(paramName+":Min");
                        maxField.setName(paramName+":Max");
                        if (paramName.contains("BAND")) {
                            String bandText=getDataParam.get(paramName);
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
                       
                    }
                    paramPanel.add(inputPanel);                  
                    i++;
                }
        
         
        }

    
    public HashMap<String, String>  getParams() {
        return getDataParam;
    }

    public void actionPerformed(ActionEvent e) {
       
        //
        // dynamically generated getdata parameters
        //
        
        Component source = (Component) e.getSource();   
        
        for (int i = 0; i < queryComponents.size(); i++) 
        {      
               Component c = queryComponents.get(i);
               if (c instanceof JComboBox) {
                   JComboBox cb = (JComboBox) c;
                   String name = cb.getName();
                   if (source.equals(clearButton)) {
                       cb.setSelectedItem("");
                   } else if (source.equals(submitButton)) {
                      // if ( cb.getSelectedItem().toString().length() > 0)
                           getDataParam.put(name, cb.getSelectedItem().toString());   
                   }
               }
               if (c instanceof JTextField) {
                   JTextField tf = (JTextField) c;
                   String name = tf.getName();
                   if (source.equals(clearButton)) {
                       tf.setText("");
                   } else if (source.equals(submitButton)) {
                   
                     
                   String keyname = name.substring(0, name.length()-4);
                   String limit = name.substring(name.length()-3); // Max oder Min
                   // has this parameter already been edited?
                   String oldvalue = getDataParam.get(keyname);
                   String newvalue = null;
                   String max="", min="";
                   
                       if (oldvalue != null && oldvalue.length() > 0) {
                           int dashindex = oldvalue.indexOf('/');
                           if (oldvalue.endsWith("/"))
                               min=oldvalue.substring(0, dashindex);
                           else if ( oldvalue.startsWith("/"))
                               max=oldvalue.substring(1);
                           else if (dashindex > 0){
                               min=oldvalue.substring(0,dashindex);
                               max=oldvalue.substring(dashindex+1);
                           }  
                           
                       } 
                       if (limit.equals("Max")) {
                           newvalue = min+"/"+tf.getText();
                       } else if (limit.equals("Min")) {
                           newvalue = tf.getText()+"/"+max;
                       } 
                       if (newvalue.trim().equals("/"))
                           newvalue="";
                       getDataParam.put(keyname, newvalue);    
                   }
                   
               }
        } // for
     
       
    }
    public void setService(String service) {
        
        paramPanel.removeAll();
        addToUI(service, services.get(service));
        paramPanel.updateUI();
        
    }
    

}
