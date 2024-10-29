package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.rta_app.SOLID.activitys.RTADetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;


public class ImageDriverService {
    private final Context context;
    private final FirebaseUser currentUser;
    private final String hour;
    private final FirebaseFirestore firestore;

    public ImageDriverService(Context context, String hour) {
        this.context = context;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        this.currentUser = mAuth.getCurrentUser();
        this.hour = hour;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void handleCameraResult(Uri photoUri, RTADetailsActivity rtaDetailsActivity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(rtaDetailsActivity.getContentResolver(), photoUri);
            Bitmap resizedBitmap = resizeBitmap(bitmap, 768, 1024);
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
                        String location = documentSnapshot.getString("Local") == null ? documentSnapshot.getString("local") : documentSnapshot.getString("Local");
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
                new GoogleDriveService(context, hour, location).uploadBitmap(bitmap);
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
