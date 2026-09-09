package com.textile.erp.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class LoginRequestDto {

    @JsonAlias({"username", "user"})
    private String email;

    private String password;

    public void setUsername(String username) {
        if (this.email == null || this.email.isBlank()) {
            this.email = username;
        }
    }

    public String getUsername() {
        return this.email;
    }
}
