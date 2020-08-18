package org.pytorch.demo.vision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.Constants;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.vision.view.ResultRowView;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

public class ImageClassificationActivity extends AbstractCameraXActivity<ImageClassificationActivity.AnalysisResult> {

  public static final String INTENT_MODULE_ASSET_NAME = "INTENT_MODULE_ASSET_NAME";
  public static final String INTENT_INFO_VIEW_TYPE = "INTENT_INFO_VIEW_TYPE";

  private static final int INPUT_TENSOR_WIDTH = 224;
  private static final int INPUT_TENSOR_HEIGHT = 224;
  private static final int TOP_K = 3;
  private static final int MOVING_AVG_PERIOD = 10;
  private static final String FORMAT_MS = "%dms";
  private static final String FORMAT_AVG_MS = "avg:%.0fms";

  private static final String FORMAT_FPS = "%.1fFPS";
  public static final String SCORES_FORMAT = "%.2f";

  static class AnalysisResult {

    private final String showtext;
    private final Bitmap showbitMap;
    private final Bitmap reallbitMap;

    private final String[] topNClassNames;
    private final float[] topNScores;
    private final long analysisDuration;
    private final long moduleForwardDuration;

    public AnalysisResult(String showtext,Bitmap showbitMap,Bitmap reallbitMap, String[] topNClassNames, float[] topNScores,
                          long moduleForwardDuration, long analysisDuration) {
      this.showtext = showtext;
      this.showbitMap = showbitMap;
      this.reallbitMap = reallbitMap;


      this.topNClassNames = topNClassNames;
      this.topNScores = topNScores;
      this.moduleForwardDuration = moduleForwardDuration;
      this.analysisDuration = analysisDuration;
    }
  }

  private boolean mAnalyzeImageErrorState;
  private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];
  private TextView mFpsText;
  private TextView mMsText;
  private TextView mMsAvgText;
  private Module mModule;
  private String mModuleAssetName;
  private FloatBuffer mInputTensorBuffer;
  private Tensor mInputTensor;
  private long mMovingAvgSum = 0;

  private TextView showText;
  private ImageView imageView;

  //提供接口处

  private Queue<Long> mMovingAvgQueue = new LinkedList<>();

  @Override
  protected int getContentViewLayoutId() {
    return R.layout.activity_image_classification;
  }

  @Override
  protected TextureView getCameraPreviewTextureView() {
    return ((ViewStub) findViewById(R.id.image_classification_texture_view_stub))
        .inflate()
        .findViewById(R.id.image_classification_texture_view);
  }

  int cameraRequestCode = 001;
  int cameraRequestCode2 = 002;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showText = findViewById(R.id.text);
    imageView = findViewById(R.id.imageView);



  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == cameraRequestCode && resultCode == RESULT_OK) {
      Intent resultView = new Intent(this, Result.class);
      resultView.putExtra("imagedata",data.getExtras());
      startActivity(resultView);
    }

  }

  @Override
  protected void applyToUiAnalyzeImageResult(AnalysisResult result) {

//    showText.setText(result.showtext);
    showText.setText("");
    imageView.setImageBitmap(result.showbitMap);
    findViewById(R.id.button).setOnClickListener(v -> {
//      Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//      startActivityForResult(cameraIntent,cameraRequestCode);
      Utils.saveImageToGallery(this,result.showbitMap);

      Utils.saveImageToGallery(this,result.reallbitMap);
      showText.setText("保存成功！！！！");

    });

  }
  //获取模型
  protected String getModuleAssetName() {
    if (!TextUtils.isEmpty(mModuleAssetName)) {
      return mModuleAssetName;
    }
    final String moduleAssetNameFromIntent = getIntent().getStringExtra(INTENT_MODULE_ASSET_NAME);
    mModuleAssetName = !TextUtils.isEmpty(moduleAssetNameFromIntent)
        ? moduleAssetNameFromIntent
        : "new.pt";

    return mModuleAssetName;
  }

  @Override
  protected String getInfoViewAdditionalText() {
    return getModuleAssetName();
  }

  @Override
  @WorkerThread
  @Nullable
  protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
    if (mAnalyzeImageErrorState) {
      return null;
    }

    try {
      if (mModule == null) {
        final String moduleFileAbsoluteFilePath = new File(
            Utils.assetFilePath(this, getModuleAssetName())).getAbsolutePath();
        mModule = Module.load(moduleFileAbsoluteFilePath);

        mInputTensorBuffer =
            Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});
        System.out.println(mInputTensor.shape().length);
        for (int i = 0; i < 4; i++) {
          System.out.println(mInputTensor.shape()[i]);
        }
      }


      final long startTime = SystemClock.elapsedRealtime();
      TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
          image.getImage(), rotationDegrees,
          INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT,
          TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
          TensorImageUtils.TORCHVISION_NORM_STD_RGB,
          mInputTensorBuffer, 0);




      final long moduleForwardStartTime = SystemClock.elapsedRealtime();
      final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
      final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

      final float[] scores = outputTensor.getDataAsFloatArray();
      final float[] scores2 = mInputTensor.getDataAsFloatArray();
//      final int[] ixs = Utils.topK(scores, TOP_K);

      //TODO 完成显示校准

      final String[] topKClassNames = new String[TOP_K];
      final float[] topKScores = new float[TOP_K];

      System.out.println(
              scores.length
      );
      final String shownum = Float.toString(scores[0]);
      Bitmap test = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
      Bitmap test2 = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
      int[] pixels = new int[224 * 224];
      int[] pixels2 = new int[256 * 256];
      for (int i = 0; i < 224 * 224; ++i) {
        //关键代码，生产灰度图
        pixels[i] = (int) (scores[i] * 50);
        pixels2[i] = (int) (scores2[i] * 50);

      }

      test.setPixels(pixels, 0, 224, 0, 0, 224, 224);
      test2.setPixels(pixels2, 0, 224, 0, 0, 224, 224);
//      for (int i = 0; i < TOP_K; i++) {
//        final int ix = ixs[i];
//        topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
//        topKScores[i] = scores[ix];
//      }
      final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
      return new AnalysisResult(shownum,test,test2,topKClassNames, topKScores, moduleForwardDuration, analysisDuration);
    } catch (Exception e) {
      Log.e(Constants.TAG, "Error during image analysis", e);
      mAnalyzeImageErrorState = true;
      runOnUiThread(() -> {
        if (!isFinishing()) {
          showErrorDialog(v -> ImageClassificationActivity.this.finish());
        }
      });
      return null;
    }
  }

  @Override
  protected int getInfoViewCode() {
    return getIntent().getIntExtra(INTENT_INFO_VIEW_TYPE, -1);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mModule != null) {
      mModule.destroy();
    }
  }
}
