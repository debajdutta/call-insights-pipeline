package com.callinsights.evaluationservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordScorerTest {

    private final KeywordScorer scorer = new KeywordScorer();

    @Test
    void scoresOneWhenEveryKeywordIsPresent() {
        var result = scorer.score(
                "Thank you for calling, here is our special offer, would you like to proceed?",
                List.of("thank", "offer", "proceed"), 0.5);

        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.passed()).isTrue();
        assertThat(result.matchedKeywords()).containsExactly("thank", "offer", "proceed");
        assertThat(result.missingKeywords()).isEmpty();
    }

    @Test
    void scoresZeroWhenNoKeywordIsPresent() {
        var result = scorer.score("A completely unrelated conversation about the weather.",
                List.of("thank", "offer", "proceed"), 0.5);

        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.passed()).isFalse();
        assertThat(result.matchedKeywords()).isEmpty();
        assertThat(result.missingKeywords()).containsExactly("thank", "offer", "proceed");
    }

    @Test
    void failsWhenScoreIsBelowThreshold() {
        var result = scorer.score("Thank you for calling today.",
                List.of("thank", "offer", "proceed", "discount"), 0.5);

        assertThat(result.score()).isEqualTo(0.25);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void passesWhenScoreMeetsThresholdExactly() {
        var result = scorer.score("Thank you, here is our offer.",
                List.of("thank", "offer", "proceed", "discount"), 0.5);

        assertThat(result.score()).isEqualTo(0.5);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void matchingIsCaseInsensitive() {
        var result = scorer.score("THANK YOU FOR CALLING", List.of("thank"), 1.0);

        assertThat(result.matchedKeywords()).containsExactly("thank");
    }

    @Test
    void returnsZeroScoreWhenNoKeywordsConfigured() {
        var result = scorer.score("Any transcript text", List.of(), 0.5);

        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.passed()).isFalse();
    }
}
