package com.example.firebasevideoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class VideosActivity extends AppCompatActivity {

    FloatingActionButton addVideosBtn;
    private RecyclerView videoRv;

    //array list
    private ArrayList<ModelVideo> videoArrayList;

    //adapter
    private AdapterVideo adapterVideo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);

        setTitle("Videos");

        addVideosBtn=findViewById(R.id.addVideosBtn);
        videoRv=findViewById(R.id.videosRv);

        //fucntion call, load vibes
        loadVideosFromFirebase();

        addVideosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(VideosActivity.this,AddVideoActivity.class));

            }
        });
    }

    private void loadVideosFromFirebase() {
        //init arraylist befoe adding data into it
        videoArrayList  =new ArrayList<>();

        //db reference
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Videos");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {
                //clear list before adding data into it
                videoArrayList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    ModelVideo modelVideo=ds.getValue(ModelVideo.class);

                    // add model/ data into list
                    videoArrayList.add(modelVideo);
                }
                //setup adapter
                adapterVideo =new AdapterVideo(VideosActivity.this,videoArrayList);
                //set adapter to recyclerview

                videoRv.setAdapter(adapterVideo);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
