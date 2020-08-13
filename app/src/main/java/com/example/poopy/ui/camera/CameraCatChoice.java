package com.example.poopy.ui.camera;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.poopy.utils.Cat;
import com.example.poopy.R;
import com.firebase.ui.firestore.SnapshotParser;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class CameraCatChoice extends Fragment implements CameraCatChoiceAdapter.OnListItemClick {
        private static final String TAG = "CAMERA CAT FRAGMENT";

        private View camera_catChoice;
        private RecyclerView catChoiceList;

        private FirebaseAuth mAuth;
        private String currintUID;
        private FirebaseFirestore db;

        public CameraCatChoice(){}

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

                camera_catChoice = inflater.inflate(R.layout.fragment_cat_choice, container, false);

                mAuth = FirebaseAuth.getInstance();
                currintUID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

                db = FirebaseFirestore.getInstance();

                PagedList.Config config = new PagedList.Config.Builder().setInitialLoadSizeHint(6).setPageSize(3).build();

                FirestorePagingOptions<Cat> options = new FirestorePagingOptions.Builder<Cat>()
                        .setLifecycleOwner(this)
                        .setQuery(db.collection("Users").document(currintUID).collection("Cat").whereEqualTo("Name", true), config, new SnapshotParser<Cat>() {
                                @NonNull
                                @Override
                                public Cat parseSnapshot(@NonNull DocumentSnapshot snapshot) {
                                        Cat cat = snapshot.toObject(Cat.class);
                                        cat.setCatName(Objects.requireNonNull(snapshot.get("c_name")).toString());
                                        cat.setProfile(Objects.requireNonNull(snapshot.get("c_uri")).toString());
                                        return cat;
                                }
                        }).build();

                CameraCatChoiceAdapter choiceAdapter = new CameraCatChoiceAdapter(options, this);

                catChoiceList = (RecyclerView) camera_catChoice.findViewById(R.id.cat_choice_view);
                catChoiceList.setLayoutManager(new GridLayoutManager(getContext(), 2));
                catChoiceList.addItemDecoration(new ItemDecoration(2, 50));
                catChoiceList.setAdapter(choiceAdapter);

                return camera_catChoice;
        }

        @Override
        public void onItemClick(DocumentSnapshot snapshot, int position) {
                Log.d("ITEM_CLICK", "Clicked an item: " + position + ", id: " + snapshot.getString("Name"));
                Intent intent = new Intent(this.getContext(), CameraActivity.class);
                intent.putExtra("Name", snapshot.getString("Name"));
                startActivity(intent);
        }

        public class ItemDecoration extends RecyclerView.ItemDecoration{
                private int spanCount;
                private int spacing;

                public ItemDecoration(int spanCount, int spacing){
                        this.spanCount = spanCount;
                        this.spacing = spacing;
                }

                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        int position = parent.getChildAdapterPosition(view);
                        int column = position % spanCount;

                        if (column < 1){
                                outRect.right = spacing - (column + 1) * spacing / spanCount;
                        } else {
                                outRect.right = 0;
                        }

                        outRect.bottom = spacing;

                        super.getItemOffsets(outRect, view, parent, state);
                }
        }
}
