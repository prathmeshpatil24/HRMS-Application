package com.hrms.auth.repository;

import com.hrms.auth.entity.LoginActivity;
import com.hrms.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findByUserOrderByLoggedInAtDesc(User user);
}
