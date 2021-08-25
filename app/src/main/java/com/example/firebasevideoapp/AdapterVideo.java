package com.example.firebasevideoapp;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class AdapterVideo extends RecyclerView.Adapter<AdapterVideo.HolderVideo> {
    //context
    private Context context;
    //arrayylist
    private ArrayList<ModelVideo> videoArrayList;

    //constructor

    public AdapterVideo(Context context, ArrayList<ModelVideo> videoArrayList) {
        this.context = context;
        this.videoArrayList = videoArrayList;
    }

    @NonNull
    @Override
    public HolderVideo onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        //inflate layout row_video.xml
        View view= LayoutInflater.from(context).inflate(R.layout.row_video,parent,false);
        return new HolderVideo(view) ;
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterVideo.HolderVideo holder, int position) {
        // Get, format, set data, handle click etc

        //get data
        ModelVideo modelVideo=videoArrayList.get(position);

        String id=modelVideo.getId();
        String title=modelVideo.getTitle();
        String timestamp=modelVideo.getTimestamp();
        String videoUrl=modelVideo.getVideoUrl();

        //formating the timestamp
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String formattedDateTime = DateFormat.format("dd/MM/yyyy k:mm a",calendar).toString();

        //set data
        holder.timeTv.setText(formattedDateTime);
        holder.titleTv.setText(title);
        setVideoUrl(modelVideo,holder);

        //handle click, download video
        holder.downloadFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downlloadVideo(modelVideo);

            }
        });

        //handle click delete video
        holder.deleteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show alert dialogconfirm to delete
                AlertDialog.Builder builder =new AlertDialog.Builder(context);
                builder.setTitle("Delete")
                        .setMessage("Are you sure you want to delete video"+ title)
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // confirm to delete
                                deleteVideo(modelVideo);
                            }
                        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                      // cancel deleting, dismiss dialog
                        dialogInterface.dismiss();
                    }
                }).show();


            }
        });
    }

    private void setVideoUrl(ModelVideo modelVideo, HolderVideo holder) {

        //show progress
        holder.progressBar.setVisibility(View.VISIBLE);

        //get video url
        String videoUrl = modelVideo.getVideoUrl();

        //Media controller for play, pause, seekbar, timer etc.
        MediaController mediaController=new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri =Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // video is ready to play
                mediaPlayer.start();
            }
        });

        holder.videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                // to check if buffering, randering etc
                switch(what){
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    {
                        //redering start
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:{
                        //buffering start
                        holder.progressBar.setVisibility(View.VISIBLE);
                        if(MediaPlayer.MEDIA_INFO_BUFFERING_END== 702){
                            holder.progressBar.setVisibility(View.GONE);
                        }
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:{
                        //buffering end
                        holder.progressBar.setVisibility(View.GONE);
                        return true;
                    }

                }
                return false;
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.start(); //restart if video is completed
            }
        });


    }

    private void deleteVideo(ModelVideo modelVideo) {
        // used to delete the file
        final String videoID=modelVideo.getId();
        String videoUrl=modelVideo.getVideoUrl();

        // delete from firebase
        StorageReference reference= FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        reference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //deleted from firebase storage

                        //delete from firebase database
                        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference("Videos");
                        databaseReference.child(videoID)
                                .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                // Successfully deleted from firebase database
                                Toast.makeText(context, "Successfully Deleted", Toast.LENGTH_SHORT).show();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull  Exception e) {
                                // failed in deleteing from storage
                                Toast.makeText(context, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                });
    }

    private void downlloadVideo(ModelVideo modelVideo) {
        final String videoUrl =modelVideo.getVideoUrl();

        //get video reference using video url
        StorageReference storageReference=FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        storageReference.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get file/video basic info e.g tile, type
                        String filenName= storageMetadata.getName();
                        String fileType=storageMetadata.getContentType();
                        String fileDirectory= Environment.DIRECTORY_DOWNLOADS; // video will be save to this folder
                        //init download manager
                        DownloadManager downloadManager=(DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

                        //get uri of file to be download
                        Uri uri =Uri.parse(videoUrl);

                        //create download request, now request for each download - yes we can download multiple file at a time
                        DownloadManager.Request request=new DownloadManager.Request(uri);

                        //notification visibility
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(""+fileDirectory,".mp4");

                        //add request to queue - cana be multiple request so is added  to queue
                        downloadManager.enqueue(request);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // failing in downloading
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }


    public void onBindViewHolder(@NonNull  AdapterVideo holder, int position) {

    }

    @Override
    public int getItemCount() {
        return videoArrayList.size(); //return size of list
    }

    //view holder class, holds,inits the UI views
    class HolderVideo extends  RecyclerView.ViewHolder{

        //Ui Views of row_video.xml
        VideoView videoView;
        TextView titleTv, timeTv;
        ProgressBar progressBar;
        FloatingActionButton deleteFab, downloadFab;

        //constructor


        public HolderVideo(@NonNull  View itemView) {
            super(itemView);

            //id to them of row_video layout
            videoView = itemView.findViewById(R.id.videoView);
            titleTv = itemView.findViewById(R.id.titleTv);
            timeTv =itemView.findViewById(R.id.timeTv);
            progressBar=itemView.findViewById(R.id.progressBar);
            deleteFab=itemView.findViewById(R.id.deleteFab);
            downloadFab=itemView.findViewById(R.id.downloadFab);
        }
    }
}
