//
// Created by BJH on 2020-07-13.
//

#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_poopy_ui_camera_CameraPreview_imageprocessing(JNIEnv *env, jobject thiz,
                                                                                    jlong input_image,
                                                                                    jlong ouput_image) {
    Mat & img_input = *(Mat *) input_image;
    Mat & img_output = *(Mat *) ouput_image;
    Mat result;
    Mat bgModel, fgModel;
    int height, width;
    int x, y, w, h;
    height = img_input.rows;
    width = img_input.cols;
    x = int(width*0.25);
    y = int(height*0.25);
    w = int(width*0.5);
    h = int(width*0.5);
    Rect rectangle(x, y, x+w, y+h);
    cvtColor(img_input , img_input , COLOR_RGBA2RGB);
    grabCut (img_input, result, rectangle, bgModel, fgModel,5,GC_INIT_WITH_RECT);
    compare(result, GC_PR_FGD, result, CMP_EQ);
    Mat foreground(img_input.size(), CV_8UC3, Scalar(0, 0, 0));
    img_input.copyTo(foreground, result);
    img_output = foreground;
}
