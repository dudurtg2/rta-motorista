package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.services.NetworkService;
import com.example.rta_app.SOLID.services.TokenService;
import com.example.rta_app.databinding.ActivityFirstBinding;
import com.example.rta_app.databinding.ActivityLoginBinding;

public class FirstActivity extends AppCompatActivity {
    private TokenService tokenService;
    private Integer id;
    private IUsersRepository usersRepository;
    private NetworkService networkService;
    private ActivityFirstBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFirstBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tokenService = new TokenService(this);

        usersRepository = new UsersRepository(this);
        if (networkService.isNetworkConnected(this)) {
            tokenService.validateAndRefreshToken().addOnSuccessListener(i -> {
                startActivity(new Intent(FirstActivity.this, MainActivity.class));
                finish();
            }).addOnFailureListener(v -> {
                startActivity(new Intent(FirstActivity.this, LoginActivity.class));
                finish();
            });
        } else {

            Toast.makeText(this, "Sem conexão com a internet", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(FirstActivity.this, WorkHourActivity.class));
                finish();



        }
    }
}