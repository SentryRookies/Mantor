package com.jiny.jwtauth.svc;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.jiny.jwtauth.dto.CreateTokenDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${sumits.encryption.jwk-private:MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALVfMfMinOBoTcy+A58AG8iVFp/pa8/n7drRU8qwq7+BcGTPArNEuYgBEscPTKNWnWREb1FzD6yoMOo0q6rC30H0TkT1V+IwSqzXYzq87yhoDlrip8wBdfOkUu4au+qETQHLYa2wcsrbn40Nk0+DxEAM6nIWj6NDKNA3hGSPiO8LAgMBAAECgYA0b3AebbxS+HS9o5Wia0KdC9U9qBs+QTw3zdret5L/y6k1y89pCo5k7oKCQD8U3d6k9VAFiTFX5kw1+cJDnRE0IG5y4yDnIdMO/ZRlxPmSLhOHdPZplXvDUyV4lkuMWqxhiEJVry3KYNVG/kEINMHHqnK/4jBH4E/qnkwVDF6roQJBAMy0ZDr/2D7D64Rc5TlYpL15Qmra15u4ydwEhViBLIiM9gaKryv/7e+/RSCiODzZWxb8nRNRCEGsbf1H9qsY2LECQQDi0gbc2tNOoXTGFRVwI/Z6LNIzwlPNeXzBy+uC7UXL9sqRYHWuRgLdzUlL6bGpLCYKt6kWiVzwp0v4q13SOnJ7AkBWBJt1m1qwDT3jUfHUpiYbpF+/bRRa0Eyqko/CAA3Jl4oud4pib9n4+6O9scz17NP1FdOfcVaJ2j4hx2KkxP0RAkEAkXFrxSLsbHdLkdjjLk1hN2aWQ2pQomlMflhsZEARiYBu1FmuHFn4bJG+dlcb/Qa6PhUW78SVPBuKs6HvP2cLEQJAd4KyL9M7fUVtf94S6R39L1B8xMwPnfhAdKSS0KPP1zrMgq9Fvb12cFPb2YaDqXq+f+wA7ZsvKeynRZWjj0jVEw==}")
    private String jwkPrivate;

    @Value("${sumits.encryption.jwk-uri:http://localhost:8080/auth/jwks}")
    private String jwksUri;

    @Value("${redis.use:0}")
    int redisUse;

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;

    final RedisTemplate<String, Object> refreshTokenTemplate;

    public JwtService(RedisTemplate<String, Object> refreshTokenTemplate) {
        this.refreshTokenTemplate = refreshTokenTemplate;
    }

    public void testKeyGen() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);

        KeyPair kp = keyGenerator.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();

        //String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        //logger.info("Public Key: " + encodedPublicKey);

        String encodedPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        logger.info("Private Key: " + encodedPrivateKey);
        //!!! Private Key는 yaml파일의 skinfosec.encryption.jwk-private에 설정해 준다.

        JWK jwk = new RSAKey.Builder((RSAPublicKey)publicKey)
                .privateKey((RSAPrivateKey)privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("sumits_platform_auth_key")
                .build();

        //logger.info(jwk.toJSONString());
        logger.info("{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}");
        //!!! Public Key는 JwksUri를 통해 MSA서비스들이 사용할 수 있도록 공개한다.
    }

    public void setPrivateKey() throws Exception {
        byte[] encoded = Base64.getDecoder().decode(jwkPrivate);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        privateKey = keyFactory.generatePrivate(keySpec);

        //String encodedPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        //logger.info("Private Key: " + encodedPrivateKey);
    }

    public Map<String, Object> getMapFromJsonObject(JSONObject jsonObj){
        Map<String, Object> map = new HashMap<>();
        try {
            map = new ObjectMapper().readValue(jsonObj.toString(), new TypeReference<>() {
            });
        } catch (Exception e) {
            logger.error(e.toString());
        }

        return map;
    }

    public String createAccessToken(CreateTokenDto dto) {

        // Header 설정
        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "JWT");
        headers.put("alg", "RS256");
        headers.put("kid", "sumits_platform_auth_key");

        // Payload 설정
        JSONObject jsonObject = new JSONObject(dto.getPayload());
        Map<String, Object> payloads = getMapFromJsonObject( jsonObject );
        payloads.put("clientIp", dto.getClientIp());

        // 유효시간 지정(분단위)
        long expiredTime = 1000 * 60L * dto.getExpired();
        Date ext = new Date();
        ext.setTime(ext.getTime() + expiredTime);

        String jwt = "";
        try {
            jwt = Jwts.builder().setSubject("user-auth")
                    .setHeader(headers)
                    .setClaims(payloads)
                    .setExpiration(ext)
                    // RS256 with privateKey
                    .signWith(SignatureAlgorithm.RS256, privateKey).compact();
        } catch(Exception e) {
            logger.error(e.toString());
        }

        return jwt;
    }

    public String createRefreshToken(CreateTokenDto dto) {

        // Header 설정
        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "JWT");
        headers.put("alg", "RS256");
        headers.put("kid", "sumits_platform_auth_key");

        // Payload 설정
        String tokenId = UUID.randomUUID().toString();
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("tokenId", tokenId);
        payloads.put("clientIp", dto.getClientIp());

        // 유효시간 지정(24시간)
        long expiredTime = 1000 * 60L * 60L * 24L;
        Date ext = new Date();
        ext.setTime(ext.getTime() + expiredTime);

        String jwt = "";
        try {
            jwt = Jwts.builder().setSubject("user-auth")
                    .setHeader(headers)
                    .setClaims(payloads)
                    .setExpiration(ext)
                    // RS256 with privateKey
                    .signWith(SignatureAlgorithm.RS256, privateKey).compact();
        } catch(Exception e) {
            logger.error(e.toString());
        }

        if(redisUse !=0) {
            // Redis에 Key를 저장한다.
            HashOperations<String, Object, Object> hashOperations = refreshTokenTemplate.opsForHash();
            Map<String, Object> map = new HashMap<>();
            map.put("refreshToken", jwt);
            hashOperations.putAll(tokenId, map);
        }

        return jwt;
    }

    public Map<String, Object> verifyAccessToken(String authorization) throws Exception {

        Map<String, Object> claimMap;

        DecodedJWT jwt = JWT.decode(authorization);
        JwkProvider provider = new UrlJwkProvider(new URL(jwksUri));

        logger.info( new String(Base64.getDecoder().decode(jwt.getHeader()), StandardCharsets.UTF_8) );
        logger.info( new String(Base64.getDecoder().decode(jwt.getPayload()), StandardCharsets.UTF_8) );
        //logger.info(jwt.getKeyId());

        Jwk jwk = provider.get(jwt.getKeyId());

        //String encodedPublicKey = Base64.getEncoder().encodeToString(jwk.getPublicKey().getEncoded());
        //logger.info("Public Key: " + encodedPublicKey);

        //Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        //algorithm.verify(jwt);

        //logger.info("Issuer: " + jwt.getIssuer());
        //logger.info("Subject: " +jwt.getSubject());

        claimMap = Jwts.parser()
                .setSigningKey(jwk.getPublicKey())
                .parseClaimsJws(authorization)
                .getBody();

        return claimMap;
    }

    public Map<String, Object> verifyRefreshToken(String authorization) throws Exception {

        Map<String, Object> claimMap = null;

        DecodedJWT jwt = JWT.decode(authorization);
        JwkProvider provider = new UrlJwkProvider(new URL(jwksUri));

        logger.info( new String(Base64.getDecoder().decode(jwt.getHeader()), StandardCharsets.UTF_8) );
        logger.info( new String(Base64.getDecoder().decode(jwt.getPayload()), StandardCharsets.UTF_8) );
        //logger.info(jwt.getKeyId());

        Jwk jwk = provider.get(jwt.getKeyId());

        claimMap = Jwts.parser()
                .setSigningKey(jwk.getPublicKey())
                .parseClaimsJws(authorization)
                .getBody();

        return claimMap;
    }

}
