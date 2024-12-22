package com.example.rta_app.SOLID.Interfaces;

import com.example.rta_app.SOLID.entities.Users;
import com.google.android.gms.tasks.Task;

public interface IUsersRepository {

    Task<Users> getUser();

    Task<Void> saveUser(Users user);

    public Task<Void> loginUser(String nome, String senha);
}
