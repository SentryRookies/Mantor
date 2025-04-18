package com.jiny.jwtauth.ctl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jiny.jwtauth.dto.CommonResponse;
import com.jiny.jwtauth.dto.CreateTokenDto;
import com.jiny.jwtauth.dto.RefreshTokenDto;
import com.jiny.jwtauth.dto.TokenVO;
import com.jiny.jwtauth.svc.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class JwtController {

    @Autowired
    JwtService jwtService;

    @Autowired
    RedisTemplate<String, Object> refreshTokenTemplate;

    @Value("${redis.use:0}")
    int redisUse;

    private static final Logger logger = LoggerFactory.getLogger(JwtController.class);

    @RequestMapping(value = "/create/token", method = {RequestMethod.POST},  produces = "application/json")
    public @ResponseBody CommonResponse createJwtToken(@RequestBody CreateTokenDto dto) {
        CommonResponse result = null;

        try {
            // private/public key 생성
            // jwtService.testKeyGen();

            // 기존 생성된 privatekey를 설정한다.
            jwtService.setPrivateKey();

            // expired 값이 0이하인 경우 default 값 설정
            if(dto.getExpired() <= 0)
                dto.setExpired(30L);

            // 생성된 Token을 result에 설정한다.
            TokenVO tokenVO = new TokenVO();
            tokenVO.setAccessToken(jwtService.createAccessToken(dto));
            tokenVO.setRefreshToken(jwtService.createRefreshToken(dto));
            result = CommonResponse.ok(tokenVO);

            logger.info("accessToken: " + tokenVO.getAccessToken());

        } catch (Exception e) {
            logger.error(e.toString());
            result = CommonResponse.fail(e);
        }

        return result;
    }

    @RequestMapping(value = "/verify/token", method = {RequestMethod.POST},  produces = "application/json")
    public @ResponseBody CommonResponse verifyJwtToken(@RequestBody String accessToken) {
        CommonResponse result = null;

        try {
            // RS256
            Map<String, Object> res = jwtService.verifyAccessToken(accessToken);

            // 여기서 검증과정 및 payload 추출
            if(res==null) {
                result = CommonResponse.fail("Invaild Key");
            } else {
                result = CommonResponse.ok(res);
            }
        } catch (ExpiredJwtException e) {
            logger.info(e.toString());
            result = CommonResponse.fail("Expired");
        } catch (Exception e) {
            logger.error(e.toString());
            result = CommonResponse.fail("Invalid Key");
        }

        return result;
    }

    @GetMapping(value = "/jwks")
    public String getJwks()
    {
        return "{\"keys\": [{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"sumits_platform_auth_key\",\"n\":\"tV8x8yKc4GhNzL4DnwAbyJUWn-lrz-ft2tFTyrCrv4FwZM8Cs0S5iAESxw9Mo1adZERvUXMPrKgw6jSrqsLfQfRORPVX4jBKrNdjOrzvKGgOWuKnzAF186RS7hq76oRNActhrbByytufjQ2TT4PEQAzqchaPo0Mo0DeEZI-I7ws\"}]}";
    }

    @RequestMapping(value = "/refresh/token", method = {RequestMethod.POST},  produces = "application/json")
    public @ResponseBody CommonResponse refreshJwtToken(@RequestBody RefreshTokenDto dto) {
        CommonResponse result = null;

        boolean bVerified = false;
        // 먼저 refreshToken을 검증한다.
        try {
            // RS256
            Map<String, Object> res = jwtService.verifyRefreshToken(dto.getRefreshToken());

            // 여기서 검증과정 및 payload 추출
            if(res==null) {
                result = CommonResponse.fail("Invalid Token");
            } else {
                // clientIp를 검증한다.
                DecodedJWT jwt = JWT.decode(dto.getRefreshToken());
                String payload = new String(Base64.getDecoder().decode(jwt.getPayload()), "UTF-8");
                JSONObject jsonObject = new JSONObject(payload);
                String clientIp = (String) jsonObject.get("clientIp");
                String tokenId = (String) jsonObject.get("tokenId");

                if(dto.getClientIp().compareTo(clientIp) != 0) {
                    result = CommonResponse.fail("Invaild clientIp");
                } else {
                    if(redisUse == 0) {
                        bVerified = true;
                        logger.info("not use redisToken");
                    } else {
                        // Redis에서 refreshToken을 가져와서 검증
                        String refreshToken = (String) refreshTokenTemplate.opsForHash().get(tokenId, "refreshToken");
                        logger.info("redisToken: " + refreshToken);
                        if(refreshToken != null && dto.getRefreshToken() !=null && refreshToken.compareTo(dto.getRefreshToken()) == 0) {
                            bVerified = true;
                        } else {
                            result = CommonResponse.fail("Invaild refreshToken");
                        }
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            logger.info(e.toString());
            result = CommonResponse.fail("Expired");
        } catch (Exception e) {
            logger.error(e.toString());
            result = CommonResponse.fail("Invalid Token");
        }

        if(bVerified) { // refreshToken 검증이 끝났으니 accessToekn을 새로 발급한다.

            try {
                // accessToken에서 payload를 가져온다.
                DecodedJWT jwt = JWT.decode(dto.getAccessToken());
                String payload = new String(Base64.getDecoder().decode(jwt.getPayload()), "UTF-8");

                CreateTokenDto dtoAccessToken = new CreateTokenDto();
                dtoAccessToken.setClientIp(dto.getClientIp());
                dtoAccessToken.setPayload(payload);
                if( dto.getExpired() <= 0)
                    dtoAccessToken.setExpired(30L);
                else
                    dtoAccessToken.setExpired(dto.getExpired());

                // 생성된 Token을 result에 설정한다.
                TokenVO tokenVO = new TokenVO();
                tokenVO.setAccessToken(jwtService.createAccessToken(dtoAccessToken));
                tokenVO.setRefreshToken(dto.getRefreshToken());
                result = CommonResponse.ok(tokenVO);

            } catch (Exception e) {
                result = CommonResponse.fail(e);
            }
        }

        return result;
    }

    @RequestMapping(value = "/delete/token", method = {RequestMethod.POST},  produces = "application/json")
    public @ResponseBody CommonResponse deleteRefreshToken(@RequestBody String refreshToken) {
        CommonResponse result = null;

        try {
            DecodedJWT jwt = JWT.decode(refreshToken);
            String payload = new String(Base64.getDecoder().decode(jwt.getPayload()), "UTF-8");
            JSONObject jsonObject = new JSONObject(payload);
            //String clientIp = (String) jsonObject.get("clientIp");
            String tokenId = (String) jsonObject.get("tokenId");

            if( redisUse == 0 ) {
                result = CommonResponse.ok("success");
            } else {
                // Redis에서 refreshToken을 삭제한다
                if (refreshTokenTemplate.delete(tokenId)) {
                    result = CommonResponse.ok("success");
                } else {
                    result = CommonResponse.fail("Not found");
                }
            }
        } catch(Exception e) {
            result = CommonResponse.fail(e);
        }

        return result;
    }
}
