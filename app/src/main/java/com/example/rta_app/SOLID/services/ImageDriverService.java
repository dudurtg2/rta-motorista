package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.activitys.RTADetailsActivity;
import com.example.rta_app.SOLID.api.PackingListRepository;
import java.io.IOException;

public class ImageDriverService {

    private final Context context;
    private final String hour;
    private IPackingListRepository packingListRepository;


    public ImageDriverService(Context context, String hour) {
        this.context = context;
        this.hour = hour;
        this.packingListRepository = new PackingListRepository(context);
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
        packingListRepository.getPackingListToRota(uid)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.getCodigodeficha().isEmpty()) {
                        String location = documentSnapshot.getLocal();
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
