package com.example.splitwise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Name is Required")
    @Size(max=100, message = "Name <=100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide valid email")
    @Size(max=255, message = "Email must not exceed 255 characters")
    private String email;

    public CreateUserRequest() {}

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

}
