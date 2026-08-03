package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.CompanyMember;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateCompanyRequest;
import com.interviewai.dto.response.CompanyResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.AccessDeniedException;
import com.interviewai.repository.CompanyMemberRepository;
import com.interviewai.service.CompanyService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Company profiles and membership")
public class CompanyController {

    private final CompanyService companyService;
    private final UserService userService;
    private final CompanyMemberRepository companyMemberRepository;

    @PostMapping
    @Operation(summary = "Create a company (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
            @Valid @RequestBody CreateCompanyRequest request, Principal principal) {
        User owner = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Company created", companyService.create(request, owner)));
    }

    @GetMapping
    @Operation(summary = "List companies with search")
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        return ResponseEntity.ok(ApiResponse.success(companyService.list(search, page, size, sort)));
    }

    @GetMapping("/mine")
    @Operation(summary = "List companies the current user belongs to")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> mine(Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(companyService.mine(user.getId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company by id")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update company branding (member/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CreateCompanyRequest request, Principal principal) {
        requireMember(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Company updated", companyService.updateBranding(id, request)));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to the company (owner/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<MessageResponse>> addMember(
            @PathVariable Long id, @RequestParam Long userId,
            @RequestParam(required = false) String roleInCompany, Principal principal) {
        requireOwner(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(companyService.addMember(id, userId, roleInCompany)));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List company members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> members(@PathVariable Long id) {
        List<Map<String, Object>> members = companyService.membersOf(id).stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "userId", m.getUser().getId(),
                        "name", m.getUser().getFirstName() + " " + m.getUser().getLastName(),
                        "role", m.getRoleInCompany(),
                        "owner", m.isOwner()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    private void requireMember(Long companyId, String email) {
        User user = userService.currentUser(email);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN"));
        boolean isMember = companyMemberRepository.existsByCompanyIdAndUserId(companyId, user.getId());
        if (!isAdmin && !isMember) {
            throw new AccessDeniedException("You are not a member of this company");
        }
    }

    private void requireOwner(Long companyId, String email) {
        User user = userService.currentUser(email);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN"));
        boolean isOwner = companyMemberRepository.findByCompanyIdAndUserId(companyId, user.getId())
                .map(CompanyMember::isOwner).orElse(false);
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Only the company owner can perform this action");
        }
    }
}
