package org.pytorch.demo.vision;

import android.graphics.Bitmap;
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

        Module module = null;
        Bitmap imageBitmap = (Bitmap) Objects.requireNonNull(getIntent().getBundleExtra("imagedata")).get("data");

        final String moduleFileAbsoluteFilePath = new File(
                Objects.requireNonNull(Utils.assetFilePath(this, "new.pt"))).getAbsolutePath();
        module = Module.load(moduleFileAbsoluteFilePath);

        //测试图像大小
        TextView textView = findViewById(R.id.size);
        assert imageBitmap != null;
        final Tensor testTensor = TensorImageUtils.bitmapToFloat32Tensor(imageBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        //裁切图像
        if (testTensor.shape()[2] - testTensor.shape()[3]>0) {
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0,(int) (testTensor.shape()[2] - testTensor.shape()[3])/2,(int) testTensor.shape()[3], (int)testTensor.shape()[3]);
        }
        else{
            imageBitmap = Bitmap.createBitmap(imageBitmap, (int) (testTensor.shape()[3] - testTensor.shape()[2])/2,(int) testTensor.shape()[2],0, (int)testTensor.shape()[2]);
        }

        assert imageBitmap != null;
        imageBitmap = Bitmap.createScaledBitmap(imageBitmap,256,256,false);

        ImageView imageView = findViewById(R.id.image);
        imageView.setImageBitmap(imageBitmap);

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(imageBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        System.out.println(inputTensor.shape().length);


        // running the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        System.out.println(inputTensor.shape().length);

        final float[] scores = outputTensor.getDataAsFloatArray();


        Bitmap test = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[256 * 256];
        for (int i = 0; i < 256 * 256; ++i) {
            //关键代码，生产灰度图
            pixels[i] = (int) (scores[i] * 50);

        }
        test.setPixels(pixels, 0, 256, 0, 0, 256, 256);

        StringBuilder testword = new StringBuilder(testTensor.shape()[2] + Long.toString(testTensor.shape()[3]));
        for (int i = 0; i < 10; ++i) {
            //关键代码，生产灰度图
            testword.insert(0, scores[i]);
        }

        textView.setText(testword.toString());
        // showing image on UI
        ImageView imageView1 = findViewById(R.id.image2);
        imageView1.setImageBitmap(test);


    }

}
