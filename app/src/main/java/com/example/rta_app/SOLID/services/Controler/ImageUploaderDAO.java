package com.example.rta_app.SOLID.services.Controler;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.rta_app.SOLID.activitys.WorkHourActivity;
import com.example.rta_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUploaderDAO {
    private final Context context;
    private final StorageReference storageReference;
    private final FirebaseUser currentUser;
    private final String hour;

    public ImageUploaderDAO(Context context, String hour) {
        this.context = context;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        this.currentUser = mAuth.getCurrentUser();
        this.storageReference = FirebaseStorage.getInstance().getReference().child("work_hours").child(currentUser.getUid()).child(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        this.hour = hour;
    }

    public void handleCameraResult(Uri photoUri, WorkHourActivity workHourActivity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(workHourActivity.getContentResolver(), photoUri);
            Bitmap resizedBitmap = resizeBitmap(bitmap, 256, 256);
            uploadFile(resizedBitmap);
        } catch (IOException e) {
            Toast.makeText(context, "Falha ao carregar a imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFile(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference fileReference = storageReference.child(hour + ".png");

            fileReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        Toast.makeText(context, "Upload bem-sucedido", Toast.LENGTH_SHORT).show();
                        loadImagem();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Falha no upload: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(context, "Nenhum arquivo selecionado", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadImagem() {
        if (currentUser != null) {
            StorageReference gsReference = storageReference.child(hour + ".png");

            gsReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (context instanceof WorkHourActivity) {
                            WorkHourActivity activity = (WorkHourActivity) context;
                            switch (hour) {
                                case "Entrada":
                                    Picasso.get().load(uri).into(activity.binding.imageFistHour);
                                    break;
                                case "Almoço":
                                    Picasso.get().load(uri).into(activity.binding.imageDinnerStarHour);
                                    break;
                                case "Saída":
                                    Picasso.get().load(uri).into(activity.binding.imageDinnerFinishHour);
                                    break;
                                case "Fim":
                                    Picasso.get().load(uri).into(activity.binding.imageStop);
                                    break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (context instanceof WorkHourActivity) {
                            WorkHourActivity activity = (WorkHourActivity) context;
                            switch (hour) {
                                case "Entrada":
                                    activity.binding.imageFistHour.setImageResource(R.drawable.chegada);
                                    break;
                                case "Almoço":
                                    activity.binding.imageDinnerStarHour.setImageResource(R.drawable.ialmouco);
                                    break;
                                case "Saída":
                                    activity.binding.imageDinnerFinishHour.setImageResource(R.drawable.ialmouco);
                                    break;
                                case "Fim":
                                    activity.binding.imageStop.setImageResource(R.drawable.fim);
                                    break;
                            }
                        }
                    });
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}
