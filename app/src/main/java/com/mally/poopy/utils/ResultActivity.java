package com.mally.poopy.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mally.poopy.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ResultActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView res_pic;
    private TextView res_date, res_stat, res_lv;
    private CollectionReference poopData;
    private DocumentReference docRef;

    private String currentUID, currentPID, itemId, date;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        res_pic = (ImageView) findViewById(R.id.res_pic);
        res_date = (TextView) findViewById(R.id.res_date);
        res_stat = (TextView) findViewById(R.id.res_stat);
        res_lv = (TextView) findViewById(R.id.res_lv);

        Button btn_ok = (Button) findViewById(R.id.res_ok);

        mAuth = FirebaseAuth.getInstance();
        currentUID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        Intent intent = getIntent();
        db = FirebaseFirestore.getInstance();
        currentPID = intent.getStringExtra("pid");
        poopData = db.collection("Users").document(currentUID).collection("Cat").document(currentPID).collection("PoopData");

        try {
            itemId = intent.getStringExtra("itemId");

            docRef = poopData.document(itemId);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            setResult(document);
                        }
                    }
                }
            });
        } catch (Exception e){
            date = intent.getStringExtra("date");

            poopData.whereEqualTo("date",date).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()){
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                            setResult(document);
                        }
                    }
                }
            });
        }

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void setResult(DocumentSnapshot document){
        final String uri = document.get("poopy_uri").toString();
        Picasso.get().load(uri)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_profile_image)
                .error(R.drawable.default_profile_image)
                .resize(0, 200)
                .into(res_pic, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(uri)
                                .placeholder(R.drawable.default_profile_image)
                                .error(R.drawable.default_profile_image)
                                .resize(0, 200)
                                .into(res_pic);
                    }
                });
        res_date.setText(Objects.requireNonNull(document.get("date")).toString());
        res_stat.setText(Objects.requireNonNull(document.get("stat")).toString());
        res_lv.setText(Objects.requireNonNull(document.get("lv")).toString());
    }
}
