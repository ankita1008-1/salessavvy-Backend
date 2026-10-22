package com.example.learn.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.learn.entity.User;
public interface UserRepository  extends JpaRepository<User, Integer> {
	Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

}
