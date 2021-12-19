package com.example.ephoto_ver1;

import static android.os.Environment.DIRECTORY_PICTURES;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.BitmapFactory;

import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PER_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    ImageView selectedImage;
    Button cameraBtn, galleryBtn;
    String currentPhotoPath;
    StorageReference storageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectedImage=findViewById(R.id.displayImageView);
        cameraBtn=findViewById(R.id.cameraBtn);
        galleryBtn=findViewById(R.id.gallareBtn);
        storageReference = FirebaseStorage.getInstance().getReference();


        //storageReference = FirebaseStorage.getInstance().getReference();


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                //PHUC_19/12
                Intent gallery = new Intent(Intent.ACTION_PICK);
//                Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.provider");

                gallery.putExtra(MediaStore.EXTRA_OUTPUT, "com.example.android.provider");

                //Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
                //galleryAddPic();
                Toast.makeText(MainActivity.this, "Gallery Btn is Clickes", Toast.LENGTH_SHORT).show();

//                Intent gallery =new Intent(Intent.ACTION_PICK);
//
//                Uri photoURI = FileProvider.getUriForFile(this,
//                        "com.example.android.provider",
//                        photoFile);
//                Log.d("photoURI", "Path: "+photoURI.toString());
//                gallery.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }
        });
    }

    static final int GALLERY_REQUEST_CODE=105;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CODE=1;


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(DIRECTORY_PICTURES);
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        //Toast.makeText(MainActivity.this,currentPhotoPath, Toast.LENGTH_SHORT).show();
        Log.d("tag", "Path: "+currentPhotoPath);
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                //Toast.makeText(MainActivity.this, "Done create file", Toast.LENGTH_SHORT).show();
                Log.d("photoURI", "Done Create file ");
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("take picture","Loi xay  ra");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.provider",
                        photoFile);
                Log.d("photoURI", "Path: "+photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Log.d("photoURI", "Path: "+photoURI.toString());
                Toast.makeText(MainActivity.this, photoURI.toString(), Toast.LENGTH_SHORT).show();
                //Toast.makeText(MainActivity.this, "Done save file", Toast.LENGTH_SHORT).show();
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("OK","OK");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK) {
            setPic();
            File f =new File (currentPhotoPath);
            Uri contentUri= Uri.fromFile(f);
            uploadImageToFirebase(f.getName(), contentUri);
            //saveImageToFireBase(data.getData());


        }
        if(requestCode==GALLERY_REQUEST_CODE){
            if(resultCode== Activity.RESULT_OK){
                Uri contentUri=data.getData();
                String timeStamp= new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName="JPEG_"+timeStamp+"."+getFileExt(contentUri);
                Log.d("tag","Done gallery");
                selectedImage.setImageURI(contentUri);
                uploadImageToFirebase(imageFileName, contentUri);
                //saveImageToFireBase(contentUri);
                
            }
        }

    }


    private void saveImageToFireBase( Uri contentUri) {
        StorageReference image = storageReference.child("images/");
        StorageReference photoRef=image.child(contentUri.getLastPathSegment());
        image.getName().equals(image.getName());
        image.getPath().equals(image.getPath());
        photoRef.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URI is "+uri.toString());

                    }

                });
                Toast.makeText(MainActivity.this, "Image is Uploaded.", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Upload Failled", Toast.LENGTH_SHORT).show();

            }
        });
    }


    private void uploadImageToFirebase(String name, Uri contentUri) {
        StorageReference image = storageReference.child("images/"+name);
//        image.getName().equals(image.getName());
//        image.getPath().equals(image.getPath());
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URI is "+uri.toString());

                    }

                });
                Toast.makeText(MainActivity.this, "Image is Uploaded.", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Upload Failled", Toast.LENGTH_SHORT).show();

            }
        });



    }


    private String getFileExt(Uri contentUri) {
        ContentResolver c=getContentResolver();
        MimeTypeMap mime= MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));


    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = selectedImage.getWidth();
        int targetH = selectedImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        selectedImage.setImageBitmap(bitmap);
        try {
            requestAPI(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    public void requestAPI(Bitmap bitmap) throws Exception {
        final TextView textView = (TextView) findViewById(R.id.infoID);


        String url = "https://api.fpt.ai/vision/idr/vnm/";

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image",currentPhotoPath,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(currentPhotoPath)))
                .build();
        Request request = new Request.Builder()
                .url("https://api.fpt.ai/vision/idr/vnm/")
                .method("POST", body)
                .addHeader("api-key", "oI0ibb5x0cG4xiT84rviTZgbz4ccISl5")
                .build();
        Toast.makeText(MainActivity.this, "Start request", Toast.LENGTH_SHORT).show();
        Response response = client.newCall(request).execute();
        Toast.makeText(MainActivity.this, "Done request", Toast.LENGTH_SHORT).show();
        //Log.d("request","Done request");
        System.out.println(response.body().string());


    }



    //121221



    private void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


}




