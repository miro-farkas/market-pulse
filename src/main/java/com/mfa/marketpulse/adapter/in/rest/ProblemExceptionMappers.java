package com.mfa.marketpulse.adapter.in.rest;

import io.quarkus.logging.Log;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Maps every error to an RFC 7807 {@code application/problem+json} response. Stack traces never leave the server. */
public class ProblemExceptionMappers {

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, UriInfo uriInfo) {
        if (isReturnValueViolation(e)) {
            // The server produced an invalid response: a bug, not a client error.
            return unexpected(e, uriInfo);
        }
        List<ProblemDetail.Violation> violations = e.getConstraintViolations().stream()
                .map(v -> new ProblemDetail.Violation(fieldOf(v.getPropertyPath()), v.getMessage()))
                .sorted(Comparator.comparing(ProblemDetail.Violation::field))
                .toList();
        var problem = new ProblemDetail(
                URI.create("about:blank"),
                "Bad Request",
                400,
                "Request validation failed",
                instance(uriInfo),
                violations);
        return problem(problem);
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uriInfo) {
        int status = e.getResponse().getStatus();
        Response.Status known = Response.Status.fromStatusCode(status);
        String title = known != null ? known.getReasonPhrase() : "HTTP " + status;
        var problem = ProblemDetail.of(status, title, e.getMessage(), instance(uriInfo));
        // fromResponse keeps headers such as Allow (405) or Retry-After (503).
        return Response.fromResponse(e.getResponse())
                .entity(problem)
                .type(ProblemDetail.MEDIA_TYPE)
                .build();
    }

    @ServerExceptionMapper
    public Response unexpected(Exception e, UriInfo uriInfo) {
        String errorId = UUID.randomUUID().toString();
        Log.errorf(e, "Unhandled error %s on %s", errorId, uriInfo.getPath());
        var problem = ProblemDetail.of(
                500, "Internal Server Error", "Unexpected error, reference " + errorId, instance(uriInfo));
        return problem(problem);
    }

    private static Response problem(ProblemDetail problem) {
        return Response.status(problem.status())
                .entity(problem)
                .type(ProblemDetail.MEDIA_TYPE)
                .build();
    }

    private static URI instance(UriInfo uriInfo) {
        return URI.create(uriInfo.getPath());
    }

    private static boolean isReturnValueViolation(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .flatMap(path -> StreamSupport.stream(path.spliterator(), false))
                .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE);
    }

    /** {@code create.arg0.name} → {@code name}: drops the method and parameter nodes, keeps the bean path. */
    private static String fieldOf(Path path) {
        List<Path.Node> nodes = StreamSupport.stream(path.spliterator(), false).toList();
        List<Path.Node> beanPath = nodes.stream()
                .filter(n -> n.getKind() == ElementKind.PROPERTY || n.getKind() == ElementKind.CONTAINER_ELEMENT)
                .toList();
        if (beanPath.isEmpty()) {
            // Constraint directly on a parameter (e.g. @QueryParam): use the parameter name.
            return nodes.isEmpty() ? "" : nodes.getLast().getName();
        }
        return beanPath.stream().map(Path.Node::toString).collect(Collectors.joining("."));
    }
}
