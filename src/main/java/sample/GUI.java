package sample;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.io.*;
import org.apache.poi.xssf.usermodel.*;

public class GUI {
    private File excelFile;
    private DefaultTableModel model;

    public void createAndShowGUI() {
        // Create the frame
        JFrame frame = new JFrame("Inventory Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

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

        productField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchAndLoadProduct(productLabel.getText(),columnNames,table);
                }
            }
        });

        // Create the save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String barcode = barcodeField.getText();
                String product = productField.getText();

                // Add the new data to the table
                model.addRow(new Object[] {barcode, product});

                // Save the data to the Excel spreadsheet
                saveDataToExcel();
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
                    model.setDataVector(data[0], columnNames);
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
        mainPanel.add(saveButton, BorderLayout.SOUTH);
        mainPanel.add(loadButton, BorderLayout.WEST);
        frame.add(mainPanel);

        // Add a TableModelListener to the table's model
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    saveDataToExcel();
                }
            }
        });

        // Show the GUI
        frame.setVisible(true);
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

    private void searchAndLoadProduct(String productName, String[] columnNames, JTable table) {
        try {
            // Open the Excel file
            FileInputStream inputStream = new FileInputStream(excelFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            // Get the first sheet
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Create a new table model
            DefaultTableModel newModel = new DefaultTableModel(new Object[0][2], columnNames);

            // Iterate through the rows and add the matching rows to the new model
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                XSSFRow row = sheet.getRow(i);
                String product = row.getCell(0).getStringCellValue();
                if (product.toLowerCase().contains(productName.toLowerCase())) {
                    String name = row.getCell(0).getStringCellValue();
                    String code = row.getCell(1).getStringCellValue();
                    System.out.println(name);
                    newModel.addRow(new Object[]{name, code});
                }
            }

            // Set the new model to the table
            table.setModel(newModel);

            // Close the input stream
            inputStream.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
            //handle the exception
            JOptionPane.showMessageDialog(null, "Error loading file. Please check the file and try again.", "File Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }



    private void saveDataToExcel() {
        try {
            // Create a new workbook
            XSSFWorkbook workbook = new XSSFWorkbook();

            // Create a new sheet
            XSSFSheet sheet = workbook.createSheet("Inventory");

            // Iterate through the data and add it to the sheet
            for (int i = 0; i < model.getRowCount(); i++) {
                XSSFRow row = sheet.createRow(i);
                for (int j = 0; j < model.getColumnCount(); j++) {
                    XSSFCell cell = row.createCell(j);
                    Object value = model.getValueAt(i, j);
                    if (value instanceof Double) {
                        cell.setCellValue(Double.toString((Double) value));
                    } else {
                        cell.setCellValue((String) value);
                    }
                }
            }

            // Write the data to the Excel file
            FileOutputStream outputStream = new FileOutputStream(excelFile);
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
