package com.example.groceryapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Objects;

public class AddProductActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    String productTitle, productDescription, productCategory, productQuantity, originalPrice, discountPrice, discountNote;
    boolean discountAvailable = false;
    private Uri imageUri;
    private ImageButton backBtn;
    private Button addProductBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedNoteEt, discountedPriceEt;
    private TextView categoryTv;
    private Switch discountSwitch;
    private String[] cameraPermissions;
    private String[] storagePermissions;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        backBtn = findViewById(R.id.backBtn);
        productIconIv = findViewById(R.id.productIconIv);
        titleEt = findViewById(R.id.titleEt);
        categoryTv = findViewById(R.id.categoryTv);
        descriptionEt = findViewById(R.id.descriptionEt);
        quantityEt = findViewById(R.id.quantityEt);
        priceEt = findViewById(R.id.priceEt);
        discountedPriceEt = findViewById(R.id.discountedPriceEt);
        discountedNoteEt = findViewById(R.id.discountedNoteEt);
        discountSwitch = findViewById(R.id.discountSwitch);
        addProductBtn = findViewById(R.id.addProductBtn);

        //on start is unchecked,so hide discountPriceEt,discountNoteEt
        discountedPriceEt.setVisibility(View.GONE);
        discountedNoteEt.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();

        // setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // init permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // if discountSwitch is checked: show discountPriceEt, discountNoteEt | If discountSwitch is not checked: hide discountPriceEt, discountNoteEt
        discountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //checked,show discountPriceEt,discountNoteEt
                discountedPriceEt.setVisibility(View.VISIBLE);
                discountedNoteEt.setVisibility(View.VISIBLE);
            } else {
                //unchecked,hide discountPriceEt,discountNoteEt
                discountedPriceEt.setVisibility(View.GONE);
                discountedNoteEt.setVisibility(View.GONE);
            }
        });

        backBtn.setOnClickListener(view -> onBackPressed());
        productIconIv.setOnClickListener(view -> showImagePickDialog());
        categoryTv.setOnClickListener(view -> categoryDialog());
        addProductBtn.setOnClickListener(view -> inputData());
    }

    private void inputData() {
        // 1) Input data
        productTitle = titleEt.getText().toString().trim();
        productDescription = descriptionEt.getText().toString().trim();
        productCategory = categoryTv.getText().toString().trim();
        productQuantity = quantityEt.getText().toString().trim();
        originalPrice = priceEt.getText().toString().trim();
        discountAvailable = discountSwitch.isChecked();

        if (TextUtils.isEmpty(productTitle)) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(productCategory)) {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(originalPrice)) {
            Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
        }

        if (discountAvailable) {
            discountPrice = discountedPriceEt.getText().toString().trim();
            discountNote = discountedNoteEt.getText().toString().trim();
            if (TextUtils.isEmpty(discountPrice)) {
                Toast.makeText(this, "Discount Price is required", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            discountPrice = "0";
            discountNote = "";
        }
        addProduct();
    }

    private void addProduct() {
        progressDialog.setMessage("Adding Product");
        progressDialog.show();

        String timestamp = "" + System.currentTimeMillis();

        if (imageUri == null) {
            // upload without image

            // setup data to upload
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productId", timestamp);
            hashMap.put("productTitle", productTitle);
            hashMap.put("productDescription", productDescription);
            hashMap.put("productQuantity", productQuantity);
            hashMap.put("productIcon", ""); // no image, set empty
            hashMap.put("originalPrice", originalPrice);
            hashMap.put("discountPrice", discountPrice);
            hashMap.put("discountNote", discountNote);
            hashMap.put("discountAvailable", discountAvailable);
            hashMap.put("timestamp", timestamp);
            hashMap.put("uid", firebaseAuth.getUid());

            // add to db
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(Objects.requireNonNull(firebaseAuth.getUid())).child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(unused -> {
                        // added to db
                        progressDialog.dismiss();
                        Toast.makeText(this, "Product Added", Toast.LENGTH_SHORT).show();
                        clearData();
                    }).addOnFailureListener(e -> {
                        // failed adding to db
                        progressDialog.dismiss();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            // upload with image

            // first upload image to storage

            // name and path of image to be uploaded
            String fileAndPathName = "product_images/" + timestamp;

            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileAndPathName);
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        //image uploaded
                        // get url of the uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) {
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()) {
                                // url of the image received, upload to db
                                // setup data to upload
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("productId", timestamp);
                                hashMap.put("productTitle", productTitle);
                                hashMap.put("productDescription", productDescription);
                                hashMap.put("productQuantity", productQuantity);
                                hashMap.put("productIcon", downloadImageUri);
                                hashMap.put("originalPrice", originalPrice);
                                hashMap.put("discountPrice", discountPrice);
                                hashMap.put("discountNote", discountNote);
                                hashMap.put("discountAvailable", discountAvailable);
                                hashMap.put("timestamp", timestamp);
                                hashMap.put("uid", firebaseAuth.getUid());

                                // add to db
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(Objects.requireNonNull(firebaseAuth.getUid())).child(timestamp).setValue(hashMap).addOnSuccessListener(unused -> {
                                    // added to db
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Product Added", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    // failed adding to db
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
        }

    }

    private void clearData() {
        // clear data after uploading the product
        titleEt.setText("");
        descriptionEt.setText("");
        categoryTv.setText("");
        quantityEt.setText("");
        priceEt.setText("");
        discountedPriceEt.setText("");
        discountedNoteEt.setText("");
        productIconIv.setImageResource(R.drawable.ic_add_shopping_primary);
        imageUri=null;
    }

    private void categoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product Category").setItems(Constants.productCategories, (dialogInterface, which) -> {
            String category = Constants.productCategories[which];
            categoryTv.setText(category);
        });
    }

    private void showImagePickDialog() {
        String[] options = {"CAMERA", "GALLERY"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, (dialogInterface, which) -> {
            if (which == 0) {
                // camera clicked
                if (checkCameraPermission()) {
                    // camera permissions allowed
                    pickFromCamera();
                } else {
                    // camera permissions not allowed, request
                    requestCameraPermission();
                }
            } else {
                // gallery clicked
                if (checkStoragePermission()) {
                    // storage permission allowed
                    pickFromGallery();
                } else {
                    // storage permissions not allowed, request
                    requestStoragePermission();
                }
            }
        }).show();
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkCameraPermission() {
        boolean res = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean res1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return res && res1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    // handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        // permission allowed
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera and storage permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        // permission allowed
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is necessary", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // handle image pick results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // set to image view
                imageUri = data.getData();
                productIconIv.setImageURI(imageUri);

            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                productIconIv.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}