package com.example.samps_000.fashionapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by samps_000 on 1/16/2016.
 */
public class CreatePost extends Activity implements OnServerCallCompleted {

    public static final int IMAGE_GALLERY_REQUEST = 20;
    private ImageView postPicture;

    private Bitmap postImage = null;

    private static final String POST_EXT = "/post";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        postPicture = (ImageView) findViewById(R.id.postImage);
    }

    public void selectImageClicked(View view) {
        //invoke the image gallery using an implicit intent
        Intent imageSelector = new Intent(Intent.ACTION_PICK);

        //where to look for image data
        File imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        String imageDirectoryPath = imageDirectory.getPath();

        //get URI representation
        Uri data = Uri.parse(imageDirectoryPath);

        imageSelector.setDataAndType(data, "image/*");

        startActivityForResult(imageSelector, IMAGE_GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            //everything went ok

            if (requestCode == IMAGE_GALLERY_REQUEST) {
                //if we are here, results are from image gallery

                //address of image on sd card
                Uri imageURI = data.getData();
                //declare a string to read image data from sd card
                InputStream inputStream;

                //getting input stream based on URI
                try {
                    inputStream = getContentResolver().openInputStream(imageURI);

                    //get a bitmap from the stream
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap image = BitmapFactory.decodeStream(inputStream, null, options);

                    //Set image
                    postPicture.setImageBitmap(image);

                    postImage = image;

                } catch (FileNotFoundException e) {
                    Log.d("Exception", "File not found: " + e.toString());
                }
            }
        }
    }

    public void postClicked(View view) {

        //get description
        EditText descEdit = (EditText) findViewById(R.id.descText);
        EditText locationEdit = (EditText) findViewById(R.id.locationText);

        //get image
        String imageEncoded = encodeToString(postImage);

        RequestBody requestBody = new FormBody.Builder()
                .add("image", imageEncoded)
                .add("desc", String.valueOf(descEdit.getText()))
                .add("location", String.valueOf(locationEdit.getText()))
                .build();

        OkhttpServerRequest request_object = new OkhttpServerRequest(this, POST_EXT, requestBody);
        Request request = request_object.getRequest();

        request_object.getClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("ERROR", "Could not post image " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    postImage.recycle();
                    Intent i = new Intent(CreatePost.this, Feed.class);
                    startActivity(i);
                }
                else{
                    Log.d("ERROR", "Error on success " + response.code() + " " + response.message());

                }
            }
        });

    }

    private String encodeToString(final Bitmap b) {
        //Might need to make this run on thread

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        b.compress(Bitmap.CompressFormat.WEBP, 80, stream);
        byte[] byte_arr = stream.toByteArray();
        String encodedString = Base64.encodeToString(byte_arr, 0);
        return encodedString;
    }

    @Override
    public void PerformResponse(int status) {
        if (status == 200){
            Toast.makeText(CreatePost.this, "POSTED", Toast.LENGTH_SHORT).show();

        }
        else if(status == 500){
            Toast.makeText(CreatePost.this, "Could not connect to server", Toast.LENGTH_SHORT).show();
            Log.d("Status Code: ", Integer.toString(status));
        }
        else{
            Toast.makeText(CreatePost.this, "Could not post! :(", Toast.LENGTH_SHORT).show();
            Log.d("Status Code: ", Integer.toString(status));
        }
    }
}
