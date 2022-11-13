package sk.vinf.wikitranslator;

import java.util.List;
import java.util.Scanner;

public class Main 
{
    private static String getLang(List<String> langs) {
        var scanner = new Scanner(System.in);
        var lang = "";
        while (!langs.contains(lang)) {
            System.out.println("Enter language from " + langs);
            try {
                lang = scanner.nextLine();
            } catch (Exception e) {
                continue;
            }
        }
        scanner.close();
        return lang;
    }

    public static void main(String[] args)
    {
        var scanner = new Scanner(System.in);
        var input = 0;

        System.out.println("1. exit");
        System.out.println("2. find article ID pairs");
        System.out.println("3. create sk-cs-hu ID conjunction with Spark");
        System.out.println("4. create docs with Spark");
        System.out.println("5. create ID mapping");
        System.out.println("6. create Lucene index");
        System.out.println("7. use translation search");
        
        while (true) {
            try {
                input = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Enter integer!");
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                scanner.close();
                return;
            }
        }
        switch (input) {
            case 1:
                break;
            case 2:
                try {
                    var translationFinder = new TranslationFinder(
                        getLang(List.of("cs", "hu"))
                    );
                    translationFinder.find();
                    translationFinder.close();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 3:
                try {
                    TranslationFinder.conjunctionSpark();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error");
                }
                break;
            case 4:
                try {
                    var documentCleaner = new DocumentParser();
                    documentCleaner.createDocsSpark();
                    documentCleaner.close();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error");
                }
                break;
            case 5:
                try {
                    var mapper = new TranslationMapper();
                    mapper.mapLanguages();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 6:
                try {
                    var luceneIndexer = new LuceneManager();
                    luceneIndexer.indexLanguage(getLang(
                        List.of("sk", "cs", "hu")
                    ));
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error");
                }
                break;
            case 7:
                try {
                    var luceneManager = new LuceneManager();
                    luceneManager.start();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            default:
                break;
        }

        scanner.close();
    }
}
