package com.opensurvey.surveys;

import java.util.List;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

// Only full reads are available, so detecting changes requires a fetchAll call.
public class OveySurveySource implements SurveySource {
    private List<Survey> surveys;
    private List<Survey> nextSurveys;
    private int fetchCount;

    public OveySurveySource() {
        this(initialExample());
        nextSurveys = changedExample();
    }

    public OveySurveySource(List<Survey> initialSurveys) {
        replaceSurveys(initialSurveys);
    }

    @Override
    public String source() {
        return "OVEY";
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
            throw new IllegalArgumentException("Survey source must be OVEY.");
        }
        if (surveys.stream().map(Survey::id).distinct().count() != surveys.size()) {
            throw new IllegalArgumentException("Survey ids must be unique within a source.");
        }
        this.surveys = List.copyOf(surveys);
        nextSurveys = null;
    }

    public static List<Survey> initialExample() {
        return List.of(
                new Survey("OVEY", "1", "식생활 조사", Status.OPEN, Instant.parse("2026-10-13T09:00:00Z"), 300, 300, 7),
                new Survey("OVEY", "2", "라이프 스타일 조사", Status.OPEN, Instant.parse("2026-10-13T09:30:00Z"), 200, 200, 5),
                new Survey("OVEY", "3", "생활용품 조사", Status.CLOSED, Instant.parse("2026-10-13T10:00:00Z"), 500, 500, 12)
        );
    }

    public static List<Survey> changedExample() {
        return List.of(
                new Survey("OVEY", "1", "식생활 조사", Status.OPEN, Instant.parse("2026-10-13T09:00:00Z"), 300, 300, 7),
                new Survey("OVEY", "2", "라이프 스타일 조사", Status.OPEN, Instant.parse("2026-10-13T09:30:00Z"), 200, 200, 5),
                new Survey("OVEY", "3", "생활용품 조사", Status.CLOSED, Instant.parse("2026-10-13T10:00:00Z"), 500, 500, 12),
                new Survey("OVEY", "4", "여가생활 조사", Status.OPEN, Instant.parse("2026-10-13T11:00:00Z"), 700, 700, 3)
        );
    }
}
