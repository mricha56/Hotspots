package com.teamhotspots.hotspots;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseIndexListAdapter;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Kathleen on 4/7/2017.
 */

public class Feed extends Fragment {
    private ListView postsListView;
    private Post itemSelected;
    private DatabaseReference mReference;

    private ValueEventListener hotspotValueListener;
    private ChildEventListener postsChildEventListener;
    private FirebaseListAdapter<Post> adapter;
    private String userID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed,
                container, false);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Feed");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) this.userID = user.getUid();
        else this.userID = "lnOu8CBcUKQKl3q9HoLr3nGsG532";  // TODO: remove this once login page is working. For now, use John's

        mReference = FirebaseDatabase.getInstance().getReference();
        postsListView = (ListView) view.findViewById(R.id.feed_list);
        //for now we won't worry about populating this
        //TextView community = (TextView) view.findViewById(R.id.community_name);
        //community.setText("Johns Hopkins University");


        String hotspotKey = getArguments().getString("hotspotKey");

        // TODO: Populate local feed with local pins
        // For now, we just display the feed for example-hotspot
        if (hotspotKey.equals("local")) {
            hotspotKey = "example-hotspot";
        }

        hotspotValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Hotspot h = dataSnapshot.getValue(Hotspot.class);
                // Handle hotspot update, e.g. title change
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        mReference.child("hotspots").child(hotspotKey).addValueEventListener(hotspotValueListener);


        adapter = new FirebaseIndexListAdapter<Post>(getActivity(), Post.class,
                R.layout.post, mReference.child("hotspots/" + hotspotKey + "/posts"),
                mReference.child("posts"))
        {
            @Override
            protected void populateView(View v, Post p, int position) {
                String key = getRef(position).getKey();

                TextView username = (TextView) v.findViewById(R.id.username);
                ImageView picture = (ImageView) v.findViewById(R.id.picture);
                ImageView icon = (ImageView) v.findViewById(R.id.user_icon);
                TextView message = (TextView) v.findViewById(R.id.message);
                TextView likes = (TextView) v.findViewById(R.id.likes);
                if (username != null) {
                    username.setText(p.getUsername());
                }

                if (p.getUsername().equals(getString(R.string.anonymous))) {
                    icon.setImageResource(R.drawable.ic_person_outline_black_24dp);
                } else {
                    //may need to format size
                    Picasso.with(getContext()).load(p.getUsericon()).into(icon);
                }

                if (picture != null && p.isPicturePost()) {
                    Picasso.with(getContext()).load(p.getImageUrl()).into(picture);
                    picture.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams params = picture.getLayoutParams();
                    params.height = dpToPx(getActivity().getApplicationContext(), 200);
                } else if (picture!= null && !p.isPicturePost()) {
                    picture.setVisibility(View.GONE);
                    picture.setBackgroundResource(0);
                    ViewGroup.LayoutParams params = picture.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }

                if (message != null) {
                    message.setText(p.getMsg());
                }

                if (likes != null) {
                    likes.setText("" + p.getNumLikes());
                }

                ImageView thumbIcon = (ImageView) v.findViewById(R.id.like_icon);
                ThumbIconOnClickListener listener = new ThumbIconOnClickListener(p, thumbIcon, likes, getRef(position));
                thumbIcon.setOnClickListener(listener);
            }
        };


        postsChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                adapter.notifyDataSetChanged();
            }
        };
        postsListView.setAdapter(adapter);
        mReference.child("posts").addChildEventListener(postsChildEventListener);

        registerForContextMenu(postsListView);
        postsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Post p = (Post) postsListView.getItemAtPosition(position);
                itemSelected = p;
                if (p.isPicturePost()) postsListView.showContextMenu();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Remove Firebase ValueEventListeners, or else the fragment will continue listening
        // after being detached from the activity.
        //mReference.child("posts").removeEventListener(postsValueListener);
        mReference.child("posts").removeEventListener(postsChildEventListener);
        mReference.child("hotspots").removeEventListener(hotspotValueListener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Select The Action");
        menu.add(0, v.getId(), 0, "Save to Gallery"); // groupId, itemId, order, title
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        if (item.getTitle()=="Save to Gallery") {
            //need to get image bitmap, need to pull image from entry
            Bitmap b = getBitmapFromURL(itemSelected.getImageUrl());
            MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), b, null, null);
            Toast.makeText(getActivity().getApplicationContext(),"Saved to gallery!",Toast.LENGTH_LONG).show();
        } else {
            return false;
        }
        return true;
    }


    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    private class ThumbIconOnClickListener implements View.OnClickListener {
        private Post post;
        private TextView likes;
        private ImageView thumbIcon;
        private boolean pressed = false;
        private DatabaseReference ref;
        private FirebaseListAdapter<Post> adapter;

        public ThumbIconOnClickListener(Post post, ImageView thumbIcon, TextView likes,
                                        DatabaseReference ref) {
            this.post = post;
            this.thumbIcon = thumbIcon;
            this.likes = likes;
            this.ref = ref;

            if (post.inLikedBy(userID)) {
                this.pressed = true;
                thumbIcon.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
            } else {
                this.pressed = false;
                thumbIcon.clearColorFilter();
            }
        }

        public void setClicked() {
            this.pressed = true;
            thumbIcon.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
        }

        @Override
        public void onClick(View view) {
            String key = ref.getKey();
            if (!pressed) {
                pressed = true;
                post.upvote();
                thumbIcon.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                post.addToLikedBy(userID);
            } else {
                pressed = false;
                post.undoVote();
                thumbIcon.clearColorFilter();
                post.removeFromLikedBy(userID);
            }

            likes.setText("" + post.getNumLikes());

            ref.child("numLikes").setValue(post.getNumLikes());
            ref.child("likedBy").setValue(post.getLikedBy());
        }
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


}
