package com.smartlogix.auth.service;

import com.smartlogix.auth.domain.Role;
import com.smartlogix.auth.domain.UserEntity;
import com.smartlogix.auth.domain.UserCoupon;
import com.smartlogix.auth.dto.*;
import com.smartlogix.auth.exception.AuthException;
import com.smartlogix.auth.repository.UserRepository;
import com.smartlogix.auth.repository.UserCouponRepository;
import com.smartlogix.auth.security.JwtProvider;
import com.smartlogix.auth.strategy.AuthStrategyResolver;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthStrategyResolver strategyResolver;
    private final UserCouponRepository couponRepository;
    private final String welcomeCouponCode;
    private final int welcomeCouponPercent;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider, AuthStrategyResolver strategyResolver,
                       UserCouponRepository couponRepository,
                       @Value("${smartlogix.welcome-coupon-code:BIENVENIDA10}") String welcomeCouponCode,
                       @Value("${smartlogix.welcome-coupon-percent:10}") int welcomeCouponPercent) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.strategyResolver = strategyResolver;
        this.couponRepository = couponRepository;
        this.welcomeCouponCode = welcomeCouponCode.trim().toUpperCase(Locale.ROOT);
        this.welcomeCouponPercent = welcomeCouponPercent;
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new AuthException("El correo ya está registrado.");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(clean(request.firstName()));
        user.setLastName(clean(request.lastName()));
        user.setEmail(email);
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        UserEntity saved = repository.saveAndFlush(user);
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(saved.getId());
        coupon.setCode(welcomeCouponCode);
        coupon.setDiscountPercentage(welcomeCouponPercent);
        coupon.setUsed(false);
        couponRepository.save(coupon);

        return new RegisterResponse(saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getEmail(),
                user.getRole().name(), welcomeCouponCode,
                "Cuenta creada correctamente. Recibiste tu cupón de bienvenida del " + welcomeCouponPercent + "%.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            UserEntity user = strategyResolver.resolve(request.credential())
                    .authenticate(request.credential(), request.password());
            String token = jwtProvider.generateToken(user);
            return toAuthResponse(user, token);
        } catch (RuntimeException ex) {
            throw new AuthException("Correo o contraseña incorrectos.");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse validateToken(String token) {
        if (token == null || !jwtProvider.validateToken(token)) {
            throw new AuthException("Token inválido o expirado.");
        }
        String email = jwtProvider.parseClaims(token).getSubject();
        UserEntity user = loadByEmail(email);
        return toAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String email) {
        return toDto(loadByEmail(email));
    }

    public UserDTO updateCurrentUser(String currentEmail, ProfileUpdateRequest request) {
        UserEntity user = loadByEmail(currentEmail);
        String newEmail = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(newEmail, user.getId())) {
            throw new AuthException("El correo ya está registrado por otra cuenta.");
        }
        user.setFirstName(clean(request.firstName()));
        user.setLastName(clean(request.lastName()));
        user.setEmail(newEmail);
        user.setUsername(newEmail);
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        return toDto(repository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserCountResponse countUsers() {
        long total = repository.count();
        long clients = repository.findAll().stream().filter(u -> u.getRole() == Role.ROLE_USER).count();
        return new UserCountResponse(total, clients);
    }

    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = repository.findById(id)
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new AuthException("El correo ya está registrado por otra cuenta.");
        }
        user.setFirstName(clean(request.firstName()));
        user.setLastName(clean(request.lastName()));
        user.setEmail(email);
        user.setUsername(email);
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.role()));
            } catch (IllegalArgumentException ex) {
                throw new AuthException("Rol inválido.");
            }
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return toDto(repository.save(user));
    }

    public void deleteUser(Long id, String currentEmail) {
        UserEntity user = repository.findById(id)
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new AuthException("No puedes eliminar tu propia cuenta administradora.");
        }
        repository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserCouponResponse couponStatus(Long userId, String code) {
        String normalized = normalizeCoupon(code);
        UserCoupon coupon = couponRepository.findByUserIdAndCodeIgnoreCase(userId, normalized)
                .orElseThrow(() -> new AuthException("El usuario no tiene asignado el cupón solicitado."));
        boolean available = !coupon.isUsed();
        return toCouponResponse(coupon, available
                ? "Cupón disponible para una compra."
                : "El cupón ya fue utilizado.");
    }

    public UserCouponResponse consumeCoupon(Long userId, String code, String orderNumber) {
        UserCoupon coupon = couponRepository.findForUpdate(userId, normalizeCoupon(code))
                .orElseThrow(() -> new AuthException("El usuario no tiene asignado el cupón solicitado."));
        if (coupon.isUsed()) {
            throw new AuthException("El cupón " + coupon.getCode() + " ya fue utilizado por este usuario.");
        }
        coupon.setUsed(true);
        coupon.setUsedAt(OffsetDateTime.now());
        coupon.setOrderNumber(orderNumber.trim().toUpperCase(Locale.ROOT));
        UserCoupon saved = couponRepository.save(coupon);
        return toCouponResponse(saved, "Cupón utilizado correctamente.");
    }

    public UserCouponResponse releaseCoupon(Long userId, String code, String orderNumber) {
        UserCoupon coupon = couponRepository.findForUpdate(userId, normalizeCoupon(code))
                .orElseThrow(() -> new AuthException("El usuario no tiene asignado el cupón solicitado."));
        if (coupon.isUsed() && coupon.getOrderNumber() != null
                && coupon.getOrderNumber().equalsIgnoreCase(orderNumber)) {
            coupon.setUsed(false);
            coupon.setUsedAt(null);
            coupon.setOrderNumber(null);
            couponRepository.save(coupon);
        }
        return toCouponResponse(coupon, "Estado del cupón actualizado.");
    }

    private UserCouponResponse toCouponResponse(UserCoupon coupon, String message) {
        return new UserCouponResponse(coupon.getCode(), coupon.getDiscountPercentage(), !coupon.isUsed(),
                coupon.isUsed(), coupon.getIssuedAt(), coupon.getUsedAt(), coupon.getOrderNumber(), message);
    }

    private String normalizeCoupon(String code) {
        if (code == null || code.isBlank()) throw new AuthException("Debe indicar un código de cupón.");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private UserEntity loadByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));
    }

    private AuthResponse toAuthResponse(UserEntity user, String token) {
        return new AuthResponse(token, "Bearer", user.getId(), user.getFirstName(), user.getLastName(),
                user.getUsername(), user.getEmail(), user.getRole().name(), jwtProvider.getExpirationMs());
    }

    private UserDTO toDto(UserEntity user) {
        return new UserDTO(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                user.getEmail(), user.getRole().name(), user.isEnabled(), user.getCreatedAt());
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(); }
    private String clean(String value) { return value.trim().replaceAll("\\s+", " "); }
}
