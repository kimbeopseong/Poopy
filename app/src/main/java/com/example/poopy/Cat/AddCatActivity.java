package com.example.poopy.Cat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.example.poopy.MainActivity;
import com.example.poopy.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.hasnat.sweettoast.SweetToast;

import static java.lang.Integer.parseInt;

public class AddCatActivity extends AppCompatActivity {
    EditText etCatSpecies, etCatName,etCatAge;
    RadioButton rbtnCMale,rbtnCFemale;
    Button btnAddCat;
    CircleImageView cvCat;
    FirebaseFirestore db;
    String documentId;

    //이미지 관련 부분
    private static final String TAG = "AddCatActivity";
    int REQUEST_IMAGE_CODE=1001;
    private CircleImageView ivUser;
    private ImageView editPhotoIcon;
    private StorageReference mStorageRef;
    int REQUEST_EXTERNAL_STORAGE_PERMISSION=1002;

    private String currentUserID;
    private FirebaseAuth mAuth;
    String pet_profile_download_url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cat);

        db = FirebaseFirestore.getInstance();
        documentId=db.collection("Pet").document().getId();
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        cvCat=(CircleImageView)findViewById(R.id.cvCat);
        etCatName = (EditText)findViewById(R.id.etCatName);
        etCatAge = (EditText)findViewById(R.id.etCatAge);
        etCatSpecies = (EditText)findViewById(R.id.etCatSpecies);
        rbtnCMale = (RadioButton)findViewById(R.id.rbtnCMale);
        rbtnCFemale = (RadioButton)findViewById(R.id.rbtnCFemale);
        btnAddCat = (Button)findViewById(R.id.btnAddCat);

        if(ContextCompat.checkSelfPermission(AddCatActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(AddCatActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){

            }else{
                ActivityCompat.requestPermissions(AddCatActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE_PERMISSION);
            }
        }else{

        }
        mStorageRef = FirebaseStorage.getInstance().getReference();

        cvCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(in, REQUEST_IMAGE_CODE);
            }
        });


        btnAddCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String catName = etCatName.getText().toString();
                int catAge = parseInt(etCatAge.getText().toString());
                String catSpecies = etCatSpecies.getText().toString();
                String catSex=null;
                if(rbtnCMale.isChecked())
                    catSex="수컷";
                else if(rbtnCFemale.isChecked())
                    catSex="암컷";
                createNewCat(catName, catAge, catSpecies, catSex);
                Intent intent = new Intent(AddCatActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
    private void createNewCat(String catName, int catAge ,String catSpecies, String catSex) {
        if (TextUtils.isEmpty(catName)) {
            SweetToast.error(AddCatActivity.this, "Your cat's name is required.");
        } else if (catAge == 0) {
            SweetToast.error(AddCatActivity.this, "Your cat's age is required.");
        } else if (catAge > 25) {
            SweetToast.error(AddCatActivity.this, "You fill in wrong age.");
        }else if (TextUtils.isEmpty(catSpecies)) {
            SweetToast.warning(AddCatActivity.this, "Your cat's Species is required.");
        } else if (TextUtils.isEmpty(catSex)) {
            SweetToast.warning(AddCatActivity.this, "Select your cat's sex");
        } else {
            final Map<String, Object> user = new HashMap<>();

            user.put("p_name", catName);
            user.put("p_sex", catSex);
            user.put("p_species", catSpecies);
            user.put("p_age", catAge);
            user.put("p_ID",currentUserID);

            // Add a new document with a generated ID
            db.collection("Pet")
                    .document(documentId).set(user, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
    }
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_IMAGE_CODE){
            final Uri image=data.getData();
            Log.d(TAG, "onActivityResult: "+image);
            Picasso.get().load(image)
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .resize(0,90)
                    .into(cvCat);

            final StorageReference riversRef = mStorageRef.child("Pets").child(currentUserID).child(documentId).child("profile.jpg");
            UploadTask uploadTask=riversRef.putFile(image);
            Task<Uri> uriTask=uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        SweetToast.error(AddCatActivity.this, "Profile Photo Error: " + task.getException().getMessage());
                    }
                    pet_profile_download_url=riversRef.getDownloadUrl().toString();
                    return riversRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        pet_profile_download_url=task.getResult().toString();

                        HashMap<String, Object> update_pet_data=new HashMap<>();
                        update_pet_data.put("p_uri",pet_profile_download_url);

                        db.collection("Pet").document(documentId).set(update_pet_data,SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                            }
                        });

                    }
                }
            });

        }
    }
}