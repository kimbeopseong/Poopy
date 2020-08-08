package com.example.poopy.ui.list;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.poopy.R;
import com.example.poopy.utils.Cat;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListCatChoiceAdapter extends FirestorePagingAdapter<Cat, ListCatChoiceAdapter.ViewHolder> {
    private String choiced_pet_uri;
    private ListCatChoiceAdapter.OnListItemClick onListItemClick;

    public ListCatChoiceAdapter(FirestorePagingOptions<Cat> options, ListCatChoiceAdapter.OnListItemClick onListItemClick){
        super(options);
        this.onListItemClick = onListItemClick;
    }

    @NonNull
    @Override
    public ListCatChoiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cat_choice_item, parent, false);
        return new ListCatChoiceAdapter.ViewHolder(view);
    }

    public interface OnListItemClick {
        void onItemClick(DocumentSnapshot snapshot, int position);
    }


    @Override
    protected void onBindViewHolder(@NonNull final ListCatChoiceAdapter.ViewHolder holder, int position, @NonNull Cat model) {

        holder.choiceName.setText(model.getCatName());
        choiced_pet_uri = model.getProfile();
        Picasso.get().load(choiced_pet_uri)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_profile_image)
                .error(R.drawable.default_profile_image)
                .resize(0,70)
                .into(holder.choiceProfile, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(choiced_pet_uri)
                                .placeholder(R.drawable.default_profile_image)
                                .error(R.drawable.default_profile_image)
                                .resize(0,70)
                                .into(holder.choiceProfile);
                    }
                });

    }

    @Override
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        String TAG = "PAGING_LOG";
        super.onLoadingStateChanged(state);
        switch (state){
            case LOADING_INITIAL:
                Log.d(TAG, "Loading Initial Data");
                break;
            case LOADING_MORE:
                Log.d(TAG, "Loading Next Page");
                break;
            case FINISHED:
                Log.d(TAG, "All Data Loaded");
                break;
            case ERROR:
                Log.d(TAG, "Error Loading Data");
                break;
            case LOADED:
                Log.d(TAG, "Total Items Loaded: " + getItemCount());
                break;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View
            .OnClickListener {
        CircleImageView choiceProfile;
        TextView choiceName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            choiceProfile = (CircleImageView) itemView.findViewById(R.id.choice_profile);
            choiceName = (TextView) itemView.findViewById(R.id.choice_name);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onListItemClick.onItemClick(getItem(getAdapterPosition()), getAdapterPosition());
        }
    }
}
