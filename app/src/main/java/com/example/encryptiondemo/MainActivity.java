package com.example.encryptiondemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final String ALGORITHM = "AES";
    private static final String HASHING = "SHA-1";
    private static final String PADDING_MODE = "/CBC/PKCS5Padding";
    private static byte[] password = "aman".getBytes(StandardCharsets.UTF_8);
    private static byte[] initVector = "1234567812345678".getBytes();
    private static final String TAG = MainActivity.class.getSimpleName();
    LinearLayout imageLinearLayout;
    EditText message;
    Button encryptButton,decryptButton, addImage;
    TextView tvImageDetails;
    ArrayList<Uri> imageList = new ArrayList<>();
    ArrayList<byte[]> imageByteArrayList = new ArrayList<>();
    ArrayList<byte[]> encryptedImageByteArray = new ArrayList<>();
    ArrayList<byte[]> decryptedImageByteArray = new ArrayList<>();
    private int totalSize = 0;
    private long startTime,endTime;
    byte[] encryptedByteArray;
    byte[] decryptedByteArray;
    Intent intent, intentData;

    public MainActivity() throws NoSuchAlgorithmException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = findViewById(R.id.messageText);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);
        addImage = findViewById(R.id.addImageButton);
        tvImageDetails = findViewById(R.id.tvImageDetails);
        imageLinearLayout = findViewById(R.id.imageLinearLayout);
        try {
            hashingTest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                intent.setType("image/jpeg");
                startActivityForResult(intent,100);
            }
        });
        encryptButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                imageByteArrayList = createImageByteArray(imageList);
                for(int i=0;i<imageByteArrayList.size();++i)
                    totalSize += imageByteArrayList.get(i).length;

                if(message.getText().toString().length() != 0 && imageList.size() != 0) {
                    TextView textView3 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params3 =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView3.setText("Original Message " + message.getText().toString());
                    textView3.setLayoutParams(params3);
                    imageLinearLayout.addView(textView3);

                    TextView textView = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView.setText("Length of Message Byte Array: " + message.getText().toString().getBytes().length);
                    textView.setLayoutParams(params);
                    imageLinearLayout.addView(textView);

                    TextView textView1 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params1 =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView1.setText("Image Byte array size: " + totalSize);
                    textView1.setLayoutParams(params1);
                    imageLinearLayout.addView(textView1);
                    startTime = System.currentTimeMillis();
                try {
                    encryptedByteArray = encrypt(message.getText().toString().getBytes(),generateCustomKey());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                    Log.d(TAG, "onClick: encrypt " + encryptedByteArray.length);
                for(int i=0;i<imageByteArrayList.size();++i)
                {
                    try {
                        encryptedImageByteArray.add(encrypt(imageByteArrayList.get(i), generateCustomKey()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                endTime = System.currentTimeMillis();
                totalSize = 0;
                    for(int i=0;i<encryptedImageByteArray.size();++i)
                        totalSize += encryptedImageByteArray.get(i).length;
                    TextView textView4 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params4 =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView4.setText("Encrypted Image Byte array size: " + totalSize);
                    textView4.setLayoutParams(params4);
                    imageLinearLayout.addView(textView4);
                    TextView textView6 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params6 =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView6.setText("Encrypted message similarity index : " + compareByteArrays(encryptedByteArray, message.getText().toString().getBytes())  + " %");
                    textView6.setLayoutParams(params6);
                    imageLinearLayout.addView(textView6);
                    for(int i=0;i<imageList.size();++i)
                    {
                        try {
                            setImageToLayout(getBitMapFromURI(imageList.get(i)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else
                    Toast.makeText(MainActivity.this, "please enter some text before encrypting", Toast.LENGTH_SHORT).show();
            }
        });
        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(message.getText().toString().length() != 0 && encryptedByteArray != null && imageList.size()!=0) {
                    try {
                        decryptedByteArray = decrypt(encryptedByteArray, generateCustomKey());
                        for (int i = 0; i < encryptedImageByteArray.size(); ++i) {

                            decryptedImageByteArray.add(decrypt(encryptedImageByteArray.get(i), generateCustomKey()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    imageLinearLayout.removeAllViews();
                    double rate;
                    rate = (endTime - startTime) / 1000.0;
                    TextView textView5 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params5 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView5.setText("Speed of Encryption: " + rate + "sec");
                    textView5.setLayoutParams(params5);
                    imageLinearLayout.addView(textView5);
                    TextView textView1 = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView1.setText("After Decryption message: " + new String(decryptedByteArray));
                    textView1.setLayoutParams(params1);
                    imageLinearLayout.addView(textView1);

                    TextView textView = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView.setText("Image After Decryption:");
                    textView.setLayoutParams(params);
                    imageLinearLayout.addView(textView);
                    for (int i = 0; i < decryptedImageByteArray.size(); ++i) {
                        setImageToLayout(getImagesFromByteArray(decryptedImageByteArray.get(i)));
                    }
                    encryptedImageByteArray.clear();
                    decryptedImageByteArray.clear();
                    imageList.clear();
                    message.setText("");
                    encryptedByteArray = null;
                    totalSize = 0;
                }
                else
                    Toast.makeText(MainActivity.this, "please encrypt something before decrypt", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hashingTest() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(HASHING);
        byte[] temp = messageDigest.digest("aabbc".getBytes());
        byte[] temp1 = Arrays.copyOf(temp,16);
        Key key1 = new SecretKeySpec(temp1, ALGORITHM);
        Key key2 = new SecretKeySpec(Arrays.copyOf( messageDigest.digest("aabbc".getBytes()),16), ALGORITHM);
        Log.d(TAG, "hashingTest: " + key1.equals(key2));
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100)
        {
            if(resultCode == RESULT_OK)
            {
                intentData = data;

                if(data != null && data.getClipData() != null)
                {
                    int selectedSize = data.getClipData().getItemCount();
                    tvImageDetails.setText("" + data.getClipData().getItemCount() + " images selected");
                    for(int i=0;i<selectedSize;++i)
                    {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        imageList.add(uri);
                    }
                }
                else
                {
                    Uri uri = data.getData();
                    imageList.add(uri);
                    tvImageDetails.setText("1 image selected");
                }
            }
            else
            {
                Toast.makeText(this, "please select something...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static byte[] encrypt(byte[] Data, Key key) throws GeneralSecurityException {
        Log.d(TAG, "encrypt: "  + key.getFormat() +  " " + key.getAlgorithm());
        Cipher c = Cipher.getInstance(ALGORITHM + PADDING_MODE);
        c.init(Cipher.ENCRYPT_MODE, key, generateIVParameterSpec(initVector));
        return c.doFinal(Data);
    }

    private static byte[] decrypt(byte[] encryptedData, Key key) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(ALGORITHM + PADDING_MODE);
        c.init(Cipher.DECRYPT_MODE, key, generateIVParameterSpec(initVector));
        return c.doFinal(encryptedData);
    }

    private static Key generateCustomKey() throws NoSuchAlgorithmException {
        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance(HASHING);
        return new SecretKeySpec(Arrays.copyOf(messageDigest.digest(password), 16), ALGORITHM);
    }

    private static IvParameterSpec generateIVParameterSpec(byte[] initVector) throws NoSuchAlgorithmException {
        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance(HASHING);
        return new IvParameterSpec(Arrays.copyOf(messageDigest.digest(initVector), 16));
    }

    private ArrayList<byte[]> createImageByteArray(ArrayList<Uri> imageList)
    {
        ArrayList<byte[]> imageByteArrayList = new ArrayList<>();
        for(int i=0;i<imageList.size();++i)
        {
            imageByteArrayList.add(getImage(imageList.get(i)));
        }
        return imageByteArrayList;
    }
    byte[] getImage(Uri uri) {

        ImageDecoder.Source imageDecoder = ImageDecoder.createSource(getApplicationContext().getContentResolver(), uri);
        try {
            Bitmap bitmap = ImageDecoder.decodeBitmap(imageDecoder);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setImageToLayout(Bitmap bitmap) {
        ImageView imageByCode = new ImageView(this);
        imageByCode.setImageBitmap(bitmap);
        LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageByCode.setLayoutParams(params);
        imageLinearLayout.addView(imageByCode);
    }

    private Bitmap getImagesFromByteArray(byte[] imageByteArray)
    {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray,0,imageByteArray.length);
        return bitmap;
    }
    private Bitmap getBitMapFromURI(Uri uri) throws IOException {
        ImageDecoder.Source imageDecoder = ImageDecoder.createSource(getApplicationContext().getContentResolver(), uri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(imageDecoder);
            return bitmap;
    }

    public double compareByteArrays(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length), nLarge = Math.max(a.length, b.length);
        int equalCount = nLarge - n;
        for (int i=0; i<n; i++)
            if (a[i] == b[i]) equalCount++;
        return equalCount * 100.0 / nLarge;
    }
}
