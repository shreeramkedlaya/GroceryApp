package com.example.groceryapp;

import android.Manifest;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

public class EditProductActivity extends AppCompatActivity {

    // ui views
    private ImageButton backBtn;
    private Button updateProductBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedNoteEt, discountedPriceEt;
    private TextView categoryTv;
    private Switch discountSwitch;

    private String productId;

    // permission constants
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    // image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    // permission arrays
    private String[] cameraPermissions;
    private String[] storagePermissions;
    // image pick uri
    private Uri imageUri;


    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    String productTitle, productDescription, productCategory, productQuantity, originalPrice, discountPrice, discountNote;
    boolean discountAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

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
        updateProductBtn = findViewById(R.id.updateProductBtn);

        // get id of the product from intent
        productId = getIntent().getStringExtra("productId");

        //on start is unchecked,so hide discountPriceEt,discountNoteEt
        discountedPriceEt.setVisibility(View.GONE);
        discountedNoteEt.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        loadProductDetails(); // to set on views

        // setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // init permissions
        cameraPermissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
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
        updateProductBtn.setOnClickListener(view -> inputData());


    }

    private void loadProductDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Products").child(productId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // get data
                String id = "" + dataSnapshot.child("productId").getValue();
                String productTitle = "" + dataSnapshot.child("productTitle").getValue();
                String productDescription = "" + dataSnapshot.child("productDescription").getValue();
                String productQuantity = "" + dataSnapshot.child("productQuantity").getValue();
                String productIcon = "" + dataSnapshot.child("productIcon").getValue();
                String originalPrice = "" + dataSnapshot.child("originalPrice").getValue();
                String discountPrice = "" + dataSnapshot.child("discountPrice").getValue();
                String productCategory = "" + dataSnapshot.child("productCategory").getValue();
                String discountNote = "" + dataSnapshot.child("discountNote").getValue();
                String discountAvailable = "" + dataSnapshot.child("discountAvailable").getValue();
                String timestamp = "" + dataSnapshot.child("timestamp").getValue();
                String uid = "" + dataSnapshot.child("uid").getValue();

                // set data to views
                if (discountAvailable.equals("true")) {
                    discountSwitch.setChecked(true);

                    discountedPriceEt.setVisibility(View.VISIBLE);
                    discountedNoteEt.setVisibility(View.VISIBLE);
                } else {
                    discountedPriceEt.setVisibility(View.GONE);
                    discountedNoteEt.setVisibility(View.GONE);
                }

                titleEt.setText(productTitle);
                descriptionEt.setText(productDescription);
                categoryTv.setText(productCategory);
                discountedNoteEt.setText(discountNote);
                quantityEt.setText(productQuantity);
                priceEt.setText(originalPrice);
                discountedPriceEt.setText(discountPrice);

                try {
                    Picasso.get().load(productIcon).placeholder(R.drawable.ic_add_shopping_white).into(productIconIv);
                } catch (Exception e) {
                    productIconIv.setImageResource(R.drawable.ic_add_shopping_white);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
        updateProduct();
    }

    private void updateProduct() {
        // show progress
        progressDialog.setMessage("Updating Product");
        progressDialog.show();

        if (imageUri == null) {
            // update without image

            // setup data to upload
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productTitle", productTitle);
            hashMap.put("productDescription", productDescription);
            hashMap.put("productCategory", productCategory);
            hashMap.put("productQuantity", productQuantity);
            hashMap.put("originalPrice", originalPrice);
            hashMap.put("discountPrice", discountPrice);
            hashMap.put("discountNote", discountNote);
            hashMap.put("discountAvailable", discountAvailable);

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child(productId).updateChildren(hashMap).addOnSuccessListener(unused -> {
                // update success
                progressDialog.dismiss();
                Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                // update failed
                progressDialog.dismiss();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // upload with image

            // first upload image to storage

            // name and path of image on firebase storage
            String fileAndPathName = "product_images/" + productId; // override previous image using same id

            // uploaded image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileAndPathName);
            storageReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                //image uploaded
                // get url of the uploaded image
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful()) {
                    Uri downloadImageUri = uriTask.getResult();

                    if (uriTask.isSuccessful()) {
                        // url of the image received, upload to db
                        // setup data to upload
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("productTitle", productTitle);
                        hashMap.put("productDescription", productDescription);
                        hashMap.put("productCategory", productCategory);
                        hashMap.put("productIcon", downloadImageUri);
                        hashMap.put("productQuantity", productQuantity);
                        hashMap.put("originalPrice", originalPrice);
                        hashMap.put("discountPrice", discountPrice);
                        hashMap.put("discountNote", discountNote);
                        hashMap.put("discountAvailable", discountAvailable);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                        reference.child(firebaseAuth.getUid()).child(productId).updateChildren(hashMap).addOnSuccessListener(unused -> {
                            // update success
                            progressDialog.dismiss();
                            Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            // update failed
                            progressDialog.dismiss();
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // upload failed
                    Toast.makeText(EditProductActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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