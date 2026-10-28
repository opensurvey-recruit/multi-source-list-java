package com.opensurvey.surveys;

import java.util.List;

public record SurveyPage(List<Survey> content, long totalElements) {}
