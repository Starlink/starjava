/*
*+
*  Name:
*     AstObject.c

*  Purpose:
*     JNI implementations of native methods of AstObject class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "grf.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_AstObject.h"

#define BUFLENG 256

/* Class methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_nativeInitialize(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   jniastInitialize( env );
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_AstObject_getAstConstantI(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of AST constant to retrieve */
) {
   const char *name;
   jint result;
   int success = 0;
   char namcopy[ BUFLENG ]; 

   name = jniastGetUTF( env, jName );
   if ( name != NULL ) {

#define TRY_CONST( Xname ) \
      if ( strcmp( #Xname, name ) == 0 ) { \
         result = (jint) Xname; \
         success = 1; \
      }
      /* Version identifiers. */
      TRY_CONST( AST_MAJOR_VERS )
      else TRY_CONST( AST_MINOR_VERS )
      else TRY_CONST( AST_RELEASE )
      else TRY_CONST( JNIAST_MAJOR_VERS )
      else TRY_CONST( JNIAST_MINOR_VERS )
      else TRY_CONST( JNIAST_RELEASE )

      /* Interpolation schemes. */
      else TRY_CONST( AST__NEAREST )
      else TRY_CONST( AST__LINEAR )
      else TRY_CONST( AST__SINC )
      else TRY_CONST( AST__SINCSINC )
      else TRY_CONST( AST__SINCCOS )
      else TRY_CONST( AST__SINCGAUSS )
      else TRY_CONST( AST__BLOCKAVE )
      else TRY_CONST( AST__UKERN1 )
      else TRY_CONST( AST__UINTERP )

      /* Symbolic frame numbers. */
      else TRY_CONST( AST__BASE )
      else TRY_CONST( AST__CURRENT )
      else TRY_CONST( AST__NOFRAME )

      /* IntraMap flags. */
      else TRY_CONST( AST__NOFWD )
      else TRY_CONST( AST__NOINV )
      else TRY_CONST( AST__SIMPFI )
      else TRY_CONST( AST__SIMPIF )

      /* WcsMap projection types. */
      else TRY_CONST( AST__AZP )
      else TRY_CONST( AST__TAN )
      else TRY_CONST( AST__SIN )
      else TRY_CONST( AST__STG )
      else TRY_CONST( AST__ARC )
      else TRY_CONST( AST__ZPN )
      else TRY_CONST( AST__ZEA )
      else TRY_CONST( AST__AIR )
      else TRY_CONST( AST__CYP )
      else TRY_CONST( AST__CAR )
      else TRY_CONST( AST__MER )
      else TRY_CONST( AST__CEA )
      else TRY_CONST( AST__COP )
      else TRY_CONST( AST__COD )
      else TRY_CONST( AST__COE )
      else TRY_CONST( AST__COO )
      else TRY_CONST( AST__BON )
      else TRY_CONST( AST__PCO )
      else TRY_CONST( AST__GLS )
      else TRY_CONST( AST__SFL )
      else TRY_CONST( AST__PAR )
      else TRY_CONST( AST__AIT )
      else TRY_CONST( AST__MOL )
      else TRY_CONST( AST__CSC )
      else TRY_CONST( AST__QSC )
      else TRY_CONST( AST__NCP )
      else TRY_CONST( AST__TSC )
      else TRY_CONST( AST__TPN )
      else TRY_CONST( AST__SZP )
      else TRY_CONST( AST__WCSBAD )

      /* GRF attribute types. */
      else TRY_CONST( GRF__STYLE )
      else TRY_CONST( GRF__WIDTH )
      else TRY_CONST( GRF__SIZE )
      else TRY_CONST( GRF__FONT )
      else TRY_CONST( GRF__COLOUR )

      /* GRF primitives. */
      else TRY_CONST( GRF__LINE )
      else TRY_CONST( GRF__MARK )
      else TRY_CONST( GRF__TEXT )
#undef TRY_CONST
   }

   if ( ! success ) {
      strncpy( namcopy, name, BUFLENG );
   }
   jniastReleaseUTF( env, jName, name );
   if ( ! success ) {
      jniastThrowError( env, 
                        "There is no AST int constant called \"%s\"", namcopy );

   }
   return result;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_AstObject_getAstConstantD(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of AST constant to retrieve */
) {
   const char *name;
   jdouble result;
   int success = 0;
   char namcopy[ BUFLENG ];

   name = jniastGetUTF( env, jName );
   if ( name != NULL ) {

#define TRY_CONST( Xname ) \
      if ( strcmp( #Xname, name ) == 0 ) { \
         result = (jdouble) Xname; \
         success = 1; \
      }
      TRY_CONST( AST__BAD )
#undef TRY_CONST
   }

   if ( ! success ) {
      strncpy( namcopy, name, BUFLENG - 1 );
   }
   jniastReleaseUTF( env, jName, name );
   if ( ! success ) {
      jniastThrowError( env, 
                        "There is no AST double constant called \"%s\"", 
                        namcopy );
   }
   return result;
}


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_annul(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   ASTCALL(
      astAnnul( pointer.AstObject );
   )
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_delete(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   ASTCALL(
      astDelete( pointer.AstObject );
   )
}

JNIEXPORT void JNICALL  Java_uk_ac_starlink_ast_AstObject_clear(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Attribute names to be cleared */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );

   ASTCALL(
      astClear( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_AstObject_clone(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer newpointer;
   jobject newobj;

   newobj = (*env)->AllocObject( env, (*env)->GetObjectClass( env, this ) );
   ASTCALL(
      newpointer.AstObject = astClone( pointer.AstObject );
   )
   jniastSetPointerField( env, newobj, newpointer );
   return newobj;
}


JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_AstObject_copy(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer newpointer;
   jobject newobj;

   newobj = (*env)->AllocObject( env, (*env)->GetObjectClass( env, this ) );
   ASTCALL(
      newpointer.AstObject = astCopy( pointer.AstObject );
   )
   jniastSetPointerField( env, newobj, newpointer );
   return newobj;
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_AstObject_getC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   jstring jValue = NULL;
   const char *value;

   ASTCALL(
      value = astGetC( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );

   if ( ! (*env)->ExceptionCheck( env ) ) {
      jValue = (*env)->NewStringUTF( env, (const char *) value );
   }
   return jValue;
}

#define MAKE_ASTGETX(Xletter,Xjtype) \
JNIEXPORT Xjtype JNICALL Java_uk_ac_starlink_ast_AstObject_get##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib       /* Name of the attribute */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
   Xjtype value; \
 \
   ASTCALL( \
      value = (Xjtype) astGet##Xletter( pointer.AstObject, attrib ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
   return value; \
}
MAKE_ASTGETX(D,jdouble)
MAKE_ASTGETX(F,jfloat)
MAKE_ASTGETX(I,jint)
MAKE_ASTGETX(L,jlong)
#undef MAKE_ASTGETX

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_setC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib,      /* Name of the character attribute */
   jstring jValue        /* The value to which it will be set */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   const char *value = jniastGetUTF( env, jValue );

   ASTCALL(
      astSetC( pointer.AstObject, attrib, value );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   jniastReleaseUTF( env, jValue, value );
}

#define MAKE_ASTSETX(Xletter,Xjtype,Xtype) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_set##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib,      /* Name of the numeric attribute */ \
   Xjtype value          /* The value to which it will be set */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
 \
   ASTCALL( \
      astSet##Xletter( pointer.AstObject, attrib, (Xtype) value ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
}
MAKE_ASTSETX(D,jdouble,double)
MAKE_ASTSETX(F,jfloat,float)
MAKE_ASTSETX(I,jint,int)
MAKE_ASTSETX(L,jlong,int)

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_set(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jSettings     /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *settings = jniastGetUTF( env, jSettings );

   ASTCALL(
      astSet( pointer.AstObject, settings );
   )
   jniastReleaseUTF( env, jSettings, settings );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_show(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   ASTCALL(
      astShow( pointer.AstObject );
   )
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_AstObject_test(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the attribute to be tested */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   int result;

   ASTCALL(
      result = astTest( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   return result ? JNI_TRUE : JNI_FALSE;
}


/* $Id$ */
