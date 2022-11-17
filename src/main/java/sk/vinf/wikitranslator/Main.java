package sk.vinf.wikitranslator;

import java.util.List;
import java.util.Scanner;

public class Main 
{
    private static String getLang(List<String> langs) {
        var scanner = new Scanner(System.in);
        var lang = "";
        while (!langs.contains(lang.toLowerCase())) {
            System.out.println("Enter language from " + langs);
            try {
                lang = scanner.nextLine();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Input error! Check message above.");
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
        System.out.println("5. create Lucene index");
        System.out.println("6. create ID mapping");
        System.out.println("7. use translation search (type 'exit' for quitting search mode)");
        
        while (true) {
            try {
                input = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Enter integer!");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error! Check message above.");
                scanner.close();
                return;
            }
        }
        switch (input) {
            case 1:
                break;
            case 2:
                try {
                    TranslationFinder.find(getLang(List.of("cs", "hu")));
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            case 3:
                try {
                    TranslationFinder.conjunctionSpark();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            case 4:
                try {
                    DocumentParser.createDocsSpark();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            case 5:
                try {
                    LuceneIndexer.indexLanguage(getLang(List.of("sk", "cs", "hu")));
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            case 6:
                try {
                    TranslationMapper.mapLanguages();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            case 7:
                try {
                    var luceneSearch = new LuceneSearch();
                    luceneSearch.start();
                    System.out.println("Success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error! Check message above.");
                }
                break;
            default:
                System.out.println("Unknown command!");
                break;
        }

        scanner.close();
    }
}
