/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kit.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2
 * {@link Button}s to move between pages. We use a
 * {@link PdfRenderer} to render PDF pages as
 * {@link Bitmap}s.
 */
public class PdfActivity extends AppCompatActivity implements android.view.View.OnClickListener {
    boolean canSpeak = false;

    TextToSpeech tts = null;

    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    private String TAG = "PdfRendererBasicFragment";
    private Context mContext;

    private Uri pdfFileUri;

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link ImageView} that shows a PDF page as a {@link Bitmap}
     */

    private RelativeLayout imageParentLayout;
    private ViewPager viewPager;

    /**
     * {@link Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link Button} to move to the next page.
     */
    private Button mButtonNext;

    /**
     * PDF page index
     */
    private int mPageIndex;
    private ImageView mImageView;
    private String text="";

    public PdfActivity() {
    }

    @Override
    protected void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_renderer_basic);

        pdfFileUri=getIntent().getData();
        // Retain view references.
        viewPager = findViewById(R.id.viewPager);
        //mImageView = (TouchImageView) view.findViewById(R.id.image);
        imageParentLayout = findViewById(R.id.rlParentWrapper);
        mButtonPrevious = (Button) findViewById(R.id.previous);
        mButtonNext = (Button) findViewById(R.id.next);
        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);







        mPageIndex = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }

    }


    @Override
    public void onStart() {
        super.onStart();
        try {
            openRenderer(this);
            //showPage(mPageIndex, null);
        } catch (IOException e) {
            e.printStackTrace();
            android.os.Bundle bundle = new android.os.Bundle();
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        ImagePagerAdapter adapter;
        adapter = new ImagePagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
    }

    @Override
    public void onStop() {
        try {
            closeRenderer();
        } catch (IOException e) {
            android.os.Bundle bundle = new android.os.Bundle();
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(tts!=null)
            if (this.tts.isSpeaking()) {
                this.tts.stop();
            }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        mContext = context;
        if(pdfFileUri != null) {
            File file = new File(context.getCacheDir(), getFileName(pdfFileUri));
            if (!file.exists()) {
                // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
                // the cache directory.
                //InputStream asset = context.getAssets().open(FILENAME);
                //Uri myUri = Uri.parse("content://com.android.providers.downloads.documents/document/4406");
                java.io.InputStream asset = context.getContentResolver().openInputStream(pdfFileUri);
                FileOutputStream output = new FileOutputStream(file);
                final byte[] buffer = new byte[1024];
                int size;
                while ((size = asset.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
                asset.close();
                output.close();
            }
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            // This is the PdfRenderer we use to render the PDF.
            if (mFileDescriptor != null) {
                mPdfRenderer = new PdfRenderer(mFileDescriptor);
            }
        }else{
            throw new IOException("Something wrong with fetching the PDF!");
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        android.util.Log.d(TAG, "getFileName: result "+result);
        return result;
    }

    /**
     * Closes the {@link PdfRenderer} and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index, ImageView lImageView) {
        mImageView = lImageView;
        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            try {
                mCurrentPage.close();
            }catch (Exception e){
                e.printStackTrace();

            }
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(
                (int)(3 * mCurrentPage.getWidth()),
                (int)(3 * mCurrentPage.getHeight()),
                Bitmap.Config.ARGB_8888
        );

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);


        mImageView.setImageBitmap(bitmap);

        mImageView.setOnTouchListener(new ImageMatrixTouchHandler(mImageView.getContext()));
        if(tts!=null)
            if (this.tts.isSpeaking()) {
                this.tts.stop();
        }
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            Task<FirebaseVisionText> result =
                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    // ...
                                    text = firebaseVisionText.getText();

                                    if (!canSpeak) {
                                        startActivityForResult(new Intent("android.speech.tts.engine.CHECK_TTS_DATA"), 1);
                                        return;
                                    }
                                    else{
                                        tts.speak(text, 1, null, null);
                                    }



                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });





        } catch (Exception e) {
            e.printStackTrace();
        }

        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
    }


    public void onActivityResult(int n, int n2, Intent intent) {
        super.onActivityResult(n, n2, intent);
        if (n2 == 1) {
            this.tts = new TextToSpeech((Context) this, new TextToSpeech.OnInitListener() {
                public void onInit(int n) {
                    if (n == 0) {
                        canSpeak = true;
                        tts.speak(text, 1, null, null);
                    }
                }
            });
            return;
        }

        this.startActivity(new Intent("android.speech.tts.engine.INSTALL_TTS_DATA"));
    }



    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(android.view.View view) {
        switch (view.getId()) {
            case R.id.previous: {
                //mImageView.resetOCR();
                // Move to the previous page
                showPage(mCurrentPage.getIndex() - 1, null);
                break;
            }
            case R.id.next: {
                //mImageView.resetOCR();
                // Move to the next page
                showPage(mCurrentPage.getIndex() + 1, null);
                break;
            }
        }
    }

    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            android.view.View layout = (android.view.View) inflater.inflate(R.layout.row_pager_image, collection, false);
            collection.addView(layout);

            ImageView ivImage = layout.findViewById(R.id.ivImage);
            showPage(position, ivImage);



            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((RelativeLayout) view);
        }

        @Override
        public int getCount() {
            return mPdfRenderer.getPageCount();
        }

        @Override
        public boolean isViewFromObject(android.view.View view, Object object) {
            return view == object;
        }


    }



}
