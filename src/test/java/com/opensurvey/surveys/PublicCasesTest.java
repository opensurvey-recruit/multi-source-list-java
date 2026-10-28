package com.opensurvey.surveys;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicCasesTest {
    private List<SurveySource> sources() {
        List<Survey> surveys = Stream.concat(OveySurveySource.initialExample().stream(), PartnerSurveySource.initialExample().stream()).toList();
        return List.of(
                new OveySurveySource(surveys.stream().filter(survey -> survey.source().equals("OVEY")).toList()),
                new PartnerSurveySource(surveys.stream().filter(survey -> survey.source().equals("PARTNER")).toList())
        );
    }

    private List<String> ids(List<Survey> surveys) {
        return surveys.stream().map(survey -> survey.source() + "/" + survey.id()).toList();
    }

    @Test
    @DisplayName("PUBLIC paginate all results after applying search and status filters")
    void paginateFilteredResults() {
        SurveyRepository repository = new SurveyRepository(sources());
        GetSurveysRequest request = new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.UPDATED_DESC);
        SurveyPage first = repository.getSurveys(request);
        SurveyPage second = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 1, 2, SurveySort.UPDATED_DESC));
        assertEquals(3L, first.totalElements());
        assertEquals(2, first.content().size());
        assertEquals("PARTNER/1", ids(first.content()).get(0));
        assertEquals(3L, second.totalElements());
        assertEquals(1, second.content().size());
        List<Survey> combined = Stream.concat(first.content().stream(), second.content().stream()).toList();
        assertEquals(Set.of("PARTNER/1", "OVEY/1", "PARTNER/3"), Set.copyOf(ids(combined)));
        assertEquals(first, repository.getSurveys(request));
    }

    @Test
    @DisplayName("PUBLIC refresh reflects additions and status changes")
    void refreshChanges() {
        List<Survey> initial = Stream.concat(OveySurveySource.initialExample().stream(), PartnerSurveySource.initialExample().stream()).toList();
        OveySurveySource ovey = new OveySurveySource(initial.stream()
                .filter(survey -> survey.source().equals("OVEY")).toList());
        PartnerSurveySource partner = new PartnerSurveySource(initial.stream()
                .filter(survey -> survey.source().equals("PARTNER")).toList());
        SurveyRepository repository = new SurveyRepository(List.of(ovey, partner));
        repository.getSurveys();
        List<Survey> changed = Stream.concat(OveySurveySource.changedExample().stream(), PartnerSurveySource.changedExample().stream()).toList();
        ovey.replaceSurveys(changed.stream().filter(survey -> survey.source().equals("OVEY")).toList());
        partner.replaceSurveys(changed.stream().filter(survey -> survey.source().equals("PARTNER")).toList());
        repository.refresh();
        SurveyPage page = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.UPDATED_DESC));
        assertEquals(3L, page.totalElements());
        assertEquals(2, page.content().size());
        assertEquals("OVEY/4", ids(page.content()).get(0));
        SurveyPage all = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 0, 100, SurveySort.UPDATED_DESC));
        assertEquals(Set.of("OVEY/4", "OVEY/1", "PARTNER/3"), Set.copyOf(ids(all.content())));
        assertEquals(all.content().subList(0, 2), page.content());
    }

    @Test
    @DisplayName("PUBLIC return an empty list and zero total when nothing matches")
    void emptySearchResult() {
        SurveyRepository repository = new SurveyRepository(sources());
        assertEquals(new SurveyPage(List.of(), 0), repository.getSurveys(
                new GetSurveysRequest("없는 조사", null, 0, 20, SurveySort.UPDATED_DESC)));
    }

    @Test
    @DisplayName("PUBLIC reward sorting preserves results and duration sorting is ascending")
    void rewardAndDurationSorting() {
        SurveyRepository repository = new SurveyRepository(sources());
        GetSurveysRequest rewardRequest = new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.REWARD_DESC);
        SurveyPage reward = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 0, 100, SurveySort.REWARD_DESC));
        SurveyPage first = repository.getSurveys(rewardRequest);
        SurveyPage second = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 1, 2, SurveySort.REWARD_DESC));
        SurveyPage duration = repository.getSurveys(new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.DURATION_ASC));
        assertEquals(3L, reward.totalElements());
        assertEquals(3, reward.content().size());
        assertEquals(Set.of("OVEY/1", "PARTNER/1", "PARTNER/3"), Set.copyOf(ids(reward.content())));
        assertEquals(3L, first.totalElements());
        assertEquals(3L, second.totalElements());
        assertEquals(reward.content(), Stream.concat(first.content().stream(), second.content().stream()).toList());
        assertEquals(first, repository.getSurveys(rewardRequest));
        assertEquals(3L, duration.totalElements());
        assertEquals(List.of("OVEY/1", "PARTNER/3"), ids(duration.content()));
    }

    @Test
    @DisplayName("SOURCE default examples advance once after the first successful read")
    void defaultExamplesAdvanceOnce() {
        for (SurveySource source : List.of(new OveySurveySource(), new PartnerSurveySource())) {
            List<Survey> initial = source.source().equals("OVEY") ? OveySurveySource.initialExample() : PartnerSurveySource.initialExample();
            List<Survey> changed = source.source().equals("OVEY") ? OveySurveySource.changedExample() : PartnerSurveySource.changedExample();
            List<Survey> first = source.fetchAll();
            assertEquals(initial, first);
            assertEquals(changed, source.fetchAll());
            assertEquals(changed, source.fetchAll());
            assertEquals(initial, first);
        }
    }

    @Test
    @DisplayName("SOURCE failed reads do not advance examples and explicit data remains stable")
    void failedReadsAndExplicitData() {
        PartnerSurveySource source = new PartnerSurveySource(0, Set.of(1));
        assertThrows(SourceUnavailableException.class, source::fetchAll);
        assertEquals(PartnerSurveySource.initialExample(), source.fetchAll());
        assertEquals(PartnerSurveySource.changedExample(), source.fetchAll());
        assertEquals(3, source.fetchCount());
        for (SurveySource stable : sources()) {
            List<Survey> first = stable.fetchAll();
            assertEquals(first, stable.fetchAll());
            assertEquals(first, stable.fetchAll());
        }
    }

}
