package com.textile.erp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRequestDto {

    private String name;
    private String slug;

    // Optional admin credentials for initial TENANT_ADMIN user creation
    private String email;
    private String password;
    private String adminEmail;
    private String adminPassword;
    private String firstName;
    private String lastName;

    public String getEffectiveEmail() {
        if (adminEmail != null && !adminEmail.isBlank()) {
            return adminEmail.trim().toLowerCase();
        }
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        return null;
    }

    public String getEffectivePassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        return password;
    }

    public String getEffectiveFirstName() {
        if (firstName != null && !firstName.isBlank()) {
            return firstName.trim();
        }
        return "Admin";
    }

    public String getEffectiveLastName() {
        if (lastName != null && !lastName.isBlank()) {
            return lastName.trim();
        }
        return null;
    }
}
