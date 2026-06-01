package com.diplom.api.service;

import com.diplom.api.dto.AuthResponse;
import com.diplom.api.dto.LoginRequest;
import com.diplom.api.dto.RegisterRequest;
import com.diplom.persistence.entity.ProducerEntity;
import com.diplom.persistence.repository.ProducerRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

/**
 * Сервис аутентификации и авторизации производителей.
 * <p>
 * Обеспечивает регистрацию нового производителя, вход по email и паролю,
 * хеширование паролей (SHA-256), генерацию и валидацию JWT-токенов.
 */
@Service
public class AuthService {

    private final ProducerRepository producerRepository;
    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    /**
     * @param producerRepository репозиторий для работы с таблицей producers
     * @param jwtSecret          секретный ключ для подписи JWT
     * @param jwtExpirationMs    срок жизни токена в миллисекундах
     */
    public AuthService(ProducerRepository producerRepository,
                       @Value("${app.jwt.secret}") String jwtSecret,
                       @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        this.producerRepository = producerRepository;
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Регистрация нового производителя.
     *
     * @param request данные регистрации (название, контакты, пароль)
     * @return ответ с токеном, идентификатором и названием организации
     * @throws RuntimeException если email уже занят
     */
    public AuthResponse register(RegisterRequest request) {
        if (producerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        ProducerEntity producer = ProducerEntity.builder()
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(hashPassword(request.getPassword()))
                .isActive(true)
                .build();
        producer = producerRepository.save(producer);

        String token = generateToken(producer.getId(), producer.getEmail());
        return AuthResponse.builder()
                .token(token)
                .producerId(producer.getId())
                .name(producer.getName())
                .build();
    }

    /**
     * Вход производителя по email и паролю.
     *
     * @param request email и пароль
     * @return ответ с токеном, идентификатором и названием организации
     * @throws RuntimeException если учётные данные неверны
     */
    public AuthResponse login(LoginRequest request) {
        ProducerEntity producer = producerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!verifyPassword(request.getPassword(), producer.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        String token = generateToken(producer.getId(), producer.getEmail());
        return AuthResponse.builder()
                .token(token)
                .producerId(producer.getId())
                .name(producer.getName())
                .build();
    }

    // Простое хеширование
    private String hashPassword(String rawPassword) {
        // SHA-256
        try {
            return Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(rawPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equals(hashedPassword);
    }

    private String generateToken(Integer producerId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("producerId", producerId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * Проверяет JWT-токен и возвращает идентификатор производителя.
     *
     * @param token строка JWT-токена
     * @return идентификатор производителя, записанный в claim "producerId"
     * @throws RuntimeException если токен невалиден
     */
    public Integer validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("producerId", Integer.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }
}