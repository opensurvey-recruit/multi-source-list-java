package com.opensurvey.surveys;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1) throw new IllegalArgumentException("Specify one scenario: fast, slow, or failure.");
        try {
            SurveyRepository repository = new SurveyRepository(List.of(
                    new OveySurveySource(), new PartnerSurveySource(args.length == 0 ? "fast" : args[0])));
            GetSurveysRequest request = new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.UPDATED_DESC);

            // 각 원본은 첫 fetchAll() 성공 시 초기 데이터를 반환한 뒤 내부 데이터를 바꾼다.
            observe("initial query", () -> repository.getSurveys(request));

            // 반복 조회로 변경 반영 시점과 원본 호출 횟수를 비교한다.
            for (int index = 1; index <= 3; index++) {
                observe("query after source changes " + index, () -> repository.getSurveys(request));
            }

            // 갱신 성공 또는 실패 뒤 조회 결과를 확인한다.
            observe("refresh", () -> { repository.refresh(); return null; });
            observe("query after refresh", () -> repository.getSurveys(request));

            // 검색어와 상태 조건을 유지하고 정렬에 따른 순서 차이를 비교한다.
            observe("reward descending", () -> repository.getSurveys(
                    new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.REWARD_DESC)));
            observe("duration ascending", () -> repository.getSurveys(
                    new GetSurveysRequest("생활", Status.OPEN, 0, 2, SurveySort.DURATION_ASC)));
        } catch (UnsupportedOperationException error) {
            if (!"Implement getSurveys.".equals(error.getMessage()) && !"Implement refresh.".equals(error.getMessage())) {
                throw error;
            }
            System.out.println("Starter code is running. Implement both TODOs in SurveyRepository.java.");
        }
    }

    private static void observe(String label, Supplier<SurveyPage> action) {
        long started = System.nanoTime();
        try {
            SurveyPage result = action.get();
            System.out.println(label + ": " + (result == null ? "completed" : result));
        } catch (SourceUnavailableException error) {
            System.out.println(label + " failed: " + error.getMessage());
        } finally {
            System.out.printf("query elapsed=%dms%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }
}
