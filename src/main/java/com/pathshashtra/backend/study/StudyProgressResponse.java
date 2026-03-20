package com.pathshashtra.backend.study;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class StudyProgressResponse {
    private int totalTopics;
    private int completedTopics;
    private int overallPercent;
    private List<SubjectProgress> subjectProgress;
    private int weakTopicsCount;
    private long daysUntilExam;

    @Getter @AllArgsConstructor
    public static class SubjectProgress {
        private String subject;
        private int total;
        private int completed;
        private int percent;
    }
}