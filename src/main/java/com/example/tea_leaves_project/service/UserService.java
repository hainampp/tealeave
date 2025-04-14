package com.example.tea_leaves_project.service;

import com.example.tea_leaves_project.dto.PackageDto;
import com.example.tea_leaves_project.dto.UserInfoDto;
import com.example.tea_leaves_project.payload.ResponseData;

import java.util.List;

public interface UserService {
    UserInfoDto getCurrentUserIfo(String email);
    List<PackageDto> getAllPackage(String email);
    ResponseData deletePackage(String email, long packageId);
}
