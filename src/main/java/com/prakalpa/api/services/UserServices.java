package com.prakalpa.api.services;

import com.prakalpa.api.repository.UserRepository;
import com.prakalpa.api.interfacedto.UserInfo;
import com.prakalpa.api.models.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServices implements UserInfo {

    @Autowired
    private UserRepository userRepository;


    @Override
    public void create(Users users) {
        userRepository.save(users);
    }

    @Override
    public Users getUsers(String name) {
        Users users = null;
        Optional<Users> users1 = userRepository.findByUsername(name);
        if(users1.isPresent()){
            users = users1.get();
        }
        return users;
    }

    @Override
    public List<Users> getUserList() {
        return userRepository.findAll();
    }


    public Optional<Users> findOne(Long id){
        return userRepository.findById(id);
    }

}
