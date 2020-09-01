cmake_minimum_required(VERSION 3.4.1)

set(pathPROJECT C:/Users/kbs54/AndroidStudioProjects/last) #본인 프로젝트의 절대경로로 수정할 것
set(pathOPENCV ${pathPROJECT}/sdk)
set(pathLIBOPENCV_JAVA ${pathOPENCV}/native/libs/${ANDROID_ABI}/libopencv_java4.so)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathOPENCV}/native/jni/include)

add_library( #라이브러리 이름 지정
        imageprocessing

        #라이브러리를 shared로 설정
        SHARED

        #소스 파일의 상대 경로
        ${pathPROJECT}/app/src/main/cpp/imageprocessing.cpp )

add_library( lib_opencv SHARED IMPORTED )

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA})

find_library( #path함수의 이름 설정
        log-lib

        #CMake가 찾을 NDK 라이브러리의 이름을 지정
        log )

target_link_libraries( # 타겟 라이브라리 지정
        imageprocessing

        lib_opencv
        #NDK에 포함 된 로그 라이브러리에 대상 라이브러리를 연결
        ${log-lib} )