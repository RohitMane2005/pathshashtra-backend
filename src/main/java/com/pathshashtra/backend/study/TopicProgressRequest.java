package com.pathshashtra.backend.study;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TopicProgressRequest {
    private Long topicId;
    private StudyTopic.TopicStatus status;  // COMPLETED, STRUGGLING, IN_PROGRESS
    private int confidenceScore;            // 1–10
}