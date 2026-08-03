package com.interviewai.common.constants;

public final class AppConstants {

    public static final String API_BASE_PATH = "/api/v1";
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final String MAX_SIZE = "100";
    public static final String SORT_DEFAULT = "createdAt:desc";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_RECRUITER = "RECRUITER";
    public static final String ROLE_CANDIDATE = "CANDIDATE";

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String TOKEN_CLAIM_ROLES = "roles";
    public static final String TOKEN_CLAIM_TYPE = "type";
    public static final String TOKEN_CLAIM_EMAIL = "email";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_VERIFY = "verify";
    public static final String TOKEN_TYPE_RESET = "reset";

    private AppConstants() {
    }
}
