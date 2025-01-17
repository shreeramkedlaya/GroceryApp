package com.example.groceryapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProfileEditSellerActivity extends AppCompatActivity implements LocationListener {
    // permission constants
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    // image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    private ImageButton backBtn, gpsBtn;
    private ImageView profileIv;
    private EditText nameEt, shopNameEt, phoneEt, deliveryFeeEt, countryEt, stateEt, cityEt, addressEt;
    private Button updateBtn;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch shopOpenSwitch;

    // permission arrays
    private String[] locationPermissions;
    private String[] cameraPermissions;
    private String[] storagePermissions;

    private double latitude = 0.0;
    private double longitude = 0.0;

    // progress dialog
    private ProgressDialog progressDialog;

    // firebase auth
    private FirebaseAuth firebaseAuth;

    private LocationManager locationManager;

    // image uri
    private Uri imageUri;
    private String name, shopName, phone, deliveryFee, country, state, city, address;
    private boolean shopOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit_seller);
        backBtn = findViewById(R.id.backBtn);
        gpsBtn = findViewById(R.id.gpsBtn);
        profileIv = findViewById(R.id.profileIv);
        nameEt = findViewById(R.id.nameEt);
        shopNameEt = findViewById(R.id.shopNameEt);
        phoneEt = findViewById(R.id.phoneEt);
        deliveryFeeEt = findViewById(R.id.deliveryFeeEt);
        countryEt = findViewById(R.id.countryEt);
        stateEt = findViewById(R.id.stateEt);
        cityEt = findViewById(R.id.cityEt);
        addressEt = findViewById(R.id.addressEt);
        shopOpenSwitch = findViewById(R.id.shopOpenSwitch);
        updateBtn = findViewById(R.id.updateBtn);

        // init permission arrays
        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // firebase auth init
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        backBtn.setOnClickListener(v -> onBackPressed());

        profileIv.setOnClickListener(v -> {
            showImagePickDialog();
        });
        gpsBtn.setOnClickListener(v -> {
            // detect location
            if (!checkLocationPermission()) {
                // already allowed
                detectLocation();
            } else {
                requestLocationPermission();
            }
        });
        updateBtn.setOnClickListener(v -> {
            // update Profile
            inputData();
        });
    }

    private void inputData() {
// input data
        name = nameEt.getText().toString().trim();
        shopName = shopNameEt.getText().toString().trim();
        phone = phoneEt.getText().toString().trim();
        deliveryFee = deliveryFeeEt.getText().toString().trim();
        country = countryEt.getText().toString().trim();
        state = stateEt.getText().toString().trim();
        city = cityEt.getText().toString().trim();
        address = addressEt.getText().toString().trim();
        shopOpen = shopOpenSwitch.isChecked(); // true or false

        updateProfile();
    }

    private void updateProfile() {
        progressDialog.setMessage("Updating Profile");
        progressDialog.show();

        if (imageUri == null) {
            // update without image

            // setup data to update
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", name);
            hashMap.put("shopName", shopName);
            hashMap.put("phone", phone);
            hashMap.put("deliveryFee", deliveryFee);
            hashMap.put("country", country);
            hashMap.put("state", state);
            hashMap.put("city", city);
            hashMap.put("address", address);
            hashMap.put("latitude", latitude);
            hashMap.put("longitude", longitude);
            hashMap.put("shopOpen", shopOpen);

            // update to db

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(Objects.requireNonNull(firebaseAuth.getUid())).updateChildren(hashMap).addOnSuccessListener(unused -> {
                // updated
            }).addOnFailureListener(e -> {
                // failed to update
                progressDialog.dismiss();
                Toast.makeText(ProfileEditSellerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // update with image
            /*-----------upload image first-----------*/
            String filePathAndName = "profileImages/" + firebaseAuth.getUid();

            // get storage reference
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                // image uploaded, get url of uploaded image
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful()) {
                    Uri downloadImageUri = uriTask.getResult();

                    if (uriTask.isSuccessful()) {
                        // image url received, now update db

                        // setup data to update
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("name", name);
                        hashMap.put("shopName", shopName);
                        hashMap.put("phone", phone);
                        hashMap.put("deliveryFee", deliveryFee);
                        hashMap.put("country", country);
                        hashMap.put("state", state);
                        hashMap.put("city", city);
                        hashMap.put("address", address);
                        hashMap.put("latitude", latitude);
                        hashMap.put("longitude", longitude);
                        hashMap.put("shopOpen", shopOpen);
                        hashMap.put("profileImage", downloadImageUri);

                        // update to db

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(Objects.requireNonNull(firebaseAuth.getUid())).updateChildren(hashMap).addOnSuccessListener(unused -> {
                            // updated
                        }).addOnFailureListener(e -> {
                            // failed to update
                            progressDialog.dismiss();
                            Toast.makeText(ProfileEditSellerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(ProfileEditSellerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        } else {
            loadMyInfo();
        }
    }

    private void loadMyInfo() {
        // load user info and set to views
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String accountType = "" + ds.child("accountType").getValue();
                    String address = "" + ds.child("address").getValue();
                    String city = "" + ds.child("city").getValue();
                    String state = "" + ds.child("state").getValue();
                    String country = "" + ds.child("country").getValue();
                    String deliveryFee = "" + ds.child("deliveryFee").getValue();
                    String email = "" + ds.child("email").getValue();
                    latitude = Double.parseDouble("" + ds.child("latitude").getValue());
                    longitude = Double.parseDouble("" + ds.child("longitude").getValue());
                    String name = "" + ds.child("name").getValue();
                    String online = "" + ds.child("online").getValue();
                    String phoneNumber = "" + ds.child("phoneNumber").getValue();
                    String profileImage = "" + ds.child("profileImage").getValue();
                    String shopName = "" + ds.child("shopName").getValue();
                    String shopOpen = "" + ds.child("shopOpen").getValue();
                    String uid = "" + ds.child("uid").getValue();

                    nameEt.setText(name);
                    phoneEt.setText(phoneNumber);
                    countryEt.setText(country);
                    stateEt.setText(state);
                    cityEt.setText(city);
                    addressEt.setText(address);
                    shopNameEt.setText(shopName);
                    deliveryFeeEt.setText(deliveryFee);

                    shopOpenSwitch.setChecked(shopOpen.equals("true"));
                    try {
                        Picasso.get().load(profileImage).placeholder(R.drawable.ic_store_gray).into(profileIv);
                    } catch (Exception e) {
                        profileIv.setImageResource(R.drawable.ic_person_gray);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showImagePickDialog() {
        // options to display in dialog
        String[] options = {"Gallery", "Camera"};

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // handle item clicks
                if (which == 0) {
                    // camera clicked
                    if (checkCameraPermissions()) {
                        // allowed open camera
                        pickFromCamera();
                    } else {
                        // not allowed, request
                        requestCameraPermission();
                    }
                } else {
                    // gallery clicked
                    if (checkStoragePermission()) {
                        // allowed, open gallery
                        pickFromGallery();
                    } else {
                        // not allowed, request
                        requestStoragePermission();
                    }
                }
            }
        });

    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkCameraPermissions() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
    }

    private void pickFromGallery() {
        // intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }


    private void pickFromCamera() {
        // intent to pick image from camera

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private void detectLocation() {
        Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

    }


    private void findAddress() {
        // find address, state, city and country
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            assert addresses != null;
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            // set addresses
            countryEt.setText(country);
            stateEt.setText(state);
            cityEt.setText(city);
            addressEt.setText(address);

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        findAddress();
    }


    @Override
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // handle image pick result
        if (resultCode == RESULT_OK) {
            if (resultCode == IMAGE_PICK_GALLERY_CODE) {
                // picked from gallery
                imageUri = data.getData();
                // set imageview
                profileIv.setImageURI(imageUri);
            } else if (resultCode == IMAGE_PICK_CAMERA_CODE) {
                profileIv.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, "Location is disabled", Toast.LENGTH_SHORT).show();
    }
}