package com.aiinsight.security;

import com.aiinsight.domain.user.User;
import com.aiinsight.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            log.info("OAuth2 loadUser started");
            log.info("Token URI: {}", userRequest.getClientRegistration().getProviderDetails().getTokenUri());
            log.info("User Info URI: {}", userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
            log.info("Redirect URI: {}", userRequest.getClientRegistration().getRedirectUri());
            log.info("Access Token: {}...", userRequest.getAccessToken().getTokenValue().substring(0, Math.min(20, userRequest.getAccessToken().getTokenValue().length())));

            OAuth2User oAuth2User = super.loadUser(userRequest);
            log.info("OAuth2User loaded from provider");

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            log.info("Registration ID: {}", registrationId);

            if (!"naver".equals(registrationId)) {
                throw new OAuth2AuthenticationException("Only Naver login is supported");
            }

            Map<String, Object> attributes = oAuth2User.getAttributes();
            log.info("Attributes: {}", attributes);

            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response == null) {
                log.error("Response is null in attributes");
                throw new OAuth2AuthenticationException("Invalid response from Naver");
            }

            String naverId = (String) response.get("id");
            String email = (String) response.get("email");
            String name = (String) response.get("name");
            String profileImage = (String) response.get("profile_image");

            log.info("Naver login: naverId={}, name={}, email={}", naverId, name, email);

            User user = userRepository.findByNaverId(naverId)
                    .map(existingUser -> {
                        log.info("Existing user found, updating profile");
                        existingUser.updateProfile(name, email, profileImage);
                        existingUser.updateLastLogin();
                        return existingUser;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new user");
                        return userRepository.save(User.builder()
                                .naverId(naverId)
                                .email(email)
                                .name(name)
                                .profileImage(profileImage)
                                .build());
                    });

            log.info("User processed successfully: userId={}", user.getId());
            return new CustomOAuth2User(user, attributes);
        } catch (Exception e) {
            log.error("OAuth2 loadUser failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
