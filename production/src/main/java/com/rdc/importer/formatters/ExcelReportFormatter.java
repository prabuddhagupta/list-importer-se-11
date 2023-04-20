package com.rdc.importer.formatters;

import java.awt.Color;
import java.util.Date;

import com.rdc.importer.formatters.Report;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.Region;

public class ExcelReportFormatter extends GridReportFormatter {
    private int logoRowNum = 0;
    private int titleRowNum = 2;

    private String sheetName = "Sheet1";
    private boolean addLogo = true;
    private boolean addFooter = true;
    private HSSFWorkbook workbook;
    private HSSFSheet sheet;
    private HSSFRow row;
    private HSSFCell cell;
    private HSSFCell footerCell;
    private short footerRowNum;
    private HSSFRow footerRow;
    private HSSFFont footerFont;
    private String footer;
    private HSSFCellStyle headerStyle;
    private HSSFCellStyle footerStyle;
    private HSSFCellStyle titleStyle;
    private int rowNum;
    private short colNum;
    private StringBuffer columnBuffer;
    private String title;
    private short maxColumn;
    private Report.DataType columnType;

    private HSSFCellStyle cellStyleNoWrap;
    private HSSFCellStyle cellStyleWrap;

    public void beginGrid() {
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet(sheetName);
        int contentRowNum;
        if (addLogo) {
            logoRowNum = 0;
            titleRowNum = 2;
            contentRowNum = 4;
        } else {
            logoRowNum = 0;
            titleRowNum = 0;
            contentRowNum = 0;
        }

        row = null;
        cell = null;
        rowNum = contentRowNum;
        colNum = 0;
        maxColumn = 0;

        HSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(font);

        headerStyle = workbook.createCellStyle();
        headerStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(HSSFColor.PALE_BLUE.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        footerStyle = workbook.createCellStyle();
        footerStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        footerStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        footerStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        footerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        footerStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        footerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        footerFont = workbook.createFont();
        footerFont.setFontHeightInPoints((short) 8);
        footerStyle.setFont(footerFont);
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public void setLogo(boolean logo) {
        this.addLogo = logo;
    }

    public void setFooter(boolean footer) {
        this.addFooter = footer;
    }

    public void endGrid() throws Exception {
        if (addFooter) {
            beginFooter();
            beginRow();
            printColumn("RDC Report generated on " + new Date());
            endRow();
            endFooter();
        }
    }

    private HSSFCellStyle createCellStyle(int index, Report.DataType columnType) {

        if (columnType == Report.DataType.WRAPPED_TEXT) {
            if (cellStyleWrap == null) {
                cellStyleWrap = workbook.createCellStyle();
                cellStyleWrap.setBorderTop(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderBottom(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderLeft(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderRight(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setWrapText(true);
            }
            return cellStyleWrap;
        } else if(columnType == Report.DataType.HYPER_LINK){
            if (cellStyleWrap == null) {
                cellStyleWrap = workbook.createCellStyle();
                cellStyleWrap.setBorderTop(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderBottom(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderLeft(HSSFCellStyle.BORDER_THIN);
                cellStyleWrap.setBorderRight(HSSFCellStyle.BORDER_THIN);
                HSSFFont font = workbook.createFont();
                font.setUnderline(HSSFFont.U_SINGLE);
                font.setColor(HSSFColor.BLUE.index);
                cellStyleWrap.setFont(font);
                cellStyleWrap.setWrapText(true);
            }
            return cellStyleWrap;
        } else {
            if (cellStyleNoWrap == null) {
                cellStyleNoWrap = workbook.createCellStyle();
                cellStyleNoWrap.setBorderTop(HSSFCellStyle.BORDER_THIN);
                cellStyleNoWrap.setBorderBottom(HSSFCellStyle.BORDER_THIN);
                cellStyleNoWrap.setBorderLeft(HSSFCellStyle.BORDER_THIN);
                cellStyleNoWrap.setBorderRight(HSSFCellStyle.BORDER_THIN);
                cellStyleNoWrap.setWrapText(false);
            }
            return cellStyleNoWrap;
        }
    }

    public void beginRow() {
        row = sheet.createRow(rowNum++);
        if (isInFooter()) {
            footerRowNum = (short) (rowNum - 1);
            footerRow = row;
        }
        colNum = 0;
    }

    public void beginColumn() {
        cell = row.createCell(colNum++);
        cell.setCellStyle(createCellStyle(colNum, columnType));
        if (isInHeader()) {
            cell.setCellStyle(headerStyle);
        } else if (isInFooter()) {
            cell.setCellStyle(footerStyle);
            footerCell = cell;
        }
        columnBuffer = new StringBuffer();

        if (colNum > maxColumn) {
            maxColumn = colNum;
        }
    }

    public void endColumn(String linkName) {
        if (isInFooter()) {
            // hold off setting the cell content, don't want this column to autosize because of the footer
            footer = columnBuffer.toString();
        } else if (isInHeader()) {
            cell.setCellValue(new HSSFRichTextString(columnBuffer.toString()));
        } else {
            if (columnType == Report.DataType.INTEGER) {
                cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
                cell.setCellValue(Double.parseDouble(columnBuffer.toString()));
            } else if (columnType == Report.DataType.HYPER_LINK)
            {    
                 HSSFHyperlink link = new  HSSFHyperlink(HSSFHyperlink.LINK_URL);
                 link.setAddress(columnBuffer.toString());
                 cell.setHyperlink(link);
                 cell.setCellValue(linkName != null ? new HSSFRichTextString(linkName) : new HSSFRichTextString("Link"));
            }
            else
            {
                cell.setCellValue(new HSSFRichTextString(columnBuffer.toString()));
            }
        }
        columnType = null;
    } 
    
    public void endColumn() {
    	endColumn(null);
    }

    public void setColumnType(Report.DataType t) {
        columnType = t;
    }

    public void setColumnWidths(int[] t) {
        //Some day
    }

    public void setColumnAlignment(ColumnAlignment t) {
        // some day
    }

    public void setNoOfColumns(int t) {
         // some day
    }

    public void print(Object obj) {
        columnBuffer.append(obj != null ? obj.toString() : " ");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void beginFormat() throws Exception {
        // sheetName = "Sheet1";
        addLogo = true;
        addFooter = true;
        title = null;
        workbook = null;
        sheet = null;
    }

    public void endFormat() throws Exception {
        // removing the gridlines cleans things up
        sheet.setDisplayGridlines(false);

        // Man, this is bad. Don't resize the first column because it'll stretch the logo
        for (short i = 1; i < maxColumn; i++) {
            sheet.autoSizeColumn(i);
        }

        if (addLogo) {
            // Add the logo
            // This is also bad... The cell is effectively hardcoded to a size that's proportional to the
            // logo image. If the logo image ever changes you need to make sure it's either the same dimensions
            // or you change this code.
            HSSFRow logoRow = sheet.createRow(logoRowNum);
            logoRow.setHeightInPoints(32);
            sheet.setColumnWidth((short) 0, (short) (15 * 256));  // units are in 1/256 of a character width

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(ExcelReportFormatter.class.getResourceAsStream("/images/rdc_logo.png"), out);

            int index = workbook.addPicture(out.toByteArray(), HSSFWorkbook.PICTURE_TYPE_PNG);
            HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
            HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, 880, 255, (short) 0, 0, (short) 0, 0); // this places the logo entirely in cell 0,0
            anchor.setAnchorType(2);
            patriarch.createPicture(anchor, index);
        }

        if (title != null) {
            HSSFRow titleRow = sheet.createRow(titleRowNum);
            HSSFCell titleCell = titleRow.createCell((short) 0);
            titleCell.setCellStyle(titleStyle);
            titleCell.setCellValue(new HSSFRichTextString(title));
            titleCell.setAsActiveCell(); // select the cell - looks nice when the user opens the spreadsheet
            sheet.addMergedRegion(new Region(titleRowNum, (short) 0, titleRowNum, (short) (maxColumn - 1)));
        }

        if (addFooter) {
            // fix up the footer
            footerCell.setCellValue(new HSSFRichTextString(footer));

            // Merging cells doesn't work like I expected.
            // The background color merges across but cell borders don't...
            // Manually create filler cells with the appropriate borders and then merge them.
            HSSFCellStyle topBottomStyle = workbook.createCellStyle();
            topBottomStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
            topBottomStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
            topBottomStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);

            topBottomStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            topBottomStyle.setFont(footerFont);

            HSSFCellStyle topBottomRightStyle = workbook.createCellStyle();
            topBottomRightStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
            topBottomRightStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
            topBottomRightStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
            topBottomRightStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            topBottomRightStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            topBottomRightStyle.setFont(footerFont);

            HSSFCell cell = null;
            for (short i = 1; i < maxColumn; i++) {
                cell = footerRow.createCell(i);
                cell.setCellStyle(topBottomStyle);
            }
            if (cell != null) {
                cell.setCellStyle(topBottomRightStyle);
            }
            sheet.addMergedRegion(new Region(footerRowNum, (short) 0, footerRowNum, (short) (maxColumn - 1)));
        }
    }

    public Object format(Report report) throws Exception {
        beginFormat();
        // report.format(this);
        endFormat();

        return workbook;
    }

    public Object getResults() {
        return workbook;
    }
}