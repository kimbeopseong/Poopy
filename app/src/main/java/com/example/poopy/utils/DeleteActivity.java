package com.example.poopy.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.example.poopy.R;
import com.example.poopy.ui.list.ListActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DeleteActivity extends Activity {

    FirebaseFirestore db;
    private static final String TAG = "DeleteActivity";
    private Intent intent;
    private String currentUID, currentPID, currentCatName, itemId, imgName;
    private Button delete, cancel;
    private CollectionReference poopData;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_delete);

        delete = (Button) findViewById(R.id.btn_delete_ok);
        cancel = (Button) findViewById(R.id.btn_delete_cancel);

        intent = getIntent();
        currentUID = intent.getStringExtra("currentUser");
        currentPID = intent.getStringExtra("currentPID");
        currentCatName = intent.getStringExtra("currentCatName");
        itemId = intent.getStringExtra("itemId");
        imgName = intent.getStringExtra("imgName");

        db = FirebaseFirestore.getInstance();
        poopData = db.collection("Users").document(currentUID).collection("Cat").document(currentPID).collection("PoopData");
        mStorageRef = FirebaseStorage.getInstance().getReference();

    }

    @Override
    protected void onStart() {
        super.onStart();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("Users").document(currentUID).collection("Cat").document(currentPID).collection("PoopData").document(itemId).delete();
                mStorageRef.child("Feeds/" + currentUID + "/" + currentCatName + "/" + imgName + ".jpg").delete();
                ListActivity.adapter.refresh();
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
}
