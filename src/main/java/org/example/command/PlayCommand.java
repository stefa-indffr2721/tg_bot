package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

import static org.example.util.MessageUtils.createMessage;

public class PlayCommand implements Command {
    private final QuizService quizService;
    private final SessionService userSessionService;

    public PlayCommand() {
        this.quizService = new QuizService();
        this.userSessionService = new SessionService();
    }

    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        showCategorySelection(chatId, bot);
    }

    public void showCategorySelection(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        List<String> categories = quizService.getAvailableCategories();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category);
            button.setCallbackData("category_" + category);
            row.add(button);
            rows.add(row);
        }

        keyboardMarkup.setKeyboard(rows);

        SendMessage message = createMessage(chatId, "🎯 Выберите категорию:");
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    public void processCategorySelection(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        List<QuizQuestion> questions = quizService.getQuestionsByCategory(categoryName);
        userSessionService.startNewGame(chatId, categoryName, questions);

        bot.execute(createMessage(chatId, "✅ Вы выбрали: " + categoryName + "\nНачинаем викторину!"));
        sendNextQuestion(chatId, bot);
    }

    private void sendNextQuestion(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
            QuizQuestion currentQuestion = gameState.getQuestions().get(gameState.getCurrentQuestionIndex());

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<String> options = currentQuestion.getOptions();
            for (int i = 0; i < options.size(); i++) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(options.get(i));
                button.setCallbackData("answer_" + i);
                row.add(button);
                rows.add(row);
            }

            keyboardMarkup.setKeyboard(rows);

            String questionText = "📝 Категория: " + gameState.getSelectedCategory() + "\n" +
                    "Вопрос " + (gameState.getCurrentQuestionIndex() + 1) + "/" + gameState.getQuestions().size() + ":\n" +
                    currentQuestion.getQuestion();

            SendMessage message = createMessage(chatId, questionText);
            message.setReplyMarkup(keyboardMarkup);
            bot.execute(message);
        } else {
            finishGame(chatId, bot);
        }
    }

    public void processAnswer(long chatId, int answerIndex, TelegramLongPollingBot bot) {
        try {
            GameState gameState = userSessionService.getGameState(chatId);
            QuizQuestion currentQuestion = gameState.getQuestions().get(gameState.getCurrentQuestionIndex());

            boolean isCorrect = quizService.validateAnswer(currentQuestion, answerIndex);

            if (isCorrect) {
                gameState.incrementCorrectAnswers();
                bot.execute(createMessage(chatId, "✅ Правильно!"));
            } else {
                String correctAnswer = quizService.getCorrectAnswer(currentQuestion);
                bot.execute(createMessage(chatId, "❌ Неправильно! Правильный ответ: " + correctAnswer));
            }

            gameState.incrementQuestionIndex();

            // Задержка перед следующим вопросом
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
                        sendNextQuestion(chatId, bot);
                    } else {
                        finishGame(chatId, bot);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (TelegramApiException e) {
                    System.err.println("[ERR]: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    private void finishGame(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);
        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.getCorrectAnswers() + "/" + gameState.getQuestions().size() + " правильных ответов!\n\n" +
                "Хотите сыграть ещё раз?\n" +
                "/play\n" +
                "Или используйте команду /help чтобы узнать что я умею.";

        bot.execute(createMessage(chatId, result));
        userSessionService.removeGameState(chatId);
    }
}
