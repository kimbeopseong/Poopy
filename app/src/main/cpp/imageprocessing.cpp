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
                                                                                    jlong output_image) {
    Mat & img_input = *(Mat *) input_image;
    Mat & img_output = *(Mat *) output_image;
    Mat grabcut, result, grayScale, alpha;
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
    cvtColor(img_input , img_input , COLOR_BGRA2BGR);
    grabCut (img_input, grabcut, rectangle, bgModel, fgModel,5,GC_INIT_WITH_RECT);
    compare(grabcut, GC_PR_FGD, grabcut, CMP_EQ);
    Mat foreground(img_input.size(), CV_8UC3, Scalar(0, 0, 0));
    img_input.copyTo(foreground, grabcut);
    cvtColor(foreground, grayScale, COLOR_BGR2GRAY);
    threshold(grayScale, alpha,0,255,THRESH_BINARY);
    Mat rgb[3];
    split(foreground, rgb);
    Mat rgba[4];
    rgba[0] = rgb[0];
    rgba[1] = rgb[1];
    rgba[2] = rgb[2];
    rgba[3] = alpha;
    merge(rgba, 4, result);
    img_output = result;
}
