package com.example.rta_app.Fuctions.DAO.Driver;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.rta_app.Activitys.User.Controler.RTADetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUploaderDAO {
    private final Context context;
    private final StorageReference storageReference;
    private final FirebaseUser currentUser;
    private final String hour;
    private final FirebaseFirestore firestore;

    public ImageUploaderDAO(Context context, String hour) {
        this.context = context;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        this.currentUser = mAuth.getCurrentUser();
        this.storageReference = FirebaseStorage.getInstance().getReference().child("work_hours").child(currentUser.getUid()).child(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        this.hour = hour;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void handleCameraResult(Uri photoUri, RTADetailsActivity rtaDetailsActivity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(rtaDetailsActivity.getContentResolver(), photoUri);
            Bitmap resizedBitmap = resizeBitmap(bitmap, 720, 1280);
            uploadFile(resizedBitmap);
        } catch (IOException e) {
            Toast.makeText(context, "Falha ao carregar a imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void getRTA(String uid, GetRTACallback callback) {
        firestore.collection("rota")
                .document(currentUser.getUid())
                .collection("pacotes")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String location = documentSnapshot.getString("Local");
                        callback.onSuccess(location);
                    } else {
                        callback.onFailure("Document does not exist");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void uploadFile(Bitmap bitmap) {
        getRTA(hour, new GetRTACallback() {
            @Override
            public void onSuccess(String location) {
                new GoogleDriveUploader(context, hour, location).uploadBitmap(bitmap);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(context, "Failed to get RTA: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private interface GetRTACallback {
        void onSuccess(String location);
        void onFailure(String error);
    }
}
