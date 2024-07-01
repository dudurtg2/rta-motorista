package com.example.lc_app.Fuctions.DAO.User;

import android.content.Context;
import android.widget.Toast;

import com.example.lc_app.Activitys.MainActivity;
import com.example.lc_app.Fuctions.DTO.UserDTO;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserDAO {
    private final FirebaseFirestore db;
    private Context context;
    public UserDAO(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> addUser(String name, String uid) {
        UserDTO userDTO = new UserDTO(name, uid);

        Map<String, Object> user = new HashMap<>();
        user.put("nome", userDTO.getName());
        user.put("uid", userDTO.getUid());

        return db.collection("usuarios").document(uid).update(user);
    }
    public Task<Void> updateWorkHours(String uid, String date, String workHours, String nome) {
        DocumentReference docRef = db.collection("usuarios").document(uid).collection("work_hours").document(date);

        return docRef.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.contains(nome)) {
                    Toast.makeText(context, "Vc ja bateu esse ponto hoje", Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> workHoursMap = new HashMap<>();
                    workHoursMap.put(nome, workHours);

                    return docRef.set(workHoursMap, SetOptions.merge());
                }
            } else {
                throw task.getException();
            }
            return null;
        });
    }
}
