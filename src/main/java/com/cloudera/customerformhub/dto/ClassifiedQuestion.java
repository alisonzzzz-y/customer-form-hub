package com.cloudera.customerformhub.dto;

public class ClassifiedQuestion {

    private String section;
    private String questionText;
    private String department;   // assigned by the LLM classifier

    public ClassifiedQuestion() {
    }

    public ClassifiedQuestion(String section, String questionText, String department) {
        this.section = section;
        this.questionText = questionText;
        this.department = department;
    }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}