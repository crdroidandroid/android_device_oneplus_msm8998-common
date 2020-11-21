ifeq (true,false)
LOCAL_PATH := $(call my-dir)
include $(call all-subdir-makefiles)
endif
