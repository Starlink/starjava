/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.util.Hashtable;

/**
 *
 * @author molinaro
 */
public class BaSTIPOSTMessage {

    /* Max returned results: defined here */
    static final String MAX_RETURNED_RESULTS = "30";
    /* query POST tokens with: visibility in results, column name, queried value */
    /* DEFAULTS */
    static final String[] DATATYPE_DEFAULT = {"1","FILE_TYPE",""};
    static final String[] AGE_DEFAULT = {"1","AGE",":"};
    static final String[] MASS_DEFAULT = {"1","MASS",":"};
    static final String[] Z_DEFAULT = {"1","Z",":"};
    static final String[] Y_DEFAULT = {"1","Y",":"};
    static final String[] FEH_DEFAULT = {"1","FE_H",":"};
    static final String[] MH_DEFAULT = {"1","M_H",":"};
    static final String[] TYPE_DEFAULT = {"1","TYPE",""};
    static final String[] MASSLOSS_DEFAULT = {"1","MASS_LOSS",""};
    static final String[] PHOTOMETRY_DEFAULT = {"1","PHOT_SYSTEM",""};
    static final String[] MIXTURE_DEFAULT = {"1","HED_TYPE",""};
    static final String[] SCENARIO_DEFAULT = {"1","SCENARIO_TYPE",""};

    /* parameters declarations */
    private String[] DataType, Age, Mass, Z, Y, FeH, MH, Type, MassLoss, Photometry, Mixture, Scenario;
    static String[] SQLresults;
    /* name conversion between interface values and DB values */
    static Hashtable DBnames = new Hashtable();
    
    /* constructor */
    public BaSTIPOSTMessage(){
        /* populates the naming conversion */
        DBnames.put("Isochrone", "ISO");
        DBnames.put("Track","TRACK");
        DBnames.put("HB Track","TRACK_HB");
        DBnames.put("ZAHB Table","TAB_ZAHB");
        DBnames.put("End He Table","TAB_EndHe");
        DBnames.put("Summary Table","TAB_tab");
        DBnames.put("Normal","NORMAL");
        DBnames.put("AGB Extended","AGB EXTENDED");
        DBnames.put("Stroemgren Castelli","STROEMGREN CASTELLI");
        DBnames.put("WFC2 (HST)","WFC2_HST");
        DBnames.put("Sloan","SLOAN");
        DBnames.put("Johnson Castelli","JOHNSON CASTELLI");
        DBnames.put("WFC3 UVIS (HST)","WFC3_UVIS_HST");
        DBnames.put("ACS","ACS");
        DBnames.put("Walraven","WALRAVEN");
        DBnames.put("Scaled Solar Model","SCALED SOLAR MODEL");
        DBnames.put("Alpha Enhanced","ALFA ENHANCED");
        DBnames.put("Overshooting","OVERSHOOTING");
        DBnames.put("Canonical","CANONICAL");
        // simply returns a default "empty" POST message
        DataType = DATATYPE_DEFAULT;
        Age = AGE_DEFAULT;
        Mass = MASS_DEFAULT;
        Z = Z_DEFAULT;
        Y = Y_DEFAULT;
        FeH = FEH_DEFAULT;
        MH = MH_DEFAULT;
        Type = TYPE_DEFAULT;
        MassLoss = MASSLOSS_DEFAULT;
        Photometry = PHOTOMETRY_DEFAULT;
        Mixture = MIXTURE_DEFAULT;
        Scenario = SCENARIO_DEFAULT;
        SQLresults = null;
    }

    /**
     * Resets the POST message to its default "empty" values
     */
    static void resetMessage( BaSTIPOSTMessage post) {
        // sets "visibility" of all querieable fields in the result to true
        // sets queried values to empty strings
        post.DataType = DATATYPE_DEFAULT;
        post.Age = AGE_DEFAULT;
        post.Mass = MASS_DEFAULT;
        post.Z = Z_DEFAULT;
        post.Y = Y_DEFAULT;
        post.FeH = FEH_DEFAULT;
        post.MH = MH_DEFAULT;
        post.Type = TYPE_DEFAULT;
        post.MassLoss = MASSLOSS_DEFAULT;
        post.Photometry = PHOTOMETRY_DEFAULT;
        post.Mixture = MIXTURE_DEFAULT;
        post.Scenario = SCENARIO_DEFAULT;
    }

