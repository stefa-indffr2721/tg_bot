package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.example.service.QuestionShaker;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.util.MessageUtils.createMessage;

public class DuelService {
    private final SessionService userSessionService;
    private final Map<Long, String> userNames;
    private final Map<Long, DuelRequest> pendingDuels;
    private final Map<String, List<Long>> duelQueue;
    private final GameFinisher gameFinisher;

    public DuelService(GameStateContainer gameStateContainer) {
        this.userSessionService = gameStateContainer.getSessionService();
        this.userNames = gameStateContainer.getUserNames();
        this.pendingDuels = new ConcurrentHashMap<>();
        this.duelQueue = new ConcurrentHashMap<>();
        this.gameFinisher = new GameFinisher();
    }

    public void startDuelSearch(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        List<Long> queue = duelQueue.get(categoryName);

        if (queue != null && !queue.isEmpty() && !queue.contains(chatId)) {
            long opponentChatId = queue.remove(0);
            if (queue.isEmpty()) {
                duelQueue.remove(categoryName);
            }

            startDuel(chatId, opponentChatId, categoryName, bot);
        } else {
            if (queue != null && queue.contains(chatId)) {
                bot.execute(createMessage(chatId, "⏳ Вы уже в очереди на поиск дуэли!"));
                return;
            }

            if (!duelQueue.containsKey(categoryName)) {
                duelQueue.put(categoryName, new ArrayList<>());
            }
            duelQueue.get(categoryName).add(chatId);

            DuelRequest duelRequest = new DuelRequest(chatId, categoryName);
            pendingDuels.put(chatId, duelRequest);

            showWaitingMessage(chatId, categoryName, bot);
        }
    }

