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

    private void sendPhotoQuestion(long chatId, String caption, String imageName, InlineKeyboardMarkup keyboardMarkup, TelegramLongPollingBot bot)
            throws TelegramApiException {
        try {
            String resourcesPath = "src/main/resources/";
            File imageFile = new File(resourcesPath + imageName);

            if (imageFile.exists()) {
                SendPhoto photoMessage = new SendPhoto();
                photoMessage.setChatId(String.valueOf(chatId));
                photoMessage.setPhoto(new InputFile(imageFile, imageName));
                photoMessage.setCaption(caption);
                photoMessage.setReplyMarkup(keyboardMarkup);
                bot.execute(photoMessage);
            } else {
                InputStream imageStream = getClass().getClassLoader().getResourceAsStream(imageName);
                if (imageStream != null) {

                    File tempFile = File.createTempFile("telegram_bot_", "_" + imageName);
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = imageStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    SendPhoto photoMessage = new SendPhoto();
                    photoMessage.setChatId(String.valueOf(chatId));
                    photoMessage.setPhoto(new InputFile(tempFile, imageName));
                    photoMessage.setCaption(caption);
                    photoMessage.setReplyMarkup(keyboardMarkup);
                    bot.execute(photoMessage);

                    tempFile.deleteOnExit();
                } else {
                    SendMessage message = createMessage(chatId, caption + "\n\n[Изображение недоступно]");
                    message.setReplyMarkup(keyboardMarkup);
                    bot.execute(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending photo: " + e.getMessage());
            e.printStackTrace();
            SendMessage message = createMessage(chatId, caption + "\n\n[Ошибка загрузки изображения]");
            message.setReplyMarkup(keyboardMarkup);
            bot.execute(message);
        }
    }
}