#include <jni.h>
#include <ftw.h>
#include <string>
#include <errno.h>
#include <android/log.h>

#define LOG_TAG "NATIVE"

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static int64_t totalDirSize = 0;

static int32_t processFile(const char* path, const struct stat *statBuf, int32_t typeFlag, struct FTW *ftwBuf){
    (void)path;
    (void)ftwBuf;
    if(typeFlag != FTW_NS){
        totalDirSize += statBuf->st_size;
    }
    return 0;
}

extern "C" JNIEXPORT jlong
JNICALL
Java_com_progressifff_filemanager_models_StorageFile_calculateDirSize(
        JNIEnv *env,
        jobject,
        jstring dirPath) {

    totalDirSize = 0;

    constexpr uint32_t openFDCount = 20;

    auto path = env->GetStringUTFChars(dirPath, nullptr);

    auto flags = 0;
    flags |= FTW_MOUNT;
    flags |= FTW_PHYS;

    if(nftw(path, processFile, openFDCount, flags) == (-1)){
        LOGE("Failed to calculate directory size: %s", strerror(errno));
        totalDirSize = -1;
    }

    env->ReleaseStringUTFChars(dirPath, path);
    return static_cast<jlong>(totalDirSize);
}