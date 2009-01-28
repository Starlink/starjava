/*
*+
*  Name:
*     KeyMap.c

*  Purpose:
*     JNI implementations of native methods of KeyMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     14-DEC-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_KeyMap.h"

#define BLANK ""

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.KeyMap = astKeyMap( "" );
   )
   jniastInitObject( env, this, pointer );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapRemove(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astMapRemove( pointer.KeyMap, key );
      )
      jniastReleaseUTF( env, jKey, key );
   }
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_KeyMap_mapSize(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int size = 0;
   THASTCALL( jniastList( 1, pointer.AstObject ),
      size = astMapSize( pointer.KeyMap );
   )
   return (jint) size;
}
 
JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_KeyMap_mapLength(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   int leng = 0;
   const char *key;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         leng = astMapLength( pointer.KeyMap, key );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   return (jint) leng;
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_KeyMap_mapHasKey(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   int has = 0;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         has = astMapHasKey( pointer.KeyMap, key );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   return has ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_KeyMap_mapKey(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint index            /* Key index */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key = NULL;
   jstring jKey = NULL;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      key = astMapKey( pointer.KeyMap, (int) index );
   )
   if ( key != NULL && ! (*env)->ExceptionCheck( env ) ) {
      jKey = (*env)->NewStringUTF( env, key );
   }
   return jKey;
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_KeyMap_mapType(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   int type = AST__BADTYPE;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         type = astMapType( pointer.KeyMap, key );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   return (jint) type;
}

