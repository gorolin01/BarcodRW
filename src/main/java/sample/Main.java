package sample;

import java.io.InputStream;
import java.util.Scanner;

public class Main {

    private static String shop = "all";

    static Excel BARCODE = new Excel();
    static Excel excel = new Excel();
    static Excel Result = new Excel();

    public static void main(String[] args) {

        // Create the GUI
        GUI gui = new GUI();
        gui.createAndShowGUI();

        /*BARCODE.createExcel("C:\\Users\\Server\\Desktop\\BARCODE.xlsx", 0);
        excel.createExcel("C:\\Users\\Server\\Desktop\\Номенклатура_24.01.23.xlsx", 0);
        Result.createExcel();
        stocktaking();*/

    }

    public static void stocktaking(){

        while(true){
            Scanner scanner = new Scanner(System.in);
            String barCodeStr = scanner.next();

            //условие выхода
            if(barCodeStr.equals("exit")){
                CreateFile();
                break;
            }

            String response = findProduct(findProductFromBarCode(barCodeStr));
            WriteProductOnFile(response);
            System.out.println(response);
        }

    }

    //запись каждого пробитого продукта в Result(Excel)
    public static void WriteProductOnFile(String res){

        //проверка на повторяющиеся позиции
        int row = 0;
        boolean flag = true;   //флаг наличия дублеката
        while(!Result.getCell(row, 0).toString().equals("")){
            if(Result.getCell(row, 0).toString().equals(res)){
                //если найден повтор, то инкрементируем его количество
                Result.setCell(row, 1, Integer.parseInt(Result.getCell(row, 1).toString().substring(0,
                        Result.getCell(row, 1).toString().indexOf("."))) + 1);
                flag = false;
                row++;  //нужно для того, чтобы if с добавлением новой карточки не сработал)
                break;
            }
            row++;
        }

        //если не найден дубликат, то добавляем новую строку
        if(flag || (row == 0)){
            Result.setCell(row, 0, res);
            Result.setCell(row, 1, 1);
        }


    }

    //В конце создает файл в указанной папке
    public static void CreateFile(){

        //в конце компануем excel файл с результатом
        Result.Build("C:\\Users\\Server\\Desktop\\res\\res.xlsx");

    }

    public static String findProductFromBarCode(String BarCode){

        int i = 7;  //начало номенклатуры
        String result = "";
        while(!BARCODE.getCell(i, 0).toString().equals("")){

            if(BARCODE.getCell(i, 0).toString().equals(BarCode)){
                return BARCODE.getCell(i, 3).toString();
            }

            i++;

        }

        return "Штрихкод не найден!";
    }

    static String findProduct(String search_string){

        int i = 6;  //начало номенклатуры
        String result = "";
        while(!excel.getCell(i, 1).toString().equals("")){

            //проверка принадлежности товара к разным магазинам
            if(!shop.equals("all")) {
                if (!excel.getCell(i, 0).toString().equals(shop)) {
                    i++;
                    continue;
                }
            }

            String [] search_expression = search_string.split(" ");
            int counter = 0;

            //проверяем каждое слово из искомого выражения. В искомом тексте должны быть эти слова в любом порядке
            for(int j = 0; j < search_expression.length; j++){
                if(excel.getCell(i, 0).toString().toLowerCase().contains(search_expression[j].toLowerCase())){
                    counter++;
                }
            }
            if(counter == search_expression.length){    //проверка, все ли слова есть в искомом выражении
                result += excel.getCell(i, 0).toString() /*+ "\n" + " Цена: " + excel.getCell(i, 4).toString() + " Руб." + "\n\n"*/;    //возврат наименование + цена
            }

            i++;
        }
        if(result.equals("")){
            return "Товар не найден.";
        }else{
            //System.out.println(result);
            return result;
        }

    }

}
