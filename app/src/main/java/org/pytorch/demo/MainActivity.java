package org.pytorch.demo;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;

import org.pytorch.demo.nlp.NLPListActivity;
import org.pytorch.demo.vision.ImageClassificationActivity;
import org.pytorch.demo.vision.VisionListActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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

    findViewById(R.id.button).setOnClickListener(v -> {
//      Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//      startActivityForResult(cameraIntent,cameraRequestCode);
    });

    findViewById(R.id.button2).setOnClickListener(v -> {
//      Intent intent = new Intent();
//      intent.setAction(Intent.ACTION_GET_CONTENT);
//      intent.setType("image/*");
//      startActivityForResult(intent, cameraRequestCode2);
    });
  }
  }