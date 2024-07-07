package com.example.rta_app.Fuctions.DAO.User;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.example.rta_app.Activitys.User.ProfileActivity;
import com.example.rta_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUploaderDAO {
  private final Context context;
  private final StorageReference storageReference;
  private final FirebaseUser currentUser;

  public ImageUploaderDAO(Context context) {
    this.context = context;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    this.currentUser = mAuth.getCurrentUser();
    this.storageReference = FirebaseStorage.getInstance().getReference().child("profile_images").child(currentUser.getUid());
  }

  public void openFileChooser(ProfileActivity profileActivity) {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    profileActivity.startActivityForResult(intent, profileActivity.PICK_IMAGE_REQUEST);
  }

  public void handleImageResult(int requestCode, int resultCode, @Nullable Intent data, ProfileActivity profileActivity) {
    if (requestCode == profileActivity.PICK_IMAGE_REQUEST && resultCode == profileActivity.RESULT_OK && data != null && data.getData() != null) {
      Uri imageUri = data.getData();
      try {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(profileActivity.getContentResolver(), imageUri);
        Bitmap resizedBitmap = resizeBitmap(bitmap, 256, 256);
        uploadFile(resizedBitmap);
      } catch (IOException e) {
        Toast.makeText(context, "Falha ao carregar a imagem", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void uploadFile(Bitmap bitmap) {
    if (bitmap != null) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
      byte[] data = baos.toByteArray();

      StorageReference fileReference = storageReference.child("profile_images").child("profile.png");

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
      StorageReference gsReference = storageReference.child("profile_images").child("profile.png");
      gsReference.getDownloadUrl().addOnSuccessListener(uri -> { Picasso.get().load(uri).into(((ProfileActivity) context).binding.UserImagenView); }).addOnFailureListener(exception -> { ((ProfileActivity) context).binding.UserImagenView.setImageResource(R.drawable.baseimageforuser); });
    }
  }

  private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
    return Bitmap.createScaledBitmap(bitmap, width, height, true);
  }
}