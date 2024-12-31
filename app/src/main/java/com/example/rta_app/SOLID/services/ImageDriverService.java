package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import android.media.ExifInterface;

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

    private Bitmap correctImageOrientation(Uri photoUri, Bitmap bitmap) throws IOException {
        ExifInterface exif = new ExifInterface(context.getContentResolver().openInputStream(photoUri));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270);
            default:
                return bitmap; // Não precisa corrigir
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void handleCameraResult(Uri photoUri, RTADetailsActivity rtaDetailsActivity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(rtaDetailsActivity.getContentResolver(), photoUri);
            // Corrigir a orientação da imagem
            Bitmap correctedBitmap = correctImageOrientation(photoUri, bitmap);
            // Redimensionar a imagem
            Bitmap resizedBitmap = resizeBitmap(correctedBitmap, 768, 1024);
            // Enviar a imagem
            uploadFile(resizedBitmap);
        } catch (IOException e) {
            Toast.makeText(context, "Falha ao carregar a imagem", Toast.LENGTH_SHORT).show();
            Log.e("ImageDriverService", "Erro ao processar a imagem", e);
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
