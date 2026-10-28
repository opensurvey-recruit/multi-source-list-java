package com.opensurvey.surveys;

public record GetSurveysRequest(
        String query,
        Status status,
        int page,
        int size,
        SurveySort sort
) {
    public GetSurveysRequest() {
        this("", null, 0, 20, SurveySort.UPDATED_DESC);
    }
}
