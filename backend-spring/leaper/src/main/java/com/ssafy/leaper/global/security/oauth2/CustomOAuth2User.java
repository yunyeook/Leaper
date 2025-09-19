package com.ssafy.leaper.global.security.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User originalUser;
    private final Map<String, Object> youtubeData;

    public CustomOAuth2User(OAuth2User originalUser, Map<String, Object> youtubeData) {
        this.originalUser = originalUser;
        this.youtubeData = youtubeData != null ? youtubeData : new HashMap<>();
    }

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>(originalUser.getAttributes());
        attributes.putAll(youtubeData);
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return originalUser.getAuthorities();
    }

    @Override
    public String getName() {
        return originalUser.getName();
    }
}