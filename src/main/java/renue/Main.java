package renue;

import renue.search.AutoComplete;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("Не указан столбец для поиска");
                System.exit(1);
            }
            AutoComplete app = new AutoComplete("airports.csv", Integer.parseInt(args[0])-1);
            long startTime, endTime;
            String[][] list;
            Scanner scanner = new Scanner(System.in);
            String searchText;
            while (true) {
                System.out.println("\nВведите строку:");
                searchText = scanner.nextLine();
                if (searchText.equals("!quit")) {
                    break;
                }
                startTime = System.currentTimeMillis();
                list = app.search(searchText);
                endTime = System.currentTimeMillis();
                for (String[] line : list) {
                    System.out.printf("%s[%s]\n", line[0], line[1]);
                }
                System.out.printf("Количество найденных строк: %d - время затраченное на поиск: %dms\n",
                        list.length, endTime - startTime);
            }
        } catch (NumberFormatException e) {
            System.err.println("Неверно указан столбец для поиска: " + e.getLocalizedMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("\nПроблемы при чтении файла: " + e.getLocalizedMessage());
            System.exit(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("\nУказанный столбец не найден: " + e.getLocalizedMessage());
            System.exit(1);
        }
    }
}