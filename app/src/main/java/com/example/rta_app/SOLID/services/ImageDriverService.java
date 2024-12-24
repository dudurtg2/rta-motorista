package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.activitys.RTADetailsActivity;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.entities.PackingList;

import java.io.IOException;

public class ImageDriverService {

    private final Context context;
    private final String codigodeficha;
    private IPackingListRepository packingListRepository;


    public ImageDriverService(Context context, String codigodeficha) {
        this.context = context;
        this.codigodeficha = codigodeficha;
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

    private void uploadFile(Bitmap bitmap) {
        packingListRepository.updateImgLinkForFinish(bitmap, codigodeficha).addOnSuccessListener(aVoid -> {
            Log.i("RTAAPITEST", "Imagem enviada com sucesso");
                });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

}
