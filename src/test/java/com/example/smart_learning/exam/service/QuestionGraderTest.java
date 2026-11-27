package com.example.smart_learning.exam.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionGraderTest {

    @Test
    void normalizesAnswerLabels() {
        assertEquals("AC", QuestionGrader.normalize(" c, a "));
        assertEquals("", QuestionGrader.normalize(null));
        assertEquals("B", QuestionGrader.normalize("b"));
        assertEquals("ABC", QuestionGrader.joinLabels(List.of("C", "A", "B", "A")));
    }

    @Test
    void comparesAnswerSetsIgnoringOrderAndCase() {
        assertTrue(QuestionGrader.isCorrect("A", "a"));
        assertTrue(QuestionGrader.isCorrect("AC", "CA"));
        assertFalse(QuestionGrader.isCorrect("AC", "A"));
        assertFalse(QuestionGrader.isCorrect("AC", "ABC"));
        assertFalse(QuestionGrader.isCorrect("", ""));
    }

    @Test
    void validatesSelectionsAgainstOptionLabels() {
        assertTrue(QuestionGrader.isValidSelection("AB", List.of("A", "B", "C")));
        assertTrue(QuestionGrader.isValidSelection("", List.of("A", "B")));
        assertFalse(QuestionGrader.isValidSelection("AD", List.of("A", "B", "C")));
        assertFalse(QuestionGrader.isValidSelection("D", List.of("A", "B")));
    }
}