#define MAKE_MAPPUT0(Xletter,Xtype,Xjtype) \
 \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut0##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jKey,         /* Map key */ \
   Xjtype value,         /* Map value */ \
   jstring jComment      /* Comment string */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *key; \
   const char *comment = NULL; \
 \
   if ( jniastCheckNotNull( env, jKey ) ) { \
      key = jniastGetUTF( env, jKey ); \
      if ( jComment != NULL ) { \
         comment = jniastGetUTF( env, jComment ); \
      } \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         astMapPut0##Xletter( pointer.KeyMap, key, (Xtype) value, comment ); \
      ) \
      jniastReleaseUTF( env, jKey, key ); \
      if ( jComment != NULL ) { \
         jniastReleaseUTF( env, jComment, comment ); \
      } \
   } \
}
MAKE_MAPPUT0(D,double,jdouble)
MAKE_MAPPUT0(I,int,jint)
#undef MAKE_MAPPUT0

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut0C(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey,         /* Map key */
   jstring jValue,       /* String value */
   jstring jComment      /* Comment string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   const char *value;
   const char *comment = NULL;

   if ( jniastCheckNotNull( env, jKey ) && 
        jniastCheckNotNull( env, jValue ) ) {
      key = jniastGetUTF( env, jKey );
      value = jniastGetUTF( env, jValue );
      if ( jComment != NULL ) {
         comment = jniastGetUTF( env, jComment );
      }
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astMapPut0C( pointer.KeyMap, key, value, comment );
      )
      jniastReleaseUTF( env, jKey, key );
      jniastReleaseUTF( env, jValue, value );
      if ( jComment != NULL ) {
         jniastReleaseUTF( env, jComment, comment );
      }
   }
}
  
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut0A(
   JNIEnv *env,          /* Interface pointer */ 
   jobject this,         /* Instance object */ 
   jstring jKey,         /* Map key */ 
   jobject jValue,       /* String value */
   jstring jComment      /* Comment string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   const char *comment = NULL;
   AstPointer value;

   if ( jniastCheckNotNull( env, jKey ) &&
        jniastCheckNotNull( env, jValue ) ) {
      key = jniastGetUTF( env, jKey );
      value = jniastGetPointerField( env, jValue );
      if ( jComment != NULL ) {
          comment = jniastGetUTF( env, jComment );
      }
      THASTCALL( jniastList( 2, pointer.AstObject, value.AstObject ),
          astMapPut0A( pointer.KeyMap, key, value.AstObject, comment );
      )
      jniastReleaseUTF( env, jKey, key );
      if ( jComment != NULL ) {
         jniastReleaseUTF( env, jComment, comment );
      }
   }
}


JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet0D(
   JNIEnv *env,          /* Interface pointer */ 
   jobject this,         /* Instance object */ 
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   double value;
   int success = 0;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astMapGet0D( pointer.KeyMap, key, &value );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   if ( success && ! (*env)->ExceptionCheck( env ) ) {
       return (*env)->NewObject( env, DoubleClass, DoubleConstructorID,
                                 (jdouble) value );
   }
   else {
       return NULL;
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet0I(
   JNIEnv *env,          /* Interface pointer */ 
   jobject this,         /* Instance object */ 
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   int value;
   int success = 0;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astMapGet0I( pointer.KeyMap, key, &value );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   if ( success && ! (*env)->ExceptionCheck( env ) ) {
       return (*env)->NewObject( env, IntegerClass, IntegerConstructorID,
                                 (jint) value );
   }
   else {
       return NULL;
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet0C(
   JNIEnv *env,          /* Interface pointer */ 
   jobject this,         /* Instance object */ 
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   jstring jValue;
   const char *value;
   int success = 0;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astMapGet0C( pointer.KeyMap, key, &value );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   if ( success && ! (*env)->ExceptionCheck( env ) ) {
      return (*env)->NewStringUTF( env, value );
   }
   else {
      return NULL;
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet0A(
   JNIEnv *env,          /* Interface pointer */ 
   jobject this,         /* Instance object */ 
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   AstObject *value;
   int success = 0;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astMapGet0A( pointer.KeyMap, key, &value );
      )
      jniastReleaseUTF( env, jKey, key );
   }
   if ( success && ! (*env)->ExceptionCheck( env ) ) {
      return jniastMakeObject( env, value );
   }
   else {
      return NULL;
   }
}


#define MAKE_MAPPUT1(Xletter,Xtype,Xjtype,XType) \
 \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut1##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jKey,         /* Map key */ \
   Xjtype##Array jValue, /* Value array */ \
   jstring jComment      /* Comment string */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *key; \
   const char *comment = NULL; \
   Xtype *valEls = NULL; \
   int size; \
 \
   ENSURE_SAME_TYPE(Xtype,Xjtype) \
 \
   if ( jniastCheckNotNull( env, jKey ) && \
        jniastCheckNotNull( env, jValue ) ) { \
      key = jniastGetUTF( env, jKey ); \
      if ( jComment != NULL ) { \
         comment = jniastGetUTF( env, jComment ); \
      } \
      size = (int) (*env)->GetArrayLength( env, jValue ); \
      valEls = (Xtype *) (*env)->Get##XType##ArrayElements( env, jValue, \
                                                            NULL ); \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         astMapPut1##Xletter( pointer.KeyMap, key, size, valEls, comment ); \
      ) \
      ALWAYS( \
         if ( valEls != NULL ) { \
            (*env)->Release##XType##ArrayElements( env, jValue, valEls, \
                                                   JNI_ABORT ); \
         } \
      ) \
      jniastReleaseUTF( env, jKey, key ); \
      if ( jComment != NULL ) { \
          jniastReleaseUTF( env, jComment, comment ); \
      } \
   } \
}
MAKE_MAPPUT1(D,double,jdouble,Double)
MAKE_MAPPUT1(I,int,jint,Int)
#undef MAKE_MAPPUT1

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut1C(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey,         /* Map key */
   jobjectArray jValue,  /* Value array (Strings) */
   jstring jComment      /* Comment string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   const char *comment = NULL;
   jstring jValEl;
   const char **valEls;
   int size;
   int i;

   if ( jniastCheckNotNull( env, jKey ) &&
        jniastCheckNotNull( env, jValue ) ) {
      key = jniastGetUTF( env, jKey );
      if ( jComment != NULL ) {
         comment = jniastGetUTF( env, jComment );
      }
      size = (int) (*env)->GetArrayLength( env, jValue );
      valEls = jniastMalloc( env, size * sizeof( char * ) );
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < size; i++ ) {
            jValEl = (*env)->GetObjectArrayElement( env, jValue, i );
            if ( jValEl != NULL ) {
               valEls[ i ] = jniastGetUTF( env, jValEl );
            }
            else {
               valEls[ i ] = BLANK;
            }
         }
         THASTCALL( jniastList( 1, pointer.AstObject ),
            astMapPut1C( pointer.KeyMap, key, size, valEls, comment );
         )
         for ( i = 0; i < size; i++ ) {
            jValEl = (*env)->GetObjectArrayElement( env, jValue, i );
            if ( jValEl != NULL ) {
               jniastReleaseUTF( env, jValEl, valEls[ i ] );
            }
         }
         free( valEls );
         jniastReleaseUTF( env, jKey, key );
         if ( jComment != NULL ) {
            jniastReleaseUTF( env, jComment, comment );
         }
      }
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_KeyMap_mapPut1A(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey,         /* Map key */
   jobjectArray jValue,  /* Value array (AstObjects) */
   jstring jComment      /* Comment string */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   const char *comment = NULL;
   jobject jValEl;
   AstObject **valEls = NULL;
   AstObject **ptrs = NULL;
   AstPointer ptr;
   int size;
   int i;

   if ( jniastCheckNotNull( env, jKey ) &&
        jniastCheckNotNull( env, jValue ) ) {
      key = jniastGetUTF( env, jKey );
      if ( jComment != NULL ) {
         comment = jniastGetUTF( env, jComment );
      }
      size = (int) (*env)->GetArrayLength( env, jValue );
      valEls = jniastMalloc( env, size * sizeof( AstObject * ) );
      if ( ! (*env)->ExceptionCheck( env ) ) {
         for ( i = 0; i < size; i++ ) {
            jValEl = (*env)->GetObjectArrayElement( env, jValue, i );
            if ( jValEl != NULL ) {
               ptr = jniastGetPointerField( env, jValEl );
               valEls[ i ] = ptr.AstObject;
            }
            else {
               valEls[ i ] = NULL;
            }
         }
         ptrs = jniastMalloc( env, ( size + 2 ) * sizeof( AstObject * ) );
         for ( i = 0; i < size; i++ ) {
            ptrs[ i ] = valEls[ i ];
         }
         ptrs[ size ] = pointer.AstObject;
         ptrs[ size + 1 ] = NULL;
         THASTCALL( ptrs,     /* ptrs will get freed by macro */
            astMapPut1A( pointer.KeyMap, key, size, valEls, comment );
         )
         free( valEls );
         jniastReleaseUTF( env, jKey, key );
         if ( jComment != NULL ) {
            jniastReleaseUTF( env, jComment, comment );
         }
      }
   }
}
  
