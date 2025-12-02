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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        if (!"naver".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Only Naver login is supported");
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String naverId = (String) response.get("id");
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String profileImage = (String) response.get("profile_image");

        log.info("Naver login: naverId={}, name={}, email={}", naverId, name, email);

        User user = userRepository.findByNaverId(naverId)
                .map(existingUser -> {
                    existingUser.updateProfile(name, email, profileImage);
                    existingUser.updateLastLogin();
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .naverId(naverId)
                        .email(email)
                        .name(name)
                        .profileImage(profileImage)
                        .build()));

        return new CustomOAuth2User(user, attributes);
    }
}
