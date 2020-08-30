package com.example.poopy.ui.list;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.poopy.R;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.DocumentSnapshot;


public class RecycleAdapter extends FirestorePagingAdapter<ListItem, RecycleAdapter.ViewHolder> {

    private OnListItemClick onListItemClick;
    private OnMenuItemClick onMenuItemClick;

    public RecycleAdapter(FirestorePagingOptions<ListItem> options, OnListItemClick onListItemClick, OnMenuItemClick onMenuItemClick){
        super(options);
        this.onListItemClick = onListItemClick;
        this.onMenuItemClick = onMenuItemClick;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        private TextView date, stat, lv;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            date = itemView.findViewById(R.id.daily_date);
            stat = itemView.findViewById(R.id.daily_stat);
            lv = itemView.findViewById(R.id.daily_lv);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View view) {
            onListItemClick.onItemClick(getItem(getAdapterPosition()), getAdapterPosition());
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            MenuItem delete = contextMenu.add(Menu.NONE, 1001, 1, "삭제");
            delete.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            onMenuItemClick.onMenuItemClick(getItem(getAdapterPosition()), getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
            notifyItemRangeChanged(getAdapterPosition(), getItemCount());
            return true;
        }
    }

    public interface OnListItemClick {
        void onItemClick(DocumentSnapshot snapshot, int position);
    }

    public interface OnMenuItemClick {
        boolean onMenuItemClick(DocumentSnapshot snapshot, int position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecycleAdapter.ViewHolder holder, int position, ListItem model) {
        holder.date.setText(model.getDate());
        holder.stat.setText(model.getStat());
        holder.lv.setText(model.getLv());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        String TAG = "PAGING_LOG";
        super.onLoadingStateChanged(state);
        switch (state){
            case LOADING_INITIAL:
                Log.d(TAG, "Loading Initial Data" );
                break;
            case LOADING_MORE:
                Log.d(TAG, "Loading Next Page" );
                break;
            case FINISHED:
                Log.d(TAG, "All Data Loaded" );
                break;
            case ERROR:
                Log.d(TAG, "Error Loading Data" );
                break;
            case LOADED:
                Log.d(TAG, "Total Items Loaded: " + getItemCount());
                break;
        }
    }
}
