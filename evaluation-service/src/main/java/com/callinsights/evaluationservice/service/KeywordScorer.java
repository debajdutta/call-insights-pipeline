package com.callinsights.evaluationservice.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SPEC.md's own guardrail: rule-based template/keyword scoring, explicitly NOT ML - this is the
 * whole scoring engine. Case-insensitive substring match of each configured keyword against the
 * transcript text.
 */
@Component
public class KeywordScorer {

    public record ScoringResult(double score, boolean passed, List<String> matchedKeywords, List<String> missingKeywords) {
    }

    public ScoringResult score(String transcriptText, List<String> keywords, double passThreshold) {
        String haystack = transcriptText.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String keyword : keywords) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        double score = keywords.isEmpty() ? 0.0 : (double) matched.size() / keywords.size();
        return new ScoringResult(score, score >= passThreshold, matched, missing);
    }
}
