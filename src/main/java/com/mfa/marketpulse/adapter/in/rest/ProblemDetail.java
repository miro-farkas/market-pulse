package com.mfa.marketpulse.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.util.List;

/**
 * RFC 7807 problem response body, sent as {@code application/problem+json}.
 *
 * @param violations invalid request fields; only present for validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        URI type, String title, int status, String detail, URI instance, List<Violation> violations) {

    public static final String MEDIA_TYPE = "application/problem+json";

    static ProblemDetail of(int status, String title, String detail, URI instance) {
        return new ProblemDetail(URI.create("about:blank"), title, status, detail, instance, null);
    }

    /** One invalid field: where it is ({@code field}) and what is wrong with it. */
    public record Violation(String field, String message) {}
}
