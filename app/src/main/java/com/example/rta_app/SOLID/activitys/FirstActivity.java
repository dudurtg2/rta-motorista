package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.SOLID.activitys.tracker.LocationTracker;
import com.example.rta_app.SOLID.services.NetworkService;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.example.rta_app.databinding.ActivityFirstBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstActivity extends AppCompatActivity {
    private static final String TAG = "FirstActivity";

    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor();

    private ActivityFirstBinding binding;
    private TokenStorage tokenStorage;
    private NetworkService networkService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFirstBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenStorage = new TokenStorage(getApplicationContext());
        networkService = new NetworkService();

        TokenStorage.warmUpAsync(getApplicationContext());
        decideNextScreen();
    }

    private void decideNextScreen() {
        boolean connected = networkService.isNetworkConnected(this);
        Log.i(TAG, "isNetworkConnected=" + connected);

        if (!connected) {
            Toast.makeText(this, "Sem conexão com a internet", Toast.LENGTH_SHORT).show();
            startWorkHour();
            return;
        }

        startupExecutor.execute(() -> {
            String apiKey = tokenStorage.getApiKey();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (apiKey.isEmpty()) {
                    Log.i(TAG, "Token ausente; abrindo LoginActivity");
                    startLogin();
                } else {
                    Log.i(TAG, "Token presente; iniciando rastreamento e abrindo MainActivity");
                    startMain();
                }
            });
        });
    }

    private void startMain() {
        LocationTracker.start(getApplicationContext());
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startWorkHour() {
        Intent intent = new Intent(this, WorkHourActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startupExecutor.shutdownNow();
    }
}
