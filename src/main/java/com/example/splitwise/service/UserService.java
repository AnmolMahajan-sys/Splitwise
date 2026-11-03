package com.example.splitwise.service;

import com.example.splitwise.dto.CreateUserRequest;
import com.example.splitwise.dto.UserResponse;
import com.example.splitwise.entity.User;
import com.example.splitwise.exception.EmailAlreadyExistsException;
import com.example.splitwise.exception.InvalidExpenseException;
import com.example.splitwise.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request){
        if(request==null) throw new InvalidExpenseException("User request cannot be null.");
        if(request.getName()==null || request.getName().trim().isEmpty()) throw new InvalidExpenseException("Name cannot be empty.");
        if(request.getEmail()==null || request.getEmail().trim().isEmpty()) throw new InvalidExpenseException("Email cannot be empty.");

        String name = request.getName().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User(name, email);

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }
}
