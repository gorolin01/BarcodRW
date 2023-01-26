package sample;

import com.sun.media.sound.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Excel {

    private String filename;
    private Integer Sheet;
    private Integer Row;
    private Integer Column;
    private XSSFWorkbook WorkbookExcel;
    private XSSFSheet SheetExcel;
    private XSSFRow RowExcel;
    private XSSFCell CellExcel;

    public void createExcel(){

        WorkbookExcel = new XSSFWorkbook();
        SheetExcel = WorkbookExcel.createSheet("1");    //пока только умеет работать с одним листом

    }

    public void createExcel(String filename, Integer Sheet){    //работа с уже созданной книгой

        this.filename = filename;
        this.Sheet = Sheet;
        WorkbookExcel = openBookDirectly(filename);
        SheetExcel = WorkbookExcel.getSheetAt(Sheet);

    }

    public XSSFCell getCell(Integer Row, Integer Column){

        this.Row = Row;
        this.Column = Column;

        RowExcel = SheetExcel.getRow(Row);
        if(RowExcel == null) { RowExcel = SheetExcel.createRow(Row); }

        CellExcel = RowExcel.getCell(Column);
        if(CellExcel == null) { CellExcel = RowExcel.createCell(Column); }

        return CellExcel;
    }

    public void setCell(Integer Row, Integer Column, String data){

        getCell(Row, Column);

        CellExcel.setCellValue(data);

    }

    public void setCell(Integer Row, Integer Column, Integer data){

        getCell(Row, Column);

        CellExcel.setCellValue(data);

    }

    public void Build(String outFilename){

        createBookDirectly(WorkbookExcel, outFilename);

    }

    private static void createBookDirectly(XSSFWorkbook book, String NEW_FILE_NAME) {

        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(new File(NEW_FILE_NAME));
            book.write(fileOut);
            fileOut.flush();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static XSSFWorkbook openBookDirectly(String filename) {

        XSSFWorkbook book = null;
        try {
            book = (XSSFWorkbook) WorkbookFactory.create(new File(filename));
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return book;
    }

}
