package org.pytorch.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class Utils {
  public static String assetFilePath(Context context, String assetName) {

    String oldpath = context.getFilesDir() +"/" +assetName;
    String newpath = Environment.getExternalStorageDirectory().getPath() + "/Crack detection/model/new.pt";
    System.out.println(oldpath);
    System.out.println(newpath);
    //File oldfile = new File(context.getFilesDir(), assetName);
    File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Crack detection/model/new.pt");

    File creatfilepath = new File(Environment.getExternalStorageDirectory().getPath() + "/Crack detection/model");

    if(!file.exists()){
      creatfilepath.mkdirs();
      Utils.copyFile(oldpath,newpath);
    }

    if (file.exists() && file.length() > 0) {
      return file.getAbsolutePath();
    }

    try (InputStream is = context.getAssets().open(assetName)) {
      try (OutputStream os = new FileOutputStream(file)) {
        byte[] buffer = new byte[4 * 1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
          os.write(buffer, 0, read);
        }
        os.flush();
      }
      return file.getAbsolutePath();
    } catch (IOException e) {
      Log.e(Constants.TAG, "Error process asset " + assetName + " to file path");
    }
    return null;
  }

  public static int[] topK(float[] a, final int topk) {
    float values[] = new float[topk];
    Arrays.fill(values, -Float.MAX_VALUE);
    int ixs[] = new int[topk];
    Arrays.fill(ixs, -1);

    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < topk; j++) {
        if (a[i] > values[j]) {
          for (int k = topk - 1; k >= j + 1; k--) {
            values[k] = values[k - 1];
            ixs[k] = ixs[k - 1];
          }
          values[j] = a[i];
          ixs[j] = i;
          break;
        }
      }
    }
    return ixs;
  }

  public static boolean saveImageToGallery(Context context, Bitmap bmp) {

    // 首先保存图片

    File appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

    String fileName = System.currentTimeMillis() + ".jpg";
    //File file = new File(appDir, fileName);

    File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Crack detection/IMG/IMG_" + System.currentTimeMillis() + ".jpg");
    File creatfilepath = new File(Environment.getExternalStorageDirectory().getPath() + "/Crack detection/img");

    if(!creatfilepath.exists()){
      creatfilepath.mkdirs();
    }


    try {
      FileOutputStream fos = new FileOutputStream(file);
      //通过io流的方式来压缩保存图片
      boolean isSuccess = bmp.compress(Bitmap.CompressFormat.JPEG, 60, fos);
      fos.flush();
      fos.close();

      //把文件插入到系统图库
      //MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);

      //保存图片后发送广播通知更新数据库
      Uri uri = Uri.fromFile(file);
      context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
      if (isSuccess) {
        return true;
      } else {
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }



  public static Bitmap doTytorch(Context context, Bitmap bitmap) {
    bitmap = Bitmap.createScaledBitmap(bitmap,256,256,false);

    Module module = null;

    final String moduleFileAbsoluteFilePath = new File(
            Objects.requireNonNull(Utils.assetFilePath(context, "new.pt"))).getAbsolutePath();
    module = Module.load(moduleFileAbsoluteFilePath);


    final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

    final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
    final float[] scores = outputTensor.getDataAsFloatArray();

    Bitmap test = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);

    int[] pixels = new int[256 * 256];
    for (int i = 0; i < 256 * 256; ++i) {
      //关键代码，生产灰度图
      pixels[i] = (int) (scores[i] * 50);
    }
    test.setPixels(pixels, 0, 256, 0, 0, 256, 256);
    return test;
  }

  //文件复制功能
  public static void copyFile(String oldPath, String newPath) {
    try {
      int bytesum = 0;
      int byteread = 0;
      File oldfile = new File(oldPath);
      if (oldfile.exists()) { //文件存在时
        InputStream inStream = new FileInputStream(oldPath); //读入原文件
        FileOutputStream fs = new FileOutputStream(newPath);
        byte[] buffer = new byte[1444];
        int length;
        while ( (byteread = inStream.read(buffer)) != -1) {
          bytesum += byteread; //字节数 文件大小
          System.out.println(bytesum);
          fs.write(buffer, 0, byteread);
        }
        inStream.close();
      }
    }
    catch (Exception e) {
      System.out.println("复制单个文件操作出错");
      e.printStackTrace();

    }

  }


}
