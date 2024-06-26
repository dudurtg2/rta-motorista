package com.example.lc_app.Activitys;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lc_app.Activitys.User.Controler.WorkHourActivity;
import com.example.lc_app.Activitys.User.ProfileActivity;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    private DocumentReference docRef;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getUser();


        binding.buttonProfileUser.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
    }
    private void getUser() {
        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.UserNameDisplay.setText(documentSnapshot.getString("nome"));
                    } else {
                        binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> { Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show(); });
    }

    

}
