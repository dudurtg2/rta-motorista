package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.repository.QueryRTA;
import com.example.rta_app.SOLID.repository.UsersRepository;
import com.example.rta_app.SOLID.Views.AdapterViewRTA;
import com.example.rta_app.R;
import com.example.rta_app.SOLID.repository.RTArepository;
import com.example.rta_app.databinding.ActivityInTravelBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.util.List;


public class InTravelActivity extends AppCompatActivity {
    public ActivityInTravelBinding binding;
    private String filter = "Todas as cidades";
    private IUsersRepository usersRepository;

    public InTravelActivity(){
        this.usersRepository = new UsersRepository();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_in_travel);
        binding = ActivityInTravelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBinding();
        queryItems(filter);
        queryFilter();
        getUser();
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

    private void setupBinding() {

        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(InTravelActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.atualizar.setOnClickListener(v -> queryItems(filter));
        binding.buttonFinaliza.setOnClickListener(v ->  new RTArepository(this).removeFromTraver().addOnCompleteListener(queryItems(filter)) );
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    new RTArepository(this).confirmDocExist(binding.RTAprocura.getText().toString().toUpperCase());
                    return true;
                }
            }
            return false;
        });
    }

    private void queryFilter() {
        new RTArepository(this).readDataLocate(listRTA -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(InTravelActivity.this, android.R.layout.simple_spinner_dropdown_item, listRTA);
            binding.DishesCategorySpinner.setAdapter(adapter);
        });
        binding.DishesCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter = (String) parent.getItemAtPosition(position);
                queryItems((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public OnCompleteListener<List<Task<?>>> queryItems(String filter) {
        QueryRTA queryRTATravel = new QueryRTA(this);
        queryRTATravel.readDataInTravel(dishesDTO -> {
            int itemCount = dishesDTO.size();

            binding.QtdRTA.setText("QTD: " + String.valueOf(itemCount));

            binding.listRTATravelview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            binding.listRTATravelview.setAdapter(new AdapterViewRTA(1, getApplicationContext(), dishesDTO));
        }, filter);
        return null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_LONG).show();
            } else {
                new RTArepository(this).confirmDocExist(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}