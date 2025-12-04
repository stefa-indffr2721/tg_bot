package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

import static org.example.util.MessageUtils.createMessage;

public class GameManager {
    private final QuizService quizService;
    private final SessionService userSessionService;
    private final QuestionSender questionSender;
    private final GameFinisher gameFinisher;
    private final Map<Long, String> userNames;

    public GameManager(GameStateContainer gameStateContainer) {
        this.quizService = new QuizService();
        this.userSessionService = gameStateContainer.getSessionService();
        this.questionSender = new QuestionSender();
        this.gameFinisher = new GameFinisher();
        this.userNames = gameStateContainer.getUserNames();
    }

    public void startGameWithTimer(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Давайте начнем сначала!"));
            return;
        }

        gameState.setStartTime(System.currentTimeMillis());

        bot.execute(createMessage(chatId, "⏱ Таймер запущен! Начинаем викторину!"));
        sendNextQuestion(chatId, bot);
    }

    private void sendNextQuestion(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
            return;
        }

        if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
            questionSender.sendQuestion(chatId, gameState, bot);
        } else {
            gameFinisher.finishGame(chatId, bot, userSessionService, userNames);
        }
    }

    public void processAnswer(long chatId, int answerIndex, Update update, TelegramLongPollingBot bot) {
        try {
            if (update != null && update.hasCallbackQuery()) {
                User user = update.getCallbackQuery().getFrom();
                String userName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    userName += " " + user.getLastName();
                }
                userNames.put(chatId, userName);
            }

            GameState gameState = userSessionService.getGameState(chatId);

            if (gameState == null) {
                bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
                return;
            }

            if (gameState.getCurrentQuestionIndex() >= gameState.getQuestions().size()) {
                gameFinisher.finishGame(chatId, bot, userSessionService, userNames);
                return;
            }

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

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
                        sendNextQuestion(chatId, bot);
                    } else {
                        gameFinisher.finishGame(chatId, bot, userSessionService, userNames);
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
}