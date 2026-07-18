package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String fullName;
    private String email;
    private String phone;
    private String bio;
    private String photoUrl;
}