package com.hakku.main.recommendation.domain;

import java.util.Set;

/**
 * Immutable snapshot of the signals used to score product recommendations
 * for a single user (PRD §3.5).
 *
 * @param personalColor    the user's diagnosed personal color (e.g. "WINTER"); may be null if undiagnosed
 * @param preferredStyles  style tags the user explicitly prefers; never null (use empty set)
 * @param recentActionTags style tags derived from recent click/like/view history; never null (use empty set)
 */
public record UserPreferenceProfile(
        String personalColor,
        Set<String> preferredStyles,
        Set<String> recentActionTags
) {
    public UserPreferenceProfile {
        preferredStyles = preferredStyles == null ? Set.of() : Set.copyOf(preferredStyles);
        recentActionTags = recentActionTags == null ? Set.of() : Set.copyOf(recentActionTags);
    }
}
