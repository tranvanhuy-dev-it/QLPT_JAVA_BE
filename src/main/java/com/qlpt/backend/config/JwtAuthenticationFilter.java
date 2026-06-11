package com.qlpt.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.qlpt.backend.entity.User;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private com.qlpt.backend.repository.UserSessionRepository userSessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                if (!userSessionRepository.existsByTokenAndActiveTrue(jwt)) {
                    response.setStatus(401); // 401 Unauthorized
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Phiên làm việc đã bị thu hồi hoặc đăng xuất từ xa!\"}");
                    return;
                }
                String username = tokenProvider.getUsernameFromJWT(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (userDetails instanceof CustomUserDetails) {
                    User user = ((CustomUserDetails) userDetails).getUser();
                    String requestURI = request.getRequestURI();
                    
                    boolean isExcluded = requestURI.startsWith("/api/auth/") 
                                      || requestURI.startsWith("/api/subscriptions/")
                                      || requestURI.equals("/api/users/profile")
                                      || requestURI.equals("/api/users/change-password")
                                      || requestURI.startsWith("/api/notifications")
                                      || user.getRole() == com.qlpt.backend.enums.Role.ADMIN;
                                      
                    if (!isExcluded) {
                        User checkUser = user.getRole() == com.qlpt.backend.enums.Role.TENANT ? user.getLandlord() : user;
                        if (checkUser != null) {
                            java.time.LocalDate now = java.time.LocalDate.now();
                            java.time.LocalDateTime regDate = checkUser.getCreatedAt() != null ? checkUser.getCreatedAt() : java.time.LocalDateTime.now().minusDays(50);
                            
                            boolean trialActive = regDate.plusDays(45).isAfter(java.time.LocalDateTime.now());
                            boolean subscriptionActive = checkUser.getSubscriptionExpiredAt() != null && !checkUser.getSubscriptionExpiredAt().isBefore(now);
                            
                            if (!trialActive && !subscriptionActive) {
                                response.setStatus(402); // 402 Payment Required
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("{\"error\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Gói dịch vụ đã hết hạn. Vui lòng thanh toán gia hạn để tiếp tục!\"}");
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Không thể thiết lập xác thực người dùng trong bộ lọc bảo mật", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
