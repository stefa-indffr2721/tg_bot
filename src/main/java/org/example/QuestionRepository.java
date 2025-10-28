package org.example;

import java.util.*;

// Класс для хранения вопросов по категориям
public class QuestionRepository {
    private final Map<String, List<QuizQuestion>> questionsByCategory;

    public QuestionRepository() {
        this.questionsByCategory = new HashMap<>();
        initializeQuestions();
    }

    private void initializeQuestions() {
        questionsByCategory.put("География", Arrays.asList( 
                new QuizQuestion("Столица Франции?",
                        Arrays.asList("Лондон", "Париж", "Берлин"), 1),
                new QuizQuestion("Самая длинная река в мире?", 
                        Arrays.asList("Амазонка", "Нил", "Янцзы"), 0), 
                new QuizQuestion("Столица Японии?", 
                        Arrays.asList("Пекин", "Сеул", "Токио"), 2), 
                new QuizQuestion("Самая высокая гора в мире?", 
                        Arrays.asList("Килиманджаро", "Эверест", "Монблан"), 1), 
                new QuizQuestion("Сколько океанов на Земле?", 
                        Arrays.asList("4", "5", "6"), 1) 
        )); 

        questionsByCategory.put("Наука", Arrays.asList( 
                new QuizQuestion("Сколько планет в Солнечной системе?",
                        Arrays.asList("8", "9", "10"), 0),
                new QuizQuestion("Химическая формула воды?",
                        Arrays.asList("CO2", "H2O", "O2"), 1),
                new QuizQuestion("Какая планета известна кольцами?", 
                        Arrays.asList("Юпитер", "Сатурн", "Уран"), 1), 
                new QuizQuestion("Сколько костей в теле взрослого человека?", 
                        Arrays.asList("206", "210", "215"), 0), 
                new QuizQuestion("Какой газ растения поглощают из атмосферы?", 
                        Arrays.asList("Кислород", "Азот", "Углекислый газ"), 2) 
        )); 

        questionsByCategory.put("Литература", Arrays.asList( 
                new QuizQuestion("Кто написал 'Войну и мир'?",
                        Arrays.asList("Достоевский", "Толстой", "Чехов"), 1),
                new QuizQuestion("Автор 'Гарри Поттера'?", 
                        Arrays.asList("Дж. Р. Р. Толкин", "Дж. К. Роулинг", "К. С. Льюис"), 1), 
                new QuizQuestion("Кто написал 'Преступление и наказание'?", 
                        Arrays.asList("Толстой", "Достоевский", "Гоголь"), 1), 
                new QuizQuestion("Автор 'Мастер и Маргарита'?", 
                        Arrays.asList("Булгаков", "Пастернак", "Набоков"), 0), 
                new QuizQuestion("Кто написал 'Евгений Онегин'?", 
                        Arrays.asList("Лермонтов", "Пушкин", "Тургенев"), 1) 
        )); 

        questionsByCategory.put("Животные", Arrays.asList( 
                new QuizQuestion("Самое большое млекопитающее?",
                        Arrays.asList("Слон", "Синий кит", "Жираф"), 1),
                new QuizQuestion("Сколько жизней у кошки?", 
                        Arrays.asList("7", "9", "6"), 1), 
                new QuizQuestion("Какое животное самое быстрое на суше?", 
                        Arrays.asList("Лев", "Гепард", "Антилопа"), 1), 
                new QuizQuestion("Сколько глаз у паука?", 
                        Arrays.asList("6", "8", "10"), 1), 
                new QuizQuestion("Какое животное может поворачивать голову на 360 градусов?", 
                        Arrays.asList("Филин", "Хамелеон", "Сова"), 0) 
        )); 

        questionsByCategory.put("История", Arrays.asList( 
                new QuizQuestion("В каком году началась Вторая мировая война?", 
                        Arrays.asList("1939", "1941", "1945"), 0), 
                new QuizQuestion("Первый человек в космосе?", 
                        Arrays.asList("Нил Армстронг", "Юрий Гагарин", "Алан Шепард"), 1), 
                new QuizQuestion("Кто был первым президентом США?", 
                        Arrays.asList("Томас Джефферсон", "Джордж Вашингтон", "Авраам Линкольн"), 1), 
                new QuizQuestion("В каком году пала Берлинская стена?", 
                        Arrays.asList("1987", "1989", "1991"), 1), 
                new QuizQuestion("Древнеримский гладиатор, возглавивший восстание?", 
                        Arrays.asList("Юлий Цезарь", "Спартак", "Ганнибал"), 1) 
        )); 
    } 

    public List<String> getAvailableCategories() {
        return new ArrayList<>(questionsByCategory.keySet());
    }

    public List<QuizQuestion> getQuestionsByCategory(String category) {
        return questionsByCategory.getOrDefault(category, new ArrayList<>());
    }
}