LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    bootanimation_main.cpp \
    AudioPlayer.cpp \
    BootAnimation.cpp

LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES -DEGL_EGLEXT_PROTOTYPES

LOCAL_CFLAGS += -Wall -Werror -Wunused -Wunreachable-code

LOCAL_C_INCLUDES += external/tinyalsa/include

ifeq ($(MTK_LCM_PHYSICAL_ROTATION), 90)
    LOCAL_CFLAGS += -DMTK_AOSP_ROTATION
endif
ifeq ($(MTK_LCM_PHYSICAL_ROTATION), 180)
    LOCAL_CFLAGS += -DMTK_AOSP_ROTATION
endif
ifeq ($(MTK_LCM_PHYSICAL_ROTATION), 270)
    LOCAL_CFLAGS += -DMTK_AOSP_ROTATION
endif

ifeq ($(strip $(SUPPORT_WISKY_XDS)), yes)
	LOCAL_CFLAGS += -DSUPPORT_WISKY_XDS
endif

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    liblog \
    libandroidfw \
    libutils \
    libbinder \
    libui \
    libskia \
    libEGL \
    libGLESv1_CM \
    libgui \
    libtinyalsa

LOCAL_MODULE:= bootanimation

LOCAL_INIT_RC := bootanim.rc

ifdef TARGET_32_BIT_SURFACEFLINGER
LOCAL_32_BIT_ONLY := true
endif

include $(BUILD_EXECUTABLE)
