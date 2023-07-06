package uk.ac.starlink.vo;

import junit.framework.TestCase;

public class IvoidTest extends TestCase {

    public void testIvoid() {
        Ivoid ivoid1 = new Ivoid( "ivo://foo.bar/Bum#baz" );
        assertTrue( ivoid1.isValid() );
        assertEquals( "ivo://foo.bar/Bum", ivoid1.getRegistryPart() );
        checkEqual( ivoid1, new Ivoid( "ivo://foo.bar/bum#baz" ) );
        checkUnequal( ivoid1, new Ivoid( "ivo://foo.bar/Bum#Baz" ) );
        assertEquals( "#baz", ivoid1.getLocalPart() );
        assertEquals( "ivo://foo.bar/Bum", ivoid1.getRegistryPart() );
        assertTrue( ivoid1.matchesRegistryPart( "IVO://FOO.BAR/BUM" ) );
        assertFalse( ivoid1.matchesRegistryPart( "ivo://bar.foo/bum" ) );
        assertEquals( ivoid1.toRegtapString(), "ivo://foo.bar/bum#baz" );
        assertFalse( ivoid1.equals( null ) );

        Ivoid ivoid2 = new Ivoid( "http://whizz.bang/Biff#Oof" );
        assertFalse( ivoid2.isValid() );
        assertNull( ivoid2.getLocalPart() );
        assertNull( ivoid2.getRegistryPart() );
        checkEqual( ivoid2, new Ivoid( "http://whizz.bang/Biff#Oof" ) );
        checkUnequal( ivoid2, new Ivoid( "http://whizz.bang/biff#Oof" ) );
        assertEquals( ivoid2.toRegtapString(), "http://whizz.bang/Biff#Oof" );
        assertFalse( ivoid2.equals( null ) );

        Ivoid ivoid3 = new Ivoid( null );
        assertFalse( ivoid3.isValid() );
        assertNull( ivoid3.getLocalPart() );
        assertNull( ivoid3.getRegistryPart() );
        checkEqual( ivoid3, new Ivoid( null ) );
        checkUnequal( ivoid3, new Ivoid( "foo:bar" ) );
        checkUnequal( ivoid3, ivoid2 );
        checkUnequal( ivoid3, ivoid1 );
        assertFalse( ivoid3.equals( null ) );

        // But note hope that we don't have any IVOIDs like this,
        // i.e. with non-lower-case local parts,
        // since the hope is that simply lowercasing everything will
        // work for comparisons.
        Ivoid ivoid4 = new Ivoid( "ivo://X.y/Z#ABC" );
        assertEquals( "ivo://x.y/z#ABC", ivoid4.toRegtapString() );
    }

    private void checkEqual( Ivoid ivoid1, Ivoid ivoid2 ) {
        assertTrue( ivoid1.equals( ivoid1 ) );
        assertTrue( ivoid2.equals( ivoid2 ) );
        assertTrue( ivoid1.equals( ivoid2 ) );
        assertTrue( ivoid2.equals( ivoid1 ) );
        assertEquals( ivoid1.hashCode(), ivoid2.hashCode() );
    }

    private void checkUnequal( Ivoid ivoid1, Ivoid ivoid2 ) {
        assertFalse( ivoid1.equals( ivoid2 ) );
        assertFalse( ivoid2.equals( ivoid1 ) );
    }
}
