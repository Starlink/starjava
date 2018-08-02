package uk.ac.starlink.splat.iface;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class IntervalField extends JPanel {
	
    JTextField _lower;
    JTextField _upper;
    
    public IntervalField () {
        _lower=new JTextField(8);
        _upper=new JTextField(8);
      

        this.add(_lower);
        this.add(new JLabel("/"));
        this.add(_upper);
    }
    
  

    public void setInterval(String interval) {
        if (interval == null || interval.isEmpty()) {
            _upper.setText("");
            _lower.setText("");
        }
        String[] values = interval.split("/", 1);
        _lower.setText(values[0]);
        if (values.length>1)
            _upper.setText(values[1]);
        else
            _upper.setText("");
    }
    
    public void setInterval(String min, String max) {
       
            _upper.setText(max);
            _lower.setText(min);
    
    }

    public String getLower() {
        return _lower.getText();
    }
    
    public String getUpper() {
        return _upper.getText();
    }
    
 /*   private String getText() {
        String value="";
        String   txt=_lower.getText();
        if ( txt == null || txt.isEmpty())           
//            txt="-Inf";
            txt="";
        value=txt+"/";
        txt=_upper.getText();
        if ( txt == null || txt.isEmpty())           
//            txt="+Inf";
            txt="";
        value+=txt;  
        
        return value;   
    }*/
    
    public void clear() {
        _lower.setText("");
        _upper.setText("");
    }
    
}

