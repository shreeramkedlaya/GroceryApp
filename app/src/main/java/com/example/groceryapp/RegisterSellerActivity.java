package com.example.groceryapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RegisterSellerActivity extends AppCompatActivity implements LocationListener {
    // permission constants
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    private ImageButton backBtn, gpsBtn;
    private ImageView profileIv;
    private EditText nameEt, countryEt, phoneEt, stateEt, cityEt, shopNameEt, addressEt, deliveryFeeEt, emailEt, passwordEt, cpasswordEt;
    private Button registerBtn;
    // permission arrays
    private String[] locationPermissions;
    private String[] cameraPermissions;
    private String[] storagePermissions;

    // image picked uri
    private Uri imageUri;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private double latitude = 0.0, longitude = 0.0;
    private String fullName, shopName, phoneNumber, deliveryFee, country, state, city, address, email, password, confirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_seller);
        backBtn = findViewById(R.id.backBtn);
        gpsBtn = findViewById(R.id.gpsBtn);
        profileIv = findViewById(R.id.profileIv);
        registerBtn = findViewById(R.id.registerBtn);
        nameEt = findViewById(R.id.nameEt);
        phoneEt = findViewById(R.id.phoneEt);
        countryEt = findViewById(R.id.countryEt);
        shopNameEt = findViewById(R.id.shopNameEt);
        stateEt = findViewById(R.id.stateEt);
        deliveryFeeEt = findViewById(R.id.deliveryFeeEt);
        cityEt = findViewById(R.id.cityEt);
        addressEt = findViewById(R.id.addressEt);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        cpasswordEt = findViewById(R.id.cpasswordEt);

        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        gpsBtn.setOnClickListener(view -> {
            if (checkLocationPermission()) {
                detectLocation();
            } else {
                requestLocationPermission();
            }
        });
        backBtn.setOnClickListener(view -> onBackPressed());
        profileIv.setOnClickListener(view -> {
            // pick image
            showImagePickDialog();
        });
        registerBtn.setOnClickListener(view -> {
            // register user
            inputData();
        });
        gpsBtn.setOnClickListener(view -> {
            if (checkLocationPermission()) {
                detectLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private void showImagePickDialog() {
        // options to display dialog
        String[] options = {"Camera", "Gallery"};
        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, (dialogInterface, i) -> {
            if (i == 0) {
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
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private void detectLocation() {
        progressDialog.setMessage("Detecting Location...");
        progressDialog.show();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private void findAddress() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);

            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            // set addresses
            addressEt.setText(address);
            cityEt.setText(city);
            stateEt.setText(state);
            countryEt.setText(country);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean res = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean res1 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        return res && res1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        findAddress();

        // Dismiss the progress dialog
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
        Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted) {
                        // permission allowed
                        detectLocation();
                    } else {
                        Toast.makeText(this, "Location permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        // permission allowed
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera permissions are necessary", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // set to image view
                profileIv.setImageURI(imageUri);

            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                profileIv.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void inputData() {
        fullName = nameEt.getText().toString().trim();
        shopName = shopNameEt.getText().toString().trim();
        phoneNumber = phoneEt.getText().toString().trim();
        deliveryFee = deliveryFeeEt.getText().toString().trim();
        country = countryEt.getText().toString().trim();
        state = stateEt.getText().toString().trim();
        city = cityEt.getText().toString().trim();
        address = addressEt.getText().toString().trim();
        email = emailEt.getText().toString().trim();
        password = passwordEt.getText().toString().trim();
        confirmPassword = cpasswordEt.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Enter Name...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(shopName)) {
            Toast.makeText(this, "Enter Shop Name...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Enter Phone Number...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(deliveryFee)) {
            Toast.makeText(this, "Enter Delivery Fee...", Toast.LENGTH_SHORT).show();
        }
        if (latitude == 0.0 || longitude == 0.0) {
            Toast.makeText(this, "Please click GPS button to detect location...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email!", Toast.LENGTH_SHORT).show();
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email pattern...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email!", Toast.LENGTH_SHORT).show();
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password greater than 6 characters...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password don't match...", Toast.LENGTH_SHORT).show();
        }

        createAccount();
    }

    private void createAccount() {
        progressDialog.setMessage("Creating account");
        progressDialog.show();

        // create account
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            // account created
            saveFirebaseData();
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // failed creating account
                progressDialog.dismiss();
                Toast.makeText(RegisterSellerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void saveFirebaseData() {
        progressDialog.setMessage("Saving Account Info");

        String timestamp = "" + System.currentTimeMillis();

        if (imageUri == null) {
            // save w/o image

            // setup data to save
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid", firebaseAuth.getUid());
            hashMap.put("email", email);
            hashMap.put("name", fullName);
            hashMap.put("shopName", shopName);
            hashMap.put("phoneNumber", phoneNumber);
            hashMap.put("deliveryFee", deliveryFee);
            hashMap.put("country", country);
            hashMap.put("state", state);
            hashMap.put("city", city);
            hashMap.put("address", address);
            hashMap.put("latitude", latitude);
            hashMap.put("longitude", longitude);
            hashMap.put("timestamp", timestamp);
            hashMap.put("accountType", "Seller");
            hashMap.put("online", "true");
            hashMap.put("shopOpen","true");
            hashMap.put("profileImage", "");

            // save to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            // db updated
                            progressDialog.dismiss();
                            startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(e -> {
                        // failed updating db
                        progressDialog.dismiss();
                        startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                        finish();
                    });

        } else {
            // save info with image

            // name and path of the file
            String filePathName = "profileImages/" + firebaseAuth.getUid();
            // upload image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathName);
            storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) {
                        Uri downloadImageUri = uriTask.getResult();
                        if (uriTask.isSuccessful()) {

                            // setup data to save
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("uid", firebaseAuth.getUid());
                            hashMap.put("email", email);
                            hashMap.put("password", password);
                            hashMap.put("timestamp", timestamp);
                            hashMap.put("accountType", "Seller");
                            hashMap.put("profile_image", downloadImageUri);

                            // save to db
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                            ref.child(Objects.requireNonNull(firebaseAuth.getUid())).setValue(hashMap).addOnSuccessListener(unused -> {
                                // db updated
                                progressDialog.dismiss();
                                startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                // failed updating db
                                progressDialog.dismiss();
                                startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                                finish();
                            });
                        }
                    }

                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(RegisterSellerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}