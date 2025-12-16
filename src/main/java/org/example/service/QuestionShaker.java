package org.example.service;

import org.example.model.QuizQuestion;
import java.util.*;

public class QuestionShaker {

    public static QuizQuestion.ShuffledQuestion createShuffled(QuizQuestion originalQuestion) {
        List<String> originalOptions = originalQuestion.getOptions();
        int originalCorrectIndex = originalQuestion.getCorrectAnswerIndex();

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < originalOptions.size(); i++) {
            indices.add(i);
        }

        Collections.shuffle(indices);

        List<String> shuffledOptions = new ArrayList<>();
        int newCorrectIndex = -1;

        for (int i = 0; i < indices.size(); i++) {
            int originalIndex = indices.get(i);
            shuffledOptions.add(originalOptions.get(originalIndex));

            if (originalIndex == originalCorrectIndex) {
                newCorrectIndex = i;
            }
        }

        return new QuizQuestion.ShuffledQuestion(
                originalQuestion.getQuestion(),
                shuffledOptions,
                newCorrectIndex,
                originalQuestion.getImage()
        );
    }
}