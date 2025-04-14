package com.example.tea_leaves_project.payload.Request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String fullname;
    private String password;
    private long role_id;

}