    /**
     * Checks wheter or not the POST message (and then the query panel fields) is empty
     */
    static boolean isEmpty( BaSTIPOSTMessage post ) {
        if (
            post.DataType[2].equals("") &&
            post.Type[2].equals("") &&
            post.MassLoss[2].equals("") &&
            post.Photometry[2].equals("") &&
            post.Mixture[2].equals("") &&
            post.Scenario[2].equals("") &&
            post.Age[2].equals(":") &&
            post.Mass[2].equals(":") &&
            post.Z[2].equals(":") &&
            post.Y[2].equals(":") &&
            post.FeH[2].equals(":") &&
            post.MH[2].equals(":")
            ) {
                //System.out.println("EMPTY");
                return true;
            } else {
                //System.out.println("NOT EMPTY");
                return false;
            }
    }

    /**
     * Checks query correctness.
     * Since only Age/Mass, Z, Y, [Fe/H] and [M/H] are free inputs anly these ones are checked
     */
    static String Validate( BaSTIPOSTMessage post) {
        // initialize validation outcome
        String ValidationResult = "correct";
        // initialize cumulative string for eventually incorrect field values
        String ErrorsIn = "";
        double dummy = 0;
        String dummyStr = "";
        try {
            dummyStr = BaSTIPanel.AgeMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Age min ";
        }
        try {
            dummyStr = BaSTIPanel.AgeMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Age max ";
        }
        try {
            dummyStr = BaSTIPanel.MassMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Mass min ";
        }
        try {
            dummyStr = BaSTIPanel.MassMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Mass max ";
        }
        try {
            dummyStr = BaSTIPanel.ZMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Z min ";
        }
        try {
            dummyStr = BaSTIPanel.ZMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Z max ";
        }
        try {
            dummyStr = BaSTIPanel.YMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Y min ";
        }
        try {
            dummyStr = BaSTIPanel.YMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "Y max ";
        }
        try {
            dummyStr = BaSTIPanel.FeHMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "[Fe/H] min ";
        }
        try {
            dummyStr = BaSTIPanel.FeHMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "[Fe/H] max";
        }
        try {
            dummyStr = BaSTIPanel.MHMin.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "[M/H] min ";
        }
        try {
            dummyStr = BaSTIPanel.MHMax.getText();
            dummy = (dummyStr.equals(""))? 0 : Double.valueOf(dummyStr);
        } catch (NumberFormatException e) {
            ErrorsIn += "[M/H] max ";
        }
        if (ErrorsIn.equals("")) {
            return ValidationResult;
        } else {
            // limits length of error output
            if ( ErrorsIn.length() > 30 ) {
                ErrorsIn = ErrorsIn.substring(0, 30);
                ErrorsIn = ErrorsIn.substring(0, ErrorsIn.lastIndexOf(" "));
                ErrorsIn += "...";
            }
            ValidationResult = "Errors in: " + ErrorsIn + " field(s)";
            return ValidationResult;
        }
    }

