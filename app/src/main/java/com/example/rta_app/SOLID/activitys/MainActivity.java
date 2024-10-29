package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.repository.PackingListRepository;
import com.example.rta_app.SOLID.repository.UsersRepository;
import com.example.rta_app.SOLID.Views.AdapterViewRTA;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityMainBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    private IUsersRepository usersRepository;
    private IPackingListRepository packingListRepository;


    public MainActivity(){
        this.packingListRepository = new PackingListRepository();
        this.usersRepository = new UsersRepository();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getUser();
        SetupBinding();
        queryItems();
    }

    private void getUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                    binding.UserNameDisplay.setText(users.getName());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao obter usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void SetupBinding(){
        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    packingListRepository.getPackingListToBase(binding.RTAprocura.getText().toString().toUpperCase()).addOnSuccessListener(packingList -> {
                        Toast.makeText(this,packingList.getCodigodeficha(),Toast.LENGTH_SHORT).show();
                        packingListRepository.movePackingListForDelivery(packingList);
                    }).addOnFailureListener(va -> Toast.makeText(this, binding.RTAprocura.getText().toString().toUpperCase() + " não encontrado", Toast.LENGTH_SHORT).show());
                    return true;
                }
            }
            return false;
        });
        binding.UserNameDisplay.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
        binding.inTravelbutton.setOnClickListener(v -> startActivity(new Intent(this, InTravelActivity.class)));

        binding.atualizar.setOnClickListener(v -> queryItems());
    }

    public void queryItems() {
        packingListRepository.getListPackingListToDirect().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<PackingList> packingList = task.getResult();
                binding.listRTAview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listRTAview.setAdapter(new AdapterViewRTA(0, this, packingList));
            } else {
                Log.d("Firestore", "Erro ao obter packing list: " + task.getException().getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_LONG).show();
            } else {
                packingListRepository.getPackingListToBase(result.getContents()).addOnSuccessListener(packingList -> {
                    Toast.makeText(this,packingList.getCodigodeficha(),Toast.LENGTH_SHORT).show();
                    packingListRepository.movePackingListForDelivery(packingList);
                }).addOnFailureListener(v -> Toast.makeText(this, result.getContents() + " não encontrado", Toast.LENGTH_SHORT).show());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}
