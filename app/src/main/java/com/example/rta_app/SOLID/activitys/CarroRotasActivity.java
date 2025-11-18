package com.example.rta_app.SOLID.activitys;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityCarroRotasBinding;
import com.example.rta_app.databinding.ActivityFirstBinding;
import com.example.rta_app.databinding.ActivityInTravelBinding;

public class CarroRotasActivity extends AppCompatActivity {

    private ActivityCarroRotasBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCarroRotasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }



}