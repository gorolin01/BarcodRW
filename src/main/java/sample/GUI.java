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
import java.net.URL;

public class GUI {
    private File excelFile;
    private DefaultTableModel model;
    private boolean isTableModelListenerEnabled = true;
    //массив для хранения результатов поиска, третьим аргументом передаем номер строки из исходной таблицы.
    private Object [][] data = new Object[20][3];
    private Excel OrderExcel;

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
        final String[] columnNames = {"Barcode", "Product Name"};
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
                    if(data[0][1][0] == null){
                        searchProductInOrder(data[0][0]);
                        try {
                            sendGet("http://192.168.0.193", "1");
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
                OrderExcel.Build("C:\\Users\\User\\Desktop\\OutputReport\\Report+Data.xlsx");
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

        // Add the input fields and table to the GUI
        JPanel inputPanel = new JPanel();
        inputPanel.add(barcodeLabel);
        inputPanel.add(barcodeField);
        inputPanel.add(productLabel);
        inputPanel.add(productField);
        JScrollPane tablePane = new JScrollPane(table);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tablePane, BorderLayout.CENTER);
        mainPanel.add(createReportButton, BorderLayout.WEST);
        mainPanel.add(saveReportButton, BorderLayout.EAST);
        mainPanel.add(loadButton, BorderLayout.SOUTH);
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
    }

    //ищет продукт по штрих коду в файле отчета. Если не находит, то добовляет новый. Итерирует количество
    private void searchProductInOrder(Object [] data){

        String barcod = data[0].toString();

        int row = 0;
        while((!OrderExcel.getCell(row, 0).toString().equals(barcod)) && (!OrderExcel.getCell(row, 0).toString().equals(""))){
            row++;
        }
        if(OrderExcel.getCell(row, 0).toString().equals(barcod)){
            int end = OrderExcel.getCell(row, 3).toString().indexOf(".");
            if(end != -1){
                OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString().substring(0, end)) + 1);
            }else{
                OrderExcel.setCell(row, 3, Integer.parseInt(OrderExcel.getCell(row, 3).toString()) + 1);
            }

        }
        else{
            OrderExcel.addCell(0, data[0].toString());
            OrderExcel.addCell(1, data[1].toString());
            OrderExcel.addCell(3, 1);
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

                    System.out.println(data[j][0]);
                    System.out.println(data[j][1]);
                    System.out.println(data[j][2]);

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

    //метод, который передает через GET запрос строку на определенный url
    //sendGet("https://www.example.com", "data=value"); - пример
    public static void sendGet(String url, String query) throws Exception {
        URL obj = new URL(url + "?" + query);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // print result
        System.out.println(response.toString());
    }

}

