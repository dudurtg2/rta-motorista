package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.SOLID.activitys.tracker.LocationTracker;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.services.NetworkService;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.example.rta_app.databinding.ActivityFirstBinding;

public class FirstActivity extends AppCompatActivity {
    private static final String TAG = "FirstActivityaa";

    private TokenStorage tokenStorage;
    private UsersRepository usersRepository;
    private NetworkService networkService;
    private ActivityFirstBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate(): início");

        // edge-to-edge
        EdgeToEdge.enable(this);
        Log.i(TAG, "EdgeToEdge habilitado");

        // inflar layout
        binding = ActivityFirstBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.i(TAG, "Layout definido");

        // inicializar serviços
        tokenStorage   = new TokenStorage(this);
        usersRepository = new UsersRepository(this);
        networkService = new NetworkService();
        Log.i(TAG, "TokenService, TokenStorage, UsersRepo e NetworkService inicializados");

        // checar rede
        boolean connected = networkService.isNetworkConnected(this);
        Log.i(TAG, "isNetworkConnected = " + connected);
        if (!connected) {
            Log.i(TAG, "Sem conexão; redirecionando para WorkHourActivity");
            Toast.makeText(this, "Sem conexão com a internet", Toast.LENGTH_SHORT).show();
            startWorkHour();
            return;
        }

        // validar/refresh token
        Log.i(TAG, "Iniciando validateAndRefreshToken()");

                    String access = tokenStorage.getApiKey();
                    Log.i(TAG, "tokenStorage.getAccessToken() = " + access);
                    if (access == null || access.isEmpty()) {
                        Log.i(TAG, "Access token ausente; redirecionando para LoginActivity");
                        startLogin();
                    } else {
                        Log.i(TAG, "Access token presente; redirecionando para MainActivity");
                        startMain();
                    }



    }

    private void startMain() {
        LocationTracker.start(this);
        Log.i(TAG, "startMain(): abrindo MainActivity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void startLogin() {
        Log.i(TAG, "startLogin(): abrindo LoginActivity e limpando pilha");
        Intent i = new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void startWorkHour() {
        Log.i(TAG, "startWorkHour(): abrindo WorkHourActivity");
        startActivity(new Intent(this, WorkHourActivity.class));
        finish();
    }
}
