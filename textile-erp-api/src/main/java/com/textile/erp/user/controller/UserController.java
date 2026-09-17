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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User Management", description = "Endpoints for user provisioning, profiles, status updates, and role assignments")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
            summary = "Provision New User",
            description = "Creates and provisions a new user account. SUPER_ADMIN can create platform users (tenantId = null) or assign users to any tenant. TENANT_ADMIN can only provision users strictly within their own tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User provisioned successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: blank fields, duplicate email, or invalid role"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - caller lacks permission to provision this user"),
            @ApiResponse(responseCode = "404", description = "Specified tenant not found")
    })
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse response = userService.provisionUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.getId())).body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get Current User Profile",
            description = "Retrieves the authenticated user's profile information, assigned roles, and associated tenant context."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT")
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get User by ID",
            description = "Retrieves user details by UUID. Enforces tenant boundary isolation: SUPER_ADMIN can view any user; TENANT_ADMIN can view users within their tenant; EMPLOYEE can only view their own profile."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Unique UUID of the user")
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getUserByIdSecured(id));
    }

    @GetMapping
    @Operation(
            summary = "List Users (Paginated & Filterable)",
            description = "Searches and lists users with pagination. SUPER_ADMIN sees all platform and tenant users; TENANT_ADMIN only sees users belonging to their own tenant. Filterable by name/email search, role, and status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of users returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - standard employees cannot list users")
    })
    public ResponseEntity<Page<UserSummaryResponse>> listUsers(
            @Parameter(description = "Search term matching first name, last name, or email")
            @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Filter by assigned user role (SUPER_ADMIN, TENANT_ADMIN, EMPLOYEE)")
            @RequestParam(name = "role", required = false) RoleName role,
            @Parameter(description = "Filter by user account status (ACTIVE, INACTIVE, SUSPENDED)")
            @RequestParam(name = "status", required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(search, role, status, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update User Profile",
            description = "Updates editable profile fields (first name, last name) for a user. Restricted by tenant boundaries."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - unauthorized to modify this user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Unique UUID of the user")
            @PathVariable("id") UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update User Account Status",
            description = "Changes the account status of a user (ACTIVE, INACTIVE, SUSPENDED). SUPER_ADMIN can update any user; TENANT_ADMIN can update users in their tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: invalid status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - unauthorized to modify status"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "Unique UUID of the user")
            @PathVariable("id") UUID id,
            @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }

    @PostMapping("/{id}/roles")
    @Operation(
            summary = "Assign Role to User",
            description = "Assigns an additional role to the user. SUPER_ADMIN can assign any role; TENANT_ADMIN can assign tenant-scoped roles (TENANT_ADMIN, EMPLOYEE)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: role already assigned or hierarchy violation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - unauthorized to assign this role"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    public ResponseEntity<UserResponse> assignRole(
            @Parameter(description = "Unique UUID of the user")
            @PathVariable("id") UUID id,
            @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userService.assignRole(id, request));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(
            summary = "Remove Role from User",
            description = "Removes an assigned role from a user. Cannot remove the user's only role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: cannot remove last role"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - unauthorized to modify user roles"),
            @ApiResponse(responseCode = "404", description = "User or role assignment not found")
    })
    public ResponseEntity<UserResponse> removeRole(
            @Parameter(description = "Unique UUID of the user")
            @PathVariable("id") UUID id,
            @Parameter(description = "Numeric ID of the role to remove")
            @PathVariable("roleId") Long roleId) {
        return ResponseEntity.ok(userService.removeRole(id, roleId));
    }
}
