package dev.careertrack;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Local-only app: reject cross-origin browser requests and require a non-simple header on writes. */
@Component
public class LocalSafetyFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
            boolean write = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
            if ("cross-site".equals(request.getHeader("Sec-Fetch-Site")) || (write && !"CareerTrack".equals(request.getHeader("X-Requested-With")))) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Use the local CareerTrack app or include X-Requested-With: CareerTrack.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
