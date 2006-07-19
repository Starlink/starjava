package uk.ac.starlink.plastic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class MessageTest extends TestCase {

    public MessageTest( String name ) {
        super( name );
    }

    public void testUriEquals() throws URISyntaxException {
        assertEquals( new URI( "ivo://x/y/z" ), new URI( "ivo://x/y/z" ) );
    }

    public void testMessages() {
        MessageDefinition[] msgs = MessageDefinition.getKnownMessages();
        Set idSet = new HashSet();
        for ( int i = 0; i < msgs.length; i++ ) {
            MessageDefinition msg = msgs[ i ];
            int narg = msg.getArgTypes().length;
            assertTrue( narg >= msg.getRequiredArgs() );
            URI id = msg.getId();
            assertTrue( ! idSet.contains( id ) );
            idSet.add( id );
            assertTrue( idSet.contains( id ) );
            assertNotNull( msg.getReturnType() );
        }
        assertEquals(
            new HashSet( Arrays.asList( MessageId.getKnownMessages() ) ),
            new HashSet( idSet ) );
    }
}
