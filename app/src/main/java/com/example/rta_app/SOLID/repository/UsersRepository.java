package com.example.rta_app.SOLID.repository;


import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.entities.Users;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class UsersRepository implements IUsersRepository {
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;


    public UsersRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public Task<Users> getUser() {

        Task<DocumentSnapshot> task = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .get();

        return task.continueWith(taskSnapshot -> {
            if (!taskSnapshot.isSuccessful()) {
                throw taskSnapshot.getException();
            }

            DocumentSnapshot document = taskSnapshot.getResult();

            if (document == null || !document.exists()) {
                return new Users("", "");
            }

            String name = document.getString("nome") == null ? "Sem registro" : document.getString("nome");
            String uid = document.getString("uid") == null ? mAuth.getCurrentUser().getUid() : document.getString("uid");


            return new Users(name, uid);
        });
    }


    public Task<Void> saveUser(Users users) {
        DocumentReference docRef = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid());

        Map<String, Object> user = new HashMap<>();
        user.put("nome", users.getName());
        user.put("uid", users.getUid());

        return docRef.update(user);
    }



}
