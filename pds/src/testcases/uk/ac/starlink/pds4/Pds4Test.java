package uk.ac.starlink.pds4;

import junit.framework.TestCase;
import gov.nasa.pds.label.object.FieldType;

public class Pds4Test extends TestCase {

    public void testReaders() {
        String[] blankTxts = new String[ 0 ];
        for ( FieldType ftype : FieldType.values() ) {
            FieldReader rdr = FieldReader.getInstance( ftype, blankTxts );
            assertNotNull( rdr );
        }
    }
}
