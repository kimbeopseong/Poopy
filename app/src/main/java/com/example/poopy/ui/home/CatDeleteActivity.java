package com.example.poopy.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.poopy.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;
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
    private HomeFragment homeFragment;

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

        homeFragment = HomeFragment.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAtPath(db.collection("Users").document(currentUID).collection("Cat").document(pickedPID).collection("PoopData").getPath());
                db.collection("Users").document(currentUID).collection("Cat").document(pickedPID).delete();
                mStorageRef.child("Cats/"+currentUID+"/"+pickedPID+"/profile.jpg").delete();
                homeRefresh();
                finish();
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
        deleteFn.call(data);
    }

    private void homeRefresh(){
        FragmentManager manager = homeFragment.getChildFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.detach(homeFragment).attach(homeFragment).commit();
    }
}
