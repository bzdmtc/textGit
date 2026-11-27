package com.example.smart_learning.exam.service;

import com.example.smart_learning.question.entity.QuestionOption;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** 纯判题逻辑：答案统一为按字母排序的选项标签串，如 "A" / "AC"。 */
public final class QuestionGrader {

    private QuestionGrader() {
    }

    public static String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        Set<Character> labels = new TreeSet<>();
        for (char ch : answer.toUpperCase().toCharArray()) {
            if (ch >= 'A' && ch <= 'Z') {
                labels.add(ch);
            }
        }
        StringBuilder builder = new StringBuilder(labels.size());
        labels.forEach(builder::append);
        return builder.toString();
    }

    public static String joinLabels(Collection<String> labels) {
        Set<String> normalized = new TreeSet<>();
        for (String label : labels) {
            if (label != null && !label.isBlank()) {
                normalized.add(label.trim().toUpperCase());
            }
        }
        return String.join("", normalized);
    }

    public static String correctAnswerOf(List<QuestionOption> options) {
        Set<String> labels = new LinkedHashSet<>();
        for (QuestionOption option : options) {
            if (Boolean.TRUE.equals(option.getIsCorrect())) {
                labels.add(option.getLabel());
            }
        }
        return joinLabels(labels);
    }

    public static boolean isCorrect(String correctAnswer, String userAnswer) {
        String correct = normalize(correctAnswer);
        String answer = normalize(userAnswer);
        return !correct.isEmpty() && correct.equals(answer);
    }

    public static boolean isValidSelection(String userAnswer, List<QuestionOption> options) {
        Set<String> allowed = new TreeSet<>();
        for (QuestionOption option : options) {
            allowed.add(option.getLabel().trim().toUpperCase());
        }
        return isValidSelection(userAnswer, allowed);
    }

    public static boolean isValidSelection(String userAnswer, Collection<String> optionLabels) {
        String normalized = normalize(userAnswer);
        if (normalized.isEmpty()) {
            return true;
        }
        Set<String> allowed = new TreeSet<>();
        for (String label : optionLabels) {
            if (label != null && !label.isBlank()) {
                allowed.add(label.trim().toUpperCase());
            }
        }
        for (char ch : normalized.toCharArray()) {
            if (!allowed.contains(String.valueOf(ch))) {
                return false;
            }
        }
        return true;
    }
}
