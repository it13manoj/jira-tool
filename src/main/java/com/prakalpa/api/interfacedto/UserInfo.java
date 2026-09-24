package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Users;

import java.util.List;

public interface UserInfo {
    void create(Users users);

    Users getUsers(String name);

    List<Users> getUserList();

}
