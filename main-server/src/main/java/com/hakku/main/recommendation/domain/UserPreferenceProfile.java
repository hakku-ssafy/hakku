package com.hakku.main.recommendation.domain;

import java.util.Set;

public record UserPreferenceProfile(
        String personalColor,
        Set<String> preferredStyles,
        Set<String> preferredColors,
        Set<String> recentActionTags
) {
    public UserPreferenceProfile {
        preferredStyles = preferredStyles == null ? Set.of() : Set.copyOf(preferredStyles);
        preferredColors = preferredColors == null ? Set.of() : Set.copyOf(preferredColors);
        recentActionTags = recentActionTags == null ? Set.of() : Set.copyOf(recentActionTags);
    }
}
