<?xml version="1.0"?>

<!-- This stylesheet extracts from an XML document (a) all those
     elements which are in the NDX namespace
     http://www.starlink.ac.uk/NDX along with (b) any elements which
     have an ndx:name attribute.  The resulting document consists only
     of elements which are in the NDX namespace, but with _no_
     namespace declaration.  This canonical form is easy to manipulate.
-->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:x="http://www.starlink.ac.uk/HDX"
  exclude-result-prefixes="x"
  >

  <!-- Note: including the declaration
       xmlns="http://www.starlink.ac.uk/HDX" declares this namespace
       as the default namespace in the output document.  As noted
       above, this is not what we want.

       Note: xt (see http://www.jclark.com/xml/xt.html) appears to
       process the exclude-result-prefixes attribute in a slightly
       different way from javax.xml.transform.Transformer.  In
       particular, xt doesn't propagate the default namespace
       declaration to the root element of the result tree, but instead
       repeats it on each child element.  These are probably
       equivalent for the purposes to which I want to put the result
       tree, so it's not worth worrying about at present.
  -->

  <xsl:template match="/*">
    <hdx>
      <xsl:apply-templates/>
    </hdx>
  </xsl:template>

  <xsl:template name="process-element">
    <xsl:choose>
      <xsl:when test="@x:uri">
	<!-- Attempt to resolve the URI, in case it is an entity name. -->
	<!-- This appears not to work with javax.xml.transform? -->
	<xsl:variable name="u" select="unparsed-entity-uri(@x:uri)"/>
	<xsl:choose>
	  <xsl:when test="$u">
	    <!-- it's an entity -->
	    <xsl:value-of select="$u"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <!-- it's not an entity -->
	    <xsl:value-of select="@x:uri"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>
      <xsl:otherwise>
	<!-- Nothing special: process the children -->
	<xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="x:*">
    <xsl:element name="{local-name()}">
      <xsl:call-template name="process-element"/>
    </xsl:element>
  </xsl:template>

  <!-- XXX Perhaps add code to insert a ndobj element above this if there
       isn't one there already -->
  <xsl:template match="*[@x:name]">
    <xsl:element name="{@x:name}">
      <xsl:call-template name="process-element"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*">
    <xsl:apply-templates select="*"/>
  </xsl:template>

</xsl:stylesheet>

<!-- Local Variables: -->
<!-- mode: xml -->
<!-- sgml-indent-data: t -->
<!-- sgml-indent-step: 2 -->
<!-- sgml-default-dtd-file: "xslt.psgmldtd" -->
<!-- End: -->
