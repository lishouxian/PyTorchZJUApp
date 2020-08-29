package org.pytorch.demo.vision;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.util.Objects;

public class Result extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Uri cropImageUri = null;
        Module module = null;

        cropImageUri = (Uri) getIntent().getExtras().get("data");
//        Bitmap imageBitmap = (Bitmap) getIntent().getBundleExtra("imagedata").get("data");

        Bitmap imageBitmap = PhotoUtils.getBitmapFromUri(cropImageUri, this);

        ImageView photo = findViewById(R.id.image);
        photo.setImageBitmap(imageBitmap);

        Bitmap test = Utils.doTytorch(this, imageBitmap);

        Utils.saveImageToGallery(this,test);

        ImageView imageView1 = findViewById(R.id.image1);
        imageView1.setImageBitmap(test);


    }

}
