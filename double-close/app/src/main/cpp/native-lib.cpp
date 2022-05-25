#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <android/fdsan.h>

void testDoubleClose();

#define print_log(fmt, ...) __android_log_print(ANDROID_LOG_WARN, "double-click-test", fmt, ##__VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_test_test_1double_1close_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_test_test_1double_1close_MainActivity_testDoubleClose(
    JNIEnv* env,
    jobject /* this */) {
  //android_fdsan_set_error_level(ANDROID_FDSAN_ERROR_LEVEL_FATAL);
  testDoubleClose();
  std::string hello = "test double close";
  return env->NewStringUTF(hello.c_str());
}

//线程1
void* threadFunc1(void* p)
{
  int fd = open("/data/data/com.test.test_double_close/cache/test1.tmp", O_CREAT|O_RDWR, S_IRWXU);
  if (fd == -1)
  {
    print_log("thread 1 open file failed %d", errno);
    return NULL;
  }
  print_log("thread 1 open file %d", fd);

  close(fd);
  usleep(100*1000);
  close(fd);
  return NULL;
}

//线程2
void* threadFunc2(void* p)
{
  usleep(50*1000);
  int fd = open("/data/data/com.test.test_double_close/cache/test2.tmp", O_CREAT|O_RDWR, S_IRWXU);
  if (fd == -1)
  {
    print_log("thread 2 open file failed %d", errno);
    return NULL;
  }
  print_log("thread 2 open file %d", fd);

  usleep(100*1000);
  int ret = write(fd, "123", 3);
  if (ret == -1)
  {
    print_log("thread 2 write file failed %d", errno);
  }
  print_log("thread 2 write file len %d", ret);
  close(fd);
  return NULL;
}

void testDoubleClose()
{
  pthread_t hThread1;
  pthread_t hThread2;
  if (pthread_create(&hThread1, NULL, &threadFunc1, NULL) != 0)
  {
    print_log("create thread 1 failed\n");
    return;
  }
//  if (pthread_create(&hThread2, NULL, &threadFunc2, NULL) != 0)
//  {
//    print_log("create thread 2 failed\n");
//    return;
//  }
}