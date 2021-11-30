package uk.ac.starlink.pds4;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

public class LabelTest extends TestCase {

    public void testBinaryLabel() throws IOException {
        URL binaryUrl =
            LabelTest.class.getResource( "Product_Table_Binary.xml");
        Label label = new LabelParser().parseLabel( binaryUrl );
        assertEquals( 1, label.getTables().length );
        BaseTable binaryTable = (BaseTable) label.getTables()[ 0 ];
        assertEquals( TableType.BINARY, binaryTable.getTableType() );
        assertEquals( 336, binaryTable.getRecordCount() );
        RecordItem[] binaryContents = binaryTable.getContents();
        assertEquals( 20, binaryContents.length );
        Field[] binaryFields = new Field[ 20 ];
        for ( int i = 0; i < 20; i++ ) {
            binaryFields[ i ] = (Field) binaryContents[ i ];
        }
        Field tempField = binaryFields[ 9 ];
        assertEquals( "TEMPERATURE_SENSOR", tempField.getName() );
        assertEquals( "degree celcius", tempField.getUnit() );  // sic
        assertEquals( 57, tempField.getFieldLocation() );
    }

    public void testMultiLabel() throws IOException {
        URL multiUrl =
            LabelTest.class.getResource( "Product_Table_Multiple_Tables.xml" );
        Label label = new LabelParser().parseLabel( multiUrl );
        Table[] tables = label.getTables();
        assertEquals( 2, tables.length );
        BaseTable t1 = (BaseTable) tables[ 0 ];
        BaseTable t2 = (BaseTable) tables[ 1 ];

        assertEquals( TableType.CHARACTER, t1.getTableType() );
        assertEquals( 88, t1.getRecordLength() );
        assertEquals( "TABLE_CHAR_1", t1.getLocalIdentifier() );
        assertEquals( TableType.CHARACTER, t2.getTableType() );
        assertEquals( 88, t2.getRecordLength() );
        assertEquals( "TABLE_CHAR_2", t2.getLocalIdentifier() );
    }

    public void testDelimited() throws IOException {
        URL delimUrl =
            LabelTest.class.getResource( "Product_Table_Delimited.xml" );
        Label label = new LabelParser().parseLabel( delimUrl );
        assertEquals( 1, label.getTables().length );
        DelimitedTable table = (DelimitedTable) label.getTables()[ 0 ];
        assertEquals( 3, table.getRecordCount() );
        assertEquals( ',', table.getFieldDelimiter() );
    }

    public void testMagic() throws IOException {
        assertTrue( isMagic( "Product_Table_Binary.xml" ) );
        assertFalse( isMagic( "LabelTest.class" ) );
    }

    private boolean isMagic( String resourceName ) throws IOException {
        DataSource datsrc =
            new URLDataSource( LabelTest.class.getResource( resourceName ) );
        return Pds4TableBuilder.isMagic( datsrc.getIntro() );
    }
}
