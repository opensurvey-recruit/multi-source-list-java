package com.opensurvey.surveys;

import java.util.List;

public interface SurveySource {
    String source();
    List<Survey> fetchAll();
}
