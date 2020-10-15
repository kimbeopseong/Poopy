package com.example.poopy.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.poopy.R;
import com.example.poopy.utils.Cat;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CatDeleteActivity extends Activity {

    FirebaseFirestore db;
    private Intent intent;
    private String currentUID, pickedPID;
    private Button delete, cancel;
    private StorageReference mStorageRef;
    private FirebaseFunctions firebaseFunctions;
    private FirestoreRecyclerAdapter<Cat, HomeFragment.CatViewHolder> catAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_delete);

        delete = (Button) findViewById(R.id.btn_delete_ok);
        cancel = (Button) findViewById(R.id.btn_delete_cancel);

        intent = getIntent();
        currentUID = intent.getStringExtra("currentUID");
        pickedPID = intent.getStringExtra("pickedPID");

        db = FirebaseFirestore.getInstance();
        firebaseFunctions = FirebaseFunctions.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        catAdapter = HomeFragment.getCatAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAtPath(db.collection("Users").document(currentUID)
                        .collection("Cat").document(pickedPID).collection("PoopData").getPath());
            }
        });
        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    public void deleteAtPath(String path){
        Map<String, Object> data = new HashMap<>();
        data.put("path", path);

        HttpsCallableReference deleteFn = firebaseFunctions.getHttpsCallable("recursiveDelete");
        deleteFn.call(data)
            .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                @Override
                public void onSuccess(HttpsCallableResult httpsCallableResult) {
                    mStorageRef.child("Cats/"+currentUID+"/"+pickedPID+"/profile.jpg").delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                db.collection("Users").document(currentUID).collection("Cat").document(pickedPID).delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            finish();
                                        }
                                    });
                            }
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    Log.e("CatDeleteFail", e.getMessage());

                    mStorageRef.child("Cats/"+currentUID+"/"+pickedPID+"/profile.jpg").delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                db.collection("Users").document(currentUID).collection("Cat").document(pickedPID).delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            finish();
                                        }
                                    });
                            }
                        });

                }
            });
    }

}
