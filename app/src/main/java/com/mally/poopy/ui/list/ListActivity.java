package com.mally.poopy.ui.list;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mally.poopy.R;
import com.mally.poopy.utils.ResultActivity;
import com.firebase.ui.firestore.SnapshotParser;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ListActivity extends AppCompatActivity implements RecycleAdapter.OnListItemClick, RecycleAdapter.OnItemLongClick{

    private RecyclerView recyclerView;
    public static RecycleAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private String currentUID, currentPID, currentName;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference poopData;
    private StorageReference mStorageRef;

    public ListActivity(){}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mAuth = FirebaseAuth.getInstance();
        currentUID = mAuth.getCurrentUser().getUid();

        Intent intent = getIntent();
        currentPID = intent.getStringExtra("pid");
        currentName = intent.getStringExtra("Name");

        db = FirebaseFirestore.getInstance();
        poopData = db.collection("Users").document(currentUID).collection("Cat").document(currentPID).collection("PoopData");
        mStorageRef = FirebaseStorage.getInstance().getReference();
        recyclerView = (RecyclerView) findViewById(R.id.recycler);

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Query for read the dataset
        PagedList.Config config = new PagedList.Config.Builder().setInitialLoadSizeHint(10).setPageSize(3).build();

        //RecyclerOptions
        FirestorePagingOptions<ListItem> options = new FirestorePagingOptions.Builder<ListItem>()
                .setLifecycleOwner(this)
                .setQuery(poopData.orderBy("date", Query.Direction.DESCENDING), config, new SnapshotParser<ListItem>() {
                    @NonNull
                    @Override
                    public ListItem parseSnapshot(@NonNull DocumentSnapshot snapshot) {
                        ListItem item = snapshot.toObject(ListItem.class);
                        String item_id = snapshot.getId();
                        item.setItem_id(item_id);
                        return item;
                    }
                })
                .build();

        adapter = new RecycleAdapter(options, this, this);

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(0);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClick(DocumentSnapshot snapshot, int position) {
        Log.d("ITEM_CLICK", "Clicked an item: " + position + ", id:" + snapshot.getId());
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("itemId", snapshot.getId());
        intent.putExtra("pid", currentPID);
        startActivity(intent);
    }

    @Override
    public void onLongClick(DocumentSnapshot snapshot, int position) {
        Log.d("ITEM_LONG_CLICK", "LONG Clicked an item: " + position + ", id:" + snapshot.getId());
        Intent intent = new Intent(this, DeleteActivity.class);
        intent.putExtra("currentUser", currentUID);
        intent.putExtra("currentPID", currentPID);
        intent.putExtra("currentCatName", currentName);
        intent.putExtra("itemId", snapshot.getId());
        intent.putExtra("imgName", snapshot.getString("date"));
        startActivity(intent);
    }
}
