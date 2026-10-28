package com.opensurvey.surveys;

import java.util.List;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.Set;

// Fixed failure call numbers make each scenario reproducible.
public class PartnerSurveySource implements SurveySource {
    private List<Survey> surveys;
    private List<Survey> nextSurveys;
    private final long delayMillis;
    private final Set<Integer> failOnCalls;
    private int fetchCount;

    public PartnerSurveySource() {
        this("fast");
    }

    public PartnerSurveySource(String scenario) {
        this(scenario.equals("slow") ? 500 : 0, scenario.equals("failure") ? Set.of(2) : Set.of());
        if (!Set.of("fast", "slow", "failure").contains(scenario)) {
            throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
    }

    public PartnerSurveySource(long delayMillis, Set<Integer> failOnCalls) {
        this(initialExample(), delayMillis, failOnCalls);
        nextSurveys = changedExample();
    }

    public PartnerSurveySource(List<Survey> initialSurveys) {
        this(initialSurveys, 0, Set.of());
    }

    public PartnerSurveySource(List<Survey> initialSurveys, long delayMillis, Set<Integer> failOnCalls) {
        if (delayMillis < 0 || delayMillis > 2000) {
            throw new IllegalArgumentException("delayMillis must be between 0 and 2000.");
        }
        if (failOnCalls.stream().anyMatch(call -> call <= 0)) {
            throw new IllegalArgumentException("Failure call numbers must be positive.");
        }
        this.delayMillis = delayMillis;
        this.failOnCalls = Set.copyOf(failOnCalls);
        replaceSurveys(initialSurveys);
    }

    @Override
    public String source() {
        return "PARTNER";
    }

    public int fetchCount() {
        return fetchCount;
    }

    @Override
    public List<Survey> fetchAll() {
        long started = System.nanoTime();
        String outcome = "failed";
        try {
            fetchCount += 1;
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while reading source PARTNER.", error);
                }
            }
            if (failOnCalls.contains(fetchCount)) {
                throw new SourceUnavailableException(source());
            }
            List<Survey> result = List.copyOf(surveys);
            // Advance after a successful read even if the repository never reads again.
            if (nextSurveys != null) {
                surveys = nextSurveys;
                nextSurveys = null;
            }
            outcome = "success";
            return result;
        } finally {
            System.out.printf("source=%s fetchCount=%d outcome=%s elapsed=%dms%n", source(), fetchCount, outcome,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }

    public void replaceSurveys(List<Survey> surveys) {
        if (surveys.stream().anyMatch(survey -> !source().equals(survey.source()))) {
            throw new IllegalArgumentException("Survey source must be PARTNER.");
        }
        if (surveys.stream().map(Survey::id).distinct().count() != surveys.size()) {
            throw new IllegalArgumentException("Survey ids must be unique within a source.");
        }
        this.surveys = List.copyOf(surveys);
        nextSurveys = null;
    }

    public static List<Survey> initialExample() {
        return List.of(
                new Survey("PARTNER", "1", "일상생활 관련 조사(해외조사)", Status.OPEN, Instant.parse("2026-10-13T09:45:00Z"), 100, 1000, 15),
                new Survey("PARTNER", "2", "Shopping habits survey", Status.OPEN, Instant.parse("2026-10-13T08:00:00Z"), 150, 150, 4),
                new Survey("PARTNER", "3", "주거생활 조사", Status.OPEN, Instant.parse("2026-10-13T09:00:00Z"), 300, 1000, 10)
        );
    }

    public static List<Survey> changedExample() {
        return List.of(
                new Survey("PARTNER", "1", "일상생활 관련 조사(해외조사)", Status.CLOSED, Instant.parse("2026-10-13T10:30:00Z"), 100, 1000, 15),
                new Survey("PARTNER", "2", "Shopping habits survey", Status.OPEN, Instant.parse("2026-10-13T08:00:00Z"), 150, 150, 4),
                new Survey("PARTNER", "3", "주거생활 조사", Status.OPEN, Instant.parse("2026-10-13T09:00:00Z"), 300, 1000, 10)
        );
    }
}
