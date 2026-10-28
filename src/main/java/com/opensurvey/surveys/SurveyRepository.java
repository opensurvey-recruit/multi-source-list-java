package com.opensurvey.surveys;

import java.util.List;

public class SurveyRepository {
    private final List<SurveySource> sources;

    public SurveyRepository(List<SurveySource> sources) {
        this.sources = sources;
    }

    public SurveyPage getSurveys() {
        return getSurveys(new GetSurveysRequest());
    }

    public SurveyPage getSurveys(GetSurveysRequest request) {
        // TODO: Choose a query structure and implement search, filtering, sorting, and pagination.
        throw new UnsupportedOperationException("Implement getSurveys.");
    }

    public void refresh() {
        // TODO: Make the latest source data available after this call.
        throw new UnsupportedOperationException("Implement refresh.");
    }
}
