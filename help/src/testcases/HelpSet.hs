<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
  "http://java.sun.com/products/javahelp/helpset_2_0.dtd">

<helpset version="2.0">

     <title>HELP a Javahelp based help package</title>

     <!-- The Map file. This documents IDs to URLs -->
     <maps>
          <homeID>main-help</homeID>
          <mapref location="Map.jhm"/>
     </maps>

     <!-- Navigation views of various kinds (tabbed panes)-->
     <view xml:lang="en">
         <name>TOC</name>
         <label>Table of contents</label>
         <type>javax.help.TOCView</type>
         <data>TOC.xml</data>
     </view>

     <!--<view xml:lang="en">
         <name>Search</name>
         <label>Search</label>
         <type>javax.help.SearchView</type>
         <data engine="com.sun.java.help.search.DefaultSearchEngine">
               JavaHelpSearch
         </data>
     </view>-->

</helpset>
