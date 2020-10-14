package com.example.poopy.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.poopy.Cat.CatSetActivity;
import com.example.poopy.R;
import com.example.poopy.Cat.AddCatActivity;
import com.example.poopy.utils.Cat;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private View privateCatView;
    private RecyclerView catsList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ImageView addCat;
    private CollectionReference cats;
    private static HomeFragment homeFragment;

    String catUri,catName,catSex,catAge,catSpec;

    public HomeFragment(){ }

    public static HomeFragment newInstance(){
        return new HomeFragment();
    }
    public static HomeFragment getInstance() { return homeFragment;}

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db=FirebaseFirestore.getInstance();
        mAuth=FirebaseAuth.getInstance();
        currentUserId=mAuth.getCurrentUser().getUid();

        privateCatView = inflater.inflate(R.layout.fragment_home, container, false);

        catsList=(RecyclerView)privateCatView.findViewById(R.id.rvCat);
        catsList.setLayoutManager(new LinearLayoutManager(getContext()));
        addCat=(ImageView) privateCatView.findViewById(R.id.ivAddCat);
        addCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), AddCatActivity.class);
                startActivity(intent);
            }
        });
        cats = db.collection("Users").document(currentUserId).collection("Cat");
        homeFragment = HomeFragment.this;

        return privateCatView;
    }

    @Override
    public void onStart(){
        super.onStart();
        FirestoreRecyclerOptions<Cat> options = new FirestoreRecyclerOptions.Builder<Cat>()
                .setQuery(cats, Cat.class).build();
        //FireRecyclerAdapter로 Firebase Cat 컬렉션의 Document를 읽어옴
        FirestoreRecyclerAdapter<Cat, CatViewHolder> catAdapter=
                new FirestoreRecyclerAdapter<Cat, CatViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final CatViewHolder holder, int position, @NonNull Cat model) {
                        final String cat_uid=getSnapshots().getSnapshot(position).getId();
                        DocumentReference docRef=getSnapshots().getSnapshot(position).getReference();
                        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                db.collection("Users").document(currentUserId).collection("Cat").document(cat_uid).get().
                                        addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()){
                                            catName=task.getResult().get("c_name").toString();
                                            catSex=task.getResult().get("c_sex").toString();
                                            catAge=task.getResult().get("c_age").toString();
                                            catSpec=task.getResult().get("c_species").toString();
                                            if(task.getResult().contains("c_uri")){
                                                catUri=task.getResult().get("c_uri").toString();
                                                Picasso.get().load(catUri)
//                                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                                        .placeholder(R.drawable.default_profile_image)
                                                        .error(R.drawable.default_profile_image)
                                                        .resize(0,90)
                                                        .into(holder.ivPet, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                Picasso.get().load(catUri)
                                                                        .placeholder(R.drawable.default_profile_image)
                                                                        .error(R.drawable.default_profile_image)
                                                                        .resize(0,90)
                                                                        .into(holder.ivPet);
                                                            }
                                                        });
                                            }
                                            else{
                                                Picasso.get().load(R.drawable.default_profile_image)
                                                        .placeholder(R.drawable.default_profile_image)
                                                        .error(R.drawable.default_profile_image)
                                                        .resize(0,90)
                                                        .into(holder.ivPet, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                            }
                                                            @Override
                                                            public void onError(Exception e) {
                                                                Picasso.get().load(R.drawable.default_profile_image)
                                                                        .placeholder(R.drawable.default_profile_image)
                                                                        .error(R.drawable.default_profile_image)
                                                                        .resize(0,90)
                                                                        .into(holder.ivPet);
                                                            }
                                                        });
                                            }
                                            holder.catname.setText(catName);
                                            holder.catage.setText(catAge+"살");
                                            holder.catspec.setText(catSpec);
                                            holder.catsex.setText(catSex);

                                            //View Item 클릭리스너, 클릭 시 CatSetActivity에서 해당 고양이 정보 수정
                                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent setting =new Intent(getActivity(), CatSetActivity.class);
                                                    setting.putExtra("cat_document_id", cat_uid);
                                                    startActivity(setting);
                                                }
                                            });
                                            holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                                                @Override
                                                public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                                                    MenuItem delete = contextMenu.add(Menu.NONE, 1001, 1, "삭제");
                                                    delete.setOnMenuItemClickListener(onDeleteItem);
                                                }
                                                private final MenuItem.OnMenuItemClickListener onDeleteItem = new MenuItem.OnMenuItemClickListener() {
                                                    @Override
                                                    public boolean onMenuItemClick(MenuItem menuItem) {
                                                        Intent intent = new Intent(getContext(), CatDeleteActivity.class);
                                                        intent.putExtra("currentUID", currentUserId);
                                                        intent.putExtra("pickedPID", cat_uid);
                                                        startActivity(intent);
                                                        return true;
                                                    }
                                                };
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public CatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cat_profile, viewGroup, false);

                        return new CatViewHolder(view);
                    }
                };
        catsList.setAdapter(catAdapter);
        catAdapter.startListening();

    }
    //RecyclerView ViewHolder
    public class CatViewHolder extends RecyclerView.ViewHolder{
        CircleImageView ivPet;
        TextView catname,catsex,catspec,catage;

        public CatViewHolder(@NonNull View itemView){
            super(itemView);
            ivPet=itemView.findViewById(R.id.ivCat);
            catname=itemView.findViewById(R.id.tvCName);
            catsex=itemView.findViewById(R.id.tvCSex);
            catspec=itemView.findViewById(R.id.tvCSpe);
            catage=itemView.findViewById(R.id.tvCAge);

        }

    }

}