<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY map.file "Map.xml">
  <!ENTITY toc.file "TOC.xml">
  <!ENTITY file.elements "abstract|sect|subsect|subsubsect|subsubsubsect">

  <!ENTITY map.pubid 
           "-//Sun Microsystems Inc.//DTD JavaHelp Map Version 1.0//EN">
  <!ENTITY map.sysid "http://java.sun.com/products/javahelp/map_1_0.dtd">
  <!ENTITY toc.pubid
           "-//Sun Microsystems Inc.//DTD JavaHelp TOC Version 1.0//EN">
  <!ENTITY toc.sysid "http://java.sun.com/products/javahelp/toc_1_0.dtd">
  <!ENTITY hs.pubid
           "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN">
  <!ENTITY hs.sysid "http://java.sun.com/products/javahelp/helpset_1_0.dtd">
  <!ENTITY html.pubid "-//W3C//DTD HTML 3.2//EN">
]>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


  <xsl:import href="toHTML1.xslt"/>

  <xsl:output method="xml"/>

  <xsl:template match="sun">
    <multisection>
      <xsl:apply-templates mode="help-map"/>
      <xsl:apply-templates mode="help-toc"/>
      <xsl:apply-templates mode="help-hs" select="."/>
      <xsl:apply-templates mode="help-text" select="."/>
    </multisection>
  </xsl:template>


<!-- Mode 'help-text' - the HTML help files themselves. -->

  <xsl:template mode="help-text" match="sun">
    <xsl:element name="filesection">
      <xsl:attribute name="file">
        <xsl:call-template name="getFile"/>
      </xsl:attribute>
      <xsl:attribute name="method">
        <xsl:text>html</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="doctype-public">
        <xsl:text>&html.pubid;</xsl:text>
      </xsl:attribute>
      <html>
        <head>
          <title> 
            <xsl:apply-templates select="docinfo/title"/>
          </title>
        </head>
        <body>
          <xsl:apply-templates select="docinfo"/>
          <h2>
            <a href="http://www.starlink.ac.uk/">Starlink Project</a>
          </h2>
          <h2>Contents</h2>
          <xsl:apply-templates mode="toc" select="docbody"/>
        </body>
      </html>
    </xsl:element>
    <xsl:apply-templates mode="help-text" select="docbody"/>
  </xsl:template>

  <xsl:template mode="help-text" match="&file.elements;">
    <xsl:element name="filesection">
      <xsl:attribute name="file">
        <xsl:call-template name="getFile"/>
      </xsl:attribute>
      <xsl:attribute name="method">
        <xsl:text>html</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="doctype-public">
        <xsl:text>&html.pubid;</xsl:text>
      </xsl:attribute>
      <html>
        <head>
          <title>
            <xsl:apply-templates mode="nameref" select="."/>
          </title>
        </head>
        <body>
          <xsl:if test="name(.)='abstract'">
            <h2>Abstract</h2>
          </xsl:if>
          <xsl:apply-templates select="subhead|p|px"/>
          <xsl:if test="&file.elements;">
            <ul>
              <xsl:apply-templates mode="toc" select="&file.elements;"/>
            </ul>
          </xsl:if>
        </body>
      </html>
    </xsl:element>
    <xsl:apply-templates mode="help-text" select="&file.elements;"/>
  </xsl:template>


<!-- Mode 'help-map' - the JavaHelp Map file. -->

  <xsl:template mode="help-map" match="*"/>

  <xsl:template mode="help-map" match="docbody">
    <filesection file="&map.file;" 
                 method="xml"
                 indent="yes"
                 doctype-public="&map.pubid;"
                 doctype-system="&map.sysid;">
      <map version="1.0">
        <xsl:apply-templates mode="help-map"/>
      </map>
    </filesection>
  </xsl:template>

  <xsl:template mode="help-map" match="&file.elements;">
    <xsl:element name="mapID">
      <xsl:attribute name="target">
        <xsl:call-template name="getId"/>
      </xsl:attribute>
      <xsl:attribute name="url">
        <xsl:call-template name="getFile"/>
      </xsl:attribute>
    </xsl:element>
    <xsl:text>&#x0a;</xsl:text>
    <xsl:apply-templates mode="help-map" select="&file.elements;"/>
  </xsl:template>

  <xsl:template name="getRef">
    <xsl:param name="node" select="."/>
    <xsl:call-template name="getFile">
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="getFile">
    <xsl:param name="node" select="."/>
    <xsl:call-template name="getId">
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
    <xsl:text>.html</xsl:text>
  </xsl:template>


<!-- Mode 'help-toc' - the JavaHelp table of contents file. -->

  <xsl:template mode="help-toc" match="*"/>

  <xsl:template mode="help-toc" match="docbody">
    <filesection file="&toc.file;"
                 method="xml"
                 indent="yes"
                 doctype-public="&toc.pubid;"
                 doctype-system="&toc.sysid;">
      <toc version="1.0">
        <xsl:apply-templates mode="help-toc" select="&file.elements;"/>
      </toc>
    </filesection>
  </xsl:template>

  <xsl:template mode="help-toc" match="&file.elements;">
    <xsl:element name="tocitem">
      <xsl:attribute name="target">
        <xsl:call-template name="getId"/>
      </xsl:attribute>
      <xsl:attribute name="text">
        <xsl:apply-templates mode="nameref" select="."/>
      </xsl:attribute>
      <xsl:apply-templates mode="help-toc" select="&file.elements;"/>
    </xsl:element>
  </xsl:template>

<!-- Mode 'help-hs' - the top-level Helpset file. -->

  <xsl:template mode="help-hs" match="sun">
    <xsl:element name="filesection">
      <xsl:attribute name="file">
        <xsl:call-template name="getId"/>
        <xsl:text>.hs</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="method">
        <xsl:text>xml</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="indent">
        <xsl:text>yes</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="doctype-public">
        <xsl:text>&hs.pubid;</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="doctype-system">
        <xsl:text>&hs.sysid;</xsl:text>
      </xsl:attribute>
      <helpset version="1.0">
        <title>
          <xsl:apply-templates select="docinfo/title"/>
        </title>
        <maps>
          <homeID>
            <xsl:call-template name="getFile"/>
          </homeID>
          <mapref location="&map.file;"/>
        </maps>
        <view>
          <name>TOC</name>
          <label>Table of Contents</label>
          <type>javax.help.TOCView</type>
          <data>&toc.file;</data>
        </view>
      </helpset>
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
