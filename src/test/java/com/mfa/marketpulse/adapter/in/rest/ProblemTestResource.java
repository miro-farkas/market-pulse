package com.mfa.marketpulse.adapter.in.rest;

import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;

/** Test-only endpoints that fail in each way {@link ProblemExceptionMappers} handles. */
@Path("/test/problems")
@Produces(MediaType.APPLICATION_JSON)
public class ProblemTestResource {

    public record Order(@NotBlank String symbol, @Positive BigDecimal quantity) {}

    @POST
    @Path("/validated")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Order> validated(@Valid Order order) {
        return Uni.createFrom().item(order);
    }

    @GET
    @Path("/query")
    public Uni<Integer> query(@QueryParam("limit") @Max(100) int limit) {
        return Uni.createFrom().item(limit);
    }

    @GET
    @Path("/invalid-response")
    @Valid public Order invalidResponse() {
        return new Order("", BigDecimal.ONE);
    }

    @GET
    @Path("/missing")
    public Uni<String> missing() {
        return Uni.createFrom().failure(new NotFoundException("Portfolio 42 not found"));
    }

    @GET
    @Path("/boom")
    public Uni<String> boom() {
        return Uni.createFrom().failure(new IllegalStateException("secret internal detail"));
    }
}
