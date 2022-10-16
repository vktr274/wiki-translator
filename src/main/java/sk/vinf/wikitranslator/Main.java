package sk.vinf.wikitranslator;

import java.util.Scanner;

public class Main 
{
    private static String getLang() {
        var scanner = new Scanner(System.in);
        var lang = "";
        while (!(lang.equals("cs") || lang.equals("hu"))) {
            System.out.println("Enter language (cs or hu)");
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
        var lang = "";

        System.out.println("1. exit");
        System.out.println("2. find article title pairs");
        System.out.println("3. create sk-cs-hu conjunction");
        System.out.println("4. clear documents");
        System.out.println("5. create forward index");
        System.out.println("6. create inverted index");
        System.out.println("7. use translator");
        
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
                lang = getLang();
                try {
                    var translationFinder = new TranslationFinder(lang);
                    translationFinder.find();
                    translationFinder.close();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                lang = "";
                break;
            case 3:
                try {
                    TranslationFinder.conjuction();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 4:
                try {
                    var documentCleaner = new DocumentCleaner();
                    documentCleaner.clean();
                    documentCleaner.close();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 5:
                try {
                    var indexer = new Indexer();
                    indexer.createForwardIndex();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 6:
                try {
                    var indexer = new Indexer();
                    indexer.createInvertedIndex();
                    System.out.println("Success");
                } catch (Exception e) {
                    System.out.println("Error");
                }
                break;
            case 7:
                try {
                    var searchEngine = new SearchEngine();
                    searchEngine.start();
                } catch (Exception e) {
                    System.out.println("Error");
                }
            default:
                break;
        }

        scanner.close();
    }
}
