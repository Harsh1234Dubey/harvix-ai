package com.interviewai.service;

import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Company;
import com.interviewai.domain.CompanyMember;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateCompanyRequest;
import com.interviewai.dto.response.CompanyResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.DuplicateResourceException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.CompanyMemberRepository;
import com.interviewai.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, User owner) {
        String slug = slugify(request.name());
        if (companyRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A company with a similar name already exists");
        }
        Company company = new Company();
        company.setName(request.name());
        company.setSlug(slug);
        company.setDescription(request.description());
        company.setLogoUrl(request.logoUrl());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setLocation(request.location());
        company.setSizeRange(request.sizeRange());
        company.setFoundedYear(request.foundedYear());
        company.setBrandingColor(request.brandingColor());
        company.setCreatedBy(owner);
        companyRepository.save(company);

        CompanyMember member = new CompanyMember();
        member.setCompany(company);
        member.setUser(owner);
        member.setOwner(true);
        member.setRoleInCompany("Owner");
        companyMemberRepository.save(member);
        return CompanyResponse.from(company);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(String search, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Company> companies = search != null && !search.isBlank()
                ? companyRepository.findByNameContainingIgnoreCase(search, pageable)
                : companyRepository.findAll(pageable);
        return PageResponse.from(companies, companies.stream().map(CompanyResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return CompanyResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> mine(Long userId) {
        return companyMemberRepository.findByUserId(userId).stream()
                .map(CompanyMember::getCompany)
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public CompanyResponse updateBranding(Long id, CreateCompanyRequest request) {
        Company company = findById(id);
        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name());
            company.setSlug(slugify(request.name()));
        }
        if (request.description() != null) company.setDescription(request.description());
        if (request.logoUrl() != null) company.setLogoUrl(request.logoUrl());
        if (request.website() != null) company.setWebsite(request.website());
        if (request.industry() != null) company.setIndustry(request.industry());
        if (request.location() != null) company.setLocation(request.location());
        if (request.sizeRange() != null) company.setSizeRange(request.sizeRange());
        if (request.foundedYear() != null) company.setFoundedYear(request.foundedYear());
        if (request.brandingColor() != null) company.setBrandingColor(request.brandingColor());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public List<CompanyMember> membersOf(Long companyId) {
        return companyMemberRepository.findByCompanyId(companyId);
    }

    @Transactional
    public MessageResponse addMember(Long companyId, Long userId, String roleInCompany) {
        Company company = findById(companyId);
        if (companyMemberRepository.existsByCompanyIdAndUserId(companyId, userId)) {
            throw new DuplicateResourceException("User is already a member of this company");
        }
        CompanyMember member = new CompanyMember();
        member.setCompany(company);
        User user = new User();
        user.setId(userId);
        member.setUser(user);
        member.setRoleInCompany(roleInCompany);
        companyMemberRepository.save(member);
        return MessageResponse.of("Member added to company");
    }

    @Transactional(readOnly = true)
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", id));
    }

    public static String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isEmpty() ? "company-" + System.currentTimeMillis() : normalized;
    }
}
