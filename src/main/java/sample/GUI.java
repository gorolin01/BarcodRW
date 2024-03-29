package sample;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.io.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GUI {
    private File excelFile;         //файл с штрих кодами
    private File excelFilePrice;    //файл с ценами
    private XSSFWorkbook WBPrice;
    private DefaultTableModel model;
    private boolean isTableModelListenerEnabled = true;
    //массив для хранения результатов поиска, третьим аргументом передаем номер строки из исходной таблицы.
    private Object [][] data = new Object[20][5];
    private Excel OrderExcel;
    private JCheckBox transmitPrice;
    //private static ProgressBar progressTransmitBar;
    //private static ProcessSpinnerBar processSpinnerBar;

    //настройки
    private String IP_ARDUINO = "http://192.168.0.137"; //ip адрес ардуино
    //const
    private String OUTPUT_FILE = "C:\\Users\\User\\Desktop\\OutputReport\\";   //файл отчета

    public void createAndShowGUI() {
        // Create the frame
        JFrame frame = new JFrame("Inventory Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        // Create the input fields
        JLabel barcodeLabel = new JLabel("Barcode:");
        final JTextField barcodeField = new JTextField(20);
        final JLabel productLabel = new JLabel("Product Name:");
        final JTextField productField = new JTextField(20);

        // Create the table
        final String[] columnNames = {"Barcode", "Product Name", "Position", "Articles", "Count"};
        final Object[][][] data = {new Object[0][2]};
        model = new DefaultTableModel(data[0], columnNames);
        final JTable table = new JTable(model);

        //слушатель barcodeField
        barcodeField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    isTableModelListenerEnabled = false;
                    resetData();
                    data[0] = searchAndLoadProduct(barcodeField.getText(),columnNames,table,0);

                    //если найден один результат, то записываем его в очтет
                    if((data[0][1][0] == null) && (data[0][0][0] != null)){
                        try {
                            //String query = "name=" + data[0][0][1] + ",count=" + data[0][0][0];
                            //ТУТ нужно добавить третий передоваемый параметр (цена)
                            String query = "";
                            if(transmitPrice.isSelected()){
                                query = "name=" + data[0][0][1].toString() + "&barcod=" + data[0][0][0].toString() + "&price=" + getPrice(data[0][0][1].toString()); //передаем только наименование товара, бар код и цену
                            }else{
                                query = "name=" + data[0][0][1].toString() + "&barcod=" + data[0][0][0].toString(); //передаем только наименование товара и бар код
                            }
                            //ОТЛАДКА
                            System.out.println(query);
                            sendGet(IP_ARDUINO, query);
                            searchProductInOrder(data[0][0]);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }else{
                        try {
                            sendGet(IP_ARDUINO, "0");   //товар не найден, 0 - код ошибки
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                    model.setDataVector(data[0], columnNames);
                    model.fireTableDataChanged();
                    table.setModel(model);
                    barcodeField.setText("");   //обнуляем поле после выполнения поиска
                    isTableModelListenerEnabled = true;

                }
            }
        });

        //слушатель productField
        productField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    isTableModelListenerEnabled = false;
                    resetData();
                    data[0] = searchAndLoadProduct(productField.getText(),columnNames,table,1);
                    model.setDataVector(data[0], columnNames);
                    model.fireTableDataChanged();
                    table.setModel(model);
                    productField.setText("");   //обнуляем поле после выполнения поиска
                    isTableModelListenerEnabled = true;

                }
            }
        });

        // Create the create_report button
        JButton createReportButton = new JButton("CreateReport");
        createReportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OrderExcel = new Excel();
                OrderExcel.createExcel();
            }
        });

        // Create the save_report button
        JButton saveReportButton = new JButton("SaveReport");
        saveReportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OrderExcel.Build(OUTPUT_FILE + "Report_" + getCurrentDateTime() + ".xlsx");
            }
        });

        // Create the load button
        JButton loadButton = new JButton("Load Excel File");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {

                    excelFile = fileChooser.getSelectedFile();
                    data[0] = loadDataFromExcel();
                    //model.setDataVector(data[0], columnNames);    //загрузает документ в таблицу при старте

                }
            }
        });

        // Create the loadPrice button
        JButton loadPriceButton = new JButton("Load Price");
        loadPriceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {

                    excelFilePrice = fileChooser.getSelectedFile();
                    loadPrice();
                    //data[0] = loadDataFromExcel();

                }
            }
        });

        transmitPrice = new JCheckBox("Transmit Price");
        //progressTransmitBar = new ProgressBar();
        //processSpinnerBar = new ProcessSpinnerBar();

        // Add the input fields and table to the GUI
        JPanel inputPanel = new JPanel();
        JPanel bottomPanel = new JPanel();
        inputPanel.add(barcodeLabel);
        inputPanel.add(barcodeField);
        inputPanel.add(productLabel);
        inputPanel.add(productField);
        bottomPanel.add(loadButton);
        bottomPanel.add(loadPriceButton);
        bottomPanel.add(transmitPrice);
        //bottomPanel.add(progressTransmitBar.getPanel());  //не работает! Не обновляет JFrame во время выполнения кода слушателя(не отображает промежуточные этапы).
        //bottomPanel.add(processSpinnerBar.getPanel());    //не работает!
        JScrollPane tablePane = new JScrollPane(table);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tablePane, BorderLayout.CENTER);
        mainPanel.add(createReportButton, BorderLayout.WEST);
        mainPanel.add(saveReportButton, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(mainPanel);

        // Add a TableModelListener to the table's model
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if ((e.getType() == TableModelEvent.UPDATE) && isTableModelListenerEnabled) {
                    saveDataToExcel();
                }
            }
        });

        // Show the GUI
        frame.setVisible(true);

        TreedGetRequest();
    }

    //ищет в файле отчета товар по ШК и возвращает его количество
    private int searchProductInOrder(String barcode){

        int result = 0;
        if(!barcode.equals("")) {
            int row = 0;
            while ((!OrderExcel.getCell(row, 0).toString().equals(barcode)) && (!OrderExcel.getCell(row, 0).toString().equals(""))) {
                row++;
            }
            if (OrderExcel.getCell(row, 0).toString().equals(barcode)) {
                int end = OrderExcel.getCell(row, 3).toString().indexOf(".");
                if(end != -1){
                    result = Integer.parseInt(OrderExcel.getCell(row, 3).toString().substring(0, end));
                }else{
                    result = Integer.parseInt(OrderExcel.getCell(row, 3).toString());    //столбец количества товара
                }
            }
        }
        return result;
    }

    //ищет продукт по штрих коду в файле отчета. Если не находит, то добовляет новый. Итерирует количество
    private void searchProductInOrder(Object [] data){

        String barcode = data[0].toString();

        int row = 0;
        while((!OrderExcel.getCell(row, 0).toString().equals(barcode)) && (!OrderExcel.getCell(row, 0).toString().equals(""))){
            row++;
        }
        if(OrderExcel.getCell(row, 0).toString().equals(barcode)){
            int end = OrderExcel.getCell(row, 3).toString().indexOf(".");
            if(end != -1){
                OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString().substring(0, end)) + 1);
            }else{
                OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString()) + 1);    //столбец количества товара
            }

        }
        else{
            OrderExcel.addCell(0, data[0].toString());
            OrderExcel.addCell(1, data[1].toString());
            OrderExcel.addCell(3, 1);
        }

    }

    //принимает бар код строкой и количество товара, которое нужно добавить в отчет к уже существующему там товару
    private void searchProductInOrder(String barcode, int count){

        if(!barcode.equals("")) {
            int row = 0;
            while ((!OrderExcel.getCell(row, 0).toString().equals(barcode)) && (!OrderExcel.getCell(row, 0).toString().equals(""))) {
                row++;
            }
            if (OrderExcel.getCell(row, 0).toString().equals(barcode)) {
                int end = OrderExcel.getCell(row, 3).toString().indexOf(".");
                if (end != -1) {
                    OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString().substring(0, end)) + count);
                } else {
                    OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString()) + count);    //столбец количества товара
                }

            } else {
                //если вдруг такого товара не было в отчете(вообще такого быть не должно!)
                OrderExcel.addCell(0, data[row][0].toString());
                OrderExcel.addCell(1, data[row][1].toString());
                OrderExcel.addCell(3, count);
            }
        }

    }

    private Object[][] loadDataFromExcel() {
        try {
            // Open the Excel file
            FileInputStream inputStream = new FileInputStream(excelFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            // Get the first sheet
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Get the number of rows
            int rowCount = sheet.getPhysicalNumberOfRows();

            // Create a 2D array to store the data
            Object[][] data = new Object[rowCount][2];

            // Iterate through the rows and add the data to the array
            for (int i = 0; i < rowCount; i++) {
                XSSFRow row = sheet.getRow(i);
                for (int j = 0; j < 2; j++) {
                    XSSFCell cell = row.getCell(j);
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case NUMERIC:
                                data[i][j] = cell.getNumericCellValue();
                                break;
                            case STRING:
                                data[i][j] = cell.getStringCellValue();
                                break;
                            case BOOLEAN:
                                data[i][j] = cell.getBooleanCellValue();
                                break;
                            case FORMULA:
                                data[i][j] = cell.getCellFormula();
                                break;
                            case BLANK:
                                data[i][j] = "";
                                break;
                            case _NONE:
                            case ERROR:
                            default:
                                data[i][j] = "";
                                break;
                        }
                    } else {
                        // обработка случая, когда ячейка не существует
                    }
                }
            }
            // Close the input stream
            inputStream.close();
            workbook.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            //handle the exception
            JOptionPane.showMessageDialog(null, "Error loading file. Please check the file and try again.", "File Load Error", JOptionPane.ERROR_MESSAGE);
            return new Object[0][2];
        }
    }

    private Object[][] searchAndLoadProduct(String productName, String[] columnNames, JTable table, int SearchColumn) {
        if(productName.equals("")){return new Object[0][2];}
        try {
            // Open the Excel file
            FileInputStream inputStream = new FileInputStream(excelFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            // Get the first sheet
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Create a new table model
            DefaultTableModel newModel = new DefaultTableModel(new Object[0][2], columnNames);
            //int rowCount = sheet.getPhysicalNumberOfRows();
            //Object[][] data = new Object[rowCount][2];

            // Iterate through the rows and add the matching rows to the new model
            int j = 0;
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                XSSFRow row = sheet.getRow(i);
                String product = row.getCell(SearchColumn).getStringCellValue();
                if (product.toLowerCase().contains(productName.toLowerCase())) {
                    /*String name = row.getCell(0).getStringCellValue();
                    String code = row.getCell(1).getStringCellValue();
                    System.out.println(name);
                    newModel.addRow(new Object[]{name, code});*/
                    data[j][0] = row.getCell(0).getStringCellValue();
                    data[j][1] = row.getCell(1).getStringCellValue();
                    data[j][2] = row.getRowNum();
                    data[j][4] = searchProductInOrder(data[j][0].toString());

                    System.out.println(data[j][0]);
                    System.out.println(data[j][1]);
                    System.out.println(data[j][2]);
                    System.out.println(data[j][4]);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>");

                    j++;
                }
            }

            // Set the new model to the table
            table.setModel(newModel);

            // Close the input stream
            inputStream.close();
            workbook.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            //handle the exception
            JOptionPane.showMessageDialog(null, "Error loading file. Please check the file and try again.", "File Load Error", JOptionPane.ERROR_MESSAGE);
        }
        return new Object[0][2];
    }

    private void resetData(){
        for(int i = 0; i < data.length; i++){
            for(int j = 0; j < data[i].length; j++){
                data[i][j] = null;
            }
        }
    }

    private void saveDataToExcel() {
        if(data[0][0] == null){return;}
        try {
            FileInputStream inputStream = new FileInputStream(excelFile);
            XSSFWorkbook workbookFile = new XSSFWorkbook(inputStream);

            // Get the first sheet
            XSSFSheet sheetFile = workbookFile.getSheetAt(0);

            //----------------------------------------------------------//

            // Create a new workbook
            XSSFWorkbook workbook = new XSSFWorkbook();

            // Create a new sheet
            XSSFSheet sheet = workbook.createSheet("Inventory");

            //перепишем изменения model в data
            for(int i = 0; i < model.getRowCount(); i++){
                for(int j = 0; j < model.getColumnCount(); j++){
                    data[i][j] = model.getValueAt(i, j);
                }
            }

            // Iterate through the data and add it to the sheet
            for (int i = 0; i < model.getRowCount(); i++) {

                if(data[i][0] != null){ //если массив пустой - не записываем
                    //вставляем в старый файл новые скоректированные значения только в те ячейки, которые изменились
                    sheetFile.getRow((Integer) data[i][2]).getCell(0).setCellValue((String) data[i][0]);
                    sheetFile.getRow((Integer) data[i][2]).getCell(1).setCellValue((String) data[i][1]);
                }

            }

            // Iterate through the data and add it to the sheet
/*            for (int i = 0; i < model.getRowCount(); i++) {
                XSSFRow row = sheet.createRow(i);
                for (int j = 0; j < model.getColumnCount(); j++) {
                    XSSFCell cell = row.createCell(j);
                    Object value = model.getValueAt(i, j);
                    if(value != null){
                        if (value instanceof Double) {
                            cell.setCellValue(Double.toString((Double) value));
                        } else {
                            cell.setCellValue((String) value);
                        }
                    }else{
                        data = loadDataFromExcel();
                        if (value instanceof Double) {
                            cell.setCellValue(Double.toString((Double) data[i][j]));
                        } else {
                            cell.setCellValue((String) data[i][j]);
                        }
                    }else{
                        
                    }
                }
            }*/

            // Write the data to the Excel file
            FileOutputStream outputStream = new FileOutputStream(excelFile);
            //workbook.write(outputStream);
            workbookFile.write(outputStream);
            outputStream.close();
            workbookFile.close();
            //workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String DateTime = formatter.format(date);
        DateTime = DateTime.replaceAll("/", "_").replaceAll(":", ";");
        return DateTime;
    }

    //загружает файл с ценами
    private void loadPrice(){
        try {
            // Open the Excel file
            FileInputStream inputStream = new FileInputStream(excelFilePrice);
            WBPrice = new XSSFWorkbook(inputStream);

            // Close the input stream
            inputStream.close();
            //WBPrice.close();
        } catch (IOException e) {
            e.printStackTrace();
            //handle the exception
            JOptionPane.showMessageDialog(null, "Error loading file. Please check the file and try again.", "File Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //метод ищет товар по нзванию в файле с ценами возвращает цену
    private String getPrice(String productName){
        //номер столбца с ценой
        int NPrice = 4;

        //номер столбца с наименованием
        int NName = 0;

        String res = "";
        // Get the first sheet
        XSSFSheet sheet = WBPrice.getSheetAt(0);

        // Iterate through the rows and add the matching rows to the new model
        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            XSSFRow row = sheet.getRow(i);
            if(row.getCell(NName) != null){     // пропускает пустые строки с наименованием
                String product = row.getCell(NName).getStringCellValue();
                if (product.toLowerCase().contains(productName.toLowerCase())) {
                    res = Double.toString(row.getCell(NPrice).getNumericCellValue());
                    break;
                }
            }
        }

        //WBPrice.close();
        return res;
    }

    //метод, который передает через GET запрос строку на определенный url
    //sendGet("https://www.example.com", "data=value"); - пример
    public static void sendGet(String url, String query) {
        try {

            URL obj = new URL(url + "/" + query);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            /*System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response.toString());*/

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            //добавить обновление статуса передачи
        }
    }

    //создает в отдельном потоке обьект класса GetRequestServer, кторый слушает порт на наличие GET запросов
    public void TreedGetRequest(){

        int port = 8021;
        final GetRequestServer server = new GetRequestServer(port);
        Thread thread = new Thread(server);
        thread.start();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String requestBody = server.getRequestBody();
                    if (requestBody != null) {
                        //тут мы получаем в ответ чего и на сколько нужно увеличить в отчете
                        String count = requestBody.substring(5, requestBody.indexOf("&barcod="));
                        String barCod = requestBody.substring(requestBody.indexOf("&barcod=") + 8, requestBody.indexOf("HTTP/1.1") - 1);

                        searchProductInOrder(barCod, Integer.parseInt(count)); //добавляем нужное количество товара в отчет, которое прислала ардуино

                        //тестовый вывод
                        System.out.println("Request : ");
                        System.out.println("    Count : " + count);
                        System.out.println("    Barcod : " + barCod);
                        server.resetRequestBody();
                    }
                }

            }
        });
        serverThread.start();

    }

}