    /**
     * Set input values from Query Panel into POST Message
     */
    static void Populate( BaSTIPOSTMessage post ) {
        /* Checked boxes for result displaying */
        post.Scenario[0] = (BaSTIPanel.ScenarioCheck.isSelected())? "1" : "0";
        post.Type[0] = (BaSTIPanel.TypeCheck.isSelected())? "1" : "0";
        post.MassLoss[0] = (BaSTIPanel.MassLossCheck.isSelected())? "1" : "0";
        post.Photometry[0] = (BaSTIPanel.PhotometryCheck.isSelected())? "1" : "0";
        post.Mixture[0] = (BaSTIPanel.MixtureCheck.isSelected())? "1" : "0";
        post.Age[0] = (BaSTIPanel.AgeCheck.isSelected())? "1" : "0";
        post.Mass[0] = (BaSTIPanel.MassCheck.isSelected())? "1" : "0";
        post.Z[0] = (BaSTIPanel.ZCheck.isSelected())? "1" : "0";
        post.Y[0] = (BaSTIPanel.YCheck.isSelected())? "1" : "0";
        post.FeH[0] = (BaSTIPanel.FeHCheck.isSelected())? "1" : "0";
        post.MH[0] = (BaSTIPanel.MHCheck.isSelected())? "1" : "0";
        /* Selected values for query */
        String DataTypeValue = BaSTIPanel.DataType.getSelectedItem().toString();
        post.DataType[2] = ( DataTypeValue.equals("") )? "" : (String) DBnames.get(DataTypeValue);
        String ScenarioValue = BaSTIPanel.Scenario.getSelectedItem().toString();
        post.Scenario[2] = ( ScenarioValue.equals("") )? "" : (String) DBnames.get(ScenarioValue);
        String TypeValue = BaSTIPanel.Type.getSelectedItem().toString();
        post.Type[2] = ( TypeValue.equals("") )? "" : (String) DBnames.get(TypeValue);
        String MassLossValue = BaSTIPanel.MassLoss.getSelectedItem().toString();
        post.MassLoss[2] = ( MassLossValue.equals("") )? "" : MassLossValue;
        String PhotometryValue = BaSTIPanel.Photometry.getSelectedItem().toString();
        post.Photometry[2] = ( PhotometryValue.equals("") )? "" : (String) DBnames.get(PhotometryValue);
        String MixtureValue = BaSTIPanel.Mixture.getSelectedItem().toString();
        post.Mixture[2] = ( MixtureValue.equals("") )? "" : (String) DBnames.get(MixtureValue);
        /* Inserted values for query */
        post.Age[2] = BaSTIPanel.AgeMin.getText().trim() + ":" + BaSTIPanel.AgeMax.getText().trim();
        post.Mass[2] = BaSTIPanel.MassMin.getText().trim() + ":" + BaSTIPanel.MassMax.getText().trim();
        post.Z[2] = BaSTIPanel.ZMin.getText().trim() + ":" + BaSTIPanel.ZMax.getText().trim();
        post.Y[2] = BaSTIPanel.YMin.getText().trim() + ":" + BaSTIPanel.YMax.getText().trim();
        post.FeH[2] = BaSTIPanel.FeHMin.getText().trim() + ":" + BaSTIPanel.FeHMax.getText().trim();
        post.MH[2] = BaSTIPanel.MHMin.getText().trim() + ":" + BaSTIPanel.MHMax.getText().trim();
        
    }

    String[] getDataType() {
        return DataType;
    }
    String[] getType() {
        return Type;
    }
    String[] getMassLoss() {
        return MassLoss;
    }
    String[] getPhotometry() {
        return Photometry;
    }
    String[] getMixture() {
        return Mixture;
    }
    String[] getScenario() {
        return Scenario;
    }
    String[] getAge() {
        return Age;
    }
    String[] getMass() {
        return Mass;
    }
    String[] getZ() {
        return Z;
    }
    String[] getY() {
        return Y;
    }
    String[] getFeH() {
        return FeH;
    }
    String[] getMH() {
        return MH;
    }

    void setDataType( String value ) {
        DataType[2] = value;
    }
    void setType( String value ) {
        Type[2] = value;
    }
    void setMassLoss( String value ) {
        MassLoss[2] = value;
    }
    void setPhotometry( String value ) {
        Photometry[2] = value;
    }
    void setMixture( String value ) {
        Mixture[2] = value;
    }
    void setScenario( String value ) {
        Scenario[2] = value;
    }
    void setAge( String value ) {
        Age[2] = value;
    }
    void setMass( String value ) {
        Mass[2] = value;
    }
    void setZ( String value ) {
        Z[2] = value;
    }
    void setY( String value ) {
        Y[2] = value;
    }
    void setFeH( String value ) {
        FeH[2] = value;
    }
    void setMH( String value ) {
        MH[2] = value;
    }

    /* for debug purposes */
    static void Print ( BaSTIPOSTMessage post) {
        System.out.println("DataType: " + post.DataType[2] + "(" + post.DataType[0] + ")");
        System.out.println("Scenario: " + post.Scenario[2] + "(" + post.Scenario[0] + ")");
        System.out.println("Type: " + post.Type[2] + "(" + post.Type[0] + ")");
        System.out.println("MassLoss: " + post.MassLoss[2] + "(" + post.MassLoss[0] + ")");
        System.out.println("Photometry: " + post.Photometry[2] + "(" + post.Photometry[0] + ")");
        System.out.println("Mixture: " + post.Mixture[2] + "(" + post.Mixture[0] + ")");
        System.out.println("Age: " + post.Age[2] + "(" + post.Age[0] + ")");
        System.out.println("Mass: " + post.Mass[2] + "(" + post.Mass[0] + ")");
        System.out.println("Z: " + post.Z[2] + "(" + post.Z[0] + ")");
        System.out.println("Y: " + post.Y[2] + "(" + post.Y[0] + ")");
        System.out.println("FeH: " + post.FeH[2] + "(" + post.FeH[0] + ")");
        System.out.println("MH: " + post.MH[2] + "(" + post.MH[0] + ")");
    }

}
