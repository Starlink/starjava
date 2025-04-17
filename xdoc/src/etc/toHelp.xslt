<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY map.file "Map.xml">
  <!ENTITY toc.file "TOC.xml">
  <!ENTITY home.file "HelpTop.html">
  <!ENTITY searchconfig.file "search-config">
  <!ENTITY file.elements 
           "abstract|sect|subsect|subsubsect|subsubsubsect|subsubsubsubsect|subsubsubsubsubsect">
  <!ENTITY file.descendents
           ".//abstract|.//sect|.//subsect|.//subsubsect|.//subsubsubsect|.//subsubsubsubsect|.//subsubsubsubsubsect">

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


<!-- Top-level processing. -->

  <xsl:template match="sun">
    <multisection>
      <xsl:apply-templates mode="help-map" select="."/>
      <xsl:apply-templates mode="help-toc"/>
      <xsl:apply-templates mode="help-hs" select="."/>
      <xsl:apply-templates mode="help-text" select="."/>
      <xsl:apply-templates mode="help-search-config" select="."/>
    </multisection>
  </xsl:template>


<!-- Reference access functions. -->

  <xsl:template name="getRef">
    <xsl:param name="node" select="."/>
    <xsl:param name="sect"
               select="($node/ancestor-or-self::sect
                       |$node/ancestor-or-self::subsect
                       |$node/ancestor-or-self::subsubsect
                       |$node/ancestor-or-self::subsubsubsect
                       |$node/ancestor-or-self::subsubsubsubsect
                       |$node/ancestor-or-self::subsubsubsubsubsect
                       |$node/ancestor-or-self::docbody
                       |$node/ancestor-or-self::docinfo
                       |$node/ancestor-or-self::abstract
                       |$node/ancestor-or-self::sun)[last()]"/>
    <xsl:param name="sectId">
      <xsl:call-template name="getId">
        <xsl:with-param name="node" select="$sect"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:param name="nodeId">
      <xsl:call-template name="getId">
        <xsl:with-param name="node" select="$node"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:param name="file">
      <xsl:call-template name="getFile">
        <xsl:with-param name="node" select="$sect"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:value-of select="$file"/>
    <xsl:if test="$sectId != $nodeId">
      <xsl:text>#</xsl:text>
      <xsl:value-of select="$nodeId"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="getFile">
    <xsl:param name="node" select="."/>
    <xsl:call-template name="getId">
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
    <xsl:text>.html</xsl:text>
  </xsl:template>


<!-- Mode 'help-text' - the HTML help files themselves. -->

  <xsl:template mode="help-text" match="sun">
    <xsl:element name="filesection">
      <xsl:attribute name="file">
        <xsl:text>&home.file;</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="method">
        <xsl:text>html</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="doctype-public">
        <xsl:text>&html.pubid;</xsl:text>
      </xsl:attribute>
      <html>
        <head>
          <xsl:call-template name="cssStylesheet"/>
          <title> 
            <xsl:apply-templates select="docinfo/title"/>
          </title>
        </head>
        <body>
          <xsl:apply-templates select="docinfo"/>
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
          <xsl:call-template name="cssStylesheet"/>
          <title>
            <xsl:apply-templates mode="nameref" select="."/>
          </title>
        </head>
        <body>
          <xsl:if test="name(.)='abstract'">
            <h2>Abstract</h2>
          </xsl:if>
          <xsl:apply-templates select="subhead|p|px|figure|subdiv"/>
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

  <xsl:template mode="help-map" match="sun">
    <filesection file="&map.file;" 
                 method="xml"
                 indent="yes"
                 doctype-public="&map.pubid;"
                 doctype-system="&map.sysid;">
      <map version="1.0">
        <xsl:apply-templates mode="help-map" select="&file.descendents;"/>
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
        <xsl:apply-templates mode="help-toc" 
                             select="&file.elements;|appendices"/>
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
      <xsl:apply-templates mode="help-toc" select="&file.elements;|appendices"/>
    </xsl:element>
  </xsl:template>

  <xsl:template mode="help-toc" match="appendices">
    <xsl:apply-templates mode="help-toc" select="&file.elements;"/>
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
          <homeID>&home.file;</homeID>
          <mapref location="&map.file;"/>
        </maps>
        <view>
          <name>Search</name>
          <label>Search for words in text</label>
          <type>javax.help.SearchView</type>
          <data engine="com.sun.java.help.search.DefaultSearchEngine">
            <xsl:text>JavaHelpSearch</xsl:text>
          </data>
        </view>
        <view>
          <name>TOC</name>
          <label>Table of Contents</label>
          <type>javax.help.TOCView</type>
          <data>&toc.file;</data>
        </view>
      </helpset>
    </xsl:element>
  </xsl:template>


<!-- Mode 'help-search-config' - config file for JavaHelp indexer. -->

  <xsl:template mode="help-search-config" match="sun">
    <filesection file="&searchconfig.file;" method="text">
      <content>
        <xsl:apply-templates mode="help-search-config"
                             select="&file.descendents;"/>
      </content>
    </filesection>
  </xsl:template>

  <xsl:template mode="help-search-config" match="&file.elements;">
    <xsl:text>File </xsl:text>
    <xsl:call-template name="getFile"/>
    <xsl:text>&#x0a;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
