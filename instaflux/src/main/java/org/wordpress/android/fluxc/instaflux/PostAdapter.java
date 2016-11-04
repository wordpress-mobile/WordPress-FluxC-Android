package org.wordpress.android.fluxc.instaflux;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.fluxc.model.PostModel;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<PostModel> postList;
    private Context mContext;

    public PostAdapter(Context context, List<PostModel> postList) {
        this.postList = postList;
        this.mContext = context;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post_list_row, null);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        PostModel postModel = postList.get(position);

        // render image
    }

    @Override
    public int getItemCount() {
        return (postList != null ? postList.size() : 0);
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        PostViewHolder(View view) {
            super(view);
            this.imageView = (ImageView) view.findViewById(R.id.image_view);
        }
    }
}
