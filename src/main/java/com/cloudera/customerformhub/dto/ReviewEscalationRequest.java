package com.cloudera.customerformhub.dto;

public class ReviewEscalationRequest {
    private String type;
    private Long suggestionSourceId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getSuggestionSourceId() { return suggestionSourceId; }
    public void setSuggestionSourceId(Long suggestionSourceId) {
        this.suggestionSourceId = suggestionSourceId;
    }
}
