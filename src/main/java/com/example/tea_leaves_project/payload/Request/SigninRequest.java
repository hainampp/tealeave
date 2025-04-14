package com.example.tea_leaves_project.payload.Request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninRequest {
    private String email;
    private String password;
}
