package org.pytorch.demo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.pytorch.demo.nlp.NLPListActivity;
import org.pytorch.demo.vision.ImageClassificationActivity;
import org.pytorch.demo.vision.PhotoUtils;
import org.pytorch.demo.vision.VisionListActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class MainActivity extends AppCompatActivity {
  private ImageView photo;


  /**
   * 图库请求码
   */
  private static final int REQUEST_CODE_GALLERY = 0xa0;
  /**
   * 相机请求码
   */
  private static final int REQUEST_CODE_CAMERA = 0xa1;
  /**
   * 裁剪请求码
   */
  private static final int REQUEST_CODE_CROP = 0xa2;


  /**
   * 拍照所得原图
   */
  private File photographedFile = new File(Environment.getExternalStorageDirectory().getPath() + "/IMG_" + System.currentTimeMillis() + ".jpg");
  /**
   * 修剪后的图片
   */
  private File cropFile = new File(Environment.getExternalStorageDirectory().getPath() + "/IMG_" + System.currentTimeMillis() + "_crop.jpg");


  /**
   * 拍照所得原图Uri
   */
  private Uri imageUri;
  /**
   * 拍照后回调，即修剪后的Uri
   */
  private Uri cropImageUri;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.button3).setOnClickListener(v -> {
      final Intent intent = new Intent(MainActivity.this, ImageClassificationActivity.class);
      intent.putExtra(ImageClassificationActivity.INTENT_MODULE_ASSET_NAME,
          "new.pt");
      intent.putExtra(ImageClassificationActivity.INTENT_INFO_VIEW_TYPE,
          InfoViewFactory.INFO_VIEW_TYPE_IMAGE_CLASSIFICATION_QMOBILENET);
      startActivity(intent);
    });



    Button btnTakePhoto = (Button) findViewById(R.id.button);
    Button btnTakeGallery = (Button) findViewById(R.id.button2);
    photo = (ImageView) findViewById(R.id.image);


    btnTakePhoto.setOnClickListener(view -> {
      imageUri = Uri.fromFile(photographedFile);

      //API 24(7.0)以上
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //通过FileProvider创建一个content类型的Uri
        imageUri = FileProvider.getUriForFile(
                MainActivity.this,
                MainActivity.this.getPackageName() + ".fileprovider",
                photographedFile
        );
      }

      PhotoUtils.takePicture(MainActivity.this, imageUri, REQUEST_CODE_CAMERA);
    });
    btnTakeGallery.setOnClickListener(view -> {
      PhotoUtils.openPic(MainActivity.this, REQUEST_CODE_GALLERY);
    });



  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //裁剪图片宽高
    int outputX = 480, outputY = 480;

    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        //拍照完成回调
        case REQUEST_CODE_CAMERA:
          cropImageUri = Uri.fromFile(cropFile);
          PhotoUtils.cropImageUri(this, imageUri, cropImageUri, 1, 1, outputX, outputY, REQUEST_CODE_CROP);
          break;
        //访问相册完成回调
        case REQUEST_CODE_GALLERY:
          if (hasSdcard()) {
            cropImageUri = Uri.fromFile(cropFile);

            Uri newUri = Uri.parse(PhotoUtils.getPath(this, data.getData()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              newUri = FileProvider.getUriForFile(
                      this,
                      BuildConfig.APPLICATION_ID + ".fileprovider",
                      new File(newUri.getPath())
              );
            }

            PhotoUtils.cropImageUri(this, newUri, cropImageUri, 1, 1, outputX, outputY, REQUEST_CODE_CROP);
          } else {
            Toast.makeText(MainActivity.this, "设备没有SD卡", Toast.LENGTH_SHORT).show();
          }
          break;
        //裁剪回调
        case REQUEST_CODE_CROP:
          Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, this);
          if (bitmap != null) {
            showImages(bitmap);
          }

                    /*
                    拍照图片、拍照后裁剪所得图片，都已被自动保存在SDCard上，但是打开系统相册时却无法显示。
                    此处发送广播让MediaScanner扫描制定的文件，在系统相册中就可以找到拍摄的照片了，

                    注意：
                        打开图库，能看到的只是拍照后裁剪所得照片，拍照原图还是看不到。
                        且，保存的是最后一次裁剪过的图片，之前裁剪过的图片会被替换。
                        若要保留每次裁剪后的图片，请在拍照、图库回调后分别调用以下代码。
                     */
          Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
          intent.setData(cropImageUri);
          sendBroadcast(intent);
          break;
        default:
          break;
      }
    }
  }

  /**
   * 将图片显示ImageView组件
   */
  private void showImages(Bitmap bitmap) {
    photo.setImageBitmap(bitmap);
  }

  /**
   * 检查设备是否存在SDCard的工具方法
   */
  public static boolean hasSdcard() {
    String state = Environment.getExternalStorageState();
    return state.equals(Environment.MEDIA_MOUNTED);
  }

}