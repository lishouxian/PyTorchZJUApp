package org.pytorch.demo.vision;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;

import java.io.IOException;

public class VisionListActivity extends AbstractListActivity {
  int cameraRequestCode = 001;
  int cameraRequestCode2 = 002;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    findViewById(R.id.vision_card_qmobilenet_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(VisionListActivity.this, ImageClassificationActivity.class);
      intent.putExtra(ImageClassificationActivity.INTENT_MODULE_ASSET_NAME,
          "new.pt");
      intent.putExtra(ImageClassificationActivity.INTENT_INFO_VIEW_TYPE,
          InfoViewFactory.INFO_VIEW_TYPE_IMAGE_CLASSIFICATION_QMOBILENET);
      startActivity(intent);
    });

    findViewById(R.id.take_a_photo).setOnClickListener(v -> {
      Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      startActivityForResult(cameraIntent,cameraRequestCode);
    });

    findViewById(R.id.select_a_picture).setOnClickListener(v -> {
      Intent intent = new Intent();
      intent.setAction(Intent.ACTION_GET_CONTENT);
      intent.setType("image/*");
      startActivityForResult(intent, cameraRequestCode2);
    });

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
  protected int getListContentLayoutRes() {
    return R.layout.vision_list_content;
  }
}
