package com.textile.erp.user.controller;

import com.textile.erp.user.dto.AssignRoleRequest;
import com.textile.erp.user.dto.CreateUserRequest;
import com.textile.erp.user.dto.UpdateUserRequest;
import com.textile.erp.user.dto.UpdateUserStatusRequest;
import com.textile.erp.user.dto.UserResponse;
import com.textile.erp.user.dto.UserSummaryResponse;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.service.UserService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse response = userService.provisionUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.getId())).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getUserByIdSecured(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> listUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) RoleName role,
            @RequestParam(name = "status", required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(search, role, status, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable("id") UUID id,
            @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable("id") UUID id,
            @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userService.assignRole(id, request));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable("id") UUID id,
            @PathVariable("roleId") Long roleId) {
        return ResponseEntity.ok(userService.removeRole(id, roleId));
    }
}
