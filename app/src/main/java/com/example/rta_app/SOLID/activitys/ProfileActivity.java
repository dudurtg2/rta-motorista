package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.entities.Users;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityProfileBinding;

import java.io.File;

public class ProfileActivity extends AppCompatActivity {

    public ActivityProfileBinding binding;

    public static final int PICK_IMAGE_REQUEST = 1;
    private IUsersRepository usersRepository;
    private static final String FILE_NAME = "user_data.json";

    public ProfileActivity() {
        usersRepository = new UsersRepository(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SetupClickListeners();
    }

    private void SetupClickListeners() {
        binding.profileUserInsert.setVisibility(View.GONE);

        binding.singOut.setOnClickListener(v -> {
            eraseToFile();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.editNameUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.profileUserInsert.setVisibility(View.VISIBLE);
                } else {
                    binding.profileUserInsert.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        getUser();

        binding.profileUserInsert.setOnClickListener(v -> updateUser());
    }

    private void eraseToFile() {
        try {
            File file = getFileStreamPath(FILE_NAME);
            if (file.exists()) {
                boolean isDeleted = this.deleteFile(FILE_NAME);
                if (isDeleted) {
                    Log.d("Arquivo", "Arquivo deletado com sucesso: " + FILE_NAME);
                } else {
                    Log.e("Arquivo", "Erro ao deletar o arquivo.");
                }
            } else {
                Log.d("Arquivo", "Arquivo não existe: " + FILE_NAME);
            }
        } catch (Exception e){
            Log.e("Arquivo", "Erro ao deletar o arquivo.", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void getUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                    binding.editNameUser.setHint(users.getName());
                    binding.editTelefoneUser.setHint(users.getTelefone());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }

    private void updateUser() {

        usersRepository.saveUser(new Users(binding.editNameUser.getText().toString(),usersRepository.getUser().getResult().getUid(), usersRepository.getUser().getResult().getTelefone(), usersRepository.getUser().getResult().getBase(),usersRepository.getUser().getResult().getBaseid())).addOnSuccessListener(aVoid1 -> {
            Toast.makeText(this, "Usuário atualizado com sucesso", Toast.LENGTH_SHORT).show();
            binding.profileUserInsert.setVisibility(View.GONE);
            binding.editNameUser.setText("");
            getUser();
        })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar o usuário", Toast.LENGTH_SHORT).show());

    }

}