    private void showWaitingMessage(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("cancel_duel");
        row.add(cancelButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);

        String messageText = "⚔️ <b>Поиск соперника...</b>\n\n" +
                "Категория: " + categoryName + "\n" +
                "Ожидаем второго игрока...\n\n" +
                "Поделитесь ссылкой на бота с другом чтобы найти соперника быстрее!";

        SendMessage message = createMessage(chatId, messageText);
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    private void startDuel(long player1ChatId, long player2ChatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        QuizService quizService = new QuizService();
        List<QuizQuestion> questions = quizService.getQuestionsByCategory(categoryName);

        if (questions == null || questions.isEmpty()) {
            String errorMessage = "❌ Ошибка: Для категории \"" + categoryName + "\" нет вопросов!";
            bot.execute(createMessage(player1ChatId, errorMessage));
            bot.execute(createMessage(player2ChatId, errorMessage));
            return;
        }

        userSessionService.startNewGame(player1ChatId, categoryName, questions);
        GameState player1State = userSessionService.getGameState(player1ChatId);
        player1State.setDuelMode(true);
        player1State.setOpponentChatId(player2ChatId);

        userSessionService.startNewGame(player2ChatId, categoryName, questions);
        GameState player2State = userSessionService.getGameState(player2ChatId);
        player2State.setDuelMode(true);
        player2State.setOpponentChatId(player1ChatId);

        pendingDuels.remove(player1ChatId);
        pendingDuels.remove(player2ChatId);

        String player1Name = userNames.getOrDefault(player1ChatId, "Игрок");
        String player2Name = userNames.getOrDefault(player2ChatId, "Игрок");

        String player1Message = "⚔️ <b>Дуэль найдена!</b>\n\n" +
                "Категория: " + categoryName + "\n" +
                "Ваш соперник: " + player2Name + "\n\n" +
                "Игра начнется через 3 секунды!";

        String player2Message = "⚔️ <b>Дуэль найдена!</b>\n\n" +
                "Категория: " + categoryName + "\n" +
                "Ваш соперник: " + player1Name + "\n\n" +
                "Игра начнется через 3 секунды!";

        bot.execute(createMessage(player1ChatId, player1Message));
        bot.execute(createMessage(player2ChatId, player2Message));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    startDuelGame(player1ChatId, player2ChatId, bot);
                    timer.cancel();
                } catch (TelegramApiException e) {
                    System.err.println("[ERR Duel Start]: " + e.getMessage());
                    try {
                        bot.execute(createMessage(player1ChatId, "❌ Ошибка при запуске дуэли: " + e.getMessage()));
                        bot.execute(createMessage(player2ChatId, "❌ Ошибка при запуске дуэли: " + e.getMessage()));
                    } catch (TelegramApiException ex) {
                        System.err.println("[ERR Sending Error]: " + ex.getMessage());
                    }
                }
            }
        }, 3000);
    }

    private void startDuelGame(long player1ChatId, long player2ChatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState player1State = userSessionService.getGameState(player1ChatId);
        GameState player2State = userSessionService.getGameState(player2ChatId);

        if (player1State != null && player2State != null) {
            if (player1State.getQuestions() == null || player1State.getQuestions().isEmpty() ||
                    player2State.getQuestions() == null || player2State.getQuestions().isEmpty()) {

                String errorMessage = "❌ Ошибка: Нет вопросов для игры!";
                bot.execute(createMessage(player1ChatId, errorMessage));
                bot.execute(createMessage(player2ChatId, errorMessage));
                return;
            }

            player1State.setStartTime(System.currentTimeMillis());
            player2State.setStartTime(System.currentTimeMillis());

            String startMessage = """
                    ⚔️ <b>Дуэль началась!</b>
                    Отвечайте на вопросы как можно быстрее!
                    Победит тот, кто ответит правильно на большее количество вопросов!""";

            bot.execute(createMessage(player1ChatId, startMessage));
            bot.execute(createMessage(player2ChatId, startMessage));

            QuestionSender questionSender = new QuestionSender();
            questionSender.sendQuestion(player1ChatId, player1State, bot);
            questionSender.sendQuestion(player2ChatId, player2State, bot);
        }
    }

    public void processDuelAnswer(long chatId, int answerIndex, TelegramLongPollingBot bot) {
        try {
            GameState gameState = userSessionService.getGameState(chatId);

            if (gameState == null || !gameState.isDuelMode()) {
                return;
            }

            long opponentChatId = gameState.getOpponentChatId();
            GameState opponentState = userSessionService.getGameState(opponentChatId);

            if (gameState.getQuestions() == null || gameState.getQuestions().isEmpty() ||
                    gameState.getCurrentQuestionIndex() >= gameState.getQuestions().size()) {
                return;
            }

            QuizQuestion.ShuffledQuestion currentQuestion = gameState.getCurrentShuffledQuestion();
            boolean isCorrect = false;

            if (currentQuestion != null) {
                isCorrect = currentQuestion.isCorrectAnswer(answerIndex);
            } else {
                QuizQuestion original = gameState.getQuestions().get(gameState.getCurrentQuestionIndex());
                currentQuestion = QuestionShaker.createShuffled(original);
                isCorrect = currentQuestion.isCorrectAnswer(answerIndex);
            }

            if (isCorrect) {
                gameState.incrementCorrectAnswers();
            }

            gameState.incrementQuestionIndex();

            if (gameState.getCurrentQuestionIndex() >= gameState.getQuestions().size()) {
                gameState.setEndTime(System.currentTimeMillis());

                if (opponentState != null && opponentState.getCurrentQuestionIndex() >= opponentState.getQuestions().size()) {
                    showDuelResults(chatId, opponentChatId, bot);
                } else {
                    String waitingMessage = "✅ Вы завершили игру!\n" +
                            "Ожидаем завершения противника...";
                    bot.execute(createMessage(chatId, waitingMessage));
                }
            } else {
                QuestionSender questionSender = new QuestionSender();
                questionSender.sendQuestion(chatId, gameState, bot);
            }

        } catch (Exception e) {
            System.err.println("[ERR Duel Answer]: " + e.getMessage());
            try {
                bot.execute(createMessage(chatId, "❌ Ошибка при обработке ответа: " + e.getMessage()));
            } catch (TelegramApiException ex) {
                System.err.println("[ERR Sending Error]: " + ex.getMessage());
            }
        }
    }

    private void showDuelResults(long player1ChatId, long player2ChatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState player1State = userSessionService.getGameState(player1ChatId);
        GameState player2State = userSessionService.getGameState(player2ChatId);

        if (player1State == null || player2State == null) {
            return;
        }

        if (player2State.getEndTime() == 0) {
            player2State.setEndTime(System.currentTimeMillis());
        }

        String player1Name = userNames.getOrDefault(player1ChatId, "Игрок 1");
        String player2Name = userNames.getOrDefault(player2ChatId, "Игрок 2");

        int player1Score = player1State.getCorrectAnswers();
        int player2Score = player2State.getCorrectAnswers();

        long player1Time = player1State.getGameDuration();
        long player2Time = player2State.getGameDuration();

        String time1 = gameFinisher.formatTime(player1Time);
        String time2 = gameFinisher.formatTime(player2Time);

        String resultMessage;
        if (player1Score > player2Score) {
            resultMessage = "🏆 <b>ПОБЕДИТЕЛЬ: " + player1Name + "</b>\n\n";
        } else if (player2Score > player1Score) {
            resultMessage = "🏆 <b>ПОБЕДИТЕЛЬ: " + player2Name + "</b>\n\n";
        } else {
            if (player1Time < player2Time) {
                resultMessage = "🏆 <b>ПОБЕДИТЕЛЬ: " + player1Name + "</b>\n" +
                        "(победа по времени при равных очках)\n\n";
            } else if (player2Time < player1Time) {
                resultMessage = "🏆 <b>ПОБЕДИТЕЛЬ: " + player2Name + "</b>\n" +
                        "(победа по времени при равных очках)\n\n";
            } else {
                resultMessage = "🤝 <b>НИЧЬЯ!</b>\n" +
                        "Оба игрока набрали одинаковое количество очков за одинаковое время!\n\n";
            }
        }

        resultMessage += "📊 <b>Результаты дуэли:</b>\n\n" +
                player1Name + ":\n" +
                "✅ " + player1Score + "/" + player1State.getQuestions().size() + " правильных ответов\n" +
                "⏱ Время: " + time1 + "\n\n" +
                player2Name + ":\n" +
                "✅ " + player2Score + "/" + player2State.getQuestions().size() + " правильных ответов\n" +
                "⏱ Время: " + time2 + "\n\n" +
                "🎮 Хотите сыграть ещё?\n" +
                "/play - начать новую игру";

        bot.execute(createMessage(player1ChatId, resultMessage));
        bot.execute(createMessage(player2ChatId, resultMessage));

        userSessionService.removeGameState(player1ChatId);
        userSessionService.removeGameState(player2ChatId);
        userNames.remove(player1ChatId);
        userNames.remove(player2ChatId);
    }

    public String getPendingDuelCategory(long chatId) {
        DuelRequest duelRequest = pendingDuels.get(chatId);
        if (duelRequest != null) {
            return duelRequest.getCategory();
        }
        else {
            return null;
        }
    }

    public void cancelDuel(long chatId, TelegramLongPollingBot bot, boolean showCancelMessage) throws TelegramApiException {
        DuelRequest duelRequest = pendingDuels.remove(chatId);

        if (duelRequest != null) {
            String category = duelRequest.getCategory();
            List<Long> queue = duelQueue.get(category);

            if (queue != null) {
                queue.remove(chatId);
                if (queue.isEmpty()) {
                    duelQueue.remove(category);
                }
            }

            if (showCancelMessage) {
                SendMessage message = createMessage(chatId, "❌ Поиск дуэли отменен.");
                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                keyboardMarkup.setKeyboard(new ArrayList<>());
                message.setReplyMarkup(keyboardMarkup);
                bot.execute(message);
            }
        } else if (showCancelMessage) {
            bot.execute(createMessage(chatId, "ℹ️ У вас нет активного поиска дуэли."));
        }
    }

}