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

    private static final String TAG = "AddCatActivity";
    int REQUEST_IMAGE_CODE=1001;
    private StorageReference mStorageRef;
    int REQUEST_EXTERNAL_STORAGE_PERMISSION=1002;

    private String currentUserID;
    private FirebaseAuth mAuth;
    String cat_profile_download_url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        //User 내부 컬렉션으로 Pet을 둠
        documentId = db.collection("Users").document(currentUserID).collection("Cat").document().getId();

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
        //고양이 사진선택 -> onActivityResult호출 2020.06.05 BJH
        cvCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(in, REQUEST_IMAGE_CODE);
            }
        });

        //고양이 추가 버튼 클릭 시 해당 내용 반영 2020.06.05 BJH
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
    //
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

            user.put("c_name", catName);
            user.put("c_sex", catSex);
            user.put("c_species", catSpecies);
            user.put("c_age", catAge);

            //User ID의 내부컬렉션 Cat에 도큐먼트 생성 2020.06.05 BJH
            db.collection("Users").document(currentUserID).collection("Cat").document(documentId).
                    set(user, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
    }

    //고양이 사진 선택을 위해 cvCat을 클릭했을 때 호출 2020.06.05 BJH
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
            //firebase storage에 사진 반영 2020.06.05 BJH
            final StorageReference riversRef = mStorageRef.child("Cats").child(currentUserID).child(documentId).child("profile.jpg");
            UploadTask uploadTask=riversRef.putFile(image);
            Task<Uri> uriTask=uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        SweetToast.error(AddCatActivity.this, "Profile Photo Error: " + task.getException().getMessage());
                    }
                    cat_profile_download_url=riversRef.getDownloadUrl().toString();
                    return riversRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    //firebase database에 Cat사진 URI 추가 2020.06.05 BJH
                    if(task.isSuccessful()){
                        cat_profile_download_url=task.getResult().toString();
                        HashMap<String, Object> update_cat_data=new HashMap<>();
                        update_cat_data.put("c_uri",cat_profile_download_url);
                        db.collection("Users").document(currentUserID).collection("Cat").document(documentId).
                                set(update_cat_data,SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
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