//
// Created by Nezero on 11/01/2018.
//

#include "hashblock.h"
#include <inttypes.h>

#include <jni.h>


jbyteArray JNICALL hash13_native(JNIEnv *env, jclass cls, jbyteArray header)
{
    jint Plen = (env)->GetArrayLength(header);
    jbyte *P = (env)->GetByteArrayElements(header, NULL);
    jbyteArray DK = NULL;

    if (P) {
        uint256 result = Hash9(P, P + Plen);

        DK = (env)->NewByteArray(32);
        if (DK) {
            (env)->SetByteArrayRegion(DK, 0, 32, (jbyte *) result.begin());
        }

        (env)->ReleaseByteArrayElements(header, P, JNI_ABORT);
    }
    return DK;
}

static const JNINativeMethod methods[] = {
        {"x13_native", "([B)[B", (void *) hash13_native}
};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv * env;

    if ((vm)->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jclass cls = (env)->FindClass("org/bitcoinj/crypto/X13");
    int r = (env)->RegisterNatives(cls, methods, 1);

    return (r == JNI_OK) ? JNI_VERSION_1_6 : -1;
}

