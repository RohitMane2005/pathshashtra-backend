package com.pathshashtra.backend.study;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicProgressRequest {
    @NotNull(message = "Topic ID is required")
    private Long topicId;

    @NotNull(message = "Status is required")
    private StudyTopic.TopicStatus status; // COMPLETED, STRUGGLING, IN_PROGRESS

    @Min(value = 1, message = "Confidence score must be between 1 and 10")
    @Max(value = 10, message = "Confidence score must be between 1 and 10")
    private int confidenceScore; // 1-10
}