#define MAKE_MAPGET1(Xletter,Xtype,Xjtype,XType) \
 \
JNIEXPORT Xjtype##Array JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet1##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jKey          /* Map key */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *key; \
   int size; \
   int nval; \
   Xjtype##Array jResult = NULL; \
   Xtype *result; \
 \
   ENSURE_SAME_TYPE(Xtype,Xjtype) \
 \
   if ( jniastCheckNotNull( env, jKey ) ) { \
      key = jniastGetUTF( env, jKey ); \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         size = astMapLength( pointer.KeyMap, key ); \
      ) \
      if ( ! (*env)->ExceptionCheck( env ) && \
           size && \
           ( jResult = (*env)->New##XType##Array( env, (jsize) size ) ) && \
           ( result = (*env)->Get##XType##ArrayElements( env, jResult, \
                                                         NULL ) ) ) { \
         THASTCALL( jniastList( 1, pointer.AstObject ), \
            astMapGet1##Xletter( pointer.KeyMap, key, size, &nval, result ); \
         ) \
         ALWAYS( \
            (*env)->Release##XType##ArrayElements( env, jResult, result, 0 ); \
         ) \
      } \
      jniastReleaseUTF( env, jKey, key ); \
   } \
   return jResult; \
}
MAKE_MAPGET1(D,double,jdouble,Double)
MAKE_MAPGET1(I,int,jint,Int)
#undef MAKE_MAPGET1

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet1C(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey,         /* Map key */
   jint sleng            /* Maximum string length */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   int size;
   int nval;
   jobjectArray jResult = NULL;
   char *buffer;
   int i;
   jstring str;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         size = astMapLength( pointer.KeyMap, key );
      )
      if ( ! (*env)->ExceptionCheck( env ) &&
           size &&
           ( jResult = (*env)->NewObjectArray( env, size, 
                                               StringClass, NULL ) ) &&
           ( buffer = jniastMalloc( env, ( sleng + 1 ) * size ) ) ) {
         THASTCALL( jniastList( 1, pointer.AstObject ),
            astMapGet1C( pointer.KeyMap, key, sleng + 1, size, &nval, buffer );
         )
         for ( i = 0; i < size; i++ ) {
            if ( ! (*env)->ExceptionCheck( env ) ) {
               str = (*env)->NewStringUTF( env, buffer + i * ( sleng + 1 ) );   
               (*env)->SetObjectArrayElement( env, jResult, i, str );
            }
         }
         ALWAYS(
            free( buffer );
         )
      }
      jniastReleaseUTF( env, jKey, key );
   }
   return jResult;
}

JNIEXPORT jobjectArray JNICALL Java_uk_ac_starlink_ast_KeyMap_mapGet1A(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jKey          /* Map key */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *key;
   int size;
   int nval;
   jobjectArray jResult = NULL;
   AstObject **result;
   jobject obj;
   int i;

   if ( jniastCheckNotNull( env, jKey ) ) {
      key = jniastGetUTF( env, jKey );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         size = astMapLength( pointer.KeyMap, key );
      )
      if ( ! (*env)->ExceptionCheck( env ) &&
           size &&
           ( jResult = (*env)->NewObjectArray( env, size, AstObjectClass, 
                                               NULL ) ) &&
           ( result = jniastMalloc( env, size * sizeof( AstObject * ) ) ) ) {
         THASTCALL( jniastList( 1, pointer.AstObject ),
            astMapGet1A( pointer.KeyMap, key, size, &nval, result );
         )
         for ( i = 0; i < size; i++ ) {
            if ( ! (*env)->ExceptionCheck( env ) ) {
               obj = jniastMakeObject( env, result[ i ] );
               (*env)->SetObjectArrayElement( env, jResult, i, obj );
            }
         }
         ALWAYS(
            free( result );
         )
      }
      jniastReleaseUTF( env, jKey, key );
   }
   return jResult;
}
