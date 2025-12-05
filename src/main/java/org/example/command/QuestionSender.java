package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.example.util.MessageUtils.createMessage;

public class QuestionSender {

    public void sendQuestion(long chatId, GameState gameState, TelegramLongPollingBot bot) throws TelegramApiException {
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

        if (currentQuestion.getImage() != null && !currentQuestion.getImage().isEmpty()) {
            sendPhotoQuestion(chatId, questionText, currentQuestion.getImage(), keyboardMarkup, bot);
        } else {
            SendMessage message = createMessage(chatId, questionText);
            message.setReplyMarkup(keyboardMarkup);
            bot.execute(message);
        }
    }

    private void sendPhotoQuestion(long chatId, String caption, String imageUrl, InlineKeyboardMarkup keyboardMarkup, TelegramLongPollingBot bot)
            throws TelegramApiException {
        try {
            if (imageUrl != null) {
                SendPhoto photoMessage = new SendPhoto();
                photoMessage.setChatId(String.valueOf(chatId));
                photoMessage.setPhoto(new InputFile(imageUrl));
                photoMessage.setCaption(caption);
                photoMessage.setReplyMarkup(keyboardMarkup);
                bot.execute(photoMessage);
            } else {
                SendMessage message = createMessage(chatId, caption);
                message.setReplyMarkup(keyboardMarkup);
                bot.execute(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            SendMessage message = createMessage(chatId, caption + "\n\n[Ошибка загрузки изображения по ссылке]");
            message.setReplyMarkup(keyboardMarkup);
            bot.execute(message);
        }
    }
}