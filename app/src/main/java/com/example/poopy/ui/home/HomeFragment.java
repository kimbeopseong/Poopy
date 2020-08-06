package com.example.poopy.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.poopy.Cat.CatSetActivity;
import com.example.poopy.R;
import com.example.poopy.Cat.AddCatActivity;
import com.example.poopy.utils.Cat;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private View privateCatView;
    private RecyclerView catsList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ImageView addCat;

    String petUri,petName,petSex,petAge,petSpec;

    public HomeFragment(){ }

    public static HomeFragment newInstance(){
        return new HomeFragment();
    }

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
        return privateCatView;
    }

    @Override
    public void onStart(){
        super.onStart();
        FirestoreRecyclerOptions<Cat> options = new FirestoreRecyclerOptions.Builder<Cat>()
                .setQuery(db.collection("Pet").whereEqualTo("p_ID",currentUserId), Cat.class).build();

        FirestoreRecyclerAdapter<Cat, CatViewHolder> catAdapter=
                new FirestoreRecyclerAdapter<Cat, CatViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final CatViewHolder holder, int position, @NonNull Cat model) {
                        final String cat_uid=getSnapshots().getSnapshot(position).getId();
                        DocumentReference docRef=getSnapshots().getSnapshot(position).getReference();
                        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                db.collection("Pet").document(cat_uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()){
                                            petName=task.getResult().get("p_name").toString();
                                            petSex=task.getResult().get("p_sex").toString();
                                            petAge=task.getResult().get("p_age").toString();
                                            petSpec=task.getResult().get("p_species").toString();
                                            if(task.getResult().contains("p_uri")){
                                                petUri=task.getResult().get("p_uri").toString();
                                                Picasso.get().load(petUri)
                                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                                        .placeholder(R.drawable.default_profile_image)
                                                        .error(R.drawable.default_profile_image)
                                                        .resize(0,90)
                                                        .into(holder.ivPet, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                Picasso.get().load(petUri)
                                                                        .placeholder(R.drawable.default_profile_image)
                                                                        .error(R.drawable.default_profile_image)
                                                                        .resize(0,90)
                                                                        .into(holder.ivPet);
                                                            }
                                                        });
                                            }
                                            holder.petname.setText(petName);
                                            holder.petage.setText(petAge+"살");
                                            holder.petspec.setText(petSpec);
                                            holder.petsex.setText(petSex);

                                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent setting =new Intent(getActivity(), CatSetActivity.class);
                                                    setting.putExtra("cat_document_id", cat_uid);
                                                    startActivity(setting);
                                                }
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
    public static class CatViewHolder extends RecyclerView.ViewHolder{
        CircleImageView ivPet;
        TextView petname,petsex,petspec,petage;

        public CatViewHolder(@NonNull View itemView){
            super(itemView);
            ivPet=itemView.findViewById(R.id.ivCat);
            petname=itemView.findViewById(R.id.tvCName);
            petsex=itemView.findViewById(R.id.tvCSex);
            petspec=itemView.findViewById(R.id.tvCSpe);
            petage=itemView.findViewById(R.id.tvCAge);
        }
    }
}